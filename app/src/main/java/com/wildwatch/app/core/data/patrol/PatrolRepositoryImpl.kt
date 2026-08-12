package com.wildwatch.app.core.data.patrol

import com.wildwatch.app.core.database.PatrolLogDao
import com.wildwatch.app.core.database.PatrolStatus
import com.wildwatch.app.core.database.RoutePoint
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.di.IoDispatcher
import com.wildwatch.app.core.model.PatrolLog
import com.wildwatch.app.core.model.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatrolRepositoryImpl @Inject constructor(
    private val patrolLogDao: PatrolLogDao,
    private val remoteDataSource: PatrolRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PatrolRepository {

    override fun observeActivePatrol(rangerUid: String): Flow<PatrolLog?> =
        patrolLogDao.observeActiveForRanger(rangerUid).map { it?.let(PatrolLog::fromEntity) }

    override suspend fun startPatrol(rangerUid: String, parkId: String?): PatrolLog = withContext(ioDispatcher) {
        val patrol = PatrolLog(
            id = UUID.randomUUID().toString(),
            rangerUid = rangerUid,
            parkId = parkId,
            routePoints = emptyList(),
            startTime = Instant.now().toString(),
            status = PatrolStatus.ACTIVE,
            syncStatus = SyncStatus.PENDING,
            lastModified = System.currentTimeMillis(),
        )
        patrolLogDao.insert(patrol.toEntity())
        patrol
    }

    override suspend fun resumeOrStartPatrol(rangerUid: String, parkId: String?): PatrolLog {
        val existing = withContext(ioDispatcher) {
            patrolLogDao.observeActiveForRanger(rangerUid).first()
        }
        return existing?.let(PatrolLog::fromEntity) ?: startPatrol(rangerUid, parkId)
    }

    override suspend fun appendPoint(patrolId: String, point: RoutePoint) = withContext(ioDispatcher) {
        val existing = patrolLogDao.getById(patrolId) ?: return@withContext
        patrolLogDao.insert(
            existing.copy(
                routePoints = existing.routePoints + point,
                lastModified = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun stopPatrol(patrolId: String) = withContext(ioDispatcher) {
        val existing = patrolLogDao.getById(patrolId) ?: return@withContext
        patrolLogDao.insert(
            existing.copy(
                status = PatrolStatus.COMPLETED,
                endTime = Instant.now().toString(),
                syncStatus = SyncStatus.PENDING,
                lastModified = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun syncPending(): PatrolSyncResult = withContext(ioDispatcher) {
        val pending = patrolLogDao.getBySyncStatus(SyncStatus.PENDING)
        var succeeded = 0
        var failed = 0
        pending.forEach { entity ->
            val result = remoteDataSource.upsert(PatrolLog.fromEntity(entity))
            if (result.isSuccess) {
                patrolLogDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                succeeded++
            } else {
                failed++
            }
        }
        PatrolSyncResult(succeeded, failed)
    }
}
