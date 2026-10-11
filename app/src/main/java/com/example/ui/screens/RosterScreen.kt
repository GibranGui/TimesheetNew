package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RosterEntity
import com.example.ui.theme.CodeDay
import com.example.ui.theme.CodeDayBg
import com.example.ui.theme.CodeLeave
import com.example.ui.theme.CodeLeaveBg
import com.example.ui.theme.CodeNight
import com.example.ui.theme.CodeNightBg
import com.example.ui.theme.CodeOff
import com.example.ui.theme.CodeOffBg
import com.example.ui.theme.CodeOther
import com.example.ui.theme.CodeOtherBg
import com.example.ui.theme.EmployeePrimarySoft
import com.example.ui.theme.EmployeeAccent
import com.example.ui.theme.EmployeeOkGreen
import com.example.ui.theme.EmployeePrimary
import com.example.ui.theme.ShapeCard
import com.example.ui.theme.ShapeInner
import com.example.ui.theme.contrastTextOn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Warna penanda kode jadwal — gaya aplikasi roaster armada. */
internal fun shiftColor(code: String): Color = when (code) {
    "D" -> CodeDay
    "N" -> CodeNight
    "OFF" -> CodeOff
    "CR", "CT", "CTD", "I" -> CodeLeave
    "CTN" -> CodeNight
    "" -> CodeOther
    else -> CodeOther
}

/** Latar lembut untuk chip kode. */
internal fun shiftSoftBg(code: String): Color = when (code) {
    "D" -> CodeDayBg
    "N" -> CodeNightBg
    "OFF" -> CodeOffBg
    "CR", "CT", "CTD", "I" -> CodeLeaveBg
    "CTN" -> CodeNightBg
    else -> CodeOtherBg
}

/** Label ramah pengguna untuk kode jadwal. */
internal fun shiftLabel(code: String): String = when (code) {
    "D" -> "Siang"
    "N" -> "Malam"
    "OFF" -> "Libur"
    "CR" -> "Cuti roster"
    "CT" -> "Cuti tahunan"
    "CTD" -> "Cuti tahunan (siang)"
    "CTN" -> "Cuti tahunan (malam)"
    "I" -> "Induksi"
    "" -> "Belum ada jadwal"
    else -> code
}

/** Status yang membuat unit tidak punya driver pada satu shift. */
private val ABSENT_CODES = setOf("CR", "CT", "CTD", "CTN", "I", "OFF")

/** Kelompokkan baris sheet menjadi pasangan dua driver per unit.
 *  Driver dengan unit "SPARE" tidak dipasangkan dengan siapa pun. */
private class UnitPair(val vehicleId: String, val dayShifts: List<List<String>>)

private fun buildUnitPairs(rosters: List<RosterEntity>, daysInMonth: Int): List<UnitPair> {
    val byVehicle = rosters.filter { !it.vehicleId.contains("SPARE", ignoreCase = true) }
        .groupBy { it.vehicleId.trim().uppercase() }
    return byVehicle.map { (vehicle, members) ->
        val lists = members.map { it.getShiftsList() }
        val size = if (lists.isEmpty()) 0 else lists.maxOf { it.size }.coerceAtMost(daysInMonth)
        val combined = (0 until size).map { i -> lists.mapNotNull { l -> l.getOrNull(i)?.trim()?.uppercase() } }
        UnitPair(vehicle, combined)
    }.sortedBy { it.vehicleId }
}

