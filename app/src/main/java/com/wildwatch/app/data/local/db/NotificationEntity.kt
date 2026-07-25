package com.wildwatch.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// wireframe community.notifications - a personal activity feed (sighting
// confirmations, claim updates, nearby alerts, patrol notices) distinct from
// AlertEntity's broadcast Community Alerts. Same "seed on first read" pattern
// as AlertRepositoryImpl since there's no backend service producing these
// yet (see NotificationRepositoryImpl).
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: NotificationType,
    val createdAt: Long,
    val isRead: Boolean,
)
