# Catatan Penataan Ulang Frontend — TimesheetNew
Tanggal: 14 Oktober 2026
Repo: https://github.com/GibranGui/TimesheetNew.git

## 1. Kondisi Saat Ini (Hasil Analisis)
- Navigasi menggunakan state manual (`mutableStateOf`), **bukan** Jetpack Navigation → state hilang saat rotasi layar, back-stack tidak dikelola.
- Terdapat **4 tab bawah** dengan masalah inkonsistensi:
  - Label tab **"Akun"** membuka `SettingsScreen` (isi = Pengaturan) → label tidak sesuai isi.
  - `AccountScreen` ada tapi **terpisah / tidak terpasang** ke tab mana pun.
  - `HistoryScreen` **disembunyikan secara manual** (bukan lewat mekanisme navigasi yang benar).
  - Warna indikator tab **tidak seragam**.
  - **Tidak ada TopAppBar** di layar-layar utama.
- Ada istilah yang salah: **"Roaster"** (seharusnya "Jadwal" / "Roster").
- Ada **artefak debug** yang tertinggal di UI (teks debug muncul di layar).
- Terjadi **tumpang tindih fungsi** antara isi tab dengan layar lain (History/Settings bisa diakses dari dua tempat berbeda).

## 2. Rencana Penataan Ulang Bar Tab
Struktur baru: **5 tab** dengan tanggung jawab jelas (mengikuti prinsip Material 3, maks. 5 tujuan):

| # | Tab        | Isi / Tanggung Jawab                                        |
|---|------------|-------------------------------------------------------------|
| 1 | **Jadwal**     | Hanya kalender jadwal (perbaiki istilah "Roaster" → "Jadwal") |
| 2 | **Aktivitas**  | Dashboard shift ATAU form mulai/akhir shift                  |
| 3 | **Rekapan**    | Gabungan riwayat + laporan; BERSIH dari teks debug           |
| 4 | **Profil**     | Data akun (`AccountScreen` dipindah ke sini)                 |
| 5 | **Pengaturan** | Konfigurasi teknis (`SettingsScreen`, label sesuai isi)      |

Catatan isi tab:
- Setiap tab hanya punya SATU tanggung jawab — hapus tumpang tindih fungsi.
- Konten tiap layar harus konsisten dan jelas (tidak berubah-ubah tanpa kejelasan).
- `HistoryScreen` dijadikan **rute tersendiri** (bukan disembunyikan manual); saat rute ini aktif, bottom bar otomatis disembunyikan lewat mekanisme navigasi.

## 3. Referensi Arsitektur yang Disarankan
### Opsi A — Minimal (rapikan yang ada dulu)
- Samakan label vs isi tab (gabungkan Account ke Settings, ATAU buat tab Profil baru).
- Seragamkan warna indikator tab.
- Ganti istilah "Roaster" → "Jadwal".
- Hapus artefak debug dari UI.

### Opsi B — Rekomendasi (migrasi Jetpack Navigation Compose) ✅
- Gunakan pola standar: `Scaffold + NavigationBar + NavHost`.
- Migrasi dari `mutableStateOf` ke `NavHost` → back-stack & state terjaga saat rotasi.
- Bottom bar terhubung ke rute navigasi (bukan boolean/manual hide).
- Tambahkan `TopAppBar` per layar untuk konsistensi.

### Opsi C — Desain Material 3
- Maksimal **5 tujuan/tab**.
- Ikon gaya **Rounded**.
- Warna indikator & state terpilih seragam (pakai token tema M3, bukan hardcode).

## 4. Referensi Open-Source untuk Dipelajari
- **Tivi** (jakewharton) — pola navigasi Compose production-ready.
- **Now in Android** (android/nowinandroid) — referensi resmi multi-module + `Scaffold + NavigationBar + NavHost`.

## 5. Legenda Kode Google Sheet (Konfirmasi Pengguna, 14 Okt 2026)
Sumber data jadwal = Google Sheet (`RosterSyncManager` mengunduh CSV dari
`https://docs.google.com/spreadsheets/d/1Q4GRZEAMyPENYyNk7hOFHrWo0zKxNKberPKVLICI0IQ/edit`).
Format baris sheet: `Nama, Unit, KodeHari1, KodeHari2, ...`. Kode yang sah:

