package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.FuelEntryEntity
import com.example.data.local.LoadingAreaEntity
import com.example.data.local.RitaseEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.SyncQueueEntity
import com.example.data.local.UnitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ShiftRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val shiftDao = db.shiftDao()
    private val ritaseDao = db.ritaseDao()
    private val fuelDao = db.fuelDao()
    private val unitDao = db.unitDao()
    private val loadingAreaDao = db.loadingAreaDao()
    private val syncQueueDao = db.syncQueueDao()

    val activeShiftFlow: Flow<ShiftEntity?> = shiftDao.getActiveShiftFlow()
    val completedShiftsFlow: Flow<List<ShiftEntity>> = shiftDao.getAllCompletedShifts()
    val allUnitsFlow: Flow<List<UnitEntity>> = unitDao.getAllUnitsFlow()
    val loadingAreaFlow: Flow<LoadingAreaEntity?> = loadingAreaDao.getLoadingAreaFlow()
    val pendingSyncCountFlow: Flow<Int> = syncQueueDao.getPendingCountFlow()

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
        kmAwal: Double
    ): Result<ShiftEntity> = withContext(Dispatchers.IO) {
        val trimmedUnit = unitId.trim()
        if (trimmedUnit.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("C/N Unit tidak boleh kosong"))
        }
        if (date.trim().isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Tanggal tidak boleh kosong"))
        }
        val existingActive = shiftDao.getActiveShiftDirect()
        if (existingActive != null) {
            return@withContext Result.failure(IllegalStateException("Sudah ada shift aktif untuk unit ${existingActive.unitId}"))
        }

        // Simpan unit ke daftar recent master
        unitDao.insertUnit(UnitEntity(cn = trimmedUnit, displayName = trimmedUnit))

        val shiftId = UUID.randomUUID().toString()
        val shift = ShiftEntity(
            id = shiftId,
            unitId = trimmedUnit,
            date = date.trim(),
            hmAwal = hmAwal,
            kmAwal = kmAwal,
            status = "ACTIVE"
        )
        shiftDao.insertShift(shift)

        // Queue sync
        val monthPeriod = extractMonthPeriod(date)
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = "SHIFT",
                entityId = shiftId,
                action = "INSERT",
                monthPeriod = monthPeriod
            )
        )

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

        // Queue sync
        val activeShift = shiftDao.getActiveShiftDirect()
        val monthPeriod = activeShift?.let { extractMonthPeriod(it.date) } ?: SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = "RITASE",
                entityId = ritaseId,
                action = "INSERT",
                monthPeriod = monthPeriod
            )
        )

        ritase
    }

    suspend fun addFuel(
        shiftId: String,
        unitId: String,
        hmIsiFuel: Double,
        liter: Double
    ): Result<FuelEntryEntity> = withContext(Dispatchers.IO) {
        if (liter <= 0.0) {
            return@withContext Result.failure(IllegalArgumentException("Liter Fuel harus lebih besar dari 0"))
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

        val activeShift = shiftDao.getActiveShiftDirect()
        val monthPeriod = activeShift?.let { extractMonthPeriod(it.date) } ?: SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = "FUEL",
                entityId = fuelId,
                action = "INSERT",
                monthPeriod = monthPeriod
            )
        )

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
            return@withContext Result.failure(IllegalArgumentException("HM Akhir ($hmAkhir) tidak boleh lebih kecil dari HM Awal (${currentShift.hmAwal})"))
        }
        if (kmAkhir < currentShift.kmAwal) {
            return@withContext Result.failure(IllegalArgumentException("KM Akhir ($kmAkhir) tidak boleh lebih kecil dari KM Awal (${currentShift.kmAwal})"))
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

        val monthPeriod = extractMonthPeriod(completedShift.date)
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = "SHIFT",
                entityId = shiftId,
                action = "UPDATE",
                monthPeriod = monthPeriod
            )
        )

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

        unitDao.insertUnit(UnitEntity(cn = trimmedUnit, displayName = trimmedUnit))
        val updated = currentShift.copy(
            unitId = trimmedUnit,
            date = date.trim(),
            hmAwal = hmAwal,
            kmAwal = kmAwal,
            updatedAt = System.currentTimeMillis()
        )
        shiftDao.updateShift(updated)

        val monthPeriod = extractMonthPeriod(updated.date)
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = "SHIFT",
                entityId = updated.id,
                action = "UPDATE",
                monthPeriod = monthPeriod
            )
        )
        Result.success(updated)
    }

    suspend fun deleteFuel(fuelId: String, shiftId: String) = withContext(Dispatchers.IO) {
        fuelDao.deleteFuelById(fuelId)
        val newTotalFuel = fuelDao.getTotalFuelForShift(shiftId) ?: 0.0
        shiftDao.updateTotalFuel(shiftId, newTotalFuel)
    }

    suspend fun deleteRitase(ritaseId: String, shiftId: String) = withContext(Dispatchers.IO) {
        ritaseDao.deleteRitaseById(ritaseId)
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
        if (hmAkhir < hmAwal) {
            return@withContext Result.failure(IllegalArgumentException("HM Akhir ($hmAkhir) tidak boleh lebih kecil dari HM Awal ($hmAwal)"))
        }
        if (kmAkhir < kmAwal) {
            return@withContext Result.failure(IllegalArgumentException("KM Akhir ($kmAkhir) tidak boleh lebih kecil dari KM Awal ($kmAwal)"))
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
            updatedAt = System.currentTimeMillis()
        )
        shiftDao.updateShift(updated)

        val monthPeriod = extractMonthPeriod(updated.date)
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = "SHIFT",
                entityId = shiftId,
                action = "UPDATE",
                monthPeriod = monthPeriod
            )
        )
        Result.success(updated)
    }

    suspend fun updateLoadingArea(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        name: String = "Pit Loading Area"
    ) = withContext(Dispatchers.IO) {
        val area = LoadingAreaEntity(
            id = "default_loading_area",
            name = name,
            latitude = lat,
            longitude = lng,
            radiusMeters = radiusMeters,
            enabled = true
        )
        loadingAreaDao.insertOrUpdate(area)
    }

    suspend fun syncAllQueue(): Int = withContext(Dispatchers.IO) {
        val pendingList = syncQueueDao.getPendingItems()
        val count = pendingList.size
        if (count > 0) {
            syncQueueDao.markAllSynced()
        }
        count
    }

    private fun extractMonthPeriod(dateStr: String): String {
        return try {
            val parts = dateStr.split("/", "-")
            if (parts.size >= 3) {
                // assume DD/MM/YYYY
                val year = parts[2]
                val month = parts[1].padStart(2, '0')
                "$year-$month"
            } else {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            }
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        }
    }
}
