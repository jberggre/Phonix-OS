package com.example

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CoolingGovernorScreen
import com.example.ui.components.DistroHubScreen
import com.example.ui.components.GamingHubScreen
import com.example.ui.components.InstallationWizardDialog
import com.example.ui.components.LinuxBootSplash
import com.example.ui.components.PhonixDesktopScreen
import com.example.ui.components.AutoHidingTopBar
import com.example.ui.components.PhonixTopBar
import com.example.ui.components.PartitionManagerScreen
import com.example.ui.components.ServerCenterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate950
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.PhonixViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PhonixViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } catch (_: Exception) {
        }
        enableEdgeToEdge()

        try {
            // Configure Immersive Desktop Mode (hides system navigation gesture bar & status bar for full PC feel)
            setImmersiveDesktopMode(true)
        } catch (_: Exception) {
        }

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.notificationMessage) {
                    uiState.notificationMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissNotification()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate950)
                ) {
                    // 1. Linux Boot Splash Animation (When booting directly to Linux distro)
                    if (uiState.isBootSplashActive) {
                        LinuxBootSplash(
                            bootLogs = uiState.bootLogs,
                            distroName = uiState.currentDistro.name
                        )
                    } else {
                        // 2. Main Phonix OS System Environment with Auto-Hiding Top Notch
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Active Tab Content Area
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (uiState.activeTab != AppNavTab.DESKTOP) Modifier.padding(top = 24.dp)
                                        else Modifier
                                    )
                            ) {
                                when (uiState.activeTab) {
                                    AppNavTab.DESKTOP -> PhonixDesktopScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    AppNavTab.COOLING_POWER -> CoolingGovernorScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    AppNavTab.GAMING -> GamingHubScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    AppNavTab.SERVERS -> ServerCenterScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    AppNavTab.PARTITIONS -> PartitionManagerScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    AppNavTab.DISTROS -> DistroHubScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                }
                            }

                            // Sleek Auto-Hiding Top Notch (collapses to ~16dp, expands on hover/tap)
                            AutoHidingTopBar(
                                isExpanded = uiState.isTopNotchExpanded,
                                activeTab = uiState.activeTab,
                                containerStatus = uiState.containerStatus,
                                isBatteryRemoved = uiState.isBatteryRemovedDirectPower,
                                onToggleExpand = { viewModel.setTopNotchExpanded(it) },
                                onTabSelected = { viewModel.setActiveTab(it) },
                                onTogglePower = { viewModel.toggleContainerPower() },
                                onRestart = { viewModel.triggerLinuxRebootAnimation() },
                                onRunDebloat = { viewModel.runScreenlessMiniPcDebloat() },
                                modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
                            )

                            // Toast Notification Host
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .align(androidx.compose.ui.Alignment.BottomCenter)
                                    .padding(bottom = 56.dp)
                            )
                        }
                    }

                    // 3. Step-by-Step Debloat, Partitioning & Installation Walkthrough Wizard
                    if (uiState.isWizardOpen) {
                        InstallationWizardDialog(
                            uiState = uiState,
                            viewModel = viewModel,
                            onDismiss = { viewModel.closeInstallationWizard() }
                        )
                    }
                }
            }
        }
    }

    private fun setImmersiveDesktopMode(immersive: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (immersive) {
            windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
