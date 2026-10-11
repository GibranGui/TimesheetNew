package com.example.data.remote

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.FuelEntryEntity
import com.example.data.local.RitaseEntity
import com.example.data.local.ShiftEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class SyncStatus {
    object Disabled : SyncStatus()                       // server belum diatur → murni offline
    object Idle : SyncStatus()
    data class Running(val progress: String) : SyncStatus()
    data class Success(val pushed: Int, val pulled: Int) : SyncStatus()
    data class Failed(val message: String) : SyncStatus()
}

/**
 * Mesin sinkronisasi dua arah.
 *
 * - Offline penuh tetap berfungsi: database lokal selalu sumber utama.
 * - Saat online: perubahan lokal dikirim (push), lalu data dari perangkat
 *   lain ditarik (pull). Konflik diselesaikan dengan "terakhir diperbarui menang".
 */
class SyncEngine(
    context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context)
) {
    private val client = SupabaseSyncClient(context)
    private val shiftDao = db.shiftDao()
    private val ritaseDao = db.ritaseDao()
    private val fuelDao = db.fuelDao()

    suspend fun pushPending(): Result<Int> = withContext(Dispatchers.IO) {
        if (!RemoteConfig.isConfigured(context)) return@withContext Result.success(0)
        var total = 0
        try {
            // 1. Shifts
            val shifts = shiftDao.getUnsyncedShifts()
            for (s in shifts) {
                client.pushShift(s.copy(syncedAt = System.currentTimeMillis()))
                    .onSuccess { shiftDao.markShiftSynced(s.id); total++ }
                    .onFailure { return@withContext Result.failure(it) }
            }
            // 2. Ritase (kirim per batch)
            val ritases = ritaseDao.getUnsyncedRitase()
            if (ritases.isNotEmpty()) {
                client.pushRitase(ritases)
                    .onSuccess { ritaseDao.markRitaseSynced(ritases.map { it.id }); total += ritases.size }
                    .onFailure { return@withContext Result.failure(it) }
            }
            // 3. Fuel (kirim per batch)
            val fuels = fuelDao.getUnsyncedFuel()
            if (fuels.isNotEmpty()) {
                client.pushFuel(fuels)
                    .onSuccess { fuelDao.markFuelSynced(fuels.map { it.id }); total += fuels.size }
                    .onFailure { return@withContext Result.failure(it) }
            }
            // Bersihkan data terhapus lama yang sudah tersinkron
            val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            shiftDao.purgeOldDeletedShifts(cutoff)
            ritaseDao.purgeOldDeletedRitase(cutoff)
            fuelDao.purgeOldDeletedFuel(cutoff)

            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullSince(sinceMillis: Long): Result<Int> = withContext(Dispatchers.IO) {
        if (!RemoteConfig.isConfigured(context)) return@withContext Result.success(0)
        var count = 0
        val shifts = client.pullShiftsUpdatedSince(sinceMillis)
            .getOrElse { return@withContext Result.failure(it) }
        val ritases = client.pullRitaseUpdatedSince(sinceMillis)
            .getOrElse { return@withContext Result.failure(it) }
        val fuels = client.pullFuelUpdatedSince(sinceMillis)
            .getOrElse { return@withContext Result.failure(it) }

        db.withTransaction {
            shifts.forEach { remote ->
                val local = getLocalShift(remote.id)
                if (local == null || remote.updatedAt > local.updatedAt) {
                    // Simpan versi remote; jika local masih belum terkirim, biarkan syncedAt lokal dulu
                    val keepUnsynced = local != null && local.syncedAt == 0L &&
                        local.updatedAt >= remote.updatedAt
                    shiftDao.insertShift(
                        remote.copy(
                            syncedAt = if (keepUnsynced) 0L else System.currentTimeMillis(),
                            status = resolveStatus(local, remote)
                        )
                    )
                    count++
                }
            }
            ritases.forEach { remote ->
                val local = getLocalRitase(remote.id)
                if (local == null || remote.updatedAt > local.updatedAt) {
                    ritaseDao.insertRitase(remote.copy(syncedAt = System.currentTimeMillis()))
                    count++
                }
            }
            fuels.forEach { remote ->
                val local = getLocalFuel(remote.id)
                if (local == null || remote.updatedAt > local.updatedAt) {
                    fuelDao.insertFuel(remote.copy(syncedAt = System.currentTimeMillis()))
                    count++
                }
            }
        }
        Result.success(count)
    }

    /** Satu siklus penuh: kirim dulu, lalu tarik. */
    suspend fun syncNow(lastPullAt: Long): SyncStatus {
        if (!RemoteConfig.isConfigured(context)) return SyncStatus.Disabled
        return try {
            val pushed = pushPending().getOrElse { throw IllegalStateException(it.message ?: "Gagal mengirim data") }
            val pulled = pullSince(lastPullAt).getOrElse { throw IllegalStateException(it.message ?: "Gagal mengambil data") }
            SyncStatus.Success(pushed, pulled)
        } catch (e: Exception) {
            SyncStatus.Failed(e.message ?: "Tidak dapat terhubung ke server. Data tetap aman di HP ini.")
        }
    }

    // ---- pembacaan baris tunggal untuk resolusi konflik ----
    private suspend fun getLocalShift(id: String): ShiftEntity? = shiftDao.getShiftByIdDirect(id)

    private suspend fun getLocalRitase(id: String): RitaseEntity? = ritaseDao.getRitaseByIdDirect(id)

    private suspend fun getLocalFuel(id: String): FuelEntryEntity? = fuelDao.getFuelByIdDirect(id)

    /** Jangan timpa shift ACTIVE lokal dengan COMPLETED remote yang lebih baru secara tidak wajar. */
    private fun resolveStatus(local: ShiftEntity?, remote: ShiftEntity): String {
        if (local != null && local.status == "ACTIVE" && remote.status == "COMPLETED" &&
            local.updatedAt >= remote.updatedAt
        ) {
            return "ACTIVE"
        }
        return remote.status
    }
}
