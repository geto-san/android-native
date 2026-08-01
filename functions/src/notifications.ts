import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import { Change, EventContext } from "firebase-functions/v1";
import DocumentSnapshot = FirebaseFirestore.DocumentSnapshot;

type IncidentData = Record<string, unknown>;

function normalizeParkId(park: unknown): string {
  if (typeof park !== "string" || park.length === 0) {
    return "unknown";
  }

  return park
    .replace(/([a-z])([A-Z])/g, "$1_$2")
    .replace(/\s+/g, "_")
    .toLowerCase();
}

function notificationDocRef() {
  return admin.firestore().collection("notifications").doc();
}

async function writeNotificationDoc(
  targetUid: string | null,
  type: string,
  title: string,
  message: string,
  data: Record<string, string> = {}
): Promise<string> {
  const ref = notificationDocRef();
  await ref.set({
    target_uid: targetUid,
    type,
    title,
    message,
    time: new Date().toISOString(),
    isRead: false,
    data,
    created_at: FieldValue.serverTimestamp(),
  });
  return ref.id;
}

async function sendTopicNotification(
  topic: string,
  title: string,
  body: string,
  type: string,
  data: Record<string, string> = {}
): Promise<void> {
  await admin.messaging().send({
    topic,
    notification: { title, body },
    data: { type, ...data },
  });
}

async function sendTokenNotifications(
  tokens: string[],
  title: string,
  body: string,
  type: string,
  data: Record<string, string> = {}
): Promise<void> {
  if (tokens.length === 0) {
    return;
  }

  await admin.messaging().sendEachForMulticast({
    tokens,
    notification: { title, body },
    data: { type, ...data },
  });
}

async function loadReporterFcmTokens(reporterUid: string | null | undefined): Promise<string[]> {
  if (!reporterUid) {
    return [];
  }

  const userDoc = await admin.firestore().collection("users").doc(reporterUid).get();
  const tokens = userDoc.data()?.fcm_tokens;
  if (!Array.isArray(tokens)) {
    return [];
  }

  return tokens.filter((token): token is string => typeof token === "string" && token.length > 0);
}

function isSightingApproval(data: IncidentData): boolean {
  const type = String(data.type ?? "").toLowerCase();
  const status = String(data.status ?? data.approval_status ?? "").toLowerCase();
  return type === "sighting" && (status === "approved" || status === "resolved");
}

export async function handleIncidentCreateNotifications(
  change: Change<DocumentSnapshot>,
  context: EventContext
): Promise<void> {
  if (!change.after.exists) {
    return;
  }

  const data = change.after.data() as IncidentData | undefined;
  if (!data) {
    return;
  }

  const incidentId = context.params.incidentId as string;
  const parkTopicSuffix = normalizeParkId(data.park);
  const incidentType = String(data.type ?? "").toLowerCase();

  if (isSightingApproval(data)) {
    const reporterUid =
      (typeof data.userId === "string" && data.userId) ||
      (typeof data.reporter_uid === "string" && data.reporter_uid) ||
      null;
    const tokens = await loadReporterFcmTokens(reporterUid);
    const title = "Sighting approved";
    const message =
      typeof data.summary === "string" && data.summary.length > 0
        ? data.summary
        : "Your wildlife sighting report has been approved.";

    await writeNotificationDoc(reporterUid, "SIGHTING_APPROVED", title, message, {
      incidentId,
    });
    await sendTokenNotifications(tokens, title, message, "SIGHTING_APPROVED", {
      incidentId,
    });
    return;
  }

  if (incidentType === "emergency") {
    const topic = `park_alerts_${parkTopicSuffix}`;
    const title = "Emergency alert";
    const message =
      typeof data.summary === "string" && data.summary.length > 0
        ? data.summary
        : "A new emergency incident requires attention.";

    await writeNotificationDoc(null, "SECURITY_ALERT", title, message, {
      incidentId,
      topic,
    });
    await sendTopicNotification(topic, title, message, "SECURITY_ALERT", { incidentId });
    return;
  }

  const topic = `warden_${parkTopicSuffix}`;
  const title = "New incident report";
  const message =
    typeof data.summary === "string" && data.summary.length > 0
      ? data.summary
      : "A new incident has been reported in your park.";

  await writeNotificationDoc(null, "SECURITY_ALERT", title, message, {
    incidentId,
    topic,
  });
  await sendTopicNotification(topic, title, message, "SECURITY_ALERT", { incidentId });
}

export async function handleSosAlertCreateNotifications(
  change: Change<DocumentSnapshot>,
  context: EventContext
): Promise<void> {
  if (!change.after.exists) {
    return;
  }

  const data = change.after.data() as IncidentData | undefined;
  if (!data) {
    return;
  }

  const sosId = context.params.sosAlertId as string;
  const parkTopicSuffix = normalizeParkId(data.park_id ?? data.park);
  const topic = `park_alerts_${parkTopicSuffix}`;
  const title = "SOS alert";
  const message =
    typeof data.description === "string" && data.description.length > 0
      ? data.description
      : "An SOS alert has been raised.";

  await writeNotificationDoc(null, "SECURITY_ALERT", title, message, {
    sosId,
    topic,
  });
  await sendTopicNotification(topic, title, message, "SECURITY_ALERT", { sosId });
}

export async function handleFeedArticleCreateNotifications(
  snap: FirebaseFirestore.DocumentSnapshot,
  context: EventContext
): Promise<void> {
  const data = snap.data() as Record<string, unknown> | undefined;
  if (!data) {
    return;
  }

  const articleId = context.params.articleId as string;
  const title = typeof data.title === "string" ? data.title : "New community article";
  const message =
    typeof data.excerpt === "string" && data.excerpt.length > 0
      ? data.excerpt
      : "A new article was published in the community feed.";

  await writeNotificationDoc(null, "NEW_FEED_ARTICLE", title, message, {
    articleId,
    topic: "park_alerts_all",
  });
  await sendTopicNotification("park_alerts_all", title, message, "NEW_FEED_ARTICLE", {
    articleId,
  });
}

export async function handleSightingApprovalNotifications(
  change: Change<DocumentSnapshot>,
  context: EventContext
): Promise<void> {
  if (!change.before.exists || !change.after.exists) {
    return;
  }

  const before = change.before.data() as IncidentData | undefined;
  const after = change.after.data() as IncidentData | undefined;
  if (!before || !after) {
    return;
  }

  const beforeStatus = String(before.status ?? before.approval_status ?? "").toLowerCase();
  const afterStatus = String(after.status ?? after.approval_status ?? "").toLowerCase();
  const becameApproved =
    after.source_system === "laravel" &&
    beforeStatus !== afterStatus &&
    (afterStatus === "approved" || afterStatus === "resolved");

  if (!becameApproved) {
    return;
  }

  const sightingId = context.params.sightingId as string;
  const reporterUid =
    (typeof after.reporter_uid === "string" && after.reporter_uid) ||
    (typeof after.userId === "string" && after.userId) ||
    null;
  const tokens = await loadReporterFcmTokens(reporterUid);
  const title = "Sighting approved";
  const message =
    typeof after.notes === "string" && after.notes.length > 0
      ? after.notes
      : "Your wildlife sighting report has been approved.";

  await writeNotificationDoc(reporterUid, "SIGHTING_APPROVED", title, message, {
    sightingId,
  });
  await sendTokenNotifications(tokens, title, message, "SIGHTING_APPROVED", {
    sightingId,
  });
}
