package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daftar nama driver yang pernah diunduh dari sheet jadwal.
 * Dipakai untuk saran otomatis saat membuat akun baru,
 * sehingga penulisan nama sama persis dengan data perusahaan.
 */
@Entity(tableName = "driver_templates")
data class DriverTemplateEntity(
    @PrimaryKey val name: String,
    val lastSeenAt: Long = System.currentTimeMillis()
)
