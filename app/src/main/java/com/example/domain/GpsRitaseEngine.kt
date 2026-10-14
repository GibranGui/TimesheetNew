package com.example.domain

import android.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class GpsState {
    UNKNOWN,
    LOADING,
    TRAVELLING
}

data class GpsEvaluationResult(
    val state: GpsState,
    val previousState: GpsState,
    val distanceToLoadingMeters: Double,
    val isRitaseTriggered: Boolean,
    val accuracy: Float,
    val statusMessage: String
)

/**
 * Mesin penghitung ritase otomatis berdasarkan posisi GPS.
 * Ritase dihitung satu kali setiap kali alat keluar dari area muat
 * setelah sebelumnya berada di dalam area tersebut.
 */
class GpsRitaseEngine(
    var loadingLat: Double = -2.123456,
    var loadingLng: Double = 115.123456,
    var loadingRadiusMeters: Double = 50.0,
    var minAccuracyMeters: Float = 15.0f,
    var exitConfirmationCount: Int = 3,
    var enterConfirmationCount: Int = 2,
    var cooldownMillis: Long = 180_000L // 3 menit, mencegah hitungan ganda
) {
    var currentState: GpsState = GpsState.UNKNOWN
        private set

    private var consecutiveExitReadings: Int = 0
    private var consecutiveEnterReadings: Int = 0
    private var lastRitaseTimestamp: Long = 0L

    fun setCenter(lat: Double, lng: Double, radius: Double = loadingRadiusMeters) {
        loadingLat = lat
        loadingLng = lng
        loadingRadiusMeters = radius
    }

    /**
     * Evaluasi posisi terbaru dan kembalikan hasilnya.
     */
    fun processLocation(location: Location): GpsEvaluationResult {
        val accuracy = location.accuracy
        val distance = calculateHaversineDistance(
            location.latitude,
            location.longitude,
            loadingLat,
            loadingLng
        )

        // Abaikan pembacaan yang akurasinya terlalu buruk agar tidak salah hitung
        if (accuracy > minAccuracyMeters) {
            return GpsEvaluationResult(
                state = currentState,
                previousState = currentState,
                distanceToLoadingMeters = distance,
                isRitaseTriggered = false,
                accuracy = accuracy,
                statusMessage = "Sinyal GPS sedang lemah, menunggu posisi yang lebih akurat."
            )
        }

        val isInsideRadius = distance <= loadingRadiusMeters
        val previousState = currentState
        var isRitaseTriggered = false
        var message = ""

        val now = System.currentTimeMillis()

        when (currentState) {
            GpsState.UNKNOWN -> {
                if (isInsideRadius) {
                    consecutiveEnterReadings++
                    if (consecutiveEnterReadings >= enterConfirmationCount) {
                        currentState = GpsState.LOADING
                        consecutiveEnterReadings = 0
                        message = "Posisi terdeteksi di area muat."
                    } else {
                        message = "Menunggu konfirmasi posisi di area muat..."
                    }
                } else {
                    consecutiveEnterReadings = 0
                    message = "Belum berada di area muat. Simpan titik muat saat alat tiba di lokasi."
                }
            }

            GpsState.LOADING -> {
                if (isInsideRadius) {
                    consecutiveExitReadings = 0
                    message = "Berada di area muat (${"%.0f".format(distance)} m dari titik)."
                } else {
                    consecutiveExitReadings++
                    if (consecutiveExitReadings >= exitConfirmationCount) {
                        val timeSinceLastRitase = now - lastRitaseTimestamp
                        if (timeSinceLastRitase >= cooldownMillis || lastRitaseTimestamp == 0L) {
                            currentState = GpsState.TRAVELLING
                            isRitaseTriggered = true
                            lastRitaseTimestamp = now
                            consecutiveExitReadings = 0
                            message = "Ritase tercatat (+1). Alat bergerak keluar dari area muat."
                        } else {
                            val waitSec = ((cooldownMillis - timeSinceLastRitase) / 1000).coerceAtLeast(0)
                            message = "Tunggu sekitar $waitSec detik sebelum ritase berikutnya dicatat."
                        }
                    } else {
                        message = "Mulai bergerak keluar dari area muat..."
                    }
                }
            }

            GpsState.TRAVELLING -> {
                if (isInsideRadius) {
                    consecutiveEnterReadings++
                    if (consecutiveEnterReadings >= enterConfirmationCount) {
                        currentState = GpsState.LOADING
                        consecutiveEnterReadings = 0
                        consecutiveExitReadings = 0
                        message = "Kembali ke area muat. Siap mencatat ritase berikutnya."
                    } else {
                        message = "Mendekat ke area muat..."
                    }
                } else {
                    consecutiveEnterReadings = 0
                    message = "Sedang dalam perjalanan hauling (${"%.0f".format(distance)} m dari titik muat)."
                }
            }
        }

        return GpsEvaluationResult(
            state = currentState,
            previousState = previousState,
            distanceToLoadingMeters = distance,
            isRitaseTriggered = isRitaseTriggered,
            accuracy = accuracy,
            statusMessage = message
        )
    }

    fun reset() {
        currentState = GpsState.UNKNOWN
        consecutiveExitReadings = 0
        consecutiveEnterReadings = 0
        lastRitaseTimestamp = 0L
    }

    companion object {
        fun calculateHaversineDistance(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val r = 6371000.0 // Radius bumi dalam meter
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
