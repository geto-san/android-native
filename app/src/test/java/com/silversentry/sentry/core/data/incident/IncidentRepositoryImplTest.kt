package com.silversentry.sentry.core.data.incident

import com.silversentry.sentry.core.data.auth.AuthRepository
import com.silversentry.sentry.core.data.bridge.LaravelBridgeDataSource
import com.silversentry.sentry.core.data.notification.NotificationRepository
import com.silversentry.sentry.core.database.IncidentDao
import com.silversentry.sentry.core.database.IncidentEntity
import com.silversentry.sentry.core.database.IncidentSeverity
import com.silversentry.sentry.core.database.IncidentStatus
import com.silversentry.sentry.core.database.IncidentType
import com.silversentry.sentry.core.database.Park
import com.silversentry.sentry.core.database.RangerProgress
import com.silversentry.sentry.core.database.SyncStatus
import com.silversentry.sentry.core.model.Incident
import com.silversentry.sentry.core.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: IncidentDao
    private lateinit var remoteDataSource: IncidentRemoteDataSource
    private lateinit var laravelBridgeDataSource: LaravelBridgeDataSource
    private lateinit var authRepository: AuthRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var repository: IncidentRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxUnitFun = true)
        remoteDataSource = mockk()
        laravelBridgeDataSource = mockk()
        authRepository = mockk()
        notificationRepository = mockk(relaxed = true)
        every { authRepository.currentUser } returns MutableStateFlow(
            User(uid = "uid-1", email = "jane@example.com", displayName = "Jane Ranger"),
        )
        every { remoteDataSource.observeChanges() } returns MutableSharedFlow()
        repository = IncidentRepositoryImpl(
            dao,
            remoteDataSource,
            laravelBridgeDataSource,
            authRepository,
            notificationRepository,
            testDispatcher,
            CoroutineScope(testDispatcher),
        )
    }

    private fun entity(
        id: String = "inc-1",
        syncStatus: SyncStatus = SyncStatus.PENDING,
        localImageUris: List<String> = emptyList(),
        evidencePhotoUrls: List<String> = emptyList(),
    ) = IncidentEntity(
        id = id,
        type = IncidentType.SIGHTING,
        status = IncidentStatus.OPEN,
        rangerProgress = null,
        isEscalated = false,
        park = Park.BWINDI_IMPENETRABLE,
        district = "Kanungu",
        subCounty = "Buhoma",
        parish = "Buhoma",
        community = "Buhoma",
        species = "Elephant",
        severity = IncidentSeverity.MEDIUM,
        category = null,
        summary = "Calm herd",
        lat = -1.5,
        lng = 29.5,
        locationName = "Buhoma sector",
        userName = "Jane Ranger",
        userEmail = "jane@example.com",
        userId = "uid-1",
        reportedAt = "2026-07-22T00:00:00Z",
        assignedTo = null,
        assignedToName = null,
        hasEvidence = evidencePhotoUrls.isNotEmpty(),
        evidenceCount = evidencePhotoUrls.size,
        evidencePhotoUrls = evidencePhotoUrls,
        localImageUris = localImageUris,
        voiceNoteUrl = null,
        voiceNoteDurationSec = null,
        syncStatus = syncStatus,
        syncedAt = null,
        lastModified = 1000L,
    )

    private fun incident(
        id: String = "inc-1",
        species: String = "Elephant",
    ) = Incident(
        id = id,
        type = IncidentType.SIGHTING,
        status = IncidentStatus.OPEN,
        park = Park.BWINDI_IMPENETRABLE,
        community = "Buhoma",
        species = species,
        severity = IncidentSeverity.MEDIUM,
        lat = -1.5,
        lng = 29.5,
        reportedAt = "2026-07-22T00:00:00Z",
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L,
    )

    @Test
    fun `create inserts an OPEN row and never touches the network`() = runTest(testDispatcher) {
        val result = repository.create(
            NewIncidentDetails(
                type = IncidentType.SIGHTING,
                park = Park.BWINDI_IMPENETRABLE,
                community = "Buhoma",
                species = "Elephant",
                severity = IncidentSeverity.MEDIUM,
                category = null,
                summary = "Calm herd",
                lat = -1.5,
                lng = 29.5,
                locationName = "Buhoma sector",
                localImageUris = listOf("file:///photo1.jpg"),
            ),
        )

        assertEquals(IncidentStatus.OPEN, result.status)
        assertEquals(SyncStatus.PENDING, result.syncStatus)
        assertEquals("uid-1", result.userId)
        coVerify { dao.insert(match { it.id == result.id && it.syncStatus == SyncStatus.PENDING }) }
        coVerify(exactly = 0) { remoteDataSource.upsert(any()) }
    }

    @Test
    fun `assignToSelf claims an OPEN row and marks it for sync update`() = runTest(testDispatcher) {
        val syncedRow = entity(syncStatus = SyncStatus.SYNCED)
        coEvery { dao.getById(syncedRow.id) } returns syncedRow

        repository.assignToSelf(syncedRow.id)

        coVerify {
            dao.insert(
                match {
                    it.id == syncedRow.id &&
                        it.status == IncidentStatus.IN_PROGRESS &&
                        it.rangerProgress == RangerProgress.EN_ROUTE &&
                        it.assignedTo == "uid-1" &&
                        it.syncStatus == SyncStatus.PENDING_UPDATE
                },
            )
        }
    }

    @Test
    fun `syncPending flips a successful upload to SYNCED once both Firestore and Laravel succeed`() =
        runTest(testDispatcher) {
            val row = entity(localImageUris = listOf("file:///photo1.jpg"))
            val uploaded = incident(id = row.id).copy(evidencePhotoUrls = listOf("https://storage/photo1.jpg"))
            coEvery { dao.getOutbox(any()) } returns listOf(row)
            coEvery { remoteDataSource.upsert(any()) } returns Result.success(uploaded)
            coEvery { laravelBridgeDataSource.postIncidentEvent(any(), any()) } returns Result.success(Unit)

            val result = repository.syncPending()

            assertEquals(SyncResult(succeeded = 1, failed = 0), result)
            coVerify { dao.updateEvidenceBookkeeping(row.id, uploaded.evidencePhotoUrls, true, 1, emptyList()) }
            coVerify {
                dao.markSynced(
                    id = row.id,
                    syncStatus = SyncStatus.SYNCED,
                    syncedAt = any(),
                    evidencePhotoUrls = uploaded.evidencePhotoUrls,
                    hasEvidence = true,
                    evidenceCount = 1,
                    localImageUris = emptyList(),
                )
            }
            coVerify { laravelBridgeDataSource.postIncidentEvent(uploaded, "create") }
        }

    @Test
    fun `syncPending marks the row FAILED when the Laravel call fails so the next pass retries it`() =
        runTest(testDispatcher) {
            val row = entity()
            val uploaded = incident(id = row.id)
            coEvery { dao.getOutbox(any()) } returns listOf(row)
            coEvery { remoteDataSource.upsert(any()) } returns Result.success(uploaded)
            coEvery { laravelBridgeDataSource.postIncidentEvent(any(), any()) } returns
                Result.failure(java.io.IOException("HTTP 401"))

            val result = repository.syncPending()

            assertEquals(SyncResult(succeeded = 0, failed = 1), result)
            // Evidence bookkeeping still gets saved so a retry doesn't re-upload images -
            // just the sync-status flip to SYNCED is what's withheld.
            coVerify { dao.updateEvidenceBookkeeping(row.id, uploaded.evidencePhotoUrls, false, 0, emptyList()) }
            coVerify { dao.updateSyncStatus(row.id, SyncStatus.FAILED) }
            coVerify(exactly = 0) { dao.markSynced(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `syncPending marks the row FAILED on a Firestore failure and never calls Laravel`() =
        runTest(testDispatcher) {
            val row = entity()
            coEvery { dao.getOutbox(any()) } returns listOf(row)
            coEvery { remoteDataSource.upsert(any()) } returns Result.failure(java.io.IOException("offline"))

            val result = repository.syncPending()

            assertEquals(SyncResult(succeeded = 0, failed = 1), result)
            coVerify { dao.updateSyncStatus(row.id, SyncStatus.FAILED) }
            coVerify(exactly = 0) { laravelBridgeDataSource.postIncidentEvent(any(), any()) }
        }

    @Test
    fun `syncPending picks up FAILED rows from a previous pass and sends an update event`() =
        runTest(testDispatcher) {
            val row = entity(syncStatus = SyncStatus.FAILED)
            val uploaded = incident(id = row.id)
            coEvery { dao.getOutbox(any()) } returns listOf(row)
            coEvery { remoteDataSource.upsert(any()) } returns Result.success(uploaded)
            coEvery { laravelBridgeDataSource.postIncidentEvent(any(), any()) } returns Result.success(Unit)

            val result = repository.syncPending()

            assertEquals(SyncResult(succeeded = 1, failed = 0), result)
            // A FAILED row is a retry of an already-touched row, so it syncs as "update".
            coVerify { laravelBridgeDataSource.postIncidentEvent(uploaded, "update") }
            coVerify { dao.markSynced(match { it == row.id }, SyncStatus.SYNCED, any(), any(), any(), any(), any()) }
        }

    @Test
    fun `syncPending fails a throwing row but still syncs the remaining rows in the same pass`() =
        runTest(testDispatcher) {
            val badRow = entity(id = "bad")
            val goodRow = entity(id = "good")
            val uploaded = incident(id = goodRow.id)
            coEvery { dao.getOutbox(any()) } returns listOf(badRow, goodRow)
            coEvery { remoteDataSource.upsert(any()) } returns Result.success(uploaded)
            coEvery { remoteDataSource.upsert(match { it.id == badRow.id }) } throws RuntimeException("boom")
            coEvery { laravelBridgeDataSource.postIncidentEvent(any(), any()) } returns Result.success(Unit)

            val result = repository.syncPending()

            // The throwing row is marked FAILED rather than aborting the whole pass over it.
            assertEquals(SyncResult(succeeded = 1, failed = 1), result)
            coVerify { dao.updateSyncStatus(badRow.id, SyncStatus.FAILED) }
            coVerify {
                dao.markSynced(
                    match { it == goodRow.id },
                    SyncStatus.SYNCED,
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `remote change for a row still waiting in the outbox is not clobbered`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<RemoteIncidentChange>()
        every { remoteDataSource.observeChanges() } returns changes
        coEvery { dao.getById("mine") } returns entity(id = "mine", syncStatus = SyncStatus.PENDING)

        repository.startObservingRemoteChanges()
        runCurrent()
        changes.emit(RemoteIncidentChange(incident(id = "mine")))
        advanceUntilIdle()

        // Firestore echoes our own just-written doc back while the Laravel leg is still
        // pending; the listener must not flip the local row to SYNCED ahead of the bridge.
        coVerify(exactly = 0) { dao.insert(match { it.id == "mine" }) }
    }

    @Test
    fun `a new remote item from someone else is inserted into Room`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<RemoteIncidentChange>()
        every { remoteDataSource.observeChanges() } returns changes
        coEvery { dao.getById("remote-1") } returns null

        repository.startObservingRemoteChanges()
        runCurrent()
        changes.emit(RemoteIncidentChange(incident(id = "remote-1", species = "Buffalo")))
        advanceUntilIdle()

        coVerify { dao.insert(match { it.id == "remote-1" && it.species == "Buffalo" }) }
    }

    @Test
    fun `portal assignment to me records an INCIDENT_ASSIGNED notification`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<RemoteIncidentChange>()
        every { remoteDataSource.observeChanges() } returns changes
        coEvery { dao.getById("remote-assigned") } returns null

        repository.startObservingRemoteChanges()
        runCurrent()
        changes.emit(
            RemoteIncidentChange(
                incident = incident(id = "remote-assigned").copy(
                    assignedTo = "uid-1",
                    assignedToName = "Jane Ranger",
                    sourceSystem = "laravel",
                ),
            ),
        )
        advanceUntilIdle()

        coVerify {
            notificationRepository.recordIncoming(
                com.silversentry.sentry.core.database.NotificationType.INCIDENT_ASSIGNED,
                any(),
                any(),
                "remote-assigned",
            )
        }
    }

    @Test
    fun `a non-laravel assignment denotes no notification and still inserts the row`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<RemoteIncidentChange>()
        every { remoteDataSource.observeChanges() } returns changes
        coEvery { dao.getById("self-claim") } returns null

        repository.startObservingRemoteChanges()
        runCurrent()
        changes.emit(
            RemoteIncidentChange(
                incident = incident(id = "self-claim").copy(
                    assignedTo = "uid-1",
                    sourceSystem = "firestore",
                ),
            ),
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { notificationRepository.recordIncoming(any(), any(), any(), any()) }
        coVerify { dao.insert(match { it.id == "self-claim" }) }
    }
}
