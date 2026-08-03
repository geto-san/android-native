const admin = require('firebase-admin');

process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

admin.initializeApp({
  projectId: process.env.GCLOUD_PROJECT || 'wildwatch-82abc',
});

const db = admin.firestore();
const auth = admin.auth();

async function seed() {
  console.log('Starting seed process...');
  console.log(`Using Project ID: ${admin.app().options.projectId}`);

  // 1. Seed Parks
  const parks = [
    {
      id: 'murchison-falls',
      name: 'Murchison Falls National Park',
      location: new admin.firestore.GeoPoint(2.1875, 31.7814),
      districts: ['Nwoya', 'Buliisa', 'Kiryandongo', 'Masindi'],
      description: 'The largest national park in Uganda, known for its powerful waterfall and diverse wildlife including lions, elephants, and giraffes.'
    },
    {
      id: 'queen-elizabeth',
      name: 'Queen Elizabeth National Park',
      location: new admin.firestore.GeoPoint(-0.2, 30.0),
      districts: ['Kasese', 'Rubirizi', 'Kamwenge', 'Rukungiri', 'Kanungu'],
      description: 'Ugandas most popular savanna reserve, featuring the tree-climbing lions of Ishasha and the Kazinga Channel.'
    },
    {
      id: 'bwindi-impenetrable',
      name: 'Bwindi Impenetrable National Park',
      location: new admin.firestore.GeoPoint(-1.05, 29.7),
      districts: ['Kanungu', 'Kisoro', 'Rubanda', 'Kabale'],
      description: 'A UNESCO World Heritage site home to half of the worlds mountain gorillas.'
    },
    {
      id: 'kidepo-valley',
      name: 'Kidepo Valley National Park',
      location: new admin.firestore.GeoPoint(3.9, 33.85),
      districts: ['Kaabong', 'Karenga'],
      description: 'A rugged savanna park in the remote northeast, often cited as one of the most beautiful wilderness areas in Africa.'
    },
    {
      id: 'kibale',
      name: 'Kibale National Park',
      location: new admin.firestore.GeoPoint(0.5, 30.4),
      districts: ['Kabarole', 'Kamwenge', 'Kyenjojo', 'Kasese'],
      description: 'Best known for chimpanzee tracking and hosting one of the highest concentrations of primates in Africa.'
    },
    {
      id: 'rwenzori-mountains',
      name: 'Rwenzori Mountains National Park',
      location: new admin.firestore.GeoPoint(0.35, 29.9),
      districts: ['Kasese', 'Bundibugyo', 'Kabarole'],
      description: 'Protecting the legendary "Mountains of the Moon," featuring snow-capped peaks and unique alpine flora.'
    },
    {
      id: 'mount-elgon',
      name: 'Mount Elgon National Park',
      location: new admin.firestore.GeoPoint(1.2, 34.55),
      districts: ['Mbale', 'Sironko', 'Bulambuli', 'Bududa', 'Kapchorwa', 'Bukwo', 'Kween'],
      description: 'Centered on an extinct volcano with the largest surface area of any extinct volcano in the world.'
    },
    {
      id: 'lake-mburo',
      name: 'Lake Mburo National Park',
      location: new admin.firestore.GeoPoint(-0.6, 31.0),
      districts: ['Kiruhura', 'Mbarara', 'Isingiro', 'Lyantonde'],
      description: 'The smallest of Ugandas savanna parks, featuring ancient Precambrian metamorphic rocks and diverse acacia woodland.'
    },
    {
      id: 'semuliki',
      name: 'Semuliki National Park',
      location: new admin.firestore.GeoPoint(0.8, 30.05),
      districts: ['Bundibugyo'],
      description: 'Includes an extension of the great Ituri Forest of the Congo Basin, famous for its hot springs and central African bird species.'
    },
    {
      id: 'mgahinga-gorilla',
      name: 'Mgahinga Gorilla National Park',
      location: new admin.firestore.GeoPoint(-1.35, 29.65),
      districts: ['Kisoro'],
      description: 'Located in the clouds, this park covers the slopes of three Virunga Volcanoes and is home to mountain gorillas and golden monkeys.'
    }
  ];

  for (const park of parks) {
    await db.collection('parks').doc(park.id).set({
      ...park,
      boundary_geojson: JSON.stringify({
        type: 'Feature',
        geometry: {
          type: 'Polygon',
          coordinates: [[[park.location.longitude - 0.1, park.location.latitude + 0.1], [park.location.longitude + 0.1, park.location.latitude + 0.1], [park.location.longitude + 0.1, park.location.latitude - 0.1], [park.location.longitude - 0.1, park.location.latitude - 0.1], [park.location.longitude - 0.1, park.location.latitude + 0.1]]]
        }
      })
    });
    console.log(`Seeded park: ${park.name}`);
  }

  // 2. Seed Location Hierarchy (from all_questions.json)
  const hierarchy = [
    {
      id: 'kisoro',
      label: 'Kisoro',
      sub_counties: [
        {
          id: 'buskimbiri',
          label: 'Nkuringo T/C',
          parishes: ['Nteeko', 'Murore', 'Kikobero', 'Kahurire A', 'Kahurire B']
        },
        {
          id: 'nyabweishenya',
          label: 'Nyanamo T/C',
          parishes: ['Rugongwe']
        },
        {
          id: 'rubuguri_town_council',
          label: 'Rubuguri T/C',
          parishes: ['Rushaaga', 'Kashija', 'Nyabaremura', 'Nombe']
        }
      ]
    },
    {
      id: 'kiruhura',
      label: 'Kiruhura/Isingiro',
      sub_counties: [
        {
          id: 'rushasha',
          label: 'Rushasha',
          parishes: ['Mirambiro', 'Ihunga']
        },
        {
          id: 'rugaga',
          label: 'Rugaga',
          parishes: ['Kashojwa']
        },
        {
          id: 'kabingo',
          label: 'Kabingo',
          parishes: ['Kyarugaju', 'Kagogo']
        },
        {
          id: 'rwetango',
          label: 'Rwetango',
          parishes: ['Rwenfunjo', 'Rwenyanga', 'Rwetango']
        },
        {
          id: 'masha',
          label: 'Masha',
          parishes: ['Rukuuba']
        }
      ]
    }
  ];

  for (const h of hierarchy) {
    await db.collection('location_hierarchy').doc(h.id).set(h);
    console.log(`Seeded hierarchy for district: ${h.label}`);
  }

  // 3. Seed Users
  const roles = [
    { email: 'ranger@wildwatch.app', role: 'ranger', park_id: 'bwindi-impenetrable', name: 'John Ranger' },
    { email: 'warden@wildwatch.app', role: 'warden', park_id: 'bwindi-impenetrable', name: 'Alice Warden' },
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

  console.log('Seed process completed successfully!');
}

seed().catch(console.error);
