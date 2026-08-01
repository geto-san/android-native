package com.wildwatch.app.feature.dashboard

import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.model.Incident
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Mirrors [HomeViewModel] rules without Android framework dependencies.
 */
class HomeViewModelAlertFilterTest {

    private fun incident(
        id: String,
        userId: String? = "other-user",
        animalSeen: Boolean? = true,
        syncStatus: SyncStatus = SyncStatus.SYNCED,
    ) = Incident(
        id = id,
        type = IncidentType.CONFLICT,
        status = IncidentStatus.OPEN,
        park = Park.BWINDI_IMPENETRABLE,
        community = "Buhoma",
        species = "Elephant",
        severity = IncidentSeverity.MEDIUM,
        summary = "Test",
        lat = -1.0,
        lng = 29.0,
        reportedAt = Instant.now().toString(),
        userId = userId,
        animalSeen = animalSeen,
        syncStatus = syncStatus,
        lastModified = 0L,
    )

    @Test
    fun `excludes reporter own incidents`() {
        val result = CommunityAlertFilter.shouldShow(
            incident = incident(id = "1", userId = "self"),
            currentUserId = "self",
            dismissedIds = emptySet(),
            seenTimestamps = emptyMap(),
        )
        assertFalse(result)
    }

    @Test
    fun `excludes animal not seen reports`() {
        val result = CommunityAlertFilter.shouldShow(
            incident = incident(id = "1", animalSeen = false),
            currentUserId = "self",
            dismissedIds = emptySet(),
            seenTimestamps = emptyMap(),
        )
        assertFalse(result)
    }

    @Test
    fun `excludes pending sync incidents`() {
        val result = CommunityAlertFilter.shouldShow(
            incident = incident(id = "1", syncStatus = SyncStatus.PENDING),
            currentUserId = "self",
            dismissedIds = emptySet(),
            seenTimestamps = emptyMap(),
        )
        assertFalse(result)
    }

    @Test
    fun `includes synced incidents from other users with animal seen`() {
        val result = CommunityAlertFilter.shouldShow(
            incident = incident(id = "1", userId = "other", animalSeen = true),
            currentUserId = "self",
            dismissedIds = emptySet(),
            seenTimestamps = emptyMap(),
        )
        assertTrue(result)
    }
}
