# WildWatch Backend (Firebase)

The shared Firebase backend for WildWatch: Firestore and Storage security rules, Cloud Functions written in TypeScript, and the seed script used to populate development data. This is the `backend` branch of the mobile app's repository — an orphan branch with no shared history with `master`, kept separate because it holds server-side configuration rather than application code. Checking it out requires a git worktree or a second clone rather than a normal branch switch; the exact commands are in `../REPOS.md`.

## What this is for

This is the Firebase side of WildWatch's two-backend architecture, driving the native mobile app's authentication, offline-sync data store, file storage, and push notifications. It is also the origin side of the Firebase-to-Laravel bridge: `functions/src/bridge.ts` and the on-write triggers in `functions/src/index.ts` sign and forward incident, sighting, and SOS-alert writes to the Laravel API in `../web-portal/backend/` as HMAC-authenticated webhooks, and carry an echo-prevention check so a write that originated on the Laravel side doesn't bounce back out as another webhook call. The full field-level contract, including which system is authoritative for which entity, is documented in `../BRIDGE-CONTRACT.md`.

Custom claims assigned here (`role`, `park_id`) are what the mobile app, the portal, and the security rules in this repository all use to authorize access — see `../REPOS.md`'s role-model write-up for the current set of roles and how they map across systems.

## Running against a real Firebase project

Deploying the Firestore rules, Storage rules, and Cloud Functions to a real project supersedes the local Emulator Suite as the default development target. This requires the Firebase CLI authenticated against the target project, the security rules and indexes deployed from this repository's root, and the Cloud Functions built and deployed from the `functions/` directory. Cloud Functions configuration — the shared HMAC secret and the Laravel webhook base URL — must be set to real, rotated values that match what the Laravel side is configured with exactly; the local emulator's placeholder secret must not be reused. The full cutover procedure is documented in `../HOSTED-CUTOVER-PLAN.md`. Once a project is live, the mobile app should have its local-backend flag turned off so it connects to real Firebase rather than emulator hosts, and the seed script can be pointed at the real project's Auth and Firestore instead of emulator hosts for populating development or demo data.

## Running locally

The Firebase Emulator Suite (Auth, Firestore, Storage, and Functions) can still run this backend entirely locally, using the emulator configuration already present in this repository's `firebase.json`, with the Firebase CLI's `emulators:start` command. The Docker stack that used to wire these emulators together with the Laravel API, a MySQL database, and the portal frontend for a full-fidelity local environment has been retired; running the emulators alone (without the rest of the stack) is now the only local option short of the hosted-services path above.

## Testing

Cloud Functions have a Jest test suite exercising the trigger-dispatch logic, the bridge's HMAC signing and echo-prevention functions in isolation, and the role-assignment callable's authorization logic. Coverage is deliberately prioritized toward the bridge and authorization code rather than the full surface — see `../BRIDGE-CONTRACT.md` and the punch-list history referenced from `../AGENTS.md` for what's covered and what still needs attention.
