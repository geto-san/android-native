package com.wildwatch.app.core.model

import com.wildwatch.app.core.database.NotificationEntity
import com.wildwatch.app.core.database.NotificationType

data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Long,
) {
    companion object {
        fun fromEntity(entity: NotificationEntity): Notification = Notification(
            id = entity.id,
            type = entity.type,
            title = entity.title,
            message = entity.message,
            isRead = entity.isRead,
            createdAt = entity.createdAt,
        )
    }
}
