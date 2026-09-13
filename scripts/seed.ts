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

interface ParkFixture {
  firestore_id: string;
  mysql_name: string;
  district: string;
}

interface SharedFixtures {
  park: ParkFixture;
  parks?: ParkFixture[];
  users: Array<{
    email: string;
    password: string;
    display_name: string;
    firebase_uid: string;
    firebase_role: string;
    park_id: string | null;
  }>;
}

const PARK_COORDS: Record<string, [number, number]> = {
  'bwindi-impenetrable': [-1.05, 29.7],
  'mgahinga-gorilla': [-1.37, 29.65],
  'queen-elizabeth': [-0.2, 30.0],
  'murchison-falls': [2.27, 31.77],
  'kibale': [0.5, 30.4],
  'semuliki': [0.85, 30.1],
  'rwenzori-mountains': [0.38, 29.98],
  'lake-mburo': [-0.61, 30.97],
  'kidepo-valley': [3.92, 33.86],
  'mount-elgon': [1.12, 34.17],
};

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

async function seedParks(parks: ParkFixture[]) {
  for (const park of parks) {
    const [lat, lng] = PARK_COORDS[park.firestore_id] ?? [0, 32];
    await db.collection('parks').doc(park.firestore_id).set({
      name: park.mysql_name,
      district: park.district,
      location: new admin.firestore.GeoPoint(lat, lng),
      boundary_geojson: JSON.stringify({
        type: 'Feature',
        geometry: {
          type: 'Polygon',
          coordinates: [[[lng - 0.1, lat - 0.05], [lng + 0.1, lat - 0.05], [lng + 0.1, lat + 0.05], [lng - 0.1, lat + 0.05], [lng - 0.1, lat - 0.05]]],
        },
      }),
      tile_package_url: `https://r2.wildwatch.app/${park.firestore_id}.pmtiles`,
      routing_graph_url: `https://routing.wildwatch.app/${park.firestore_id}`,
    });
    console.log(`Seeded park: ${park.mysql_name}`);
  }
}

async function seedLocationHierarchy() {
  const hierarchy = [
    {
      id: 'kisoro',
      label: 'Kisoro',
      sub_counties: [
        {
          id: 'buskimbiri',
          label: 'Nkuringo T/C',
          parishes: ['nteeko', 'murore', 'kikobero', 'kahurire_a', 'kahurire_b'],
        },
        {
          id: 'nyabweishenya',
          label: 'Nyanamo T/C',
          parishes: ['rugongwe'],
        },
        {
          id: 'rubuguri_town_council',
          label: 'Rubuguri T/C',
          parishes: ['rushaaga', 'kashija', 'nyabaremura', 'nombe'],
        },
      ],
    },
    {
      id: 'kiruhura',
      label: 'Isingiro',
      sub_counties: [
        {
          id: 'rushasha',
          label: 'Rushasha',
          parishes: ['mirambiro', 'ihunga'],
        },
        {
          id: 'rugaga',
          label: 'Rugaga',
          parishes: ['kashojwa'],
        },
        {
          id: 'kabingo',
          label: 'Kabingo',
          parishes: ['kyarugaju'],
        },
        {
          id: 'rwetango',
          label: 'Rwetango',
          parishes: ['rwetango_parish'],
        },
        {
          id: 'masha',
          label: 'Masha',
          parishes: ['masha_parish'],
        },
      ],
    },
  ];

  for (const district of hierarchy) {
    await db.collection('location_hierarchy').doc(district.id).set(district);
    console.log(`Seeded location hierarchy: ${district.label}`);
  }
}

