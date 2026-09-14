package com.silversentry.sentry.core.data.patrol

import com.silversentry.sentry.core.model.PatrolLog

interface PatrolRemoteDataSource {
    suspend fun upsert(patrolLog: PatrolLog): Result<Unit>
}
