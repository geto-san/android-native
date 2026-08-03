package com.wildwatch.app.core.model

import com.wildwatch.app.core.database.PatrolLogEntity
import com.wildwatch.app.core.database.PatrolStatus
import com.wildwatch.app.core.database.RoutePoint
import com.wildwatch.app.core.database.SyncStatus

// Mirrors the documented Firestore 'patrol_logs' shape (docs/schema-v1.md) -
// ranger_uid/route_points/startTime/endTime - plus parkId, which the schema
// doc omits but firestore.rules already requires for warden-scoped reads
// (belongsToPark(resource.data.park_id)); a pre-existing doc/rules gap this
// fills rather than papers over.
data class PatrolLog(
    val id: String,
    val rangerUid: String,
    val parkId: String? = null,
    val routePoints: List<RoutePoint> = emptyList(),
    val startTime: String,
    val endTime: String? = null,
    val status: PatrolStatus = PatrolStatus.ACTIVE,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastModified: Long = 0L,
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "ranger_uid" to rangerUid,
        "park_id" to parkId,
        "route_points" to routePoints.map { mapOf("lat" to it.lat, "lng" to it.lng, "timestamp" to it.timestamp) },
        "startTime" to startTime,
        "endTime" to endTime,
    )

    companion object {
        fun fromEntity(entity: PatrolLogEntity): PatrolLog = PatrolLog(
            id = entity.id,
            rangerUid = entity.rangerUid,
            parkId = entity.parkId,
            routePoints = entity.routePoints,
            startTime = entity.startTime,
            endTime = entity.endTime,
            status = entity.status,
            syncStatus = entity.syncStatus,
            lastModified = entity.lastModified,
        )
    }
}

fun PatrolLog.toEntity(): PatrolLogEntity = PatrolLogEntity(
    id = id,
    rangerUid = rangerUid,
    parkId = parkId,
    routePoints = routePoints,
    startTime = startTime,
    endTime = endTime,
    status = status,
    syncStatus = syncStatus,
    lastModified = lastModified,
)
