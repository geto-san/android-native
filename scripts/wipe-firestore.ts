import admin from 'firebase-admin';

// Wipes the mobile-app Firestore collections that mirror live operational data from the
// Laravel backend (incidents, sightings, sos_alerts, feed, notifications, tasks) plus the
// users collection. Users is included because provisioning is driven by Firebase Auth (and
// the Laravel bridge), so any stale custom-claim/uid drift clears with the rest.
//
// Usage:
//   Target the local emulator        -> set GCLOUD_PROJECT + FIRESTORE_EMULATOR_HOST first
//                                       (defaults target 'demo-wildwatch-local' emulator, same
//                                       as scripts/seed.ts)
//   Target the hosted project        -> GOOGLE_APPLICATION_CREDENTIALS=/path/to/svc-account.json
//                                       and NO FIRESTORE_EMULATOR_HOST set. This is destructive
//                                       and irreversible - it deletes every document in the
//                                       seven collections below, on the LIVE project.
//
// Compiled/run with the same tooling as seed.ts, e.g.:
//   npx ts-node scripts/wipe-firestore.ts               (emulator)
//   WIPE_CONFIRM="DELETE ALL" GOOGLE_APPLICATION_CREDENTIALS=... \
//     npx ts-node scripts/wipe-firestore.ts --live      (live project, destructive)
const LIVE_CONFIRMATION = 'DELETE ALL';

process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || 'localhost:9099';

const isLive = process.argv.includes('--live');

admin.initializeApp({
  projectId: isLive
    ? undefined
    : process.env.GCLOUD_PROJECT || 'demo-wildwatch-local',
});

// Live mode MUST use a real service account (application default credentials). Without it
// the admin SDK either points at the emulator via FIRESTORE_EMULATOR_HOST or fails fast.
if (isLive && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error(
    '--live requires GOOGLE_APPLICATION_CREDENTIALS to be set to a service-account JSON file.',
  );
  process.exit(1);
}

const db = admin.firestore();

const COLLECTIONS = [
  'incidents',
  'sightings',
  'sos_alerts',
  'feed',
  'notifications',
  'tasks',
  'users',
];

async function wipeCollection(name: string): Promise<number> {
  const refs = await db.collection(name).listDocuments();
  const BATCH_LIMIT = 500;
  let deleted = 0;
  for (let i = 0; i < refs.length; i += BATCH_LIMIT) {
    const batch = db.batch();
    refs.slice(i, i + BATCH_LIMIT).forEach((ref) => batch.delete(ref));
    await batch.commit();
    deleted += Math.min(BATCH_LIMIT, refs.length - i);
  }
  return deleted;
}

async function wipe() {
  const target = isLive
    ? `LIVE Firestore project "${admin.app().options.projectId ?? '(default creds)'}"`
    : `emulator (${process.env.FIRESTORE_EMULATOR_HOST}) project "${admin.app().options.projectId}"`;
  console.log(`Targeting ${target}`);

  if (isLive) {
    const answer = process.env.WIPE_CONFIRM || '';
    if (answer !== LIVE_CONFIRMATION) {
      console.error(
        `Destructive live wipe requires WIPE_CONFIRM="${LIVE_CONFIRMATION}" in the environment.`,
      );
      process.exit(1);
    }
  }

  for (const name of COLLECTIONS) {
    const deleted = await wipeCollection(name);
    console.log(`Wiped ${name}: ${deleted} documents deleted`);
  }
  console.log('Wipe complete. Run scripts/seed.ts against the same target to re-seed.');
}

wipe().catch((err) => {
  console.error('Wipe failed:', err);
  process.exit(1);
});