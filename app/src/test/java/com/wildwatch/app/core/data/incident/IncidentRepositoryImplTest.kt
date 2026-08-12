package com.wildwatch.app.core.data.incident

import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.bridge.LaravelBridgeDataSource
import com.wildwatch.app.core.database.IncidentDao
import com.wildwatch.app.core.database.IncidentEntity
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.database.RangerProgress
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.model.User
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
    private lateinit var repository: IncidentRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxUnitFun = true)
        remoteDataSource = mockk()
        laravelBridgeDataSource = mockk()
        authRepository = mockk()
        every { authRepository.currentUser } returns MutableStateFlow(
            User(uid = "uid-1", email = "jane@example.com", displayName = "Jane Ranger"),
        )
        every { remoteDataSource.observeChanges() } returns MutableSharedFlow()
        repository = IncidentRepositoryImpl(
            dao,
            remoteDataSource,
            laravelBridgeDataSource,
            authRepository,
            mockk(), // locationRepository not used
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
            coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(row)
            coEvery { dao.getBySyncStatus(SyncStatus.PENDING_UPDATE) } returns emptyList()
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
    fun `syncPending keeps the row pending for retry when the Laravel call fails`() =
        runTest(testDispatcher) {
            val row = entity()
            val uploaded = incident(id = row.id)
            coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(row)
            coEvery { dao.getBySyncStatus(SyncStatus.PENDING_UPDATE) } returns emptyList()
            coEvery { remoteDataSource.upsert(any()) } returns Result.success(uploaded)
            coEvery { laravelBridgeDataSource.postIncidentEvent(any(), any()) } returns
                Result.failure(java.io.IOException("HTTP 401"))

            val result = repository.syncPending()

            assertEquals(SyncResult(succeeded = 0, failed = 1), result)
            // Evidence bookkeeping still gets saved so a retry doesn't re-upload images -
            // just the sync-status flip to SYNCED is what's withheld.
            coVerify { dao.updateEvidenceBookkeeping(row.id, uploaded.evidencePhotoUrls, false, 0, emptyList()) }
            coVerify(exactly = 0) { dao.markSynced(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `syncPending counts a Firestore failure without ever calling Laravel`() =
        runTest(testDispatcher) {
            val row = entity()
            coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(row)
            coEvery { dao.getBySyncStatus(SyncStatus.PENDING_UPDATE) } returns emptyList()
            coEvery { remoteDataSource.upsert(any()) } returns Result.failure(java.io.IOException("offline"))

            val result = repository.syncPending()

            assertEquals(SyncResult(succeeded = 0, failed = 1), result)
            coVerify(exactly = 0) { laravelBridgeDataSource.postIncidentEvent(any(), any()) }
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
}
