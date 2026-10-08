package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveShiftFlow(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveShiftDirect(): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    fun getShiftById(id: String): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE status = 'COMPLETED' ORDER BY createdAt DESC")
    fun getAllCompletedShifts(): Flow<List<ShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("UPDATE shifts SET totalRitase = totalRitase + 1, updatedAt = :timestamp WHERE id = :shiftId")
    suspend fun incrementRitase(shiftId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shifts SET totalRitase = :count, updatedAt = :timestamp WHERE id = :shiftId")
    suspend fun updateRitaseCount(shiftId: String, count: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shifts SET totalFuel = :totalFuel, updatedAt = :timestamp WHERE id = :shiftId")
    suspend fun updateTotalFuel(shiftId: String, totalFuel: Double, timestamp: Long = System.currentTimeMillis())
}
