package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.data.local.AccountManager
import com.example.data.local.LoadingAreaEntity
import com.example.data.remote.SyncStatus
import com.example.ui.theme.MiningAmber
import com.example.ui.viewmodel.UpdateInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    loadingArea: LoadingAreaEntity?,
    updateInfo: UpdateInfo,
    profile: AccountManager.Profile?,
    serverConfigured: Boolean,
    syncStatus: SyncStatus,
    onSaveLoadingArea: (lat: Double, lng: Double, radius: Double) -> Unit,
    onCalibrateFromGpsClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onSaveServerSettings: (url: String, key: String) -> Unit,
    onSyncNowClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var latText by remember(loadingArea) {
        mutableStateOf((loadingArea?.latitude ?: -2.123456).toString())
    }
    var lngText by remember(loadingArea) {
        mutableStateOf((loadingArea?.longitude ?: 115.123456).toString())
    }
    var radiusText by remember(loadingArea) {
        mutableStateOf((loadingArea?.radiusMeters ?: 50.0).toString())
    }
    var saveSuccess by remember { mutableStateOf(false) }
    var serverUrlText by remember { mutableStateOf("") }
    var serverKeyText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan & Konfigurasi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_back_settings")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
            // KONFIGURASI LOADING AREA & RADIUS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MiningAmber)
                        Text("Konfigurasi Loading Area", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Radius menentukan batas zona loading. Default adalah 50 meter. Ritase hanya terhitung jika DT keluar dari radius ini setelah sebelumnya berada di dalam zona loading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = radiusText,
                        onValueChange = { radiusText = it },
                        label = { Text("Radius Loading Area (meter)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_radius_settings"),
                        singleLine = true
                    )

                    // 1x KLIK AMBIL KOORDINAT DARI GPS SAAT INI
                    Button(
                        onClick = onCalibrateFromGpsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_gps_1click_settings")
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📍 AMBIL LOKASI GPS SAAT INI (1x KLIK)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Text(
                        text = "Atau sesuaikan angka koordinat manual jika diperlukan:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("Latitude") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_lat_settings"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lngText,
                            onValueChange = { lngText = it },
                            label = { Text("Longitude") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_lng_settings"),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            val lat = latText.toDoubleOrNull() ?: -2.123456
                            val lng = lngText.toDoubleOrNull() ?: 115.123456
                            val rad = radiusText.toDoubleOrNull() ?: 50.0
                            onSaveLoadingArea(lat, lng, rad)
                            saveSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_simpan_area_settings")
                    ) {
                        Text("SIMPAN KONFIGURASI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }

                    if (saveSuccess) {
                        Text(
                            text = "✓ Koordinat & radius berhasil disimpan!",
                            color = MiningAmber,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // AKUN SAYA
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MiningAmber)
                        Text("Akun Saya", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (profile != null) {
                        Text("Nama: ${profile.fullName}", style = MaterialTheme.typography.bodyMedium)
                        Text("Nomor HP: ${profile.phone}", style = MaterialTheme.typography.bodyMedium)
                        Text("Nama pengguna: ${profile.username}", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = onSignOutClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.height(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Keluar dari Akun")
                        }
                        Text(
                            text = "Data shift tetap tersimpan di HP ini. Untuk masuk kembali cukup gunakan nama pengguna & kata sandi Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // PENYIMPANAN DATA & SINKRONISASI ONLINE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (serverConfigured) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MiningAmber
                        )
                        Text("Penyimpanan & Cadangan Online", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    Text("• Semua data shift, ritase, dan bahan bakar selalu tersimpan di memori HP ini — aplikasi tetap bisa dipakai tanpa internet.", style = MaterialTheme.typography.bodyMedium)
                    Text(if (serverConfigured)
                        "• Cadangan online AKTIF. Data terkirim otomatis saat ada jaringan."
                    else
                        "• Belum ada cadangan online. Opsional: daftarkan alamat server agar data juga tersimpan di internet (berguna jika HP hilang/rusak).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when (syncStatus) {
                        is SyncStatus.Running -> Text("Status: ${syncStatus.progress}…", color = MiningAmber, style = MaterialTheme.typography.bodySmall)
                        is SyncStatus.Success -> Text("Sinkron terakhir berhasil (${syncStatus.pushed} dikirim, ${syncStatus.pulled} diperbarui).", style = MaterialTheme.typography.bodySmall)
                        is SyncStatus.Failed -> Text("Sinkron belum berhasil: ${syncStatus.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        else -> {}
                    }

                    if (serverConfigured) {
                        Button(
                            onClick = onSyncNowClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SINKRONKAN SEKARANG", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        OutlinedTextField(
                            value = serverUrlText,
                            onValueChange = { serverUrlText = it },
                            label = { Text("Alamat Server (URL)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = serverKeyText,
                            onValueChange = { serverKeyText = it },
                            label = { Text("Kunci Akses (dari penyedia server)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (serverUrlText.isNotBlank() && serverKeyText.isNotBlank()) {
                                    onSaveServerSettings(serverUrlText, serverKeyText)
                                }
                            },
                            enabled = serverUrlText.isNotBlank() && serverKeyText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("AKTIFKAN CADANGAN ONLINE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text(
                            text = "Pengaturan cadangan online biasanya diisi oleh kantor/pengelola armada. Jika kosong, biarkan saja — aplikasi tetap berfungsi penuh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // GITHUB RELEASES & UPDATER
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val context = LocalContext.current
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MiningAmber)
                        Text("Pembaruan Aplikasi (GitHub Releases)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Versi Terpasang: v${updateInfo.currentVersion}", fontWeight = FontWeight.Medium)

                    if (updateInfo.message.isNotEmpty()) {
                        Text(
                            text = updateInfo.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (updateInfo.isAvailable) MiningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onCheckUpdateClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_check_update")
                    ) {
                        Text("CEK PEMBARUAN DARI GITHUB")
                    }

                    if (updateInfo.isAvailable && updateInfo.downloadUrl.isNotEmpty()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("UNDUH & INSTALL PEMBARUAN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            // SECURITY POLICY INFO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.height(18.dp))
                        Text("Keamanan & Privasi Data", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = "Akun dan seluruh data pencatatan tersimpan aman di HP ini. Kata sandi dikunci sehingga tidak terbaca. Cadangan online hanya aktif jika server diatur, dan data Anda tetap menjadi milik Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
