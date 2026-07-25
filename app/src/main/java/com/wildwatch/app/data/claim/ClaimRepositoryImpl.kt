package com.wildwatch.app.data.claim

import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.local.db.ClaimDao
import com.wildwatch.app.data.local.db.ClaimStatus
import com.wildwatch.app.data.local.db.SyncStatus
import com.wildwatch.app.di.ApplicationScope
import com.wildwatch.app.di.IoDispatcher
import com.wildwatch.app.domain.model.Claim
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
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

// Mirrors IncidentRepositoryImpl - see its own doc comment for the guardrail
// rationale (G2/G3/G5) behind this shape.
@Singleton
class ClaimRepositoryImpl @Inject constructor(
    private val claimDao: ClaimDao,
    private val remoteDataSource: ClaimRemoteDataSource,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ClaimRepository {

    private val syncMutex = Mutex()
    private val pendingOwnWriteIds = ConcurrentHashMap.newKeySet<String>()
    private var remoteChangesJob: Job? = null

    override fun observeAllForCurrentUser(): Flow<List<Claim>> =
        authRepository.currentUser.flatMapLatest { user ->
            val uid = user?.uid ?: return@flatMapLatest emptyFlow()
            claimDao.observeAllForUser(uid).map { entities -> entities.map(Claim::fromEntity) }
        }

    override suspend fun create(details: NewClaimDetails): Claim = withContext(ioDispatcher) {
        val user = authRepository.currentUser.value
        val claim = Claim(
            id = UUID.randomUUID().toString(),
            category = details.category,
            status = ClaimStatus.UNDER_VERIFICATION,
            park = details.park,
            description = details.description,
            lat = details.lat,
            lng = details.lng,
            locationName = details.locationName,
            userName = user?.displayNameOrFallback,
            userEmail = user?.email,
            userId = user?.uid,
            filedAt = Instant.now().toString(),
            relatedIncidentId = details.relatedIncidentId,
            localImageUris = details.localImageUris,
            syncStatus = SyncStatus.PENDING,
            lastModified = System.currentTimeMillis(),
        )
        claimDao.insert(claim.toEntity())
        claim
    }

    override suspend fun syncPending(): SyncResult = withContext(ioDispatcher) {
        syncMutex.withLock {
            val pending = claimDao.getBySyncStatus(SyncStatus.PENDING)
            var succeeded = 0
            var failed = 0

            for (entity in pending) {
                claimDao.updateSyncStatus(entity.id, SyncStatus.SYNCING)
                try {
                    val uploadedUrls = remoteDataSource.uploadImages(entity.localImageUris, entity.id)
                    val allEvidenceUrls = entity.evidencePhotoUrls + uploadedUrls
                    val syncedAt = Instant.now().toString()
                    val claimToSync = Claim.fromEntity(entity).copy(
                        evidencePhotoUrls = allEvidenceUrls,
                        syncedAt = syncedAt,
                    )

                    pendingOwnWriteIds.add(entity.id)
                    remoteDataSource.writeDocument(entity.id, claimToSync.toFirestoreMap())

                    claimDao.markSynced(
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
                    Timber.e(e, "Sync failed for claim %s", entity.id)
                    claimDao.updateSyncStatus(entity.id, SyncStatus.PENDING)
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

    private suspend fun applyRemoteChange(change: RemoteClaimChange) {
        when (change) {
            is RemoteClaimChange.Added -> {
                if (pendingOwnWriteIds.remove(change.id)) return
                if (claimDao.getById(change.id) == null) {
                    val claim = Claim.fromFirestoreDocument(change.id, change.data)
                    claimDao.insert(claim.toEntity())
                }
            }

            is RemoteClaimChange.Modified -> applyModifiedChange(change)

            is RemoteClaimChange.Removed -> claimDao.deleteById(change.id)
        }
    }

    private suspend fun applyModifiedChange(change: RemoteClaimChange.Modified) {
        val existing = claimDao.getById(change.id) ?: return
        val incoming = Claim.fromFirestoreDocument(change.id, change.data)
        val merged = incoming.copy(
            syncStatus = existing.syncStatus,
            localImageUris = existing.localImageUris,
        )
        claimDao.update(merged.toEntity())
    }
}
