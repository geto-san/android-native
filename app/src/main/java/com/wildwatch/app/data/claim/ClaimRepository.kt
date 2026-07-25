package com.wildwatch.app.data.claim

import com.wildwatch.app.data.local.db.ClaimCategory
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.domain.model.Claim
import kotlinx.coroutines.flow.Flow

data class SyncResult(val succeeded: Int, val failed: Int)

data class NewClaimDetails(
    val category: ClaimCategory,
    val park: Park,
    val description: String?,
    val lat: Double?,
    val lng: Double?,
    val locationName: String?,
    val relatedIncidentId: String?,
    val localImageUris: List<String>,
)

// Mirrors IncidentRepository - see its own doc comment.
interface ClaimRepository {

    fun observeAllForCurrentUser(): Flow<List<Claim>>

    suspend fun create(details: NewClaimDetails): Claim

    suspend fun syncPending(): SyncResult

    fun startObservingRemoteChanges()
}
