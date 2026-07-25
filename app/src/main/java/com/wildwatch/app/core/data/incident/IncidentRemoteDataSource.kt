package com.wildwatch.app.core.data.incident

import com.wildwatch.app.core.model.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentRemoteDataSource {
    suspend fun upsert(incident: Incident): Result<Unit>
    fun observeChanges(): Flow<RemoteIncidentChange>
}
