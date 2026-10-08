package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY lastUsedAt DESC")
    fun getAllUnitsFlow(): Flow<List<UnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity)

    @Query("UPDATE units SET lastUsedAt = :timestamp WHERE cn = :cn")
    suspend fun updateLastUsed(cn: String, timestamp: Long = System.currentTimeMillis())
}
