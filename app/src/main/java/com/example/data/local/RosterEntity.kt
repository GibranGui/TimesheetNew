package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jadwal kerja driver yang bersumber dari Google Sheets.
 * Shift disimpan sebagai daftar kode yang dipisah koma, contoh: "D,N,OFF,CR,I".
 */
@Entity(tableName = "rosters")
data class RosterEntity(
    @PrimaryKey val id: String, // kombinasi nama & unit, unik per baris jadwal
    val driverName: String,
    val vehicleId: String,
    val shiftsCommaSeparated: String,
    val updatedAt: Long
) {
    fun getShiftsList(): List<String> =
        if (shiftsCommaSeparated.isEmpty()) emptyList() else shiftsCommaSeparated.split(",")
}
