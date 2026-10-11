package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserAccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: UserAccountEntity)

    @Query("SELECT * FROM user_accounts WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun findByUsername(username: String): UserAccountEntity?

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun count(): Int

    @Query("UPDATE user_accounts SET lastLoginAt = :now WHERE id = :id")
    suspend fun touchLogin(id: String, now: Long)
}
