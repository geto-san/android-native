<div align="center">
  <img src="./art/icons/ic_launcher_foreground_exact.png" width="120" height="120" alt="WildWatch Logo" />
  <h1>WildWatch</h1>
  <p>A native Android app for wildlife conservation reporting and ranger operations in Uganda.</p>
</div>

## What this is

WildWatch is an offline-first Android app serving two audiences: rangers, who use it for incident response, patrol tracking, and evidence collection, and the public, who use it to report wildlife sightings and human-wildlife conflict incidents. It is built with Kotlin and Jetpack Compose, backed by Firebase (Authentication, Firestore, Storage, Cloud Messaging), with Room providing local persistence and an offline outbox so reports and patrol data can be captured with no signal and synced automatically once connectivity returns.

This app's data reaches a separate Laravel-based web portal (`../web-portal/`) for wardens and UWA officials through a Firebase-to-Laravel bridge; the full contract for what data crosses that bridge and how is documented in `../BRIDGE-CONTRACT.md`. Which features and screens a signed-in account can reach depends on its role — ranger, warden, UWA official, or public — documented in `../REPOS.md`'s role-model write-up alongside the equivalent portal-side roles.

## Capabilities

Incident and wildlife-sighting reporting with camera capture and GPS tagging, submitted through an offline-first outbox that queues locally and syncs when connectivity allows. A ranger tracking screen showing live location, incident pins, and park points of interest, including an in-app flow for flagging new points of interest such as danger zones. Background patrol tracking that persists a ranger's route locally, syncs it periodically, and correctly resumes rather than duplicating a patrol if the app process is killed mid-patrol. Push notifications with tap-to-deep-link behavior into the relevant screen, and an unread-count badge on the home and dashboard bells. Offline map regions: the base map for a ranger's assigned park downloads automatically in the background over an unmetered connection, so the map keeps working without signal in the field. Community news feed sourced from the portal. Guest mode for reporting without creating an account, with light and dark themes including a true-black dark mode.

## Tech stack

Jetpack Compose with Material 3 for UI, an MVVM architecture, Hilt for dependency injection, Room for local persistence, Firebase (Authentication, Firestore, Storage, Cloud Messaging, Functions) for the backend, the Mapbox Maps SDK for mapping and offline tile regions, Kotlin Coroutines and Flow for asynchronous and reactive code, WorkManager for background sync and downloads, and Timber for logging.

## Running against a real Firebase project

This is now the intended default. Add a real `google-services.json` for the target Firebase project to the `app/` directory, and leave `USE_LOCAL_BACKEND` unset or set to false in `local.properties` so the app connects to real Firebase services rather than emulator hosts. A Mapbox access token is required regardless of backend target and is set in `local.properties` as well. The Firebase project itself needs the rules and Cloud Functions from `../android-native-backend-branch/` deployed to it; see that repository's README and `../HOSTED-CUTOVER-PLAN.md` for the full procedure, including the still-open decisions that need a choice before this is complete (a production mail provider for the portal side, and verifying the relational database migrates cleanly to Postgres).

## Running against local Firebase emulators

The full local Docker stack that used to wire Firebase emulators, the Laravel API, and the portal frontend together has been retired; there is currently no packaged local full-stack environment. Firebase's own emulator suite (Auth, Firestore, Storage, Cloud Functions) can still be run directly from `android-native-backend-branch/` if a local backend target is wanted (see that repository's README) — start it, then in this repository's `local.properties` set `USE_LOCAL_BACKEND=true` and `LOCAL_BACKEND_HOST` to the address the emulators are reachable at. For the Android Emulator, `10.0.2.2` reaches the host machine; for a physical device on the same Wi-Fi, the recommendation is to use the host machine's actual LAN IP instead, since it works for both the emulator and any physical device without needing to be swapped per test target — it does need updating whenever that IP changes. For a physical device with no shared Wi-Fi, forward the emulator's Auth, Firestore, Cloud Functions, and Storage ports from the host over USB instead, then point `LOCAL_BACKEND_HOST` at the loopback address. Rebuild after changing `local.properties`. Emulators serve plain HTTP, and physical devices block cleartext connections by default; the debug build's network security configuration permits cleartext for debug builds only, merged automatically over a strict configuration that release builds always use, so nothing needs editing there when a LAN IP changes. The hosted-Firebase path above is the primary documented workflow; this is the fallback when a real project isn't available.

## Getting started

Open this repository in Android Studio, sync Gradle, and run on a physical device or emulator. Java 17 or newer and a recent Android Studio release are required; the Firebase CLI and Node.js are additionally needed only if working on the backend rules or Cloud Functions in the sibling `android-native-backend-branch/` repository.

## Testing

Unit tests run through Gradle's standard test task and use mocked repositories rather than a real device or emulator. An instrumented test suite also exists but currently requires an Android emulator or device to run and has known gaps in what it covers versus what actually gets exercised in day-to-day development — treat its coverage as unverified until it's confirmed to run cleanly in whatever environment is being used.

## Screenshots

| Home Dashboard | Community News | Profile & Settings |
|:---:|:---:|:---:|
| <img src="./art/screenshots/home_light.png" width="200" /> | <img src="./art/screenshots/feed_light.png" width="200" /> | <img src="./art/screenshots/profile_light.png" width="200" /> |
| <img src="./art/screenshots/home_dark.png" width="200" /> | <img src="./art/screenshots/feed_dark.png" width="200" /> | <img src="./art/screenshots/profile_dark.png" width="200" /> |

## Further reading

`AGENTS.md` in this repository has the fuller development lifecycle and conventions. `../REPOS.md` has the full cross-repository map and role model. `../BRIDGE-CONTRACT.md` has the field-level Firebase-to-Laravel bridge contract. `../HOSTED-CUTOVER-PLAN.md` has the hosted-services cutover procedure.
