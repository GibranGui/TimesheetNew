package com.example.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Pengelola akun pengguna.
 *
 * Pendaftaran cukup dengan nama, nomor HP, username, dan kata sandi.
 * Semua tersimpan aman di HP ini saja; kata sandi sudah dikunci (di-hash)
 * sehingga tidak terbaca walau HP diperiksa orang lain.
 */
class AccountManager private constructor(private val context: Context) {

    data class Profile(val fullName: String, val phone: String, val username: String)

    private val db by lazy { AppDatabase.getInstance(context) }

    companion object {
        @Volatile
        private var INSTANCE: AccountManager? = null

        fun getInstance(context: Context): AccountManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AccountManager(context.applicationContext).also { INSTANCE = it }
            }

        private const val PREFS = "account_session"
        private const val KEY_ACCOUNT_ID = "active_account_id"
    }

    /** Sesi aktif di HP ini (siapa yang sedang masuk). */
    private fun prefs() =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun activeAccountId(): String? = prefs().getString(KEY_ACCOUNT_ID, null)

    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        activeAccount() != null
    }

    /** Profil pemilik sesi saat ini; null jika belum masuk. */
    suspend fun currentProfile(): Profile? = withContext(Dispatchers.IO) {
        val account = activeAccount() ?: return@withContext null
        Profile(account.fullName, account.phone, account.username)
    }

    suspend fun hasAnyAccount(): Boolean = withContext(Dispatchers.IO) {
        db.userAccountDao().count() > 0
    }

    /**
     * Saran nama driver dari daftar jadwal perusahaan (Google Sheet yang pernah diunduh).
     * Dipakai saat membuat akun baru agar penulisan nama sama persis dengan data kantor,
     * sehingga timesheet di server langsung cocok dengan nama driver tersebut.
     */
    suspend fun suggestDriverNames(query: String): List<String> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        try {
            db.driverTemplateDao().search(q).map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun activeAccount(): UserAccountEntity? {
        val id = activeAccountId() ?: return null
        // Simpan juga username terakhir agar bisa diverifikasi cepat.
        val username = prefs().getString("active_username", null) ?: return null
        val acc = db.userAccountDao().findByUsername(username) ?: return null
        return if (acc.id == id) acc else null
    }

    /**
     * Mendaftarkan akun baru.
     * @return null jika berhasil, atau pesan kesalahan berbahasa awam.
     */
    suspend fun register(
        fullName: String,
        phone: String,
        username: String,
        password: String,
        confirmPassword: String
    ): String? = withContext(Dispatchers.IO) {
        val name = fullName.trim()
        val cleanPhone = phone.trim()
        val user = username.trim()
        if (name.length < 3) return@withContext "Nama minimal 3 huruf ya."
        if (cleanPhone.filter { it.isDigit() }.length < 8)
            return@withContext "Nomor HP belum benar. Tulis nomor aktif minimal 8 angka."
        if (user.length < 4) return@withContext "Nama pengguna minimal 4 huruf/angka."
        if (user.any { it.isWhitespace() })
            return@withContext "Nama pengguna tidak boleh mengandung spasi."
        if (password.length < 4) return@withContext "Kata sandi minimal 4 karakter."
        if (password != confirmPassword) return@withContext "Kata sandi keduanya berbeda, cek lagi ya."

        val dao = db.userAccountDao()
        if (dao.findByUsername(user) != null)
            return@withContext "Nama pengguna \"$user\" sudah dipakai. Pilih nama lain."

        val salt = UUID.randomUUID().toString()
        val account = UserAccountEntity(
            id = UUID.randomUUID().toString(),
            fullName = name,
            phone = cleanPhone,
            username = user,
            salt = salt,
            passwordHash = sha256(salt + password)
        )
        try {
            dao.insert(account)
        } catch (e: Exception) {
            return@withContext "Nama pengguna sudah dipakai. Coba nama lain."
        }
        startSession(account)
        null
    }

    /**
     * Memasukkan akun dengan username & kata sandi.
     * @return null jika berhasil, atau pesan kesalahan berbahasa awam.
     */
    suspend fun login(username: String, password: String): String? =
        withContext(Dispatchers.IO) {
            val account = db.userAccountDao().findByUsername(username.trim())
                ?: return@withContext "Nama pengguna tidak ditemukan. Yakin sudah terdaftar?"
            if (account.passwordHash != sha256(account.salt + password))
                return@withContext "Kata sandi salah. Coba lagi ya."
            db.userAccountDao().touchLogin(account.id, System.currentTimeMillis())
            startSession(account)
            null
        }

    /** Keluar dari sesi. Data shift tetap aman tersimpan di HP ini. */
    fun signOut() {
        prefs().edit().clear().apply()
    }

    private fun startSession(account: UserAccountEntity) {
        prefs().edit()
            .putString(KEY_ACCOUNT_ID, account.id)
            .putString("active_username", account.username)
            .apply()
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
