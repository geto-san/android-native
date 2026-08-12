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
    // Deep-link target, meaning depends on `type` (see NotificationPayload) -
    // e.g. an incident id for SIGHTING_APPROVED, an article id for
    // NEW_FEED_ARTICLE. Null when this notification has no specific screen to
    // open, or the sender's payload didn't carry a navigable id for its type.
    val targetId: String? = null,
)
