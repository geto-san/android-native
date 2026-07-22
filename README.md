# SilverBack Sentry — Native Android (Kotlin + Compose)

Native Android rewrite of the SilverBack Sentry Expo app, built to full feature parity
before the team cuts over. See the full migration plan and architecture guardrails at
the repo root (or ask for a copy of `imperative-whistling-engelbart.md` — the plan this
project is being built against). The Expo app at the repo root is untouched by this
project and keeps serving rangers in the meantime; this is a fully self-contained
Gradle project with its own build, own `.gitignore`, own tests.

## Required one-time setup: real `google-services.json`

`app/google-services.json` in this project is a **placeholder**. Its `project_number`,
`project_id`, and `storage_bucket` are the real values for the `silverback-sentry-c6727`
Firebase project (matching the Expo app's `firebaseConfig.js`), but `mobilesdk_app_id`
and `current_key` are fake — the build will succeed, but any real Firebase call
(auth/Firestore/Storage) will fail until you replace this file.

To fix it:
1. Open the [Firebase console](https://console.firebase.google.com) for project
   `silverback-sentry-c6727`.
2. Under Project Settings, check whether an Android app is already registered for
   package `com.silverback.sentry` (the same `applicationId`/package this project and
   the Expo app's Android build both use). If not, register one.
3. Download the real `google-services.json` and replace `app/google-services.json`
   with it.

Reusing the same package name here as the Expo app means both apps can share one
Firebase Android app registration and read/write the exact same Firestore/Storage data
during the transition — this is intentional, not a coincidence.

## Build & run

```bash
./gradlew :app:assembleDebug      # compile
./gradlew :app:testDebugUnitTest  # fast JVM unit tests
./gradlew :app:connectedDebugAndroidTest  # instrumented tests (needs an emulator/device)
./gradlew :app:ktlintCheck :app:detekt    # static analysis
```

Or open the `android-native/` folder directly in Android Studio.

## Architecture guardrails

These are non-negotiable structural rules, not just style preferences — each one
exists because the equivalent Expo/React Native code *didn't* enforce it and that's
exactly where it broke (silent data loss, duplicate Firestore uploads, orphaned sync
paths, a cross-account biometric leak). Full detail is in the migration plan; summary:

- **G1** — `ObservationRepositoryImpl` is the only class that touches Firestore/Storage
  for observations. No ViewModel, Composable, or Worker does it directly.
- **G2** — Room is the single source of truth. UI always reads through the
  repository's `Flow`; Firestore snapshots are merged into it, never read standalone.
- **G3** — Sync (`ObservationRepository.syncPending()`) is one atomic, single-writer
  function: mark `SYNCING` → upload → on success flip to `SYNCED` in one transaction,
  on failure revert to `PENDING`. Called from exactly three triggers, never a second
  parallel implementation.
- **G4** — A client-generated UUID is both the Room primary key and the Firestore
  document ID, set before any network call.
- **G5** — "Skip my own echo" is scoped to a short-lived ID set (forgotten after the
  first confirming snapshot), never to author-comparison — so a teammate's update to
  my own observation is never suppressed.
- **G6** — Biometric-lock preference is stored per signed-in user (DataStore key
  namespaced by `uid`), never as one device-global flag.
- **G7** — UI never talks to Firebase/Room/Location/Camera APIs directly; only
  `data/` layer classes do, which is what keeps the sync logic unit-testable.

## Firestore/Storage shape (must stay compatible with the Expo app)

- `observations` collection (flat, **not** a per-user subcollection): `gorillaGroup`,
  `location` ("lat, lng" string), `locationName`, `healthStatus`, `notes`, `userName`,
  `userEmail`, `userId`, `createdAt` (ISO string), `synced` (bool), `syncedAt` (ISO
  string), `status` (`pending`/`attended`), `attendedAt`, `attendedBy`,
  `attendedByName`, `hasImages` (bool), `imageCount` (number), `imageUrls` (array of
  Storage download URLs).
- Storage path: `observations/{observationId}/{n}_{timestamp}.jpg`.
- `messages` collection: `text`, `userId`, `userName`, `createdAt`, `timestamp`
  (server timestamp).

## Status

Phase 0 (project scaffolding) — Compose + Hilt + KSP + Room + WorkManager + Firebase
+ ktlint/detekt all wired into the Gradle build; app builds and shows a themed empty
screen. Subsequent phases build out auth, the offline-first observation engine, and
the rest of the feature set — see the migration plan for the full phase list.
