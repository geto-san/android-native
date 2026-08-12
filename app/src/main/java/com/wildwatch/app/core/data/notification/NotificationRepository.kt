package com.wildwatch.app.core.data.notification

import com.wildwatch.app.core.database.NotificationType
import com.wildwatch.app.core.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeAll(): Flow<List<Notification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markRead(id: String)
    suspend fun notifyPendingSync(incidentId: String)

    // Notifications are entirely local/device-scoped (no per-user column - see
    // NotificationEntity), so signing out has nothing else to key a filter on. Called from
    // AuthRepositoryImpl.signOut() so the next account (or a guest) doesn't see a previous
    // account's notification history.
    suspend fun clearAll()

    // Persists a push notification received via WildWatchMessagingService so
    // it shows up in NotificationsScreen/unread count - separate from
    // notifyPendingSync (a locally-originated, device-only notification).
    suspend fun recordIncoming(type: NotificationType, title: String, message: String, targetId: String?)
}
