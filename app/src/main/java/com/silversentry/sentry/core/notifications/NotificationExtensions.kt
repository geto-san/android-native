package com.silversentry.sentry.core.notifications

import android.Manifest
import android.app.Notification
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker

// Mihon-style notification helpers: every caller builds on channel ids owned by
// Notifications and posts through notify(), which silently no-ops when the Android 13+
// POST_NOTIFICATIONS runtime grant is missing - a reporter or background service never
// crashes because the OS denied notifications.
fun Context.notify(id: Int, notification: Notification) {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        PermissionChecker.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PermissionChecker.PERMISSION_GRANTED
    ) {
        return
    }

    NotificationManagerCompat.from(this).notify(id, notification)
}

/**
 * Helper to create a notification builder.
 *
 * @param channelId the channel id (owned by [Notifications]).
 * @param block the function that will execute inside the builder.
 * @return a notification builder to be displayed or updated.
 */
fun Context.notificationBuilder(
    channelId: String,
    block: (NotificationCompat.Builder.() -> Unit)? = null,
): NotificationCompat.Builder {
    val builder = NotificationCompat.Builder(this, channelId)
    if (block != null) {
        builder.block()
    }
    return builder
}
