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
    - `target_uid`: string
    - `message`: string
    - `time`: string
    - `type`: string (`alert`, `task`, `update`)

## Enums (Case-insensitive)

- **IncidentType**: `SIGHTING`, `CONFLICT`, `EMERGENCY`, `POACHING`
- **IncidentStatus**: `OPEN`, `IN_PROGRESS`, `RESOLVED`
- **IncidentSeverity**: `LOW`, `MEDIUM`, `HIGH`, `LIGHT`
- **RangerProgress**: `EN_ROUTE`, `ON_SITE`
