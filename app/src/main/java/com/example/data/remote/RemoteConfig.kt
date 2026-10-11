package com.example.data.remote

import android.content.Context

/**
 * Pengaturan server online (Supabase).
 *
 * Aplikasi tetap dapat dipakai sepenuhnya secara offline. Setelah alamat
 * server diisi, data akan terkirim otomatis saat ada jaringan, dan bisa
 * ditarik kembali lewat menu "Tarik Data Terbaru".
 */
object RemoteConfig {
    private const val PREFS = "remote_settings"
    private const val KEY_URL = "supabase_url"
    private const val KEY_KEY = "supabase_key"

    fun getUrl(context: Context): String =
        prefs(context).getString(KEY_URL, "")?.trim().orEmpty()

    fun getKey(context: Context): String =
        prefs(context).getString(KEY_KEY, "")?.trim().orEmpty()

    fun isConfigured(context: Context): Boolean =
        getUrl(context).isNotEmpty() && getKey(context).isNotEmpty()

    fun save(context: Context, url: String, key: String) {
        prefs(context).edit()
            .putString(KEY_URL, url.trim())
            .putString(KEY_KEY, key.trim())
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
