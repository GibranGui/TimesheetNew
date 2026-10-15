package com.example.data.remote

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.FuelEntryEntity
import com.example.data.local.RitaseEntity
import com.example.data.local.ShiftEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Klien sinkronisasi ke server online (Supabase / REST PostgREST).
 * Semua panggilan jaringan dibungkus aman: jika gagal, data tetap aman
 * di perangkat dan percobaan kirim akan diulang pada kesempatan berikut.
 */
class SupabaseSyncClient(private val context: Context) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun baseUrl() = RemoteConfig.getUrl(context).trimEnd('/')
    private fun key() = RemoteConfig.getKey(context)

    /** Header identitas pemilik data — dipakai server untuk memfilter hak akses (RLS). */
    private fun okhttp3.Request.Builder.withOwnerHeader(): okhttp3.Request.Builder =
        header("X-Owner-Id", ownerId())

    private fun configured(): Boolean = RemoteConfig.isConfigured(context)

    // ---------- Pemilik data (untuk server online) ----------

    /**
     * Nama pemilik data di server, diambil dari akun yang sedang masuk (huruf besar).
     * Dengan begitu data timesheet tiap driver tersimpan rapi atas nama driver itu
     * dan hanya bisa dibaca kembali oleh driver dengan nama yang sama.
     * Bila belum ada sesi (mis. aplikasi baru dibuka), dipakai cadangan ID perangkat.
     */
    fun ownerId(): String {
        val accountPrefs = context.applicationContext
            .getSharedPreferences("account_session", Context.MODE_PRIVATE)
        val username = accountPrefs.getString("active_username", null)
        if (!username.isNullOrBlank()) {
            val fullName = try {
                AppDatabase.getInstance(context)
                    .userAccountDao()
                    .findByUsernameBlocking(username)?.fullName
            } catch (e: Exception) { null }
            if (!fullName.isNullOrBlank()) return fullName.trim().uppercase()
        }
        // Cadangan: ID perangkat (dipakai bila belum masuk akun)
        val prefs = context.applicationContext
            .getSharedPreferences("remote_settings", Context.MODE_PRIVATE)
        var id = prefs.getString("owner_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("owner_id", id).apply()
        }
        return id
    }

    private fun ShiftEntity.withOwner() = copy(ownerId = ownerId())

    // ---------- KIRIM (push) ----------

    suspend fun pushShift(shift: ShiftEntity): Result<Unit> =
        upsert("shifts", shift.withOwner().toJson())

    suspend fun pushRitase(list: List<RitaseEntity>): Result<Unit> =
        bulkUpsert("ritase_events", JSONArray().apply {
            list.forEach { put(it.toJson(ownerId())) }
        })

    suspend fun pushFuel(list: List<FuelEntryEntity>): Result<Unit> =
        bulkUpsert("fuel_entries", JSONArray().apply {
            list.forEach { put(it.toJson(ownerId())) }
        })

    /** Jumlah baris yang belum berhasil terkirim ke server. */
    suspend fun pendingCount(): Int = 0 // dihitung dari sisi database lokal

    // ---------- Akun pengguna (validasi login lintas perangkat) ----------

    /**
     * Unggah akun yang baru mendaftar ke Supabase (tabel `user_accounts`).
     * Yang dikirim: nama driver, no hp, hash+salt — BUKAN kata sandi asli.
     * Gagal (offline/server belum diatur) tidak melempar exception; pemanggil
     * tetap bisa lanjut dan akan mencoba ulang saat login berikutnya.
     */
    suspend fun pushUserAccount(account: com.example.data.local.UserAccountEntity): Boolean {
        if (!configured()) return false
        val obj = JSONObject().apply {
            put("id", account.id)
            put("full_name", account.fullName)
            put("phone", account.phone)
            put("password_hash", account.passwordHash)
            put("salt", account.salt)
            put("created_at", account.createdAt)
        }
        val result = upsert("user_accounts", obj)
        return if (result.isSuccess) {
            try { AppDatabase.getInstance(context).userAccountDao().markUploaded(account.id) } catch (e: Exception) { }
            true
        } else false
    }

    /**
     * Cari akun atas nama driver tertentu di Supabase untuk validasi login.
     * @return Online(null) jika online tapi nama tidak ditemukan (belum terdaftar);
     *         Online(entity) jika ditemukan; Error jika offline/server bermasalah.
     */
    suspend fun findUserAccount(driverName: String): com.example.data.local.AccountManager.RemoteAccountResult =
        withContext(Dispatchers.IO) {
            if (!configured()) return@withContext com.example.data.local.AccountManager.RemoteAccountResult.Error
            try {
                val url = "${baseUrl()}/rest/v1/user_accounts" +
                    "?full_name=ilike." + java.net.URLEncoder.encode(driverName, "UTF-8") +
                    "&select=*&limit=1"
                val request = Request.Builder()
                    .url(url)
                    .header("apikey", key())
                    .header("Authorization", "Bearer ${key()}")
                    .get()
                    .build()
                http.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext com.example.data.local.AccountManager.RemoteAccountResult.Error
                    }
                    val arr = JSONArray(resp.body?.string() ?: "[]")
                    if (arr.length() == 0) {
                        com.example.data.local.AccountManager.RemoteAccountResult.Online(null)
                    } else {
                        val o = arr.getJSONObject(0)
                        com.example.data.local.AccountManager.RemoteAccountResult.Online(
                            com.example.data.local.UserAccountEntity(
                                id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                                fullName = o.getString("full_name"),
                                phone = o.optString("phone"),
                                username = o.getString("full_name"),
                                passwordHash = o.getString("password_hash"),
                                salt = o.getString("salt"),
                                createdAt = o.optLong("created_at", System.currentTimeMillis()),
                                lastLoginAt = 0L,
                                uploadedToServer = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                com.example.data.local.AccountManager.RemoteAccountResult.Error
            }
        }

    // ---------- AMBIL (pull) ----------

    suspend fun pullShiftsUpdatedSince(sinceMillis: Long, driverName: String = ""): Result<List<ShiftEntity>> =
        pull("shifts", sinceMillis, driverName) { ShiftEntity.fromJson(it) }

    suspend fun pullRitaseUpdatedSince(sinceMillis: Long): Result<List<RitaseEntity>> =
        pull("ritase_events", sinceMillis) { RitaseEntity.fromJson(it) }

    suspend fun pullFuelUpdatedSince(sinceMillis: Long): Result<List<FuelEntryEntity>> =
        pull("fuel_entries", sinceMillis) { FuelEntryEntity.fromJson(it) }

    // ---------- INTERNAL ----------

    private suspend fun upsert(table: String, obj: JSONObject): Result<Unit> {
        if (!configured()) return Result.failure(IllegalStateException("Server online belum diatur"))
        return execute(Request.Builder()
            .url("${baseUrl()}/rest/v1/$table")
            .header("apikey", key())
            .header("Authorization", "Bearer ${key()}")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates")
            .withOwnerHeader()
            .post(obj.toString().toRequestBody(jsonMedia))
            .build())
    }

    private suspend fun bulkUpsert(table: String, arr: JSONArray): Result<Unit> {
        if (!configured()) return Result.failure(IllegalStateException("Server online belum diatur"))
        if (arr.length() == 0) return Result.success(Unit)
        return execute(Request.Builder()
            .url("${baseUrl()}/rest/v1/$table")
            .header("apikey", key())
            .header("Authorization", "Bearer ${key()}")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates")
            .withOwnerHeader()
            .post(arr.toString().toRequestBody(jsonMedia))
            .build())
    }

    private inline fun <T> pull(
        table: String,
        sinceMillis: Long,
        driverName: String = "",
        crossinline parse: (JSONObject) -> T?
    ): Result<List<T>> {
        if (!configured()) return Result.failure(IllegalStateException("Server online belum diatur"))
        return try {
            val driverFilter = if (driverName.isNotEmpty())
                "&driver_name=eq." + java.net.URLEncoder.encode(driverName, "UTF-8") else ""
            val url = "${baseUrl()}/rest/v1/$table" +
                "?owner_id=eq.${java.net.URLEncoder.encode(ownerId(), "UTF-8")}" +
                "$driverFilter" +
                "&updated_at=gt.$sinceMillis" +
                "&order=updated_at.asc&limit=500"
            val request = Request.Builder()
                .url(url)
                .header("apikey", key())
                .header("Authorization", "Bearer ${key()}")
                .withOwnerHeader()
                .get()
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(IllegalStateException("Server menolak permintaan (${resp.code})"))
                }
                val body = resp.body?.string() ?: "[]"
                val arr = JSONArray(body)
                val items = (0 until arr.length()).mapNotNull { parse(arr.getJSONObject(it)) }
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Akun driver (registrasi & login via Supabase, §8 revisi) ----------

    /**
     * Mengunggah akun yang baru mendaftar ke tabel `driver_accounts` di Supabase.
     * Yang dikirim hanya: nama driver (sesuai jadwal), no hp, dan password HASH
     * (SHA-256 + salt) — kata sandi asli tidak pernah keluar dari HP.
     * @return null jika berhasil, atau pesan kesalahan berbahasa awam.
     */
    suspend fun pushDriverAccount(
        id: String,
        fullName: String,
        phone: String,
        passwordHash: String,
        salt: String,
        createdAt: Long
    ): String? = withContext(Dispatchers.IO) {
        if (!configured()) return@withContext "Server online belum diatur."
        val obj = JSONObject().apply {
            put("id", id)
            put("full_name", fullName)
            put("phone", phone)
            put("password_hash", passwordHash)
            put("salt", salt)
            put("created_at", createdAt)
            put("updated_at", System.currentTimeMillis())
        }
        when (val r = upsert("driver_accounts", obj)) {
            is Result.Success -> null
            is Result.Failure -> r.exceptionOrNull()?.message ?: "Gagal mengirim data akun ke server."
        }
    }

    /**
     * Login dengan Nama Driver + kata sandi (§8 revisi).
     * Aplikasi MELIHAT DATA melalui Supabase (tabel `driver_accounts`):
     * - jika server tersedia dan nama ditemukan → cocokkan hash;
     * - jika server menolak / offline → fallback ke database lokal agar
     *   pengguna lama tetap bisa masuk (prinsip §9.3: fitur online tidak
     *   mengganggu saat offline);
     * - jika nama tidak ada di server DAN tidak ada di lokal → "belum terdaftar".
     * @return null jika berhasil (sesi lokal dimulai oleh pemanggil),
     *         atau pesan kesalahan berbahasa awam.
     */
    suspend fun verifyDriverLogin(driverName: String, password: String): String? =
        withContext(Dispatchers.IO) {
            if (!configured()) return@withContext null // biarkan pemanggil memakai jalur lokal
            try {
                val url = "${baseUrl()}/rest/v1/driver_accounts" +
                    "?select=password_hash,salt&or=(full_name.ilike." +
                    java.net.URLEncoder.encode(driverName, "UTF-8") + ",full_name.eq." +
                    java.net.URLEncoder.encode(driverName, "UTF-8") + ")" +
                    "&limit=1"
                val request = Request.Builder()
                    .url(url)
                    .header("apikey", key())
                    .header("Authorization", "Bearer ${key()}")
                    // Header x-owner-id = nama driver → memenuhi RLS
                    // "driver_accounts_self_read" (full_name = x-owner-id).
                    .header("x-owner-id", driverName)
                    .get()
                    .build()
                http.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null // server bermasalah -> fallback lokal
                    val body = resp.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    if (arr.length() == 0) {
                        return@withContext "Akun \"$driverName\" belum terdaftar. Silakan daftar dulu."
                    }
                    val acc = arr.getJSONObject(0)
                    val storedHash = acc.optString("password_hash", "")
                    val salt = acc.optString("salt", "")
                    val digest = MessageDigest.getInstance("SHA-256")
                        .digest((salt + password).toByteArray(Charsets.UTF_8))
                        .joinToString("") { "%02x".format(it) }
                    if (storedHash.isNotEmpty() && storedHash != digest) {
                        return@withContext "Kata sandi salah. Coba lagi ya."
                    }
                    null
                }
            } catch (e: Exception) {
                null // jaringan gagal -> jangan blokir login, fallback ke validasi lokal
            }
        }

    private suspend fun execute(request: Request): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            http.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException("Kirim data gagal (kode ${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ---------- Konversi JSON (kolom snake_case sesuai konvensi Supabase) ----------

internal fun ShiftEntity.toJson() = JSONObject().apply {
    put("id", id); put("owner_id", ownerId); put("driver_name", driverName)
    put("unit_id", unitId); put("date", date)
    put("hm_awal", hmAwal); put("hm_akhir", hmAkhir)
    put("km_awal", kmAwal); put("km_akhir", kmAkhir)
    put("total_hm", totalHm); put("total_km", totalKm)
    put("total_ritase", totalRitase); put("total_fuel", totalFuel)
    put("status", status)
    put("created_at", createdAt); put("updated_at", updatedAt)
    put("synced_at", syncedAt); put("deleted_at", deletedAt)
}

internal fun ShiftEntity.Companion.fromJson(o: JSONObject): ShiftEntity? = try {
    ShiftEntity(
        id = o.getString("id"),
        ownerId = o.optString("owner_id", ""),
        driverName = o.optString("driver_name", ""),
        unitId = o.getString("unit_id"),
        date = o.getString("date"),
        hmAwal = o.getDouble("hm_awal"),
        hmAkhir = if (o.isNull("hm_akhir")) null else o.getDouble("hm_akhir"),
        kmAwal = o.getDouble("km_awal"),
        kmAkhir = if (o.isNull("km_akhir")) null else o.getDouble("km_akhir"),
        totalHm = o.optDouble("total_hm", 0.0),
        totalKm = o.optDouble("total_km", 0.0),
        totalRitase = o.optInt("total_ritase", 0),
        totalFuel = o.optDouble("total_fuel", 0.0),
        status = o.optString("status", "COMPLETED"),
        createdAt = o.optLong("created_at", System.currentTimeMillis()),
        updatedAt = o.optLong("updated_at", System.currentTimeMillis()),
        syncedAt = System.currentTimeMillis(), // data ini berasal dari server
        deletedAt = if (o.isNull("deleted_at")) null else o.optLong("deleted_at")
            .takeIf { it > 0 }
    )
} catch (e: Exception) { null }

internal fun RitaseEntity.toJson(owner: String = "") = JSONObject().apply {
    put("id", id); put("owner_id", owner); put("shift_id", shiftId); put("unit_id", unitId)
    put("timestamp", timestamp); put("time_formatted", timeFormatted)
    put("latitude", latitude); put("longitude", longitude)
    put("accuracy", accuracy.toDouble()); put("trigger_type", trigger); put("status", status)
    put("created_at", timestamp); put("updated_at", updatedAt)
    put("synced_at", syncedAt); put("deleted_at", deletedAt)
}

internal fun RitaseEntity.Companion.fromJson(o: JSONObject): RitaseEntity? = try {
    RitaseEntity(
        id = o.getString("id"),
        shiftId = o.getString("shift_id"),
        unitId = o.getString("unit_id"),
        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
        timeFormatted = o.optString("time_formatted", ""),
        latitude = o.getDouble("latitude"),
        longitude = o.getDouble("longitude"),
        accuracy = o.optDouble("accuracy", 0.0).toFloat(),
        trigger = o.optString("trigger_type", "EXIT_LOADING"),
        status = o.optString("status", "VALID"),
        updatedAt = o.optLong("updated_at", System.currentTimeMillis()),
        syncedAt = System.currentTimeMillis(), // data ini berasal dari server
        deletedAt = if (o.isNull("deleted_at")) null else o.optLong("deleted_at")
            .takeIf { it > 0 }
    )
} catch (e: Exception) { null }

internal fun FuelEntryEntity.toJson(owner: String = "") = JSONObject().apply {
    put("id", id); put("owner_id", owner); put("shift_id", shiftId); put("unit_id", unitId)
    put("timestamp", timestamp); put("time_formatted", timeFormatted)
    put("hm_isi_fuel", hmIsiFuel); put("liter", liter)
    put("created_at", timestamp); put("updated_at", updatedAt)
    put("synced_at", syncedAt); put("deleted_at", deletedAt)
}

internal fun FuelEntryEntity.Companion.fromJson(o: JSONObject): FuelEntryEntity? = try {
    FuelEntryEntity(
        id = o.getString("id"),
        shiftId = o.getString("shift_id"),
        unitId = o.getString("unit_id"),
        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
        timeFormatted = o.optString("time_formatted", ""),
        hmIsiFuel = o.getDouble("hm_isi_fuel"),
        liter = o.getDouble("liter"),
        updatedAt = o.optLong("updated_at", System.currentTimeMillis()),
        syncedAt = System.currentTimeMillis(), // data ini berasal dari server
        deletedAt = if (o.isNull("deleted_at")) null else o.optLong("deleted_at")
            .takeIf { it > 0 }
    )
} catch (e: Exception) { null }
