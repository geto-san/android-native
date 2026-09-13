import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";

/** Maximum concurrent devices per account before oldest sessions are evicted. */
export const MAX_ACTIVE_DEVICES = 3;

interface ActiveDeviceRecord {
  device_id: string;
  device_name: string | null;
  platform: string;
  last_active_ms: number;
}

export async function mergeCustomClaims(
  uid: string,
  patch: Record<string, unknown>
): Promise<Record<string, unknown>> {
  const user = await admin.auth().getUser(uid);
  const merged = { ...(user.customClaims ?? {}), ...patch };
  await admin.auth().setCustomUserClaims(uid, merged);
  return merged;
}

/**
 * Registers the calling device in users/{uid}.active_devices.
 * When the cap is exceeded, bumps session_version, revokes refresh tokens,
 * and forces other devices to re-authenticate on the next token refresh.
 */
export const syncActiveDevice = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Must be signed in to register a device."
    );
  }

  const deviceId = (data?.deviceId as string | undefined)?.trim();
  if (!deviceId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "deviceId is required."
    );
  }

  const uid = context.auth.uid;
  const deviceName = (data?.deviceName as string | undefined)?.trim() || null;
  const platform = (data?.platform as string | undefined)?.trim() || "android";
  const userRef = admin.firestore().collection("users").doc(uid);
  const nowMs = Date.now();

  let sessionVersion = 1;
  let evicted = false;

  await admin.firestore().runTransaction(async (tx) => {
    const snap = await tx.get(userRef);
    const existing = snap.data() ?? {};
    sessionVersion = (existing.session_version as number | undefined) ?? 1;

    let devices: ActiveDeviceRecord[] = Array.isArray(existing.active_devices)
      ? (existing.active_devices as ActiveDeviceRecord[]).filter(
          (entry) => entry?.device_id
        )
      : [];

    devices = devices.filter((entry) => entry.device_id !== deviceId);
    devices.push({
      device_id: deviceId,
      device_name: deviceName,
      platform,
      last_active_ms: nowMs,
    });
    devices.sort((a, b) => a.last_active_ms - b.last_active_ms);

    if (devices.length > MAX_ACTIVE_DEVICES) {
      const overflow = devices.length - MAX_ACTIVE_DEVICES;
      devices = devices.slice(overflow);
      sessionVersion += 1;
      evicted = true;
    }

    tx.set(
      userRef,
      {
        active_devices: devices,
        session_version: sessionVersion,
        device_synced_at: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
  });

  if (evicted) {
    await mergeCustomClaims(uid, { session_version: sessionVersion });
    await admin.auth().revokeRefreshTokens(uid);
  }

  return { sessionVersion, evicted };
});
