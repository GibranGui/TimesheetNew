package com.example.data.remote

import android.content.Context

/**
 * Pengaturan server online (Supabase).
 *
 * Aplikasi tetap dapat dipakai sepenuhnya secara offline. Setelah alamat
 * server diisi, data akan terkirim otomatis saat ada jaringan, dan bisa
 * ditarik kembali lewat menu "Tarik Data Terbaru".
 *
 * PENTING: alamat & kunci server dikunci oleh pemilik aplikasi.
 * Pengguna biasa TIDAK bisa mengubahnya dari dalam aplikasi — kolom
 * pengaturan sengaja dibuat terkunci agar data tidak dikirim ke server
 * milik orang lain.
 */
object RemoteConfig {
    private const val PREFS = "remote_settings"
    private const val KEY_URL = "supabase_url"
    private const val KEY_KEY = "supabase_key"
    private const val KEY_LOCKED = "server_locked"

    /**
     * Alamat server bawaan yang ditanam oleh pembuat aplikasi.
     * Isi string ini dengan URL project Supabase Anda, contoh:
     *   "https://abcdefghijklm.supabase.co"
     * Biarkan kosong jika belum punya server (aplikasi murni offline).
     */
    private const val BUILT_IN_URL = "https://swzsbuurnkjqdfrwuvqf.supabase.co"

    /**
     * Kunci akses (publishable key) Supabase yang ditanam oleh pembuat aplikasi.
     * Contoh: "eyJhbGciOi..." (Publishable key di Project Settings -> API).
     */
    private const val BUILT_IN_KEY = "sb_publishable_5W1JroG8vZjvXz3Tdq043w_j2KUa3if"

    fun getUrl(context: Context): String {
        if (BUILT_IN_URL.isNotEmpty()) return BUILT_IN_URL
        // Hanya nilai lama hasil sinkronisasi admin yang dipakai; pengguna tak bisa mengisi manual.
        return prefs(context).getString(KEY_URL, "")?.trim().orEmpty()
    }

    fun getKey(context: Context): String {
        if (BUILT_IN_KEY.isNotEmpty()) return BUILT_IN_KEY
        return prefs(context).getString(KEY_KEY, "")?.trim().orEmpty()
    }

    fun isConfigured(context: Context): Boolean =
        getUrl(context).isNotEmpty() && getKey(context).isNotEmpty()

    /** Server dianggap "dikunci" bila alamat ditanam langsung oleh pembuat aplikasi. */
    fun isLocked(context: Context): Boolean =
        BUILT_IN_URL.isNotEmpty() && BUILT_IN_KEY.isNotEmpty()

    /**
     * Menyimpan pengaturan server.
     * Ditolak (hasil false) bila alamat server sudah dikunci oleh pembuat aplikasi,
     * sehingga driver/pengguna lain tidak bisa mengarahkan data ke server asing.
     */
    fun save(context: Context, url: String, key: String): Boolean {
        if (isLocked(context)) return false
        prefs(context).edit()
            .putString(KEY_URL, url.trim())
            .putString(KEY_KEY, key.trim())
            .putBoolean(KEY_LOCKED, true)
            .apply()
        return true
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
