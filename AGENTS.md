# Wildlife park management platform — development & integration plan

*Working assumption: a wildlife-park conservation platform in the style of a Uganda Wildlife Authority deployment (rangers, wardens, UWA officials, tourists/residents). Swap the domain nouns and the architecture holds for any dual-role, dual-app, offline-map-heavy system.*

## Contents

1. Assumptions & how to read this
2. Recommended free-tier stack
3. Roles & permissions
4. Architecture at a glance
5. Shared data model
6. Track A — Backend & shared services
7. Track B — Mobile app (Flutter)
8. Track C — Web app (React)
9. Integration plan
10. Free-tier ceilings & upgrade triggers
11. Risks & practical notes
12. Suggested repo layout

---

## 1. Assumptions & how to read this

- **Domain**: rangers doing patrols and incident reporting; tourists/residents/anonymous visitors doing park navigation and sighting reports; wardens and UWA officials doing oversight. Everything below is written against this concrete example so it's actionable rather than abstract.
- **"Native mobile app"** is read as one Flutter codebase for iOS + Android, not two separate native codebases. If you actually want fully separate Swift/Kotlin apps, every phase in Track B still applies — it just roughly doubles that track's effort and splits it into two parallel sub-tracks.
- **Roles**: mobile has *Ranger* and *Public* (covering tourist/resident/anonymous — grouped since the brief put them at one permission level); web has *Warden* and *UWA Official*.
- **State management** isn't actually a hosted "platform" with a free tier like your other choices — it's a library (Riverpod, Zustand) that's free at any scale. Included in the stack table for completeness only.
- **Free-tier figures** below were verified in July 2026 and change often, sometimes without notice (see §10 and §11). Re-verify anything load-bearing before committing engineering time to a specific number.

## 2. Recommended free-tier stack

| Layer | Choice                                                                                                  | Free-tier reality (verified July 2026) |
|---|---------------------------------------------------------------------------------------------------------|---|
| Mobile framework | Kotlin (Jetpak Compose)                                                                                 | Open source, no cost at any scale |
| Web framework | React + Vite + TypeScript                                                                               | Open source, no cost |
| Mobile state management | Riverpod                                                                                                | Library, free — not a hosted service |
| Web state management | Zustand + TanStack Query                                                                                | Library, free |
| Primary database | **Firebase Firestore**                                                                                  | 1 GiB stored data, 50,000 reads/day, 20,000 writes/day, 20,000 deletes/day, no time limit, no card required |
| Authentication | **Firebase Authentication**                                                                             | Free/unbounded for email+password and most OAuth; 50,000 MAU free; anonymous auth (covers the unregistered "Public" tier) is free; phone/SMS auth free for the first 10,000 verifications/month, then billed |
| User-generated file storage | Cloudinary                                                                                              | Free allotment for new Spark projects — confirm at setup, this has changed more than once |
| Map tiles & other large static assets | **Cloudflare R2**                                                                                       | 10 GB storage, 1M write ops/month, 10M read ops/month, **zero egress fees, permanently** — the right home for repeatedly-downloaded map packages |
| Map rendering | **MapLibre GL** — `maplibre_gl` Flutter plugin (wraps MapLibre Native) on mobile, MapLibre GL JS on web | Open source, no usage cap, actively maintained — offline-region handling and PMTiles support both shipped updates in 2026 |
| Offline map packaging | **Protomaps (PMTiles format)**                                                                          | Free, self-hosted static files — a whole park's map is one file a phone downloads and reads locally, no tile server required |
| Routing / navigation engine | Valhalla or GraphHopper, self-hosted                                                                    | Free — it's your own server, not a metered API |
| Push notifications | Firebase Cloud Messaging                                                                                | Free, unlimited, no card |
| Custom HTTP backend logic | **Cloudflare Workers**                                                                                  | 100,000 requests/day free, no card |
| Firestore-triggered logic (on-write functions) | Firebase Cloud Functions                                                                                | Technically requires enabling the Blaze (pay-as-you-go) plan, but Blaze includes the same ~2M-invocations/month free quota Spark has — cost stays $0 unless you exceed it. You will need to add a card. |
| Web app hosting | **Cloudflare Pages**                                                                                    | Unlimited bandwidth, 500 builds/month, commercial use explicitly allowed, no card |
| Self-hosted compute (routing engine, tile fallback) | **Oracle Cloud "Always Free" Ampere A1**                                                                | Cut from 4 OCPU/24GB RAM to 2 OCPU/12GB RAM in June 2026 with no advance notice — still workable for this workload; budget for possible further tightening and regional capacity limits |
| CI/CD | GitHub Actions                                                                                          | 2,000 free minutes/month on private repos, unlimited on public |
| Crash & error monitoring | Firebase Crashlytics (mobile) + Sentry free tier (web)                                                  | Both free at this scale |

