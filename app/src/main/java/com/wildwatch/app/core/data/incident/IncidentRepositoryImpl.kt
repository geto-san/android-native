package com.wildwatch.app.core.data.incident

import com.wildwatch.app.core.data.bridge.LaravelBridgeDataSource
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepositoryImpl @Inject constructor(
    private val incidentDao: IncidentDao,
    private val remoteDataSource: IncidentRemoteDataSource,
    private val laravelBridgeDataSource: LaravelBridgeDataSource,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : IncidentRepository {

    init {
        startObservingRemoteChanges()
    }

    private val syncMutex = Mutex()

    // Shared rather than a fresh cold Flow per collector: HomeViewModel, DashboardViewModel,
    // ProfileViewModel, and RangerTrackingViewModel all observe this independently, and without
    // sharing, every incident write re-triggers Room's invalidation tracker once per subscriber
    // instead of once total - a real source of app-wide jank on top of whatever screen actually
    // wrote the change.
    override fun observeAll(): Flow<List<Incident>> =
        incidentDao.observeAll()
            .map { entities -> entities.map(Incident::fromEntity) }
            .shareIn(applicationScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    override suspend fun getById(id: String): Incident? = withContext(ioDispatcher) {
        incidentDao.getById(id)?.let(Incident::fromEntity)
    }

    override suspend fun create(details: NewIncidentDetails, asDraft: Boolean): Incident = withContext(ioDispatcher) {
        val user = authRepository.currentUser.first()
        val incident = Incident(
            id = java.util.UUID.randomUUID().toString(),
            // ...
            type = details.type,
            status = IncidentStatus.OPEN,
            park = details.park,
            district = details.district,
            subCounty = details.subCounty,
            parish = details.parish,
            animalSeen = details.animalSeen,
            answersJson = details.answersJson,
            schemaVersion = details.schemaVersion,
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
            syncStatus = if (asDraft) SyncStatus.DRAFT else SyncStatus.PENDING,
            lastModified = System.currentTimeMillis()
        )
        incidentDao.insert(incident.toEntity())
        incident
    }

    override suspend fun update(id: String, details: NewIncidentDetails, asDraft: Boolean) = withContext(ioDispatcher) {
        val existing = incidentDao.getById(id) ?: return@withContext
        val updated = existing.copy(
            type = details.type,
            park = details.park,
            district = details.district,
            subCounty = details.subCounty,
            parish = details.parish,
            animalSeen = details.animalSeen,
            answersJson = details.answersJson,
            schemaVersion = details.schemaVersion,
            community = details.community,
            species = details.species,
            severity = details.severity,
            category = details.category,
            summary = details.summary,
            lat = details.lat,
            lng = details.lng,
            locationName = details.locationName,
            localImageUris = details.localImageUris,
            syncStatus = if (asDraft) SyncStatus.DRAFT else SyncStatus.PENDING,
            lastModified = System.currentTimeMillis()
        )
        incidentDao.update(updated)
    }

    override suspend fun finalizeDraft(id: String) = withContext(ioDispatcher) {
        incidentDao.updateSyncStatus(id, SyncStatus.PENDING)
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
                val eventType = if (entity.syncStatus == SyncStatus.PENDING) "create" else "update"

                val firestoreResult = remoteDataSource.upsert(Incident.fromEntity(entity))
                val syncedIncident = firestoreResult.getOrNull()
                if (syncedIncident == null) {
                    failed++
                    return@forEach
                }

                // Persisted regardless of the Laravel leg's outcome below, so a retry never
                // re-uploads images that already made it to Storage (see updateEvidenceBookkeeping).
                incidentDao.updateEvidenceBookkeeping(
                    id = syncedIncident.id,
                    evidencePhotoUrls = syncedIncident.evidencePhotoUrls,
                    hasEvidence = syncedIncident.hasEvidence,
                    evidenceCount = syncedIncident.evidenceCount,
                    localImageUris = syncedIncident.localImageUris,
                )

                val laravelResult = laravelBridgeDataSource.postIncidentEvent(syncedIncident, eventType)
                if (laravelResult.isSuccess) {
                    incidentDao.markSynced(
                        id = syncedIncident.id,
                        syncStatus = SyncStatus.SYNCED,
                        syncedAt = java.time.Instant.now().toString(),
                        evidencePhotoUrls = syncedIncident.evidencePhotoUrls,
                        hasEvidence = syncedIncident.hasEvidence,
                        evidenceCount = syncedIncident.evidenceCount,
                        localImageUris = syncedIncident.localImageUris,
                    )
                    succeeded++
                } else {
                    Timber.w(
                        laravelResult.exceptionOrNull(),
                        "Laravel bridge call failed for incident %s; will retry",
                        syncedIncident.id,
                    )
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
