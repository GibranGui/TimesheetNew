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
fun EditActiveShiftDialog(
    shift: ShiftEntity,
    onDismiss: () -> Unit,
    onConfirm: (unitId: String, date: String, hmAwal: Double, kmAwal: Double) -> Unit
) {
    var unitText by remember { mutableStateOf(shift.unitId) }
    var dateText by remember { mutableStateOf(shift.date) }
    var hmAwalText by remember { mutableStateOf(shift.hmAwal.toString()) }
    var kmAwalText by remember { mutableStateOf(shift.kmAwal.toInt().toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MiningAmber)
                Text("Edit Data Shift Aktif", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = unitText,
                    onValueChange = { unitText = it },
                    label = { Text("C/N Unit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_unit_input")
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Tanggal (DD/MM/YYYY)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_date_input")
                )

                OutlinedTextField(
                    value = hmAwalText,
                    onValueChange = { hmAwalText = it },
                    label = { Text("HM Awal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_hm_awal_input")
                )

                OutlinedTextField(
                    value = kmAwalText,
                    onValueChange = { kmAwalText = it },
                    label = { Text("KM Awal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_km_awal_input")
                )

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
                    val hm = hmAwalText.toDoubleOrNull()
                    val km = kmAwalText.toDoubleOrNull()
                    if (unitText.trim().isEmpty()) {
                        errorMessage = "C/N Unit tidak boleh kosong"
                        return@Button
                    }
                    if (dateText.trim().isEmpty()) {
                        errorMessage = "Tanggal tidak boleh kosong"
                        return@Button
                    }
                    if (hm == null) {
                        errorMessage = "HM Awal harus berupa angka"
                        return@Button
                    }
                    if (km == null) {
                        errorMessage = "KM Awal harus berupa angka"
                        return@Button
                    }
                    onConfirm(unitText, dateText, hm, km)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                modifier = Modifier.testTag("btn_save_edit_shift")
            ) {
                Text("SIMPAN PERUBAHAN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("BATAL")
            }
        }
    )
}
