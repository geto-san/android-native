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
    val district: String? = null,
    val subCounty: String? = null,
    val parish: String? = null,
    val community: String,
    val species: String,
    val severity: IncidentSeverity,
    val category: String?,
    val summary: String?,
    val lat: Double,
    val lng: Double,
    val locationName: String?,
    val localImageUris: List<String>,
    val animalSeen: Boolean? = null,
    val answersJson: String? = null,
    val schemaVersion: String? = null,
)

interface IncidentRepository {
    fun observeAll(): Flow<List<Incident>>
    suspend fun getById(id: String): Incident?
    suspend fun create(details: NewIncidentDetails, asDraft: Boolean = false): Incident
    suspend fun update(id: String, details: NewIncidentDetails, asDraft: Boolean = false)
    suspend fun finalizeDraft(id: String)
    suspend fun assignToSelf(id: String)
    suspend fun syncPending(): SyncResult
    fun startObservingRemoteChanges()
}
