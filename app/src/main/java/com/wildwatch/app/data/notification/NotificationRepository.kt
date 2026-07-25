package com.wildwatch.app.data.notification

import com.wildwatch.app.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    fun observeAll(): Flow<List<Notification>>

    fun observeUnreadCount(): Flow<Int>

    suspend fun markAsRead(id: String)
}
