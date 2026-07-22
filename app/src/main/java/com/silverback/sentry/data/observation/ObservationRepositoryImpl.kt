package com.silverback.sentry.data.observation

import com.silverback.sentry.data.auth.AuthRepository
import com.silverback.sentry.data.local.db.ObservationDao
import com.silverback.sentry.data.local.db.ObservationStatus
import com.silverback.sentry.data.local.db.SyncStatus
import com.silverback.sentry.di.ApplicationScope
import com.silverback.sentry.di.IoDispatcher
import com.silverback.sentry.domain.model.Observation
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
class ObservationRepositoryImpl @Inject constructor(
    private val observationDao: ObservationDao,
    private val remoteDataSource: ObservationRemoteDataSource,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ObservationRepository {

    // Serializes syncPending() calls (guardrail G3): a second call arriving while
    // one is in flight simply waits its turn and then finds nothing left PENDING,
    // rather than racing the first call over the same rows.
    private val syncMutex = Mutex()

    // Guardrail G5: ids this device is in the middle of syncing for the first
    // time. An "added" snapshot for one of these ids is Firestore echoing back
    // our own just-written doc - Room already has it, so it's dropped, and the
    // id is removed from the set immediately after. Any *later* change to that
    // same id (a teammate marking it attended, for example) is no longer in this
    // set, so it is never suppressed - only the initial echo is.
    private val pendingOwnWriteIds = ConcurrentHashMap.newKeySet<String>()
    private var remoteChangesJob: Job? = null

    override fun observeAll(): Flow<List<Observation>> =
        observationDao.observeAll().map { entities -> entities.map(Observation::fromEntity) }

    override suspend fun create(
        gorillaGroup: String,
        location: String,
        locationName: String?,
        healthStatus: String?,
        notes: String?,
        localImageUris: List<String>,
    ): Observation = withContext(ioDispatcher) {
        val user = authRepository.currentUser.value
        val observation = Observation(
            id = UUID.randomUUID().toString(), // guardrail G4
            gorillaGroup = gorillaGroup,
            location = location,
            locationName = locationName,
            healthStatus = healthStatus,
            notes = notes,
            userName = user?.displayNameOrFallback,
            userEmail = user?.email,
            userId = user?.uid,
            createdAt = Instant.now().toString(),
            observationStatus = ObservationStatus.PENDING,
            localImageUris = localImageUris,
            syncStatus = SyncStatus.PENDING,
            lastModified = System.currentTimeMillis(),
        )
        observationDao.insert(observation.toEntity())
        observation
    }

    override suspend fun markAttended(id: String) = withContext(ioDispatcher) {
        val user = authRepository.currentUser.value
        observationDao.markAttended(
            id = id,
            observationStatus = ObservationStatus.ATTENDED,
            attendedAt = Instant.now().toString(),
            attendedBy = user?.uid,
            attendedByName = user?.displayNameOrFallback,
        )

        val entity = observationDao.getById(id) ?: return@withContext
        if (entity.syncStatus == SyncStatus.SYNCED) {
            runCatching {
                remoteDataSource.writeDocument(id, Observation.fromEntity(entity).toFirestoreMap())
            }.onFailure { Timber.e(it, "Failed to push attended update for observation %s", id) }
        }
        // If it hasn't synced yet, the attended fields are already part of this
        // row and will go out with its normal syncPending() write - no separate
        // remote call needed.
    }

    override suspend fun syncPending(): SyncResult = withContext(ioDispatcher) {
        syncMutex.withLock {
            val pending = observationDao.getBySyncStatus(SyncStatus.PENDING)
            var succeeded = 0
            var failed = 0

            for (entity in pending) {
                observationDao.updateSyncStatus(entity.id, SyncStatus.SYNCING)
                try {
                    val uploadedUrls = remoteDataSource.uploadImages(entity.localImageUris, entity.id)
                    val allImageUrls = entity.imageUrls + uploadedUrls
                    val syncedAt = Instant.now().toString()
                    val observationToSync = Observation.fromEntity(entity).copy(
                        imageUrls = allImageUrls,
                        syncedAt = syncedAt,
                    )

                    // Must be set before writeDocument(), not after: the echo can
                    // arrive on the remote-changes listener before this call even
                    // returns (guardrail G5).
                    pendingOwnWriteIds.add(entity.id)
                    remoteDataSource.writeDocument(entity.id, observationToSync.toFirestoreMap())

                    observationDao.markSynced(
                        id = entity.id,
                        syncStatus = SyncStatus.SYNCED,
                        syncedAt = syncedAt,
                        imageUrls = allImageUrls,
                        hasImages = allImageUrls.isNotEmpty(),
                        imageCount = allImageUrls.size,
                    )
                    succeeded++
                } catch (
                    @Suppress("TooGenericExceptionCaught")
                    e: Exception,
                ) {
                    // Deliberately broad: any failure here (network, Firestore,
                    // Storage, serialization) must revert this row to PENDING and
                    // let the loop continue to the next observation, rather than
                    // crash the whole sync pass over one bad row.
                    Timber.e(e, "Sync failed for observation %s", entity.id)
                    // guardrail G3: revert to PENDING so it's picked up by the next
                    // sync attempt, rather than silently staying stuck in SYNCING.
                    observationDao.updateSyncStatus(entity.id, SyncStatus.PENDING)
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

    private suspend fun applyRemoteChange(change: RemoteObservationChange) {
        when (change) {
            is RemoteObservationChange.Added -> {
                if (pendingOwnWriteIds.remove(change.id)) {
                    // Firestore echoing back our own just-synced doc (guardrail
                    // G5) - Room already has it via create()/syncPending(), so
                    // there is nothing to insert. The id is now gone from the set,
                    // so a *later* change to this same id is never suppressed.
                    return
                }
                if (observationDao.getById(change.id) == null) {
                    val observation = Observation.fromFirestoreDocument(change.id, change.data)
                    observationDao.insert(observation.toEntity())
                }
            }

            is RemoteObservationChange.Modified -> {
                applyModifiedChange(change)
            }

            is RemoteObservationChange.Removed -> {
                observationDao.deleteById(change.id)
            }
        }
    }

    private suspend fun applyModifiedChange(change: RemoteObservationChange.Modified) {
        val existing = observationDao.getById(change.id) ?: return
        val incoming = Observation.fromFirestoreDocument(change.id, change.data)
        // Preserve this device's own sync bookkeeping - a remote status update
        // (e.g. a teammate marking it attended) is about the document's content,
        // not about whether this device still has local-only photos to upload.
        val merged = incoming.copy(
            syncStatus = existing.syncStatus,
            localImageUris = existing.localImageUris,
        )
        observationDao.update(merged.toEntity())
    }
}
