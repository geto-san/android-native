package com.silversentry.sentry.core.model

import com.silversentry.sentry.core.database.NotificationEntity
import com.silversentry.sentry.core.database.NotificationType

data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Long,
    val targetId: String? = null,
) {
    companion object {
        fun fromEntity(entity: NotificationEntity): Notification = Notification(
            id = entity.id,
            type = entity.type,
            title = entity.title,
            message = entity.message,
            isRead = entity.isRead,
            createdAt = entity.createdAt,
            targetId = entity.targetId,
        )
    }
}
