package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey
    val id: String,
    val unitId: String,
    val date: String, // "DD/MM/YYYY"
    val hmAwal: Double,
    val hmAkhir: Double? = null,
    val kmAwal: Double,
    val kmAkhir: Double? = null,
    val totalHm: Double = 0.0,
    val totalKm: Double = 0.0,
    val totalRitase: Int = 0,
    val totalFuel: Double = 0.0,
    val status: String = "ACTIVE", // "ACTIVE" | "COMPLETED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
