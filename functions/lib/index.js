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
exports.setUserRole = exports.onUserCreated = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-admin/firestore");
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
    const callerParkId = context.auth.token.park_id;
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
        return { message: `Success! User ${targetUid} is now ${role}.` };
    }
    catch (error) {
        console.error("Error in setUserRole:", error);
        throw new functions.https.HttpsError("internal", "Failed to update user role.");
    }
});
//# sourceMappingURL=index.js.map