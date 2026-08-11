package com.wildwatch.app.core.data.patrol

import com.wildwatch.app.core.database.RoutePoint
import com.wildwatch.app.core.model.PatrolLog
import kotlinx.coroutines.flow.Flow

data class PatrolSyncResult(val succeeded: Int, val failed: Int)

interface PatrolRepository {
    fun observeActivePatrol(rangerUid: String): Flow<PatrolLog?>
    suspend fun startPatrol(rangerUid: String, parkId: String?): PatrolLog
    suspend fun appendPoint(patrolId: String, point: RoutePoint)
    suspend fun stopPatrol(patrolId: String)
    suspend fun syncPending(): PatrolSyncResult
}
