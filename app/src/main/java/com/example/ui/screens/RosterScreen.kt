package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Button
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
private fun shiftColor(code: String): Color = when (code) {
    "D" -> Color(0xFF2E7D32)      // Hari kerja (green)
    "N" -> Color(0xFF6A1B9A)      // Malam (purple)
    "OFF" -> Color(0xFF455A64)    // Libur (gray-blue)
    "CR" -> Color(0xFFEF6C00)     // Crane / repair (orange)
    "CTD" -> Color(0xFFAD1457)    // Crash day (pink)
    "CTN" -> Color(0xFF00838F)    // Crash night (teal)
    "I" -> Color(0xFFC62828)      // Insident (red)
    else -> Color(0xFF9E9E9E)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    rosters: List<RosterEntity>,
    isSyncing: Boolean,
    lastSyncTime: Long,
    driverName: String,
    onRefreshClick: () -> Unit,
    onSaveSheetUrl: (String) -> Unit,
    defaultSheetUrl: String
) {
    var searchQuery by remember { mutableStateOf("") }
    var showUrlEditor by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf(defaultSheetUrl) }

    val calendar = remember { Calendar.getInstance() }
    val daysInMonth = remember { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val todayDay = remember { calendar.get(Calendar.DAY_OF_MONTH) }
    val monthLabel = remember {
        SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(calendar.time)
            .replaceFirstChar { it.titlecase(Locale("id", "ID")) }
    }

    val filtered = remember(rosters, searchQuery) {
        if (searchQuery.isBlank()) rosters
        else rosters.filter {
            it.driverName.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.vehicleId.contains(searchQuery.trim(), ignoreCase = true)
        }
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
                "Jadwal Kerja $monthLabel",
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
            OutlinedButton(onClick = { showUrlEditor = !showUrlEditor }) {
                Text("Alamat Sheet")
            }
        }

        if (showUrlEditor) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Alamat Google Sheet jadwal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("https://docs.google.com/spreadsheets/d/...") }
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            onSaveSheetUrl(urlInput)
                            showUrlEditor = false
                        }) { Text("Simpan") }
                        OutlinedButton(onClick = {
                            urlInput = defaultSheetUrl
                            onSaveSheetUrl(defaultSheetUrl)
                            showUrlEditor = false
                        }) { Text("Pakai Bawaan") }
                    }
                }
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
            placeholder = { Text("Cari nama driver atau unit...") }
        )

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
        shape = RoundedCornerShape(10.dp),
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
