package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.RosterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Mengunduh jadwal kerja driver dari Google Sheets dan menyimpannya ke database lokal,
 * sehingga jadwal tetap bisa dibuka walaupun HP tidak ada kuota / offline.
 */
class RosterSyncManager(private val context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("roster_sync_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    /** URL Google Sheet yang tersimpan; bawaannya adalah sheet resmi jadwal dump truck. */
    fun getSavedSheetUrl(): String =
        prefs.getString(KEY_URL, DEFAULT_SHEET_URL)?.trim().orEmpty().ifEmpty { DEFAULT_SHEET_URL }

    fun saveSheetUrl(url: String) {
        prefs.edit().putString(KEY_URL, url.trim()).apply()
    }

    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, time).apply()
    }

    fun extractSpreadsheetId(urlOrId: String): String {
        val trimmed = urlOrId.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains("docs.google.com/spreadsheets/d/")) {
            val match = Regex("spreadsheets/d/([a-zA-Z0-9-_]+)").find(trimmed)
            if (match != null) return match.groupValues[1]
        }
        // Sudah berupa ID polos
        if (trimmed.matches(Regex("[a-zA-Z0-9-_]{20,}"))) return trimmed
        return ""
    }

    private fun buildCsvExportUrl(spreadsheetUrlOrId: String): String {
        val id = extractSpreadsheetId(spreadsheetUrlOrId)
        return if (id.isNotEmpty()) {
            "https://docs.google.com/spreadsheets/d/$id/export?format=csv"
        } else ""
    }

    /**
     * Mengunduh & mem-parsing jadwal dari Google Sheets.
     * Gagal dengan pesan ramah pengguna bila offline / format sheet tidak dikenali.
     */
    suspend fun fetchRostersFromSheets(spreadsheetUrlOrId: String): Result<List<RosterEntity>> =
        withContext(Dispatchers.IO) {
            val csvUrl = buildCsvExportUrl(spreadsheetUrlOrId)
            if (csvUrl.isEmpty()) {
                return@withContext Result.failure(
                    Exception("Alamat Google Sheet belum benar. Contoh alamat yang benar: https://docs.google.com/spreadsheets/d/xxxx/edit")
                )
            }

            if (!isOnline()) {
                return@withContext Result.failure(
                    Exception("Koneksi internet tidak tersedia. Jadwal terakhir tetap bisa dibuka seperti biasa.")
                )
            }

            try {
                val request = Request.Builder()
                    .url(csvUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Jadwal belum bisa diambil sekarang. Pastikan sheet dapat diakses siapa saja (Bagikan → Semua orang).")
                        )
                    }

                    val csvContent = response.body?.string() ?: ""
                    if (csvContent.isBlank()) {
                        return@withContext Result.failure(Exception("Google Sheet kosong."))
                    }

                    val parsedRosters = parseCsvToRosters(csvContent)
                    if (parsedRosters.isEmpty()) {
                        return@withContext Result.failure(
                            Exception("Format Google Sheet tidak dikenali. Urutan kolom harus: Nama Driver, Unit Kendaraan, lalu jadwal harian.")
                        )
                    }

                    setLastSyncTime(System.currentTimeMillis())
                    Result.success(parsedRosters)
                }
            } catch (e: Exception) {
                Result.failure(Exception("Jadwal gagal diambil. Periksa koneksi internet lalu coba lagi."))
            }
        }

    /**
     * Format baris sheet: Nama,Unit,KodeHari1,KodeHari2,...
     * Header otomatis dilewati. Sel kosong berarti tidak ada perubahan jadwal hari itu.
     */
    fun parseCsvToRosters(csvText: String): List<RosterEntity> {
        val result = mutableListOf<RosterEntity>()
        val now = System.currentTimeMillis()
        for (rawLine in csvText.split("\n")) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val parts = parseCsvLine(line)
            if (parts.size < 2) continue

            val firstCol = parts[0].replace("\"", "").trim()
            val secondCol = parts[1].replace("\"", "").trim()

            // Lewati baris judul
            if (firstCol.equals("NAMA", ignoreCase = true) ||
                firstCol.contains("NAMA DRIVER", ignoreCase = true) ||
                firstCol.equals("DRIVER", ignoreCase = true) ||
                firstCol.contains("JADWAL", ignoreCase = true)
            ) continue

            if (firstCol.isNotEmpty() && secondCol.isNotEmpty()) {
                val shifts = parts.drop(2)
                    .map { it.replace("\"", "").trim().uppercase() }
                    .filter { it.isNotEmpty() }
                    .joinToString(",")

                result.add(
                    RosterEntity(
                        id = "${firstCol.uppercase()}|${secondCol.uppercase()}",
                        driverName = firstCol.uppercase(),
                        vehicleId = secondCol.uppercase(),
                        shiftsCommaSeparated = shifts,
                        updatedAt = now
                    )
                )
            }
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line.toCharArray()) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    companion object {
        // Sheet resmi jadwal dump truck Hanvan (EPM 24)
        const val DEFAULT_SHEET_URL =
            "https://docs.google.com/spreadsheets/d/1Q4GRZEAMyPENYyNk7hOFHrWo0zKxNKberPKVLICI0IQ/edit"

        private const val KEY_URL = "sheet_url"
        private const val KEY_LAST_SYNC = "last_sync_time"
    }
}
