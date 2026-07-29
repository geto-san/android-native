package com.wildwatch.app.core.data.incident

import com.wildwatch.app.core.database.IncidentDao
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.RangerProgress
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.di.ApplicationScope
import com.wildwatch.app.core.di.IoDispatcher
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.location.LocationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepositoryImpl @Inject constructor(
    private val incidentDao: IncidentDao,
    private val remoteDataSource: IncidentRemoteDataSource,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : IncidentRepository {

    private val syncMutex = Mutex()

    override fun observeAll(): Flow<List<Incident>> =
        incidentDao.observeAll()
            .map { entities -> entities.map(Incident::fromEntity) }

    override suspend fun create(details: NewIncidentDetails): Incident = withContext(ioDispatcher) {
        val user = authRepository.currentUser.first()
        val incident = Incident(
            id = java.util.UUID.randomUUID().toString(),
            type = details.type,
            status = IncidentStatus.OPEN,
            park = details.park,
            community = details.community,
            species = details.species,
            severity = details.severity,
            category = details.category,
            summary = details.summary,
            lat = details.lat,
            lng = details.lng,
            locationName = details.locationName,
            userName = user?.displayName,
            userEmail = user?.email,
            userId = user?.uid,
            reportedAt = java.time.Instant.now().toString(),
            localImageUris = details.localImageUris,
            syncStatus = SyncStatus.PENDING,
            lastModified = System.currentTimeMillis()
        )
        incidentDao.insert(incident.toEntity())
        incident
    }

    override suspend fun assignToSelf(id: String) = withContext(ioDispatcher) {
        val user = authRepository.currentUser.first() ?: return@withContext
        val entity = incidentDao.getById(id) ?: return@withContext
        val updated = entity.copy(
            status = IncidentStatus.IN_PROGRESS,
            assignedTo = user.uid,
            assignedToName = user.displayName ?: user.email,
            rangerProgress = RangerProgress.EN_ROUTE,
            syncStatus = if (entity.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_UPDATE else entity.syncStatus,
            lastModified = System.currentTimeMillis()
        )
        incidentDao.insert(updated)
    }

    override suspend fun syncPending(): SyncResult = syncMutex.withLock {
        withContext(ioDispatcher) {
            val pending = incidentDao.getBySyncStatus(SyncStatus.PENDING)
            val pendingUpdates = incidentDao.getBySyncStatus(SyncStatus.PENDING_UPDATE)
            
            var succeeded = 0
            var failed = 0
            
            (pending + pendingUpdates).forEach { entity ->
                val result = remoteDataSource.upsert(Incident.fromEntity(entity))
                if (result.isSuccess) {
                    incidentDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                    succeeded++
                } else {
                    failed++
                }
            }
            SyncResult(succeeded, failed)
        }
    }

    override fun startObservingRemoteChanges() {
        applicationScope.launch {
            remoteDataSource.observeChanges().collect { change ->
                incidentDao.insert(change.incident.toEntity())
            }
        }
    }
}
