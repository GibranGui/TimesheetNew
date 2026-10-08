package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditActiveShiftDialog
import com.example.ui.screens.EndShiftDialog
import com.example.ui.screens.FuelInputDialog
import com.example.ui.screens.GoogleDriveScreen
import com.example.ui.screens.GoogleLoginScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RekapanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StartShiftScreen
import com.example.ui.theme.MiningAmber
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

enum class AppTab {
    SHIFT,
    REKAPAN,
    GOOGLE_DRIVE,
    SETTINGS
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
    val activeShift by viewModel.activeShift.collectAsStateWithLifecycle()
    val completedShifts by viewModel.completedShifts.collectAsStateWithLifecycle()
    val recentUnits by viewModel.recentUnits.collectAsStateWithLifecycle()
    val loadingArea by viewModel.loadingArea.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val gpsState by viewModel.gpsState.collectAsStateWithLifecycle()
    val ritaseList by viewModel.activeRitaseList.collectAsStateWithLifecycle()
    val fuelList by viewModel.activeFuelList.collectAsStateWithLifecycle()
    val uiError by viewModel.uiError.collectAsStateWithLifecycle()
    val syncSuccessMessage by viewModel.syncSuccessMessage.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val googleAccount by viewModel.googleAccount.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val showLoginGate by viewModel.showLoginGate.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppTab.SHIFT) }
    var showHistoryScreen by remember { mutableStateOf(false) }
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

    LaunchedEffect(syncSuccessMessage) {
        syncSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    // Jika pertama kali dibuka dan belum ada akun login: Tampilkan UI Google Login terlebih dahulu
    if (showLoginGate && !googleAccount.isSignedIn) {
        GoogleLoginScreen(
            onSignInSuccess = { email -> viewModel.signInGoogle(email) },
            onContinueOffline = { viewModel.dismissLoginGate() }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!showHistoryScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.SHIFT,
                        onClick = { currentTab = AppTab.SHIFT },
                        icon = {
                            Icon(Icons.Default.LocalShipping, contentDescription = "Shift")
                        },
                        label = { Text("Shift", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MiningAmber,
                            selectedTextColor = MiningAmber,
                            indicatorColor = MiningAmber.copy(alpha = 0.15f)
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
                            selectedIconColor = MiningAmber,
                            selectedTextColor = MiningAmber,
                            indicatorColor = MiningAmber.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("tab_rekapan")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.GOOGLE_DRIVE,
                        onClick = { currentTab = AppTab.GOOGLE_DRIVE },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingSyncCount > 0) {
                                        Badge { Text("$pendingSyncCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Drive")
                            }
                        },
                        label = { Text("Drive / Sync", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MiningAmber,
                            selectedTextColor = MiningAmber,
                            indicatorColor = MiningAmber.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("tab_drive")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.SETTINGS,
                        onClick = { currentTab = AppTab.SETTINGS },
                        icon = {
                            Icon(Icons.Default.Tune, contentDescription = "Master")
                        },
                        label = { Text("Master", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MiningAmber,
                            selectedTextColor = MiningAmber,
                            indicatorColor = MiningAmber.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (showHistoryScreen) {
                BackHandler { showHistoryScreen = false }
                HistoryScreen(
                    completedShifts = completedShifts,
                    onBackClick = { showHistoryScreen = false }
                )
            } else {
                when (currentTab) {
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
                            DashboardScreen(
                                shift = currentShift,
                                ritaseList = ritaseList,
                                fuelList = fuelList,
                                gpsState = gpsState,
                                pendingSyncCount = pendingSyncCount,
                                onAddFuelClick = { showFuelDialog = true },
                                onEndShiftClick = { showEndShiftDialog = true },
                                onSyncClick = { viewModel.syncAllPending() },
                                onOpenHistoryClick = { showHistoryScreen = true },
                                onOpenSettingsClick = { currentTab = AppTab.SETTINGS },
                                onSimulateRitaseClick = { viewModel.simulateManualRitase() },
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
                            onSyncClick = { viewModel.syncAllPending() },
                            onEditShift = { shiftId, unit, date, hmAwal, hmAkhir, kmAwal, kmAkhir ->
                                viewModel.updateCompletedShift(
                                    shiftId, unit, date, hmAwal, hmAkhir, kmAwal, kmAkhir
                                )
                            }
                        )
                    }

                    AppTab.GOOGLE_DRIVE -> {
                        GoogleDriveScreen(
                            accountState = googleAccount,
                            pendingSyncCount = pendingSyncCount,
                            onSignInClick = { viewModel.signInGoogle() },
                            onSignOutClick = { viewModel.signOutGoogle() },
                            onSyncNowClick = { viewModel.syncAllPending() }
                        )
                    }

                    AppTab.SETTINGS -> {
                        SettingsScreen(
                            loadingArea = loadingArea,
                            updateInfo = updateInfo,
                            onSaveLoadingArea = { lat, lng, radius ->
                                viewModel.updateLoadingArea(lat, lng, radius)
                            },
                            onCalibrateFromGpsClick = {
                                viewModel.calibrateLoadingAreaFromCurrentLocation()
                            },
                            onCheckUpdateClick = {
                                viewModel.checkUpdateManual()
                            },
                            onBackClick = { currentTab = AppTab.SHIFT }
                        )
                    }
                }
            }
        }
    }
}
