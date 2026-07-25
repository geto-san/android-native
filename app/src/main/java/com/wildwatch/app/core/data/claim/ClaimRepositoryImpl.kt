package com.wildwatch.app.core.data.claim

import com.wildwatch.app.core.database.ClaimDao
import com.wildwatch.app.core.database.ClaimEntity
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.di.IoDispatcher
import com.wildwatch.app.core.model.Claim
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaimRepositoryImpl @Inject constructor(
    private val claimDao: ClaimDao,
    private val remoteDataSource: ClaimRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ClaimRepository {

    override fun observeAll(): Flow<List<Claim>> =
        claimDao.observeAll().map { entities -> entities.map(Claim::fromEntity) }

    override suspend fun create(claim: Claim) = withContext(ioDispatcher) {
        claimDao.insert(claim.toEntity())
    }

    override suspend fun syncPending(): Int = withContext(ioDispatcher) {
        val pending = claimDao.getBySyncStatus(SyncStatus.PENDING)
        var succeeded = 0
        pending.forEach { entity ->
            val result = remoteDataSource.upsert(Claim.fromEntity(entity))
            if (result.isSuccess) {
                claimDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                succeeded++
            }
        }
        succeeded
    }
}
