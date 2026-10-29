package com.example

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
import com.example.service.DesktopAutoLaunchService
import com.example.util.DesktopAutoLaunch
import com.example.util.WallpaperProvisioner
import com.example.ui.theme.Slate950
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.PhonixViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PhonixViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No forced orientation: on DeX an orientation-locked activity is confined to a
        // phone-shaped window, which is exactly what we are trying to escape. The desktop
        // adapts to whatever window the window manager gives us.
        enableEdgeToEdge()

        // Paint the Phonix wallpaper behind everything Samsung draws, so the phone home,
        // lock screen and (where the platform mirrors it) DeX match the desktop.
        // Off the main thread: decoding the wallpaper is expensive on a cold start.
        Thread { WallpaperProvisioner.applyOnce(applicationContext) }.start()

        try {
            // Configure Immersive Desktop Mode (hides system navigation gesture bar & status bar for full PC feel)
            setImmersiveDesktopMode(true)
        } catch (_: Exception) {
        }

        // The desktop is a window onto a monitor, so make sure something is always watching
        // for one: BootReceiver starts this at power-on, this covers a manual launch.
        DesktopAutoLaunchService.start(applicationContext)

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

    override fun onResume() {
        super.onResume()
        moveToDesktopDisplayIfNeeded()
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
