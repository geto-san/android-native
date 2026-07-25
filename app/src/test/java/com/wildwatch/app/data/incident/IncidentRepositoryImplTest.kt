package com.wildwatch.app.data.incident

import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.local.db.IncidentDao
import com.wildwatch.app.data.local.db.IncidentEntity
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.RangerProgress
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.data.local.db.SyncStatus
import com.wildwatch.app.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// This is the highest-risk file in the whole app (this is where every one of
// the RN app's worst bugs lived: a stale-closure duplicate-upload bug, a
// silent-failure-on-missing-photo bug, a background-sync path that wrote
// records nobody else ever read). These tests exist specifically to make each
// of those failure modes impossible to reintroduce here.
@OptIn(ExperimentalCoroutinesApi::class)
class IncidentRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: IncidentDao
    private lateinit var remoteDataSource: IncidentRemoteDataSource
    private lateinit var authRepository: AuthRepository
    private lateinit var repository: IncidentRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxUnitFun = true)
        remoteDataSource = mockk()
        authRepository = mockk()
        every { authRepository.currentUser } returns MutableStateFlow(
            User(uid = "uid-1", email = "jane@example.com", displayName = "Jane Ranger"),
        )
        repository = IncidentRepositoryImpl(
            dao,
            remoteDataSource,
            authRepository,
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
        community = "Buhoma",
        species = "Elephant",
        severity = Severity.MEDIUM,
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

    @Test
    fun `create inserts an OPEN row and never touches the network`() = runTest(testDispatcher) {
        val result = repository.create(
            NewIncidentDetails(
                type = IncidentType.SIGHTING,
                park = Park.BWINDI_IMPENETRABLE,
                community = "Buhoma",
                species = "Elephant",
                severity = Severity.MEDIUM,
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
        coVerify(exactly = 0) { remoteDataSource.writeDocument(any(), any()) }
        coVerify(exactly = 0) { remoteDataSource.uploadImages(any(), any()) }
    }

    @Test
    fun `assignToSelf claims an OPEN row and pushes a remote update when already synced`() = runTest(testDispatcher) {
        val syncedRow = entity(syncStatus = SyncStatus.SYNCED)
        coEvery { dao.getById(syncedRow.id) } returns syncedRow
        coEvery { remoteDataSource.writeDocument(any(), any()) } returns Unit

        repository.assignToSelf(syncedRow.id)

        coVerify {
            dao.update(
                match {
                    it.id == syncedRow.id &&
                        it.status == IncidentStatus.IN_PROGRESS &&
                        it.rangerProgress == RangerProgress.EN_ROUTE &&
                        it.assignedTo == "uid-1"
                },
            )
        }
        coVerify { remoteDataSource.writeDocument(syncedRow.id, any()) }
    }

    @Test
    fun `assignToSelf does not push a remote update for a row that has not synced yet`() = runTest(testDispatcher) {
        val pendingRow = entity(syncStatus = SyncStatus.PENDING)
        coEvery { dao.getById(pendingRow.id) } returns pendingRow

        repository.assignToSelf(pendingRow.id)

        coVerify(exactly = 0) { remoteDataSource.writeDocument(any(), any()) }
    }

    @Test
    fun `syncPending flips a successful upload to SYNCED with merged evidence urls`() =
        runTest(testDispatcher) {
            val row = entity(localImageUris = listOf("file:///photo1.jpg"))
            val uploadedUrl = "https://storage/photo1.jpg"
            coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(row)
            coEvery { remoteDataSource.uploadImages(row.localImageUris, row.id) } returns listOf(uploadedUrl)
            coEvery { remoteDataSource.writeDocument(any(), any()) } returns Unit

            val result = repository.syncPending()

            assertEquals(SyncResult(succeeded = 1, failed = 0), result)
            coVerify { dao.updateSyncStatus(row.id, SyncStatus.SYNCING) }
            coVerify {
                dao.markSynced(
                    id = row.id,
                    syncStatus = SyncStatus.SYNCED,
                    syncedAt = any(),
                    evidencePhotoUrls = listOf(uploadedUrl),
                    hasEvidence = true,
                    evidenceCount = 1,
                )
            }
            coVerify {
                remoteDataSource.writeDocument(
                    row.id,
                    match { (it["evidencePhotoUrls"] as List<*>) == listOf(uploadedUrl) },
                )
            }
        }

    @Test
    fun `syncPending reverts to PENDING and never marks synced when the remote write fails`() =
        runTest(testDispatcher) {
            val row = entity()
            coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(row)
            coEvery { remoteDataSource.uploadImages(any(), any()) } returns emptyList()
            coEvery { remoteDataSource.writeDocument(any(), any()) } throws RuntimeException("network down")

            val result = repository.syncPending()

            assertEquals(SyncResult(succeeded = 0, failed = 1), result)
            coVerify(exactly = 0) { dao.markSynced(any(), any(), any(), any(), any(), any(), any()) }
            coVerify { dao.updateSyncStatus(row.id, SyncStatus.PENDING) }
        }

    @Test
    fun `two concurrent syncPending calls never process the same row twice`() = runTest(testDispatcher) {
        val row = entity()
        // The mutex serializes the two calls, so the second call's query only ever
        // runs after the first has fully finished - by then this row is no longer
        // PENDING in a real database, which this models via returnsMany.
        coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returnsMany listOf(listOf(row), emptyList())
        coEvery { remoteDataSource.uploadImages(any(), any()) } returns emptyList()
        coEvery { remoteDataSource.writeDocument(any(), any()) } returns Unit

        val first = async { repository.syncPending() }
        val second = async { repository.syncPending() }
        val results = awaitAll(first, second)

        assertEquals(1, results.sumOf { it.succeeded })
        coVerify(exactly = 1) { remoteDataSource.writeDocument(row.id, any()) }
        coVerify(exactly = 1) { remoteDataSource.uploadImages(any(), row.id) }
    }

    // --- Remote merge/dedup (guardrail G5) ------------------------------------
    // These are the direct regression tests for the RN app's bug: a teammate
    // updating my own reported incident was silently dropped because the old
    // code skipped every remote change where userId matched the local user, not
    // just the echo of a just-created doc.

    private fun remoteData(species: String = "Buffalo", userId: String = "uid-2") = mapOf(
        "type" to "sighting",
        "park" to Park.BWINDI_IMPENETRABLE.name,
        "community" to "Nkuringo",
        "species" to species,
        "severity" to "medium",
        "category" to null,
        "summary" to null,
        "lat" to -1.4,
        "lng" to 29.6,
        "locationName" to "Nkuringo sector",
        "userName" to "John Ranger",
        "userEmail" to "john@example.com",
        "userId" to userId,
        "reportedAt" to "2026-07-22T02:00:00Z",
        "status" to "open",
        "evidencePhotoUrls" to emptyList<String>(),
    )

    @Test
    fun `a new remote item from someone else is inserted into Room`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteIncidentChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        coEvery { dao.getById("remote-1") } returns null

        repository.startObservingRemoteChanges()
        runCurrent()
        changes.emit(listOf(RemoteIncidentChange.Added("remote-1", remoteData())))
        advanceUntilIdle()

        coVerify { dao.insert(match { it.id == "remote-1" && it.species == "Buffalo" }) }
    }

    @Test
    fun `an update to an existing item by someone else is applied`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteIncidentChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        val existingRow = entity(id = "remote-1", syncStatus = SyncStatus.SYNCED)
        coEvery { dao.getById("remote-1") } returns existingRow

        repository.startObservingRemoteChanges()
        runCurrent()
        val resolvedData = remoteData() + mapOf("status" to "resolved")
        changes.emit(listOf(RemoteIncidentChange.Modified("remote-1", resolvedData)))
        advanceUntilIdle()

        coVerify { dao.update(match { it.id == "remote-1" && it.status == IncidentStatus.RESOLVED }) }
    }

    @Test
    fun `an update to my own item authored by me is not suppressed`() = runTest(testDispatcher) {
        // The bug this directly regression-tests: the doc's userId is the
        // original author (me), unchanged by a teammate's update - that must
        // never be used as a reason to skip the change.
        val changes = MutableSharedFlow<List<RemoteIncidentChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        val myRow = entity(id = "my-inc", syncStatus = SyncStatus.SYNCED)
        coEvery { dao.getById("my-inc") } returns myRow

        repository.startObservingRemoteChanges()
        runCurrent()
        val resolvedByTeammate = remoteData(userId = "uid-1") + mapOf("status" to "resolved")
        changes.emit(listOf(RemoteIncidentChange.Modified("my-inc", resolvedByTeammate)))
        advanceUntilIdle()

        coVerify { dao.update(match { it.id == "my-inc" && it.status == IncidentStatus.RESOLVED }) }
    }

    @Test
    fun `the first echo of my own just-synced incident is not duplicated`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteIncidentChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        val myRow = entity(id = "my-inc")
        coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(myRow)
        coEvery { remoteDataSource.uploadImages(any(), any()) } returns emptyList()
        coEvery { remoteDataSource.writeDocument(any(), any()) } returns Unit

        repository.startObservingRemoteChanges()
        runCurrent()
        repository.syncPending() // populates pendingOwnWriteIds with "my-inc"

        changes.emit(listOf(RemoteIncidentChange.Added("my-inc", remoteData(userId = "uid-1"))))
        advanceUntilIdle()

        // Room already has this row from create()/syncPending() - the echo must
        // not cause a second insert.
        coVerify(exactly = 0) { dao.insert(match { it.id == "my-inc" }) }
    }

    @Test
    fun `a second update after my own echo is never suppressed`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteIncidentChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        val myRow = entity(id = "my-inc", syncStatus = SyncStatus.SYNCED)
        coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(entity(id = "my-inc"))
        coEvery { remoteDataSource.uploadImages(any(), any()) } returns emptyList()
        coEvery { remoteDataSource.writeDocument(any(), any()) } returns Unit
        coEvery { dao.getById("my-inc") } returns myRow

        repository.startObservingRemoteChanges()
        runCurrent()
        repository.syncPending() // populates, then the echo below consumes, pendingOwnWriteIds

        changes.emit(listOf(RemoteIncidentChange.Added("my-inc", remoteData(userId = "uid-1"))))
        advanceUntilIdle()

        // A second, later change to the same id - e.g. a teammate resolving it -
        // must be applied normally now that the id is no longer in the "ignore
        // my own echo" set (guardrail G5's core claim).
        val resolvedByTeammate = remoteData(userId = "uid-1") + mapOf("status" to "resolved")
        changes.emit(listOf(RemoteIncidentChange.Modified("my-inc", resolvedByTeammate)))
        advanceUntilIdle()

        coVerify { dao.update(match { it.id == "my-inc" && it.status == IncidentStatus.RESOLVED }) }
    }

    @Test
    fun `a removed remote item is deleted from Room`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteIncidentChange>>()
        every { remoteDataSource.observeChanges() } returns changes

        repository.startObservingRemoteChanges()
        runCurrent()
        changes.emit(listOf(RemoteIncidentChange.Removed("remote-1")))
        advanceUntilIdle()

        coVerify { dao.deleteById("remote-1") }
    }
}
