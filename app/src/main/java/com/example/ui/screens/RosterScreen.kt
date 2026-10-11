package com.example.ui.screens

import com.example.ui.theme.ShapeInner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Kode shift yang dikenal jadwal. Huruf pertama dipakai sebagai warna penanda. */
internal fun shiftColor(code: String): Color = when (code) {
    "D" -> Color(0xFF2E7D32)      // Siang (green)
    "N" -> Color(0xFF6A1B9A)      // Malam (purple)
    "OFF" -> Color(0xFF455A64)    // Libur (gray-blue)
    "CR" -> Color(0xFFEF6C00)     // Cuti roster (orange)
    "CT" -> Color(0xFFAD1457)     // Cuti tahunan (pink)
    "CTD" -> Color(0xFFAD1457)    // Cuti tahunan siang (pink)
    "CTN" -> Color(0xFF00838F)    // Cuti tahunan malam (teal)
    "I" -> Color(0xFFC62828)      // Induksi setelah cuti (red)
    else -> Color(0xFF9E9E9E)
}

/** Label ramah pengguna untuk kode shift pada jadwal. */
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

/** Deteksi unit kosong untuk tab [shift] ("D" siang atau "N" malam).
 *  Aturan armada: 2 driver 1 unit beda shift. Jika driver D berhalangan
 *  (CR/CT/I/OFF) sementara pasangannya N masuk, maka unit tanpa driver SIANG
 *  — dan sebaliknya. Spare bisa mengambil alih, jadi peringatan diletakkan paling atas. */
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
    isSyncing: Boolean,
    lastSyncTime: Long,
    driverName: String,
    onRefreshClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var unitFilter by remember { mutableStateOf<String?>(null) }
    var shiftTab by remember { mutableStateOf("D") } // "D" = Siang, "N" = Malam

    val calendar = remember { Calendar.getInstance() }
    val daysInMonth = remember { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val todayDay = remember { calendar.get(Calendar.DAY_OF_MONTH) }
    val monthLabel = remember {
        SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(calendar.time)
            .replaceFirstChar { it.titlecase(Locale("id", "ID")) }
    }

    val filtered = remember(rosters, searchQuery, unitFilter, shiftTab) {
        rosters.filter {
            (unitFilter == null || it.vehicleId.trim().uppercase() == unitFilter) &&
                    (searchQuery.isBlank() ||
                            it.driverName.contains(searchQuery.trim(), ignoreCase = true) ||
                            it.vehicleId.contains(searchQuery.trim(), ignoreCase = true)) &&
                    // tab siang hanya menampilkan driver berjadwal D hari ini, tab malam hanya N
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

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

        // Judul + info sinkronisasi
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Roaster Kerja $monthLabel",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        val syncText = if (lastSyncTime > 0) {
            "Diperbarui: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(lastSyncTime)
        } else "Belum pernah diperbarui — jadwal tersimpan terakhir di HP ini"
        Text(syncText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.width(0.dp).height(8.dp))

        // Tab pilihan shift: Siang / Malam
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("D" to "Siang", "N" to "Malam").forEach { (code, label) ->
                Button(
                    onClick = { shiftTab = code },
                    colors = if (shiftTab == code)
                        ButtonDefaults.buttonColors(containerColor = shiftColor(code))
                    else ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Baris aksi
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRefreshClick, enabled = !isSyncing) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Memperbarui...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.width(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Perbarui Jadwal")
                }
            }
            OutlinedButton(onClick = { unitFilter = null }) {
                Text("Semua Unit")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Pencarian nama driver / unit
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Cari nama driver atau nomor unit...") }
        )

        Spacer(Modifier.height(8.dp))

        // Filter cepat berdasarkan nomor unit
        if (vehicleOptions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                vehicleOptions.forEach { v ->
                    val selected = unitFilter == v
                    OutlinedButton(
                        onClick = { unitFilter = if (selected) null else v },
                        colors = if (selected)
                            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else ButtonDefaults.outlinedButtonColors(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(v, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Peringatan unit tanpa driver pada shift terpilih — selalu PALING ATAS
        if (emptyUnits.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeInner,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFEF6C00)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Unit Tanpa Driver Shift ${if (shiftTab == "D") "Siang" else "Malam"} Hari Ini",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                    Text(
                        "${emptyUnits.size} unit sedang kosong karena pasangannya berhalangan. Driver cadangan (spare) dapat mengambil alih unit ini.",
                        fontSize = 11.sp,
                        color = Color(0xFFBF360C),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    emptyUnits.forEach { (vehicle, absentCode) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(8.dp).height(8.dp)
                                    .background(shiftColor(absentCode), RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Unit $vehicle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(6.dp))
                            Text("(pasangan: $absentCode)", fontSize = 11.sp, color = Color(0xFF6D4C41))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }


        Spacer(Modifier.height(8.dp))

        if (rosters.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Jadwal belum tersedia",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tekan tombol \"Perbarui Jadwal\" saat ada koneksi internet untuk mengambil jadwal dari Google Sheet.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        if (filtered.isEmpty()) {
            Text(
                "Tidak ada driver shift ${if (shiftTab == "D") "siang" else "malam"} yang cocok dengan pencarian/filter ini.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Text(
            "${filtered.size} driver ditampilkan",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { roster ->
                RosterRow(
                    roster = roster,
                    daysInMonth = daysInMonth,
                    todayDay = todayDay,
                    highlightSelf = driverName.isNotBlank() &&
                            roster.driverName == driverName.uppercase()
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun RosterRow(
    roster: RosterEntity,
    daysInMonth: Int,
    todayDay: Int,
    highlightSelf: Boolean
) {
    val shifts = remember(roster) { roster.getShiftsList() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeInner,
        colors = CardDefaults.cardColors(
            containerColor = if (highlightSelf)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                roster.driverName + if (highlightSelf) "  ★" else "",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                roster.vehicleId,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                for (day in 1..daysInMonth) {
                    val code = shifts.getOrElse(day - 1) { "" }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$day",
                            fontSize = 9.sp,
                            color = if (day == todayDay)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (day == todayDay) FontWeight.Bold else FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(if (code.length > 1) 26.dp else 18.dp)
                                .height(22.dp)
                                .background(
                                    if (code.isEmpty()) Color(0x14000000) else shiftColor(code),
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                code.ifEmpty { "-" },
                                fontSize = 9.sp,
                                color = if (code.isEmpty())
                                    Color(0x66000000)
                                else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(3.dp))
                }
            }
        }
    }
}
