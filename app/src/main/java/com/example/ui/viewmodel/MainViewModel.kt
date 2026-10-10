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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UpdateInfo(
    val isAvailable: Boolean = false,
    val currentVersion: String = "1.0",
    val latestVersion: String = "1.0",
    val message: String = "",
    val downloadUrl: String = ""
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

    val gpsState: StateFlow<GpsTrackingState> = GpsTrackingService.trackingState

    private val _selectedMonth = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    )
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

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    fun clearError() {
        _uiError.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
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
                _successMessage.value = "Shift dimulai. Pelacakan ritase otomatis aktif."
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
            result.onSuccess {
                _successMessage.value = "Pengisian bahan bakar tersimpan."
            }.onFailure { e ->
                _uiError.value = e.message ?: "Gagal menyimpan data bahan bakar"
            }
        }
    }

    fun updateActiveShiftInfo(unitId: String, date: String, hmAwal: Double, kmAwal: Double) {
        viewModelScope.launch {
            val result = repository.updateActiveShiftInfo(unitId, date, hmAwal, kmAwal)
            result.onSuccess {
                GpsTrackingService.updateUnitInfo(getApplication(), unitId.trim())
                _successMessage.value = "Informasi shift berhasil diperbarui."
            }.onFailure { e ->
                _uiError.value = e.message ?: "Gagal memperbarui shift"
            }
        }
    }

    fun deleteFuelEntry(fuelId: String) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            repository.deleteFuel(fuelId, shift.id)
            _successMessage.value = "Data bahan bakar berhasil dihapus."
        }
    }

    fun deleteRitaseEntry(ritaseId: String) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            repository.deleteRitase(ritaseId, shift.id)
            GpsTrackingService.correctRitaseCount(getApplication())
            _successMessage.value = "Ritase terakhir berhasil dikoreksi."
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
                _successMessage.value = "Data shift berhasil dikoreksi."
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
                _successMessage.value = "Shift selesai. Semua data tersimpan di HP Anda."
            }.onFailure { e ->
                _uiError.value = e.message ?: "Gagal mengakhiri shift"
            }
        }
    }

    fun updateLoadingArea(lat: Double, lng: Double, radius: Double) {
        viewModelScope.launch {
            repository.updateLoadingArea(lat, lng, radius)
            GpsTrackingService.reloadLoadingArea(getApplication())
            _successMessage.value = "Area loading berhasil disimpan."
        }
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun calibrateLoadingAreaFromCurrentLocation() {
        val currentGps = gpsState.value
        val lat = currentGps.lastLatitude
        val lng = currentGps.lastLongitude
        if (lat == null || lng == null) {
            _uiError.value = "Sinyal GPS belum diterima. Pastikan lokasi (GPS) HP aktif dan berada di luar ruangan, lalu coba lagi."
            return
        }
        viewModelScope.launch {
            repository.updateLoadingArea(lat, lng, radiusMeters = 50.0)
            GpsTrackingService.reloadLoadingArea(getApplication())
            _successMessage.value = "Titik loading berhasil disimpan pada posisi saat ini (radius 50 m)."
        }
    }

    /**
     * Cek koneksi GPS berkala: memastikan izin lokasi & layanan GPS HP aktif.
     * Hanya menampilkan peringatan saat pelacakan shift sedang berjalan.
     */
    fun refreshGpsConnectivity() {
        val state = gpsState.value
        if (!state.isRunning) return

        val context = getApplication<Application>()

        val hasLocationPermission =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            GpsTrackingService.setGpsWarning(
                context,
                "Izin lokasi belum diberikan. Buka pengaturan aplikasi dan izinkan akses lokasi agar ritase tercatat."
            )
            return
        }

        val locationOn = try {
            val manager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            manager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }

        if (!locationOn) {
            GpsTrackingService.setGpsWarning(
                context,
                "Lokasi (GPS) pada HP sedang mati. Nyalakan mode lokasi agar ritase dapat dihitung otomatis."
            )
        } else if (state.isGpsDisturbed && state.lastLatitude != null) {
            // GPS sudah kembali normal
            GpsTrackingService.clearGpsWarning(context)
        }
    }

    fun checkUpdateManual() {
        viewModelScope.launch {
            _updateInfo.value = _updateInfo.value.copy(message = "Memeriksa pembaruan...")
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.github.com/repos/GibranGui/TimesheetNew/releases/latest")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string() ?: ""
                            val json = JSONObject(bodyString)
                            val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                            val htmlUrl = json.optString(
                                "html_url",
                                "https://github.com/GibranGui/TimesheetNew/releases"
                            )

                            var apkUrl = htmlUrl
                            val assets = json.optJSONArray("assets")
                            if (assets != null) {
                                for (i in 0 until assets.length()) {
                                    val asset = assets.getJSONObject(i)
                                    val name = asset.optString("name", "")
                                    if (name.endsWith(".apk", ignoreCase = true)) {
                                        apkUrl = asset.optString("browser_download_url", htmlUrl)
                                        break
                                    }
                                }
                            }

                            val currentVersion = getCurrentVersionName()
                            val hasNewer = tagName.isNotEmpty() && isNewerVersion(tagName, currentVersion)

                            _updateInfo.value = UpdateInfo(
                                isAvailable = hasNewer,
                                currentVersion = currentVersion,
                                latestVersion = tagName.ifEmpty { currentVersion },
                                message = if (hasNewer) {
                                    "Versi baru v$tagName tersedia untuk diunduh."
                                } else {
                                    "Aplikasi Anda sudah versi terbaru (v$currentVersion)."
                                },
                                downloadUrl = apkUrl
                            )
                        } else {
                            val v = getCurrentVersionName()
                            _updateInfo.value = UpdateInfo(
                                isAvailable = false,
                                currentVersion = v,
                                latestVersion = v,
                                message = "Belum ada informasi pembaruan yang tersedia."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                val v = getCurrentVersionName()
                _updateInfo.value = UpdateInfo(
                    isAvailable = false,
                    currentVersion = v,
                    latestVersion = v,
                    message = "Gagal memeriksa pembaruan. Periksa koneksi internet Anda."
                )
            }
        }
    }

    private fun getCurrentVersionName(): String {
        return try {
            val context = getApplication<Application>()
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val a = latest.split(".").map { it.trim().toIntOrNull() ?: 0 }
        val b = current.split(".").map { it.trim().toIntOrNull() ?: 0 }
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
