package com.wildwatch.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Community Alerts are ranger/UWA-authored broadcast content, not something
// this citizen-facing app lets a user create - there is no admin tool behind
// it yet, so AlertRepositoryImpl seeds this table locally on first read
// instead of syncing it from Firestore (see that class's doc comment).
@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val location: String,
    val category: AlertCategory,
    val severity: AlertSeverity,
    val createdAt: Long,
)
