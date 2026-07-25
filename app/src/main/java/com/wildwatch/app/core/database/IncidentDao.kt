package com.wildwatch.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Pure data-layer access to the incidents table. No sync orchestration lives
// here (guardrail G3 puts that one atomic state machine in
// IncidentRepositoryImpl) - this DAO only offers the primitives that state
// machine (and the UI's read-only Flow) need.
@Dao
interface IncidentDao {

    // Default OnConflictStrategy.ABORT (unset) enforces id uniqueness (guardrail
    // G4): inserting a duplicate id throws rather than silently overwriting.
    @Insert
    suspend fun insert(incident: IncidentEntity)

    @Update
    suspend fun update(incident: IncidentEntity)

    @Query("SELECT * FROM incidents ORDER BY reportedAt DESC")
    fun observeAll(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun getById(id: String): IncidentEntity?

    @Query("SELECT * FROM incidents WHERE syncStatus = :syncStatus")
    suspend fun getBySyncStatus(syncStatus: SyncStatus): List<IncidentEntity>

    @Query("UPDATE incidents SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus)

    // Clears localImageUris once its contents have been uploaded and folded into
    // evidencePhotoUrls - there is nothing left for a retry to re-upload from
    // that point.
    @Query(
        """
        UPDATE incidents
        SET syncStatus = :syncStatus, syncedAt = :syncedAt, evidencePhotoUrls = :evidencePhotoUrls,
            hasEvidence = :hasEvidence, evidenceCount = :evidenceCount, localImageUris = :localImageUris
        WHERE id = :id
        """,
    )
    suspend fun markSynced(
        id: String,
        syncStatus: SyncStatus,
        syncedAt: String,
        evidencePhotoUrls: List<String>,
        hasEvidence: Boolean,
        evidenceCount: Int,
        localImageUris: List<String> = emptyList(),
    )

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM incidents WHERE syncStatus != :syncedStatus")
    suspend fun getPendingCount(syncedStatus: SyncStatus = SyncStatus.SYNCED): Int
}
