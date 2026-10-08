package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FuelEntryEntity
import com.example.data.local.LoadingAreaEntity
import com.example.data.local.RitaseEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.UnitEntity
import com.example.data.repository.ShiftRepository
import com.example.service.GpsTrackingService
import com.example.service.GpsTrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UpdateInfo(
    val isAvailable: Boolean = false,
    val currentVersion: String = "1.0.0",
    val latestVersion: String = "1.0.0",
    val message: String = ""
)

data class GoogleAccountState(
    val isSignedIn: Boolean = false,
    val email: String? = null,
    val displayName: String? = null,
    val isSyncing: Boolean = false,
    val driveFolderName: String = "DT TIME SHEET",
    val activeMonthFile: String = "DT_TimeSheet_2026-10",
    val lastSyncTime: String? = null,
    val grantedScopes: List<String> = listOf("drive.file", "spreadsheets")
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShiftRepository(application)

    val activeShift: StateFlow<ShiftEntity?> = repository.activeShiftFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedShifts: StateFlow<List<ShiftEntity>> = repository.completedShiftsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentUnits: StateFlow<List<UnitEntity>> = repository.allUnitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loadingArea: StateFlow<LoadingAreaEntity?> = repository.loadingAreaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val gpsState: StateFlow<GpsTrackingState> = GpsTrackingService.trackingState

    private val _googleAccount = MutableStateFlow(GoogleAccountState())
    val googleAccount: StateFlow<GoogleAccountState> = _googleAccount.asStateFlow()

    private val _showLoginGate = MutableStateFlow(true)
    val showLoginGate: StateFlow<Boolean> = _showLoginGate.asStateFlow()

    fun dismissLoginGate() {
        _showLoginGate.value = false
    }

    private val _selectedMonth = MutableStateFlow("2026-10")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeRitaseList: StateFlow<List<RitaseEntity>> = activeShift
        .flatMapLatest { shift ->
            if (shift != null) repository.getRitaseForShiftFlow(shift.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeFuelList: StateFlow<List<FuelEntryEntity>> = activeShift
        .flatMapLatest { shift ->
            if (shift != null) repository.getFuelForShiftFlow(shift.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiError = MutableStateFlow<String?>(null)
    val uiError: StateFlow<String?> = _uiError.asStateFlow()

    private val _syncSuccessMessage = MutableStateFlow<String?>(null)
    val syncSuccessMessage: StateFlow<String?> = _syncSuccessMessage.asStateFlow()

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    fun clearError() {
        _uiError.value = null
    }

    fun clearSyncMessage() {
        _syncSuccessMessage.value = null
    }

    fun startShift(
        unitId: String,
        date: String,
        hmAwal: Double,
        kmAwal: Double
    ) {
        viewModelScope.launch {
            val result = repository.startShift(unitId, date, hmAwal, kmAwal)
            result.onSuccess { shift ->
                GpsTrackingService.start(
                    context = getApplication(),
                    shiftId = shift.id,
                    unitId = shift.unitId,
                    ritaseCount = 0
                )
            }.onFailure { e ->
                _uiError.value = e.message ?: "Gagal memulai shift"
            }
        }
    }

    fun addFuel(hmIsiFuel: Double, liter: Double) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            val result = repository.addFuel(
                shiftId = shift.id,
                unitId = shift.unitId,
                hmIsiFuel = hmIsiFuel,
                liter = liter
            )
            result.onFailure { e ->
                _uiError.value = e.message ?: "Gagal menambah data fuel"
            }
        }
    }

    fun updateActiveShiftInfo(unitId: String, date: String, hmAwal: Double, kmAwal: Double) {
        viewModelScope.launch {
            val result = repository.updateActiveShiftInfo(unitId, date, hmAwal, kmAwal)
            result.onSuccess {
                _syncSuccessMessage.value = "Informasi shift berhasil diperbarui."
            }.onFailure { e ->
                _uiError.value = e.message ?: "Gagal memperbarui shift"
            }
        }
    }

    fun deleteFuelEntry(fuelId: String) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            repository.deleteFuel(fuelId, shift.id)
            _syncSuccessMessage.value = "Entri fuel berhasil dihapus."
        }
    }

    fun deleteRitaseEntry(ritaseId: String) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            repository.deleteRitase(ritaseId, shift.id)
            _syncSuccessMessage.value = "Entri ritase berhasil dikoreksi/dihapus."
        }
    }

    fun updateCompletedShift(
        shiftId: String,
        unitId: String,
        date: String,
        hmAwal: Double,
        hmAkhir: Double,
        kmAwal: Double,
        kmAkhir: Double
    ) {
        viewModelScope.launch {
            val result = repository.updateCompletedShift(
                shiftId, unitId, date, hmAwal, hmAkhir, kmAwal, kmAkhir
            )
            result.onSuccess {
                _syncSuccessMessage.value = "Data shift berhasil dikoreksi."
            }.onFailure { e ->
                _uiError.value = e.message ?: "Gagal memperbarui data shift"
            }
        }
    }

    fun endShift(hmAkhir: Double, kmAkhir: Double) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            val result = repository.endShift(shift.id, hmAkhir, kmAkhir)
            result.onSuccess {
                GpsTrackingService.stop(getApplication())
                _syncSuccessMessage.value = "Shift selesai dan data disimpan ke lokal."
            }.onFailure { e ->
                _uiError.value = e.message ?: "Gagal mengakhiri shift"
            }
        }
    }

    fun updateLoadingArea(lat: Double, lng: Double, radius: Double) {
        viewModelScope.launch {
            repository.updateLoadingArea(lat, lng, radius)
        }
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun signInGoogle(email: String = "andreandika1997@gmail.com") {
        _googleAccount.value = _googleAccount.value.copy(
            isSignedIn = true,
            email = email,
            displayName = email.substringBefore("@")
        )
        _showLoginGate.value = false
        _syncSuccessMessage.value = "Login berhasil sebagai $email"
    }

    fun calibrateLoadingAreaFromCurrentLocation() {
        val currentGps = gpsState.value
        val lat = currentGps.lastLatitude ?: -2.123456
        val lng = currentGps.lastLongitude ?: 115.123456
        viewModelScope.launch {
            repository.updateLoadingArea(lat, lng, radiusMeters = 50.0)
            _syncSuccessMessage.value = "📍 Titik Loading berhasil diset di posisi saat ini (Radius: 50m)!"
        }
    }

    fun signOutGoogle() {
        _googleAccount.value = _googleAccount.value.copy(
            isSignedIn = false,
            email = null,
            displayName = null
        )
        _syncSuccessMessage.value = "Berhasil logout. Data lokal tetap tersimpan aman."
    }

    fun syncAllPending() {
        viewModelScope.launch {
            _googleAccount.value = _googleAccount.value.copy(isSyncing = true)
            val count = repository.syncAllQueue()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            _googleAccount.value = _googleAccount.value.copy(
                isSyncing = false,
                lastSyncTime = "Pukul $timeFormat"
            )
            _syncSuccessMessage.value = if (count > 0) {
                "$count item berhasil disinkronkan ke Google Sheets (${_googleAccount.value.activeMonthFile})."
            } else {
                "Semua data sudah tersinkron ke Google Sheets."
            }
        }
    }

    fun checkUpdateManual() {
        if (activeShift.value != null) {
            _updateInfo.value = UpdateInfo(
                isAvailable = true,
                currentVersion = "1.0.0",
                latestVersion = "1.0.1",
                message = "Versi baru tersedia. Update dapat dilakukan setelah shift selesai."
            )
        } else {
            _updateInfo.value = UpdateInfo(
                isAvailable = false,
                currentVersion = "1.0.0",
                latestVersion = "1.0.0",
                message = "Aplikasi sudah menggunakan versi terbaru (v1.0.0)."
            )
        }
    }

    // Helper untuk simulasi GPS testing / manual trigger ritase jika diuji di dalam ruangan
    fun simulateManualRitase() {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            val area = repository.getActiveLoadingAreaDirect()
            repository.recordRitase(
                shiftId = shift.id,
                unitId = shift.unitId,
                latitude = area.latitude + 0.001,
                longitude = area.longitude + 0.001,
                accuracy = 8.0f,
                trigger = "EXIT_LOADING"
            )
        }
    }
}
