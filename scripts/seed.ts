import admin from 'firebase-admin';

process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

admin.initializeApp({
  projectId: 'wildwatch-dev',
});

const db = admin.firestore();
const auth = admin.auth();

async function seed() {
  console.log('Starting seed process...');

  const parkId = 'bwindi-impenetrable';

  // 1. Seed Park
  await db.collection('parks').doc(parkId).set({
    name: 'Bwindi Impenetrable National Park',
    location: new admin.firestore.GeoPoint(-1.05, 29.7),
    boundary_geojson: JSON.stringify({
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [[[29.6, -1.0], [29.8, -1.0], [29.8, -1.1], [29.6, -1.1], [29.6, -1.0]]]
      }
    }),
    tile_package_url: 'https://r2.wildwatch.app/bwindi.pmtiles',
    routing_graph_url: 'https://routing.wildwatch.app/bwindi'
  });

  // 2. Seed Users
  const roles = [
    { email: 'ranger@wildwatch.app', role: 'ranger', park_id: parkId, name: 'John Ranger' },
    { email: 'warden@wildwatch.app', role: 'warden', park_id: parkId, name: 'Alice Warden' },
    { email: 'official@wildwatch.app', role: 'uwa_official', park_id: null, name: 'Bob Official' },
    { email: 'tourist@gmail.com', role: 'public', park_id: null, name: 'Tom Tourist' }
  ];

  for (const r of roles) {
    let user;
    try {
      user = await auth.getUserByEmail(r.email);
    } catch (e) {
      user = await auth.createUser({
        email: r.email,
        password: 'password123',
        displayName: r.name
      });
    }

    await auth.setCustomUserClaims(user.uid, { role: r.role, park_id: r.park_id });

    await db.collection('users').doc(user.uid).set({
      uid: user.uid,
      role: r.role,
      park_id: r.park_id,
      name: r.name,
      contact: r.email,
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log(`Seeded user: ${r.name} (${r.role})`);
  }

  // 3. Seed POIs
  const pois = [
    { name: 'Mubare Gorilla Group', type: 'wildlife', location: new admin.firestore.GeoPoint(-1.06, 29.71) },
    { name: 'Buhoma Park Office', type: 'office', location: new admin.firestore.GeoPoint(-1.055, 29.705) },
    { name: 'Waterfall Trail Start', type: 'trailhead', location: new admin.firestore.GeoPoint(-1.065, 29.72) }
  ];

  for (const poi of pois) {
    await db.collection('pois').add({ ...poi, park_id: parkId });
  }

  // 4. Seed Zones
  await db.collection('zones').add({
    park_id: parkId,
    type: 'restricted',
    name: 'Strict Nature Reserve',
    boundary_geojson: JSON.stringify({
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [[[29.7, -1.05], [29.75, -1.05], [29.75, -1.08], [29.7, -1.08], [29.7, -1.05]]]
      }
    })
  });

  console.log('Seed process completed successfully!');
}

seed().catch(console.error);