**Avoid, for this specific project:**

- **MongoDB Realm / Atlas Device Sync** — fully retired since September 2025. Older tutorials still recommend it; don't build on it.
- **Railway** as a "free" host — its free tier is now $1–5/month minimum after a 30-day trial, and requires a card.
- **Vercel Hobby** for the web app — free, but restricted to personal, non-commercial use and actively enforced. Cloudflare Pages has no such restriction.
- **Supabase as the primary database** — a fine product, but free-tier projects auto-pause after 7 days without activity. Acceptable for a staging environment; risky for something a ranger might reach for unpredictably in the field.

## 3. Roles & permissions

| Capability                                        | Ranger (mobile) | Public (mobile) | Warden (web) | UWA Official (web) |
|---------------------------------------------------|--|--|---|---|
| Browse public map layers                          | ✓ | ✓ | ✓ | ✓ |
| View restricted-zone layers                       | ✓ | – | ✓ (own park) | ✓ (all parks) |
| Download offline park maps                        | ✓ | ✓ | – (assumes connectivity) | – |
| Log patrol routes                                 | ✓ | – | – | – |
| File incident reports (photo/GPS, offline-queued) | ✓ | ✓ | – | – |
| Submit wildlife sightings                         | ✓ | ✓ | – | – |
| SOS / emergency alert                             | – | ✓ | – | – |
| Review & triage incidents                         | – | – | ✓ (own park) | ✓ (all parks) |
| Assign ranger tasks                               | – | – | ✓ | – |
| Edit zones / POIs / trails                        | ✓ | – | ✓ (own park) | ✓ (all parks) |
| Manage warden accounts                            | – | – | – | ✓ |
| Onboard a new park                                | – | – | – | ✓ |
| Cross-park analytics & export                     | – | – | – | ✓ |
| View Feed and news bulletin                       | – | ✓ | – | ✓ |


**Implementation**: Firebase custom claims (`role: "ranger" | "public" | "warden" | "uwa_official"`, plus `park_id` for rangers/wardens) set via a Cloud Function when an account is created or promoted, then read directly inside Firestore Security Rules. Fragment (wrapped in the standard `service cloud.firestore` block in the real file):

```
match /incidents/{incidentId} {
  allow read:   if request.auth.token.role in ['warden','uwa_official']
                || request.auth.uid == resource.data.reporterUid;
  allow create: if request.auth.token.role in ['ranger','public'];
  allow update: if request.auth.token.role in ['warden','uwa_official'];
}
```

## 4. Architecture at a glance

Both apps are thin clients: neither holds business logic the other depends on. They talk to the same shared services — Firebase Auth, Firestore, map storage (R2 + PMTiles), and FCM — plus a small self-hosted routing service. That's what makes the three tracks below genuinely independent: nothing in Track B or C is blocked on the other finishing, only on Track A publishing a stable local emulator + schema early.

## 5. Shared data model

| Collection | Key fields | Written by | Read by |
|---|---|---|---|
| `users` | uid, role, park_id, name, contact | Self / admin | All (own record); Warden/UWA (roster) |
| `parks` | id, name, boundary geojson, tile_package_url, routing_graph_url | UWA Official | All |
| `zones` | id, park_id, type (restricted/patrol/safe), boundary geojson | Warden | Ranger, Warden, UWA |
| `incidents` | id, reporter_uid, park_id, location, type, media_urls[], status, created_at, synced_at | Ranger, Public | Warden, UWA |
| `patrol_logs` | id, ranger_uid, park_id, route_points[], start/end time | Ranger | Warden, UWA |
| `sightings` | id, reporter_uid, species, location, media_url, created_at | Public | Warden, UWA |
| `pois` | id, park_id, type, name, location | Warden | All |
| `tasks` | id, assigned_ranger_uid, assigned_by, description, status | Warden | Ranger, Warden |
| `notifications` | id, target_uid/topic, message, read | Cloud Function | Target user |

