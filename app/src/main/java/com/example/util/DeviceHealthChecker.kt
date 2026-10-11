package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Pemeriksa kondisi HP yang penting agar pencatatan shift berjalan lancar:
 * 1) Layanan lokasi (GPS) dinyalakan atau belum.
 * 2) Aplikasi masuk daftar penghematan baterai (optimalisasi) atau tidak.
 */
object DeviceHealthChecker {

    /** True bila layanan lokasi (GPS / jaringan) aktif di pengaturan HP. */
    fun isLocationEnabled(context: Context): Boolean {
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * True bila aplikasi DIKECUALIKAN dari optimalisasi baterai (kondisi ideal).
     * False bila aplikasi masih dioptimalkan (berisiko pelacakan shift terhenti
     * saat layar mati). Pada perangkat tanpa PowerManager dianggap aman.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else true
        } catch (_: Exception) {
            true
        }
    }

    /** Membuka halaman pengaturan lokasi HP agar pengguna bisa menyalakan GPS. */
    fun openLocationSettings(activity: Activity) {
        try {
            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } catch (_: Exception) {
            openAppSettings(activity)
        }
    }

    /**
     * Mengarahkan pengguna mengeluarkan aplikasi dari penghematan baterai.
     * Jika dialog sistem tidak tersedia, fallback ke halaman info aplikasi.
     */
    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                activity.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                )
                return
            } catch (_: Exception) {
                // lanjut ke fallback
            }
            try {
                activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                return
            } catch (_: Exception) {
                // lanjut ke fallback
            }
        }
        openAppSettings(activity)
    }

    /** Membuka halaman info aplikasi di pengaturan HP. */
    fun openAppSettings(activity: Activity) {
        try {
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
            )
        } catch (_: Exception) {
            //abaikan jika halaman tidak tersedia
        }
    }
}
