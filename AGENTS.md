# Wildlife park management platform — Development & Integration Plan

*Working assumption: a wildlife-park conservation platform in the style of a Uganda Wildlife Authority deployment (rangers, community residents, tourists). This plan focuses on the Kotlin-native Android implementation and its supporting Firebase backend.*

## Contents

1. Project Intent
2. Current Tech Stack
3. Roles & Permissions
4. Architecture at a Glance
5. Shared Data Model
6. Track A — Backend & Shared Services
7. Track B — Mobile App (Android Native)
8. Track C — Web App (Future Oversight)
9. Integration & Verification
10. Free-tier Ceilings & Practical Notes

---

## 1. Project Intent

- **Domain**: Rangers doing patrols and incident reporting; community members/tourists doing sightings and conflict reports.
- **Native Android App**: Built using modern Android standards (Jetpack Compose, Hilt, Room, Coroutines).
- **Roles**: Mobile has *Ranger* and *Public* (including Guest mode). Web (future) handles *Warden* and *UWA Official*.

## 2. Current Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| **Mobile Framework** | Kotlin + Jetpack Compose | Modern UI toolkit, no cost at scale |
| **Dependency Injection** | Hilt | Standard Android DI |
| **State Management** | StateFlow + ViewModel | Reactive, lifecycle-aware architecture |
| **Local Database** | Room | Offline-first source of truth with background sync |
| **Primary Database** | Firebase Firestore | Real-time, scalable NoSQL |
| **Authentication** | Firebase Auth | Email/Pass, Google, and Anonymous (Guest) support |
| **Maps** | Mapbox Maps SDK | High-performance vector maps with offline support |
| **Push Notifications** | Firebase Cloud Messaging | Pending wiring for Guest topic-based alerts |
| **Backend Logic** | Cloud Functions (Node.js/TS) | RBAC, on-write triggers, and data validation |

## 3. Roles & Permissions

| Capability | Ranger (mobile) | Public (mobile) |
|---|--|--|
| Browse public map layers | ✓ | ✓ |
| View restricted-zone layers | ✓ | – |
| Log patrol routes | ✓ | – |
| File incident reports (GPS, offline-queued) | ✓ | ✓ |
| Submit wildlife sightings | ✓ | ✓ |
| Manage Profile & Settings | ✓ | ✓ (Restricted for Guests) |
| Receive high-priority alerts | ✓ | ✓ |

**Implementation**: Firebase custom claims (`role: "ranger" | "public"`) set via a Cloud Function, read directly inside Firestore Security Rules.

---

## 4. Architecture at a Glance

The app follows an **Offline-First Outbox Pattern**:
1. **Local Writes**: User actions (reports, logs) are saved immediately to **Room**.
2. **Background Sync**: A background worker (WorkManager) pushes pending data to **Firestore** when connectivity returns.
3. **Remote Listeners**: Firestore snapshots pull down community updates and alerts in real-time.
4. **Auth Layer**: `AuthRepository` manages the user state and maps Firebase users to our domain `User` model, including `isGuest` identification.

## 5. Shared Data Model

| Collection | Key Fields | Written by | Read by |
|---|---|---|---|
| `users` | uid, role, park_id, name, contact | Self / Admin | All (own), Admin (all) |
| `parks` | id, name, boundary, tile_urls | Admin | All |
| `incidents` | reporter_uid, type, location, status, photos | Ranger, Public | All |
| `patrol_logs` | ranger_uid, route_points, start/end | Ranger | Warden |
| `notifications` | target_uid, message, time, type | System | Target User |

---

## 6. Track A — Backend & Shared Services

**These checklists were left stale for a long time — A3/A4/A5 (and B4/B5/B7 below) were already done well before this update, and this file just never got synced forward. Fixed 2026-08-13; treat the top-level `AGENTS.md`/`REPOS.md`/`BRIDGE-CONTRACT.md` as the source of truth for current status over this file going forward.**

