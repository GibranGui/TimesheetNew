package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountManager
import com.example.data.local.AppDatabase
import com.example.data.local.FuelEntryEntity
import com.example.data.local.LoadingAreaEntity
import com.example.data.local.RosterEntity
import com.example.data.local.RitaseEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.UnitEntity
import com.example.data.repository.ShiftRepository
import com.example.data.remote.RemoteConfig
import com.example.data.remote.RosterSyncManager
import com.example.data.remote.SyncEngine
import com.example.data.remote.SyncStatus
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

    /** True bila ada versi baru dan popup belum pernah ditampilkan hari ini. */
    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
        try {
            getApplication<Application>()
                .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("last_update_prompt_date", todayKey())
                .apply()
        } catch (_: Exception) {
            //abaikan jika penyimpanan tidak tersedia
        }
    }

    /**
     * Pengecekan pembaruan otomatis saat aplikasi dibuka (maks. 1x per hari).
     * Jika versi baru tersedia, popup akan muncul setelah pengecekan selesai.
     */
    fun checkUpdateOnLaunch() {
        viewModelScope.launch {
            val prefs = getApplication<Application>()
                .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val lastPrompt = prefs.getString("last_update_prompt_date", "") ?: ""
            if (lastPrompt == todayKey()) return@launch

            val before = _updateInfo.value
            checkUpdateManual()
            // hanya tampilkan popup bila benar-benar ada versi baru
            if (_updateInfo.value.isAvailable && _updateInfo.value.latestVersion != before.latestVersion) {
                _showUpdateDialog.value = true
            } else if (_updateInfo.value.isAvailable) {
                _showUpdateDialog.value = true
            }
        }
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ---------- Sinkronisasi online (opsional; offline tetap penuh berfungsi) ----------
    private val syncEngine = SyncEngine(application)
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _serverConfigured = MutableStateFlow(RemoteConfig.isConfigured(application))
    val serverConfigured: StateFlow<Boolean> = _serverConfigured.asStateFlow()

    private var lastPullAt: Long = 0L

    // ---------- Jadwal kerja driver (Google Sheets → tersimpan lokal, tetap bisa dibaca offline) ----------
    private val rosterSyncManager = RosterSyncManager(application)
    private val db = AppDatabase.getInstance(application)

    val rosters: StateFlow<List<RosterEntity>> = db.rosterDao().getAllRosters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRosterSyncing = MutableStateFlow(false)
    val isRosterSyncing: StateFlow<Boolean> = _isRosterSyncing.asStateFlow()

    private val _rosterLastSync = MutableStateFlow(rosterSyncManager.getLastSyncTime())
    val rosterLastSync: StateFlow<Long> = _rosterLastSync.asStateFlow()

    val rosterSheetUrl: String get() = rosterSyncManager.getSavedSheetUrl()

    /**
     * Ambil kode jadwal untuk satu hari kalender (1..31) dari baris roster driver tertentu.
     * Mengembalikan string kosong bila tidak ada data.
     */
    fun rosterCodeForDay(driverName: String, dayOfMonth: Int): String {
        if (driverName.isBlank()) return ""
        val target = driverName.trim().uppercase()
        val row = rosters.value.firstOrNull {
            // Nama di sheet bisa berisi beberapa driver dalam satu baris
            // (contoh: "BUDI SANTOSO / AGUS"), jadi cocokkan juga per bagian.
            val name = it.driverName.trim().uppercase()
            name == target || name.split('/').any { part -> part.trim() == target }
        } ?: return ""
        return row.getShiftsList().getOrElse(dayOfMonth - 1) { "" }.trim().uppercase()
    }

    /** Cocokkan satu baris roster dengan nama driver yang login (termasuk format gabungan). */
    fun rosterMatchesDriver(row: RosterEntity, driverName: String): Boolean {
        if (driverName.isBlank()) return false
        val target = driverName.trim().uppercase()
        val name = row.driverName.trim().uppercase()
        return name == target || name.split('/').any { it.trim() == target }
    }

    init {
        // Ambil jadwal terbaru otomatis setiap hari saat aplikasi dibuka.
        autoSyncRosterDaily()
    }

    /**
     * Sinkronisasi otomatis harian: setiap kali aplikasi dibuka, jadwal kerja
     * diambil ulang dari Google Sheet bila kalender sudah berganti hari.
     * Gagal ambil data (offline) tidak masalah — jadwal terakhir tetap tersimpan di HP.
     */
    private fun autoSyncRosterDaily() {
        // §9 revisi: sinkronisasi jadwal HANYA saat aplikasi dibuka DAN sedang online.
        if (!rosterSyncManager.isOnline()) return
        if (!rosterSyncManager.isDailySyncDue()) return
        if (_isRosterSyncing.value) return
        viewModelScope.launch {
            _isRosterSyncing.value = true
            try {
                val result = rosterSyncManager.fetchRostersFromSheets(rosterSyncManager.getSavedSheetUrl())
                result.onSuccess { list ->
                    val rosterDao = db.rosterDao()
                    rosterDao.deleteAll()
                    rosterDao.insertAll(list)
                    rosterSyncManager.setLastAutoSyncDay(System.currentTimeMillis())
                    _rosterLastSync.value = rosterSyncManager.getLastSyncTime()
                }.onFailure { /* diam-diam saja; pengguna tetap bisa menekan tombol perbarui */ }
            } finally {
                _isRosterSyncing.value = false
            }
        }
    }

    fun syncRosterNow() {
        if (_isRosterSyncing.value) return
        viewModelScope.launch {
            _isRosterSyncing.value = true
            try {
                val result = rosterSyncManager.fetchRostersFromSheets(rosterSyncManager.getSavedSheetUrl())
                result.onSuccess { list ->
                    val rosterDao = db.rosterDao()
                    rosterDao.deleteAll()
                    rosterDao.insertAll(list)
                    rosterSyncManager.setLastAutoSyncDay(System.currentTimeMillis())
                    _rosterLastSync.value = rosterSyncManager.getLastSyncTime()
                    _successMessage.value = "Jadwal berhasil diperbarui (${list.size} driver)."
                }.onFailure { err ->
                    _uiError.value = err.message ?: "Jadwal gagal diperbarui."
                }
            } finally {
                _isRosterSyncing.value = false
            }
        }
    }

    // ---------- Akun pengguna ----------
    private val accountManager = AccountManager.getInstance(application)

    data class AccountState(
        val checked: Boolean = false,   // masih memeriksa sesi awal?
        val loggedIn: Boolean = false,
        val profile: AccountManager.Profile? = null,
        val hasAnyAccount: Boolean = false
    )

    private val _accountState = MutableStateFlow(AccountState())
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    private val _driverSuggestions = MutableStateFlow<List<String>>(emptyList())
    val driverSuggestions: StateFlow<List<String>> = _driverSuggestions.asStateFlow()

    private val _hasDriverList = MutableStateFlow(false)
    val hasDriverList: StateFlow<Boolean> = _hasDriverList.asStateFlow()

    /** Daftar akun yang pernah dibuat di HP ini — untuk pemilih akun / multi-akun (§8). */
    private val _savedAccounts = MutableStateFlow<List<AccountManager.SavedAccount>>(emptyList())
    val savedAccounts: StateFlow<List<AccountManager.SavedAccount>> = _savedAccounts.asStateFlow()

    /** Pilih akun lain yang sudah terdaftar (isi kata sandi tetap required saat login). */
    fun useSavedAccount(accountId: String) {
        viewModelScope.launch {
            val error = accountManager.switchAccount(accountId)
            if (error != null) _uiError.value = error else refreshAccountState()
        }
    }

    /** Cari saran nama driver dari daftar jadwal perusahaan saat mengetik di form pendaftaran. */
    fun searchDriverNames(query: String) {
        viewModelScope.launch {
            _driverSuggestions.value = accountManager.suggestDriverNames(query)
            _hasDriverList.value = accountManager.hasDriverList()
        }
    }

    fun refreshAccountState() {
        viewModelScope.launch {
            try {
                _hasDriverList.value = accountManager.hasDriverList()
                _savedAccounts.value = accountManager.savedAccounts()
                val loggedIn = accountManager.isLoggedIn()
                _accountState.value = if (loggedIn) {
                    AccountState(
                        checked = true,
                        loggedIn = true,
                        profile = accountManager.currentProfile(),
                        hasAnyAccount = accountManager.hasAnyAccount()
                    )
                } else {
                    AccountState(
                        checked = true,
                        loggedIn = false,
                        profile = null,
                        hasAnyAccount = accountManager.hasAnyAccount()
                    )
                }
            } catch (e: Exception) {
                // Jangan biarkan exception DB sesi lama menjatuhkan aplikasi saat dibuka.
                // Tampilkan layar login; data di database tetap utuh (tidak dihapus).
                _accountState.value = AccountState(checked = true, loggedIn = false, profile = null)
                _uiError.value = "Ada masalah membaca akun lama. Silakan login kembali."
            }
        }
    }

    fun registerAccount(
        fullName: String, phone: String,
        password: String, confirmPassword: String
    ) {
        viewModelScope.launch {
            val error = accountManager.register(fullName, phone, password, confirmPassword)
            if (error != null) {
                _uiError.value = error
            } else {
                refreshAccountState()
                _successMessage.value = "Akun berhasil dibuat. Selamat datang, $fullName!"
            }
        }
    }

    /** Login dengan Nama Driver + kata sandi (§8 — tanpa email/username terpisah). */
    fun loginAccount(driverName: String, password: String) {
        viewModelScope.launch {
            val error = accountManager.login(driverName, password)
            if (error != null) {
                _uiError.value = error
            } else {
                refreshAccountState()
                _successMessage.value = "Berhasil masuk. Data Anda siap digunakan."
            }
        }
    }

    fun signOutAccount() {
        accountManager.signOut()
        refreshAccountState()
    }

    fun saveServerSettings(url: String, key: String) {
        if (RemoteConfig.isLocked(getApplication())) {
            _uiError.value = "Alamat server sudah dikunci oleh pengelola aplikasi dan tidak bisa diubah dari HP ini."
            return
        }
        if (RemoteConfig.save(getApplication(), url, key)) {
            _serverConfigured.value = RemoteConfig.isConfigured(getApplication())
            _successMessage.value = "Pengaturan server tersimpan. Data akan terkirim otomatis saat ada jaringan."
            syncNow()
        } else {
            _uiError.value = "Pengaturan server tidak dapat disimpan."
        }
    }

    fun syncNow() {
        if (!RemoteConfig.isConfigured(getApplication())) return
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Running("Menghubungkan ke server...")
            val result = syncEngine.syncNow(lastPullAt)
            _syncStatus.value = result
            if (result is SyncStatus.Success) {
                lastPullAt = System.currentTimeMillis()
                _successMessage.value = when {
                    result.pushed == 0 && result.pulled == 0 -> "Semua data sudah tersinkron."
                    else -> "Sinkronisasi selesai (${result.pushed} data terkirim, ${result.pulled} data diperbarui)."
                }
            }
        }
    }

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
            val driverName = accountManager.currentProfile()?.fullName.orEmpty()
            val result = repository.startShift(unitId, date, hmAwal, kmAwal, driverName)
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
