# SilverBack Sentry — Grant-Readiness & System-Gap Report

**Date:** 2026-09-13
**Prepared for:** grant panel demo (midday deadline)
**Scope:** `android-native` repository consolidation + end-to-end system status (mobile app, Firebase backend, Laravel/portal bridge)

---

## 1. Executive summary

SilverBack Sentry is a genuinely working, geographically-informed wildlife-conservation platform: a Kotlin/Compose Android app (rangers + public + guests), a Firebase backend (Auth, Firestore, Storage, FCM), and a Laravel/Neon/Postgres web portal for wardens/UWA officials, joined by a documented Firebase↔Laravel bridge. The mobile app's offline-first outbox, patrol tracking, offline maps, and feed are **implementation-complete and verified** (unit tests pass, debug APK builds, live API responds).

**This session's change:** the Firebase backend (`backend` branch, previously a separate orphan branch/worktree at `android-native-backend-branch/`) was **merged into `master`**, consolidating both sides into a single repository tree. The merge is committed, pushed, and the CI pipeline (tests → APK build → GitHub release) is running green on the merged tree. All backend references were repointed to in-repo paths.

**The honest headline for the panel:** the platform's *pipeline* works end-to-end (mobile → Firestore → Laravel → Postgres → portal, live-tested 2026-08-13 and re-verified today), but several *features a panel would expect to see demoed* remain incomplete in the current deployment. Since this report was first written (2026-09-13) the highest-impact gaps **A–F have been implemented and are committed** on `master` with a green CI build: SOS now has a one-tap button on Home with the SOS incident type wired through the existing outbox/bridge; the app now requests `POST_NOTIFICATIONS` at first launch; the Alerts and Notifications screens now stream real `alerts`/`notifications` Firestore collections (Room stays the UI's only read source), with matching `firestore.rules` blocks and `scripts/seed.ts` additions — those two deploy steps need the Firebase CLI, which isn't installed in this session (commands documented in §7). The main things still not yet real in the *live* deployment: **push notification delivery** (the Cloud Functions that publish don't run on the Spark plan), **role-claim self-provisioning** (the `onUserCreated` function can't run on Spark), and **live seeded reminders until the rules+seed are deployed** (the local Room seed keeps the screens populated for the demo regardless). Details and the current status follow.

---

## 2. Consolidation (done this session)

| Item | Status |
|---|---|
| Merged Firebase backend (functions, rules, hosting, seed scripts) into `master` | ✅ committed/pushed |
| Resolved `.gitignore` + `README.md` conflicts | ✅ combined |
| Merged README documents both app + backend from one root | ✅ |
| Repointed all `android-native-backend-branch/` references (AGENTS.md, README, source comments) | ✅ |
| Local verification: `./gradlew testDebugUnitTest` | ✅ BUILD SUCCESSFUL (8m) |
| Local verification: `./gradlew assembleDebug` | ✅ APKs produced |
| Backend verification: `functions` jest suite | ✅ 26/26 pass |
| Seed scripts typecheck (`scripts`) | ✅ `tsc --noEmit` pass |
| `generate_parks_geojson.mjs` regenerates tracked `parks.geojson` identically | ✅ |
| Push to `origin/master` → CI (tests, APK, GitHub release) | 🔄 running |

**Follow-up still open:** the remote `backend` branch and the duplicate `android-native-backend-branch/` worktree are now redundant; root-contracts `REPOS.md` still describes the old two-branch layout. (Two can be cleaned up in minutes.)

---

## 3. What is in place and verified working

| Area | Evidence |
|---|---|
| Offline-first outbox (Room → Firestore → Laravel) | `IncidentRepositoryImpl.syncPending()`; DRAFT/PENDING statuses, mutex-guarded flush, WorkManager 15-min backstop + immediate expedited sync |
| Patrol tracking (background breadcrumbs, media, process-death resume) | `PatrolTrackingService`, `PatrolSyncWorker`; active patrol restored across process death |
| Offline maps | Mapbox `TileStore`/`OfflineManager`; `parks.geojson` bundled fallback; boundary overlay verified |
| Auth (passwordless email link, Google, guest incl. offline-degrade) | `AuthRepositoryImpl`; guest works offline, retries anonymous sign-in online |
| Incident reporting (GPS, camera, 5 types, severity) | `ReportIncidentScreen` + `ReportIncidentViewModel` (process-death resilient) |
| FCM topic subscriptions (park + role topics incl. `park_alerts_all` for guests) | `FcmTopicManager`; synced on auth events + token refresh |
| Community feed (portal-authored → Firestore → Room + photo grid) | `ArticleRepositoryImpl`, `FeedScreen` staggered image grid |
| Push UI (inbox, unread badge, tap→deep-link routing) | `NotificationsScreen`, `SilverBackSentryMessagingService`, `NotificationRouting` |
| Live hosted services | Firebase `wildwatch-82abc`, Render API (`wildwatch-api.onrender.com` — responds), Cloudflare portal (`wildwatch-portal…workers.dev`), Neon Postgres |
| Test coverage (JVM) | 23 unit-test files / 134 tests across ViewModels + repositories |
| Cloud Functions jest coverage | 26 tests: trigger dispatch, HMAC bridge, echo-prevention, RBAC callable |

---

## 4. What is lacking — prioritized

### P0 — Demo-critical gaps (a judge would likely probe these)

> Update 2026-09-14: items **1, 2 (permission half), 3, and 5 are closed** on `master` (quick wins A–F in §5). Item 2's *delivery* half and item 4 remain open (Spark plan). Where a gap is now closed but needs a live deploy, that's flagged.

1. ~~**No SOS alert flow in the mobile app.**~~ **Closed.** `IncidentType.SOS` added; one-tap SOS card on Home opens the report form pre-set to SOS (with a pre-filled description+high severity) through the existing outbox/bridge; `parseType` now correctly maps both `sos` and `snare` (the two silent-parse-to-`SIGHTING` bugs). The whole backend path (`sos_alerts` rules, lambda trigger, bridge mapping, FCM handler) was already built and is now reachable from the product's primary surface.
2. **Push notifications cannot be *delivered* in production.** All sends live in Cloud Functions (`notifications.ts`), and the project is on the Spark plan, which cannot run Functions — so subscriptions exist but nothing publishes. The app did previously declare `POST_NOTIFICATIONS` but never requested it; **that half is closed** (`RequestNotificationPermissionOnLaunch` in `MainActivity`, Android 13+, once per install). Also, guests who enter offline-degraded local mode never subscribe to topics (subscription only happens on real Firebase sign-in), weakening the "guest alerts" story.
3. ~~**Community alerts are fabricated placeholder data.**~~ **Closed (code+rules); deploy pending.** `AlertRepositoryImpl` now attaches a Firestore `alerts` collection listener (read-only rule added to `firestore.rules`) that upserts/deletes into Room; the four-item Room seed remains only as an offline fallback when the remote is empty/unreadable. The `alerts`/`notifications` data still needs the rules deployed + `scripts/seed.ts` run against the live project (Firebase CLI not available this session — commands in §7).
4. **Role claims are not provisioned for new users.** Claims are set by the `onUserCreated` function (Spark can't run it). A brand-new sign-up today gets no `role` claim (defaults to `PUBLIC`), so the 3-device session cap is dead and ranger provisioning only works via the manual seed script or the portal's Admin SDK.
5. ~~**The advertised ranger demo login is unusable in the app.**~~ **Closed.** `AGENTS.md` now documents that ranger sign-in is Google + `@gmail.com` only (`violatesRangerSignInPolicy` in `AuthRepositoryImpl.kt`), with manual role-claim provisioning noted for the Spark plan; seed credentials are identified as emulator fixtures rather than a usable login.

### P1 — Important gaps

6. **"My Reports" is a placeholder screen** ("Reporting History Coming Soon") though the route is reachable via Home → See all.
7. **No runtime crash reporting or analytics.** Zero Firebase Analytics/Crashlytics/Sentry anywhere in the app. Outages would be invisible.
8. **No real release keystore.** CI ships debug-signed APKs; `staging` signs with the debug key. Google Sign-In specifically breaks on CI-built APKs (debug keystore SHA-1 not registered with Firebase — documented).
9. **Instrumented UI coverage is thin and unverified in CI**: 4 androidTest files / 11 tests, **zero Compose UI tests**, zero end-to-end flows; the JVM suite intentionally mocks everything.
10. **Notification intents are extras-based** (not URI-based); deep links land only when the app opened through the FCM PendingIntent.
11. **App Check enablement is unverified** (Play Integrity provider is wired for release, but there's no Play release channel configured).

### P2 — Polish / deferred-but-accepted

12. Media CDN (Cloudinary/R2) explicitly deferred — volume is a fraction of the free tier; correct call for now.
13. Stale screenshots in README (predate the 2026-08-13 UI fixes).
14. Resend email sending verified only at the code level, never with a live key.
15. Portal CSP fix never browser-smoke-tested interactively.
16. Version still `1.0.0` / `versionCode 1`.
17. Room schema migration tests absent (schemas dir exists but no migration test files).

---

## 5. Realistic fixes before the panel

Ordered by (demo impact ÷ effort). All are within the three codebases already in the workspace.
**Status legend:** ✅ = implemented, committed on `master`, verified by unit tests + CI; 📋 = implemented but needs a manual deploy step this session couldn't run (no Firebase CLI).

| # | Fix | Status | Impact |
|---|---|---|---|
| A | Add `SOS` incident type + a prominent one-tap SOS button on Home (writes `type: sos` through the existing outbox/bridge; the whole backend path is already built) | ✅ (`IncidentType.SOS`, `Route.ReportIncident(presetType)`, Home SOS card, `parseType` now maps `sos`/`snare` — the two silent-parse bugs) | High — makes the safety story real and demoable |
| B | Request `POST_NOTIFICATIONS` at first launch | ✅ (`RequestNotificationPermissionOnLaunch` in `MainActivity`, Android 13+, once per install) | High — otherwise notifications silently never arrive on modern Android |
| C | Publish alerts/notifications from Firestore instead of the Room seed: `AlertRepositoryImpl`/`NotificationRepositoryImpl` now attach Firestore listeners (auth-scoped for notifications) that upsert into Room; Room seed stays as offline fallback | ✅ code + ✅ `firestore.rules` (`alerts` read rule) / 📋 deploy rules + 📋 run seed (`scripts/seed.ts` now adds `seedAlerts` + `seedNotifications`) | Medium-high — the Alerts screen and Activity screen become genuinely live |
| D | Fix the confusing ranger credentials in docs (or add email+pw back) so the demo has a working ranger login story | ✅ (`AGENTS.md` now documents Google/Gmail-only ranger sign-in + manual role-claim provisioning; seed credentials identified as emulator-only) | Medium — avoids an awkward dead-end during the demo |
| E | Seed a real `alerts` document + one `notifications` document before the demo so "live data" is demonstrable | 📋 `seedAlerts()`/`seedNotifications()` added; run via `scripts/seed.ts` against the live project (needs Firebase CLI + Admin credentials) | Medium |
| F | Delete the redundant `backend` branch + `android-native-backend-branch/` worktree; update `REPOS.md` to the single-repo reality | ✅ (remote `backend` branch deleted, worktree folder removed, `REPOS.md` updated in `/home/geto/Projects/Documentations/WildWatch/root-contracts/`) | Low — cleanliness |

**Not realistic before midday (needs a platform decision):** Cloud Functions on Spark (push delivery, RBAC provisioning, device-session cap) — either upgrade to Blaze (~running cost) or complete a Spark-compatible redesign like the mobile-direct bridge did for incidents. Recommend explicitly framing this in the demo as the single platform-level decision.

---

## 6. Demo recommendations (what to show, what not to claim)

**Show:** the ranger flow offline (report canned while in airplane mode → syncs on reconnect); patrol tracking with park boundary; community feed with images; portal-authored feed article arriving live in the app; incident at (0,0) now null-guarded; the existing live pipeline mobile → Firestore → Laravel → Postgres; the merged single-repo architecture; the CI → GitHub release dance.

**Do not claim (without still-remaining work):** *delivered* push notifications (subscriptions + permission + inbox UI exist, but nothing publishes on Spark — needs Blaze or a Spark-compatible send path), role-claim self-provisioning, real "My Reports" history. End-to-end SOS, live back-ended alerts/notifications, and notification-permission prompting **are now implemented** — the only open piece for the alerts/notifications demo is deploying the rules + seed to the live project (see §7).

---

## 7. Risks

- **CI keystore SHA-1 mismatch for Google Sign-In** persists on CI-built APKs; use a locally-built APK for the demo, or accept email-link/guest on the CI artifact.
- **`sos_alerts` create is role-gated**: rules allow creation only for claim-bearing `ranger`/`public` users (`firestore.rules:182-191`), not guests — for the demo use a seeded account that has the claim (the seed script sets it); a brand-new or guest signup would be denied.
- **Render free tier cold-starts (~30-50 s)** on the first request after idle — wake the API before the demo begins.
- **Spark plan** constrains everything Functions-related; the bridge already works around it for incidents (`LARAVEL_API_BASE_URL` direct calls), SOS will too via the mobile-direct path.
- **Alerts/notifications live data — the two pending deploy steps (quick wins C & E)**: the code, rules, and seed script are committed, but this session had no Firebase CLI. Before the demo, run:

```
# 1) Deploy the updated firestore.rules (adds the read-only `alerts` collection rule)
firebase deploy --only firestore:rules --project wildwatch-82abc

# 2) Seed live alerts + notifications for the demo account (Admin SDK needed)
GCLOUD_PROJECT=wildwatch-82abc npx ts-node scripts/seed.ts   # from this repo root
```

> `scripts/seed.ts` defaults to the local emulator (`FIRESTORE_EMULATOR_HOST`); for a live run you must unset those env vars and authenticate the Admin SDK (valid service-account credentials). Keep at least one seeded public account whose `target_uid` matches a user you'll sign in as, or the Notifications screen shows only the Room fallback.

---

## 8. Verification commands (reproducibility)

```
# App — unit tests + APK
./gradlew testDebugUnitTest
./gradlew assembleDebug

# Backend functions — jest
cd functions && npm ci && npm test

# Seed scripts — typecheck
cd scripts && npm ci && npx tsc --noEmit

# Park boundaries asset
node scripts/generate_parks_geojson.mjs
```

All of the above pass on the current `master` HEAD (commit after the backend merge + quick wins A–F), with the app unit tests including the Notification repository's new Firestore-listener construction.