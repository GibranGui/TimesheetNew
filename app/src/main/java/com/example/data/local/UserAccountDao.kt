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

    /** Versi non-coroutine, dipakai mesin sinkronisasi saat menyiapkan nama pemilik data. */
    @Query("SELECT * FROM user_accounts WHERE username = :username COLLATE NOCASE LIMIT 1")
    fun findByUsernameBlocking(username: String): UserAccountEntity?

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun count(): Int

    /** Semua akun yang pernah dibuat di HP ini (dipakai validasi pendaftaran & multi-akun). */
    @Query("SELECT * FROM user_accounts ORDER BY lastLoginAt DESC")
    suspend fun getAll(): List<UserAccountEntity>

    @Query("UPDATE user_accounts SET lastLoginAt = :now WHERE id = :id")
    suspend fun touchLogin(id: String, now: Long)

    @Update
    suspend fun update(account: UserAccountEntity)

    /** Tandai akun sudah berhasil diunggah ke Supabase. */
    @Query("UPDATE user_accounts SET uploadedToServer = 1 WHERE id = :id")
    suspend fun markUploaded(id: String)
}
