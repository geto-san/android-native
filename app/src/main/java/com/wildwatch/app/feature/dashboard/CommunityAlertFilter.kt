package com.wildwatch.app.feature.dashboard

import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.model.ParkIdMatcher
import com.wildwatch.app.core.model.UserRole
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
        val isUnresolved = incident.status != IncidentStatus.RESOLVED
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
