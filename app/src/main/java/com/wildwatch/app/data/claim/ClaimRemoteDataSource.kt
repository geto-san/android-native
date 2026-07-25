package com.wildwatch.app.data.claim

import kotlinx.coroutines.flow.Flow

// Mirrors IncidentRemoteDataSource - see its own doc comment.
interface ClaimRemoteDataSource {

    suspend fun uploadImages(localUris: List<String>, claimId: String): List<String>

    suspend fun writeDocument(id: String, data: Map<String, Any?>)

    fun observeChanges(): Flow<List<RemoteClaimChange>>
}
