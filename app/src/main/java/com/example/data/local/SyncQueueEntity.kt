package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val id: String,
    val entityType: String, // "SHIFT" | "RITASE" | "FUEL" | "MASTER"
    val entityId: String,
    val action: String = "INSERT",
    val status: String = "PENDING", // "PENDING" | "SYNCING" | "SYNCED" | "FAILED"
    val retryCount: Int = 0,
    val lastError: String? = null,
    val payloadJson: String = "",
    val monthPeriod: String = "", // e.g. "2026-10"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
