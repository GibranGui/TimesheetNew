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
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
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

data class GpsTrackingState(
    val isRunning: Boolean = false,
    val unitId: String = "",
    val state: GpsState = GpsState.UNKNOWN,
    val distanceMeters: Double = 0.0,
    val accuracy: Float = 0f,
    val ritaseCount: Int = 0,
    val statusMessage: String = "GPS Standby",
    val isGpsDisturbed: Boolean = false,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null
)

class GpsTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: ShiftRepository
    private var wakeLock: PowerManager.WakeLock? = null

    private val engine = GpsRitaseEngine()
    private var currentShiftId: String = ""
    private var currentUnitId: String = ""
    private var currentRitaseCount: Int = 0

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleLocationUpdate(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = ShiftRepository(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val shiftId = intent?.getStringExtra(EXTRA_SHIFT_ID) ?: ""
        val unitId = intent?.getStringExtra(EXTRA_UNIT_ID) ?: ""
        val ritase = intent?.getIntExtra(EXTRA_RITASE_COUNT, 0) ?: 0

        if (shiftId.isNotEmpty()) {
            currentShiftId = shiftId
            currentUnitId = unitId
            currentRitaseCount = ritase

            startForeground(NOTIFICATION_ID, buildNotification("GPS Aktif - $currentUnitId", currentRitaseCount, GpsState.UNKNOWN))

            serviceScope.launch(Dispatchers.IO) {
                val loadingArea = repository.getActiveLoadingAreaDirect()
                engine.setCenter(loadingArea.latitude, loadingArea.longitude, loadingArea.radiusMeters)
            }

            startLocationUpdates()
            _trackingState.value = GpsTrackingState(
                isRunning = true,
                unitId = currentUnitId,
                ritaseCount = currentRitaseCount,
                statusMessage = "Mencari sinyal GPS..."
            )
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(2f)
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
                statusMessage = "Izin lokasi tidak tersedia"
            )
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val eval = engine.processLocation(location)

        val isDisturbed = location.accuracy > engine.minAccuracyMeters

        if (eval.isRitaseTriggered && currentShiftId.isNotEmpty()) {
            currentRitaseCount++
            serviceScope.launch(Dispatchers.IO) {
                repository.recordRitase(
                    shiftId = currentShiftId,
                    unitId = currentUnitId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    trigger = "EXIT_LOADING"
                )
            }
        }

        _trackingState.value = GpsTrackingState(
            isRunning = true,
            unitId = currentUnitId,
            state = eval.state,
            distanceMeters = eval.distanceToLoadingMeters,
            accuracy = eval.accuracy,
            ritaseCount = currentRitaseCount,
            statusMessage = eval.debugReason,
            isGpsDisturbed = isDisturbed,
            lastLatitude = location.latitude,
            lastLongitude = location.longitude
        )

        updateNotification(eval)
    }

    private fun updateNotification(eval: GpsEvaluationResult) {
        val stateName = when (eval.state) {
            GpsState.LOADING -> "LOADING"
            GpsState.TRAVELLING -> "TRAVELLING"
            GpsState.UNKNOWN -> "MENUNGGU POSISI"
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
            .setContentTitle("🚚 DT Time Sheet - GPS Ritase Aktif")
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
                "DT Time Sheet GPS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pelacakan otomatis ritase dump truck saat shift aktif"
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
        _trackingState.value = GpsTrackingState(isRunning = false, statusMessage = "GPS Nonaktif")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "dt_timesheet_location_channel"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_SHIFT_ID = "extra_shift_id"
        const val EXTRA_UNIT_ID = "extra_unit_id"
        const val EXTRA_RITASE_COUNT = "extra_ritase_count"

        private val _trackingState = MutableStateFlow(GpsTrackingState())
        val trackingState = _trackingState.asStateFlow()

        fun start(context: Context, shiftId: String, unitId: String, ritaseCount: Int) {
            val intent = Intent(context, GpsTrackingService::class.java).apply {
                putExtra(EXTRA_SHIFT_ID, shiftId)
                putExtra(EXTRA_UNIT_ID, unitId)
                putExtra(EXTRA_RITASE_COUNT, ritaseCount)
            }
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
