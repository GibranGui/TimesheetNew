package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries WHERE shiftId = :shiftId AND deletedAt IS NULL ORDER BY timestamp ASC")
    fun getFuelForShiftFlow(shiftId: String): Flow<List<FuelEntryEntity>>

    @Query("SELECT * FROM fuel_entries WHERE shiftId = :shiftId AND deletedAt IS NULL ORDER BY timestamp ASC")
    suspend fun getFuelForShiftDirect(shiftId: String): List<FuelEntryEntity>

    @Query("SELECT * FROM fuel_entries WHERE id = :id LIMIT 1")
    suspend fun getFuelByIdDirect(id: String): FuelEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuel(fuel: FuelEntryEntity)

    @Query("SELECT SUM(liter) FROM fuel_entries WHERE shiftId = :shiftId AND deletedAt IS NULL")
    suspend fun getTotalFuelForShift(shiftId: String): Double?

    @Query("UPDATE fuel_entries SET deletedAt = :now, updatedAt = :now, syncedAt = 0 WHERE id = :id")
    suspend fun softDeleteById(id: String, now: Long = System.currentTimeMillis())

    // ---------- Sinkronisasi online ----------

    @Query("SELECT * FROM fuel_entries WHERE syncedAt = 0 ORDER BY updatedAt ASC")
    suspend fun getUnsyncedFuel(): List<FuelEntryEntity>

    @Query("UPDATE fuel_entries SET syncedAt = :now WHERE id IN (:ids)")
    suspend fun markFuelSynced(ids: List<String>, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM fuel_entries WHERE syncedAt > 0 AND deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeOldDeletedFuel(olderThan: Long)
}
