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
exports.onFeedArticleCreated = exports.onSosAlertWritten = exports.onSightingWritten = exports.onIncidentWritten = exports.setUserRole = exports.onUserCreated = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-admin/firestore");
const bridge_1 = require("./bridge");
const notifications_1 = require("./notifications");
admin.initializeApp();
exports.onUserCreated = functions.auth.user().onCreate(async (user) => {
    if (!user.providerData || user.providerData.length === 0) {
        console.log(`Skipping Firestore profile for guest user: ${user.uid}`);
        return;
    }
    const role = "public";
    try {
        await admin.auth().setCustomUserClaims(user.uid, { role });
        await admin.firestore().collection("users").doc(user.uid).set({
            uid: user.uid,
            email: user.email,
            displayName: user.displayName,
            role: role,
            park_id: null,
            created_at: firestore_1.FieldValue.serverTimestamp(),
        });
        console.log(`User ${user.uid} initialized with role: ${role}`);
    }
    catch (error) {
        console.error("Error in onUserCreated trigger:", error);
    }
});
exports.setUserRole = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Only authenticated users can update roles.");
    }
    const { targetUid, role, parkId } = data;
    if (!targetUid || !role) {
        throw new functions.https.HttpsError("invalid-argument", "The function must be called with targetUid and role.");
    }
    const callerRole = context.auth.token.role;
    if (callerRole !== "uwa_official") {
        if (callerRole === "warden") {
            if (role === "uwa_official" || role === "warden") {
                throw new functions.https.HttpsError("permission-denied", "Wardens cannot promote users to Warden or UWA Official.");
            }
        }
        else {
            throw new functions.https.HttpsError("permission-denied", "Only UWA Officials or Wardens can update roles.");
        }
    }
    try {
        await admin.auth().setCustomUserClaims(targetUid, { role, park_id: parkId });
        await admin.firestore().collection("users").doc(targetUid).update({
            role: role,
            park_id: parkId || null,
            updated_at: firestore_1.FieldValue.serverTimestamp(),
        });
        await admin.firestore().collection("role_audit").add({
            targetUid,
            newRole: role,
            newParkId: parkId || null,
            changedBy: context.auth.uid,
            changedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        return { message: `Success! User ${targetUid} is now ${role}.` };
    }
    catch (error) {
        console.error("Error in setUserRole:", error);
        throw new functions.https.HttpsError("internal", "Failed to update user role.");
    }
});
function buildBridgePayload(change, docId) {
    const before = change.before.exists
        ? change.before.data()
        : null;
    const after = change.after.exists
        ? change.after.data()
        : null;
    let eventType = "update";
    if (!change.before.exists && change.after.exists) {
        eventType = "create";
    }
    else if (change.before.exists && !change.after.exists) {
        eventType = "delete";
    }
    return { docId, before, after, eventType };
}
function latestBridgeData(payload) {
    return payload.after ?? payload.before;
}
exports.onIncidentWritten = functions.firestore
    .document("incidents/{incidentId}")
    .onWrite(async (change, context) => {
    const payload = buildBridgePayload(change, context.params.incidentId);
    const latest = latestBridgeData(payload);
    if ((0, bridge_1.shouldSkipBridge)(latest)) {
        console.log(`Skipping incidents bridge echo for ${payload.docId}`);
        return;
    }
    await (0, bridge_1.postToLaravelWebhook)("incidents", payload);
    if (payload.eventType === "create" && payload.after) {
        await (0, notifications_1.handleIncidentCreateNotifications)(change, context);
    }
});
exports.onSightingWritten = functions.firestore
    .document("sightings/{sightingId}")
    .onWrite(async (change, context) => {
    const payload = buildBridgePayload(change, context.params.sightingId);
    const latest = latestBridgeData(payload);
    if ((0, bridge_1.shouldSkipBridge)(latest)) {
        console.log(`Skipping sightings bridge echo for ${payload.docId}`);
        return;
    }
    await (0, bridge_1.postToLaravelWebhook)("sightings", payload);
    if (payload.eventType === "update") {
        await (0, notifications_1.handleSightingApprovalNotifications)(change, context);
    }
});
exports.onSosAlertWritten = functions
    .runWith({ timeoutSeconds: 60 })
    .firestore.document("sos_alerts/{sosAlertId}")
    .onWrite(async (change, context) => {
    const payload = buildBridgePayload(change, context.params.sosAlertId);
    const latest = latestBridgeData(payload);
    if ((0, bridge_1.shouldSkipBridge)(latest)) {
        console.log(`Skipping sos_alerts bridge echo for ${payload.docId}`);
        return;
    }
    await (0, bridge_1.postToLaravelWebhook)("sos-alerts", payload);
    if (payload.eventType === "create" && payload.after) {
        await (0, notifications_1.handleSosAlertCreateNotifications)(change, context);
    }
});
exports.onFeedArticleCreated = functions.firestore
    .document("feed/{articleId}")
    .onCreate(async (snap, context) => {
    await (0, notifications_1.handleFeedArticleCreateNotifications)(snap, context);
});
//# sourceMappingURL=index.js.map