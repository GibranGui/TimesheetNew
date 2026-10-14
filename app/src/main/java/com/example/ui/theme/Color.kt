package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Gaya "Karyawan" — tema TERANG profesional (Material 3)
// Diadaptasi dari aplikasi Roaster armada: aksen unta (safety
// orange) untuk shift siang, biru tua untuk shift malam, hijau
// untuk libur. Semua warna teks dijamin kontras terhadap latar.
// ============================================================

val EmployeeBg = Color(0xFFF6F7FB)        // latar halaman
val EmployeeSurface = Color(0xFFFFFFFF)   // kartu putih bersih
val EmployeeSurfaceSoft = Color(0xFFEEF1F7) // elemen halus / isian
val EmployeeOutline = Color(0xFFD5DBE7)   // garis tepi
val EmployeeTextMain = Color(0xFF1B2430)  // teks utama (gelap)
val EmployeeTextSub = Color(0xFF5C6B80)   // teks pendukung

val EmployeePrimary = Color(0xFF1F4E79)   // biru baja — aksi utama
val EmployeeOnPrimary = Color(0xFFFFFFFF)
val EmployeePrimarySoft = Color(0xFFDCE9F6)
val EmployeeAccent = Color(0xFFE8890C)    // oranye keselamatan — sorotan
val EmployeeInfoBlue = Color(0xFF2563EB)
val EmployeeWarnAmber = Color(0xFFB45309)
val EmployeeOkGreen = Color(0xFF15803D)
val EmployeeDangerRed = Color(0xFFDC2626)
val EmployeeViolet = Color(0xFF6D28D9)

// ---------- Warna kode jadwal (dipakai seragam di semua layar) ----------
val CodeDay = Color(0xFFE8890C)      // D  : Siang (oranye keselamatan)
val CodeNight = Color(0xFF1D4ED8)    // N  : Malam (biru tengah malam)
val CodeOff = Color(0xFF2E7D32)      // OFF: Libur (hijau istirahat)
val CodeLeave = Color(0xFFC62828)    // CR/CT/I: merah cuti/induksi
val CodeOther = Color(0xFF455A64)    // lainnya: abu batu

val CodeDayBg = Color(0xFFFFF3E0)
val CodeNightBg = Color(0xFFE3F2FD)
val CodeOffBg = Color(0xFFE8F5E9)
val CodeLeaveBg = Color(0xFFFFEBEE)
val CodeOtherBg = Color(0xFFECEFF1)

// ---------- Warna status libur per kode (legenda §5; CTD/CTN bukan shift penuh) ----------
val CodeCtd = Color(0xFFEF6C00)         // CTD: cuti tahunan siang (oranye redup, bukan D murni)
val CodeCtn = Color(0xFF5C6BC0)         // CTN: cuti tahunan malam (indigo, bukan N murni)
val CodeInduction = Color(0xFF00897B)   // I  : induksi (teal — bukan cuti)
val CodeCtdBg = Color(0xFFFFF8E1)
val CodeCtnBg = Color(0xFFE8EAF6)
val CodeInductionBg = Color(0xFFE0F2F1)

/** Teks yang selalu kontras saat diletakkan di atas [bg]. */
fun contrastTextOn(bg: Color): Color =
    if (0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue > 0.6)
        Color(0xFF1B2430)
    else Color.White
