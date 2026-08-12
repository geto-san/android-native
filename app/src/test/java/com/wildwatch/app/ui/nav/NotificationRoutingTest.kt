package com.wildwatch.app.ui.nav

import com.wildwatch.app.core.database.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRoutingTest {

    @Test
    fun `SECURITY_ALERT always routes to CommunityAlerts`() {
        assertEquals(Route.CommunityAlerts, routeForNotification(NotificationType.SECURITY_ALERT, null))
        assertEquals(Route.CommunityAlerts, routeForNotification(NotificationType.SECURITY_ALERT, "inc-1"))
    }

    @Test
    fun `NEW_FEED_ARTICLE with a targetId routes to ArticleDetail`() {
        assertEquals(Route.ArticleDetail("art-1"), routeForNotification(NotificationType.NEW_FEED_ARTICLE, "art-1"))
    }

    @Test
    fun `NEW_FEED_ARTICLE without a targetId routes nowhere`() {
        assertNull(routeForNotification(NotificationType.NEW_FEED_ARTICLE, null))
    }

    @Test
    fun `SIGHTING_APPROVED with a targetId routes to IncidentDetail`() {
        assertEquals(Route.IncidentDetail("inc-1"), routeForNotification(NotificationType.SIGHTING_APPROVED, "inc-1"))
    }

    @Test
    fun `SIGHTING_APPROVED without a targetId routes nowhere`() {
        assertNull(routeForNotification(NotificationType.SIGHTING_APPROVED, null))
    }

    @Test
    fun `an unhandled or null type routes nowhere`() {
        assertNull(routeForNotification(NotificationType.SYSTEM, "whatever"))
        assertNull(routeForNotification(null, "whatever"))
    }
}
