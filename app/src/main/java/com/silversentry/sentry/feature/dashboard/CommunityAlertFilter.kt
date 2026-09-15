package com.silversentry.sentry.feature.dashboard

import com.silversentry.sentry.core.database.IncidentStatus
import com.silversentry.sentry.core.database.IncidentType
import com.silversentry.sentry.core.database.SyncStatus
import com.silversentry.sentry.core.model.Incident
import com.silversentry.sentry.core.model.ParkIdMatcher
import com.silversentry.sentry.core.model.UserRole
import java.time.Instant

internal object CommunityAlertFilter {
    fun shouldShow(
        incident: Incident,
        currentUserId: String?,
        currentUserParkId: String?,
        currentUserRole: UserRole?,
        dismissedIds: Set<String>,
        seenTimestamps: Map<String, Long>,
    ): Boolean {
        val twentyFourHoursAgo = Instant.now().minusSeconds(24 * 60 * 60)
        val now = Instant.now()
        val isAlertableType = incident.type == IncidentType.SIGHTING ||
            incident.type == IncidentType.CONFLICT ||
            incident.type == IncidentType.EMERGENCY
        val isRecent = runCatching { Instant.parse(incident.reportedAt) }.getOrNull()?.isAfter(twentyFourHoursAgo) == true
        // Both terminal states pull an alert out of the community feed: resolved is the
        // natural close, cancelled is a withdrawn alert (e.g. a false-alarm SOS turned off
        // via HOLD TO CANCEL) - neither should keep nagging the community.
        val isUnresolved = incident.status != IncidentStatus.RESOLVED && incident.status != IncidentStatus.CANCELLED
        val isSynced = incident.syncStatus == SyncStatus.SYNCED
        val isNotReporter = incident.userId != null && incident.userId != currentUserId
        val animalEligible = incident.animalSeen != false
        val seenAt = seenTimestamps[incident.id]
        val seenExpired = seenAt?.let { Instant.ofEpochMilli(it).plusSeconds(24 * 60 * 60).isBefore(now) } ?: false
        val notDismissed = incident.id !in dismissedIds
        // UWA officials oversee every park (mirrors the "uwa_official" FCM
        // topic, which isn't park-scoped). A user with no park assigned yet
        // falls back to seeing everything, matching the "park_alerts_all"
        // topic FcmTopicManager subscribes non-ranger/warden users to.
        val isInScope = currentUserRole == UserRole.UWA_OFFICIAL ||
            currentUserParkId.isNullOrBlank() ||
            ParkIdMatcher.matches(incident.park, currentUserParkId)
        return isAlertableType && isRecent && isUnresolved && isSynced && isNotReporter &&
            animalEligible && notDismissed && !seenExpired && isInScope
    }
}
