package com.example

import com.example.ui.components.TASKBAR_HEIGHT_DP

import com.example.model.TaskbarPosition

import android.os.Build
import android.os.Bundle
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import com.example.model.FoldPosture
import com.example.ui.components.CoolingGovernorScreen
import com.example.ui.components.DistroHubScreen
import com.example.ui.components.GamingHubScreen
import com.example.ui.components.InstallationWizardDialog
import com.example.ui.components.LinuxBootSplash
import com.example.ui.components.NetworkSetupScreen
import com.example.ui.components.PhonixDesktopScreen
import com.example.ui.components.AutoHidingTopBar
import com.example.ui.components.PhonixTopBar
import com.example.ui.components.PartitionManagerScreen
import com.example.ui.components.PcAppsScreen
import com.example.ui.components.ServerCenterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.service.DesktopAutoLaunchService
import com.example.util.DesktopAutoLaunch
import com.example.util.WallpaperProvisioner
import com.example.util.DeviceSetupPreferences
import com.example.ui.theme.Slate950
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.PhonixViewModel

class MainActivity : ComponentActivity(), DisplayManager.DisplayListener {

    private val viewModel: PhonixViewModel by viewModels()
    private val displayManager by lazy { getSystemService(DISPLAY_SERVICE) as DisplayManager }
    private val displayHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayManager.registerDisplayListener(this, displayHandler)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { layoutInfo ->
                        val foldingFeature = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                        val posture = when (foldingFeature?.state) {
                            FoldingFeature.State.FLAT -> FoldPosture.FLAT
                            FoldingFeature.State.HALF_OPENED -> FoldPosture.HALF_OPENED
                            else -> FoldPosture.UNKNOWN
                        }
                        val widthDp = resources.configuration.screenWidthDp
                        viewModel.updateFoldPosture(posture, widthDp)
                    }
            }
        }

        // No forced orientation: on DeX an orientation-locked activity is confined to a
        // phone-shaped window, which is exactly what we are trying to escape. The desktop
        // adapts to whatever window the window manager gives us.
        //
        // Explicitly dark: the default keeps light system-bar icons whenever the device is in
        // light mode, which paints white status/navigation icons onto the near-black desktop.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // System wallpaper is an explicit setup choice. Daily-driver installs leave the
        // user's home/lock-screen wallpaper untouched.
        if (DeviceSetupPreferences.shouldApplySystemWallpaper(applicationContext)) {
            Thread { WallpaperProvisioner.applyOnce(applicationContext) }.start()
        }

        try {
            setImmersiveDesktopMode(isOnExternalDisplay() && DeviceSetupPreferences.shouldUseImmersiveExternalDisplay(this))
        } catch (_: Exception) {
        }

        // External display watching is an opt-in appliance behavior, not a default for
        // ordinary phones. An explicit external-display choice starts the watcher.
        if (DeviceSetupPreferences.shouldWatchExternalDisplays(applicationContext)) {
            DesktopAutoLaunchService.start(applicationContext)
        }

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.isOobeCompleted) {
                    if (viewModel.shouldPromptDeviceSetup()) viewModel.openInstallationWizard()
                }

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
                                    AppNavTab.APPS -> PcAppsScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                }
                            }

                            // Sleek Auto-Hiding Top Notch (compact pill; tap/click to expand)
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
                                networkLabel = uiState.network.networkName
                                    ?: if (uiState.network.online) "ONLINE" else "OFFLINE",
                                isOnline = uiState.network.online,
                                onOpenNetwork = { viewModel.openNetworkSetup() },
                                modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
                                // A taskbar along the top edge would draw over the pill, so the
                                // pill hangs off the bar's bottom edge instead.
                                topInset = if (uiState.customization.taskbarPosition == TaskbarPosition.TOP) {
                                    TASKBAR_HEIGHT_DP.dp
                                } else {
                                    0.dp
                                }
                            )

                            // Toast Notification Host
                            // The toast clears a bottom bar, and only a bottom bar: the 56 dp
                            // reserve was applied whatever edge the bar was docked to.
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .align(androidx.compose.ui.Alignment.BottomCenter)
                                    .padding(
                                        bottom = if (uiState.customization.taskbarPosition == TaskbarPosition.BOTTOM) {
                                            56.dp
                                        } else {
                                            12.dp
                                        }
                                    )
                            )
                        }
                    }

                    // 3. Network setup. Above the desktop on purpose: a unit with no working
                    // link cannot install anything, so this is the first thing a customer sees.
                    if (uiState.showNetworkSetup) {
                        NetworkSetupScreen(
                            state = uiState.network,
                            busy = uiState.networkBusy,
                            message = uiState.networkMessage,
                            canDismiss = true,
                            onConnect = { ssid, password -> viewModel.connectToWifi(ssid, password) },
                            onOpenSettings = { viewModel.openAndroidWifiSettings() },
                            onRefresh = { viewModel.checkNetworkAgain() },
                            onDismiss = { viewModel.dismissNetworkSetup() }
                        )
                    }

                    // 4. Step-by-Step Debloat, Partitioning & Installation Walkthrough Wizard
                    if (uiState.isWizardOpen) {
                        InstallationWizardDialog(
                            uiState = uiState,
                            viewModel = viewModel,
                            onDismiss = { viewModel.closeInstallationWizard() },
                            onDeviceChoicesSaved = { refreshSavedDeviceDisplayPolicy() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCapabilitySnapshot()
        setImmersiveDesktopMode(isOnExternalDisplay() && DeviceSetupPreferences.shouldUseImmersiveExternalDisplay(this))
        moveToDesktopDisplayIfNeeded()
        // Returning from the Play Store or Android's Wi-Fi settings must change what the UI
        // shows, so re-read the package list rather than trusting where the customer was.
        viewModel.loadInstalledApps(this)
    }

    override fun onDisplayAdded(displayId: Int) {
        refreshCapabilitySnapshot()
        setImmersiveDesktopMode(isOnExternalDisplay() && DeviceSetupPreferences.shouldUseImmersiveExternalDisplay(this))
    }

    override fun onDisplayChanged(displayId: Int) {
        refreshCapabilitySnapshot()
        setImmersiveDesktopMode(isOnExternalDisplay() && DeviceSetupPreferences.shouldUseImmersiveExternalDisplay(this))
    }

    override fun onDisplayRemoved(displayId: Int) {
        refreshCapabilitySnapshot()
        setImmersiveDesktopMode(isOnExternalDisplay() && DeviceSetupPreferences.shouldUseImmersiveExternalDisplay(this))
    }

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(this)
        super.onDestroy()
    }

    private fun refreshCapabilitySnapshot() {
        viewModel.refreshDeviceCapabilityReport(this, resources.configuration.screenWidthDp)
    }

    fun refreshSavedDeviceDisplayPolicy() {
        refreshCapabilitySnapshot()
        setImmersiveDesktopMode(
            isOnExternalDisplay() && DeviceSetupPreferences.shouldUseImmersiveExternalDisplay(this)
        )
    }

    private fun isOnExternalDisplay(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.displayId?.let { it != android.view.Display.DEFAULT_DISPLAY } == true
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.displayId != android.view.Display.DEFAULT_DISPLAY
        }
    } catch (_: Exception) {
        false
    }

    /**
     * Keeps the desktop on the screen the customer can see.
     *
     * A task keeps the display it was created on. If Phonix was first started there while DeX
     * had not settled (or by a stale launcher intent), every later launch just resumes that
     * invisible instance and the monitor keeps showing Samsung's launcher. Moving the task is
     * not allowed, so the fix is to open a clean instance on the desktop display and retire
     * this one. Runs at most once per process.
     */
    private fun moveToDesktopDisplayIfNeeded() {
        if (!DeviceSetupPreferences.shouldWatchExternalDisplays(this)) return
        if (relocateAttempted) return
        // Only ever move onto a real workspace. Moving onto the bare HDMI output would put the
        // window under DeX's presentation and provoke DeX's cross-display restart dialog.
        val target = DesktopAutoLaunch.workspaceDisplay(this) ?: return
        val current = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.displayId
            else @Suppress("DEPRECATION") windowManager.defaultDisplay.displayId
        } catch (_: Exception) {
            null
        } ?: return
        if (current == target.displayId) return

        relocateAttempted = true
        if (DesktopAutoLaunch.launchDesktop(applicationContext)) finish()
    }

    private companion object {
        /**
         * Process-wide, not per instance: if the re-launch itself lands on the wrong display the
         * new activity would otherwise try again, and again, restarting the desktop forever.
         * One attempt per process is enough — the auto-launch service retries on display events.
         */
        @Volatile
        private var relocateAttempted = false
    }

    private fun setImmersiveDesktopMode(immersive: Boolean) {
        // Draw edge to edge: without this the window still reserves the phone's status bar
        // strip at the top, which on DeX is a band of Samsung chrome over the desktop.
        WindowCompat.setDecorFitsSystemWindows(window, !immersive)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide the status bar as well as the navigation bar: on DeX the status bar is
        // Samsung's, it sits across the top of the monitor and covers the desktop. In
        // immersive mode the app is allowed to take that space, which is what makes the
        // maximised window look like a real desktop instead of an app with a phone bar.
        val bars = WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.statusBars()
        if (immersive) {
            windowInsetsController.hide(bars)
            // DeX's own window title bar is NOT removable from here: measured on One UI 4, the
            // dark window theme, forced fullscreen windowing, captionBar() insets and the legacy
            // immersive flags all leave it in place. It is system decor, drawn outside our window.
            // One UI 8 added a hidden immersive mode; this hardware does not have it.
            // DeX draws its own caption bar above a maximised window (app icon, title,
            // minimise/maximise/close) and the status-bar hide does not remove it. Asking for
            // the caption bar to go as well is the one supported way to get a clean screen; a
            // window manager that does not implement it simply ignores the request.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    windowInsetsController.hide(WindowInsetsCompat.Type.captionBar())
                } catch (_: Exception) {
                }
            }
        } else {
            windowInsetsController.show(bars)
        }
    }
}
