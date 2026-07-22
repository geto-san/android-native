package com.silverback.sentry.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Pure data-layer access to the observations table. No sync orchestration lives
// here (guardrail G3 puts that one atomic state machine in
// ObservationRepositoryImpl, Phase 4) - this DAO only offers the primitives that
// state machine (and the UI's read-only Flow) need.
@Dao
interface ObservationDao {

    // Default OnConflictStrategy.ABORT (unset) enforces id uniqueness (guardrail
    // G4): inserting a duplicate id throws rather than silently overwriting.
    @Insert
    suspend fun insert(observation: ObservationEntity)

    @Update
    suspend fun update(observation: ObservationEntity)

    @Query("SELECT * FROM observations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM observations WHERE id = :id")
    suspend fun getById(id: String): ObservationEntity?

    @Query("SELECT * FROM observations WHERE syncStatus = :syncStatus")
    suspend fun getBySyncStatus(syncStatus: SyncStatus): List<ObservationEntity>

    @Query("UPDATE observations SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus)

    // Clears localImageUris once its contents have been uploaded and folded into
    // imageUrls - there is nothing left for a retry to re-upload from that point.
    @Query(
        """
        UPDATE observations
        SET syncStatus = :syncStatus, syncedAt = :syncedAt, imageUrls = :imageUrls,
            hasImages = :hasImages, imageCount = :imageCount, localImageUris = :localImageUris
        WHERE id = :id
        """,
    )
    suspend fun markSynced(
        id: String,
        syncStatus: SyncStatus,
        syncedAt: String,
        imageUrls: List<String>,
        hasImages: Boolean,
        imageCount: Int,
        localImageUris: List<String> = emptyList(),
    )

    @Query(
        """
        UPDATE observations
        SET observationStatus = :observationStatus, attendedAt = :attendedAt,
            attendedBy = :attendedBy, attendedByName = :attendedByName
        WHERE id = :id
        """,
    )
    suspend fun markAttended(
        id: String,
        observationStatus: ObservationStatus,
        attendedAt: String,
        attendedBy: String?,
        attendedByName: String?,
    )

    @Query("DELETE FROM observations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM observations WHERE syncStatus != :syncedStatus")
    suspend fun getPendingCount(syncedStatus: SyncStatus = SyncStatus.SYNCED): Int
}
