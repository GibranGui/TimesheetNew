package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_entries")
data class FuelEntryEntity(
    @PrimaryKey
    val id: String,
    val shiftId: String,
    val unitId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String, // e.g. "10:30"
    val hmIsiFuel: Double,
    val liter: Double,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long = 0L, // 0 = belum terkirim ke server online
    val deletedAt: Long? = null // penanda hapus agar ikut tersinkron
) {
    companion object
}
