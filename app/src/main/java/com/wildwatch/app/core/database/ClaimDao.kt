package com.wildwatch.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Mirrors IncidentDao's shape/conventions - see its own doc comment.
@Dao
interface ClaimDao {

    @Insert
    suspend fun insert(claim: ClaimEntity)

    @Update
    suspend fun update(claim: ClaimEntity)

    @Query("SELECT * FROM claims WHERE userId = :userId ORDER BY filedAt DESC")
    fun observeAllForUser(userId: String): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims ORDER BY filedAt DESC")
    fun observeAll(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE id = :id")
    suspend fun getById(id: String): ClaimEntity?

    @Query("SELECT * FROM claims WHERE syncStatus = :syncStatus")
    suspend fun getBySyncStatus(syncStatus: SyncStatus): List<ClaimEntity>

    @Query("UPDATE claims SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus)

    @Query(
        """
        UPDATE claims
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

    @Query("DELETE FROM claims WHERE id = :id")
    suspend fun deleteById(id: String)
}
