package com.wildwatch.app.core.data.patrol

import com.wildwatch.app.core.database.PatrolLogDao
import com.wildwatch.app.core.database.PatrolLogEntity
import com.wildwatch.app.core.database.PatrolStatus
import com.wildwatch.app.core.database.RoutePoint
import com.wildwatch.app.core.database.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatrolRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: PatrolLogDao
    private lateinit var remoteDataSource: PatrolRemoteDataSource
    private lateinit var repository: PatrolRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxUnitFun = true)
        remoteDataSource = mockk()
        repository = PatrolRepositoryImpl(dao, remoteDataSource, testDispatcher)
    }

    private fun entity(
        id: String = "patrol-1",
        routePoints: List<RoutePoint> = emptyList(),
        status: PatrolStatus = PatrolStatus.ACTIVE,
        syncStatus: SyncStatus = SyncStatus.PENDING,
        endTime: String? = null,
    ) = PatrolLogEntity(
        id = id,
        rangerUid = "ranger-1",
        parkId = "park-1",
        routePoints = routePoints,
        startTime = "2026-08-03T00:00:00Z",
        endTime = endTime,
        status = status,
        syncStatus = syncStatus,
        lastModified = 1000L,
    )

    @Test
    fun `startPatrol inserts an ACTIVE row pending sync`() = runTest(testDispatcher) {
        val result = repository.startPatrol(rangerUid = "ranger-1", parkId = "park-1")

        assertEquals("ranger-1", result.rangerUid)
        assertEquals(PatrolStatus.ACTIVE, result.status)
        assertEquals(SyncStatus.PENDING, result.syncStatus)
        assertNotNull(result.startTime)
        coVerify { dao.insert(match { it.id == result.id && it.status == PatrolStatus.ACTIVE }) }
    }

    @Test
    fun `resumeOrStartPatrol starts a new patrol when none is active`() = runTest(testDispatcher) {
        every { dao.observeActiveForRanger("ranger-1") } returns flowOf(null)

        val result = repository.resumeOrStartPatrol(rangerUid = "ranger-1", parkId = "park-1")

        assertEquals(PatrolStatus.ACTIVE, result.status)
        coVerify { dao.insert(match { it.id == result.id && it.rangerUid == "ranger-1" }) }
    }

    @Test
    fun `resumeOrStartPatrol resumes an existing active patrol instead of forking a new one`() =
        runTest(testDispatcher) {
            val existing = entity(id = "patrol-orphaned", routePoints = listOf(RoutePoint(-1.0, 30.0, "t1")))
            every { dao.observeActiveForRanger("ranger-1") } returns flowOf(existing)

            val result = repository.resumeOrStartPatrol(rangerUid = "ranger-1", parkId = "park-1")

            assertEquals("patrol-orphaned", result.id)
            assertEquals(1, result.routePoints.size)
            coVerify(exactly = 0) { dao.insert(any()) }
        }

    @Test
    fun `appendPoint adds to the existing route without dropping prior points`() = runTest(testDispatcher) {
        val existing = entity(routePoints = listOf(RoutePoint(-1.0, 30.0, "t1")))
        coEvery { dao.getById("patrol-1") } returns existing

        repository.appendPoint("patrol-1", RoutePoint(-1.1, 30.1, "t2"))

        coVerify {
            dao.insert(
                match {
                    it.id == "patrol-1" &&
                        it.routePoints.size == 2 &&
                        it.routePoints.last().timestamp == "t2"
                },
            )
        }
    }

    @Test
    fun `appendPoint is a no-op when the patrol no longer exists`() = runTest(testDispatcher) {
        coEvery { dao.getById("missing") } returns null

        repository.appendPoint("missing", RoutePoint(-1.0, 30.0, "t1"))

        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `stopPatrol marks the row COMPLETED with an endTime and re-queues it for sync`() =
        runTest(testDispatcher) {
            val existing = entity(syncStatus = SyncStatus.SYNCED)
            coEvery { dao.getById("patrol-1") } returns existing

            repository.stopPatrol("patrol-1")

            coVerify {
                dao.insert(
                    match {
                        it.status == PatrolStatus.COMPLETED &&
                            it.endTime != null &&
                            it.syncStatus == SyncStatus.PENDING
                    },
                )
            }
        }

    @Test
    fun `syncPending flips a successful upload to SYNCED`() = runTest(testDispatcher) {
        val row = entity()
        coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(row)
        coEvery { remoteDataSource.upsert(any()) } returns Result.success(Unit)

        val result = repository.syncPending()

        assertEquals(PatrolSyncResult(succeeded = 1, failed = 0), result)
        coVerify { dao.updateSyncStatus(row.id, SyncStatus.SYNCED) }
    }

    @Test
    fun `syncPending counts a failed upload without marking it SYNCED`() = runTest(testDispatcher) {
        val row = entity()
        coEvery { dao.getBySyncStatus(SyncStatus.PENDING) } returns listOf(row)
        coEvery { remoteDataSource.upsert(any()) } returns Result.failure(IllegalStateException("offline"))

        val result = repository.syncPending()

        assertEquals(PatrolSyncResult(succeeded = 0, failed = 1), result)
        coVerify(exactly = 0) { dao.updateSyncStatus(any(), SyncStatus.SYNCED) }
    }
}
