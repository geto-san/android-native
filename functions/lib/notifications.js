"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleIncidentCreateNotifications = handleIncidentCreateNotifications;
exports.handleSosAlertCreateNotifications = handleSosAlertCreateNotifications;
exports.handleFeedArticleCreateNotifications = handleFeedArticleCreateNotifications;
exports.handleSightingApprovalNotifications = handleSightingApprovalNotifications;
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-admin/firestore");
function normalizeParkId(park) {
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
async function writeNotificationDoc(targetUid, type, title, message, data = {}) {
    const ref = notificationDocRef();
    await ref.set({
        target_uid: targetUid,
        type,
        title,
        message,
        time: new Date().toISOString(),
        isRead: false,
        data,
        created_at: firestore_1.FieldValue.serverTimestamp(),
    });
    return ref.id;
}
async function sendTopicNotification(topic, title, body, type, data = {}) {
    await admin.messaging().send({
        topic,
        notification: { title, body },
        data: { type, ...data },
    });
}
async function sendTokenNotifications(tokens, title, body, type, data = {}) {
    if (tokens.length === 0) {
        return;
    }
    await admin.messaging().sendEachForMulticast({
        tokens,
        notification: { title, body },
        data: { type, ...data },
    });
}
async function loadReporterFcmTokens(reporterUid) {
    if (!reporterUid) {
        return [];
    }
    const userDoc = await admin.firestore().collection("users").doc(reporterUid).get();
    const tokens = userDoc.data()?.fcm_tokens;
    if (!Array.isArray(tokens)) {
        return [];
    }
    return tokens.filter((token) => typeof token === "string" && token.length > 0);
}
function isSightingApproval(data) {
    const type = String(data.type ?? "").toLowerCase();
    const status = String(data.status ?? data.approval_status ?? "").toLowerCase();
    return type === "sighting" && (status === "approved" || status === "resolved");
}
async function handleIncidentCreateNotifications(change, context) {
    if (!change.after.exists) {
        return;
    }
    const data = change.after.data();
    if (!data) {
        return;
    }
    const incidentId = context.params.incidentId;
    const parkTopicSuffix = normalizeParkId(data.park);
    const incidentType = String(data.type ?? "").toLowerCase();
    if (isSightingApproval(data)) {
        const reporterUid = (typeof data.userId === "string" && data.userId) ||
            (typeof data.reporter_uid === "string" && data.reporter_uid) ||
            null;
        const tokens = await loadReporterFcmTokens(reporterUid);
        const title = "Sighting approved";
        const message = typeof data.summary === "string" && data.summary.length > 0
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
        const message = typeof data.summary === "string" && data.summary.length > 0
            ? data.summary
            : "A new emergency incident requires attention.";
        await writeNotificationDoc(null, "SECURITY_ALERT", title, message, {
            incidentId,
            topic,
        });
        await sendTopicNotification(topic, title, message, "SECURITY_ALERT", { incidentId });
        return;
    }
    if (data.animalSeen === false) {
        return;
    }
    const wardenTopic = `warden_${parkTopicSuffix}`;
    const publicTopic = `park_alerts_${parkTopicSuffix}`;
    const title = "New incident report";
    const message = typeof data.summary === "string" && data.summary.length > 0
        ? data.summary
        : "A new incident has been reported in your park.";
    await writeNotificationDoc(null, "SECURITY_ALERT", title, message, {
        incidentId,
        topic: publicTopic,
    });
    await sendTopicNotification(wardenTopic, title, message, "SECURITY_ALERT", { incidentId });
    await sendTopicNotification(publicTopic, title, message, "SECURITY_ALERT", { incidentId });
}
async function handleSosAlertCreateNotifications(change, context) {
    if (!change.after.exists) {
        return;
    }
    const data = change.after.data();
    if (!data) {
        return;
    }
    const sosId = context.params.sosAlertId;
    const parkTopicSuffix = normalizeParkId(data.park_id ?? data.park);
    const topic = `park_alerts_${parkTopicSuffix}`;
    const title = "SOS alert";
    const message = typeof data.description === "string" && data.description.length > 0
        ? data.description
        : "An SOS alert has been raised.";
    await writeNotificationDoc(null, "SECURITY_ALERT", title, message, {
        sosId,
        topic,
    });
    await sendTopicNotification(topic, title, message, "SECURITY_ALERT", { sosId });
}
async function handleFeedArticleCreateNotifications(snap, context) {
    const data = snap.data();
    if (!data) {
        return;
    }
    const articleId = context.params.articleId;
    const title = typeof data.title === "string" ? data.title : "New community article";
    const message = typeof data.excerpt === "string" && data.excerpt.length > 0
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
async function handleSightingApprovalNotifications(change, context) {
    if (!change.before.exists || !change.after.exists) {
        return;
    }
    const before = change.before.data();
    const after = change.after.data();
    if (!before || !after) {
        return;
    }
    const beforeStatus = String(before.status ?? before.approval_status ?? "").toLowerCase();
    const afterStatus = String(after.status ?? after.approval_status ?? "").toLowerCase();
    const becameApproved = after.source_system === "laravel" &&
        beforeStatus !== afterStatus &&
        (afterStatus === "approved" || afterStatus === "resolved");
    if (!becameApproved) {
        return;
    }
    const sightingId = context.params.sightingId;
    const reporterUid = (typeof after.reporter_uid === "string" && after.reporter_uid) ||
        (typeof after.userId === "string" && after.userId) ||
        null;
    const tokens = await loadReporterFcmTokens(reporterUid);
    const title = "Sighting approved";
    const message = typeof after.notes === "string" && after.notes.length > 0
        ? after.notes
        : "Your wildlife sighting report has been approved.";
    await writeNotificationDoc(reporterUid, "SIGHTING_APPROVED", title, message, {
        sightingId,
    });
    await sendTokenNotifications(tokens, title, message, "SIGHTING_APPROVED", {
        sightingId,
    });
}
//# sourceMappingURL=notifications.js.map