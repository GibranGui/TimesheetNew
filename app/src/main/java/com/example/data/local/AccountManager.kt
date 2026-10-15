package com.example.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Pengelola akun pengguna.
 *
 * Pendaftaran cukup dengan Nama Driver (sesuai jadwal kerja) + kata sandi (§8 — tanpa email/username).
 * Semua tersimpan aman di HP ini saja; kata sandi sudah dikunci (di-hash)
 * sehingga tidak terbaca walau HP diperiksa orang lain.
 */
class AccountManager private constructor(private val context: Context) {

    data class Profile(val fullName: String, val phone: String)

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
        Profile(account.fullName, account.phone)
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

    /** True bila daftar nama driver dari jadwal kantor sudah tersimpan di HP ini. */
    suspend fun hasDriverList(): Boolean = withContext(Dispatchers.IO) {
        try { db.driverTemplateDao().count() > 0 } catch (e: Exception) { false }
    }

    /**
     * Akun-akun yang pernah dibuat di HP ini (untuk pemilih akun / multi-akun §8 —
     * 1 unit = 2 driver beda shift, satu HP bisa dipakai bergantian).
     */
    data class SavedAccount(val id: String, val fullName: String)

    suspend fun savedAccounts(): List<SavedAccount> = withContext(Dispatchers.IO) {
        try {
            db.userAccountDao().getAll().map { SavedAccount(it.id, it.fullName) }
        } catch (e: Exception) { emptyList() }
    }

    /** Pindah sesi ke akun lain yang sudah terdaftar di HP ini (tanpa kata sandi baru,
     * pengguna tetap diminta memasukkan sandi saat memilih lewat layar login). */
    suspend fun switchAccount(accountId: String): String? = withContext(Dispatchers.IO) {
        val acc = try { db.userAccountDao().getAll().firstOrNull { it.id == accountId } }
                  catch (e: Exception) { null } ?: return@withContext "Akun tidak ditemukan."
        startSession(acc)
        null
    }

    private suspend fun activeAccount(): UserAccountEntity? {
        val id = activeAccountId() ?: return null
        // Simpan juga username terakhir agar bisa diverifikasi cepat.
        val username = prefs().getString("active_username", null) ?: return null
        val acc = db.userAccountDao().findByUsername(username) ?: return null
        return if (acc.id == id) acc else null
    }

    /**
     * Mendaftarkan akun baru (§8: TANPA email — cukup Nama Driver + Password).
     * Nama harus sama persis dengan driver yang terdaftar di Google Sheet jadwal;
     * begitu berhasil, Nama + Password (di-hash) langsung tersimpan dan sesi dimulai.
     * @return null jika berhasil, atau pesan kesalahan berbahasa awam.
     */
    suspend fun register(
        fullName: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): String? = withContext(Dispatchers.IO) {
        val name = fullName.trim().replace(Regex("\\s+"), " ")
        val cleanPhone = phone.trim()
        if (name.length < 3) return@withContext "Nama minimal 3 huruf ya."
        if (phone.isNotEmpty() && cleanPhone.filter { it.isDigit() }.isNotEmpty() &&
            cleanPhone.filter { it.isDigit() }.length < 8
        ) return@withContext "Nomor HP belum benar. Tulis nomor aktif minimal 8 angka atau kosongkan."
        if (password.length < 4) return@withContext "Kata sandi minimal 4 karakter."
        if (password != confirmPassword) return@withContext "Kata sandi keduanya berbeda, cek lagi ya."

        // Jika daftar jadwal sudah pernah termuat, pastikan nama sesuai jadwal kantor.
        val templateDao = db.driverTemplateDao()
        val rosterLoaded = try { templateDao.count() > 0 } catch (e: Exception) { false }
        if (rosterLoaded) {
            val nameMatches = try { templateDao.existsExact(name) } catch (e: Exception) { false }
            if (!nameMatches) {
                return@withContext "Nama \"$name\" tidak terdaftar di jadwal kerja kantor. Pilih nama dari saran di bawah kolom nama, atau tulis persis seperti di jadwal."
            }
        }

        val dao = db.userAccountDao()
        // Kunci login = nama driver (dicocokkan tanpa memperhatikan besar/kecil huruf).
        val existingAccounts = try { dao.getAll() } catch (e: Exception) { emptyList() }
        if (existingAccounts.any { it.fullName.equals(name, ignoreCase = true) }) {
            return@withContext "Nama \"$name\" sudah terdaftar. Silakan login dengan kata sandi Anda."
        }

        val salt = UUID.randomUUID().toString()
        val account = UserAccountEntity(
            id = UUID.randomUUID().toString(),
            fullName = name,
            phone = cleanPhone,
            username = name,
            salt = salt,
            passwordHash = sha256(salt + password)
        )
        try {
            dao.insert(account)
        } catch (e: Exception) {
            return@withContext "Nama sudah terdaftar. Silakan login."
        }
        // §8 revisi: unggah data akun (nama, no hp, password HASH — bukan sandi asli)
        // ke Supabase agar bisa dipakai login dari HP lain. Kegagalan upload TIDAK
        // memblokir pendaftaran (prinsip offline-first §9.3) — akun tetap aktif lokal.
        try {
            com.example.data.remote.SupabaseSyncClient(context).pushDriverAccount(
                id = account.id,
                fullName = account.fullName,
                phone = account.phone,
                passwordHash = account.passwordHash,
                salt = account.salt,
                createdAt = account.createdAt
            )
        } catch (e: Exception) { /* diam-diam; sinkron menyusul */ }
        startSession(account)
        null
    }

    /**
     * Login dengan Nama Driver + kata sandi (§8).
     * Aplikasi WAJIB memvalidasi apakah akun sudah terdaftar atau belum:
     * - belum mendaftar → minta user mendaftar dulu;
     * - sudah mendaftar tapi sandi salah → tolak;
     * - cocok → langsung masuk.
     * @return null jika berhasil, atau pesan kesalahan berbahasa awam.
     */
    suspend fun login(driverName: String, password: String): String? =
        withContext(Dispatchers.IO) {
            val name = driverName.trim().replace(Regex("\\s+"), " ")
            if (name.isBlank()) return@withContext "Nama driver tidak boleh kosong."
            // §8 revisi: validasi MELALUI SUPABASE terlebih dahulu (data akun yang
            // diunggah saat pendaftaran). Saat offline/server bermasalah, fungsi ini
            // mengembalikan null (= tidak menolak) lalu dilanjutkan cek lokal (§9.3).
            val remoteError = try {
                com.example.data.remote.SupabaseSyncClient(context)
                    .verifyDriverLogin(name, password)
            } catch (e: Exception) { null }
            if (remoteError != null) return@withContext remoteError

            val account = db.userAccountDao().findByUsername(name)
                ?: return@withContext "Akun \"$name\" belum terdaftar. Silakan daftar dulu."
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
