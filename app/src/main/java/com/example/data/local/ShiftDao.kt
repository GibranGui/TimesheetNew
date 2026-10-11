package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE status = 'ACTIVE' AND deletedAt IS NULL LIMIT 1")
    fun getActiveShiftFlow(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE status = 'ACTIVE' AND deletedAt IS NULL LIMIT 1")
    suspend fun getActiveShiftDirect(): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    fun getShiftById(id: String): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftByIdDirect(id: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE status = 'COMPLETED' AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllCompletedShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE status = 'COMPLETED' AND deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getAllCompletedShiftsDirect(): List<ShiftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("UPDATE shifts SET totalRitase = totalRitase + 1, updatedAt = :timestamp, syncedAt = 0 WHERE id = :shiftId")
    suspend fun incrementRitase(shiftId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shifts SET totalRitase = :count, updatedAt = :timestamp, syncedAt = 0 WHERE id = :shiftId")
    suspend fun updateRitaseCount(shiftId: String, count: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shifts SET totalFuel = :totalFuel, updatedAt = :timestamp, syncedAt = 0 WHERE id = :shiftId")
    suspend fun updateTotalFuel(shiftId: String, totalFuel: Double, timestamp: Long = System.currentTimeMillis())

    // ---------- Sinkronisasi online ----------

    @Query("SELECT * FROM shifts WHERE syncedAt = 0 ORDER BY updatedAt ASC")
    suspend fun getUnsyncedShifts(): List<ShiftEntity>

    @Query("UPDATE shifts SET syncedAt = :now WHERE id = :id")
    suspend fun markShiftSynced(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE shifts SET deletedAt = :now, updatedAt = :now, syncedAt = 0 WHERE id = :id")
    suspend fun softDeleteShift(id: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM shifts WHERE syncedAt > 0 AND deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeOldDeletedShifts(olderThan: Long)
}
