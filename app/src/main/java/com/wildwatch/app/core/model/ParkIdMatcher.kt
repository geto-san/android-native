package com.wildwatch.app.core.model

import com.wildwatch.app.core.database.Park

// Incident.park is the Park enum (e.g. BWINDI_IMPENETRABLE); User.parkId comes from the
// Firebase "park_id" custom claim in Firestore-seed format (e.g. "bwindi-impenetrable") -
// normalize both to compare (see BRIDGE-CONTRACT.md's documented park-id-format gap; same
// normalization FcmTopicManager.normalizeTopicSegment uses for FCM topic names). Extracted
// out of CommunityAlertFilter (its original, private home) so ranger-side park-scoping can
// share the exact same matching logic instead of re-deriving it.
object ParkIdMatcher {
    fun matches(incidentPark: Park, userParkId: String): Boolean =
        normalize(incidentPark.name) == normalize(userParkId)

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("[\\s-]+"), "_")
}
