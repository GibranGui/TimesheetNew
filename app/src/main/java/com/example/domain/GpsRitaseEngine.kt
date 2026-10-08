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
    val debugReason: String
)

class GpsRitaseEngine(
    var loadingLat: Double = -2.123456,
    var loadingLng: Double = 115.123456,
    var loadingRadiusMeters: Double = 50.0,
    var minAccuracyMeters: Float = 15.0f,
    var exitConfirmationCount: Int = 3,
    var enterConfirmationCount: Int = 2,
    var cooldownMillis: Long = 180_000L // 3 menit
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
     * Evaluasi lokasi baru.
     * Mengembalikan GpsEvaluationResult.
     */
    fun processLocation(location: Location): GpsEvaluationResult {
        val accuracy = location.accuracy
        val distance = calculateHaversineDistance(
            location.latitude,
            location.longitude,
            loadingLat,
            loadingLng
        )

        // 1. Validasi akurasi GPS (Anti False-Positive)
        if (accuracy > minAccuracyMeters) {
            return GpsEvaluationResult(
                state = currentState,
                previousState = currentState,
                distanceToLoadingMeters = distance,
                isRitaseTriggered = false,
                accuracy = accuracy,
                debugReason = "Akurasi GPS buruk (${accuracy}m > ${minAccuracyMeters}m)"
            )
        }

        val isInsideRadius = distance <= loadingRadiusMeters
        val previousState = currentState
        var isRitaseTriggered = false
        var reason = ""

        val now = System.currentTimeMillis()

        when (currentState) {
            GpsState.UNKNOWN -> {
                if (isInsideRadius) {
                    consecutiveEnterReadings++
                    if (consecutiveEnterReadings >= enterConfirmationCount) {
                        currentState = GpsState.LOADING
                        consecutiveEnterReadings = 0
                        reason = "Masuk Loading Area (Inisialisasi)"
                    } else {
                        reason = "Menunggu konfirmasi masuk loading ($consecutiveEnterReadings/$enterConfirmationCount)"
                    }
                } else {
                    consecutiveEnterReadings = 0
                    reason = "Di luar area loading pada start ($distance m). Belum pernah LOADING."
                }
            }

            GpsState.LOADING -> {
                if (isInsideRadius) {
                    consecutiveExitReadings = 0
                    reason = "Tetap di dalam Loading Area ($distance m <= $loadingRadiusMeters m)"
                } else {
                    consecutiveExitReadings++
                    // Cek konfirmasi exit berturut-turut & cooldown
                    if (consecutiveExitReadings >= exitConfirmationCount) {
                        val timeSinceLastRitase = now - lastRitaseTimestamp
                        if (timeSinceLastRitase >= cooldownMillis || lastRitaseTimestamp == 0L) {
                            // VALID EXIT LOADING -> RITASE +1
                            currentState = GpsState.TRAVELLING
                            isRitaseTriggered = true
                            lastRitaseTimestamp = now
                            consecutiveExitReadings = 0
                            reason = "EXIT_LOADING VALID: Ritase +1! Jarak=$distance m"
                        } else {
                            val waitSec = (cooldownMillis - timeSinceLastRitase) / 1000
                            reason = "Exit terdeteksi tapi masih dalam cooldown ($waitSec detik tersisa)"
                        }
                    } else {
                        reason = "Deteksi di luar area ($consecutiveExitReadings/$exitConfirmationCount reading)"
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
                        reason = "Kembali ke Loading Area. Siap untuk ritase berikutnya."
                    } else {
                        reason = "Mendekati loading area ($consecutiveEnterReadings/$enterConfirmationCount reading)"
                    }
                } else {
                    consecutiveEnterReadings = 0
                    reason = "Sedang hauling / travelling di luar loading area ($distance m)"
                }
            }
        }

        return GpsEvaluationResult(
            state = currentState,
            previousState = previousState,
            distanceToLoadingMeters = distance,
            isRitaseTriggered = isRitaseTriggered,
            accuracy = accuracy,
            debugReason = reason
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
            val r = 6371000.0 // Earth radius in meters
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
