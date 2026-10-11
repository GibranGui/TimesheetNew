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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FuelEntryEntity
import com.example.data.local.RitaseEntity
import com.example.data.local.RosterEntity
import com.example.data.local.ShiftEntity
import com.example.domain.GpsState
import com.example.service.GpsTrackingState
import com.example.ui.theme.EmployeeAccent
import com.example.ui.theme.EmployeePrimary
import com.example.ui.theme.contrastTextOn
import com.example.ui.theme.MiningGreenActive
import com.example.ui.theme.MiningRedAlert
import com.example.ui.theme.ShapeCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Ringkasan jadwal satu hari untuk kartu kalender di dashboard. */
data class DaySchedule(val day: Int, val code: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    shift: ShiftEntity,
    ritaseList: List<RitaseEntity>,
    fuelList: List<FuelEntryEntity>,
    gpsState: GpsTrackingState,
    monthShifts: List<ShiftEntity> = emptyList(),
    todayRosterCode: String = "",
    driverName: String = "",
    myMonthSchedule: List<DaySchedule> = emptyList(),
    onAddFuelClick: () -> Unit,
    onEndShiftClick: () -> Unit,
    onOpenHistoryClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onCalibrateLoadingAreaClick: () -> Unit,
    onEditShiftClick: () -> Unit,
    onDeleteFuelClick: (fuelId: String) -> Unit,
    onDeleteLastRitaseClick: () -> Unit
) {
    // Kelompokkan ritase per jam untuk tampilan rincian
    val hourlyBreakdown = remember(ritaseList) {
        groupRitaseByHour(ritaseList)
    }

    val totalFuel = remember(fuelList) {
        fuelList.sumOf { it.liter }
    }

    // Nama bulan berjalan (kalender saat ini) — dipakai untuk judul ringkasan & jadwal
    val currentMonthLabel = remember {
        SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Calendar.getInstance().time)
    }
    val todayDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(EmployeePrimary.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = EmployeePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                if (driverName.isNotBlank()) driverName else shift.unitId,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${shift.unitId} • ${shift.date}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onEditShiftClick, modifier = Modifier.testTag("btn_edit_active_shift")) {
                        Icon(Icons.Default.Edit, contentDescription = "Ubah Data Shift", tint = EmployeePrimary)
                    }
                    IconButton(onClick = onOpenHistoryClick, modifier = Modifier.testTag("btn_menu_history")) {
                        Icon(Icons.Default.History, contentDescription = "Riwayat")
                    }
                    IconButton(onClick = onOpenSettingsClick, modifier = Modifier.testTag("btn_menu_settings")) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // RINGKASAN KERJA SAYA: total jam kerja, total ritase bulan berjalan, dan jadwal hari ini
            val monthTotalHm = remember(monthShifts) { monthShifts.sumOf { it.totalHm } }
            val monthTotalRitase = remember(monthShifts) { monthShifts.sumOf { it.totalRitase } }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmployeePrimary, ShapeCard)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        if (driverName.isNotBlank()) "HALO, $driverName 👋" else "RINGKASAN KERJA SAYA",
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        currentMonthLabel.uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                            Text("${"%.1f".format(monthTotalHm)}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Jam Kerja (HM)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                        Box(Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.25f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("$monthTotalRitase", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                            Text("Total Ritase", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                        Box(Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.25f)))
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (todayRosterCode.isBlank()) "-" else todayRosterCode,
                                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            )
                            Text("Jadwal Hari Ini", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                    }
                }
            }

            // JADWAL KERJA BULAN INI (mengikuti kalender bulan berjalan)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmployeePrimary)
                        Text(
                            text = "Jadwal Kerja $currentMonthLabel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (myMonthSchedule.isEmpty()) {
                        Text(
                            text = "Jadwal bulan ini belum tersedia. Buka tab Roaster lalu tekan Perbarui Jadwal saat ada internet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            myMonthSchedule.forEach { day ->
                                val isToday = day.day == todayDay
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 34.dp, height = 44.dp)
                                            .background(
                                                if (isToday) EmployeePrimary.copy(alpha = 0.16f) else Color.Transparent,
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                "${day.day}",
                                                fontSize = 10.sp,
                                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isToday) EmployeePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                day.code.ifBlank { "-" },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = shiftColor(day.code)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = "Siang = shift siang • Malam = shift malam • Libur = istirahat • Cuti roster/tahunan/induksi",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // KARTU STATUS GPS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (gpsState.isGpsDisturbed || !gpsState.isRunning) MiningRedAlert else MiningGreenActive,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = when {
                                !gpsState.isRunning -> "PELACAKAN BELUM AKTIF"
                                gpsState.isGpsDisturbed -> "SINYAL GPS LEMAH"
                                else -> "PELACAKAN GPS AKTIF"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (gpsState.isGpsDisturbed || !gpsState.isRunning) MiningRedAlert else MiningGreenActive
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.GpsNotFixed,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (gpsState.state) {
                            GpsState.LOADING -> "Sedang di area muat"
                            GpsState.TRAVELLING -> "Sedang dalam perjalanan"
                            GpsState.UNKNOWN -> "Menunggu sinyal GPS"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = EmployeePrimary
                    )
                    Text(
                        text = gpsState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (gpsState.isRunning) {
                        Text(
                            text = "Jarak ke titik muat: ${gpsState.distanceMeters.toInt()} m | Akurasi: ${gpsState.accuracy.toInt()} m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // TITIK LOADING
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeInner,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = EmployeePrimary)
                        Text(
                            text = "Titik Area Muat",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = "Tekan tombol di bawah satu kali saat alat tiba di area muat. Posisi GPS saat ini akan disimpan sebagai titik acuan hitung ritase otomatis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onCalibrateLoadingAreaClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmployeePrimary),
                        shape = ShapeInner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_calibrate_loading_gps")
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SIMPAN POSISI INI SEBAGAI TITIK MUAT",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // KARTU RITASE HARI INI
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RITASE HARI INI",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${ritaseList.size}",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmployeePrimary
                    )
                    Text(
                        text = "Dihitung otomatis oleh GPS saat alat keluar dari area muat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // RINCIAN RITASE PER JAM
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Rincian Ritase per Jam",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()

                    if (hourlyBreakdown.isEmpty()) {
                        Text(
                            text = "Belum ada ritase tercatat pada shift ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        hourlyBreakdown.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.timeListString,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmployeePrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${item.count} rit",
                                        fontWeight = FontWeight.Bold,
                                        color = EmployeePrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Tombol koreksi jika ada ritase yang keliru tercatat
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = onDeleteLastRitaseClick,
                                modifier = Modifier.testTag("btn_koreksi_ritase")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hapus 1 Ritase Terakhir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // BAHAN BAKAR & RINGKASAN
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pengisian Bahan Bakar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total: ${totalFuel.toInt()} L",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmployeePrimary
                        )
                    }
                    HorizontalDivider()

                    if (fuelList.isEmpty()) {
                        Text(
                            text = "Belum ada pengisian bahan bakar pada shift ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        fuelList.forEach { fuel ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "HM ${fuel.hmIsiFuel} (${fuel.timeFormatted})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${fuel.liter.toInt()} L",
                                        fontWeight = FontWeight.Bold,
                                        color = EmployeePrimary
                                    )
                                    IconButton(
                                        onClick = { onDeleteFuelClick(fuel.id) },
                                        modifier = Modifier.size(24.dp).testTag("btn_delete_fuel_${fuel.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus Data Bahan Bakar",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // INFO HM & KM AWAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("HM Awal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${shift.hmAwal}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("KM Awal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${shift.kmAwal.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // TOMBOL AKSI UTAMA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddFuelClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = ShapeInner,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("btn_tambah_fuel")
                ) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = EmployeePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ISI BBM",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onEndShiftClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmployeePrimary),
                    shape = ShapeInner,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("btn_selesai_shift")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AKHIRI SHIFT",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

data class HourlyRitaseItem(
    val hourKey: String,
    val timeListString: String,
    val count: Int
)

private fun groupRitaseByHour(ritaseList: List<RitaseEntity>): List<HourlyRitaseItem> {
    if (ritaseList.isEmpty()) return emptyList()

    // Kelompokkan berdasarkan jam pada waktu pencatatan (2 digit pertama)
    val grouped = ritaseList.groupBy { ritase ->
        if (ritase.timeFormatted.contains(":")) {
            ritase.timeFormatted.split(":")[0]
        } else "00"
    }

    return grouped.map { (hour, list) ->
        val formattedMinuteList = list.joinToString(", ") { ritase ->
            ritase.timeFormatted
        }
        HourlyRitaseItem(
            hourKey = hour,
            timeListString = formattedMinuteList,
            count = list.size
        )
    }.sortedBy { it.hourKey }
}
