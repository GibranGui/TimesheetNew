package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
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
import com.example.ui.theme.MintPrimary

@Composable
fun FuelInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (hmIsi: Double, liter: Double) -> Unit
) {
    var hmText by remember { mutableStateOf("") }
    var literText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MintPrimary
                )
                Text(
                    text = "Tambah Pengisian Fuel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = hmText,
                    onValueChange = { hmText = it },
                    label = { Text("HM Isi Fuel") },
                    placeholder = { Text("Contoh: 104.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_hm_fuel"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = literText,
                    onValueChange = { literText = it },
                    label = { Text("Liter Fuel") },
                    placeholder = { Text("Contoh: 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_liter_fuel"),
                    singleLine = true
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
                    val hm = hmText.toDoubleOrNull()
                    val liter = literText.toDoubleOrNull()

                    if (hm == null) {
                        errorMessage = "HM Isi Fuel harus angka valid"
                        return@Button
                    }
                    if (liter == null || liter <= 0.0) {
                        errorMessage = "Liter Fuel harus lebih besar dari 0"
                        return@Button
                    }
                    onConfirm(hm, liter)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                modifier = Modifier.testTag("btn_simpan_fuel")
            ) {
                Text("SIMPAN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_batal_fuel")
            ) {
                Text("BATAL")
            }
        }
    )
}
