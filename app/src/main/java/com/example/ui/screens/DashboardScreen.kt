package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import com.example.data.local.ShiftEntity
import com.example.domain.GpsState
import com.example.service.GpsTrackingState
import com.example.ui.theme.MiningAccentBlue
import com.example.ui.theme.MiningAmber
import com.example.ui.theme.MiningGreenActive
import com.example.ui.theme.MiningRedAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    shift: ShiftEntity,
    ritaseList: List<RitaseEntity>,
    fuelList: List<FuelEntryEntity>,
    gpsState: GpsTrackingState,
    pendingSyncCount: Int,
    onAddFuelClick: () -> Unit,
    onEndShiftClick: () -> Unit,
    onSyncClick: () -> Unit,
    onOpenHistoryClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onSimulateRitaseClick: () -> Unit,
    onCalibrateLoadingAreaClick: () -> Unit,
    onEditShiftClick: () -> Unit,
    onDeleteFuelClick: (fuelId: String) -> Unit,
    onDeleteLastRitaseClick: () -> Unit
) {
    // Kelompokkan ritase per jam (format: e.g. "07:25, 35, 45 | 3")
    val hourlyBreakdown = remember(ritaseList) {
        groupRitaseByHour(ritaseList)
    }

    val totalFuel = remember(fuelList) {
        fuelList.sumOf { it.liter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = MiningAmber)
                        Column {
                            Text(shift.unitId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text(shift.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onEditShiftClick, modifier = Modifier.testTag("btn_edit_active_shift")) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Data Shift", tint = MiningAmber)
                    }
                    IconButton(onClick = onOpenHistoryClick, modifier = Modifier.testTag("btn_menu_history")) {
                        Icon(Icons.Default.History, contentDescription = "Riwayat")
                    }
                    IconButton(onClick = onOpenSettingsClick, modifier = Modifier.testTag("btn_menu_settings")) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
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
            // STATUS BAR (GPS Status + Sync Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // GPS Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (gpsState.isGpsDisturbed) MiningRedAlert else MiningGreenActive,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (gpsState.isGpsDisturbed) "⚠️ GPS TERGANGGU" else "🟢 GPS AKTIF",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (gpsState.state) {
                                GpsState.LOADING -> "STATUS: LOADING"
                                GpsState.TRAVELLING -> "STATUS: TRAVELLING"
                                GpsState.UNKNOWN -> "STATUS: MENUNGGU GPS"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MiningAmber
                        )
                        if (gpsState.isRunning) {
                            Text(
                                text = "Jarak: ${gpsState.distanceMeters.toInt()}m | Akurasi: ${gpsState.accuracy.toInt()}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Sync Card
                Surface(
                    onClick = onSyncClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_sync_status")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (pendingSyncCount == 0) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (pendingSyncCount == 0) MiningGreenActive else MiningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "SYNC STATUS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (pendingSyncCount == 0) "☁️ Tersinkron" else "📴 $pendingSyncCount Menunggu",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pendingSyncCount == 0) MiningGreenActive else MiningAmber
                        )
                        Text(
                            text = "Ketuk untuk sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 📍 1x KLIK: SET TITIK LOADING DARI LOKASI GPS SAAT INI (TANPA INPUT MANUAL!)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = MiningAmber)
                        Text(
                            text = "Titik Loading Pit (Radius 50m)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = "Tekan tombol di bawah 1x saat DT tiba di area muat / excavator untuk mengunci titik GPS loading otomatis tanpa mengetik koordinat manual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onCalibrateLoadingAreaClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_calibrate_loading_gps")
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📍 SET TITIK LOADING DI SINI (1x KLIK)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // BIG METRIC CARD: RITASE HARI INI
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
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
                        color = MiningAmber
                    )
                    Text(
                        text = "Otomatis dihitung via GPS saat keluar loading area",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // BREAKDOWN RITASE PER JAM
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "RITASE PER JAM",
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
                                    color = MiningAmber.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${item.count} rit",
                                        fontWeight = FontWeight.Bold,
                                        color = MiningAmber,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Tombol Koreksi jika ada ritase keliru / false positive
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = onDeleteLastRitaseClick,
                                modifier = Modifier.testTag("btn_koreksi_ritase")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Koreksi / Hapus 1 Ritase Terakhir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // FUEL SECTION & SUMMARY
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FUEL ENTRY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TOTAL: ${totalFuel.toInt()} L",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MiningAmber
                        )
                    }
                    HorizontalDivider()

                    if (fuelList.isEmpty()) {
                        Text(
                            text = "Belum ada pengisian fuel pada shift ini.",
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
                                        color = MiningAmber
                                    )
                                    IconButton(
                                        onClick = { onDeleteFuelClick(fuel.id) },
                                        modifier = Modifier.size(24.dp).testTag("btn_delete_fuel_${fuel.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus Data Fuel",
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

            // INFO AWAL HM & KM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("HM AWAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${shift.hmAwal}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("KM AWAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${shift.kmAwal.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // TOMBOL AKSI UTAMA (BESAR & JELAS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TAMBAH FUEL BUTTON
                Button(
                    onClick = onAddFuelClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("btn_tambah_fuel")
                ) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = MiningAmber)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TAMBAH FUEL",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // SELESAI SHIFT BUTTON
                Button(
                    onClick = onEndShiftClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("btn_selesai_shift")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SELESAI SHIFT",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // SIMULASI RITASE (Untuk pengujian indoor / lab test tanpa harus membawa DT di lapangan)
            FilledTonalButton(
                onClick = onSimulateRitaseClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_simulasi_ritase")
            ) {
                Icon(Icons.Default.GpsFixed, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulasi GPS Exit (+1 Ritase Uji Coba)")
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

    // Kelompokkan berdasarkan 2 digit pertama jam (misal "07")
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