| Kode | Arti                                  | Seharusnya di UI        |
|------|---------------------------------------|--------------------------|
| D    | Shift day (siang)                     | chip biru siang          |
| N    | Shift night (malam)                   | chip malam               |
| CR   | Cuti Roaster                          | chip cuti                |
| CTD  | Cuti tahunan Day                      | chip cuti (terang/siang) |
| CTN  | Cuti tahunan Night                    | chip cuti (gelap/malam)  |
| I    | Induksi                               | warna sendiri (bukan cuti)|

### Temuan ketidaksesuaian kode vs legenda (di `RosterScreen.kt`)
1. **Kode "CT" fiktif** — muncul di `shiftColor` (baris 80), `shiftSoftBg` (91),
   `shiftLabel` → "Cuti tahunan" (102), dan `ABSENT_CODES` (111). Tidak ada "CT"
   di sheet; yang benar adalah **CTD** dan **CTN**. Hapus/normalisasi.
2. **"I" (Induksi) digolongkan sebagai cuti** — `CR, CT, CTD, I -> CodeLeave`
   (baris 80 & 91). Induksi bukan cuti, harus punya warna/label sendiri.
3. **Label "CTD" salah arah** — saat ini "Cuti tahunan (siang)/(malam)" masih OK,
   tapi pastikan CTD = Day (tampilan siang) dan CTN = Night (tampilan malam);
   sekarang CTN memakai warna shift malam penuh (`CodeNight`), bukan warna cuti.
4. **Tab D/N di halaman Roaster** (`shiftTab == "D"/"N"`, baris 162–183) memfilter
   hanya driver dengan kode persis "D"/"N" hari ini — konsekuensi: pemegang **CTD/CTN**
   tidak muncul di tab mana pun meski mereka "hadir secara administrasi". Pertimbangkan
   badge status cuti terpisah per tab.
5. Judul layar masih **"Roaster Kerja"** (baris 218) — sesuai catatan §1, ganti ke
   "Jadwal Kerja" (istilah "Cut/Roaster" tetap dipakai hanya untuk keterangan CR).

## 6. Model Unit & Driver Spare (Konfirmasi Pengguna, 14 Okt 2026)
- **1 unit = 2 driver dengan shift BERBEDA** (bukan 2 driver 1 shift):
  satu masuk siang (D), satunya masuk malam (N). Mereka saling menjadi **pasangan**.
- **Driver spare**: tidak punya unit dan tidak punya pasangan. Tugasnya
  **menggantikan** saat unit kosong.
- Unit dianggap **kosong / butuh pengganti** bila salah satu drivernya berstatus:
  **CR, CTD, CTN, OFF, atau I** (semua = libur/kosong; OFF = jadwal off resmi).

### Struktur Tab Roaster (barunya)
Tab dalam layar Roaster: **Semua | Siang | Malam**
- **Siang**: daftar unit shift siang hari itu → menampilkan driver D **beserta
  pasangannya** yang shift malam (N maupun status liburnya: CR/CTD/CTN/OFF/I).
- **Malam**: kebalikannya — driver N beserta pasangannya yang shift siang.
- **Urutan baris (kunci UX untuk spare)**: semua unit dengan status
  **CR / CTD / CTN / I / OFF ditempatkan PALING ATAS**, baru kemudian unit normal.
  Dengan begitu driver spare langsung melihat unit mana yang sedang kosong.

### Aturan Tampilan Harian (Koreksi, 14 Okt 2026) ✅ PENTING
- Tab **Roaster (Semua/Siang/Malam) HANYA menampilkan 1 hari saja** — yaitu
  tanggal yang sedang dipilih (default: hari ini). **Bukan** rentang seluruh minggu/bulan.
- Alasannya: spare bisa **langsung melihat unit kosong pada hari ia bekerja**,
  tanpa harus menelusuri banyak tanggal.
