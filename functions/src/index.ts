import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import {
  BridgePayload,
  postToLaravelWebhook,
  shouldSkipBridge,
} from "./bridge";
import {
  handleFeedArticleCreateNotifications,
  handleIncidentCreateNotifications,
  handleSightingApprovalNotifications,
  handleSosAlertCreateNotifications,
} from "./notifications";
import { mergeCustomClaims, syncActiveDevice } from "./deviceSessions";

export { syncActiveDevice };

admin.initializeApp();

/**
 * Triggered when a new user is created in Firebase Auth.
 * Sets the default 'public' role in custom claims and creates a Firestore user document.
 */
export const onUserCreated = functions.auth.user().onCreate(async (user) => {
  const role = "public";
  const isAnonymous = !user.providerData || user.providerData.length === 0;

  try {
    await admin.auth().setCustomUserClaims(user.uid, {
      role,
      session_version: 1,
    });

    await admin.firestore().collection("users").doc(user.uid).set({
      uid: user.uid,
      email: user.email ?? null,
      displayName: user.displayName ?? (isAnonymous ? "Guest" : null),
      role: role,
      park_id: null,
      is_anonymous: isAnonymous,
      session_version: 1,
      active_devices: [],
      created_at: FieldValue.serverTimestamp(),
    });

    console.log(
      `User ${user.uid} initialized with role: ${role}${isAnonymous ? " (anonymous)" : ""}`
    );
  } catch (error) {
    console.error("Error in onUserCreated trigger:", error);
  }
});

/**
 * Callable function to update a user's role.
 * In production, this should be restricted to 'uwa_official' or 'warden'.
 */
export const setUserRole = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Only authenticated users can update roles."
    );
  }

  const { targetUid, role, parkId } = data;

  if (!targetUid || !role) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The function must be called with targetUid and role."
    );
  }

  const callerRole = context.auth.token.role;

  if (callerRole !== "uwa_official") {
    if (callerRole === "warden") {
      if (role === "uwa_official" || role === "warden") {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Wardens cannot promote users to Warden or UWA Official."
        );
      }
    } else {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Only UWA Officials or Wardens can update roles."
      );
    }
  }

  try {
    await mergeCustomClaims(targetUid, { role, park_id: parkId ?? null });

    await admin.firestore().collection("users").doc(targetUid).update({
      role: role,
      park_id: parkId || null,
      updated_at: FieldValue.serverTimestamp(),
    });

    await admin.firestore().collection("role_audit").add({
      targetUid,
      newRole: role,
      newParkId: parkId || null,
      changedBy: context.auth.uid,
      changedAt: FieldValue.serverTimestamp(),
    });

    return { message: `Success! User ${targetUid} is now ${role}.` };
  } catch (error) {
    console.error("Error in setUserRole:", error);
    throw new functions.https.HttpsError("internal", "Failed to update user role.");
  }
});

function buildBridgePayload(
  change: functions.Change<FirebaseFirestore.DocumentSnapshot>,
  docId: string
): BridgePayload {
  const before = change.before.exists
    ? (change.before.data() as Record<string, unknown>)
    : null;
  const after = change.after.exists
    ? (change.after.data() as Record<string, unknown>)
    : null;

  let eventType: BridgePayload["eventType"] = "update";
  if (!change.before.exists && change.after.exists) {
    eventType = "create";
  } else if (change.before.exists && !change.after.exists) {
    eventType = "delete";
  }

  return { docId, before, after, eventType };
}

function latestBridgeData(payload: BridgePayload): Record<string, unknown> | null {
  return payload.after ?? payload.before;
}

/**
 * Firestore → Laravel bridge for incidents, plus FCM fan-out on create.
 */
export const onIncidentWritten = functions.firestore
  .document("incidents/{incidentId}")
  .onWrite(async (change, context) => {
    const payload = buildBridgePayload(change, context.params.incidentId);
    const latest = latestBridgeData(payload);

    if (shouldSkipBridge(latest)) {
      console.log(`Skipping incidents bridge echo for ${payload.docId}`);
      return;
    }

    await postToLaravelWebhook("incidents", payload);

    if (payload.eventType === "create" && payload.after) {
      await handleIncidentCreateNotifications(change, context);
    }
  });

/**
 * Firestore → Laravel bridge for wildlife sightings.
 */
export const onSightingWritten = functions.firestore
  .document("sightings/{sightingId}")
  .onWrite(async (change, context) => {
    const payload = buildBridgePayload(change, context.params.sightingId);
    const latest = latestBridgeData(payload);

    if (shouldSkipBridge(latest)) {
      console.log(`Skipping sightings bridge echo for ${payload.docId}`);
      return;
    }

    await postToLaravelWebhook("sightings", payload);

    if (payload.eventType === "update") {
      await handleSightingApprovalNotifications(change, context);
    }
  });

/**
 * Firestore → Laravel bridge for SOS alerts.
 * Runs synchronously (awaits webhook + notifications before returning).
 */
export const onSosAlertWritten = functions
  .runWith({ timeoutSeconds: 60 })
  .firestore.document("sos_alerts/{sosAlertId}")
  .onWrite(async (change, context) => {
    const payload = buildBridgePayload(change, context.params.sosAlertId);
    const latest = latestBridgeData(payload);

    if (shouldSkipBridge(latest)) {
      console.log(`Skipping sos_alerts bridge echo for ${payload.docId}`);
      return;
    }

    await postToLaravelWebhook("sos-alerts", payload);

    if (payload.eventType === "create" && payload.after) {
      await handleSosAlertCreateNotifications(change, context);
    }
  });

/**
 * Fan out a community feed article to all park alert subscribers.
 */
export const onFeedArticleCreated = functions.firestore
  .document("feed/{articleId}")
  .onCreate(async (snap, context) => {
    await handleFeedArticleCreateNotifications(snap, context);
  });
