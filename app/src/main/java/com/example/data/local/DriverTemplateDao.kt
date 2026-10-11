package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DriverTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(drivers: List<DriverTemplateEntity>)

    @Query("SELECT * FROM driver_templates WHERE name LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY name ASC LIMIT 12")
    suspend fun search(query: String): List<DriverTemplateEntity>

    @Query("SELECT COUNT(*) FROM driver_templates")
    suspend fun count(): Int

    @Query("DELETE FROM driver_templates")
    suspend fun deleteAll()
}