- Implikasi teknis:
  - Filter data per kolom tanggal di sheet (`KodeHari[n]` untuk tanggal terpilih),
    bukan render semua kolom hari sekaligus.
  - Cukup tampilkan **label tanggal tunggal** (mis. "Senin, 14 Okt") + navigasi
    ganti hari sederhana (← hari sebelumnya / berikutnya →) bila diperlukan.
  - Pengurutan prioritas (libur di atas) dihitung **per hari yang ditampilkan**,
    sehingga daftar selalu singkat: hanya unit kosong hari itu di puncak, sisanya normal.
  - Kalender penuh/multi-hari, jika tetap dibutuhkan, pindah ke layar/tab lain
    (lihat §2 tab Jadwal) — jangan dicampur dengan tampilan harian Roaster.

## 7. Tab Utama / Home Saat Aplikasi Dibuka (Konfirmasi Pengguna, 14 Okt 2026) ✅ PENTING
Tab pertama yang tampil saat aplikasi dibuka adalah **Jadwal Saya (Home driver login)** —
bukan Roaster spare. Isinya:

1. **Kalender penuh 1 bulan** milik driver yang sedang login (difilter dari sheet
   berdasarkan nama/unit driver login).
   - **Hari ini ditandai jelas** (highlight/outline khusus, mis. lingkaran M3 primary).
   - Setiap sel tanggal menampilkan kode status: D / N / CR / CTD / CTN / OFF / I
     (warna mengikuti legenda §5 yang sudah dinormalisasi).
2. **Kartu ringkasan total per bulan berjalan**:
   - **Total HM** (hour meter) yang sudah dicatat driver tersebut.
   - **Total Ritase** yang sudah dicatat.
   - Sumber angka = hasil agregasi pencatatan (§8/FAB di bawah), bukan dari sheet.
3. **Sesi pencatatan HM/Ritase yang SEDANG AKTIF** (bila ada):
   - Ditampilkan sebagai banner/kartu status: unit, shift, jam mulai,
     HM/Ritase sementara, tombol lanjutkan/selesai.
   - Bila tidak ada sesi aktif, banner disembunyikan (atau diganti hint "Belum ada sesi aktif").
4. **Tombol + (pencatatan HM/Ritase) yang MENONJOL**:
   - Bentuk **FAB (FloatingActionButton) / Extended FAB** dengan **warna berbeda**
     dari tema umum (mis. aksen hijau/secondary kontras), ukuran lebih besar,
     berikon "+ Catat HM/Ritase".
   - Posisi tetap melayang di bawah konten tab utama agar mudah dijangkau jempol.
   - Sekali tap → membuka form/alur pencatatan (mulai sesi → input HM/Ritase → simpan/selesai).

### Hubungan dengan struktur §2
- Tab utama "Jadwal" pada bar tab lama berubah menjadi **Home/Jadwal Saya** dengan isi
  di atas (kalender bulanan pribadi + total + sesi aktif + FAB).
- **Roaster 1 hari untuk spare (§6)** tetap ada, namun posisinya sebagai layar/tab lain
  (mis. tab "Roaster" tersendiri atau sub-tab), sehingga tidak bercampur dengan Home.
- Kalender Home = **personal, sebulan penuh**; layar Roaster = **seluruh unit, 1 hari terpilih**.

## 8. Alur Login / Daftar Akun (Revisi Pengguna, 14 Okt 2026) ✅ PENTING
**Keputusan baru: TIDAK menggunakan email.** Login/daftar cukup dengan:

1. **Nama Driver** — harus **sama persis dengan nama yang terdaftar di Google Sheet**
   (kolom "Nama" pada roster). Template input: satu kolom Nama + satu kolom Password.
2. **Password** — dibuat oleh driver saat daftar; diverifikasi saat login.

### Aturan yang disepakati
- **Daftar (Register)**: Nama harus ditemukan di sheet roster (validasi terhadap daftar
  nama driver); jika nama tidak ada di sheet → ditolak ("Nama tidak terdaftar").
- **Daftar = simpan kredensial**: Saat driver mendaftar, **Nama + Password-nya ikut
  tersimpan** (Nama → password) di Supabase (via email virtual). Sheet hanya sebagai
  daftar nama yang *boleh* mendaftar, bukan penyimpan password.
