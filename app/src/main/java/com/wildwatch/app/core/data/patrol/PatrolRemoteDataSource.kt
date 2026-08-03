package com.wildwatch.app.core.data.patrol

import com.wildwatch.app.core.model.PatrolLog

interface PatrolRemoteDataSource {
    suspend fun upsert(patrolLog: PatrolLog): Result<Unit>
}
