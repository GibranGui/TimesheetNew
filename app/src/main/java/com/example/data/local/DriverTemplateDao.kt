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

    /** Cek apakah nama persis (tanpa peduli huruf besar/kecil) ada di daftar jadwal kantor. */
    @Query("SELECT EXISTS(SELECT 1 FROM driver_templates WHERE name = :name COLLATE NOCASE)")
    suspend fun existsExact(name: String): Boolean

    @Query("DELETE FROM driver_templates")
    suspend fun deleteAll()
}