- **Login wajib validasi akun**: Aplikasi HARUS memvalidasi apakah akun (Nama+Password)
  tersebut **sudah terdaftar atau belum**:
  - Belum pernah mendaftar → tolak: "Akun belum terdaftar, silakan daftar dulu".
  - Sudah mendaftar tapi password salah → tolak: "Password salah".
  - Sudah mendaftar & cocok → langsung masuk (tidak perlu daftar ulang).
  - Percobaan daftar dengan nama yang sudah punya akun → tampilkan
    pesan "Nama sudah terdaftar, silakan login".
- **Multi akun**: Tetap didukung (fitur switcher/choose account seperti catatan sebelumnya),
  karena 1 unit = 2 driver beda shift, kemungkinan 1 HP dipakai bergantian.
- **Penyimpanan password**: 
  - Opsi utama: tetap via **Supabase Auth**, tetapi gunakan **email virtual** hasil
    template dari nama (mis. `<nama-normalized>@timesheet.local`) agar unik & valid bagi
    Supabase — user tidak pernah melihat/menginput email.
  - Password TIDAK disimpan plaintext; hashing dilakukan server Supabase. Aplikasi hanya
    menyimpan token sesi lokal per akun.
- **Sinkron identitas**: Setelah login, nama driver dipakai sebagai kunci untuk mengambil
  barisnya di sheet (jadwal §7 Home) dan mencatat HM/Ritase.

### Dampak ke UI (menggantikan temuan lama)
- `LoginScreen.kt`: hapus field Email; ganti menjadi **Nama Driver + Password**, toggle
  mode Login/Daftar dipertahankan.
- Validasi lemah sebelumnya (format email) tidak relevan → ganti validasi: nama tidak boleh
  kosong, minimal X karakter password, cek kecocokan nama dengan daftar roster.
- Bug `loadCarouselImages()` & artefak debug `println` tetap masuk daftar bersih-bersih.

## 9. Auto Sync & Auto Check Update Saat Aplikasi Dibuka
Setiap kali aplikasi dibuka (cold start), jalankan dua proses otomatis:

### 9.1 Auto Sync
- Otomatis sinkronisasi Google Sheet roster (via `RosterSyncManager`) tanpa user menekan tombol sync manual.
- Berjalan di background (coroutine) dengan indikator ringan (progress kecil/snackbar) — tidak memblokir UI.
- Setelah selesai: Home/Jadwal Saya (§7) & Roaster (§6) langsung menampilkan data tanggal terkini.
- Jika gagal (offline): tampilkan data cache terakhir + pesan halus "Gagal sync, menampilkan data terakhir".

### 9.2 Auto Check Update
- Saat startup, aplikasi memeriksa versi terbaru (mis. lewat endpoint/GitHub Releases/Supabase config yang berisi `versionCode` terbaru + URL unduhan APK).
- Bandingkan dengan versi aplikasi terpasang (`BuildConfig.VERSION_CODE`).
- Jika ada update lebih baru → **pop-up dialog** dengan tombol **"Download Update Terbaru"** (membuka URL unduhan) + tombol "Nanti" (dismissibel). Untuk update wajib (force update): tombol hanya "Download", dialog tidak bisa ditutup.
- Jika versi sama → tidak ada pop-up, lanjut ke alur normal.
- Pemeriksaan TIDAK boleh membuat crash jika server mati/network error (try-catch, silent fail / log saja).

### 9.3 Prinsip Offline-First (Konfirmasi Pengguna, 14 Okt 2026) ✅ PENTING
**Semua fitur online TIDAK akan mengganggu pengguna saat offline — aplikasi tetap bisa dibuka**,
KECUALI bila pengguna belum/tidak sedang login:

- **Offline + sudah login (sesi tersimpan)** → aplikasi normal:
  - Home/Jadwal Saya (§7), Roaster (§6), dan pencatatan HM/Ritase tetap berjalan memakai **data cache terakhir** (roster sheet & data lokal).
  - Auto Sync (§9.1) gagal diam-diam → cukup tampilkan data cache + pesan halus, tanpa dialog blocking, tanpa crash.
  - Auto Check Update (§9.2) gagal → skip diam-diam, tidak ada pop-up error.
  - Pencatatan HM/Ritase saat offline disimpan di **antrean lokal (offline queue)** dan otomatis dikirim/disinkronkan begitu koneksi pulih.
