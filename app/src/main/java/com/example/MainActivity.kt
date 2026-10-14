package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.EditActiveShiftDialog
import com.example.ui.screens.EndShiftDialog
import com.example.ui.screens.FuelInputDialog
import com.example.ui.screens.RekapanScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RosterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StartShiftScreen
import com.example.ui.theme.EmployeePrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

enum class AppTab {
    ROASTER,   // Jadwal Kerja: daftar harian seluruh unit, 1 hari terpilih (§6)
    SHIFT,     // Catat HM: shift aktif / mulai shift (timesheet)
    REKAPAN,   // Rekapan HM bulan berjalan
    PROFIL,    // Akun driver yang login (§2 — label sesuai isi)
    PENGATURAN // Pengaturan teknis (§2 — label sesuai isi)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current

    // Pengecekan berkala (tiap 10 detik): apakah izin & layanan GPS aktif,
    // sehingga peringatan langsung muncul bila lokasi dimatikan saat shift berjalan.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshGpsConnectivity()
            delay(10_000L)
        }
    }

    val activeShift by viewModel.activeShift.collectAsStateWithLifecycle()
    val completedShifts by viewModel.completedShifts.collectAsStateWithLifecycle()
    val recentUnits by viewModel.recentUnits.collectAsStateWithLifecycle()
    val loadingArea by viewModel.loadingArea.collectAsStateWithLifecycle()
    val gpsState by viewModel.gpsState.collectAsStateWithLifecycle()
    val ritaseList by viewModel.activeRitaseList.collectAsStateWithLifecycle()
    val fuelList by viewModel.activeFuelList.collectAsStateWithLifecycle()
    val uiError by viewModel.uiError.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val serverConfigured by viewModel.serverConfigured.collectAsStateWithLifecycle()
    val rosters by viewModel.rosters.collectAsStateWithLifecycle()
    val savedAccountsList by viewModel.savedAccounts.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshAccountState()
    }

    // Pengecekan pembaruan otomatis saat aplikasi dibuka (popup muncul jika ada versi baru).
    LaunchedEffect(accountState.loggedIn) {
        if (accountState.loggedIn) viewModel.checkUpdateOnLaunch()
    }

    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    var showLocationWarning by remember { mutableStateOf(false) }
    var showBatteryWarning by remember { mutableStateOf(false) }

    var locationDismissedThisShift by remember(activeShift?.id) { mutableStateOf(false) }

    // Peringatan bila layanan lokasi (GPS) di HP belum dinyalakan SAAT SHIFT AKTIF saja.
    // (Sebelumnya dialog muncul berulang tiap 15 detik bahkan saat tidak ada shift — membingungkan.)
    LaunchedEffect(activeShift != null, locationDismissedThisShift) {
        if (activeShift != null && !locationDismissedThisShift) {
            while (true) {
                if (!com.example.util.DeviceHealthChecker.isLocationEnabled(context)) {
                    showLocationWarning = true
                } else {
                    showLocationWarning = false
                }
                delay(15_000L)
            }
        } else {
            showLocationWarning = false
        }
    }

    // Arahan optimalisasi baterai: sekali saja per sesi aplikasi.
    var batteryCheckedThisSession by remember { mutableStateOf(false) }
    LaunchedEffect(accountState.loggedIn) {
        if (accountState.loggedIn && !batteryCheckedThisSession) {
            batteryCheckedThisSession = true
            if (!com.example.util.DeviceHealthChecker.isIgnoringBatteryOptimizations(context)) {
                showBatteryWarning = true
            }
        }
    }

    val activity = context as? android.app.Activity

    if (showLocationWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showLocationWarning = false
                locationDismissedThisShift = true
            },
            icon = {
                Icon(
                    Icons.Default.GpsNotFixed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Lokasi Belum Nyala") },
            text = {
                Text("Agar ritase dan jam kerja tercatat otomatis, nyalakan lokasi (GPS) pada HP Anda melalui pengaturan, lalu kembali ke aplikasi.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    activity?.let { com.example.util.DeviceHealthChecker.openLocationSettings(it) }
                    showLocationWarning = false
                }) { Text("Buka Pengaturan Lokasi") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showLocationWarning = false
                    locationDismissedThisShift = true
                }) {
                    Text("Nanti Saja")
                }
            }
        )
    }

    if (showBatteryWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBatteryWarning = false },
            icon = {
                Icon(
                    Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            },
            title = { Text("Hemat Baterai Bisa Menghentikan Hitungan") },
            text = {
                Text("Penghematan baterai HP Anda dapat menghentikan pelacakan shift saat layar mati. Agar ritase tercatat akurat, izinkan aplikasi berjalan tanpa batas hemat baterai.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    activity?.let { com.example.util.DeviceHealthChecker.requestIgnoreBatteryOptimizations(it) }
                    showBatteryWarning = false
                }) { Text("Atur Sekarang") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBatteryWarning = false }) {
                    Text("Nanti Saja")
                }
            }
        )
    }

    if (showUpdateDialog && updateInfo.isAvailable) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            icon = {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = EmployeePrimary
                )
            },
            title = { Text("Pembaruan Tersedia") },
            text = {
                Text(
                    "Versi baru v${updateInfo.latestVersion} sudah tersedia.\n" +
                            "Versi Anda sekarang v${updateInfo.currentVersion}.\n\n" +
                            "Unduh versi terbaru agar fitur dan hitungan ritase tetap lancar."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    try {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(updateInfo.downloadUrl)
                            )
                        )
                    } catch (_: Exception) {
                        //abaikan jika browser tidak tersedia
                    }
                    viewModel.dismissUpdateDialog()
                }) { Text("Unduh Sekarang") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text("Nanti Saja")
                }
            }
        )
    }

    var currentTab by remember { mutableStateOf(AppTab.ROASTER) }
    var showFuelDialog by remember { mutableStateOf(false) }
    var showEndShiftDialog by remember { mutableStateOf(false) }
    var showEditActiveShiftDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiError) {
        uiError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    // Semua kolektor state dibaca DI SINI (level komposabel yang sama, tanpa kondisi),
    // supaya jumlah/perintah pemanggilan Compose identik di setiap recomposition.
    // Sebelumnya kolektor-kolektor ini berada di dalam `if (...)` sehingga saat status
    // login berubah (checked -> loggedIn/loggedOut), urutan slot Compose berubah dan
    // aplikasi crash sesaat setelah layar putih muncul (< 1 detik).
    val driverSuggestions by viewModel.driverSuggestions.collectAsStateWithLifecycle()
    val hasDriverList by viewModel.hasDriverList.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val isRosterSyncing by viewModel.isRosterSyncing.collectAsStateWithLifecycle()
    val rosterLastSync by viewModel.rosterLastSync.collectAsStateWithLifecycle()

    if (!accountState.checked) {
        // Pemeriksaan sesi singkat — tampilkan loading kecil agar tidak blank/crash.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(color = EmployeePrimary)
        }
        return
    }

    if (!accountState.loggedIn) {
        AccountScreen(
            hasAnyAccount = accountState.hasAnyAccount,
            savedAccounts = savedAccounts,
            driverSuggestions = driverSuggestions,
            hasDriverList = hasDriverList,
            onSearchDriverName = { q -> viewModel.searchDriverNames(q) },
            onLogin = { nama, pass -> viewModel.loginAccount(nama, pass) },
            onRegister = { nama, hp, pass, passUlang ->
                viewModel.registerAccount(nama, hp, pass, passUlang)
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    // ---- Menu utama: Roaster (jadwal kerja) ----
                    NavigationBarItem(
                        selected = currentTab == AppTab.ROASTER,
                        onClick = { currentTab = AppTab.ROASTER },
                        icon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Jadwal")
                        },
                        label = { Text("Jadwal", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmployeePrimary,
                            selectedTextColor = EmployeePrimary,
                            indicatorColor = EmployeePrimary.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.testTag("tab_roaster")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.SHIFT,
                        onClick = { currentTab = AppTab.SHIFT },
                        icon = {
                            Icon(Icons.Default.LocalShipping, contentDescription = "Shift")
                        },
                        label = { Text("Catat HM", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmployeePrimary,
                            selectedTextColor = EmployeePrimary,
                            indicatorColor = EmployeePrimary.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.testTag("tab_shift")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.REKAPAN,
                        onClick = { currentTab = AppTab.REKAPAN },
                        icon = {
                            Icon(Icons.Default.Assessment, contentDescription = "Rekapan")
                        },
                        label = { Text("Rekapan", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmployeePrimary,
                            selectedTextColor = EmployeePrimary,
                            indicatorColor = EmployeePrimary.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.testTag("tab_rekapan")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.PROFIL,
                        onClick = { currentTab = AppTab.PROFIL },
                        icon = {
                            Icon(Icons.Default.Person, contentDescription = "Profil")
                        },
                        label = { Text("Profil", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmployeePrimary,
                            selectedTextColor = EmployeePrimary,
                            indicatorColor = EmployeePrimary.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.testTag("tab_profil")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.PENGATURAN,
                        onClick = { currentTab = AppTab.PENGATURAN },
                        icon = {
                            Icon(Icons.Default.Tune, contentDescription = "Pengaturan")
                        },
                        label = { Text("Pengaturan", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmployeePrimary,
                            selectedTextColor = EmployeePrimary,
                            indicatorColor = EmployeePrimary.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentTab) {
                    AppTab.ROASTER -> {
                        // Menu utama: jadwal kerja driver yang login, format kalender sebulan.
                        val calNow0 = java.util.Calendar.getInstance()
                        val daysInMonth0 = calNow0.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                        val myName0 = accountState.profile?.fullName.orEmpty()
                        val mySchedule = remember(rosters, myName0) {
                            (1..daysInMonth0).map { d ->
                                com.example.ui.screens.DaySchedule(d, viewModel.rosterCodeForDay(myName0, d))
                            }
                        }
                        RosterScreen(
                            rosters = rosters,
                            mySchedule = mySchedule,
                            driverName = myName0,
                            vehicleId = activeShift?.unitId ?: recentUnits.firstOrNull()?.cn.orEmpty(),
                            isSyncing = isRosterSyncing,
                            lastSyncTime = rosterLastSync,
                            onRefreshClick = { viewModel.syncRosterNow() },
                            onStartShiftClick = { currentTab = AppTab.SHIFT },
                            isMyRow = { row -> viewModel.rosterMatchesDriver(row, myName0) }
                        )
                    }

                    AppTab.SHIFT -> {
                        if (activeShift == null) {
                            StartShiftScreen(
                                recentUnits = recentUnits,
                                onStartShift = { unit, date, hmAwal, kmAwal ->
                                    viewModel.startShift(unit, date, hmAwal, kmAwal)
                                }
                            )
                        } else {
                            val currentShift = activeShift!!
                            // Ringkasan bulan berjalan + jadwal sebulan driver yang login
                            val calNow = java.util.Calendar.getInstance()
                            val myName = accountState.profile?.fullName.orEmpty()
                            val daysInMonthNow = calNow.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                            // Tanggal shift tersimpan sebagai "dd/MM/yyyy" (lihat StartShiftScreen),
                            // jadi cocokkan bagian bulan/tahun agar filter bulan berjalan benar.
                            val curYear = String.format(
                                java.util.Locale.US, "%04d", calNow.get(java.util.Calendar.YEAR)
                            )
                            val curMonth = String.format(
                                java.util.Locale.US, "%02d", calNow.get(java.util.Calendar.MONTH) + 1
                            )
                            val monthShiftsNow = completedShifts.filter { s ->
                                val mm = Regex("^(\\d{2})/(\\d{2})/(\\d{4})$").find(s.date.trim())
                                mm != null && mm.groupValues[3] == curYear && mm.groupValues[2] == curMonth
                            }
                            val mySchedule = remember(rosters, myName) {
                                (1..daysInMonthNow).map { d ->
                                    com.example.ui.screens.DaySchedule(d, viewModel.rosterCodeForDay(myName, d))
                                }
                            }
                            DashboardScreen(
                                shift = currentShift,
                                ritaseList = ritaseList,
                                fuelList = fuelList,
                                gpsState = gpsState,
                                monthShifts = monthShiftsNow,
                                todayRosterCode = viewModel.rosterCodeForDay(myName, calNow.get(java.util.Calendar.DAY_OF_MONTH)),
                                driverName = myName,
                                myMonthSchedule = mySchedule,
                                onAddFuelClick = { showFuelDialog = true },
                                onEndShiftClick = { showEndShiftDialog = true },
                                onOpenHistoryClick = { currentTab = AppTab.REKAPAN },
                                onOpenSettingsClick = { currentTab = AppTab.PENGATURAN },
                                onCalibrateLoadingAreaClick = { viewModel.calibrateLoadingAreaFromCurrentLocation() },
                                onEditShiftClick = { showEditActiveShiftDialog = true },
                                onDeleteFuelClick = { fuelId -> viewModel.deleteFuelEntry(fuelId) },
                                onDeleteLastRitaseClick = {
                                    ritaseList.lastOrNull()?.let { viewModel.deleteRitaseEntry(it.id) }
                                }
                            )

                            if (showEditActiveShiftDialog) {
                                EditActiveShiftDialog(
                                    shift = currentShift,
                                    onDismiss = { showEditActiveShiftDialog = false },
                                    onConfirm = { unit, date, hmAwal, kmAwal ->
                                        viewModel.updateActiveShiftInfo(unit, date, hmAwal, kmAwal)
                                        showEditActiveShiftDialog = false
                                    }
                                )
                            }

                            if (showFuelDialog) {
                                FuelInputDialog(
                                    onDismiss = { showFuelDialog = false },
                                    onConfirm = { hmIsi, liter ->
                                        viewModel.addFuel(hmIsi, liter)
                                        showFuelDialog = false
                                    }
                                )
                            }

                            if (showEndShiftDialog) {
                                EndShiftDialog(
                                    shift = currentShift,
                                    totalRitase = ritaseList.size,
                                    totalFuel = fuelList.sumOf { it.liter },
                                    onDismiss = { showEndShiftDialog = false },
                                    onConfirm = { hmAkhir, kmAkhir ->
                                        viewModel.endShift(hmAkhir, kmAkhir)
                                        showEndShiftDialog = false
                                    }
                                )
                            }
                        }
                    }

                    AppTab.REKAPAN -> {
                        RekapanScreen(
                            shifts = completedShifts,
                            selectedMonth = selectedMonth,
                            onEditShift = { shiftId, unit, date, hmAwal, hmAkhir, kmAwal, kmAkhir ->
                                viewModel.updateCompletedShift(
                                    shiftId, unit, date, hmAwal, hmAkhir, kmAwal, kmAkhir
                                )
                            }
                        )
                    }

                    AppTab.PROFIL -> {
                        ProfileScreen(
                            profile = accountState.profile,
                            savedAccounts = savedAccountsList,
                            onSwitchAccount = { accountId -> viewModel.useSavedAccount(accountId) },
                            onSignOutClick = { viewModel.signOutAccount() }
                        )
                    }

                    AppTab.PENGATURAN -> {
                        SettingsScreen(
                            loadingArea = loadingArea,
                            updateInfo = updateInfo,
                            profile = accountState.profile,
                            serverConfigured = serverConfigured,
                            syncStatus = syncStatus,
                            onSaveLoadingArea = { lat, lng, radius ->
                                viewModel.updateLoadingArea(lat, lng, radius)
                            },
                            onCalibrateFromGpsClick = {
                                viewModel.calibrateLoadingAreaFromCurrentLocation()
                            },
                            onCheckUpdateClick = {
                                viewModel.checkUpdateManual()
                            },
                            onSyncNowClick = {
                                viewModel.syncNow()
                            },
                            onSignOutClick = {
                                viewModel.signOutAccount()
                            },
                            onBackClick = { currentTab = AppTab.ROASTER }
                        )
                    }
                }
            }
        }
    }
