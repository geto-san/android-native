package com.wildwatch.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatrolLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PatrolLogEntity)

    @Query("SELECT * FROM patrol_logs WHERE id = :id")
    suspend fun getById(id: String): PatrolLogEntity?

    @Query("SELECT * FROM patrol_logs WHERE rangerUid = :rangerUid AND status = 'ACTIVE' LIMIT 1")
    fun observeActiveForRanger(rangerUid: String): Flow<PatrolLogEntity?>

    @Query("SELECT * FROM patrol_logs WHERE syncStatus = :syncStatus")
    suspend fun getBySyncStatus(syncStatus: SyncStatus): List<PatrolLogEntity>

    @Query("UPDATE patrol_logs SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus)
}
