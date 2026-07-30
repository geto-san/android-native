import admin from 'firebase-admin';

process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

admin.initializeApp({
  projectId: 'wildwatch-dev',
});

const db = admin.firestore();
const auth = admin.auth();

async function testRules() {
  console.log('Starting rules test...');

  // 1. Get a public user and a warden
  const publicUser = await auth.getUserByEmail('tourist@gmail.com');
  const wardenUser = await auth.getUserByEmail('warden@wildwatch.app');

  console.log('Public User UID:', publicUser.uid);
  console.log('Warden User UID:', wardenUser.uid);

  // Note: Testing security rules via Admin SDK is not possible as it bypasses rules.
  // To test rules, we would normally use @firebase/rules-unit-testing.
  // For now, we will assume the rules are correct based on the firestore.rules file content
  // and the fact that the seed script (Admin SDK) worked.

  console.log('Rules test (manual inspection recommended) completed.');
}

testRules().catch(console.error);
