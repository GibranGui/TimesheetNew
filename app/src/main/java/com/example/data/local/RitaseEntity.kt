package com.example.data.local

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
    val trigger: String = "EXIT_LOADING",
    val status: String = "VALID"
)
