package com.silversentry.sentry.core.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.silversentry.sentry.MainActivity
import com.silversentry.sentry.R
import com.silversentry.sentry.core.data.notification.NotificationRepository
import com.silversentry.sentry.core.database.NotificationType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("DEPRECATION")
class SilverBackSentryMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("From: ${message.from}")

        val type = NotificationPayload.parseType(message.data["type"])
        val targetId = NotificationPayload.targetId(type, message.data)

        message.notification?.let {
            val title = it.title ?: "SilverBack Sentry"
            val body = it.body ?: ""
            showNotification(title, body, type, targetId)

            if (type != null) {
                serviceScope.launch {
                    notificationRepository.recordIncoming(type, title, body, targetId)
                }
            }
        }

        if (message.data.isNotEmpty()) {
            Timber.d("Message data payload: ${message.data}")
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        type: NotificationType?,
        targetId: String?,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_NOTIFICATION_TYPE, type?.name)
            putExtra(EXTRA_NOTIFICATION_TARGET_ID, targetId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Channels are created once up-front in Notifications.createChannels()
        // (SilverBackSentryApplication.onCreate) - nothing creates them lazily at
        // post time anymore, and notify() drops the notification (instead of
        // crashing) if the Android 13+ POST_NOTIFICATIONS grant is missing.
        val notification = notificationBuilder(Notifications.CHANNEL_ALERTS) {
            setSmallIcon(R.drawable.ic_stat_notification)
            setContentTitle(title)
            setContentText(body)
            setAutoCancel(true)
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setDefaults(NotificationCompat.DEFAULT_ALL)
            setContentIntent(pendingIntent)
        }.build()

        notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed")
        serviceScope.launch {
            fcmTokenRepository.syncToken(token)
        }
    }

    companion object {
        const val EXTRA_NOTIFICATION_TYPE = "com.silversentry.sentry.extra.NOTIFICATION_TYPE"
        const val EXTRA_NOTIFICATION_TARGET_ID = "com.silversentry.sentry.extra.NOTIFICATION_TARGET_ID"
    }
}