- **Offline + BELUM login** → fitur yang butuh validasi akun tidak dapat digunakan:
  - Login/daftar dengan Nama+Password (§8) membutuhkan server (Supabase) → saat offline, tampilkan pesan jelas "Tidak ada koneksi internet, login tidak dapat divalidasi" (bukan crash).
  - Konsekuensi: karena semua layar utama berada setelah gerbang login, pengguna yang belum login praktis tidak bisa masuk ke konten apa pun saat offline. Ini sesuai aturan: *"masih bisa dibuka kecuali tidak sedang login"*.
- **Syarat teknis**:
  - Sesi/token login disimpan persistif di perangkat → tidak hilang saat app ditutup atau offline.
  - Semua pemanggilan jaringan (sheet, Supabase, cek update) wajib dibungkus try-catch + timeout, fallback ke cache/data lokal.
  - Layar startup tidak boleh menggantung menunggu network (sync & update check berjalan paralel di background).

## 10. Status Implementasi

### Checkpoint 2026-10-14 — Push + Build GitHub (TERVERIFIKASI VIA TERMINAL)
- Commit `03b983d`, `522db67`, `4b3418b` (fix MIGRATION_4_5 tambah kolom driverName, indeks unique username, fallbackToDestructiveMigrationOnDowngrade, dialog GPS kondisional & fillMaxSize) sudah ada di origin/main — diverifikasi `git ls-remote` = `4b3418b`.
- `git push` dengan token dieksekusi ulang: hasil `Everything up-to-date`, exit 0 → tidak ada perubahan tertinggal; working tree bersih (`git status --short` = 0).
- GitHub Actions **Run #28** (id 38528527459, commit `4b3418b`): completed / **success** ✅.
- Artifact `app-debug-apk` (18.476.230 bytes) dari Run #28 berhasil DIUNDUH & DIBONGKAR; pemeriksaan isi dex MEMBUKTIKAN perbaikan ikut ter-compile ke APK: string `ALTER TABLE shifts ... driverName`, `index_user_accounts_username`, dan `fallbackToDestructiveMigrationOnDowngrade` ditemukan (classes7.dex, classes10.dex).
- Runtime test di perangkat: NOT RUN (tidak ada emulator/adb di lingkungan ini). Pengguna diminta instal app-debug.apk Run #28 DI ATAS instalasi lama (tanpa uninstall) untuk memverifikasi crash startup hilang.
- ✅ Semua rencana §1–§9 sudah diimplementasikan ke kode (lihat checkpoint di atas).
- Langkah berikutnya: uji runtime APK Run #28 di perangkat; jika crash masih muncul, kirim logcat untuk investigasi lanjutan.

## 11. Checkpoint Build CI — 14 Okt 2026 (VERIFIED)
- Run #21–#23 (commit 011948c, de861da, 58d7738): **FAIL** — sintaks Kotlin `MainActivity.kt` (brace berlebih baris 538, penutup `bottomBar` salah) + `AccountScreen.kt` memakai properti tak dikenal `acc.driverName`.
- Perbaikan (commit `78604f7`): hapus brace liar di akhir `MainAppContent`, tutup lambda `bottomBar` dengan benar, ganti `driverName` → `fullName` (sesuai `SavedAccount(id, fullName)`).
- Verifikasi: scanner keseimbangan brace semua file `.kt` → 0 file tidak seimbang; **GitHub Actions Run #24 pada commit 78604f7 = SUCCESS** ✅ (bukti via API: `24 78604f7e completed success`).
- APK debug tersedia sebagai artifact workflow "Build APK" run #24.
- Status fitur §2/§5/§6/§7/§8/§9/§9.3: sudah diterapkan di kode & lolos build. Belum: migrasi penuh Jetpack Navigation Compose (masih state tab manual), Home §7 sebagian (kalender bulan berjalan sudah ada di Dashboard), validasi register lebih ketat (§Login catatan lama).
