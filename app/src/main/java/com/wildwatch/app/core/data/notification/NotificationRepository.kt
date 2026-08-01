package com.wildwatch.app.core.data.notification

import com.wildwatch.app.core.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeAll(): Flow<List<Notification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markRead(id: String)
    suspend fun notifyPendingSync(incidentId: String)
}
