package com.wildwatch.app.core.data.notification

import com.wildwatch.app.core.database.NotificationType
import com.wildwatch.app.core.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeAll(): Flow<List<Notification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markRead(id: String)
    suspend fun notifyPendingSync(incidentId: String)

    // Persists a push notification received via WildWatchMessagingService so
    // it shows up in NotificationsScreen/unread count - separate from
    // notifyPendingSync (a locally-originated, device-only notification).
    suspend fun recordIncoming(type: NotificationType, title: String, message: String, targetId: String?)
}
