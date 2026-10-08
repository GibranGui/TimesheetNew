package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    fun getFuelForShiftFlow(shiftId: String): Flow<List<FuelEntryEntity>>

    @Query("SELECT * FROM fuel_entries WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    suspend fun getFuelForShiftDirect(shiftId: String): List<FuelEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuel(fuel: FuelEntryEntity)

    @Query("SELECT SUM(liter) FROM fuel_entries WHERE shiftId = :shiftId")
    suspend fun getTotalFuelForShift(shiftId: String): Double?

    @Query("DELETE FROM fuel_entries WHERE id = :id")
    suspend fun deleteFuelById(id: String)
}
