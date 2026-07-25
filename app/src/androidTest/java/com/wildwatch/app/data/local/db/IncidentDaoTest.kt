package com.wildwatch.app.data.local.db

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
class IncidentDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: IncidentDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.incidentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleEntity(id: String = "inc-1", syncStatus: SyncStatus = SyncStatus.PENDING) = IncidentEntity(
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
        reportedAt = "2026-07-22T00:00:00.000Z",
        assignedTo = null,
        assignedToName = null,
        hasEvidence = false,
        evidenceCount = 0,
        evidencePhotoUrls = emptyList(),
        localImageUris = emptyList(),
        voiceNoteUrl = null,
        voiceNoteDurationSec = null,
        syncStatus = syncStatus,
        syncedAt = null,
        lastModified = 1000L,
    )

    @Test
    fun insertAndGetById() = runTest {
        dao.insert(sampleEntity())

        val result = dao.getById("inc-1")

        assertEquals("Elephant", result?.species)
        assertEquals(SyncStatus.PENDING, result?.syncStatus)
    }

    @Test
    fun observeAllEmitsInsertedRows() = runTest {
        dao.observeAll().test {
            assertEquals(emptyList<IncidentEntity>(), awaitItem())

            dao.insert(sampleEntity())

            val afterInsert = awaitItem()
            assertEquals(1, afterInsert.size)
            assertEquals("inc-1", afterInsert.first().id)
        }
    }

    @Test
    fun syncStatusTransitionsThroughSyncingToSynced() = runTest {
        dao.insert(sampleEntity())

        dao.updateSyncStatus("inc-1", SyncStatus.SYNCING)
        assertEquals(SyncStatus.SYNCING, dao.getById("inc-1")?.syncStatus)

        dao.markSynced(
            id = "inc-1",
            syncStatus = SyncStatus.SYNCED,
            syncedAt = "2026-07-22T00:05:00.000Z",
            evidencePhotoUrls = listOf("https://example.com/photo.jpg"),
            hasEvidence = true,
            evidenceCount = 1,
        )

        val synced = dao.getById("inc-1")
        assertEquals(SyncStatus.SYNCED, synced?.syncStatus)
        assertEquals(listOf("https://example.com/photo.jpg"), synced?.evidencePhotoUrls)
        assertTrue(synced?.hasEvidence == true)
        assertEquals(1, synced?.evidenceCount)
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

        dao.deleteById("inc-1")

        assertNull(dao.getById("inc-1"))
    }
}
