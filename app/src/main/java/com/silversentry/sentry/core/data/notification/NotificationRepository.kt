package com.silversentry.sentry.core.data.notification

import com.silversentry.sentry.core.database.NotificationType
import com.silversentry.sentry.core.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeAll(): Flow<List<Notification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markRead(id: String)

    // Notifications are entirely local/device-scoped (no per-user column - see
    // NotificationEntity), so signing out has nothing else to key a filter on. Called from
    // AuthRepositoryImpl.signOut() so the next account (or a guest) doesn't see a previous
    // account's notification history.
    suspend fun clearAll()

    // Persists a push notification received via SilverBackSentryMessagingService so
    // it shows up in NotificationsScreen/unread count. Locally-originated notifications
    // (e.g. "report queued for upload" on creation) are deliberately NOT persisted -
    // matching how Mihon never notifies on entity creation, only on progress/errors.
    suspend fun recordIncoming(type: NotificationType, title: String, message: String, targetId: String?)
}
