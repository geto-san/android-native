package com.wildwatch.app.domain.model

import com.wildwatch.app.data.local.db.NotificationEntity
import com.wildwatch.app.data.local.db.NotificationType

data class Notification(
    val id: String,
    val title: String,
    val type: NotificationType,
    val createdAt: Long,
    val isRead: Boolean,
) {
    companion object {
        fun fromEntity(entity: NotificationEntity): Notification = Notification(
            id = entity.id,
            title = entity.title,
            type = entity.type,
            createdAt = entity.createdAt,
            isRead = entity.isRead,
        )
    }
}
