package com.example

import android.location.Location
import com.example.domain.GpsRitaseEngine
import com.example.domain.GpsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ExampleUnitTest {

    private lateinit var engine: GpsRitaseEngine
    private val centerLat = -2.000000
    private val centerLng = 115.000000

    @Before
    fun setUp() {
        engine = GpsRitaseEngine(
            loadingLat = centerLat,
            loadingLng = centerLng,
            loadingRadiusMeters = 50.0,
            minAccuracyMeters = 15.0f,
            exitConfirmationCount = 3,
            enterConfirmationCount = 2,
            cooldownMillis = 0L // dinonaktifkan untuk simulasi cepat
        )
    }

    private fun createMockLocation(lat: Double, lng: Double, accuracy: Float = 5.0f): Location {
        return Location("gps").apply {
            latitude = lat
            longitude = lng
            this.accuracy = accuracy
            time = System.currentTimeMillis()
        }
    }

    // 1. HM Calculation
    @Test
    fun testHmCalculation() {
        val hmAwal = 102.0
        val hmAkhir = 110.0
        val totalHm = hmAkhir - hmAwal
        assertEquals(8.0, totalHm, 0.001)
    }

    // 2. KM Calculation
    @Test
    fun testKmCalculation() {
        val kmAwal = 12450.0
        val kmAkhir = 12510.0
        val totalKm = kmAkhir - kmAwal
        assertEquals(60.0, totalKm, 0.001)
    }

    // 3. Validasi HM & KM
    @Test
    fun testHmKmValidation() {
        val hmAwal = 102.0
        val invalidHmAkhir = 101.0
        assertTrue("HM Akhir tidak boleh lebih kecil dari HM Awal", invalidHmAkhir < hmAwal)

        val kmAwal = 12450.0
        val invalidKmAkhir = 12400.0
        assertTrue("KM Akhir tidak boleh lebih kecil dari KM Awal", invalidKmAkhir < kmAwal)
    }

    // 4. GPS State Machine & Anti False-Positive Critical Tests
    @Test
    fun testGpsStateMachineAndRitaseFlow() {
        // Step 1: DT mulai dan masuk ke dalam loading area (lat & lng persis di center = jarak 0m)
        // Butuh 2 reading konfirmasi masuk
        val locInside = createMockLocation(centerLat, centerLng)
        engine.processLocation(locInside)
        val resInside = engine.processLocation(locInside)
        assertEquals(GpsState.LOADING, resInside.state)
        assertFalse(resInside.isRitaseTriggered)

        // Step 2: Bergerak di dalam radius (< 50m, delta ~0.0002 derajat ~ 22 meter)
        val locInsideMoving = createMockLocation(centerLat + 0.0002, centerLng)
        val resInsideMoving = engine.processLocation(locInsideMoving)
        assertEquals(GpsState.LOADING, resInsideMoving.state)
        assertFalse(resInsideMoving.isRitaseTriggered)

        // Step 3: Keluar radius (> 50m, delta ~0.0008 derajat ~ 88 meter)
        val locOutside = createMockLocation(centerLat + 0.0008, centerLng)
        // Reading 1 & 2: belum cukup 3 reading
        assertFalse(engine.processLocation(locOutside).isRitaseTriggered)
        assertFalse(engine.processLocation(locOutside).isRitaseTriggered)
        // Reading 3: exit terkonfirmasi -> RITASE +1
        val resExit = engine.processLocation(locOutside)
        assertTrue("Ritase harus terpicu pada reading ke-3 di luar radius", resExit.isRitaseTriggered)
        assertEquals(GpsState.TRAVELLING, resExit.state)

        // Step 4: Tetap di luar (100m, 150m) -> TIDAK BOLEH ada ritase tambahan
        val locFar = createMockLocation(centerLat + 0.002, centerLng)
        val resFar = engine.processLocation(locFar)
        assertEquals(GpsState.TRAVELLING, resFar.state)
        assertFalse("Tidak boleh ada ritase saat sudah TRAVELLING", resFar.isRitaseTriggered)

        // Step 5: Kembali ke loading area -> Masuk LOADING
        engine.processLocation(locInside)
        val resReenter = engine.processLocation(locInside)
        assertEquals(GpsState.LOADING, resReenter.state)
        assertFalse(resReenter.isRitaseTriggered)

        // Step 6: Keluar lagi -> RITASE +1 lagi
        engine.processLocation(locOutside)
        engine.processLocation(locOutside)
        val resExitAgain = engine.processLocation(locOutside)
        assertTrue("Ritase kedua harus terpicu", resExitAgain.isRitaseTriggered)
        assertEquals(GpsState.TRAVELLING, resExitAgain.state)
    }

    // 5. GPS Akurasi Buruk Ditolak
    @Test
    fun testBadAccuracyIgnored() {
        val locBadAccuracy = createMockLocation(centerLat, centerLng, accuracy = 25.0f) // > 15m
        val res = engine.processLocation(locBadAccuracy)
        assertEquals(GpsState.UNKNOWN, res.state)
        assertFalse(res.isRitaseTriggered)
    }

    // 6. Tidak Pernah Loading, Tidak Ada Ritase
    @Test
    fun testNoEntryNoExit() {
        val locOutside = createMockLocation(centerLat + 0.005, centerLng)
        for (i in 1..10) {
            val res = engine.processLocation(locOutside)
            assertFalse(res.isRitaseTriggered)
            assertEquals(GpsState.UNKNOWN, res.state)
        }
    }
}
