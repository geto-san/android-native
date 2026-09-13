// Generates app/src/main/assets/parks.geojson — the offline fallback park-boundary
// overlay for the ranger tracking map.
//
// Mirrors android-native-backend-branch/scripts/seed.ts's seedParks() exactly: same
// park firestore_ids, same PARK_COORDS centroids, and the same ±0.1°/±0.05° boundary
// rectangle formula. Keeping this script in lockstep with seed.ts means the bundled
// boundary renders offline identically to the Firestore boundary_geojson the app would
// otherwise read from the network — so a ranger opening RangerTracking in the field
// without connectivity still sees their park outline on the first frame.
//
// Run: node scripts/generate_parks_geojson.mjs

import { writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

// Duplicated from seed.ts on purpose: the generator must keep working even when the
// sibling backend-branch's fixtures aren't checked out on this machine.
const PARK_COORDS = {
  "bwindi-impenetrable": [-1.05, 29.7],
  "mgahinga-gorilla": [-1.37, 29.65],
  "queen-elizabeth": [-0.2, 30.0],
  "murchison-falls": [2.27, 31.77],
  kibale: [0.5, 30.4],
  semuliki: [0.85, 30.1],
  "rwenzori-mountains": [0.38, 29.98],
  "lake-mburo": [-0.61, 30.97],
  "kidepo-valley": [3.92, 33.86],
  "mount-elgon": [1.12, 34.17],
};

// Same boundary ring the seed script writes into Firestore's boundary_geojson.
function boundaryRingFor([lat, lng]) {
  return [
    [lng - 0.1, lat - 0.05],
    [lng + 0.1, lat - 0.05],
    [lng + 0.1, lat + 0.05],
    [lng - 0.1, lat + 0.05],
    [lng - 0.1, lat - 0.05],
  ];
}

const features = Object.entries(PARK_COORDS).map(([id, coords]) => ({
  type: "Feature",
  geometry: {
    type: "Polygon",
    coordinates: [boundaryRingFor(coords)],
  },
  properties: { id },
}));

const featureCollection = {
  type: "FeatureCollection",
  features,
};

const outPath = resolve(
  dirname(fileURLToPath(import.meta.url)),
  "../app/src/main/assets/parks.geojson",
);
mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, JSON.stringify(featureCollection, null, 2) + "\n");

console.log(`Wrote ${features.length} park boundaries to ${outPath}`);