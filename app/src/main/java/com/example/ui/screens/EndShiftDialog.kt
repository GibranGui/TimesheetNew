package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ShiftEntity
import com.example.ui.theme.MiningAmber
import com.example.ui.theme.MiningGreenActive

@Composable
fun EndShiftDialog(
    shift: ShiftEntity,
    totalRitase: Int,
    totalFuel: Double,
    onDismiss: () -> Unit,
    onConfirm: (hmAkhir: Double, kmAkhir: Double) -> Unit
) {
    var hmAkhirText by remember { mutableStateOf("") }
    var kmAkhirText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hmAkhirVal = hmAkhirText.toDoubleOrNull()
    val kmAkhirVal = kmAkhirText.toDoubleOrNull()

    val totalHm = if (hmAkhirVal != null && hmAkhirVal >= shift.hmAwal) {
        hmAkhirVal - shift.hmAwal
    } else 0.0

    val totalKm = if (kmAkhirVal != null && kmAkhirVal >= shift.kmAwal) {
        kmAkhirVal - shift.kmAwal
    } else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MiningGreenActive
                )
                Text(
                    text = "Selesai Shift - ${shift.unitId}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info Awal
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("HM Awal", style = MaterialTheme.typography.labelMedium)
                            Text("${shift.hmAwal}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("KM Awal", style = MaterialTheme.typography.labelMedium)
                            Text("${shift.kmAwal.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Input HM Akhir
                OutlinedTextField(
                    value = hmAkhirText,
                    onValueChange = { hmAkhirText = it },
                    label = { Text("HM Akhir") },
                    placeholder = { Text(">= ${shift.hmAwal}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_hm_akhir"),
                    singleLine = true
                )

                // Input KM Akhir
                OutlinedTextField(
                    value = kmAkhirText,
                    onValueChange = { kmAkhirText = it },
                    label = { Text("KM Akhir") },
                    placeholder = { Text(">= ${shift.kmAwal}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_km_akhir"),
                    singleLine = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Ringkasan Total
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total HM:", fontWeight = FontWeight.Medium)
                            Text(
                                String.format("%.1f", totalHm),
                                fontWeight = FontWeight.Bold,
                                color = MiningAmber
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total KM:", fontWeight = FontWeight.Medium)
                            Text(
                                String.format("%.1f", totalKm),
                                fontWeight = FontWeight.Bold,
                                color = MiningAmber
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Ritase:", fontWeight = FontWeight.Medium)
                            Text("$totalRitase", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Fuel:", fontWeight = FontWeight.Medium)
                            Text("${totalFuel.toInt()} L", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hmAkhirVal == null) {
                        errorMessage = "HM Akhir harus diisi angka valid"
                        return@Button
                    }
                    if (hmAkhirVal < shift.hmAwal) {
                        errorMessage = "HM Akhir tidak boleh lebih kecil dari HM Awal (${shift.hmAwal})"
                        return@Button
                    }
                    if (kmAkhirVal == null) {
                        errorMessage = "KM Akhir harus diisi angka valid"
                        return@Button
                    }
                    if (kmAkhirVal < shift.kmAwal) {
                        errorMessage = "KM Akhir tidak boleh lebih kecil dari KM Awal (${shift.kmAwal})"
                        return@Button
                    }
                    onConfirm(hmAkhirVal, kmAkhirVal)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                modifier = Modifier.testTag("btn_simpan_selesai")
            ) {
                Text("SIMPAN & SELESAI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_batal_selesai")
            ) {
                Text("BATAL")
            }
        }
    )
}
