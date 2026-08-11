package com.wildwatch.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Mirrors the Firestore 'patrol_logs' document shape (docs/schema-v1.md) plus
// local-only bookkeeping (syncStatus, lastModified) - same id-is-the-doc-id
// convention as IncidentEntity.
@Entity(tableName = "patrol_logs")
data class PatrolLogEntity(
    @PrimaryKey val id: String,
    val rangerUid: String,
    val parkId: String?,
    val routePoints: List<RoutePoint>,
    val startTime: String,
    val endTime: String?,
    val status: PatrolStatus,
    val syncStatus: SyncStatus,
    val lastModified: Long,
)
