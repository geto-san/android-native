package com.wildwatch.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: Long,
)
