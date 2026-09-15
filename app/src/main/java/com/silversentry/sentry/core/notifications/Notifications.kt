package com.silversentry.sentry.core.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

// Single registry of every notification channel the app uses - modelled on how Mihon
// centralizes its channels (eu.kanade.tachiyomi.data.notification.Notifications): one
// place owns the channel ids + names, and they're created once at app startup
// (SilverBackSentryApplication.onCreate) instead of lazily inside whichever service
// happens to post first.
object Notifications {

    // Incoming remote alerts (incident/SOS pushed from Cloud Functions). HIGH keeps them
    // interrupting; POST_NOTIFICATIONS on Android 13+ is requested on first launch.
    const val CHANNEL_ALERTS = "alerts_channel"

    // Keeps the historical patrol id so the foreground-service notification from
    // PatrolTrackingService resolves to the same channel across upgrades.
    const val CHANNEL_PATROL = "patrol_tracking"

    // Channel ids retired by this registry - deleted on startup so a channel whose id or
    // name changed never leaves a stale duplicate behind in system settings.
    private val deprecatedChannels = listOf("default_channel")

    /**
     * Creates every notification channel the app uses. No-op on Android versions that
     * don't support notification channels (Oreo and below).
     *
     * @param context The application context.
     */
    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        deprecatedChannels.forEach(notificationManager::deleteNotificationChannel)

        notificationManager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_ALERTS, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName("SilverBack Sentry Alerts")
                    .setDescription("Urgent conservation and security updates")
                    .setShowBadge(true)
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_PATROL, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName("Patrol tracking")
                    .setDescription("Shown while background patrol GPS tracking is active")
                    .setShowBadge(false)
                    .build(),
            ),
        )
    }
}
