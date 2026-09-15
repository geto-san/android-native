package com.silversentry.sentry.core.data.incident

import com.silversentry.sentry.core.data.bridge.LaravelBridgeDataSource
import com.silversentry.sentry.core.database.IncidentDao
import com.silversentry.sentry.core.database.IncidentEntity
import com.silversentry.sentry.core.database.IncidentStatus
import com.silversentry.sentry.core.database.NotificationType
import com.silversentry.sentry.core.database.RangerProgress
import com.silversentry.sentry.core.database.SyncStatus
import com.silversentry.sentry.core.di.ApplicationScope
import com.silversentry.sentry.core.di.IoDispatcher
import com.silversentry.sentry.core.model.Incident
import com.silversentry.sentry.core.data.auth.AuthRepository
import com.silversentry.sentry.core.data.notification.NotificationRepository
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
    private val notificationRepository: NotificationRepository,
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

    // Withdraws an alert (the SOS "HOLD TO CANCEL" path). Marks the incident CANCELLED and
    // re-queues it so the outbox pushes the "cancelled" status to Firestore - which is the
    // single turn-off signal the ranger map, the community stream, and the web portal all
    // read. If the alert never synced yet (still PENDING), the very first remote write is the
    // cancelled one, so the alert is never visible as active anywhere.
    override suspend fun withdraw(id: String) = withContext(ioDispatcher) {
        val entity = incidentDao.getById(id) ?: return@withContext
        val updated = entity.copy(
            status = IncidentStatus.CANCELLED,
            syncStatus = if (entity.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_UPDATE else entity.syncStatus,
            lastModified = System.currentTimeMillis()
        )
        incidentDao.insert(updated)
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun syncPending(): SyncResult = syncMutex.withLock {
        withContext(ioDispatcher) {
            // FAILED rows are retried too - "failed last time" must never become "abandoned
            // forever" just because a transient blip happened mid-sync.
            val outbox = incidentDao.getOutbox(OUTBOX_SYNC_STATUSES)

            var succeeded = 0
            var failed = 0

            // Each row is isolated: one bad row (an upload that throws unexpectedly, a DAO
            // hiccup) must never abort the whole pass, or every still-pending incident would
            // silently stop syncing with it. The state machine either moves the row to SYNCED
            // or marks it FAILED (visible in the UI as "Upload failed") so the next pass -
            // periodic, immediate, or connectivity-triggered - picks it right back up.
            outbox.forEach { entity ->
                val eventType = if (entity.syncStatus == SyncStatus.PENDING) "create" else "update"
                try {
                    val firestoreResult = remoteDataSource.upsert(Incident.fromEntity(entity))
                    val syncedIncident = firestoreResult.getOrNull()
                    if (syncedIncident == null) {
                        incidentDao.updateSyncStatus(entity.id, SyncStatus.FAILED)
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
                            "Laravel bridge call failed for incident %s; marking failed for retry",
                            syncedIncident.id,
                        )
                        incidentDao.updateSyncStatus(syncedIncident.id, SyncStatus.FAILED)
                        failed++
                    }
                } catch (e: Exception) {
                    // An unexpected failure (upload throws rather than returning a Result, mapper
                    // chokes, DAO write fails) must not kill the pass for every other row - mark
                    // this one FAILED so it is retried, then keep going.
                    Timber.e(e, "Unexpected sync failure for incident %s; marking failed for retry", entity.id)
                    incidentDao.updateSyncStatus(entity.id, SyncStatus.FAILED)
                    failed++
                }
            }
            SyncResult(succeeded, failed)
        }
    }

    override fun startObservingRemoteChanges() {
        applicationScope.launch {
            remoteDataSource.observeChanges().collect { change ->
                // Guard against the realtime listener clobbering a row that is still in the
                // local outbox. After our own Firestore write succeeds, the snapshot listener
                // sees that document and would otherwise overwrite the Room row with a SYNCED
                // copy BEFORE this device has finished the Laravel bridge leg - which both
                // yanked the incident out of the queue early (it would never be retried if the
                // bridge was slow or failed) and made "no communication with the server" look
                // indistinguishable from "synced". Rows actively being synced stay exactly as
                // they are locally; only genuinely-new remote rows (and already-SYNCED ones)
                // flow in from the listener.
                val existing = incidentDao.getById(change.incident.id)
                if (existing != null && existing.syncStatus in OUTBOX_SYNC_STATUSES) {
                    return@collect
                }
                maybeNotifyRangerOfAssignment(change, existing)
                incidentDao.insert(change.incident.toEntity())
            }
        }
    }

    // Spark-plan fallback for the portal's ranger-assignment ping. Cloud Functions don't run
    // there, so the registered (functions/src/notifications.ts) handler that pushes an
    // INCIDENT_ASSIGNED notification never fires - this listener is the whole path that gets
    // the assignment into the assigned ranger's inbox. When functions DO run, recordIncoming
    // dedupes against the FCM/document copies of the same event (same type + targetId), so the
    // ranger still sees exactly one row.
    private suspend fun maybeNotifyRangerOfAssignment(change: RemoteIncidentChange, existing: IncidentEntity?) {
        val incident = change.incident
        // Only portal assignments are stamped source_system "laravel"; a mobile self-claim
        // writes "firestore" with status in_progress and just appears in the caller's own map.
        val assignedTo = incident.assignedTo
        val currentUid = authRepository.currentUser.value?.uid
        val shouldNotify = !change.isRemoved &&
            incident.sourceSystem == "laravel" &&
            assignedTo != null &&
            assignedTo == currentUid &&
            existing?.assignedTo != assignedTo

        if (!shouldNotify) return

        notificationRepository.recordIncoming(
            type = NotificationType.INCIDENT_ASSIGNED,
            title = "You've been assigned an incident",
            message = "Incident #${incident.id.take(8)} was assigned to you. Open it to start responding.",
            targetId = incident.id,
        )
    }

    private companion object {
        val OUTBOX_SYNC_STATUSES = listOf(
            SyncStatus.PENDING,
            SyncStatus.PENDING_UPDATE,
            SyncStatus.FAILED,
        )
    }
}
