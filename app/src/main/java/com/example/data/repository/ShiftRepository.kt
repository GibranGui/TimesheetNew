package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.FuelEntryEntity
import com.example.data.local.LoadingAreaEntity
import com.example.data.local.RitaseEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.UnitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Penyimpanan data sepenuhnya memakai database lokal (SQLite via Room)
 * di perangkat. Tidak ada sinkronisasi cloud maupun akun login.
 */
class ShiftRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val shiftDao = db.shiftDao()
    private val ritaseDao = db.ritaseDao()
    private val fuelDao = db.fuelDao()
    private val unitDao = db.unitDao()
    private val loadingAreaDao = db.loadingAreaDao()

    val activeShiftFlow: Flow<ShiftEntity?> = shiftDao.getActiveShiftFlow()
    val completedShiftsFlow: Flow<List<ShiftEntity>> = shiftDao.getAllCompletedShifts()
    val allUnitsFlow: Flow<List<UnitEntity>> = unitDao.getAllUnitsFlow()
    val loadingAreaFlow: Flow<LoadingAreaEntity?> = loadingAreaDao.getLoadingAreaFlow()

    fun getRitaseForShiftFlow(shiftId: String): Flow<List<RitaseEntity>> =
        ritaseDao.getRitaseForShiftFlow(shiftId)

    fun getFuelForShiftFlow(shiftId: String): Flow<List<FuelEntryEntity>> =
        fuelDao.getFuelForShiftFlow(shiftId)

    suspend fun getActiveShiftDirect(): ShiftEntity? = withContext(Dispatchers.IO) {
        shiftDao.getActiveShiftDirect()
    }

    suspend fun getActiveLoadingAreaDirect(): LoadingAreaEntity = withContext(Dispatchers.IO) {
        loadingAreaDao.getActiveLoadingAreaDirect() ?: LoadingAreaEntity().also {
            loadingAreaDao.insertOrUpdate(it)
        }
    }

    suspend fun startShift(
        unitId: String,
        date: String,
        hmAwal: Double,
        kmAwal: Double,
        driverName: String = ""
    ): Result<ShiftEntity> = withContext(Dispatchers.IO) {
        val trimmedUnit = unitId.trim()
        if (trimmedUnit.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("C/N Unit tidak boleh kosong"))
        }
        if (date.trim().isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Tanggal tidak boleh kosong"))
        }
        if (!isDateValid(date.trim())) {
            return@withContext Result.failure(IllegalArgumentException("Format tanggal belum benar. Gunakan format DD/MM/YYYY, contoh: 15/10/2026"))
        }
        if (hmAwal < 0.0) {
            return@withContext Result.failure(IllegalArgumentException("HM Awal tidak boleh negatif"))
        }
        if (kmAwal < 0.0) {
            return@withContext Result.failure(IllegalArgumentException("KM Awal tidak boleh negatif"))
        }
        val existingActive = shiftDao.getActiveShiftDirect()
        if (existingActive != null) {
            return@withContext Result.failure(IllegalStateException("Selesaikan shift ${existingActive.unitId} yang sedang berjalan terlebih dahulu."))
        }

        unitDao.insertUnit(UnitEntity(cn = trimmedUnit, displayName = trimmedUnit))

        val shiftId = UUID.randomUUID().toString()
        val shift = ShiftEntity(
            id = shiftId,
            unitId = trimmedUnit,
            date = date.trim(),
            hmAwal = hmAwal,
            kmAwal = kmAwal,
            driverName = driverName.trim().uppercase(),
            status = "ACTIVE"
        )
        shiftDao.insertShift(shift)

        Result.success(shift)
    }

    suspend fun recordRitase(
        shiftId: String,
        unitId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        trigger: String = "EXIT_LOADING"
    ): RitaseEntity = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        val ritaseId = UUID.randomUUID().toString()

        val ritase = RitaseEntity(
            id = ritaseId,
            shiftId = shiftId,
            unitId = unitId,
            timeFormatted = formattedTime,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            trigger = trigger,
            status = "VALID"
        )
        ritaseDao.insertRitase(ritase)
        shiftDao.incrementRitase(shiftId)

        ritase
    }

    suspend fun addFuel(
        shiftId: String,
        unitId: String,
        hmIsiFuel: Double,
        liter: Double
    ): Result<FuelEntryEntity> = withContext(Dispatchers.IO) {
        if (liter <= 0.0) {
            return@withContext Result.failure(IllegalArgumentException("Jumlah liter harus lebih besar dari 0"))
        }
        if (hmIsiFuel < 0.0) {
            return@withContext Result.failure(IllegalArgumentException("HM saat pengisian tidak boleh negatif"))
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        val fuelId = UUID.randomUUID().toString()

        val fuel = FuelEntryEntity(
            id = fuelId,
            shiftId = shiftId,
            unitId = unitId,
            timeFormatted = formattedTime,
            hmIsiFuel = hmIsiFuel,
            liter = liter
        )
        fuelDao.insertFuel(fuel)

        val totalFuel = fuelDao.getTotalFuelForShift(shiftId) ?: liter
        shiftDao.updateTotalFuel(shiftId, totalFuel)

        Result.success(fuel)
    }

    suspend fun endShift(
        shiftId: String,
        hmAkhir: Double,
        kmAkhir: Double
    ): Result<ShiftEntity> = withContext(Dispatchers.IO) {
        val currentShift = shiftDao.getActiveShiftDirect()
            ?: return@withContext Result.failure(IllegalStateException("Tidak ada shift aktif"))

        if (hmAkhir < currentShift.hmAwal) {
            return@withContext Result.failure(IllegalArgumentException("HM Akhir tidak boleh lebih kecil dari HM Awal (${currentShift.hmAwal})"))
        }
        if (kmAkhir < currentShift.kmAwal) {
            return@withContext Result.failure(IllegalArgumentException("KM Akhir tidak boleh lebih kecil dari KM Awal (${currentShift.kmAwal.toInt()}))".replace("${currentShift.kmAwal.toInt()}", currentShift.kmAwal.toInt().toString())))
        }

        val totalHm = hmAkhir - currentShift.hmAwal
        val totalKm = kmAkhir - currentShift.kmAwal
        val totalRitase = ritaseDao.countRitase(shiftId)
        val totalFuel = fuelDao.getTotalFuelForShift(shiftId) ?: 0.0

        val completedShift = currentShift.copy(
            hmAkhir = hmAkhir,
            kmAkhir = kmAkhir,
            totalHm = totalHm,
            totalKm = totalKm,
            totalRitase = totalRitase,
            totalFuel = totalFuel,
            status = "COMPLETED",
            updatedAt = System.currentTimeMillis()
        )
        shiftDao.updateShift(completedShift)

        Result.success(completedShift)
    }

    suspend fun updateActiveShiftInfo(
        unitId: String,
        date: String,
        hmAwal: Double,
        kmAwal: Double
    ): Result<ShiftEntity> = withContext(Dispatchers.IO) {
        val currentShift = shiftDao.getActiveShiftDirect()
            ?: return@withContext Result.failure(IllegalStateException("Tidak ada shift aktif"))

        val trimmedUnit = unitId.trim()
        if (trimmedUnit.isEmpty()) return@withContext Result.failure(IllegalArgumentException("C/N Unit tidak boleh kosong"))
        if (date.trim().isEmpty()) return@withContext Result.failure(IllegalArgumentException("Tanggal tidak boleh kosong"))
        if (!isDateValid(date.trim())) return@withContext Result.failure(IllegalArgumentException("Format tanggal belum benar. Gunakan format DD/MM/YYYY."))

        unitDao.insertUnit(UnitEntity(cn = trimmedUnit, displayName = trimmedUnit))
        val updated = currentShift.copy(
            unitId = trimmedUnit,
            date = date.trim(),
            hmAwal = hmAwal,
            kmAwal = kmAwal,
            updatedAt = System.currentTimeMillis()
        )
        shiftDao.updateShift(updated)

        Result.success(updated)
    }

    suspend fun deleteFuel(fuelId: String, shiftId: String) = withContext(Dispatchers.IO) {
        fuelDao.softDeleteById(fuelId)
        val newTotalFuel = fuelDao.getTotalFuelForShift(shiftId) ?: 0.0
        shiftDao.updateTotalFuel(shiftId, newTotalFuel)
    }

    suspend fun deleteRitase(ritaseId: String, shiftId: String) = withContext(Dispatchers.IO) {
        ritaseDao.softDeleteById(ritaseId)
        val newCount = ritaseDao.countRitase(shiftId)
        shiftDao.updateRitaseCount(shiftId, newCount)
    }

    suspend fun updateCompletedShift(
        shiftId: String,
        unitId: String,
        date: String,
        hmAwal: Double,
        hmAkhir: Double,
        kmAwal: Double,
        kmAkhir: Double
    ): Result<ShiftEntity> = withContext(Dispatchers.IO) {
        val existing = shiftDao.getShiftByIdDirect(shiftId)
        if (unitId.trim().isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("C/N Unit tidak boleh kosong"))
        }
        if (!isDateValid(date.trim())) {
            return@withContext Result.failure(IllegalArgumentException("Format tanggal belum benar. Gunakan format DD/MM/YYYY."))
        }
        if (hmAkhir < hmAwal) {
            return@withContext Result.failure(IllegalArgumentException("HM Akhir tidak boleh lebih kecil dari HM Awal"))
        }
        if (kmAkhir < kmAwal) {
            return@withContext Result.failure(IllegalArgumentException("KM Akhir tidak boleh lebih kecil dari KM Awal"))
        }

        val totalHm = hmAkhir - hmAwal
        val totalKm = kmAkhir - kmAwal
        val totalRitase = ritaseDao.countRitase(shiftId)
        val totalFuel = fuelDao.getTotalFuelForShift(shiftId) ?: 0.0

        val updated = ShiftEntity(
            id = shiftId,
            unitId = unitId.trim(),
            date = date.trim(),
            hmAwal = hmAwal,
            hmAkhir = hmAkhir,
            kmAwal = kmAwal,
            kmAkhir = kmAkhir,
            totalHm = totalHm,
            totalKm = totalKm,
            totalRitase = totalRitase,
            totalFuel = totalFuel,
            status = "COMPLETED",
            ownerId = existing?.ownerId.orEmpty(),
            driverName = existing?.driverName.orEmpty(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        shiftDao.updateShift(updated)

        Result.success(updated)
    }

    suspend fun updateLoadingArea(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        name: String = "Area Muat"
    ) = withContext(Dispatchers.IO) {
        val area = LoadingAreaEntity(
            id = "default_loading_area",
            name = name,
            latitude = lat,
            longitude = lng,
            radiusMeters = radiusMeters.coerceIn(10.0, 5000.0),
            enabled = true
        )
        loadingAreaDao.insertOrUpdate(area)
    }

    private fun isDateValid(dateStr: String): Boolean {
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                isLenient = false
            }
            val parsed = format.parse(dateStr) ?: return false
            // Pastikan hasil parsing konsisten dengan teks aslinya
            format.format(parsed) == dateStr
        } catch (_: Exception) {
            false
        }
    }
}
