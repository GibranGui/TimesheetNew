package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey
    val cn: String, // e.g. "DT-012"
    val displayName: String = cn,
    val lastUsedAt: Long = System.currentTimeMillis()
)
