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
exports.syncActiveDevice = exports.MAX_ACTIVE_DEVICES = void 0;
exports.mergeCustomClaims = mergeCustomClaims;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-admin/firestore");
exports.MAX_ACTIVE_DEVICES = 3;
async function mergeCustomClaims(uid, patch) {
    const user = await admin.auth().getUser(uid);
    const merged = { ...(user.customClaims ?? {}), ...patch };
    await admin.auth().setCustomUserClaims(uid, merged);
    return merged;
}
exports.syncActiveDevice = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be signed in to register a device.");
    }
    const deviceId = data?.deviceId?.trim();
    if (!deviceId) {
        throw new functions.https.HttpsError("invalid-argument", "deviceId is required.");
    }
    const uid = context.auth.uid;
    const deviceName = data?.deviceName?.trim() || null;
    const platform = data?.platform?.trim() || "android";
    const userRef = admin.firestore().collection("users").doc(uid);
    const nowMs = Date.now();
    let sessionVersion = 1;
    let evicted = false;
    await admin.firestore().runTransaction(async (tx) => {
        const snap = await tx.get(userRef);
        const existing = snap.data() ?? {};
        sessionVersion = existing.session_version ?? 1;
        let devices = Array.isArray(existing.active_devices)
            ? existing.active_devices.filter((entry) => entry?.device_id)
            : [];
        devices = devices.filter((entry) => entry.device_id !== deviceId);
        devices.push({
            device_id: deviceId,
            device_name: deviceName,
            platform,
            last_active_ms: nowMs,
        });
        devices.sort((a, b) => a.last_active_ms - b.last_active_ms);
        if (devices.length > exports.MAX_ACTIVE_DEVICES) {
            const overflow = devices.length - exports.MAX_ACTIVE_DEVICES;
            devices = devices.slice(overflow);
            sessionVersion += 1;
            evicted = true;
        }
        tx.set(userRef, {
            active_devices: devices,
            session_version: sessionVersion,
            device_synced_at: firestore_1.FieldValue.serverTimestamp(),
        }, { merge: true });
    });
    if (evicted) {
        await mergeCustomClaims(uid, { session_version: sessionVersion });
        await admin.auth().revokeRefreshTokens(uid);
    }
    return { sessionVersion, evicted };
});
//# sourceMappingURL=deviceSessions.js.map