import admin from 'firebase-admin';

process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

admin.initializeApp({
  projectId: 'demo-wildwatch-local',
});

const auth = admin.auth();
const db = admin.firestore();

async function seed() {
  console.log('Starting auth seed process...');

  const users = [
    {
      email: 'headlesspackage@gmail.com',
      password: '12345678',
      displayName: 'Headless Package',
      role: 'public'
    },
    {
      email: 'ranger@wildwatch.com',
      password: '12345678',
      displayName: 'WildWatch Ranger',
      role: 'ranger'
    }
  ];

  for (const userData of users) {
    let user;
    try {
      user = await auth.getUserByEmail(userData.email);
      console.log(`User already exists: ${userData.email}. Updating...`);
      await auth.updateUser(user.uid, {
        password: userData.password,
        displayName: userData.displayName
      });
    } catch (e) {
      user = await auth.createUser({
        email: userData.email,
        password: userData.password,
        displayName: userData.displayName
      });
      console.log(`Created new user: ${userData.email}`);
    }

    // Set custom claims for role-based access
    await auth.setCustomUserClaims(user.uid, { role: userData.role });
    console.log(`Set custom claims for ${userData.email}: { role: ${userData.role} }`);

    // Create user document in Firestore (matches onUserCreated logic but forced here)
    await db.collection('users').doc(user.uid).set({
      uid: user.uid,
      email: user.email,
      displayName: user.displayName,
      role: userData.role,
      park_id: userData.role === 'ranger' ? 'bwindi-impenetrable' : null,
      created_at: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    console.log(`Updated Firestore document for ${userData.email}`);
  }

  console.log('Auth seed process completed successfully!');
}

seed().catch(console.error);