async function seedUsers(fixtures: SharedFixtures) {
  for (const r of fixtures.users) {
    let user;
    try {
      user = await auth.getUser(r.firebase_uid);
      await auth.updateUser(r.firebase_uid, {
        email: r.email,
        password: r.password,
        displayName: r.display_name,
      });
    } catch {
      user = await auth.createUser({
        uid: r.firebase_uid,
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
}

async function seedPois(parks: ParkFixture[]) {
  const poiTemplates: Record<string, Array<{ name: string; type: string; latOffset: number; lngOffset: number }>> = {
    'bwindi-impenetrable': [
      { name: 'Mubare Gorilla Group', type: 'wildlife', latOffset: -0.01, lngOffset: 0.01 },
      { name: 'Buhoma Park Office', type: 'office', latOffset: -0.005, lngOffset: 0.005 },
    ],
    'mgahinga-gorilla': [
      { name: 'Nyakagezi Gorilla Group', type: 'wildlife', latOffset: 0.01, lngOffset: -0.01 },
      { name: 'Ntebeko Visitor Centre', type: 'office', latOffset: 0.005, lngOffset: 0.005 },
    ],
    'queen-elizabeth': [
      { name: 'Kazinga Channel Viewpoint', type: 'viewpoint', latOffset: 0.02, lngOffset: 0.01 },
      { name: 'Mweya Park HQ', type: 'office', latOffset: -0.01, lngOffset: -0.01 },
    ],
    'lake-mburo': [
      { name: 'Rwonyo Park HQ', type: 'office', latOffset: 0.01, lngOffset: 0.01 },
      { name: 'Lake Mburo Viewpoint', type: 'viewpoint', latOffset: -0.02, lngOffset: 0.02 },
    ],
  };

  for (const park of parks) {
    const [baseLat, baseLng] = PARK_COORDS[park.firestore_id] ?? [0, 32];
    const pois = poiTemplates[park.firestore_id] ?? [
      { name: `${park.mysql_name} HQ`, type: 'office', latOffset: 0, lngOffset: 0 },
      { name: `${park.mysql_name} Gate`, type: 'gate', latOffset: 0.01, lngOffset: 0.01 },
    ];

    for (const poi of pois) {
      await db.collection('pois').add({
        name: poi.name,
        type: poi.type,
        park_id: park.firestore_id,
        location: new admin.firestore.GeoPoint(baseLat + poi.latOffset, baseLng + poi.lngOffset),
      });
    }
  }

  console.log('Seeded POIs for all parks');
}

async function seedIncidents(parks: ParkFixture[], fixtures: SharedFixtures) {
  const reporter = fixtures.users.find((u) => u.firebase_role === 'public') ?? fixtures.users[0];
  const incidentTypes = ['crop damage', 'livestock loss', 'property damage', 'sighting', 'human injury'];
  const statuses = ['open', 'assigned', 'in_progress', 'resolved'];

  const locationByDistrict: Record<string, { district: string; sub_county: string; parish: string; village: string }[]> = {
    Kisoro: [
      { district: 'kisoro', sub_county: 'buskimbiri', parish: 'nteeko', village: 'Nteeko' },
      { district: 'kisoro', sub_county: 'rubuguri_town_council', parish: 'rushaaga', village: 'Rushaaga' },
    ],
    Kiruhura: [
      { district: 'kiruhura', sub_county: 'rushasha', parish: 'mirambiro', village: 'Mirambiro' },
      { district: 'kiruhura', sub_county: 'rugaga', parish: 'kashojwa', village: 'Kashojwa' },
    ],
  };

  for (const park of parks) {
    const [baseLat, baseLng] = PARK_COORDS[park.firestore_id] ?? [0, 32];
    const locations = locationByDistrict[park.district] ?? [
      {
        district: park.district.toLowerCase(),
        sub_county: 'central',
        parish: 'central_parish',
        village: 'Central Village',
      },
    ];

    const count = 5 + (parks.indexOf(park) % 4);

    for (let i = 0; i < count; i++) {
      const loc = locations[i % locations.length];
      const docId = `seed-${park.firestore_id}-incident-${i + 1}`;

      await db.collection('incidents').doc(docId).set({
        type: incidentTypes[i % incidentTypes.length],
        status: statuses[i % statuses.length],
        park: park.firestore_id,
        park_id: park.firestore_id,
        district: loc.district,
        sub_county: loc.sub_county,
        subCounty: loc.sub_county,
        parish: loc.parish,
        community: loc.village,
        village: loc.village,
        summary: `Sample incident ${i + 1} near ${loc.village} (${park.mysql_name})`,
        description: `Sample incident ${i + 1} near ${loc.village} (${park.mysql_name})`,
        lat: baseLat + (i * 0.008) - 0.02,
        lng: baseLng + (i * 0.008) - 0.02,
        latitude: baseLat + (i * 0.008) - 0.02,
        longitude: baseLng + (i * 0.008) - 0.02,
        userId: reporter.firebase_uid,
        userName: reporter.display_name,
        userEmail: reporter.email,
        reportedAt: admin.firestore.FieldValue.serverTimestamp(),
        source_system: 'firestore',
      });
    }

    console.log(`Seeded ${count} incidents for ${park.mysql_name}`);
  }
}

async function seed() {
  console.log('Starting seed process (shared fixtures)...');
  const fixtures = loadFixtures();
  const parks = fixtures.parks ?? [fixtures.park];

  await seedParks(parks);
  await seedLocationHierarchy();
  await seedUsers(fixtures);
  await seedPois(parks);
  await seedIncidents(parks, fixtures);

  console.log('Seed process completed successfully!');
}

seed().catch(console.error);
