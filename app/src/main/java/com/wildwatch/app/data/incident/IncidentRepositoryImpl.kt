package com.wildwatch.app.data.incident

import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.local.db.IncidentDao
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.local.db.RangerProgress
import com.wildwatch.app.data.local.db.SyncStatus
import com.wildwatch.app.di.ApplicationScope
import com.wildwatch.app.di.IoDispatcher
import com.wildwatch.app.domain.model.Incident
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepositoryImpl @Inject constructor(
    private val incidentDao: IncidentDao,
    private val remoteDataSource: IncidentRemoteDataSource,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : IncidentRepository {

    // Serializes syncPending() calls (guardrail G3): a second call arriving while
    // one is in flight simply waits its turn and then finds nothing left PENDING,
    // rather than racing the first call over the same rows.
    private val syncMutex = Mutex()

    // Guardrail G5: ids this device is in the middle of syncing for the first
    // time. An "added" snapshot for one of these ids is Firestore echoing back
    // our own just-written doc - Room already has it, so it's dropped, and the
    // id is removed from the set immediately after. Any *later* change to that
    // same id (a teammate updating its status, for example) is no longer in
    // this set, so it is never suppressed - only the initial echo is.
    private val pendingOwnWriteIds = ConcurrentHashMap.newKeySet<String>()
    private var remoteChangesJob: Job? = null

    override fun observeAll(): Flow<List<Incident>> =
        incidentDao.observeAll().map { entities -> entities.map(Incident::fromEntity) }

    override suspend fun create(details: NewIncidentDetails): Incident = withContext(ioDispatcher) {
        val user = authRepository.currentUser.value
        val incident = Incident(
            id = UUID.randomUUID().toString(), // guardrail G4
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
            userName = user?.displayNameOrFallback,
            userEmail = user?.email,
            userId = user?.uid,
            reportedAt = Instant.now().toString(),
            localImageUris = details.localImageUris,
            syncStatus = SyncStatus.PENDING,
            lastModified = System.currentTimeMillis(),
        )
        incidentDao.insert(incident.toEntity())
        incident
    }

    override suspend fun assignToSelf(id: String) = withContext(ioDispatcher) {
        val user = authRepository.currentUser.value
        val entity = incidentDao.getById(id) ?: return@withContext
        val updated = Incident.fromEntity(entity).copy(
            status = IncidentStatus.IN_PROGRESS,
            rangerProgress = RangerProgress.EN_ROUTE,
            assignedTo = user?.uid,
            assignedToName = user?.displayNameOrFallback,
        )
        incidentDao.update(updated.toEntity())
        if (entity.syncStatus == SyncStatus.SYNCED) {
            runCatching {
                remoteDataSource.writeDocument(id, updated.toFirestoreMap())
            }.onFailure { Timber.e(it, "Failed to push assignment update for incident %s", id) }
        }
        // If it hasn't synced yet, the assignment fields are already part of
        // this row and will go out with its normal syncPending() write - no
        // separate remote call needed.
    }

    override suspend fun syncPending(): SyncResult = withContext(ioDispatcher) {
        syncMutex.withLock {
            val pending = incidentDao.getBySyncStatus(SyncStatus.PENDING)
            var succeeded = 0
            var failed = 0

            for (entity in pending) {
                incidentDao.updateSyncStatus(entity.id, SyncStatus.SYNCING)
                try {
                    val uploadedUrls = remoteDataSource.uploadImages(entity.localImageUris, entity.id)
                    val allEvidenceUrls = entity.evidencePhotoUrls + uploadedUrls
                    val syncedAt = Instant.now().toString()
                    val incidentToSync = Incident.fromEntity(entity).copy(
                        evidencePhotoUrls = allEvidenceUrls,
                        syncedAt = syncedAt,
                    )

                    // Must be set before writeDocument(), not after: the echo can
                    // arrive on the remote-changes listener before this call even
                    // returns (guardrail G5).
                    pendingOwnWriteIds.add(entity.id)
                    remoteDataSource.writeDocument(entity.id, incidentToSync.toFirestoreMap())

                    incidentDao.markSynced(
                        id = entity.id,
                        syncStatus = SyncStatus.SYNCED,
                        syncedAt = syncedAt,
                        evidencePhotoUrls = allEvidenceUrls,
                        hasEvidence = allEvidenceUrls.isNotEmpty(),
                        evidenceCount = allEvidenceUrls.size,
                    )
                    succeeded++
                } catch (
                    @Suppress("TooGenericExceptionCaught")
                    e: Exception,
                ) {
                    // Deliberately broad: any failure here (network, Firestore,
                    // Storage, serialization) must revert this row to PENDING and
                    // let the loop continue to the next incident, rather than
                    // crash the whole sync pass over one bad row.
                    Timber.e(e, "Sync failed for incident %s", entity.id)
                    // guardrail G3: revert to PENDING so it's picked up by the next
                    // sync attempt, rather than silently staying stuck in SYNCING.
                    incidentDao.updateSyncStatus(entity.id, SyncStatus.PENDING)
                    // No doc was written, so no echo is coming - don't leave this id
                    // sitting in the set forever.
                    pendingOwnWriteIds.remove(entity.id)
                    failed++
                }
            }

            SyncResult(succeeded = succeeded, failed = failed)
        }
    }

    override fun startObservingRemoteChanges() {
        if (remoteChangesJob != null) return
        remoteChangesJob = applicationScope.launch {
            remoteDataSource.observeChanges().collect { changes ->
                for (change in changes) {
                    applyRemoteChange(change)
                }
            }
        }
    }

    private suspend fun applyRemoteChange(change: RemoteIncidentChange) {
        when (change) {
            is RemoteIncidentChange.Added -> {
                if (pendingOwnWriteIds.remove(change.id)) {
                    // Firestore echoing back our own just-synced doc (guardrail
                    // G5) - Room already has it via create()/syncPending(), so
                    // there is nothing to insert. The id is now gone from the set,
                    // so a *later* change to this same id is never suppressed.
                    return
                }
                if (incidentDao.getById(change.id) == null) {
                    val incident = Incident.fromFirestoreDocument(change.id, change.data)
                    incidentDao.insert(incident.toEntity())
                }
            }

            is RemoteIncidentChange.Modified -> {
                applyModifiedChange(change)
            }

            is RemoteIncidentChange.Removed -> {
                incidentDao.deleteById(change.id)
            }
        }
    }

    private suspend fun applyModifiedChange(change: RemoteIncidentChange.Modified) {
        val existing = incidentDao.getById(change.id) ?: return
        val incoming = Incident.fromFirestoreDocument(change.id, change.data)
        // Preserve this device's own sync bookkeeping - a remote status update
        // (e.g. a teammate assigning or resolving it) is about the document's
        // content, not about whether this device still has local-only photos
        // to upload.
        val merged = incoming.copy(
            syncStatus = existing.syncStatus,
            localImageUris = existing.localImageUris,
        )
        incidentDao.update(merged.toEntity())
    }
}
