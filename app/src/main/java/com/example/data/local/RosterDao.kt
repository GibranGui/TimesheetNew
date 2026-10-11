package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RosterDao {

    @Query("SELECT * FROM rosters ORDER BY driverName ASC")
    fun getAllRosters(): Flow<List<RosterEntity>>

    @Query("SELECT * FROM rosters ORDER BY driverName ASC")
    suspend fun getAllRostersOnce(): List<RosterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rosters: List<RosterEntity>)

    @Query("DELETE FROM rosters")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM rosters")
    suspend fun getRosterCount(): Int
}
