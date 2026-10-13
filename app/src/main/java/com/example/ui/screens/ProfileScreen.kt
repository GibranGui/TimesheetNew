package com.example.ui.screens

import com.example.ui.theme.ShapeInner

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountManager
import com.example.ui.theme.EmployeePrimary

/**
 * Tab Profil (§2): identitas akun driver yang sedang login.
 * Dulu label "Akun" salah menunjuk ke SettingsScreen — sekarang terpisah rapi:
 * Profil = data akun & multi-akun (§8), Pengaturan = konfigurasi teknis.
 */
@Composable
fun ProfileScreen(
    profile: AccountManager.Profile?,
    savedAccounts: List<AccountManager.SavedAccount>,
    onSwitchAccount: (accountId: String) -> Unit,
    onSignOutClick: () -> Unit
) {
    var pendingSwitch by remember { mutableStateOf<AccountManager.SavedAccount?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Profil Saya",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // ---- Kartu identitas akun aktif ----
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_card"),
            shape = ShapeInner,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(EmployeePrimary.copy(alpha = 0.15f), ShapeInner),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = EmployeePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            profile?.fullName ?: "Driver",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val phone = profile?.phone.orEmpty()
                        if (phone.isNotBlank()) {
                            Text(
                                phone,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    "Login memakai Nama Driver + kata sandi — tanpa email (§8).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Multi-akun: akun lain yang pernah login di HP ini (§8) ----
        val others = savedAccounts.filter {
            !it.fullName.equals(profile?.fullName.orEmpty(), ignoreCase = true)
        }
        if (others.isNotEmpty()) {
            Text(
                "Ganti akun (satu HP bisa dipakai bergantian)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_switch_list"),
                shape = ShapeInner,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    others.forEach { acc ->
                        OutlinedButton(
                            onClick = { pendingSwitch = acc },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = EmployeePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(acc.fullName, fontSize = 13.sp, color = EmployeePrimary)
                        }
                    }
                    Text(
                        "Catatan: berpindah akun hanya untuk HP yang sama. Data tiap akun tetap tersimpan terpisah.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Keluar ----
        Button(
            onClick = onSignOutClick,
            enabled = profile != null,
            colors = ButtonDefaults.buttonColors(containerColor = EmployeePrimary),
            shape = ShapeInner,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_sign_out")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Keluar dari Akun", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = Color.Black)
        }
        Text(
            "Data shift & catatan Anda tetap aman tersimpan di HP ini meski keluar.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Konfirmasi pindah akun — tetap butuh kata sandi saat login kembali (§8).
    if (pendingSwitch != null) {
        AlertDialog(
            onDismissRequest = { pendingSwitch = null },
            title = { Text("Ganti Akun") },
            text = {
                Text(
                    "Anda akan keluar dari \"${profile?.fullName ?: "-"}\" lalu login sebagai " +
                            "\"${pendingSwitch?.fullName}\" dengan memasukkan kata sandinya."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val acc = pendingSwitch
                    pendingSwitch = null
                    if (acc != null) onSwitchAccount(acc.id)
                }) { Text("Ganti") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwitch = null }) { Text("Batal") }
            }
        )
    }
}
