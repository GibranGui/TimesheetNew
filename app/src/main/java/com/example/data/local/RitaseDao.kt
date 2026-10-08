package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RitaseDao {
    @Query("SELECT * FROM ritase_events WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    fun getRitaseForShiftFlow(shiftId: String): Flow<List<RitaseEntity>>

    @Query("SELECT * FROM ritase_events WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    suspend fun getRitaseForShiftDirect(shiftId: String): List<RitaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRitase(ritase: RitaseEntity)

    @Query("SELECT COUNT(*) FROM ritase_events WHERE shiftId = :shiftId")
    suspend fun countRitase(shiftId: String): Int

    @Query("DELETE FROM ritase_events WHERE id = :id")
    suspend fun deleteRitaseById(id: String)
}
