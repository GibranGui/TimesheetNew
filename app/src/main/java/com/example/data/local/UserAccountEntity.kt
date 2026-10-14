package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Akun pengguna aplikasi (disimpan aman di HP ini saja).
 * Kata sandi tidak pernah disimpan apa adanya — sudah dikunci dengan SHA-256 + salt.
 */
@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val phone: String,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = 0L
)
