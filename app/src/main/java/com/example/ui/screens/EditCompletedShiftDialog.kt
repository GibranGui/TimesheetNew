package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ShiftEntity
import com.example.ui.theme.MiningAmber

@Composable
fun EditCompletedShiftDialog(
    shift: ShiftEntity,
    onDismiss: () -> Unit,
    onConfirm: (unitId: String, date: String, hmAwal: Double, hmAkhir: Double, kmAwal: Double, kmAkhir: Double) -> Unit
) {
    var unitText by remember { mutableStateOf(shift.unitId) }
    var dateText by remember { mutableStateOf(shift.date) }
    var hmAwalText by remember { mutableStateOf(shift.hmAwal.toString()) }
    var hmAkhirText by remember { mutableStateOf((shift.hmAkhir ?: shift.hmAwal).toString()) }
    var kmAwalText by remember { mutableStateOf(shift.kmAwal.toInt().toString()) }
    var kmAkhirText by remember { mutableStateOf((shift.kmAkhir ?: shift.kmAwal).toInt().toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MiningAmber)
                Text("Koreksi Data Shift Selesai", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = unitText,
                    onValueChange = { unitText = it },
                    label = { Text("C/N Unit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_comp_unit")
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Tanggal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_comp_date")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hmAwalText,
                        onValueChange = { hmAwalText = it },
                        label = { Text("HM Awal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_comp_hm_awal")
                    )
                    OutlinedTextField(
                        value = hmAkhirText,
                        onValueChange = { hmAkhirText = it },
                        label = { Text("HM Akhir") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_comp_hm_akhir")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kmAwalText,
                        onValueChange = { kmAwalText = it },
                        label = { Text("KM Awal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_comp_km_awal")
                    )
                    OutlinedTextField(
                        value = kmAkhirText,
                        onValueChange = { kmAkhirText = it },
                        label = { Text("KM Akhir") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_comp_km_akhir")
                    )
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
                    val hmAwal = hmAwalText.toDoubleOrNull()
                    val hmAkhir = hmAkhirText.toDoubleOrNull()
                    val kmAwal = kmAwalText.toDoubleOrNull()
                    val kmAkhir = kmAkhirText.toDoubleOrNull()

                    if (hmAwal == null || hmAkhir == null) {
                        errorMessage = "HM Awal dan Akhir harus berupa angka"
                        return@Button
                    }
                    if (hmAkhir < hmAwal) {
                        errorMessage = "HM Akhir ($hmAkhir) tidak boleh lebih kecil dari HM Awal ($hmAwal)"
                        return@Button
                    }
                    if (kmAwal == null || kmAkhir == null) {
                        errorMessage = "KM Awal dan Akhir harus berupa angka"
                        return@Button
                    }
                    if (kmAkhir < kmAwal) {
                        errorMessage = "KM Akhir ($kmAkhir) tidak boleh lebih kecil dari KM Awal ($kmAwal)"
                        return@Button
                    }

                    onConfirm(unitText, dateText, hmAwal, hmAkhir, kmAwal, kmAkhir)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                modifier = Modifier.testTag("btn_save_comp_shift")
            ) {
                Text("SIMPAN KOREKSI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("BATAL")
            }
        }
    )
}
