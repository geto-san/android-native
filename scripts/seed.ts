import admin from 'firebase-admin';
import * as fs from 'fs';
import * as path from 'path';

process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || 'localhost:9099';

admin.initializeApp({
  projectId: process.env.GCLOUD_PROJECT || 'demo-wildwatch-local',
});

const db = admin.firestore();
const auth = admin.auth();

interface SharedFixtures {
  park: { firestore_id: string; mysql_name: string; district: string };
  users: Array<{
    email: string;
    password: string;
    display_name: string;
    firebase_role: string;
    park_id: string | null;
  }>;
}

function loadFixtures(): SharedFixtures {
  const candidates = [
    path.join(__dirname, 'fixtures/shared-seed-fixtures.json'),
    path.join(__dirname, '../../wildwatch-local-development-env-setup/seed-shared-fixtures.json'),
  ];
  for (const file of candidates) {
    if (fs.existsSync(file)) {
      return JSON.parse(fs.readFileSync(file, 'utf8')) as SharedFixtures;
    }
  }
  throw new Error('Shared seed fixtures not found');
}

async function seed() {
  console.log('Starting seed process (shared fixtures)...');
  const fixtures = loadFixtures();
  const parkId = fixtures.park.firestore_id;

  await db.collection('parks').doc(parkId).set({
    name: fixtures.park.mysql_name,
    location: new admin.firestore.GeoPoint(-1.05, 29.7),
    boundary_geojson: JSON.stringify({
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [[[29.6, -1.0], [29.8, -1.0], [29.8, -1.1], [29.6, -1.1], [29.6, -1.0]]],
      },
    }),
    tile_package_url: 'https://r2.wildwatch.app/bwindi.pmtiles',
    routing_graph_url: 'https://routing.wildwatch.app/bwindi',
  });

  for (const r of fixtures.users) {
    let user;
    try {
      user = await auth.getUserByEmail(r.email);
    } catch {
      user = await auth.createUser({
        email: r.email,
        password: r.password,
        displayName: r.display_name,
      });
    }

    await auth.setCustomUserClaims(user.uid, { role: r.firebase_role, park_id: r.park_id });

    await db.collection('users').doc(user.uid).set({
      uid: user.uid,
      role: r.firebase_role,
      park_id: r.park_id,
      name: r.display_name,
      contact: r.email,
      fcm_tokens: [],
      created_at: admin.firestore.FieldValue.serverTimestamp(),
    });

    console.log(`Seeded user: ${r.display_name} (${r.firebase_role})`);
  }

  const pois = [
    { name: 'Mubare Gorilla Group', type: 'wildlife', location: new admin.firestore.GeoPoint(-1.06, 29.71) },
    { name: 'Buhoma Park Office', type: 'office', location: new admin.firestore.GeoPoint(-1.055, 29.705) },
  ];

  for (const poi of pois) {
    await db.collection('pois').add({ ...poi, park_id: parkId });
  }

  console.log('Seed process completed successfully!');
}

seed().catch(console.error);