| Phase | Work | Status |
|---|---|---|
| **A1** | Project initialization & Firestore collection skeleton | [x] |
| **A2** | **RBAC**: `onUserCreated` trigger & `setUserRole` callable | [x] (Spark plan can't run this today — see `../HOSTED-CUTOVER-PLAN.md` §"Still genuinely open") |
| **A3** | Storage rules & bucket configuration for incident media | [x] — `android-native-backend-branch/storage.rules`, includes a `feed/` rule added 2026-08-13 for feed-article images |
| **A4** | Map tile hosting & offline region distribution | [x] — Mapbox `TileStore`/`OfflineManager`, see `../BRIDGE-CONTRACT.md`'s "Offline map tiles" section |
| **A5** | FCM Topics: Park-based and role-based notification channels | [x] — `FcmTopicManager`, see §11 below |

## 7. Track B — Mobile App (Android)

| Phase | Work | Status |
|---|---|---|
| **B1** | Navigation scaffolding & Instagram-style UI implementation | [x] |
| **B2** | **Offline-First**: Room implementation with `SyncStatus` tracking | [x] |
| **B3** | **Auth Integration**: Support for Ranger/Public/Guest with persistent settings | [x] — Guest ("Continue without account") no longer blocks on connectivity as of 2026-08-13, see `README.md`'s Capabilities section |
| **B4** | **Incident Reporting**: Sighting/Conflict forms with Camera/GPS integration | [x] |
| **B5** | **Ranger Tracking**: Background location breadcrumbs for patrols | [x] — a real crash on this exact screen (Mapbox plugin-registry timing race in the scale-bar/logo/attribution setup, not the map itself) was found and fixed 2026-08-13, confirmed live via a real device's crash log |
| **B6** | **Permissions**: Custom `PermissionDialog` (Instagram-style) integration | [x] |
| **B7** | **Push Notifications**: Topic-based messaging for all user types (e.g., `park_alerts_all` for Guests) | [x] |

---

## 8. Track C — Web App (Laravel & TanStack)

- **Warden Dashboard**: lives at the sibling `../web-portal/` repo (`backend/` = Laravel, `frontend/` = TanStack Start) — the path this section originally pointed at (`android-native-webaportal`) no longer exists, corrected 2026-08-13.
- **Current Status**: live and deployed (Render + Cloudflare, see `../HOSTED-CUTOVER-PLAN.md`), well past "UI foundation" — includes incident/claims/personnel management and, as of 2026-08-13, a `/portal/feed` screen for composing the mobile app's community feed.
- **Warden Dashboard**: Roster management, incident triage, and task assignment.
- **UWA Official**: Cross-park analytics, map data management, and Warden account oversight.

---

## 9. Integration & Verification

- **Emulator Testing**: All features are validated against the Firebase Local Emulator Suite.
- **Performance**: Zero-signal simulation for outbox verification.
- **Theming**: High-fidelity Light and Dark mode (True Black) support.

---

## 10. Practical Notes

- **Guest Notifications**: Topic-based messaging (e.g., `park_alerts_all`) will allow Guests to receive info without being logged in.
- **Media Optimization**: a CDN (Cloudinary/R2) was considered but explicitly deferred (decision made 2026-08-12, see `../BRIDGE-CONTRACT.md`'s "Storage (incident media)" section) — current volume is a small fraction of Firebase Storage's free tier and no CDN credentials exist yet. Revisit once real usage data shows the free tier is actually being approached, not preemptively.
- **Ranger Login**: Use `ranger@wildwatch.app` (pw: `password123`) in development to access professional features (see `android-native-backend-branch/scripts/seed.ts`).

## 11. Local-first Docker development

The local Docker stack this section used to point at has been retired (2026-08-12) — see `../HOSTED-CUTOVER-PLAN.md` for the hosted-services replacement and workspace `../REPOS.md` for the current repo map. Firebase's own emulator suite can still be run directly from `android-native-backend-branch/` if a local backend target is needed.

- `USE_LOCAL_BACKEND` + `LOCAL_BACKEND_HOST` in `local.properties` → `BuildConfig` → `FirebaseModule.kt` emulator wiring.
- Never hardcode emulator hosts/ports outside that gated path.
- Repository-interface + Hilt-binding pattern; no Firebase SDK calls from ViewModels/Composables.
- **FCM topics** (via `FcmTopicManager`): after login/token refresh, subscribe `park_alerts_{parkId}` + role topic (`park_alerts_all`, `ranger_{parkId}`, `warden_{parkId}`, `uwa_official`); unsubscribe previous topics first.
- **Community feed:** portal's `/portal/feed` screen writes Firestore `feed/{id}` (including an optional header image as of 2026-08-13); mobile reads via `ArticleRepositoryImpl` (Room cache + Firestore listener) → `FeedScreen`'s staggered image grid. Full field mapping: `../BRIDGE-CONTRACT.md`.
- **Bridge:** mobile data authoritative in Firestore; portal relational data in Laravel/Postgres; `source_system` prevents echo loops.
- **CI/CD:** `.github/workflows/release.yml` builds and releases a debug APK on every push to `master` — see `README.md`'s CI/CD section for the required secrets and the Google Sign-In caveat.