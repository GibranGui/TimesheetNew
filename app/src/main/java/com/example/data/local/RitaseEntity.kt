package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ritase_events")
data class RitaseEntity(
    @PrimaryKey
    val id: String,
    val shiftId: String,
    val unitId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String, // e.g. "07:25"
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    @ColumnInfo(name = "trigger_type")
    val trigger: String = "EXIT_LOADING",
    val status: String = "VALID",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long = 0L, // 0 = belum terkirim ke server online
    val deletedAt: Long? = null // penanda hapus agar ikut tersinkron
) {
    companion object
}
