package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loading_areas")
data class LoadingAreaEntity(
    @PrimaryKey
    val id: String = "default_loading_area",
    val name: String = "Pit Loading Area",
    val latitude: Double = -2.123456, // default or calibrated coordinate
    val longitude: Double = 115.123456,
    val radiusMeters: Double = 50.0,
    val enabled: Boolean = true
)
