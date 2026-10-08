package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.UnitEntity
import com.example.ui.theme.MiningAmber
import com.example.ui.theme.MiningSlateBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartShiftScreen(
    recentUnits: List<UnitEntity>,
    onStartShift: (unit: String, date: String, hmAwal: Double, kmAwal: Double) -> Unit
) {
    val context = LocalContext.current
    val todayFormatted = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    var unitText by remember { mutableStateOf("DT-012") }
    var dateText by remember { mutableStateOf(todayFormatted) }
    var hmAwalText by remember { mutableStateOf("102") }
    var kmAwalText by remember { mutableStateOf("12450") }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            val hmAwal = hmAwalText.toDoubleOrNull() ?: 0.0
            val kmAwal = kmAwalText.toDoubleOrNull() ?: 0.0
            onStartShift(unitText, dateText, hmAwal, kmAwal)
        } else {
            validationError = "GPS diperlukan untuk menghitung ritase otomatis. Tanpa izin lokasi, pencatatan ritase tidak dapat berjalan."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = MiningAmber)
                        Text("DT TIME SHEET", fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mulai Shift Baru",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MiningAmber
                    )
                    Text(
                        text = "Pastikan C/N unit, tanggal, serta HM dan KM awal sudah terisi sesuai speedometer alat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Input C/N Unit
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("C/N Unit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = unitText,
                    onValueChange = { unitText = it },
                    placeholder = { Text("Contoh: DT-012") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MiningAmber)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_unit"),
                    singleLine = true
                )

                // Recent unit suggestions
                if (recentUnits.isNotEmpty()) {
                    Text(
                        "Unit Terakhir Digunakan:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recentUnits.take(4).forEach { unit ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (unitText == unit.cn) MiningAmber else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { unitText = unit.cn }
                            ) {
                                Text(
                                    text = unit.cn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (unitText == unit.cn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Input Tanggal (Editable)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tanggal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    placeholder = { Text("DD/MM/YYYY") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MiningAmber)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tanggal"),
                    singleLine = true
                )
            }

            // Input HM Awal & KM Awal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("HM Awal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = hmAwalText,
                        onValueChange = { hmAwalText = it },
                        placeholder = { Text("Contoh: 102") },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MiningAmber)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_hm_awal"),
                        singleLine = true
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("KM Awal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = kmAwalText,
                        onValueChange = { kmAwalText = it },
                        placeholder = { Text("Contoh: 12450") },
                        leadingIcon = {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MiningAmber)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_km_awal"),
                        singleLine = true
                    )
                }
            }

            if (validationError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol MULAI SHIFT (Besar, min touch target tinggi)
            Button(
                onClick = {
                    if (unitText.trim().isEmpty()) {
                        validationError = "C/N Unit tidak boleh kosong"
                        return@Button
                    }
                    if (dateText.trim().isEmpty()) {
                        validationError = "Tanggal tidak boleh kosong"
                        return@Button
                    }
                    val hmAwal = hmAwalText.toDoubleOrNull()
                    if (hmAwal == null) {
                        validationError = "HM Awal harus berupa angka valid"
                        return@Button
                    }
                    val kmAwal = kmAwalText.toDoubleOrNull()
                    if (kmAwal == null) {
                        validationError = "KM Awal harus berupa angka valid"
                        return@Button
                    }

                    // Check and request location permission
                    val fineLocation = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    val postNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else true

                    if (!fineLocation || !postNotification) {
                        val permissions = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    } else {
                        onStartShift(unitText, dateText, hmAwal, kmAwal)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_mulai_shift")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MULAI SHIFT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
