package com.wildwatch.app.core.data.incident

import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.model.Incident
import kotlinx.coroutines.flow.Flow

data class SyncResult(val succeeded: Int, val failed: Int)

data class NewIncidentDetails(
    val type: IncidentType,
    val park: Park,
    val community: String,
    val species: String,
    val severity: IncidentSeverity,
    val category: String?,
    val summary: String?,
    val lat: Double,
    val lng: Double,
    val locationName: String?,
    val localImageUris: List<String>,
)

interface IncidentRepository {
    fun observeAll(): Flow<List<Incident>>
    suspend fun create(details: NewIncidentDetails): Incident
    suspend fun assignToSelf(id: String)
    suspend fun syncPending(): SyncResult
    fun startObservingRemoteChanges()
}