/** Deteksi unit kosong untuk tab [shift] ("D" siang atau "N" malam). */
private fun emptyUnitsForShift(pairs: List<UnitPair>, shift: String, todayDay: Int): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    for (pair in pairs) {
        val shiftsToday = pair.dayShifts.getOrElse(todayDay - 1) { continue }
        if (shiftsToday.isEmpty()) continue
        val codes = shiftsToday.take(2)
        val dCode = if (codes.getOrNull(0) == "D") codes[0] else if (codes.getOrNull(1) == "D") codes[1] else null
        val nCode = if (codes.getOrNull(0) == "N") codes[0] else if (codes.getOrNull(1) == "N") codes[1] else null
        val partnerAbsent: String? = when (shift) {
            "D" -> if (dCode == null && codes.any { it in ABSENT_CODES }) codes.first { it in ABSENT_CODES } else null
            "N" -> if (nCode == null && codes.any { it in ABSENT_CODES }) codes.first { it in ABSENT_CODES } else null
            else -> null
        }
        if (partnerAbsent != null) result += pair.vehicleId to partnerAbsent
    }
    return result.sortedBy { it.first }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    rosters: List<RosterEntity>,
    mySchedule: List<DaySchedule>,
    driverName: String,
    vehicleId: String,
    isSyncing: Boolean,
    lastSyncTime: Long,
    onRefreshClick: () -> Unit,
    onStartShiftClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var unitFilter by remember { mutableStateOf<String?>(null) }
    var shiftTab by remember { mutableStateOf("D") } // "D" = Siang, "N" = Malam

    val calendar = remember { Calendar.getInstance() }
    val daysInMonth = remember { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val todayDay = remember { calendar.get(Calendar.DAY_OF_MONTH) }
    val idLocale = remember { Locale("id", "ID") }
    val monthLabel = remember {
        SimpleDateFormat("MMMM yyyy", idLocale).format(calendar.time)
            .replaceFirstChar { it.titlecase(idLocale) }
    }
    val todayLabel = remember {
        SimpleDateFormat("EEEE, d MMMM", idLocale).format(calendar.time)
            .replaceFirstChar { it.titlecase(idLocale) }
    }

    val filtered = remember(rosters, searchQuery, unitFilter, shiftTab, todayDay) {
        rosters.filter {
            (unitFilter == null || it.vehicleId.trim().uppercase().contains(unitFilter!!)) &&
                    (searchQuery.isBlank() ||
                            it.driverName.contains(searchQuery.trim(), ignoreCase = true) ||
                            it.vehicleId.contains(searchQuery.trim(), ignoreCase = true)) &&
                    it.getShiftsList().getOrNull(todayDay - 1)?.trim()?.uppercase() == shiftTab
        }.sortedBy { it.driverName }
    }

    val pairs = remember(rosters, daysInMonth) { buildUnitPairs(rosters, daysInMonth) }
    val emptyUnits = remember(pairs, shiftTab, todayDay) {
        emptyUnitsForShift(pairs, shiftTab, todayDay)
    }
    val vehicleOptions = remember(rosters) {
        rosters.map { it.vehicleId.trim().uppercase() }.filter { !it.contains("SPARE", true) }
            .distinct().sorted()
    }
    val todayCode = mySchedule.getOrNull(todayDay - 1)?.code.orEmpty()
    val workDaysCount = remember(mySchedule) { mySchedule.count { it.code == "D" || it.code == "N" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(10.dp))

        // ================= HEADER =================
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(EmployeeAccent.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CalendarMonth, null, tint = EmployeeAccent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Roaster Kerja", fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(monthLabel + " • " + todayLabel, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onRefreshClick,
                enabled = !isSyncing,
                shape = ShapeInner,
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(if (isSyncing) "Memuat…" else "Perbarui", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ================= KARTU JADWAL SAYA (KALENDER) =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCard,
            colors = CardDefaults.cardColors(containerColor = EmployeePrimary),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(driverName.ifBlank { "Driver" }, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (vehicleId.isNotBlank()) "Unit $vehicleId" else "Unit belum dipilih",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(shiftLabel(todayCode).uppercase(), fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Hari ini", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                MonthlyCalendarGrid(
                    schedule = mySchedule,
                    daysInMonth = daysInMonth,
                    todayDay = todayDay,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Total hari kerja bulan ini: $workDaysCount hari",
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f)
                    )
                    Button(
                        onClick = onStartShiftClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White, contentColor = EmployeePrimary
                        ),
                        shape = ShapeInner,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp, vertical = 6.dp
                        )
                    ) {
                        Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Catat HM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ================= TAB SIANG / MALAM =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, ShapeInner),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("D" to "Siang", "N" to "Malam").forEach { (code, label) ->
                val selected = shiftTab == code
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .then(
                            if (selected)
                                Modifier.background(shiftColor(code), ShapeInner)
                            else Modifier
                        )
                        .clickable { shiftTab = code }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) contrastTextOn(shiftColor(code))
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ================= PENCARIAN =================
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = ShapeInner,
            placeholder = { Text("Cari nama driver atau nomor unit…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Close, "Hapus pencarian")
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        // Filter cepat nomor unit
        if (vehicleOptions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                vehicleOptions.forEach { v ->
                    val selected = unitFilter == v
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) EmployeePrimary else MaterialTheme.colorScheme.surface,
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (selected) EmployeePrimary else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                            .clickable { unitFilter = if (selected) null else v }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            v, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ================= UNIT TANPA DRIVER (PALING ATAS) =================
        if (emptyUnits.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                colors = CardDefaults.cardColors(containerColor = CodeDayBg),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, null, tint = EmployeeAccent)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Unit Tanpa Driver Shift ${if (shiftTab == "D") "Siang" else "Malam"} Hari Ini",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CodeDay,
                        )
                    }
                    Text(
                        "${emptyUnits.size} unit kosong karena pasangannya berhalangan. " +
                                "Driver cadangan (spare) dapat mengambil alih unit ini.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    emptyUnits.forEach { (vehicle, absentCode) ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(Modifier.size(8.dp)
                                .background(shiftColor(absentCode), RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(6.dp))
                            Text("Unit $vehicle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(6.dp))
                            Text("(pasangan: ${shiftLabel(absentCode)})", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // ================= DAFTAR DRIVER ARMADA =================
        if (rosters.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Jadwal belum tersedia", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hubungi mandor/foreman Anda agar akun didaftarkan dan jadwal dimuat. " +
                                "Jadwal juga tersimpan otomatis saat aplikasi dibuka jika ada internet.",
                        fontSize = 12.sp, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Daftar Driver — Shift ${if (shiftTab == "D") "Siang" else "Malam"} ($monthLabel)",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text("${filtered.size} orang", fontSize = 12.sp,
                    color = EmployeeOkGreen, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Text(
                    "Tidak ada driver shift ${if (shiftTab == "D") "siang" else "malam"} yang cocok dengan pencarian/filter ini.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filtered.forEach { roster ->
                    RosterRow(
                        roster = roster,
                        daysInMonth = daysInMonth,
                        todayDay = todayDay,
                        highlightSelf = driverName.isNotBlank() &&
                                roster.driverName == driverName.uppercase()
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        val syncText = if (lastSyncTime > 0) {
            "Jadwal diperbarui: " + SimpleDateFormat("dd/MM/yyyy HH:mm", idLocale).format(lastSyncTime)
        } else "Jadwal tersimpan terakhir di HP ini"
        Text(syncText, fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
    }
}

/** Grid kalender sebulan untuk driver yang login. */
@Composable
private fun MonthlyCalendarGrid(
    schedule: List<DaySchedule>,
    daysInMonth: Int,
    todayDay: Int,
) {
    val firstDow = remember {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        ((c.get(Calendar.DAY_OF_WEEK) + 5) % 7) // 0 = Senin
    }
    val dayNames = remember { listOf("S", "S", "R", "K", "J", "S", "M") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            dayNames.forEach { d ->
                Text(d, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        val cells = firstDow + daysInMonth
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val idx = r * 7 + col
                    val day = idx - firstDow + 1
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day in 1..daysInMonth) {
                            val code = schedule.getOrNull(day - 1)?.code.orEmpty()
                            val isToday = day == todayDay
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$day", fontSize = 9.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) Color.White else Color.White.copy(alpha = 0.85f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            if (code.isEmpty()) Color.White.copy(alpha = 0.12f)
                                            else shiftColor(code).copy(alpha = 0.92f),
                                            CircleShape
                                        )
                                        .then(if (isToday) Modifier.border(2.dp, Color.White, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        code.ifEmpty { "-" },
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (code.isEmpty()) Color.White.copy(alpha = 0.5f)
                                        else contrastTextOn(shiftColor(code)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RosterRow(
    roster: RosterEntity,
    daysInMonth: Int,
    todayDay: Int,
    highlightSelf: Boolean,
) {
    val shifts = remember(roster) { roster.getShiftsList() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeCard,
        colors = CardDefaults.cardColors(
            containerColor = if (highlightSelf) EmployeePrimarySoft else MaterialTheme.colorScheme.surface
        ),
        border = if (highlightSelf) androidx.compose.foundation.BorderStroke(1.dp, EmployeePrimary) else null,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        roster.driverName + if (highlightSelf) "  ★ Anda" else "",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(roster.vehicleId, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val todayCode = shifts.getOrNull(todayDay - 1)?.trim()?.uppercase().orEmpty()
                if (todayCode.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(shiftColor(todayCode), ShapeInner)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(shiftLabel(todayCode), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = contrastTextOn(shiftColor(todayCode)))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                for (day in 1..daysInMonth) {
                    val code = shifts.getOrElse(day - 1) { "" }.trim().uppercase()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$day", fontSize = 8.sp,
                            color = if (day == todayDay) EmployeeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (day == todayDay) FontWeight.Bold else FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(if (code.length > 1) 24.dp else 16.dp)
                                .height(20.dp)
                                .background(
                                    if (code.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
                                    else shiftColor(code),
                                    RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                code.ifEmpty { "-" },
                                fontSize = 8.sp,
                                color = if (code.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else contrastTextOn(shiftColor(code)),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.width(3.dp))
                }
            }
        }
    }
}

