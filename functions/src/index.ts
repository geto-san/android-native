import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";

admin.initializeApp();

/**
 * Triggered when a new user is created in Firebase Auth.
 * Sets the default 'public' role in custom claims and creates a Firestore user document.
 */
export const onUserCreated = functions.auth.user().onCreate(async (user) => {
  // Anonymous users (guests) don't have provider data.
  // We skip creating a Firestore document for them per AGENTS.md requirements.
  if (!user.providerData || user.providerData.length === 0) {
    console.log(`Skipping Firestore profile for guest user: ${user.uid}`);
    return;
  }

  const role = "public";

  try {
    // Set custom claims
    await admin.auth().setCustomUserClaims(user.uid, { role });

    // Create shadow document in Firestore
    await admin.firestore().collection("users").doc(user.uid).set({
      uid: user.uid,
      email: user.email,
      displayName: user.displayName,
      role: role,
      park_id: null,
      created_at: FieldValue.serverTimestamp(),
    });

    console.log(`User ${user.uid} initialized with role: ${role}`);
  } catch (error) {
    console.error("Error in onUserCreated trigger:", error);
  }
});

/**
 * Callable function to update a user's role.
 * In production, this should be restricted to 'uwa_official' or 'warden'.
 */
export const setUserRole = functions.https.onCall(async (data, context) => {
  // Authentication check
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

  // Basic authorization check (allow uwa_official to set any role, warden only rangers/public in their park)
  const callerRole = context.auth.token.role;
  const callerParkId = context.auth.token.park_id;

  if (callerRole !== "uwa_official") {
    if (callerRole === "warden") {
      if (role === "uwa_official" || role === "warden") {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Wardens cannot promote users to Warden or UWA Official."
        );
      }
      // Ensure the target is in the same park or role change is valid for their scope
      // (Simplified for now)
    } else {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Only UWA Officials or Wardens can update roles."
      );
    }
  }

  try {
    await admin.auth().setCustomUserClaims(targetUid, { role, park_id: parkId });

    await admin.firestore().collection("users").doc(targetUid).update({
      role: role,
      park_id: parkId || null,
      updated_at: FieldValue.serverTimestamp(),
    });

    return { message: `Success! User ${targetUid} is now ${role}.` };
  } catch (error) {
    console.error("Error in setUserRole:", error);
    throw new functions.https.HttpsError("internal", "Failed to update user role.");
  }
});
