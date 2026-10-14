package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.remote.RemoteConfig
import com.example.data.remote.SyncEngine
import com.example.data.repository.ShiftRepository
import com.example.domain.GpsEvaluationResult
import com.example.domain.GpsRitaseEngine
import com.example.domain.GpsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GpsTrackingState(
    val isRunning: Boolean = false,
    val unitId: String = "",
    val state: GpsState = GpsState.UNKNOWN,
    val distanceMeters: Double = 0.0,
    val accuracy: Float = 0f,
    val ritaseCount: Int = 0,
    val statusMessage: String = "Pelacakan belum aktif",
    val isGpsDisturbed: Boolean = false,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null
)

class GpsTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: ShiftRepository
    private var wakeLock: PowerManager.WakeLock? = null

    private val engine = GpsRitaseEngine()
    private var currentShiftId: String = ""
    private var currentUnitId: String = ""
    private var currentRitaseCount: Int = 0
    private var lastAutoSyncAt: Long = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            // Proses semua lokasi terbaru agar perpindahan cepat tetap terdeteksi
            result.locations.lastOrNull()?.let { handleLocationUpdate(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = ShiftRepository(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Wake lock hanya dikantongi saat pelacakan benar-benar berjalan
        if (wakeLock?.isHeld != true) acquireWakeLock()

        when (intent?.action) {
            ACTION_RELOAD_AREA -> {
                reloadLoadingArea()
                return START_STICKY
            }

            ACTION_UPDATE_UNIT -> {
                val unitId = intent.getStringExtra(EXTRA_UNIT_ID).orEmpty()
                if (unitId.isNotEmpty()) {
                    currentUnitId = unitId
                    _trackingState.value = _trackingState.value.copy(unitId = unitId)
                    updateNotificationText()
                }
                return START_STICKY
            }

            ACTION_CORRECT_RITASE -> {
                currentRitaseCount = (currentRitaseCount - 1).coerceAtLeast(0)
                _trackingState.value = _trackingState.value.copy(ritaseCount = currentRitaseCount)
                updateNotificationText()
                return START_STICKY
            }
        }

        val shiftId = intent?.getStringExtra(EXTRA_SHIFT_ID) ?: ""

        if (shiftId.isNotEmpty()) {
            val isNewShift = shiftId != currentShiftId
            currentShiftId = shiftId
            currentUnitId = intent?.getStringExtra(EXTRA_UNIT_ID).orEmpty()
            currentRitaseCount = intent?.getIntExtra(EXTRA_RITASE_COUNT, 0) ?: 0

            if (isNewShift) {
                engine.reset()
            }

            startForeground(NOTIFICATION_ID, buildNotification("Memeriksa sinyal GPS...", currentRitaseCount, GpsState.UNKNOWN))

            serviceScope.launch(Dispatchers.IO) {
                try {
                    val loadingArea = repository.getActiveLoadingAreaDirect()
                    withContext(Dispatchers.Main) {
                        engine.setCenter(loadingArea.latitude, loadingArea.longitude, loadingArea.radiusMeters)
                    }
                } catch (_: Exception) {
                    // Area muat default tetap dipakai bila gagal membaca
                }
            }

            if (!isDeviceLocationEnabled()) {
                _trackingState.value = GpsTrackingState(
                    isRunning = true,
                    unitId = currentUnitId,
                    ritaseCount = currentRitaseCount,
                    isGpsDisturbed = true,
                    statusMessage = "Lokasi (GPS) pada HP belum aktif. Aktifkan layanan lokasi agar ritase dapat dihitung otomatis."
                )
            } else {
                _trackingState.value = GpsTrackingState(
                    isRunning = true,
                    unitId = currentUnitId,
                    ritaseCount = currentRitaseCount,
                    statusMessage = "Mencari sinyal GPS..."
                )
            }

            startLocationUpdates()
        }

        return START_STICKY
    }

    private fun isDeviceLocationEnabled(): Boolean {
        return try {
            val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // GPS dipaksa ke mode akurasi tinggi penuh:
        // - PRIORITY_HIGH_ACCURACY => memakai satelit GPS + jaringan (bukan hemat baterai)
        // - setRequireAccurateMode => menolak lokasi perkiraan, hanya fix presisi tinggi
        // - setWaitForIdleOnApp = false => langsung mengambil fix tanpa menunggu diam
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(2f)
            .apply {
                try {
                    javaClass.getMethod("setRequireAccurateMode", Boolean::class.javaPrimitiveType)
                        .invoke(this, true)
                } catch (_: Exception) {
                    // Tidak tersedia di versi Play Services lama — prioritas tinggi tetap aktif.
                }
            }
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            _trackingState.value = _trackingState.value.copy(
                isGpsDisturbed = true,
                statusMessage = "Izin lokasi belum diberikan. Buka pengaturan aplikasi dan izinkan akses lokasi."
            )
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val eval = engine.processLocation(location)

        val isDisturbed = location.accuracy > engine.minAccuracyMeters

        if (eval.isRitaseTriggered && currentShiftId.isNotEmpty()) {
            currentRitaseCount++
            serviceScope.launch(Dispatchers.IO) {
                try {
                    repository.recordRitase(
                        shiftId = currentShiftId,
                        unitId = currentUnitId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        trigger = "EXIT_LOADING"
                    )
                } catch (_: Exception) {
                    // Pencatatan akan dicoba kembali pada perpindahan berikutnya
                }
                // Kirim otomatis ke server online bila jaringan tersedia (diam-diam, tanpa mengganggu)
                tryAutoSync()
            }
        }

        _trackingState.value = GpsTrackingState(
            isRunning = true,
            unitId = currentUnitId,
            state = eval.state,
            distanceMeters = eval.distanceToLoadingMeters,
            accuracy = eval.accuracy,
            ritaseCount = currentRitaseCount,
            statusMessage = eval.statusMessage,
            isGpsDisturbed = isDisturbed,
            lastLatitude = location.latitude,
            lastLongitude = location.longitude
        )

        updateNotification(eval)
    }

    /**
     * Kirim data ke server online secara berkala saat shift berjalan
     * (maksimal 1x per 2 menit). Bila tidak ada jaringan, data tetap
     * tersimpan di HP dan dicoba lagi pada ritase berikutnya.
     */
    private fun tryAutoSync() {
        if (!RemoteConfig.isConfigured(applicationContext)) return
        val now = System.currentTimeMillis()
        if (now - lastAutoSyncAt < 2L * 60 * 1000) return
        lastAutoSyncAt = now
        serviceScope.launch(Dispatchers.IO) {
            try {
                SyncEngine(applicationContext).pushPending()
            } catch (_: Exception) {
                // Gagal? Tidak masalah, data aman di HP dan akan dicoba lagi nanti.
            }
        }
    }

    private fun reloadLoadingArea() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val loadingArea = repository.getActiveLoadingAreaDirect()
                withContext(Dispatchers.Main) {
                    engine.setCenter(loadingArea.latitude, loadingArea.longitude, loadingArea.radiusMeters)
                }
            } catch (_: Exception) {
                // Pertahankan konfigurasi yang sedang berjalan
            }
        }
    }

    private fun updateNotificationText() {
        val stateName = when (_trackingState.value.state) {
            GpsState.LOADING -> "Di area muat"
            GpsState.TRAVELLING -> "Perjalanan"
            GpsState.UNKNOWN -> "Menunggu posisi"
        }
        val text = "Unit: $currentUnitId | Ritase: $currentRitaseCount | $stateName"
        val notification = buildNotification(text, currentRitaseCount, _trackingState.value.state)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(eval: GpsEvaluationResult) {
        val stateName = when (eval.state) {
            GpsState.LOADING -> "Di area muat"
            GpsState.TRAVELLING -> "Perjalanan"
            GpsState.UNKNOWN -> "Menunggu posisi"
        }
        val text = "Unit: $currentUnitId | Ritase: $currentRitaseCount | $stateName"
        val notification = buildNotification(text, currentRitaseCount, eval.state)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(text: String, ritase: Int, state: GpsState): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DT Time Sheet - Pelacakan Ritase Aktif")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Layanan Lokasi DT Time Sheet",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan status pelacakan ritase saat shift berlangsung"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dt_timesheet:location_wakelock").apply {
            acquire()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        serviceScope.cancel()
        _trackingState.value = GpsTrackingState(isRunning = false, statusMessage = "Pelacakan berhenti")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "dt_timesheet_location_channel"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_SHIFT_ID = "extra_shift_id"
        const val EXTRA_UNIT_ID = "extra_unit_id"
        const val EXTRA_RITASE_COUNT = "extra_ritase_count"

        const val ACTION_RELOAD_AREA = "com.example.action.RELOAD_LOADING_AREA"
        const val ACTION_UPDATE_UNIT = "com.example.action.UPDATE_UNIT"
        const val ACTION_CORRECT_RITASE = "com.example.action.CORRECT_RITASE"

        private val _trackingState = MutableStateFlow(GpsTrackingState())
        val trackingState = _trackingState.asStateFlow()

        fun start(context: Context, shiftId: String, unitId: String, ritaseCount: Int) {
            val intent = Intent(context, GpsTrackingService::class.java).apply {
                putExtra(EXTRA_SHIFT_ID, shiftId)
                putExtra(EXTRA_UNIT_ID, unitId)
                putExtra(EXTRA_RITASE_COUNT, ritaseCount)
            }
            startServiceCompat(context, intent)
        }

        fun reloadLoadingArea(context: Context) {
            val intent = Intent(context, GpsTrackingService::class.java).apply {
                action = ACTION_RELOAD_AREA
            }
            if (_trackingState.value.isRunning) {
                context.startService(intent)
            }
        }

        fun updateUnitInfo(context: Context, unitId: String) {
            val intent = Intent(context, GpsTrackingService::class.java).apply {
                action = ACTION_UPDATE_UNIT
                putExtra(EXTRA_UNIT_ID, unitId)
            }
            if (_trackingState.value.isRunning) {
                context.startService(intent)
            }
        }

        fun correctRitaseCount(context: Context) {
            val intent = Intent(context, GpsTrackingService::class.java).apply {
                action = ACTION_CORRECT_RITASE
            }
            if (_trackingState.value.isRunning) {
                context.startService(intent)
            }
        }

        /**
         * Tampilkan peringatan bila koneksi/izin GPS bermasalah (dipakai pengecekan berkala).
         */
        fun setGpsWarning(context: Context, message: String) {
            val current = _trackingState.value
            if (!current.isRunning) return
            if (current.isGpsDisturbed && current.statusMessage == message) return
            _trackingState.value = current.copy(
                isGpsDisturbed = true,
                statusMessage = message
            )
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(CHANNEL_ID) != null) {
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("DT Time Sheet - Masalah Lokasi")
                        .setContentText(message)
                        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .build()
                    manager.notify(NOTIFICATION_ID, notification)
                }
            } catch (_: Exception) {
            }
        }

        /**
         * Pulihkan status normal setelah GPS kembali terhubung.
         */
        fun clearGpsWarning(context: Context) {
            val current = _trackingState.value
            if (!current.isRunning) return
            _trackingState.value = current.copy(
                isGpsDisturbed = false,
                statusMessage = "GPS kembali terhubung. Pelacakan berjalan normal."
            )
        }

        private fun startServiceCompat(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GpsTrackingService::class.java)
            context.stopService(intent)
        }
    }
}
