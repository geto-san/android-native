package com.wildwatch.app.core.notifications

import com.wildwatch.app.core.database.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationPayloadTest {

    @Test
    fun `parseType maps a matching string to its enum value`() {
        assertEquals(NotificationType.SECURITY_ALERT, NotificationPayload.parseType("SECURITY_ALERT"))
        assertEquals(NotificationType.SIGHTING_APPROVED, NotificationPayload.parseType("SIGHTING_APPROVED"))
        assertEquals(NotificationType.NEW_FEED_ARTICLE, NotificationPayload.parseType("NEW_FEED_ARTICLE"))
    }

    @Test
    fun `parseType returns null for an unknown or missing type string`() {
        assertNull(NotificationPayload.parseType("NOT_A_REAL_TYPE"))
        assertNull(NotificationPayload.parseType(null))
    }

    @Test
    fun `targetId reads incidentId for SIGHTING_APPROVED sourced from an incident`() {
        val targetId = NotificationPayload.targetId(
            NotificationType.SIGHTING_APPROVED,
            mapOf("incidentId" to "inc-1"),
        )
        assertEquals("inc-1", targetId)
    }

    @Test
    fun `targetId is null for SIGHTING_APPROVED sourced only from a wildlife_sightings row`() {
        val targetId = NotificationPayload.targetId(
            NotificationType.SIGHTING_APPROVED,
            mapOf("sightingId" to "sight-1"),
        )
        assertNull(targetId)
    }

    @Test
    fun `targetId reads articleId for NEW_FEED_ARTICLE`() {
        val targetId = NotificationPayload.targetId(
            NotificationType.NEW_FEED_ARTICLE,
            mapOf("articleId" to "art-1"),
        )
        assertEquals("art-1", targetId)
    }

    @Test
    fun `targetId is null for SECURITY_ALERT regardless of which id field is present`() {
        assertNull(NotificationPayload.targetId(NotificationType.SECURITY_ALERT, mapOf("incidentId" to "inc-1")))
        assertNull(NotificationPayload.targetId(NotificationType.SECURITY_ALERT, mapOf("sosId" to "sos-1")))
    }

    @Test
    fun `targetId is null when type is null`() {
        assertNull(NotificationPayload.targetId(null, mapOf("incidentId" to "inc-1")))
    }
}
