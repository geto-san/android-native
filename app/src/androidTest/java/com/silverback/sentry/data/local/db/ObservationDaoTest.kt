package com.silverback.sentry.data.local.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObservationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ObservationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.observationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleEntity(id: String = "obs-1", syncStatus: SyncStatus = SyncStatus.PENDING) = ObservationEntity(
        id = id,
        gorillaGroup = "Susa Group",
        location = "-1.5, 29.5",
        locationName = "Volcanoes National Park",
        healthStatus = "Healthy",
        notes = null,
        userName = "Jane Ranger",
        userEmail = "jane@example.com",
        userId = "uid-1",
        createdAt = "2026-07-22T00:00:00.000Z",
        observationStatus = ObservationStatus.PENDING,
        attendedAt = null,
        attendedBy = null,
        attendedByName = null,
        hasImages = false,
        imageCount = 0,
        imageUrls = emptyList(),
        localImageUris = emptyList(),
        syncStatus = syncStatus,
        syncedAt = null,
        lastModified = 1000L,
    )

    @Test
    fun insertAndGetById() = runTest {
        dao.insert(sampleEntity())

        val result = dao.getById("obs-1")

        assertEquals("Susa Group", result?.gorillaGroup)
        assertEquals(SyncStatus.PENDING, result?.syncStatus)
    }

    @Test
    fun observeAllEmitsInsertedRows() = runTest {
        dao.observeAll().test {
            assertEquals(emptyList<ObservationEntity>(), awaitItem())

            dao.insert(sampleEntity())

            val afterInsert = awaitItem()
            assertEquals(1, afterInsert.size)
            assertEquals("obs-1", afterInsert.first().id)
        }
    }

    @Test
    fun syncStatusTransitionsThroughSyncingToSynced() = runTest {
        dao.insert(sampleEntity())

        dao.updateSyncStatus("obs-1", SyncStatus.SYNCING)
        assertEquals(SyncStatus.SYNCING, dao.getById("obs-1")?.syncStatus)

        dao.markSynced(
            id = "obs-1",
            syncStatus = SyncStatus.SYNCED,
            syncedAt = "2026-07-22T00:05:00.000Z",
            imageUrls = listOf("https://example.com/photo.jpg"),
            hasImages = true,
            imageCount = 1,
        )

        val synced = dao.getById("obs-1")
        assertEquals(SyncStatus.SYNCED, synced?.syncStatus)
        assertEquals(listOf("https://example.com/photo.jpg"), synced?.imageUrls)
        assertTrue(synced?.hasImages == true)
        assertEquals(1, synced?.imageCount)
    }

    @Test
    fun markAttendedUpdatesObservationStatusWithoutTouchingSyncStatus() = runTest {
        dao.insert(sampleEntity(syncStatus = SyncStatus.SYNCED))

        dao.markAttended(
            id = "obs-1",
            observationStatus = ObservationStatus.ATTENDED,
            attendedAt = "2026-07-22T01:00:00.000Z",
            attendedBy = "uid-2",
            attendedByName = "John Ranger",
        )

        val result = dao.getById("obs-1")
        assertEquals(ObservationStatus.ATTENDED, result?.observationStatus)
        assertEquals("John Ranger", result?.attendedByName)
        // Marking attended is a separate concern from sync state (guardrail
        // separation between ObservationStatus and SyncStatus).
        assertEquals(SyncStatus.SYNCED, result?.syncStatus)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insertingADuplicateIdThrows() = runTest {
        dao.insert(sampleEntity(id = "dup"))
        dao.insert(sampleEntity(id = "dup"))
    }

    @Test
    fun getPendingCountExcludesOnlySyncedRows() = runTest {
        dao.insert(sampleEntity(id = "a", syncStatus = SyncStatus.PENDING))
        dao.insert(sampleEntity(id = "b", syncStatus = SyncStatus.SYNCED))
        dao.insert(sampleEntity(id = "c", syncStatus = SyncStatus.FAILED))
        dao.insert(sampleEntity(id = "d", syncStatus = SyncStatus.SYNCING))

        assertEquals(3, dao.getPendingCount())
    }

    @Test
    fun deleteByIdRemovesTheRow() = runTest {
        dao.insert(sampleEntity())

        dao.deleteById("obs-1")

        assertNull(dao.getById("obs-1"))
    }
}
