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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MiningAccentBlue
import com.example.ui.theme.MiningAmber
import com.example.ui.theme.MiningGreenActive
import com.example.ui.viewmodel.GoogleAccountState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveScreen(
    accountState: GoogleAccountState,
    pendingSyncCount: Int,
    onSignInClick: (String) -> Unit,
    onSignOutClick: () -> Unit,
    onSyncNowClick: () -> Unit
) {
    var showLoginDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf(accountState.email ?: "andreandika1997@gmail.com") }

    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("Login Akun Google") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Masukkan email akun Google yang ingin digunakan untuk sinkronisasi Google Sheets:")
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Google") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginDialog = false
                        onSignInClick(emailInput.ifBlank { "user@gmail.com" })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiningAmber)
                ) {
                    Text("Masuk", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = MiningAmber)
                        Text("Google Drive & Sheets", fontWeight = FontWeight.Bold)
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
            // CARD 1: AKUN GOOGLE
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MiningAmber, modifier = Modifier.size(28.dp))
                            Text("Akun Google Terhubung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (accountState.isSignedIn) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MiningGreenActive.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "TERHUBUNG",
                                    fontWeight = FontWeight.Bold,
                                    color = MiningGreenActive,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    if (accountState.isSignedIn) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = accountState.displayName ?: "Akun Pengguna",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = accountState.email ?: "andreandika1997@gmail.com",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Scope: drive.file, spreadsheets (Least Privilege)",
                                color = MiningAmber,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        OutlinedButton(
                            onClick = onSignOutClick,
                            modifier = Modifier.fillMaxWidth().testTag("btn_logout_google")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Logout Akun Google")
                        }
                    } else {
                        Text(
                            text = "Login diperlukan untuk mengakses folder DT TIME SHEET dan memperbarui spreadsheet bulanan secara otomatis.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showLoginDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_login_google")
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LOGIN DENGAN GOOGLE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            // CARD 2: ANTREAN SINKRONISASI (SYNC QUEUE)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (pendingSyncCount == 0) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (pendingSyncCount == 0) MiningGreenActive else MiningAmber
                            )
                            Text("Status Sinkronisasi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        Text(
                            text = if (pendingSyncCount == 0) "☁️ Tersinkron" else "📴 $pendingSyncCount Pending",
                            fontWeight = FontWeight.Bold,
                            color = if (pendingSyncCount == 0) MiningGreenActive else MiningAmber
                        )
                    }

                    Text(
                        text = "Sinkronisasi Terakhir: ${accountState.lastSyncTime ?: "Belum ada"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onSyncNowClick,
                        enabled = !accountState.isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_sync_now")
                    ) {
                        if (accountState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyinkronkan...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SINKRONKAN SEKARANG KE GOOGLE SHEETS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            // CARD 3: STRUKTUR FOLDER & SPREADSHEET BULANAN
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MiningAmber)
                        Text("Struktur Google Drive Bulanan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    HorizontalDivider()

                    Text("📁 Folder Utama:", fontWeight = FontWeight.SemiBold)
                    Text("• ${accountState.driveFolderName}", color = MiningAmber, fontWeight = FontWeight.Bold)

                    Text("📊 File Spreadsheet Aktif:", fontWeight = FontWeight.SemiBold)
                    Text("• ${accountState.activeMonthFile} (Bulan Oktober 2026)", color = MiningAmber, fontWeight = FontWeight.Bold)

                    Text("📑 Struktur 4 Tab Baku:", fontWeight = FontWeight.SemiBold)
                    Text("1. TIME SHEET (1 baris per shift, Total HM, KM, Ritase, Fuel)")
                    Text("2. RITASE (Log detail tiap waktu ritase & koordinat)")
                    Text("3. FUEL (Log tiap pengisian bahan bakar)")
                    Text("4. MASTER (Daftar unit C/N & Loading Area)")
                }
            }

            // CARD 4: JAMINAN KEAMANAN & OFFLINE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MiningGreenActive, modifier = Modifier.size(18.dp))
                        Text("Prinsip Anti Data Loss & Offline Safe", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = "• Semua data selalu tercatat di SQLite lokal terlebih dahulu.\n• Jika internet mati, GPS dan pencatatan shift tetap berjalan normal tanpa error.\n• Saat online kembali, antrean sinkronisasi dikirim ke Google Sheets tanpa duplikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
