package com.silverback.sentry.data.observation

import com.silverback.sentry.data.auth.AuthRepository
import com.silverback.sentry.data.local.db.ObservationDao
import com.silverback.sentry.data.local.db.ObservationEntity
import com.silverback.sentry.data.local.db.ObservationStatus
import com.silverback.sentry.data.local.db.SyncStatus
import com.silverback.sentry.domain.model.User
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

// Phase 4 is the highest-risk phase in the whole migration (this is where every
// one of the RN app's worst bugs lived: a stale-closure duplicate-upload bug, a
// silent-failure-on-missing-photo bug, a background-sync path that wrote
// observations nobody else ever read). These tests exist specifically to make
// each of those failure modes impossible to reintroduce here.
@OptIn(ExperimentalCoroutinesApi::class)
class ObservationRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: ObservationDao
    private lateinit var remoteDataSource: ObservationRemoteDataSource
    private lateinit var authRepository: AuthRepository
    private lateinit var repository: ObservationRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxUnitFun = true)
        remoteDataSource = mockk()
        authRepository = mockk()
        every { authRepository.currentUser } returns MutableStateFlow(
            User(uid = "uid-1", email = "jane@example.com", displayName = "Jane Ranger"),
        )
        repository = ObservationRepositoryImpl(
            dao,
            remoteDataSource,
            authRepository,
            testDispatcher,
            CoroutineScope(testDispatcher),
        )
    }

    private fun entity(
        id: String = "obs-1",
        syncStatus: SyncStatus = SyncStatus.PENDING,
        localImageUris: List<String> = emptyList(),
        imageUrls: List<String> = emptyList(),
    ) = ObservationEntity(
        id = id,
        gorillaGroup = "Susa Group",
        location = "-1.5, 29.5",
        locationName = "Volcanoes National Park",
        healthStatus = "Healthy",
        notes = null,
        userName = "Jane Ranger",
        userEmail = "jane@example.com",
        userId = "uid-1",
        createdAt = "2026-07-22T00:00:00Z",
        observationStatus = ObservationStatus.PENDING,
        attendedAt = null,
        attendedBy = null,
        attendedByName = null,
        hasImages = imageUrls.isNotEmpty(),
        imageCount = imageUrls.size,
        imageUrls = imageUrls,
        localImageUris = localImageUris,
        syncStatus = syncStatus,
        syncedAt = null,
        lastModified = 1000L,
    )

    @Test
    fun `create inserts a PENDING row and never touches the network`() = runTest(testDispatcher) {
        val result = repository.create(
            gorillaGroup = "Susa Group",
            location = "-1.5, 29.5",
            locationName = "Volcanoes National Park",
            healthStatus = "Healthy",
            notes = "Calm group",
            localImageUris = listOf("file:///photo1.jpg"),
        )

        assertEquals(SyncStatus.PENDING, result.syncStatus)
        assertEquals("uid-1", result.userId)
        coVerify { dao.insert(match { it.id == result.id && it.syncStatus == SyncStatus.PENDING }) }
        coVerify(exactly = 0) { remoteDataSource.writeDocument(any(), any()) }
        coVerify(exactly = 0) { remoteDataSource.uploadImages(any(), any()) }
    }

    @Test
    fun `syncPending flips a successful upload to SYNCED with merged image urls`() =
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
                    imageUrls = listOf(uploadedUrl),
                    hasImages = true,
                    imageCount = 1,
                )
            }
            coVerify {
                remoteDataSource.writeDocument(
                    row.id,
                    match { (it["imageUrls"] as List<*>) == listOf(uploadedUrl) },
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

    @Test
    fun `markAttended pushes a remote update when the row has already synced`() = runTest(testDispatcher) {
        val syncedRow = entity(syncStatus = SyncStatus.SYNCED)
        coEvery { dao.getById(syncedRow.id) } returns syncedRow
        coEvery { remoteDataSource.writeDocument(any(), any()) } returns Unit

        repository.markAttended(syncedRow.id)

        coVerify {
            dao.markAttended(
                id = syncedRow.id,
                observationStatus = ObservationStatus.ATTENDED,
                attendedAt = any(),
                attendedBy = "uid-1",
                attendedByName = "Jane Ranger",
            )
        }
        coVerify { remoteDataSource.writeDocument(syncedRow.id, any()) }
    }

    @Test
    fun `markAttended does not push a remote update for a row that has not synced yet`() = runTest(testDispatcher) {
        val pendingRow = entity(syncStatus = SyncStatus.PENDING)
        coEvery { dao.getById(pendingRow.id) } returns pendingRow

        repository.markAttended(pendingRow.id)

        coVerify(exactly = 0) { remoteDataSource.writeDocument(any(), any()) }
    }

    // --- Phase 7: remote merge/dedup (guardrail G5) --------------------------
    // These are the direct regression tests for the RN app's bug: a teammate
    // marking my own reported sighting "attended" was silently dropped because
    // the old code skipped every remote change where userId matched the local
    // user, not just the echo of a just-created doc.

    private fun remoteData(gorillaGroup: String = "Kwitonda Group", userId: String = "uid-2") = mapOf(
        "gorillaGroup" to gorillaGroup,
        "location" to "-1.4, 29.6",
        "locationName" to "Kinigi",
        "healthStatus" to "Healthy",
        "notes" to null,
        "userName" to "John Ranger",
        "userEmail" to "john@example.com",
        "userId" to userId,
        "createdAt" to "2026-07-22T02:00:00Z",
        "status" to "pending",
        "imageUrls" to emptyList<String>(),
    )

    @Test
    fun `a new remote item from someone else is inserted into Room`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteObservationChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        coEvery { dao.getById("remote-1") } returns null

        repository.startObservingRemoteChanges()
        runCurrent()
        changes.emit(listOf(RemoteObservationChange.Added("remote-1", remoteData())))
        advanceUntilIdle()

        coVerify { dao.insert(match { it.id == "remote-1" && it.gorillaGroup == "Kwitonda Group" }) }
    }

    @Test
    fun `an update to an existing item by someone else is applied`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteObservationChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        val existingRow = entity(id = "remote-1", syncStatus = SyncStatus.SYNCED)
        coEvery { dao.getById("remote-1") } returns existingRow

        repository.startObservingRemoteChanges()
        runCurrent()
        val attendedData = remoteData() + mapOf("status" to "attended", "attendedByName" to "John Ranger")
        changes.emit(listOf(RemoteObservationChange.Modified("remote-1", attendedData)))
        advanceUntilIdle()

        coVerify { dao.update(match { it.id == "remote-1" && it.observationStatus == ObservationStatus.ATTENDED }) }
    }

    @Test
    fun `an update to my own item authored by me is not suppressed`() = runTest(testDispatcher) {
        // The bug this directly regression-tests: the doc's userId is the
        // original author (me), unchanged by a teammate's update - that must
        // never be used as a reason to skip the change.
        val changes = MutableSharedFlow<List<RemoteObservationChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        val myRow = entity(id = "my-obs", syncStatus = SyncStatus.SYNCED)
        coEvery { dao.getById("my-obs") } returns myRow

        repository.startObservingRemoteChanges()
        runCurrent()
        val attendedByTeammate = remoteData(userId = "uid-1") + mapOf("status" to "attended", "attendedBy" to "uid-2")
        changes.emit(listOf(RemoteObservationChange.Modified("my-obs", attendedByTeammate)))
        advanceUntilIdle()

        coVerify { dao.update(match { it.id == "my-obs" && it.observationStatus == ObservationStatus.ATTENDED }) }
    }

    @Test
    fun `the first echo of my own just-synced observation is not duplicated`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteObservationChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        val myRow = entity(id = "my-obs")
        coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(myRow)
        coEvery { remoteDataSource.uploadImages(any(), any()) } returns emptyList()
        coEvery { remoteDataSource.writeDocument(any(), any()) } returns Unit

        repository.startObservingRemoteChanges()
        runCurrent()
        repository.syncPending() // populates pendingOwnWriteIds with "my-obs"

        changes.emit(listOf(RemoteObservationChange.Added("my-obs", remoteData(userId = "uid-1"))))
        advanceUntilIdle()

        // Room already has this row from create()/syncPending() - the echo must
        // not cause a second insert.
        coVerify(exactly = 0) { dao.insert(match { it.id == "my-obs" }) }
    }

    @Test
    fun `a second update after my own echo is never suppressed`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteObservationChange>>()
        every { remoteDataSource.observeChanges() } returns changes
        val myRow = entity(id = "my-obs", syncStatus = SyncStatus.SYNCED)
        coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(entity(id = "my-obs"))
        coEvery { remoteDataSource.uploadImages(any(), any()) } returns emptyList()
        coEvery { remoteDataSource.writeDocument(any(), any()) } returns Unit
        coEvery { dao.getById("my-obs") } returns myRow

        repository.startObservingRemoteChanges()
        runCurrent()
        repository.syncPending() // populates, then the echo below consumes, pendingOwnWriteIds

        changes.emit(listOf(RemoteObservationChange.Added("my-obs", remoteData(userId = "uid-1"))))
        advanceUntilIdle()

        // A second, later change to the same id - e.g. a teammate attending it -
        // must be applied normally now that the id is no longer in the "ignore
        // my own echo" set (guardrail G5's core claim).
        val attendedByTeammate = remoteData(userId = "uid-1") + mapOf("status" to "attended", "attendedBy" to "uid-2")
        changes.emit(listOf(RemoteObservationChange.Modified("my-obs", attendedByTeammate)))
        advanceUntilIdle()

        coVerify { dao.update(match { it.id == "my-obs" && it.observationStatus == ObservationStatus.ATTENDED }) }
    }

    @Test
    fun `a removed remote item is deleted from Room`() = runTest(testDispatcher) {
        val changes = MutableSharedFlow<List<RemoteObservationChange>>()
        every { remoteDataSource.observeChanges() } returns changes

        repository.startObservingRemoteChanges()
        runCurrent()
        changes.emit(listOf(RemoteObservationChange.Removed("remote-1")))
        advanceUntilIdle()

        coVerify { dao.deleteById("remote-1") }
    }
}
