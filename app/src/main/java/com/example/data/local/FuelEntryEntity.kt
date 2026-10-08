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
    val liter: Double
)
