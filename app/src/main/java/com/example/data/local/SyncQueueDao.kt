package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC LIMIT 50")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = 'SYNCED', updatedAt = :timestamp WHERE id = :id")
    suspend fun markSynced(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'FAILED', retryCount = retryCount + 1, lastError = :error, updatedAt = :timestamp WHERE id = :id")
    suspend fun markFailed(id: String, error: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'SYNCED', updatedAt = :timestamp WHERE status = 'PENDING' OR status = 'FAILED'")
    suspend fun markAllSynced(timestamp: Long = System.currentTimeMillis())
}
