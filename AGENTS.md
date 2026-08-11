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

| Phase | Work | Status |
|---|---|---|
| **A1** | Project initialization & Firestore collection skeleton | [x] |
| **A2** | **RBAC**: `onUserCreated` trigger & `setUserRole` callable | [x] |
| **A3** | Storage rules & bucket configuration for incident media | [ ] |
| **A4** | Map tile hosting & offline region distribution | [ ] |
| **A5** | FCM Topics: Park-based and role-based notification channels | [ ] |

## 7. Track B — Mobile App (Android)

| Phase | Work | Status |
|---|---|---|
| **B1** | Navigation scaffolding & Instagram-style UI implementation | [x] |
| **B2** | **Offline-First**: Room implementation with `SyncStatus` tracking | [x] |
| **B3** | **Auth Integration**: Support for Ranger/Public/Guest with persistent settings | [x] |
| **B4** | **Incident Reporting**: Sighting/Conflict forms with Camera/GPS integration | [/] |
| **B5** | **Ranger Tracking**: Background location breadcrumbs for patrols | [ ] |
| **B6** | **Permissions**: Custom `PermissionDialog` (Instagram-style) integration | [x] |
| **B7** | **Push Notifications**: Topic-based messaging for all user types (e.g., `park_alerts_all` for Guests) | [ ] |

---

## 8. Track C — Web App (Laravel & TanStack)

- **Warden Dashboard**: Located at `/home/geto/Projects/Github/android-native-webaportal`. Built with Laravel (backend) and TanStack Start (frontend).
- **Current Status**: UI foundation established. Needs Firestore integration based on `docs/schema-v1.md`.
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
- **Media Optimization**: Cloudinary or R2 will be used for large media files to stay within Firebase Storage free-tier limits.
- **Ranger Login**: Use `ranger@wildwatch.app` (pw: `password123`) in development to access professional features (see `android-native-backend-branch/scripts/seed.ts`).

## 11. Local-first Docker development

See `../wildwatch-local-development-env-setup/SETUP.md` and workspace `REPOS.md`.

- `USE_LOCAL_BACKEND` + `LOCAL_BACKEND_HOST` in `local.properties` → `BuildConfig` → `FirebaseModule.kt` emulator wiring.
- Never hardcode emulator hosts/ports outside that gated path.
- Repository-interface + Hilt-binding pattern; no Firebase SDK calls from ViewModels/Composables.
- **FCM topics** (via `FcmTopicManager`): after login/token refresh, subscribe `park_alerts_{parkId}` + role topic (`park_alerts_all`, `ranger_{parkId}`, `warden_{parkId}`, `uwa_official`); unsubscribe previous topics first.
- **Community feed:** portal writes Firestore `feed/{id}`; mobile reads via `ArticleRepositoryImpl` (Room cache + Firestore listener) → `FeedScreen`.
- **Bridge:** mobile data authoritative in Firestore; portal relational data in Laravel/MySQL; `source_system` prevents echo loops.