package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RitaseDao {
    @Query("SELECT * FROM ritase_events WHERE shiftId = :shiftId AND deletedAt IS NULL ORDER BY timestamp ASC")
    fun getRitaseForShiftFlow(shiftId: String): Flow<List<RitaseEntity>>

    @Query("SELECT * FROM ritase_events WHERE shiftId = :shiftId AND deletedAt IS NULL ORDER BY timestamp ASC")
    suspend fun getRitaseForShiftDirect(shiftId: String): List<RitaseEntity>

    @Query("SELECT * FROM ritase_events WHERE id = :id LIMIT 1")
    suspend fun getRitaseByIdDirect(id: String): RitaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRitase(ritase: RitaseEntity)

    @Query("SELECT COUNT(*) FROM ritase_events WHERE shiftId = :shiftId AND deletedAt IS NULL")
    suspend fun countRitase(shiftId: String): Int

    @Query("UPDATE ritase_events SET deletedAt = :now, updatedAt = :now, syncedAt = 0 WHERE id = :id")
    suspend fun softDeleteById(id: String, now: Long = System.currentTimeMillis())

    // ---------- Sinkronisasi online ----------

    @Query("SELECT * FROM ritase_events WHERE syncedAt = 0 ORDER BY updatedAt ASC")
    suspend fun getUnsyncedRitase(): List<RitaseEntity>

    @Query("UPDATE ritase_events SET syncedAt = :now WHERE id IN (:ids)")
    suspend fun markRitaseSynced(ids: List<String>, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM ritase_events WHERE syncedAt > 0 AND deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeOldDeletedRitase(olderThan: Long)
}
