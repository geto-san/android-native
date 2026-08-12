package com.wildwatch.app.core.model

import com.wildwatch.app.core.database.RangerProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncidentTest {

    private fun baseDocument(overrides: Map<String, Any?> = emptyMap()): Map<String, Any?> =
        mapOf(
            "type" to "sighting",
            "status" to "in_progress",
            "park" to "BWINDI_IMPENETRABLE",
            "community" to "Buhoma",
            "species" to "Elephant",
            "lat" to -1.05,
            "lng" to 29.7,
            "reportedAt" to "2026-08-11T00:00:00Z",
        ) + overrides

    @Test
    fun `fromFirestoreDocument parses en_route ranger progress`() {
        val incident = Incident.fromFirestoreDocument("doc1", baseDocument(mapOf("rangerProgress" to "en_route")))
        assertEquals(RangerProgress.EN_ROUTE, incident.rangerProgress)
    }

    @Test
    fun `fromFirestoreDocument parses on_site ranger progress`() {
        val incident = Incident.fromFirestoreDocument("doc1", baseDocument(mapOf("rangerProgress" to "on_site")))
        assertEquals(RangerProgress.ON_SITE, incident.rangerProgress)
    }

    @Test
    fun `fromFirestoreDocument parses completed ranger progress`() {
        val incident = Incident.fromFirestoreDocument("doc1", baseDocument(mapOf("rangerProgress" to "completed")))
        assertEquals(RangerProgress.COMPLETED, incident.rangerProgress)
    }

    @Test
    fun `fromFirestoreDocument leaves ranger progress null when absent`() {
        val incident = Incident.fromFirestoreDocument("doc1", baseDocument())
        assertNull(incident.rangerProgress)
    }

    @Test
    fun `fromFirestoreDocument reads isEscalated flag independently of status`() {
        val incident = Incident.fromFirestoreDocument(
            "doc1",
            baseDocument(mapOf("status" to "in_progress", "isEscalated" to true)),
        )
        assertEquals(com.wildwatch.app.core.database.IncidentStatus.IN_PROGRESS, incident.status)
        assertEquals(true, incident.isEscalated)
    }
}
