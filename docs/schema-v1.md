# WildWatch Backend Schema Contract (v1)

This document defines the canonical structure of Firestore collections and documents. All clients (Mobile, Web Portal) MUST adhere to this schema to ensure data integrity.

## Collections

### `users`
Information about rangers, wardens, and UWA officials.
- **Path**: `/users/{uid}`
- **Fields**:
    - `role`: string (one of: `public`, `ranger`, `warden`, `uwa_official`)
    - `park_id`: string (e.g., `BWINDI_IMPENETRABLE`)
    - `name`: string
    - `contact`: string
    - `email`: string

### `parks`
Static information about wildlife parks.
- **Path**: `/parks/{id}`
- **Fields**:
    - `name`: string
    - `boundary`: Geopoint (or geo-json structure)
    - `tile_urls`: list of strings

### `incidents`
Reports filed by rangers or public users.
- **Path**: `/incidents/{id}`
- **Fields**:
    - `type`: string (`conflict`, `emergency`, `poaching`, `sighting`)
    - `status`: string (`open`, `in_progress`, `resolved`)
    - `rangerProgress`: string (`en_route`, `on_site` | null)
    - `isEscalated`: boolean
    - `park`: string (Park name)
    - `community`: string
    - `species`: string
    - `severity`: string (`low`, `medium`, `high`, `light`)
    - `category`: string | null
    - `summary`: string | null
    - `lat`: number
    - `lng`: number
    - `locationName`: string | null
    - `userName`: string | null
    - `userEmail`: string | null
    - `userId`: string | null
    - `reportedAt`: string (ISO 8601)
    - `synced`: boolean
    - `syncedAt`: string (ISO 8601) | null
    - `assignedTo`: string (uid) | null
    - `assignedToName`: string | null
    - `hasEvidence`: boolean
    - `evidenceCount`: number
    - `evidencePhotoUrls`: list of strings
    - `voiceNoteUrl`: string | null
    - `voiceNoteDurationSec`: number | null

### `patrol_logs`
GPS traces from ranger patrols.
- **Path**: `/patrol_logs/{id}`
- **Fields**:
    - `ranger_uid`: string
    - `route_points`: list of map { `lat`: number, `lng`: number, `timestamp`: string }
    - `startTime`: string
    - `endTime`: string | null

### `notifications`
Targeted alerts for specific users.
- **Path**: `/notifications/{id}`
- **Fields**:
    - `target_uid`: string | null
    - `type`: string — mobile enum: `SYSTEM`, `SIGHTING_APPROVED`, `SECURITY_ALERT`, `LIKE`, `COMMENT`, `NEW_FEED_ARTICLE` (see `NotificationType.kt`)
    - `title`: string
    - `message`: string
    - `time`: string (ISO 8601)
    - `isRead`: boolean
    - `data`: map (optional metadata, e.g. `articleId`, `incidentId`)

### `feed`
Community news articles authored by the warden portal (Laravel → Firestore via Admin SDK).
- **Path**: `/feed/{id}` — `{id}` matches Laravel `news_articles.firestore_doc_id` (defaults to MySQL `article_id`)
- **Fields**:
    - `title`: string
    - `excerpt`: string
    - `body`: string | null
    - `category`: string
    - `source`: string
    - `readTime`: string (e.g. `"3 min"`)
    - `theme`: string (`forest`, `wildlife`, `security`) — maps to mobile `ArticleTheme`
    - `likes`: number
    - `comments`: number
    - `publishedAt`: string (ISO 8601)
    - `authorId`: string (Laravel user id as string)
    - `source_system`: string (`laravel` on portal-originated writes)
- **Mobile consumer:** `FeedScreen` / `ArticleRepositoryImpl` (Room cache + Firestore listener)
- **Not the same as:** `CommunityAlertsScreen` / `AlertEntity` (operational alerts, separate schema)

## Bridge metadata (cross-system)

Documents written by Laravel observers or Functions webhooks may include:
- `source_system`: `"laravel"` | `"firestore"` — used to prevent write-loop echoes

## Enums (Case-insensitive)

- **IncidentType**: `SIGHTING`, `CONFLICT`, `EMERGENCY`, `POACHING`
- **IncidentStatus**: `OPEN`, `IN_PROGRESS`, `RESOLVED`
- **IncidentSeverity**: `LOW`, `MEDIUM`, `HIGH`, `LIGHT`
- **RangerProgress**: `EN_ROUTE`, `ON_SITE`
