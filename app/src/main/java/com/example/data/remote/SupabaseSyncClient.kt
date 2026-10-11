package com.example.data.remote

import android.content.Context
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

    private fun configured(): Boolean = RemoteConfig.isConfigured(context)

    // ---------- Pemilik data (untuk server online) ----------

    /** ID unik perangkat+akun; dipakai agar data tiap pengguna terpisah di server. */
    fun ownerId(): String {
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

    // ---------- AMBIL (pull) ----------

    suspend fun pullShiftsUpdatedSince(sinceMillis: Long): Result<List<ShiftEntity>> =
        pull("shifts", sinceMillis) { ShiftEntity.fromJson(it) }

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
            .post(arr.toString().toRequestBody(jsonMedia))
            .build())
    }

    private inline fun <T> pull(
        table: String,
        sinceMillis: Long,
        crossinline parse: (JSONObject) -> T?
    ): Result<List<T>> {
        if (!configured()) return Result.failure(IllegalStateException("Server online belum diatur"))
        return try {
            val url = "${baseUrl()}/rest/v1/$table" +
                "?owner_id=eq.${java.net.URLEncoder.encode(ownerId(), "UTF-8")}" +
                "&updated_at=gt.$sinceMillis" +
                "&order=updated_at.asc&limit=500"
            val request = Request.Builder()
                .url(url)
                .header("apikey", key())
                .header("Authorization", "Bearer ${key()}")
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
    put("id", id); put("owner_id", ownerId); put("unit_id", unitId); put("date", date)
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