## 6. Track A — Backend & shared services

No UI of its own — the foundation the other two tracks build against. Start this first and stay a phase or two ahead of B and C.

| Phase | Work | Exit criteria |
|---|---|---|
| A1 | Firebase project ×3 (dev/staging/prod); Firestore collections + composite indexes; security-rule skeleton | Rules deployed, empty collections queryable |
| A2 | RBAC: custom-claims Cloud Function; per-collection rules per role (§3) | Unit tests confirm each role can/can't do what §3 says |
| A3 | Storage buckets + rules; R2 bucket + CORS config for PMTiles range requests | A test file round-trips through both |
| A4 | Map pipeline: OSM extract → tilemaker/planetiler → PMTiles per park → upload to R2, versioned per park | One real park's PMTiles file downloadable end-to-end |
| A5 | Routing: Valhalla or GraphHopper on Oracle Cloud; trail graph built from OSM path data per park | A sample route returns for that one park |
| A6 | FCM topics (per park, per role) + Cloud Functions for the obvious triggers (incident created → notify warden; task assigned → notify ranger) | A test write fires a real push notification |
| A7 | Publish the **Firebase Local Emulator Suite** config + seed data for Tracks B and C | Both frontend teams can run the full stack locally with zero cloud dependency |

## 7. Track B — Mobile app (Flutter)

| Phase | Work |
|---|---|
| B1 | Scaffolding, design system, role-aware navigation shell chosen at login/anonymous entry |
| B2 | Offline-first data layer: local DB (Drift or Isar) as source of truth; outbox pattern — writes land locally and queue immediately, a background worker syncs to Firestore when connectivity returns, Firestore listeners pull remote changes back down |
| B3 | Map integration: `maplibre_gl`, PMTiles offline-region download manager (progress UI, storage housekeeping, "download this park" flow), toggleable layers for trails/zones/POIs |
| B4 | Ranger flows: GPS breadcrumb patrol logging (fully offline), incident reporting via long-press-to-drop-a-pin plus offline-queued photo/video, task inbox |
| B5 | Public flows: anonymous-first entry (Firebase anonymous auth, optional upgrade to a full account), park map download + POI browsing, tap-to-report sightings, SOS button |
| B6 | Navigation, tiered: bearing/distance-to-point for off-trail cases, trail-graph routing where a path exists, full turn-by-turn only on the handful of roads that have one |
| B7 | Push notification wiring; build and test entirely against Track A's emulator + sample PMTiles — **no live cloud dependency yet** |

**Exit criteria**: offline queue survives an app kill/restart; a ranger can complete an entire patrol and incident report in airplane mode and have it sync correctly once reconnected.

## 8. Track C — Web app (React)

| Phase | Work |
|---|---|
| C1 | Scaffolding (Vite + TS + Tailwind), routing, auth-guard shell for Warden vs UWA Official |
| C2 | Auth flows, protected routes, role-aware navigation |
| C3 | Map integration: MapLibre GL JS, live incident/ranger markers via Firestore listeners, marker clustering, incident-density heatmap layer, draw tools for zone editing, toggleable layers (trails, zones, POIs, ranger positions) |
| C4 | Warden dashboard: ranger roster + last-known location, incident triage queue, task assignment, park-level POI/zone editor |
| C5 | UWA Official dashboard: cross-park analytics (trend charts, incident heatmaps), warden account management, "add a new park" flow that kicks off Track A's tile pipeline, report export |
| C6 | Notification center, real-time polish |
| C7 | Build and test against Track A's emulator + the same seed data Track B uses — **no live cloud dependency yet** |

## 9. Integration plan

