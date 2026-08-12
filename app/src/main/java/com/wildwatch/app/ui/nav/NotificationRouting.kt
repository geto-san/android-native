package com.wildwatch.app.ui.nav

import com.wildwatch.app.core.database.NotificationType

// Turns a push notification's (type, targetId) - already resolved by
// NotificationPayload from the raw FCM data payload - into the screen a tap
// should open. Null means "no specific screen", e.g. a type with no handler
// or a targetId-requiring type that didn't get one (see NotificationPayload's
// SIGHTING_APPROVED case).
fun routeForNotification(type: NotificationType?, targetId: String?): Route? = when (type) {
    NotificationType.SECURITY_ALERT -> Route.CommunityAlerts
    NotificationType.NEW_FEED_ARTICLE -> targetId?.let(Route::ArticleDetail)
    NotificationType.SIGHTING_APPROVED -> targetId?.let(Route::IncidentDetail)
    else -> null
}
