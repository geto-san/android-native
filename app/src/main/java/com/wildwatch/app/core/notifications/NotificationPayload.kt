package com.wildwatch.app.core.notifications

import com.wildwatch.app.core.database.NotificationType

// Cloud Functions (functions/src/notifications.ts) tags every push
// notification's FCM `data` payload with a `type` string matching
// NotificationType's names, plus a type-specific id field - which field
// carries the navigable target differs per type and, for SIGHTING_APPROVED,
// even per trigger (see BRIDGE-CONTRACT.md's FCM table). This is the one
// place that knows that mapping, shared by WildWatchMessagingService
// (persisting + tagging the tap intent) and the UI layer's route resolution.
object NotificationPayload {

    fun parseType(raw: String?): NotificationType? =
        raw?.let { value -> runCatching { NotificationType.valueOf(value) }.getOrNull() }

    // Null when this type has no navigable target on mobile today - e.g.
    // SIGHTING_APPROVED can be sent from two different Cloud Functions
    // triggers: one for an incident-typed sighting (data.incidentId, and
    // IncidentDetail exists on mobile to open it), another for a
    // wildlife_sightings-table approval (data.sightingId only) - that table
    // isn't synced to mobile as its own entity, so there is no screen to
    // deep-link to for that case.
    fun targetId(type: NotificationType?, data: Map<String, String>): String? = when (type) {
        NotificationType.SIGHTING_APPROVED -> data["incidentId"]
        NotificationType.NEW_FEED_ARTICLE -> data["articleId"]
        else -> null
    }
}
