package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LoadingAreaDao {
    @Query("SELECT * FROM loading_areas WHERE id = 'default_loading_area' LIMIT 1")
    fun getLoadingAreaFlow(): Flow<LoadingAreaEntity?>

    @Query("SELECT * FROM loading_areas WHERE id = 'default_loading_area' LIMIT 1")
    suspend fun getActiveLoadingAreaDirect(): LoadingAreaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(area: LoadingAreaEntity)
}
