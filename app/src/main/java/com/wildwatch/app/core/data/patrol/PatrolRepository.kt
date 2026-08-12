package com.wildwatch.app.core.data.patrol

import com.wildwatch.app.core.database.RoutePoint
import com.wildwatch.app.core.model.PatrolLog
import kotlinx.coroutines.flow.Flow

data class PatrolSyncResult(val succeeded: Int, val failed: Int)

interface PatrolRepository {
    fun observeActivePatrol(rangerUid: String): Flow<PatrolLog?>
    suspend fun startPatrol(rangerUid: String, parkId: String?): PatrolLog

    // Idempotent entry point for PatrolTrackingService.onStartCommand(): if the
    // service process was killed mid-patrol (OS memory pressure, force-stop)
    // and later restarted, Room still has that patrol as ACTIVE - resuming it
    // rather than calling startPatrol() again avoids forking a second,
    // never-completed row and preserves the existing route on the map.
    suspend fun resumeOrStartPatrol(rangerUid: String, parkId: String?): PatrolLog
    suspend fun appendPoint(patrolId: String, point: RoutePoint)
    suspend fun stopPatrol(patrolId: String)
    suspend fun syncPending(): PatrolSyncResult
}
