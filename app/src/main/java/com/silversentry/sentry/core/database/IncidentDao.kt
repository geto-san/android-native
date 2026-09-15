package com.silversentry.sentry.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(incident: IncidentEntity)

    @Update
    suspend fun update(incident: IncidentEntity)

    @Query("SELECT * FROM incidents ORDER BY reportedAt DESC")
    fun observeAll(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun getById(id: String): IncidentEntity?

    // The complete outbox a sync pass is responsible for: rows still waiting to be
    // created server-side, rows needing an update, and rows that previously failed and are
    // therefore eligible to be retried on the next pass. FAILED is deliberately part of the
    // retry set - "failed last time, try again" - never a terminal "give up forever" state.
    // Oldest-first ordering means work already waiting the longest finally goes out first,
    // mirrored on the query side the way mihon's SQLDelight queries keep such bookkeeping
    // in the data layer rather than scattered through callers.
    @Query(
        """
        SELECT * FROM incidents
        WHERE syncStatus IN (:statuses)
        ORDER BY lastModified ASC
        """,
    )
    suspend fun getOutbox(statuses: List<SyncStatus>): List<IncidentEntity>

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

    // Persists evidence-photo bookkeeping (real Storage URLs, cleared local URIs) without
    // touching syncStatus - used when the Firestore leg of a sync succeeded but a later leg
    // (the Laravel bridge call) failed, so a retry doesn't re-upload images that already made
    // it to Storage while the row correctly stays PENDING/PENDING_UPDATE.
    @Query(
        """
        UPDATE incidents
        SET evidencePhotoUrls = :evidencePhotoUrls, hasEvidence = :hasEvidence,
            evidenceCount = :evidenceCount, localImageUris = :localImageUris
        WHERE id = :id
        """,
    )
    suspend fun updateEvidenceBookkeeping(
        id: String,
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
