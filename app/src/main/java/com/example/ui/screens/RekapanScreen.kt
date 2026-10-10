package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShiftEntity
import com.example.ui.theme.MiningAccentBlue
import com.example.ui.theme.MiningAmber
import com.example.ui.theme.MiningGreenActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekapanScreen(
    shifts: List<ShiftEntity>,
    selectedMonth: String,
    onEditShift: (shiftId: String, unitId: String, date: String, hmAwal: Double, hmAkhir: Double, kmAwal: Double, kmAkhir: Double) -> Unit
) {
    var selectedShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var editingShift by remember { mutableStateOf<ShiftEntity?>(null) }

    // Hitung total akumulasi
    val totalShiftCount = shifts.size
    val totalRitase = shifts.sumOf { it.totalRitase }
    val totalHm = shifts.sumOf { it.totalHm }
    val totalKm = shifts.sumOf { it.totalKm }
    val totalFuel = shifts.sumOf { it.totalFuel }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = MiningAmber)
                        Text("Rekapan Time Sheet", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Data tersimpan di HP",
                        tint = MiningGreenActive,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Periode Filter Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MiningAmber)
                            Text("Periode Aktif:", fontWeight = FontWeight.SemiBold)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MiningAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Oktober 2026 ($selectedMonth)",
                                fontWeight = FontWeight.Bold,
                                color = MiningAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // RINGKASAN AKUMULASI KPI (STATISTIK BULAN INI)
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "TOTAL AKUMULASI BULAN INI",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MiningAmber
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiStatBox(
                                label = "Total Shift",
                                value = "$totalShiftCount",
                                unit = "shift",
                                modifier = Modifier.weight(1f)
                            )
                            KpiStatBox(
                                label = "Total Ritase",
                                value = "$totalRitase",
                                unit = "rit",
                                modifier = Modifier.weight(1f),
                                highlightColor = MiningAmber
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiStatBox(
                                label = "Total HM",
                                value = String.format("%.1f", totalHm),
                                unit = "HM",
                                modifier = Modifier.weight(1f)
                            )
                            KpiStatBox(
                                label = "Total KM",
                                value = "${totalKm.toInt()}",
                                unit = "KM",
                                modifier = Modifier.weight(1f)
                            )
                            KpiStatBox(
                                label = "Total Fuel",
                                value = "${totalFuel.toInt()}",
                                unit = "Liter",
                                modifier = Modifier.weight(1f),
                                highlightColor = MiningAccentBlue
                            )
                        }
                    }
                }
            }

            // JUDUL TABEL TIME SHEET
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAFTAR TIME SHEET SHIFT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Format Tab: TIME SHEET",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (shifts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum Ada Shift Selesai",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Data shift yang telah selesai akan otomatis tampil di sini.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(shifts, key = { it.id }) { shift ->
                    ShiftTableRowCard(
                        shift = shift,
                        onClick = { selectedShift = shift }
                    )
                }
            }
        }

        selectedShift?.let { shift ->
            ShiftDetailPopup(
                shift = shift,
                onDismiss = { selectedShift = null },
                onEditClick = {
                    editingShift = shift
                    selectedShift = null
                }
            )
        }

        editingShift?.let { shift ->
            EditCompletedShiftDialog(
                shift = shift,
                onDismiss = { editingShift = null },
                onConfirm = { unit, date, hmAwal, hmAkhir, kmAwal, kmAkhir ->
                    onEditShift(shift.id, unit, date, hmAwal, hmAkhir, kmAwal, kmAkhir)
                    editingShift = null
                }
            )
        }
    }
}

@Composable
fun KpiStatBox(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    highlightColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = highlightColor)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ShiftTableRowCard(
    shift: ShiftEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("row_timesheet_${shift.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Baris: Tanggal | C/N Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${shift.date} • ${shift.unitId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MiningAmber
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MiningGreenActive.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "✓ TERSIMPAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MiningGreenActive,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider()

            // Baris Detail Kolom
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("HM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${shift.hmAwal} → ${shift.hmAkhir ?: '-'}", style = MaterialTheme.typography.bodySmall)
                    Text("Total: ${String.format("%.1f", shift.totalHm)}", fontWeight = FontWeight.Bold, color = MiningAmber)
                }
                Column {
                    Text("KM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${shift.kmAwal.toInt()} → ${shift.kmAkhir?.toInt() ?: '-'}", style = MaterialTheme.typography.bodySmall)
                    Text("Total: ${shift.totalKm.toInt()}", fontWeight = FontWeight.Bold, color = MiningAmber)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("RITASE & FUEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${shift.totalRitase} Rit", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MiningAmber)
                    Text("${shift.totalFuel.toInt()} Liter", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ShiftDetailPopup(
    shift: ShiftEntity,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rincian Shift: ${shift.unitId} (${shift.date})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Struktur Kolom Tab TIME SHEET:")
                HorizontalDivider()
                Text("• Tanggal: ${shift.date}")
                Text("• C/N Unit: ${shift.unitId}")
                Text("• HM Awal: ${shift.hmAwal} | HM Akhir: ${shift.hmAkhir ?: '-'}")
                Text("• Total HM: ${String.format("%.1f", shift.totalHm)} (HM Akhir - HM Awal)")
                Text("• KM Awal: ${shift.kmAwal.toInt()} | KM Akhir: ${shift.kmAkhir?.toInt() ?: '-'}")
                Text("• Total KM: ${shift.totalKm.toInt()} (KM Akhir - KM Awal)")
                Text("• Total Ritase: ${shift.totalRitase}")
                Text("• Total Fuel: ${shift.totalFuel.toInt()} L")
                HorizontalDivider()
                Text(
                    "Catatan: Kolom rasio efisiensi (L/HM atau L/KM) sengaja tidak dihitung sesuai ketentuan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onEditClick,
                colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                modifier = Modifier.testTag("btn_popup_edit_shift")
            ) {
                Text("KOREKSI DATA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("TUTUP")
            }
        }
    )
}
