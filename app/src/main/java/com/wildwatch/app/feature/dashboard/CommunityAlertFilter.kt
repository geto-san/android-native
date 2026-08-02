package com.wildwatch.app.feature.dashboard

import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.model.Incident
import java.time.Instant

internal object CommunityAlertFilter {
    fun shouldShow(
        incident: Incident,
        currentUserId: String?,
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
        return isAlertableType && isRecent && isUnresolved && isSynced && isNotReporter &&
            animalEligible && notDismissed && !seenExpired
    }
}
