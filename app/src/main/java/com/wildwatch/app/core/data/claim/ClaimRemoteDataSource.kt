package com.wildwatch.app.core.data.claim

import com.wildwatch.app.core.model.Claim

interface ClaimRemoteDataSource {
    suspend fun upsert(claim: Claim): Result<Unit>
}
