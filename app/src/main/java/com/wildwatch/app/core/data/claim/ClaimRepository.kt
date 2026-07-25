package com.wildwatch.app.core.data.claim

import com.wildwatch.app.core.model.Claim
import kotlinx.coroutines.flow.Flow

interface ClaimRepository {
    fun observeAll(): Flow<List<Claim>>
    suspend fun create(claim: Claim)
    suspend fun syncPending(): Int
}