| Phase | Work |
|---|---|
| I1 | Point both apps at a real staging Firebase project (swap config only; prod stays untouched) |
| I2 | Deploy real security rules + a small real dataset (1–2 parks) to staging R2 and the routing instance |
| I3 | Cross-role scenarios end to end: ranger logs an incident offline → comes back online → warden sees it within seconds → warden assigns a follow-up → ranger gets the push; tourist submits a sighting → appears in the warden's queue |
| I4 | Offline/online transition testing: deliberately create concurrent/conflicting writes and confirm the resolution behavior is acceptable (last-write-wins is fine for status fields; confirm logs/incidents are append-only so nothing is silently dropped) |
| I5 | Load testing against the free-tier ceilings in §10 — script realistic concurrent usage and watch your Firestore/R2 counters, so you know your actual runway before anything costs money |
| I6 | Security review: attempt role escalation (can Public write to `incidents.status`? can a Warden from Park A see Park B's ranger locations?) and confirm rules deny by default |
| I7 | Staged rollout: internal dogfood with real rangers/wardens (2–4 weeks) → limited public beta in one park → full multi-park launch |
| I8 | Post-launch: Crashlytics + Sentry + a simple usage dashboard, feedback loop, quota alerts set before you hit a ceiling rather than after |

## 10. Free-tier ceilings & upgrade triggers

| Service | Free ceiling | What happens at the ceiling | Cheapest next step |
|---|---|---|---|
| Firestore (Spark) | 1 GiB data, 50K reads / 20K writes / 20K deletes per day | Requests beyond the daily quota fail until reset | Enable Blaze — pay-as-you-go only on the overage |
| Firebase Auth | 50K MAU free (email/social); phone auth free to 10K verifications/mo | Billed per verification beyond 10K | Usually a non-issue unless SMS auth is heavily used |
| Cloudflare R2 | 10 GB storage, 1M writes, 10M reads/month | Billed per GB/op beyond free tier — egress stays $0 regardless | $0.015/GB storage beyond 10GB; cheap even at scale |
| Cloudflare Workers | 100K requests/day | Requests beyond that fail | $5/month for 10M requests/month |
| Oracle Cloud Always Free | 2 OCPU / 12 GB RAM (cut from 4/24 in June 2026) | New resource creation blocked past the cap; existing instances at risk if ever terminated | Paid Ampere compute, or a small paid VPS for routing |
| Cloudflare Pages | 500 builds/month, unlimited bandwidth | Builds beyond 500/month queue until next cycle | $5/month Pro tier: 5,000 builds |
| GitHub Actions | 2,000 min/month (private repos) | Jobs stop running until next billing cycle | Pay-per-minute beyond included minutes |
| Mapbox (if used instead of MapLibre) | 50K web loads/month, 25K MAU mobile, 100K geocoding/directions | No hard cap — bills automatically past the free allowance | Budget explicitly if you go this route |

## 11. Risks & practical notes

- **Connectivity is the default assumption, not the exception.** Every mobile flow should work with zero signal for days at a time; sync is something that happens eventually, never something a screen waits on.
- **OSM attribution** is required wherever OpenStreetMap-derived data is used — a visible "© OpenStreetMap contributors" line satisfies it.
- **App store fees are real and outside the "free tier" scope**: Google Play is a $25 one-time fee, Apple's Developer Program is $99/year. Firebase App Distribution is free for internal testing before you pay for either.
- **Cloud Functions need a credit card even to stay free.** If avoiding that entirely matters, lean harder on Cloudflare Workers and accept that Firestore-triggered logic (e.g. "on incident created") is more awkward to replicate there.
- **Load balancing, in the literal sense, mostly isn't something you need to build.** Firebase and Cloudflare absorb this invisibly for the managed pieces. The only piece you run yourself — routing/tiles on Oracle Cloud — sits behind Cloudflare's free CDN, which caches away most repeat requests. If that one self-hosted piece ever needs more than one instance, that's the point the free tier stops covering everything, not a problem to engineer around for free.
- **Free tiers move, sometimes without notice.** Oracle cut its compute allowance in half mid-2026 with no announcement, Firebase has changed its storage/hosting pricing more than once, and Render's free web services now sleep after 15 minutes instead of 30. Re-check §10 periodically rather than assuming it's stable.

## 12. Suggested repo layout

Three repositories, matching the three independent tracks:

```
park-backend/       # Track A: Firebase config, security rules, Cloud Functions,
                     # map/routing pipeline scripts, emulator seed data
park-mobile/        # Track B: Flutter app
park-web/           # Track C: React app
park-contracts/     # shared: Firestore schema doc + generated TypeScript/Dart
                     # types, so B and C never silently drift from A
```

`park-contracts` is what actually lets the three tracks stay independent without drifting apart — treat changes to it as requiring sign-off from whichever track didn't propose them.

---

Happy to go deeper on any single phase — Firestore security rules, the offline sync/outbox implementation, and the PMTiles build pipeline are the three that usually need the most hands-on detail once you start building.