# SilverBack Sentry / Wildwatch HWC — Backend Integration Plan

> **Status:** DRAFT v0.1 — living document, updated as each screen is converted.
> **Scope of this document:** where and how the backend for the *new* HWC features
> (conflict reports, compensation claims, community alerts, SOS, ranger live
> tracking) will live, once UI conversion is complete. No backend code is written
> yet — per the current task, UI conversion comes first.

---

## 1. What already exists (do not re-architect this)

This repo is **mid-migration** from an Expo/React Native app to native Kotlin, and
already has a working, wired backend for the *observation-reporting* slice of the
product:

| Concern | Current implementation |
|---|---|
| Auth | Firebase Auth (email/password) |
| Data | Cloud Firestore, flat `observations` collection |
| Files | Firebase Storage (`observations/{id}/{n}_{timestamp}.jpg`) |
| Push | Firebase Cloud Messaging (dependency present) |
| Offline cache | Room (single source of truth for UI, per guardrail G2) |
| Sync | `ObservationRepositoryImpl`, single-writer sync worker (guardrails G1–G5) |

This is a **free-tier-friendly stack already in production use** (Firebase's
Spark plan covers Auth/Firestore/Storage/FCM at this project's scale). The
architecture guardrails in `README.md` (G1–G7) exist specifically to keep this
reliable. **Recommendation: extend this same stack for the new HWC entities
rather than introducing a second backend**, for three reasons:
1. The Expo app and this app currently share one Firebase project/registration —
   splitting backends mid-migration doubles the sync surface area.
2. Firestore already solves "the future uwa-portal website reads the same data"
   for free, via the Firebase Web SDK — no separate API needed.
3. Auth, offline cache, and file upload are already solved and tested.

Section 3 gives this as **Option A**. Section 4 gives the **Option B** stack you
asked about explicitly (Render/Vercel/Postgres/Cloudinary) for if the team
decides to move off Firebase later — e.g. to avoid Blaze-plan billing risk at
scale, or to get a relational model for reporting/analytics.

---

## 2. New data entities the Wildwatch UI introduces

Beyond the existing `observations` collection, the screens being converted need:

- `conflicts` — human-wildlife conflict reports (species, location, damage type,
  severity, photos, status)
- `claims` — compensation claims (linked to a `conflict`, claimant details, claim
  amount, supporting docs, approval status/workflow)
- `alerts` — community alerts broadcast by rangers/UWA (title, body, park,
  radius/geofence, severity, expiry)
- `sos_events` — SOS button presses (user, live location, timestamp, acknowledged
  ranger, resolution)
- `ranger_tracking` — live ranger location pings (for the Ranger Map / Community
  "nearby alert" map)
- `notifications` — per-user notification feed (read/unread, deep link target)
- `users` (profile fields beyond Firebase Auth) — role (`community`/`ranger`/
  `admin`), park assignment, language, display name — this already exists
  informally via `user-prefs` in the wireframe; needs a real `users` collection/
  table with a `role` field so the app can route Community vs Ranger correctly
  post-login (the wireframe prototype hardcodes this via local prefs; the real
  app must not).

---

## 3. Option A — Extend Firebase (recommended default)

- **Auth:** unchanged (Firebase Auth). Add a custom claim or a `users/{uid}.role`
  Firestore field for `community` / `ranger` / `admin`, read once at login to
  decide the nav-host start destination.
- **Data:** new Firestore collections `conflicts`, `claims`, `alerts`,
  `sos_events`, `ranger_locations`, `notifications` — flat, top-level, same
  pattern as `observations`, each with a client-generated UUID as both the Room
  primary key and Firestore doc ID (guardrail G4 pattern).
- **Files:** reuse Firebase Storage, `conflicts/{id}/...`, `claims/{id}/...`.
- **Live ranger tracking:** Firestore's realtime listeners are sufficient at this
  scale (no need for a separate websocket service) — a ranger's device writes a
  location doc every N seconds; Community/Ranger map screens attach a snapshot
  listener.
- **SOS push fan-out:** a small **Cloud Function** (Firebase's serverless
  compute, free tier: 2M invocations/month) triggered `onCreate` of a
  `sos_events` doc, which sends an FCM push to nearby rangers. This is the one
  piece of "backend code" this option needs beyond client + Firestore rules.
- **Future uwa-portal website:** reads the same Firestore project via the
  Firebase Web SDK + Firebase Hosting (also free tier) — zero extra integration
  work, and it's already the plan for the Expo/native parity period.
- **Cost at this stage:** effectively \$0 (Spark plan) until real production
  traffic; Cloud Functions triggers stay well inside the free grant for a
  single-park pilot.

---

## 4. Option B — Postgres + Render + Cloudinary (if moving off Firebase)

If the team later decides Firestore's document model is a poor fit for
claims-approval workflows and reporting (relational joins across
conflicts→claims→users are awkward in Firestore), here is the free-tier stack
matching what you named:

| Layer | Service | Free tier notes |
|---|---|---|
| API | **Render** (Web Service, Node/Express or similar) | Free instance spins down after 15 min idle — acceptable for a pilot, not for production SOS latency requirements |
| Database | **Render Postgres** or **Neon**/**Supabase** Postgres | Free tier: ~0.5–1GB storage, single region — fine for a single-park pilot's row counts |
| File/image storage | **Cloudinary** | Free tier: 25 credits/month (~25GB storage+bandwidth combined) — good fit for sighting/conflict photos, includes on-the-fly image resizing which Firebase Storage doesn't |
| Auth | Either keep Firebase Auth (cheapest path — no need to rebuild login) or move to Postgres + a JWT-based auth layer on Render | Keeping Firebase Auth and only moving *data* to Postgres is the lowest-risk hybrid |
| Push notifications | Keep Firebase Cloud Messaging (works independently of where data lives) | |
| Live ranger tracking | Needs a **WebSocket** channel (Render supports long-lived WebSocket connections on paid instances; free tier's spin-down makes this unreliable) — realistically this is the strongest argument for staying on Firestore realtime listeners even if everything else moves | |
| Future uwa-portal website | Hosted on **Vercel** (free tier, ideal for a Next.js/Remix admin dashboard), fetching from the Render API over REST | |

**Trade-off summary:** Option B gives cleaner relational queries and cheaper
large-file storage (Cloudinary), at the cost of rebuilding auth-integration,
losing "free realtime sync" for ranger tracking, and adding a second backend to
operate alongside Firebase (since FCM push is easiest left on Firebase either
way). This is a bigger lift — recommend only if reporting/analytics needs
outgrow Firestore, not as the default starting point.

---

## 5. Decision needed before backend work starts

- [ ] Confirm Option A vs Option B vs hybrid (keep Firebase Auth + FCM, move data
      to Postgres) before any backend code is written.
- [ ] Decide whether `ranger_locations` realtime tracking is a hard requirement
      for the pilot (this pushes strongly toward keeping Firestore regardless of
      the Option B choice for everything else).
- [ ] Confirm the `users.role` field's source of truth (Firestore custom claim
      vs Firestore doc vs future Postgres row) — this gates Community vs Ranger
      navigation in the app and needs to exist before the role-based home
      routing screen is wired.

---

## 6. Conversion log (updated per screen, no backend code written yet)

| # | Screen | Status | Backend entity touched (future) |
|---|---|---|---|
| 1 | Splash | ✅ UI converted, ✅ OS SplashScreen wired (no more double/default-icon splash) | none |
| 2 | Auth (Login/Register) | ✅ UI converted (combined into one screen; phone sign-in, Google/Apple, language/park are UI-only stubs - see commit) | `users` (role field) |
| 3 | Location permission | pending | none |
| 4 | Community Home | pending | `observations`, `conflicts` (counts) |
| 5 | Community Feed | pending | `observations` |
| 6 | Wildlife Sighting | pending | `observations` (existing) |
| 7 | HWC Conflict report | pending | `conflicts` (new) |
| 8 | Claim prompt | pending | `conflicts` |
| 9 | Claim form | pending | `claims` (new) |
| 10 | Community Alerts | pending | `alerts` (new) |
| 11 | SOS | pending | `sos_events` (new) |
| 12 | Notifications | pending | `notifications` (new) |
| 13 | Community Profile | pending | `users` |
| 14 | Ranger Dashboard | pending | `conflicts`, `sos_events` |
| 15 | Ranger Map | pending | `ranger_locations`, `conflicts` |
| 16 | Ranger Tracking | pending | `ranger_locations` (new) |
| 17 | Ranger Incident | pending | `conflicts` |
| 18 | Ranger Profile | pending | `users` |

*(This table is appended to / checked off as we go — no backend integration is
implemented until UI conversion is complete and Section 5's decisions are made,
per your instructions.)*
