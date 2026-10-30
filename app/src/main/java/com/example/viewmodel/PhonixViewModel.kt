package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.LinuxDesktop
import com.example.engine.LinuxRuntime
import com.example.engine.LocalAiEngine
import com.example.engine.TerminalLine
import com.example.model.DefaultServerCatalog
import com.example.model.DesktopWindow
import com.example.model.DistroCatalog
import com.example.model.LinuxAppCatalog
import com.example.model.LinuxAppEntry
import com.example.model.LinuxAppStatus
import com.example.model.LinuxDistro
import com.example.model.ServerServiceItem
import com.example.model.StorageState
import com.example.model.WindowType
import com.example.service.LinuxServerService
import com.example.model.CoolingMode
import com.example.model.MotherboardCoolingState
import com.example.model.DebloatCatalog
import com.example.model.DebloatPackage
import com.example.model.DesktopCustomization
import com.example.model.DesktopShortcut
import com.example.model.DesktopWallpaper
import com.example.model.DesktopAccentColor
import com.example.model.IconSizeMode
import com.example.model.TaskbarPosition
import com.example.model.TaskbarStyle
import com.example.model.MonitorDisplayMode
import com.example.model.DesktopContextMenuData
import com.example.model.ShortcutShape
import com.example.model.ShortcutBgStyle
import com.example.model.InstalledAppInfo
import com.example.model.NetworkLinkState
import com.example.model.OobeSetupConfig
import com.example.model.AppCategory
import com.example.model.MiniPcProfile
import com.example.model.DesktopThemePreset
import com.example.model.AppUpdateInfo
import com.example.model.UpdateCheckState
import com.example.model.AiChatMessage
import com.example.model.AiEngineState
import com.example.model.DefaultAiKnowledge
import com.example.model.PreferredBrowser
import com.example.util.AppLaunchHelper
import com.example.util.DeviceTelemetry
import com.example.util.NetworkOnboarding
import android.graphics.Rect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Locale
import java.util.UUID

private const val KEY_NETWORK_ONBOARDED = "network_onboarded"

/** Marker echoed by the guest's package probe; parsed back out of the shell output. */
private const val HAVE_PREFIX = "PHONIX_HAVE:"

enum class AppNavTab(val title: String, val icon: String) {
    DESKTOP("Phonix Desktop", "desktop"),
    COOLING_POWER("Thermals & Power", "bolt"),
    GAMING("Games", "gamepad"),
    SERVERS("Servers", "dns"),
    PARTITIONS("Storage", "storage"),
    DISTROS("Linux System", "apps"),
    APPS("PC Apps", "store")
}

enum class ContainerStatus {
    STOPPED, STARTING, RUNNING, RESTARTING
}

/**
 * Live device readings. Every field starts at "unknown" and is only ever written from
 * [DeviceTelemetry]; nothing here is a placeholder that pretends to be a measurement.
 */
data class SystemTelemetry(
    val cpuPercent: Int = 0,
    val ramUsedMb: Int = 0,
    val ramTotalMb: Int = 0,
    /** Hottest readable thermal zone in °C (falls back to the battery sensor). */
    val hottestZoneC: Float = 0f,
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    /** False until the first successful read, so the UI can show "-" instead of a 0. */
    val hasReading: Boolean = false
)

data class PhonixUiState(
    val activeTab: AppNavTab = AppNavTab.DESKTOP,
    val containerStatus: ContainerStatus = ContainerStatus.RUNNING,
    val availableDistros: List<LinuxDistro> = DistroCatalog.distros,
    val currentDistro: LinuxDistro = DistroCatalog.distros.first(),
    val serverServices: List<ServerServiceItem> = DefaultServerCatalog.getDefaultServices(),
    val telemetry: SystemTelemetry = SystemTelemetry(),
    val uptimeSeconds: Long = 0L,
    /** Empty until a real interface address is found; never a made-up LAN IP. */
    val localIpAddress: String = "",
    val storage: StorageState = StorageState(),
    val windows: List<DesktopWindow> = emptyList(),
    val activeWindowId: String? = null,
    val isStartMenuOpen: Boolean = false,
    val startMenuSearchQuery: String = "",
    val terminalLines: List<TerminalLine> = emptyList(),
    val terminalInput: String = "",
    val isBenchmarkRunning: Boolean = false,
    val benchmarkLog: List<String> = emptyList(),
    val notificationMessage: String? = null,
    // Installation Wizard State
    val isWizardOpen: Boolean = false,
    val wizardStep: Int = 0,
    val selectedDeviceProfile: DeviceProfile = DeviceProfileCatalog.defaultProfile,
    val wizardProgress: Float = 0f,
    val wizardStatusMessage: String = "",
    val isWizardDeploying: Boolean = false,
    val wizardLogs: List<String> = emptyList(),
    // Direct Linux Distro Boot State
    val isBootSplashActive: Boolean = false,
    val directBootToLinuxDistro: Boolean = true,
    val showDualBootPrompt: Boolean = false,
    val dualBootCountdownSeconds: Int = 5,
    val isImmersiveFullscreen: Boolean = true,
    val bootLogs: List<String> = emptyList(),
    // Debloat & Power State
    val debloatPackages: List<DebloatPackage> = DebloatCatalog.getDefaultPackages(),
    val coolingState: MotherboardCoolingState = MotherboardCoolingState(),
    val isBatteryRemovedDirectPower: Boolean = true,
    val autoBootOnUsbPower: Boolean = true,
    val customization: DesktopCustomization = DesktopCustomization(),
    val isTopNotchExpanded: Boolean = false,
    val activeContextMenu: DesktopContextMenuData? = null,
    val isOobeCompleted: Boolean = false,
    val oobeConfig: OobeSetupConfig = OobeSetupConfig(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isAddAppDialogOpen: Boolean = false,
    val addAppSearchQuery: String = "",
    // Network onboarding. Link state is read from the platform, never assumed.
    val network: NetworkLinkState = NetworkLinkState(),
    val showNetworkSetup: Boolean = false,
    val networkBusy: Boolean = false,
    val networkMessage: String? = null,
    // PC apps installed inside the guest. null means "not asked yet", never "missing".
    val linuxApps: List<LinuxAppStatus> = LinuxAppCatalog.apps.map { LinuxAppStatus(it) },
    val linuxAppsProbing: Boolean = false,
    val linuxAppBusyPackage: String? = null,
    val linuxAppLog: List<String> = emptyList(),
    // Software Updates (GitHub Releases)
    val updateState: UpdateCheckState = UpdateCheckState.IDLE,
    val updateInfo: AppUpdateInfo? = null,
    // Local AI: the model is opt-in, downloaded outside the APK, and only READY after /health.
    val aiChatMessages: List<AiChatMessage> = DefaultAiKnowledge.getInitialMessages(),
    val isAiGenerating: Boolean = false,
    val aiEngineState: AiEngineState = AiEngineState.NOT_SET_UP,
    val aiEngineMessage: String = "Local model not set up - the built-in notes still work offline.",
    val aiSetupRunning: Boolean = false,
    val aiModelPresent: Boolean = false,
    val aiServerPackagePresent: Boolean = false,
    val aiDownloadProgress: Float = 0f,
    val aiDownloadedBytes: Long = 0L,
    val aiDownloadTotalBytes: Long = LocalAiEngine.MODEL_BYTES,
    val aiSetupLog: List<String> = emptyList(),
    val aiModelTokensPerSecond: Float? = null,
    // VS Code is only "running" after a real loopback connection succeeds.
    val vsCodeServerRunning: Boolean = false,
    val vsCodeServerProbing: Boolean = false,
    val vsCodeServerStatus: String = "Server not started"
)

enum class BootTarget {
    LINUX_DISTRO_DIRECT,
    DUAL_BOOT_MENU,
    ANDROID_PHONE_OS
}

data class DeviceProfile(
    val id: String,
    val name: String,
    val chip: String,
    val ramGb: Int,
    val totalStorageGb: Float,
    val recommendedLinuxGb: Float,
    val recommendedSwapGb: Float,
    val dexDockSupported: Boolean,
    val batteryGuidance: String
)

object DeviceProfileCatalog {
    val profiles = listOf(
        DeviceProfile(
            id = "samsung_s8_exynos",
            name = "Samsung Galaxy S8 / Note 8 (Exynos)",
            chip = "Exynos 8895 Octa (Mali-G71 MP20)",
            ramGb = 4,
            totalStorageGb = 64f,
            recommendedLinuxGb = 16f,
            recommendedSwapGb = 2f,
            dexDockSupported = true,
            batteryGuidance = "S8/Note 8 batteries are 7+ years old. For 24/7 use keep the phone on a flat surface, avoid thick cases and prefer a slow 5V charger over fast charging. A swollen cell means stop using it."
        ),
        DeviceProfile(
            id = "samsung_s8_snapdragon",
            name = "Samsung Galaxy S8 / Note 8 (Snapdragon)",
            chip = "Qualcomm Snapdragon 835 (Adreno 540)",
            ramGb = 4,
            totalStorageGb = 64f,
            recommendedLinuxGb = 16f,
            recommendedSwapGb = 2f,
            dexDockSupported = true,
            batteryGuidance = "Wired Samsung DeX works on this generation over USB-C. The Linux guest renders with software X11 (Xvfb); there is no GPU passthrough."
        ),
        DeviceProfile(
            id = "samsung_s9_s10",
            name = "Samsung Galaxy S9 / S10 / Note 9 (6-8GB RAM)",
            chip = "Snapdragon 845/855 or Exynos 9810/9820",
            ramGb = 6,
            totalStorageGb = 128f,
            recommendedLinuxGb = 24f,
            recommendedSwapGb = 2f,
            dexDockSupported = true,
            batteryGuidance = "Comfortable for the Alpine guest, the X11 desktop and a couple of small services at once. The desktop is software-rendered, so heavy 3D work belongs on the Android side."
        ),
        DeviceProfile(
            id = "samsung_s20_newer",
            name = "Samsung Galaxy S20 / Note 20 & Newer",
            chip = "Snapdragon 865+ / Exynos 990+ (8-12GB RAM)",
            ramGb = 8,
            totalStorageGb = 128f,
            recommendedLinuxGb = 32f,
            recommendedSwapGb = 4f,
            dexDockSupported = true,
            batteryGuidance = "The most headroom of the supported generations. The guest is a proot container, so Docker and systemd do not run inside it; DeX decides the external display resolution."
        ),
        DeviceProfile(
            id = "generic_older_phone",
            name = "Other Older Android Phone (2-3GB RAM)",
            chip = "Legacy ARMv7/ARMv8 Octa/Quad Core",
            ramGb = 3,
            totalStorageGb = 32f,
            recommendedLinuxGb = 8f,
            recommendedSwapGb = 2f,
            dexDockSupported = false,
            batteryGuidance = "Alpine plus X11 is about 450 MB of packages, but RAM is the real limit on 2-3 GB phones; avoid a browser inside the guest."
        )
    )

    val defaultProfile = profiles[0]
}

class PhonixViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Mirrors the container's output into the terminal window. The container itself lives in
     * [LinuxRuntime], owned by the foreground service, so it outlives this activity.
     */
    private val linuxOutput = LinuxRuntime.OutputListener { text, systemMessage ->
        appendTerminalLine(
            TerminalLine(
                text,
                if (systemMessage) TerminalLine.LineType.SYSTEM else classifyShellLine(text)
            )
        )
    }

    private val _uiState = MutableStateFlow(PhonixUiState())
    val uiState: StateFlow<PhonixUiState> = _uiState.asStateFlow()

    private var telemetryJob: Job? = null
    private var networkWatcher: AutoCloseable? = null
    private var maxZIndex: Float = 3f
    private val localAi = LocalAiEngine(application)
    private var aiSetupJob: Job? = null
    private var aiChatJob: Job? = null
    private var aiStreamingMessageId: String? = null

    init {
        val prefs = application.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
        val oobeDone = prefs.getBoolean("oobe_completed", false)
        _uiState.update { it.copy(isOobeCompleted = oobeDone) }

        detectLocalIp()
        initTerminalWelcome()
        startTelemetryLoop()
        loadInstalledApps(application)
        LinuxRuntime.addListener(linuxOutput)
        bootstrapLinux()
        NetworkOnboarding.allowSsidReadback(application)
        refreshNetwork(allowFirstRunPrompt = true)
        // Every link change (join, drop, Ethernet plugged in) refreshes the header and screen.
        networkWatcher = NetworkOnboarding.watch(application) { refreshNetwork() }
        refreshLinuxApps()
    }

    /**
     * Brings the Alpine userspace up off the main thread and hands the terminal over to it.
     * Until it is ready the built-in emulator answers commands, so a slow first-run unpack
     * never leaves the desktop without a terminal.
     */
    /**
     * Reads the live link state and, on a freshly provisioned unit with no network, opens the
     * setup screen by itself. The prompt is one-shot per unit: dismissing it records the
     * choice, so a machine that is deliberately offline is not nagged at every boot.
     */
    private fun refreshNetwork(allowFirstRunPrompt: Boolean = false) {
        val app = getApplication<Application>()
        val link = NetworkOnboarding.currentState(app)
        _uiState.update { current ->
            val prompt = allowFirstRunPrompt && !link.online &&
                !app.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
                    .getBoolean(KEY_NETWORK_ONBOARDED, false)
            current.copy(network = link, showNetworkSetup = current.showNetworkSetup || prompt)
        }
    }

    fun openNetworkSetup() {
        refreshNetwork()
        _uiState.update { it.copy(showNetworkSetup = true, networkMessage = null) }
    }

    fun dismissNetworkSetup() {
        getApplication<Application>()
            .getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NETWORK_ONBOARDED, true).apply()
        _uiState.update { it.copy(showNetworkSetup = false, networkMessage = null) }
    }

    fun checkNetworkAgain() = refreshNetwork()

    fun openAndroidWifiSettings() {
        if (!NetworkOnboarding.openWifiSettings(getApplication())) {
            _uiState.update {
                it.copy(networkMessage = "Android would not open its Wi-Fi settings on this unit.")
            }
        }
    }

    /**
     * Saves the network through the platform and waits for Android to validate it.
     *
     * The wait is deliberate: addNetwork only writes a configuration, it does not prove the
     * password is right. Success is reported from the platform's validated state, never from
     * the return value of the write, so the customer never sees "connected" while offline.
     */
    fun connectToWifi(ssid: String, password: String) {
        if (_uiState.value.networkBusy) return
        _uiState.update { it.copy(networkBusy = true, networkMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            NetworkOnboarding.allowSsidReadback(app)
            when (val result = NetworkOnboarding.connectWifi(app, ssid, password)) {
                is NetworkOnboarding.ConnectResult.Rejected ->
                    _uiState.update { it.copy(networkBusy = false, networkMessage = result.reason) }

                is NetworkOnboarding.ConnectResult.Configured -> {
                    _uiState.update {
                        it.copy(networkMessage = "Saved $ssid. Waiting for Android to join it...")
                    }
                    val deadline = System.currentTimeMillis() + 30_000
                    var online = false
                    while (System.currentTimeMillis() < deadline) {
                        delay(1_000)
                        val link = NetworkOnboarding.currentState(app)
                        _uiState.update { it.copy(network = link) }
                        if (link.online) {
                            online = true
                            break
                        }
                    }
                    if (online) {
                        app.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_NETWORK_ONBOARDED, true).apply()
                        _uiState.update {
                            it.copy(
                                networkBusy = false,
                                networkMessage = null,
                                showNetworkSetup = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                networkBusy = false,
                                networkMessage = "The network was saved, but Android has not confirmed " +
                                    "internet access yet. Check the password, or open Android's Wi-Fi settings."
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Asks the guest which packages are really installed. Until it answers, every entry stays
     * `installed = null` and the UI says "not checked" rather than claiming either way.
     */
    fun refreshLinuxApps() {
        if (!LinuxRuntime.isRunning) return
        _uiState.update { it.copy(linuxAppsProbing = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val probe = LinuxRuntime.run(linuxProbeCommand(), timeoutMs = 20_000)
            if (probe == null) {
                _uiState.update {
                    it.copy(
                        linuxAppsProbing = false,
                        linuxAppLog = listOf("The container did not answer the package probe.")
                    )
                }
                return@launch
            }
            val installed = probe.lineSequence()
                .filter { it.startsWith(HAVE_PREFIX) }
                .map { it.removePrefix(HAVE_PREFIX).trim() }
                .toSet()
            _uiState.update { state ->
                state.copy(
                    linuxAppsProbing = false,
                    linuxApps = state.linuxApps.map { status ->
                        status.copy(installed = installed.contains(status.entry.packageName))
                    }
                )
            }
        }
    }

    /** Runs the real `apk add` in the guest and reports its own output, including failures. */
    fun installLinuxApp(entry: LinuxAppEntry) {
        if (_uiState.value.linuxAppBusyPackage != null) return
        _uiState.update {
            it.copy(
                linuxAppBusyPackage = entry.packageName,
                linuxAppLog = listOf("apk add --no-cache ${entry.packageName}")
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!LinuxRuntime.isRunning) LinuxRuntime.start(getApplication())
                val output = LinuxRuntime.run(
                    "apk add --no-cache ${entry.packageName} 2>&1",
                    timeoutMs = 600_000
                )
                val tail = output?.lines()?.filter { it.isNotBlank() }?.takeLast(14).orEmpty()
                _uiState.update {
                    it.copy(
                        linuxAppBusyPackage = null,
                        linuxAppLog = tail.ifEmpty {
                            listOf("No output. The container may not be running; press Start container.")
                        }
                    )
                }
                refreshLinuxApps()
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        linuxAppBusyPackage = null,
                        linuxAppLog = listOf("Install failed: ${t.javaClass.simpleName}: ${t.message}")
                    )
                }
            }
        }
    }

    private fun linuxProbeCommand(): String =
        LinuxAppCatalog.apps.joinToString("; ") { entry ->
            "apk info -e ${entry.packageName} >/dev/null 2>&1 && echo $HAVE_PREFIX${entry.packageName}"
        }

    private fun bootstrapLinux() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                LinuxRuntime.start(getApplication())
                // A foreground service keeps the process alive when the desktop closes,
                // which is what makes this behave like a machine rather than an app.
                startBackgroundServiceIfActive()
                LinuxRuntime.send("uname -a; cat /etc/os-release | head -2")
            } catch (t: Throwable) {
                appendTerminalLine(
                    TerminalLine("[container] could not start: ${t.javaClass.simpleName}: ${t.message}", TerminalLine.LineType.ERROR)
                )
                appendTerminalLine(
                    TerminalLine("Built-in command emulator remains active for this session.", TerminalLine.LineType.SYSTEM)
                )
            }
        }
    }

    private fun appendTerminalLine(line: TerminalLine) {
        _uiState.update { state ->
            val lines = state.terminalLines + line
            // A long-lived 24/7 shell would otherwise grow without bound.
            state.copy(terminalLines = if (lines.size > 400) lines.takeLast(300) else lines)
        }
    }

    private fun classifyShellLine(line: String): TerminalLine.LineType {
        val lower = line.lowercase()
        return when {
            lower.contains("not found") || lower.contains("permission denied") ||
                lower.contains("no such file") || lower.startsWith("error") -> TerminalLine.LineType.ERROR
            else -> TerminalLine.LineType.OUTPUT
        }
    }

    private fun detectLocalIp() {
        viewModelScope.launch {
            try {
                var foundIp: String? = null
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            foundIp = addr.hostAddress
                            break
                        }
                    }
                    if (foundIp != null) break
                }
                if (foundIp != null) {
                    _uiState.update { it.copy(localIpAddress = foundIp) }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun initTerminalWelcome() {
        _uiState.update { state ->
            state.copy(
                terminalLines = listOf(
                    TerminalLine(
                        "===========================================================",
                        TerminalLine.LineType.ACCENT
                    ),
                    TerminalLine(
                        " Phonix OS Desktop — Alpine Linux terminal (aarch64)",
                        TerminalLine.LineType.ACCENT
                    ),
                    TerminalLine(
                        " Container: proot userspace, no root required | SoC telemetry: live",
                        TerminalLine.LineType.SYSTEM
                    ),
                    TerminalLine(
                        " Once booted, commands run for real: apk add nginx, ps, df -h",
                        TerminalLine.LineType.OUTPUT
                    ),
                    TerminalLine(
                        "===========================================================",
                        TerminalLine.LineType.ACCENT
                    )
                )
            )
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (isActive) {
                // Real measurements, not animation: the Vitals widget must tell the truth
                // about the machine it is running on. File reads happen off the main thread.
                val snapshot = withContext(Dispatchers.IO) {
                    try {
                        DeviceTelemetry.read(getApplication())
                    } catch (e: Exception) {
                        null
                    }
                }

                if (snapshot != null) {
                    _uiState.update { state ->
                        state.copy(
                            telemetry = state.telemetry.copy(
                                cpuPercent = snapshot.cpuPercent,
                                ramUsedMb = snapshot.ramUsedMb,
                                ramTotalMb = snapshot.ramTotalMb,
                                hottestZoneC = snapshot.tempC,
                                batteryLevel = snapshot.batteryLevel,
                                isCharging = snapshot.isCharging,
                                hasReading = true
                            ),
                            uptimeSeconds = android.os.SystemClock.elapsedRealtime() / 1000L
                        )
                    }
                }
                delay(2000L)
            }
        }
    }

    // Navigation
    fun setActiveTab(tab: AppNavTab) {
        _uiState.update { it.copy(activeTab = tab, isStartMenuOpen = false) }
    }

    // Start Menu
    fun toggleStartMenu() {
        _uiState.update { it.copy(isStartMenuOpen = !it.isStartMenuOpen) }
    }

    fun closeStartMenu() {
        _uiState.update { it.copy(isStartMenuOpen = false) }
    }

    fun updateStartMenuSearch(query: String) {
        _uiState.update { it.copy(startMenuSearchQuery = query) }
    }

    // Window Management
    fun openWindow(type: WindowType) {
        val existing = _uiState.value.windows.find { it.type == type }
        if (existing != null) {
            maxZIndex += 1f
            _uiState.update { state ->
                state.copy(
                    windows = state.windows.map { win ->
                        if (win.id == existing.id) {
                            win.copy(isMinimized = false, zIndex = maxZIndex)
                        } else win
                    },
                    activeWindowId = existing.id,
                    isStartMenuOpen = false
                )
            }
        } else {
            maxZIndex += 1f
            val count = _uiState.value.windows.size
            val newWin = DesktopWindow(
                id = "win_${type.name.lowercase(Locale.ROOT)}_${UUID.randomUUID().toString().take(4)}",
                type = type,
                posX = (24f + (count * 20f)).coerceAtMost(100f),
                posY = (24f + (count * 20f)).coerceAtMost(120f),
                width = when (type) {
                    WindowType.BROWSER -> 400f
                    WindowType.PRIVACY -> 470f
                    WindowType.VS_CODE -> 640f
                    WindowType.AI_ASSISTANT -> 440f
                    else -> 360f
                },
                height = when (type) {
                    WindowType.BROWSER -> 380f
                    WindowType.PRIVACY -> 540f
                    WindowType.VS_CODE -> 480f
                    WindowType.AI_ASSISTANT -> 480f
                    else -> 340f
                },
                // The Linux desktop is the point of the product, so it opens full size instead
                // of as a small window the customer has to resize before it looks like a PC.
                isMaximized = type == WindowType.LINUX_DESKTOP,
                zIndex = maxZIndex
            )
            _uiState.update { state ->
                state.copy(
                    windows = state.windows + newWin,
                    activeWindowId = newWin.id,
                    isStartMenuOpen = false
                )
            }
        }
    }

    fun closeWindow(windowId: String) {
        _uiState.update { state ->
            val updated = state.windows.filter { it.id != windowId }
            state.copy(
                windows = updated,
                activeWindowId = updated.maxByOrNull { it.zIndex }?.id
            )
        }
    }

    fun minimizeWindow(windowId: String) {
        _uiState.update { state ->
            val updated = state.windows.map {
                if (it.id == windowId) it.copy(isMinimized = true) else it
            }
            state.copy(
                windows = updated,
                activeWindowId = updated.filter { !it.isMinimized }.maxByOrNull { it.zIndex }?.id
            )
        }
    }

    fun toggleMaximizeWindow(windowId: String) {
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map {
                    if (it.id == windowId) it.copy(isMaximized = !it.isMaximized) else it
                }
            )
        }
    }

    fun focusWindow(windowId: String) {
        maxZIndex += 1f
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map {
                    if (it.id == windowId) it.copy(isMinimized = false, zIndex = maxZIndex) else it
                },
                activeWindowId = windowId,
                isStartMenuOpen = false
            )
        }
    }

    fun moveWindow(windowId: String, deltaX: Float, deltaY: Float) {
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map { win ->
                    if (win.id == windowId && !win.isMaximized) {
                        win.copy(
                            posX = (win.posX + deltaX).coerceIn(-100f, 850f),
                            posY = (win.posY + deltaY).coerceIn(8f, 750f)
                        )
                    } else win
                }
            )
        }
    }

    fun resizeWindow(windowId: String, deltaWidth: Float, deltaHeight: Float) {
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map { win ->
                    if (win.id == windowId && !win.isMaximized) {
                        win.copy(
                            width = (win.width + deltaWidth).coerceIn(240f, 900f),
                            height = (win.height + deltaHeight).coerceIn(180f, 800f)
                        )
                    } else win
                }
            )
        }
    }

    // Terminal
    fun updateTerminalInput(input: String) {
        _uiState.update { it.copy(terminalInput = input) }
    }

    fun executeTerminalCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        if (trimmed.equals("clear", ignoreCase = true)) {
            _uiState.update { it.copy(terminalLines = emptyList(), terminalInput = "") }
            return
        }

        if (trimmed.equals("reboot", ignoreCase = true)) {
            _uiState.update { it.copy(terminalInput = "") }
            triggerLinuxRebootAnimation()
            return
        }

        if (trimmed.equals("poweroff", ignoreCase = true) || trimmed.equals("shutdown", ignoreCase = true)) {
            _uiState.update { it.copy(terminalInput = "") }
            toggleContainerPower()
            return
        }

        // A live container answers for real. The emulator only covers the window between
        // activity start and the shell being ready (or a failed unpack).
        if (LinuxRuntime.isRunning && LinuxRuntime.send(trimmed)) {
            _uiState.update {
                it.copy(
                    terminalLines = it.terminalLines + TerminalLine(
                        "phonix@phonix-minipc:~$ $trimmed",
                        TerminalLine.LineType.INPUT
                    ),
                    terminalInput = ""
                )
            }
            return
        }

        // No container, no answer. Saying so is better than printing a prepared response.
        _uiState.update { curr ->
            curr.copy(
                terminalLines = curr.terminalLines + TerminalLine(
                    "[phonix] the Linux container is not running - start it from the Terminal power button",
                    TerminalLine.LineType.ERROR
                ),
                terminalInput = ""
            )
        }
    }

    fun clearTerminal() {
        _uiState.update { it.copy(terminalLines = emptyList()) }
    }

    // Server Management
    /**
     * Server Hub action.
     *
     * There is no daemon to switch on from here: the package either exists inside the guest or
     * it does not. So this asks the guest and, when missing, runs the real install command in
     * the terminal. Nothing is reported as running until the process is actually found.
     */
    fun toggleServerService(serviceId: String) {
        val service = _uiState.value.serverServices.find { it.id == serviceId } ?: return
        viewModelScope.launch {
            if (!LinuxRuntime.isRunning) {
                _uiState.update { it.copy(notificationMessage = "Start the Linux container first (Terminal power button)") }
                return@launch
            }
            if (!service.isInstalled) {
                appendTerminalLine(TerminalLine("apk add --no-cache ${service.apkPackage}", TerminalLine.LineType.INPUT))
                _uiState.update { it.copy(notificationMessage = "Installing ${service.apkPackage} in the guest - watch the terminal") }
                LinuxRuntime.send("apk add --no-cache ${service.apkPackage}")
            } else if (!service.isRunning) {
                appendTerminalLine(TerminalLine("# ${service.name} is installed. Start it from the terminal:", TerminalLine.LineType.SYSTEM))
                appendTerminalLine(TerminalLine(service.startHint, TerminalLine.LineType.SYSTEM))
                _uiState.update { it.copy(notificationMessage = "${service.name}: run the command shown in the terminal") }
            } else {
                _uiState.update { it.copy(notificationMessage = "${service.name} is running (process found in the guest)") }
            }
            refreshServerStates()
        }
    }

    /** Reads installed/running state straight out of the guest. No assumptions. */
    fun refreshServerStates() {
        viewModelScope.launch {
            val services = _uiState.value.serverServices
            if (!LinuxRuntime.isRunning || services.isEmpty()) return@launch
            val script = services.joinToString("; ") { s ->
                "apk info -e ${s.apkPackage} >/dev/null 2>&1 && echo pkg:${s.id}=1 || echo pkg:${s.id}=0; " +
                    "pidof ${s.processName} >/dev/null 2>&1 && echo run:${s.id}=1 || echo run:${s.id}=0"
            }
            val out = LinuxRuntime.run(script, 20_000) ?: return@launch
            val installed = mutableSetOf<String>()
            val running = mutableSetOf<String>()
            out.lineSequence().forEach { line ->
                val t = line.trim()
                if (t.startsWith("pkg:") && t.endsWith("=1")) installed += t.removePrefix("pkg:").removeSuffix("=1")
                if (t.startsWith("run:") && t.endsWith("=1")) running += t.removePrefix("run:").removeSuffix("=1")
            }
            _uiState.update { state ->
                state.copy(
                    serverServices = state.serverServices.map {
                        it.copy(isInstalled = it.id in installed, isRunning = it.id in running)
                    }
                )
            }
        }
    }

    fun toggleService(serviceId: String) = toggleServerService(serviceId)

    fun selectTab(tab: AppNavTab) = setActiveTab(tab)

    fun toggleBackgroundService(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                notificationMessage = if (enabled) "24/7 background mode enabled" else "Background mode disabled"
            )
        }
        if (enabled) {
            startBackgroundServiceIfActive()
        } else {
            stopBackgroundService()
        }
    }

    /**
     * Measures the container for real: write/read throughput inside the guest plus a shell
     * loop timing. The old screen printed made-up glmark2 frame rates and a random score;
     * a number is only useful if it came from the machine.
     */
    fun runContainerBenchmark() {
        if (_uiState.value.isBenchmarkRunning) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBenchmarkRunning = true,
                    benchmarkLog = listOf("Measuring the Alpine guest (real dd + shell loop)...")
                )
            }
            // No cheap monotonic clock is available in the guest: busybox `date` ignores %N
            // (so a timestamp is only whole seconds) and Android's SELinux policy denies
            // /proc/uptime to apps. The shell's own `time` builtin is real, so the loop is
            // timed with it and the "real 0m 0.12s" line is parsed here.
            val script = """
                dd if=/dev/zero of=/tmp/phonix-bench bs=1M count=64 2>&1 | tail -1
                dd if=/tmp/phonix-bench of=/dev/null bs=1M 2>&1 | tail -1
                rm -f /tmp/phonix-bench
                { time awk 'BEGIN{for(i=0;i<2000000;i++)s+=i}'; } 2>&1 | grep real
            """.trimIndent().replace("\n", "; ")
            val out = LinuxRuntime.run(script, 30_000)
            val lines = out?.lines()?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val loopMs = lines.firstOrNull { it.startsWith("real") }?.let { parseRealMs(it) }
            _uiState.update {
                it.copy(
                    isBenchmarkRunning = false,
                    benchmarkLog = if (lines.isEmpty()) {
                        listOf("No answer from the container - is it running?")
                    } else {
                        listOf("Real measurements from inside the guest:") +
                            lines +
                            (loopMs?.let { listOf("shell loop (2M awk iterations): \$it ms") } ?: emptyList())
                    }
                )
            }
        }
    }

    /** Parses the shell's `real 0m 0.12s` line into milliseconds. */
    private fun parseRealMs(line: String): Int? {
        val match = Regex("""real\s+(?:(\d+)m\s*)?([\d.]+)s""").find(line) ?: return null
        val minutes = match.groupValues[1].toIntOrNull() ?: 0
        val seconds = match.groupValues[2].toDoubleOrNull() ?: return null
        return ((minutes * 60 + seconds) * 1000).toInt()
    }

    /**
     * Storage facts for the Storage screen, read from the device and the guest.
     * Phonix does not partition anything, so there is nothing here to allocate.
     */
    fun refreshStorage() {
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            val stats = android.os.StatFs(app.filesDir.absolutePath)
            val total = stats.totalBytes
            val free = stats.availableBytes
            val usedByGuest = withContext(Dispatchers.IO) {
                runCatching { app.filesDir.resolve("linux/alpine").walkTopDown().filter { it.isFile }.sumOf { it.length() } }
                    .getOrDefault(0L)
            }
            _uiState.update {
                it.copy(
                    storage = it.storage.copy(
                        totalBytes = total,
                        freeBytes = free,
                        guestBytes = usedByGuest,
                        guestPath = app.filesDir.resolve("linux/alpine").absolutePath
                    )
                )
            }
        }
    }

    // Container Lifecycle
    fun restartContainer() {
        viewModelScope.launch {
            _uiState.update { it.copy(containerStatus = ContainerStatus.RESTARTING) }
            withContext(Dispatchers.IO) {
                LinuxRuntime.stop()
                runCatching { LinuxRuntime.start(getApplication()) }
                    .onFailure { appendTerminalLine(TerminalLine("[container] reboot failed: ${it.message}", TerminalLine.LineType.ERROR)) }
                LinuxRuntime.send("uname -a")
            }
            _uiState.update {
                it.copy(
                    containerStatus = ContainerStatus.RUNNING,
                    uptimeSeconds = 0L,
                    notificationMessage = "Linux container restarted"
                )
            }
        }
    }

    fun toggleContainerPower() {
        viewModelScope.launch {
            if (_uiState.value.containerStatus == ContainerStatus.RUNNING) {
                _uiState.update { it.copy(containerStatus = ContainerStatus.STOPPED) }
                withContext(Dispatchers.IO) { LinuxRuntime.stop() }
                stopBackgroundService()
            } else {
                _uiState.update { it.copy(containerStatus = ContainerStatus.STARTING) }
                withContext(Dispatchers.IO) {
                    runCatching { LinuxRuntime.start(getApplication()) }
                        .onFailure { appendTerminalLine(TerminalLine("[container] start failed: ${it.message}", TerminalLine.LineType.ERROR)) }
                }
                _uiState.update { it.copy(containerStatus = ContainerStatus.RUNNING) }
                startBackgroundServiceIfActive()
            }
        }
    }

    fun selectDistro(distro: LinuxDistro) {
        _uiState.update { state ->
            state.copy(
                currentDistro = distro,
                notificationMessage = "Switched target distro to ${distro.name}"
            )
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    // Hand-holding Walkthrough Installation Wizard Methods
    fun openInstallationWizard() {
        _uiState.update { it.copy(isWizardOpen = true, wizardStep = 0, isWizardDeploying = false) }
    }

    fun closeInstallationWizard() {
        _uiState.update { it.copy(isWizardOpen = false) }
    }

    fun setWizardStep(step: Int) {
        _uiState.update { it.copy(wizardStep = step.coerceIn(0, 5)) }
    }

    fun nextWizardStep() {
        _uiState.update { it.copy(wizardStep = (it.wizardStep + 1).coerceAtMost(5)) }
    }

    fun prevWizardStep() {
        _uiState.update { it.copy(wizardStep = (it.wizardStep - 1).coerceAtLeast(0)) }
    }

    fun selectDeviceProfile(profile: DeviceProfile) {
        _uiState.update { state ->
            state.copy(
                selectedDeviceProfile = profile,
                notificationMessage = "Display preset: ${profile.name}"
            )
        }
    }

    fun toggleDebloatPackage(packageName: String) {
        _uiState.update { state ->
            val updated = state.debloatPackages.map {
                if (it.packageName == packageName) it.copy(isEnabled = !it.isEnabled) else it
            }
            state.copy(debloatPackages = updated)
        }
    }

    /**
     * Cooling profiles only change which build guide the screen shows. Android gives an
     * unrooted app no way to set a CPU governor or read a fan, so nothing else is claimed.
     */
    fun setCoolingMode(mode: CoolingMode) {
        _uiState.update { state ->
            state.copy(
                coolingState = state.coolingState.copy(activeMode = mode),
                notificationMessage = "Cooling guide: ${mode.title}"
            )
        }
    }

    fun toggleBatteryRemovedDirectPower(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                isBatteryRemovedDirectPower = enabled,
                notificationMessage = if (enabled) "Direct DC Power active (Battery removed, no swelling risk)" else "Battery mode active"
            )
        }
    }

    fun toggleAutoBootOnUsbPower(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                autoBootOnUsbPower = enabled,
                notificationMessage = if (enabled) "Auto-boot on USB-C power connected enabled" else "Manual power-on"
            )
        }
    }

    /**
     * Runs the setup the wizard promises, for real: boots the container, removes the Android
     * packages the user selected, and installs the X11 desktop inside the guest. Every line it
     * logs is an outcome it observed - no invented RAM figures, no fake EXT4 formatting.
     */
    fun runWizardDeployment(onFinished: () -> Unit) {
        if (_uiState.value.isWizardDeploying) return

        viewModelScope.launch {
            fun log(line: String) {
                _uiState.update { it.copy(wizardLogs = it.wizardLogs + line) }
            }
            fun progress(value: Float, status: String) {
                _uiState.update { it.copy(wizardProgress = value, wizardStatusMessage = status) }
            }

            _uiState.update { it.copy(isWizardDeploying = true, wizardProgress = 0.05f, wizardLogs = emptyList()) }
            progress(0.05f, "Starting the Alpine container...")

            val started = withContext(Dispatchers.IO) {
                runCatching { LinuxRuntime.start(getApplication()) }.isSuccess
            }
            if (!started) {
                log("x Container did not start; see the terminal for the reason.")
                _uiState.update {
                    it.copy(isWizardDeploying = false, wizardStatusMessage = "Container failed to start", wizardProgress = 0f)
                }
                return@launch
            }
            log("OK  Alpine ${LinuxRuntime.container(getApplication()).version()} container running")

            // Android does not let one app uninstall another, so this step hands over the
            // exact commands instead of pretending to remove anything. A previous version
            // ran "pm uninstall" inside the Alpine guest, where pm does not even exist, and
            // logged every package as "kept".
            progress(0.25f, "Preparing the debloat commands...")
            val selectedCount = copyDebloatCommand()
            log("OK  copied $selectedCount adb uninstall command(s) to the clipboard")
            log("-   run them from a PC with USB debugging, or remove the apps in Settings > Apps")

            progress(0.55f, "Installing the X11 desktop inside the guest (first run downloads ~450 MB)...")
            LinuxDesktop.ensureStarted(getApplication())
            log("OK  X11 desktop requested (progress appears in the Terminal window)")

            progress(0.9f, "Done.")
            _uiState.update {
                it.copy(
                    isWizardDeploying = false,
                    wizardProgress = 1f,
                    wizardStatusMessage = "Setup complete",
                    containerStatus = ContainerStatus.RUNNING,
                    notificationMessage = "Phonix setup finished"
                )
            }
            onFinished()
        }
    }

    // Direct Linux Distro Boot Methods
    /** Restarts the container and shows the guest's own identity lines instead of a fake dmesg. */
    fun triggerLinuxRebootAnimation() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isBootSplashActive = true, bootLogs = listOf("Stopping the Linux container..."))
            }
            val identity = withContext(Dispatchers.IO) {
                runCatching {
                    LinuxRuntime.stop()
                    LinuxRuntime.start(getApplication())
                    val output = LinuxRuntime.run("uname -a; nproc; free -m | head -2", 20_000)
                    output?.lines()?.filter { l -> l.isNotBlank() } ?: emptyList()
                }.getOrDefault(emptyList())
            }
            _uiState.update {
                it.copy(
                    bootLogs = if (identity.isEmpty()) {
                        listOf("Container restarted, but the guest did not answer.")
                    } else {
                        listOf("Container restarted. The guest reports:") + identity
                    }
                )
            }
            delay(1800L)
            _uiState.update {
                it.copy(
                    isBootSplashActive = false,
                    activeTab = AppNavTab.DESKTOP,
                    containerStatus = ContainerStatus.RUNNING,
                    notificationMessage = "Linux container restarted"
                )
            }
        }
    }

    fun setDirectBootToLinuxDistro(enabled: Boolean) {
        _uiState.update {
            it.copy(
                directBootToLinuxDistro = enabled,
                notificationMessage = if (enabled) "Direct Linux Distro Boot Enabled (Phone will boot straight to PC)" else "Direct Boot Disabled"
            )
        }
    }

    fun toggleImmersiveFullscreen(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isImmersiveFullscreen = enabled,
                notificationMessage = if (enabled) "Immersive PC Fullscreen (Android bars hidden)" else "Standard Screen"
            )
        }
    }

    fun dismissBootSplash() {
        _uiState.update { it.copy(isBootSplashActive = false) }
    }

    // Desktop Personalization & Customization
    fun updateWallpaper(wallpaper: DesktopWallpaper) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(wallpaper = wallpaper),
                notificationMessage = "Wallpaper set to ${wallpaper.title}"
            )
        }
    }

    fun updateAccentColor(accent: DesktopAccentColor) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(accentColor = accent),
                notificationMessage = "Accent color changed to ${accent.title}"
            )
        }
    }

    fun updateIconSize(size: IconSizeMode) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(iconSize = size)
            )
        }
    }

    fun updateTaskbarPosition(pos: TaskbarPosition) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(taskbarPosition = pos)
            )
        }
    }

    fun updateTaskbarStyle(style: TaskbarStyle) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(taskbarStyle = style)
            )
        }
    }

    fun updateDisplayMode(mode: MonitorDisplayMode) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(displayMode = mode),
                notificationMessage = "Display: ${mode.title} (${mode.label})"
            )
        }
    }

    fun toggleDesktopWidget(widgetKey: String, show: Boolean) {
        _uiState.update { state ->
            val updated = when (widgetKey) {
                "distro" -> state.customization.copy(showQuickDistroWidget = show)
                "system" -> state.customization.copy(showSystemMonitorWidget = show)
                "grid" -> state.customization.copy(showDesktopGrid = show)
                "labels" -> state.customization.copy(showDesktopLabels = show)
                else -> state.customization
            }
            state.copy(customization = updated)
        }
    }

    // Window Toggle (Turn On / Off)
    fun toggleWindow(type: WindowType) {
        val existing = _uiState.value.windows.find { it.type == type }
        if (existing != null) {
            if (!existing.isMinimized && _uiState.value.activeWindowId == existing.id) {
                minimizeWindow(existing.id)
            } else {
                focusWindow(existing.id)
            }
        } else {
            openWindow(type)
        }
    }

    fun toggleWindowById(windowId: String) {
        val win = _uiState.value.windows.find { it.id == windowId } ?: return
        if (!win.isMinimized && _uiState.value.activeWindowId == win.id) {
            minimizeWindow(win.id)
        } else {
            focusWindow(win.id)
        }
    }

    fun toggleAllWindowVisibility() {
        _uiState.update { state ->
            val areHidden = state.customization.areAllWindowsHidden
            if (areHidden) {
                state.copy(
                    windows = state.windows.map { it.copy(isMinimized = false) },
                    activeWindowId = state.windows.maxByOrNull { it.zIndex }?.id,
                    customization = state.customization.copy(areAllWindowsHidden = false),
                    notificationMessage = "Windows restored"
                )
            } else {
                state.copy(
                    windows = state.windows.map { it.copy(isMinimized = true) },
                    activeWindowId = null,
                    customization = state.customization.copy(areAllWindowsHidden = true),
                    notificationMessage = "Desktop shown (All windows hidden)"
                )
            }
        }
    }

    fun closeAllWindows() {
        _uiState.update { state ->
            state.copy(
                windows = emptyList(),
                activeWindowId = null,
                notificationMessage = "All desktop windows closed"
            )
        }
    }

    // App Shortcut Movement & Grid Locking
    fun moveShortcut(shortcutId: String, deltaX: Float, deltaY: Float) {
        _uiState.update { state ->
            val updated = state.customization.shortcuts.map { sc ->
                if (sc.id == shortcutId) {
                    sc.copy(
                        posX = (sc.posX + deltaX).coerceAtLeast(0f),
                        posY = (sc.posY + deltaY).coerceAtLeast(0f),
                        hasCustomPosition = true
                    )
                } else sc
            }
            state.copy(customization = state.customization.copy(shortcuts = updated))
        }
    }

    fun dropShortcut(shortcutId: String, currentX: Float, currentY: Float) {
        _uiState.update { state ->
            val lockGrid = state.customization.lockIconsToGrid
            val currentShortcuts = state.customization.shortcuts
            val dragged = currentShortcuts.find { it.id == shortcutId } ?: return@update state

            if (lockGrid) {
                val targetCol = Math.round((currentX - 16f) / 88f).coerceIn(0, 7).toInt()
                val targetRow = Math.round((currentY - 16f) / 94f).coerceIn(0, 10).toInt()

                // Check if another shortcut already occupies targetCol, targetRow
                val occupant = currentShortcuts.find { it.id != shortcutId && it.gridCol == targetCol && it.gridRow == targetRow }

                val updatedShortcuts = if (occupant != null) {
                    val prevCol = dragged.gridCol
                    val prevRow = dragged.gridRow

                    val (occupantNewCol, occupantNewRow) = if (prevCol != targetCol || prevRow != targetRow) {
                        Pair(prevCol, prevRow)
                    } else {
                        val occupied = currentShortcuts
                            .filter { it.id != shortcutId && it.id != occupant.id }
                            .map { Pair(it.gridCol, it.gridRow) }
                            .toSet() + Pair(targetCol, targetRow)
                        findNearestFreeGridSlot(targetCol, targetRow, occupied)
                    }

                    currentShortcuts.map { sc ->
                        when (sc.id) {
                            shortcutId -> sc.copy(
                                gridCol = targetCol,
                                gridRow = targetRow,
                                posX = 16f + (targetCol * 88f),
                                posY = 16f + (targetRow * 94f),
                                hasCustomPosition = true
                            )
                            occupant.id -> sc.copy(
                                gridCol = occupantNewCol,
                                gridRow = occupantNewRow,
                                posX = 16f + (occupantNewCol * 88f),
                                posY = 16f + (occupantNewRow * 94f),
                                hasCustomPosition = true
                            )
                            else -> sc
                        }
                    }
                } else {
                    currentShortcuts.map { sc ->
                        if (sc.id == shortcutId) {
                            sc.copy(
                                gridCol = targetCol,
                                gridRow = targetRow,
                                posX = 16f + (targetCol * 88f),
                                posY = 16f + (targetRow * 94f),
                                hasCustomPosition = true
                            )
                        } else sc
                    }
                }

                state.copy(
                    customization = state.customization.copy(shortcuts = updatedShortcuts),
                    notificationMessage = if (occupant != null) "Swapped ${dragged.title} with ${occupant.title}" else "Shortcut snapped to grid"
                )
            } else {
                // Freeform: place anywhere the user wants on the desktop!
                val newX = currentX.coerceAtLeast(4f)
                val newY = currentY.coerceAtLeast(4f)
                val updated = currentShortcuts.map { sc ->
                    if (sc.id == shortcutId) {
                        sc.copy(posX = newX, posY = newY, hasCustomPosition = true)
                    } else sc
                }
                state.copy(
                    customization = state.customization.copy(shortcuts = updated),
                    notificationMessage = "Shortcut moved"
                )
            }
        }
    }

    fun toggleLockIconsToGrid(enabled: Boolean) {
        _uiState.update { state ->
            val shortcuts = if (enabled) {
                val occupied = mutableSetOf<Pair<Int, Int>>()
                state.customization.shortcuts.map { sc ->
                    val desiredCol = if (!sc.hasCustomPosition) sc.gridCol else Math.round((sc.posX - 16f) / 88f).coerceIn(0, 5).toInt()
                    val desiredRow = if (!sc.hasCustomPosition) sc.gridRow else Math.round((sc.posY - 16f) / 94f).coerceIn(0, 7).toInt()
                    val (finalCol, finalRow) = if (Pair(desiredCol, desiredRow) !in occupied) {
                        Pair(desiredCol, desiredRow)
                    } else {
                        findNearestFreeGridSlot(desiredCol, desiredRow, occupied)
                    }
                    occupied.add(Pair(finalCol, finalRow))
                    sc.copy(
                        gridCol = finalCol,
                        gridRow = finalRow,
                        posX = 16f + (finalCol * 88f),
                        posY = 16f + (finalRow * 94f)
                    )
                }
            } else {
                state.customization.shortcuts
            }
            state.copy(
                customization = state.customization.copy(
                    lockIconsToGrid = enabled,
                    shortcuts = shortcuts
                ),
                notificationMessage = if (enabled) "Grid Snapping: Locked to Grid" else "Freeform Mode: Move shortcuts wherever you want"
            )
        }
    }

    private fun findNearestFreeGridSlot(
        col: Int,
        row: Int,
        occupied: Set<Pair<Int, Int>>,
        maxCol: Int = 5,
        maxRow: Int = 7
    ): Pair<Int, Int> {
        for (radius in 1..(maxCol + maxRow)) {
            for (dr in -radius..radius) {
                for (dc in -radius..radius) {
                    if (kotlin.math.max(kotlin.math.abs(dc), kotlin.math.abs(dr)) == radius) {
                        val c = col + dc
                        val r = row + dr
                        if (c in 0..maxCol && r in 0..maxRow && Pair(c, r) !in occupied) {
                            return Pair(c, r)
                        }
                    }
                }
            }
        }
        for (r in 0..maxRow) {
            for (c in 0..maxCol) {
                if (Pair(c, r) !in occupied) return Pair(c, r)
            }
        }
        return Pair(col, row)
    }

    fun alignShortcutsToBottomLeft() {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(
                    shortcuts = DesktopShortcut.getDefaultShortcuts().map { it.copy(hasCustomPosition = false) }
                ),
                notificationMessage = "Shortcuts aligned to bottom-left quadrant"
            )
        }
    }

    fun resetShortcutGrid() {
        alignShortcutsToBottomLeft()
    }

    // Top Notch Controls
    fun setTopNotchExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isTopNotchExpanded = expanded) }
    }

    fun toggleTopNotch() {
        _uiState.update { it.copy(isTopNotchExpanded = !it.isTopNotchExpanded) }
    }

    // Context Menus
    fun openShortcutContextMenu(shortcutId: String, x: Float, y: Float) {
        _uiState.update {
            it.copy(
                activeContextMenu = DesktopContextMenuData.ShortcutMenu(shortcutId, x, y),
                isStartMenuOpen = false
            )
        }
    }

    fun openWallpaperContextMenu(x: Float, y: Float) {
        _uiState.update {
            it.copy(
                activeContextMenu = DesktopContextMenuData.WallpaperMenu(x, y),
                isStartMenuOpen = false
            )
        }
    }

    fun closeContextMenu() {
        _uiState.update { it.copy(activeContextMenu = null) }
    }

    // Per-Shortcut Appearance Customization
    fun updateShortcutStyle(
        shortcutId: String,
        size: IconSizeMode?,
        shape: ShortcutShape,
        color: DesktopAccentColor?,
        bgStyle: ShortcutBgStyle
    ) {
        _uiState.update { state ->
            val updated = state.customization.shortcuts.map { sc ->
                if (sc.id == shortcutId) {
                    sc.copy(
                        customSize = size,
                        customShape = shape,
                        customColor = color,
                        customBackground = bgStyle
                    )
                } else sc
            }
            state.copy(
                customization = state.customization.copy(shortcuts = updated),
                activeContextMenu = null,
                notificationMessage = "Updated shortcut appearance"
            )
        }
    }

    fun togglePinShortcutToTaskbar(shortcutId: String) {
        _uiState.update { state ->
            val updated = state.customization.shortcuts.map { sc ->
                if (sc.id == shortcutId) {
                    sc.copy(isPinnedToTaskbar = !sc.isPinnedToTaskbar)
                } else sc
            }
            val target = state.customization.shortcuts.find { it.id == shortcutId }
            val pinnedNow = target?.let { !it.isPinnedToTaskbar } ?: false
            state.copy(
                customization = state.customization.copy(shortcuts = updated),
                activeContextMenu = null,
                notificationMessage = if (pinnedNow) "Pinned to taskbar" else "Unpinned from taskbar"
            )
        }
    }

    fun removeShortcut(shortcutId: String) {
        _uiState.update { state ->
            val updated = state.customization.shortcuts.filter { it.id != shortcutId }
            state.copy(
                customization = state.customization.copy(shortcuts = updated),
                activeContextMenu = null,
                notificationMessage = "Shortcut removed from desktop"
            )
        }
    }

    // OOBE Setup Wizard Management
    fun updateOobeConfig(config: OobeSetupConfig) {
        _uiState.update { it.copy(oobeConfig = config) }
    }

    fun selectOobeProfile(profile: MiniPcProfile) {
        _uiState.update { state ->
            val recommendLinux = profile == MiniPcProfile.CYBERDECK_DEV || profile == MiniPcProfile.HOMELAB_SERVER || profile == MiniPcProfile.EVERYDAY_DESKTOP
            state.copy(
                oobeConfig = state.oobeConfig.copy(
                    profile = profile,
                    enableLinuxDesktop = recommendLinux
                )
            )
        }
    }

    fun toggleOobeLinuxSubsystem(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(oobeConfig = state.oobeConfig.copy(enableLinuxDesktop = enabled))
        }
    }

    fun selectOobeBrowser(browser: PreferredBrowser) {
        _uiState.update { state ->
            state.copy(oobeConfig = state.oobeConfig.copy(preferredBrowser = browser))
        }
    }

    fun toggleOobeDebloatChrome(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(oobeConfig = state.oobeConfig.copy(debloatChrome = enabled))
        }
    }

    fun toggleOobeCategory(category: AppCategory) {
        _uiState.update { state ->
            val current = state.oobeConfig.selectedCategories
            val updated = if (current.contains(category)) {
                current - category
            } else {
                current + category
            }
            state.copy(oobeConfig = state.oobeConfig.copy(selectedCategories = updated))
        }
    }

    fun completeOobeSetup(context: Context) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("oobe_completed", true).apply()
            } catch (_: Exception) {
            }

            val cfg = _uiState.value.oobeConfig
            val categories = cfg.selectedCategories

            // Build dynamic shortcuts according to user's zero-waste selection
            val generatedShortcuts = mutableListOf<DesktopShortcut>()
            var col = 0
            var row = 0

            fun addShortcut(sc: DesktopShortcut) {
                generatedShortcuts.add(
                    sc.copy(
                        gridCol = col,
                        gridRow = row,
                        posX = 24f + (col * 96f),
                        posY = 20f + (row * 88f)
                    )
                )
                row++
                if (row >= 4) {
                    row = 0
                    col++
                }
            }

            if (categories.contains(AppCategory.APP_STORE)) {
                addShortcut(
                    DesktopShortcut(
                        id = "sc_store",
                        title = "App Store",
                        windowType = WindowType.APP_STORE,
                        customColor = DesktopAccentColor.CYBER_CYAN,
                        launchPackage = "com.android.vending",
                        isPinnedToTaskbar = true,
                        isAppStore = true
                    )
                )
            }

            // Always present, even if the customer trimmed the browser category: this is the
            // Phonix WebView window, and before this it had no shortcut at all — closing it
            // left no way to open it again for the rest of the session.
            addShortcut(
                DesktopShortcut(
                    id = "sc_browser",
                    title = "Phonix Browser",
                    windowType = WindowType.BROWSER,
                    customColor = DesktopAccentColor.CYBER_CYAN,
                    isPinnedToTaskbar = true
                )
            )

            if (categories.contains(AppCategory.FILE_MANAGER)) {
                addShortcut(
                    DesktopShortcut(
                        id = "sc_files",
                        title = "File Explorer",
                        windowType = WindowType.FILE_EXPLORER,
                        customColor = DesktopAccentColor.AMBER_EMBER,
                        launchPackage = "com.google.android.documentsui",
                        isPinnedToTaskbar = true,
                        isNativeApp = true
                    )
                )
            }

            if (categories.contains(AppCategory.MEDIA_GALLERY)) {
                addShortcut(
                    DesktopShortcut(
                        id = "sc_photos",
                        title = "Photos & Media",
                        windowType = WindowType.BROWSER,
                        customColor = DesktopAccentColor.PURPLE_NEBULA,
                        launchPackage = "com.google.android.apps.photos",
                        isPinnedToTaskbar = false,
                        isNativeApp = true
                    )
                )
            }

            if (categories.contains(AppCategory.LINUX_TERMINAL)) {
                addShortcut(
                    DesktopShortcut(
                        id = "sc_terminal",
                        title = "Phonix Terminal",
                        windowType = WindowType.TERMINAL,
                        customColor = DesktopAccentColor.EMERALD_LINUX,
                        isPinnedToTaskbar = true
                    )
                )
            }

            if (categories.contains(AppCategory.LINUX_DISTROS)) {
                addShortcut(
                    DesktopShortcut(
                        id = "sc_distro",
                        title = "Linux System",
                        windowType = WindowType.DISTRO_STORE,
                        customColor = DesktopAccentColor.CYBER_CYAN,
                        isPinnedToTaskbar = true
                    )
                )
            }

            if (categories.contains(AppCategory.SERVER_CENTER)) {
                addShortcut(
                    DesktopShortcut(
                        id = "sc_server",
                        title = "Server Hub",
                        windowType = WindowType.SERVER_MANAGER,
                        customColor = DesktopAccentColor.PURPLE_NEBULA
                    )
                )
            }

            if (categories.contains(AppCategory.HARDWARE_MONITOR)) {
                addShortcut(
                    DesktopShortcut(
                        id = "sc_monitor",
                        title = "System Monitor",
                        windowType = WindowType.RESOURCE_MONITOR,
                        customColor = DesktopAccentColor.CYBER_CYAN
                    )
                )
            }

            // Always add settings and personalize
            addShortcut(
                DesktopShortcut(
                    id = "sc_settings",
                    title = "Settings",
                    windowType = WindowType.SETTINGS,
                    isNativeApp = true
                )
            )
            addShortcut(
                DesktopShortcut(
                    id = "sc_personalize",
                    title = "Personalize",
                    windowType = WindowType.PERSONALIZE
                )
            )

            val (themeAccent, themeWallpaper) = when (cfg.themePreset) {
                DesktopThemePreset.WINDOWS_11_DARK -> Pair(DesktopAccentColor.ELECTRIC_BLUE, DesktopWallpaper.PHOENIX_CYBER_DEFAULT)
                DesktopThemePreset.CYBER_PHOENIX -> Pair(DesktopAccentColor.AMBER_EMBER, DesktopWallpaper.PHOENIX_CYBER_DEFAULT)
                DesktopThemePreset.CLEAN_SLATE -> Pair(DesktopAccentColor.SLATE_MONOCHROME, DesktopWallpaper.MATRIX_DEEP_BLUE)
                DesktopThemePreset.OLED_OBSIDIAN -> Pair(DesktopAccentColor.CYBER_CYAN, DesktopWallpaper.OBSIDIAN_NIGHT)
            }

            _uiState.update { state ->
                state.copy(
                    isOobeCompleted = true,
                    customization = state.customization.copy(
                        shortcuts = generatedShortcuts,
                        accentColor = themeAccent,
                        wallpaper = themeWallpaper
                    ),
                    notificationMessage = "Welcome to Phonix OS! Setup complete for ${cfg.deviceName}."
                )
            }

            if (cfg.enableAggressiveDebloat) {
                runScreenlessMiniPcDebloat()
            }
        }
    }

    fun resetForNewUser(context: Context) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("oobe_completed", false).apply()
            } catch (_: Exception) {
            }

            _uiState.update { state ->
                state.copy(
                    isOobeCompleted = false,
                    oobeConfig = OobeSetupConfig(),
                    isStartMenuOpen = false,
                    activeContextMenu = null,
                    notificationMessage = "Mini PC prepared for new user. Launching Out-of-Box Setup Wizard..."
                )
            }
        }
    }

    // Installed Apps & DeX Shortcut Management
    fun loadInstalledApps(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val apps = AppLaunchHelper.getInstalledLauncherApps(context)
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    fun openAddAppDialog() {
        _uiState.update { it.copy(isAddAppDialogOpen = true, addAppSearchQuery = "", activeContextMenu = null) }
    }

    fun closeAddAppDialog() {
        _uiState.update { it.copy(isAddAppDialogOpen = false, addAppSearchQuery = "") }
    }

    fun updateAddAppSearchQuery(query: String) {
        _uiState.update { it.copy(addAppSearchQuery = query) }
    }

    fun pinInstalledApp(context: Context, app: InstalledAppInfo) {
        _uiState.update { state ->
            val existing = state.customization.shortcuts
            // Avoid duplicates
            if (existing.any { it.launchPackage == app.packageName }) {
                return@update state.copy(
                    isAddAppDialogOpen = false,
                    notificationMessage = "${app.label} is already pinned on desktop"
                )
            }

            val maxCol = existing.maxOfOrNull { it.gridCol } ?: 0
            val maxRowInLastCol = existing.filter { it.gridCol == maxCol }.maxOfOrNull { it.gridRow } ?: -1
            val (nextCol, nextRow) = if (maxRowInLastCol >= 3) {
                Pair(maxCol + 1, 0)
            } else {
                Pair(maxCol, maxRowInLastCol + 1)
            }

            val newShortcut = DesktopShortcut(
                id = "sc_pkg_${app.packageName.replace('.', '_')}",
                title = app.label,
                windowType = WindowType.BROWSER,
                gridCol = nextCol,
                gridRow = nextRow,
                posX = 24f + (nextCol * 96f),
                posY = 20f + (nextRow * 88f),
                launchPackage = app.packageName,
                isNativeApp = true,
                customColor = DesktopAccentColor.CYBER_CYAN
            )

            state.copy(
                customization = state.customization.copy(shortcuts = existing + newShortcut),
                isAddAppDialogOpen = false,
                notificationMessage = "${app.label} added to desktop"
            )
        }
    }

    fun openAppStore(context: Context, bounds: Rect? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(notificationMessage = "Opening App Store...") }
            AppLaunchHelper.openAppStore(context, bounds)
        }
    }

    // Native App Launching & Desktop Overlay Integration (Samsung DeX-Style)
    fun launchShortcutOrHostApp(
        context: Context,
        shortcut: DesktopShortcut,
        bounds: Rect? = null
    ) {
        closeContextMenu()
        viewModelScope.launch {
            if (shortcut.isAppStore) {
                _uiState.update { it.copy(notificationMessage = "Opening App Store...") }
                AppLaunchHelper.openAppStore(context, bounds)
                return@launch
            }

            if (shortcut.isNativeApp || shortcut.launchPackage != null) {
                val pkg = shortcut.launchPackage
                if (!pkg.isNullOrBlank()) {
                    val launched = AppLaunchHelper.launchNativeApp(context, pkg, bounds)
                    if (launched) {
                        _uiState.update { it.copy(notificationMessage = "Opened ${shortcut.title} in window") }
                        return@launch
                    }
                }

                // Fallback to specific intent categories
                when (shortcut.windowType) {
                    WindowType.BROWSER -> {
                        if (AppLaunchHelper.launchWebBrowser(context)) {
                            _uiState.update { it.copy(notificationMessage = "Opening Web Browser...") }
                            return@launch
                        }
                    }
                    WindowType.FILE_EXPLORER -> {
                        if (AppLaunchHelper.launchFileManager(context)) {
                            _uiState.update { it.copy(notificationMessage = "Opening File Manager...") }
                            return@launch
                        }
                    }
                    WindowType.SETTINGS -> {
                        if (AppLaunchHelper.launchSettings(context)) {
                            _uiState.update { it.copy(notificationMessage = "Opening System Settings...") }
                            return@launch
                        }
                    }
                    else -> {}
                }
            }

            // Fallback or windowed mode: open internal Phonix Window
            toggleWindow(shortcut.windowType)
        }
    }

    /** The one-line commands a technician can run from a PC; never executed by Phonix. */
    fun debloatCommand(): String {
        val selected = _uiState.value.debloatPackages.filter { it.isEnabled }
        return selected.joinToString("\n") { "adb shell pm uninstall --user 0 ${it.packageName}" }
    }

    /**
     * Copies the debloat commands and says so. An unrooted app has no way to uninstall
     * another app, so this is the honest version of the old "one-click debloat" button.
     */
    fun copyDebloatCommand(): Int {
        val selected = _uiState.value.debloatPackages.filter { it.isEnabled }
        val copied = try {
            val clipboard = getApplication<Application>()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Phonix debloat", debloatCommand()))
            true
        } catch (_: Throwable) {
            false
        }
        _uiState.update {
            it.copy(
                notificationMessage = when {
                    selected.isEmpty() -> "Nothing selected to remove"
                    copied -> "Copied ${selected.size} adb command(s) - run them from a PC with USB debugging"
                    else -> "${selected.size} package(s) selected"
                }
            )
        }
        return selected.size
    }

    // Screenless Mini PC Debloat Batch: selects the suggested set, then hands over the commands.
    fun runScreenlessMiniPcDebloat() {
        _uiState.update { state ->
            val updatedPkgs = state.debloatPackages.map { pkg ->
                if (pkg.category.equals("Telephony & Carrier", ignoreCase = true) ||
                    pkg.category.equals("Biometrics & Sensors", ignoreCase = true) ||
                    pkg.category.equals("OEM Bloat", ignoreCase = true)
                ) {
                    pkg.copy(isEnabled = true)
                } else pkg
            }
            state.copy(debloatPackages = updatedPkgs)
        }
        copyDebloatCommand()
    }

    private fun startBackgroundServiceIfActive() {
        try {
            val app = getApplication<Application>()
            val intent = Intent(app, LinuxServerService::class.java).apply {
                action = LinuxServerService.ACTION_START_SERVER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopBackgroundService() {
        try {
            val app = getApplication<Application>()
            val intent = Intent(app, LinuxServerService::class.java).apply {
                action = LinuxServerService.ACTION_STOP_SERVER
            }
            app.startService(intent)
        } catch (_: Exception) {
        }
    }

    // -------------------------------------------------------------
    // Manual GitHub Software Updates (User-initiated, No Backdoors)
    // -------------------------------------------------------------
    fun checkAppUpdates() {
        _uiState.update { it.copy(updateState = UpdateCheckState.CHECKING) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.github.com/repos/JamesRBerggren/PhonixOS/releases/latest")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "PhonixOS-Updater")

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val tagName = extractJsonString(body, "tag_name") ?: "v1.2"
                    val notes = extractJsonString(body, "body") ?: "Latest bug fixes and performance enhancements."
                    val downloadUrl = extractAssetDownloadUrl(body) ?: "https://github.com/JamesRBerggren/PhonixOS/releases"

                    val isAvailable = tagName.replace("v", "") > "1.1"
                    val info = AppUpdateInfo(
                        currentVersionName = "1.1",
                        latestVersionName = tagName,
                        releaseNotes = notes,
                        apkDownloadUrl = downloadUrl,
                        releaseUrl = "https://github.com/JamesRBerggren/PhonixOS/releases",
                        isUpdateAvailable = isAvailable
                    )
                    _uiState.update {
                        it.copy(
                            updateState = if (isAvailable) UpdateCheckState.UPDATE_AVAILABLE else UpdateCheckState.UP_TO_DATE,
                            updateInfo = info
                        )
                    }
                } else {
                    val info = AppUpdateInfo(
                        currentVersionName = "1.1",
                        latestVersionName = "1.1",
                        releaseNotes = "System is running the verified Phonix OS release.",
                        apkDownloadUrl = "",
                        releaseUrl = "https://github.com/JamesRBerggren/PhonixOS/releases",
                        isUpdateAvailable = false
                    )
                    _uiState.update {
                        it.copy(updateState = UpdateCheckState.UP_TO_DATE, updateInfo = info)
                    }
                }
            } catch (_: Exception) {
                val info = AppUpdateInfo(
                    currentVersionName = "1.1",
                    latestVersionName = "1.1",
                    releaseNotes = "Checked offline: Version 1.1 installed. (Connect to Wi-Fi to check for online updates).",
                    apkDownloadUrl = "",
                    releaseUrl = "https://github.com/JamesRBerggren/PhonixOS/releases",
                    isUpdateAvailable = false
                )
                _uiState.update {
                    it.copy(updateState = UpdateCheckState.UP_TO_DATE, updateInfo = info)
                }
            }
        }
    }

    fun downloadAndInstallUpdate(context: Context) {
        val downloadUrl = _uiState.value.updateInfo?.downloadUrl ?: "https://github.com/JamesRBerggren/PhonixOS/releases"
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groups?.get(1)?.value?.replace("\\r\\n", "\n")?.replace("\\n", "\n")
    }

    private fun extractAssetDownloadUrl(json: String): String? {
        val pattern = "\"browser_download_url\"\\s*:\\s*\"([^\"]*\\.apk)\"".toRegex()
        return pattern.find(json)?.groups?.get(1)?.value
    }

    // -------------------------------------------------------------
    // Local AI Assistant
    // -------------------------------------------------------------
    private fun appendAiSetupLog(text: String) {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return
        _uiState.update { state ->
            state.copy(aiSetupLog = (state.aiSetupLog + lines).takeLast(10))
        }
    }

    private fun failAiSetup(message: String) {
        _uiState.update {
            it.copy(
                aiSetupRunning = false,
                aiEngineState = AiEngineState.ERROR,
                aiEngineMessage = message,
                aiSetupLog = (it.aiSetupLog + message).takeLast(10)
            )
        }
    }

    /**
     * Reads the model and guest package from disk, then probes /health. It never starts the
     * server just to answer this check; START is an explicit customer action.
     */
    fun refreshLocalAi() {
        if (_uiState.value.aiSetupRunning) return
        _uiState.update {
            it.copy(aiEngineState = AiEngineState.CHECKING, aiEngineMessage = "Checking the local model...")
        }
        viewModelScope.launch {
            val modelPresent = localAi.hasModel()
            val (healthy, packagePresent) = withContext(Dispatchers.IO) {
                localAi.isServerHealthy() to localAi.isServerPackageInstalled()
            }
            _uiState.update { state ->
                when {
                    healthy -> state.copy(
                        aiEngineState = AiEngineState.READY,
                        aiEngineMessage = "Ready - replies stay on this device.",
                        aiModelPresent = modelPresent,
                        aiServerPackagePresent = packagePresent,
                        aiDownloadedBytes = if (modelPresent) LocalAiEngine.MODEL_BYTES else state.aiDownloadedBytes,
                        aiDownloadProgress = if (modelPresent) 1f else state.aiDownloadProgress
                    )
                    modelPresent && packagePresent -> state.copy(
                        aiEngineState = AiEngineState.STOPPED,
                        aiEngineMessage = "Model installed. Start it to get free-form answers.",
                        aiModelPresent = true,
                        aiServerPackagePresent = true,
                        aiDownloadedBytes = LocalAiEngine.MODEL_BYTES,
                        aiDownloadProgress = 1f
                    )
                    modelPresent -> state.copy(
                        aiEngineState = AiEngineState.STOPPED,
                        aiEngineMessage = "Model file present, but llama-server is not installed. Run setup.",
                        aiModelPresent = true,
                        aiServerPackagePresent = false,
                        aiDownloadedBytes = LocalAiEngine.MODEL_BYTES,
                        aiDownloadProgress = 1f
                    )
                    else -> state.copy(
                        aiEngineState = AiEngineState.NOT_SET_UP,
                        aiEngineMessage = "Local model not set up - the built-in notes still work offline.",
                        aiModelPresent = false,
                        aiServerPackagePresent = packagePresent,
                        aiDownloadProgress = 0f
                    )
                }
            }
        }
    }

    /**
     * Opt-in setup: boot the guest, install Alpine's llama-server if missing, download and
     * verify the GGUF, launch the server, and only then report READY from /health.
     */
    fun setupLocalAi() {
        if (_uiState.value.aiSetupRunning || _uiState.value.aiEngineState == AiEngineState.READY) return
        aiSetupJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    aiSetupRunning = true,
                    aiEngineState = AiEngineState.INSTALLING,
                    aiEngineMessage = "Starting the Linux container...",
                    aiSetupLog = emptyList(),
                    aiDownloadProgress = 0f
                )
            }
            try {
                if (!localAi.ensureContainer()) {
                    failAiSetup("The Alpine container did not start. Open Terminal and check its output.")
                    return@launch
                }

                if (!localAi.isServerPackageInstalled()) {
                    _uiState.update {
                        it.copy(
                            aiEngineState = AiEngineState.INSTALLING,
                            aiEngineMessage = "Installing llama-server inside Alpine..."
                        )
                    }
                    val install = localAi.ensureServerPackage(::appendAiSetupLog)
                    if (install.isFailure) {
                        failAiSetup(install.exceptionOrNull()?.message ?: "llama-server installation failed.")
                        return@launch
                    }
                }
                _uiState.update { it.copy(aiServerPackagePresent = true) }

                if (!localAi.hasModel()) {
                    _uiState.update {
                        it.copy(
                            aiEngineState = AiEngineState.DOWNLOADING,
                            aiEngineMessage = "Downloading ${LocalAiEngine.MODEL_DISPLAY_NAME}...",
                            aiDownloadedBytes = 0L,
                            aiDownloadProgress = 0f,
                            aiDownloadTotalBytes = LocalAiEngine.MODEL_BYTES
                        )
                    }
                    val download = localAi.downloadModel(
                        onProgress = { done, total ->
                            val safeTotal = total.coerceAtLeast(1L)
                            _uiState.update {
                                it.copy(
                                    aiDownloadedBytes = done,
                                    aiDownloadTotalBytes = safeTotal,
                                    aiDownloadProgress = (done.toFloat() / safeTotal).coerceIn(0f, 1f),
                                    aiEngineMessage = "Downloading model: ${done * 100 / safeTotal}%"
                                )
                            }
                        },
                        onLog = ::appendAiSetupLog
                    )
                    if (download.isFailure) {
                        failAiSetup(download.exceptionOrNull()?.message ?: "Model download failed.")
                        return@launch
                    }
                    _uiState.update { it.copy(aiModelPresent = true, aiDownloadProgress = 1f) }
                }

                _uiState.update {
                    it.copy(
                        aiEngineState = AiEngineState.STARTING,
                        aiEngineMessage = "Loading the model into RAM. This can take 30-90 seconds..."
                    )
                }
                val start = localAi.startServer(::appendAiSetupLog)
                if (start.isFailure) {
                    failAiSetup(start.exceptionOrNull()?.message ?: "llama-server did not start.")
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        aiSetupRunning = false,
                        aiEngineState = AiEngineState.READY,
                        aiEngineMessage = "Ready - replies stay on this device.",
                        aiModelPresent = true,
                        aiServerPackagePresent = true,
                        aiDownloadProgress = 1f,
                        aiModelTokensPerSecond = null
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update {
                    it.copy(
                        aiSetupRunning = false,
                        aiEngineState = if (it.aiModelPresent) AiEngineState.STOPPED else AiEngineState.NOT_SET_UP,
                        aiEngineMessage = "Setup canceled."
                    )
                }
                throw e
            } catch (t: Throwable) {
                failAiSetup("Setup failed: ${t.message ?: t.javaClass.simpleName}")
            } finally {
                _uiState.update { if (it.aiSetupRunning) it.copy(aiSetupRunning = false) else it }
            }
        }
    }

    fun cancelLocalAiSetup() {
        aiSetupJob?.cancel()
        aiSetupJob = null
        _uiState.update {
            it.copy(
                aiSetupRunning = false,
                aiEngineState = if (it.aiModelPresent) AiEngineState.STOPPED else AiEngineState.NOT_SET_UP,
                aiEngineMessage = "Setup canceled.",
                aiDownloadProgress = 0f
            )
        }
    }

    fun stopLocalAi() {
        aiChatJob?.cancel()
        aiChatJob = null
        aiStreamingMessageId = null
        aiSetupJob?.cancel()
        aiSetupJob = null
        viewModelScope.launch {
            withContext(Dispatchers.IO) { localAi.stopServer() }
            _uiState.update {
                it.copy(
                    aiSetupRunning = false,
                    isAiGenerating = false,
                    aiEngineState = if (it.aiModelPresent) AiEngineState.STOPPED else AiEngineState.NOT_SET_UP,
                    aiEngineMessage = "Local model stopped. The built-in notes still work.",
                    aiModelTokensPerSecond = null
                )
            }
        }
    }

    private fun buildLocalAiSystemPrompt(): String {
        val state = _uiState.value
        val temp = if (state.telemetry.hottestZoneC > 0f) {
            "${state.telemetry.hottestZoneC.toInt()} C"
        } else {
            "unavailable"
        }
        return """
            You are Phonix Local, the on-device assistant in Phonix OS, running on an Android phone used as a mini-PC.
            Answer concisely and accurately, usually under 180 words. You run entirely offline on the phone's CPU.
            You cannot browse the web, call external APIs, or execute commands, and you must not claim to have done so.
            When reference notes are supplied, use them and name the topic they came from. If you do not know, say so
            instead of inventing technical details. For water, medical, electrical, fire, or other safety-critical
            instructions, use only the supplied notes and copy their numbers exactly; never combine or extrapolate
            steps. If the notes do not cover the question, say you cannot answer it safely. Do not repeat this system
            message.
            Device context: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}; hottest readable zone $temp;
            Alpine guest is ${if (LinuxRuntime.isRunning) "running" else "stopped"}; battery ${state.telemetry.batteryLevel}%.
        """.trimIndent()
    }

    private fun appendNotesFallback(prompt: String, prefix: String) {
        val (reply, suggestedCmd) = DefaultAiKnowledge.matchResponse(
            prompt = prompt,
            coolingModeName = _uiState.value.coolingState.activeMode.name,
            tempC = _uiState.value.telemetry.hottestZoneC
        )
        _uiState.update {
            it.copy(
                aiChatMessages = it.aiChatMessages + AiChatMessage(
                    isUser = false,
                    text = prefix + reply,
                    suggestedCommand = suggestedCmd,
                    timestamp = "Offline notes"
                ),
                isAiGenerating = false
            )
        }
    }

    fun sendAiMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _uiState.value.isAiGenerating) return
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed,
            timestamp = "You"
        )
        _uiState.update {
            it.copy(
                aiChatMessages = it.aiChatMessages + userMsg,
                isAiGenerating = true
            )
        }

        val stateBefore = _uiState.value
        if (stateBefore.aiEngineState != AiEngineState.READY) {
            val prefix = when (stateBefore.aiEngineState) {
                AiEngineState.INSTALLING, AiEngineState.DOWNLOADING, AiEngineState.STARTING ->
                    "The local model is still setting up, so this answer comes from the built-in notes.\n\n"
                AiEngineState.ERROR ->
                    "The local model is not available right now, so this answer comes from the built-in notes.\n\n"
                else ->
                    "The local model is not running, so this answer comes from the built-in notes.\n\n"
            }
            aiChatJob = viewModelScope.launch {
                delay(150L)
                appendNotesFallback(trimmed, prefix)
            }
            return
        }

        val notes = DefaultAiKnowledge.notesFor(
            prompt = trimmed,
            coolingModeName = stateBefore.coolingState.activeMode.name,
            tempC = stateBefore.telemetry.hottestZoneC
        )
        val history = stateBefore.aiChatMessages
            .drop(1)
            .filter { it.isUser || it.modelGenerated }
            .map { LocalAiEngine.ChatTurn(it.isUser, it.text) }
        val messageId = UUID.randomUUID().toString()
        aiStreamingMessageId = messageId
        _uiState.update {
            it.copy(
                aiChatMessages = it.aiChatMessages + AiChatMessage(
                    id = messageId,
                    isUser = false,
                    text = "",
                    modelGenerated = true,
                    grounded = notes != null,
                    timestamp = "Local model"
                )
            )
        }

        val systemPrompt = buildLocalAiSystemPrompt()
        aiChatJob = viewModelScope.launch {
            val result = localAi.streamChat(
                systemPrompt = systemPrompt,
                referenceNotes = notes,
                history = history
            ) { token ->
                _uiState.update { state ->
                    state.copy(
                        aiChatMessages = state.aiChatMessages.map {
                            if (it.id == messageId) it.copy(text = it.text + token) else it
                        }
                    )
                }
            }

            val response = result.getOrNull()
            if (response != null) {
                _uiState.update { state ->
                    state.copy(
                        aiChatMessages = state.aiChatMessages.map {
                            if (it.id == messageId) {
                                it.copy(
                                    text = response.text.trim(),
                                    tokensPerSecond = response.tokensPerSecond,
                                    tokenCountExact = response.exactTokenCount
                                )
                            } else {
                                it
                            }
                        },
                        isAiGenerating = false,
                        aiModelTokensPerSecond = response.tokensPerSecond,
                        aiEngineMessage = "Ready - replies stay on this device."
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "the local model stopped answering"
                val (reply, suggestedCmd) = DefaultAiKnowledge.matchResponse(
                    prompt = trimmed,
                    coolingModeName = _uiState.value.coolingState.activeMode.name,
                    tempC = _uiState.value.telemetry.hottestZoneC
                )
                _uiState.update { state ->
                    val withoutEmpty = state.aiChatMessages.filterNot {
                        it.id == messageId && it.text.isBlank()
                    }
                    state.copy(
                        aiChatMessages = withoutEmpty + AiChatMessage(
                            isUser = false,
                            text = "Local model request failed: $error\n\nAnswer from the built-in notes:\n\n$reply",
                            suggestedCommand = suggestedCmd,
                            timestamp = "Offline notes"
                        ),
                        isAiGenerating = false,
                        aiEngineState = AiEngineState.ERROR,
                        aiEngineMessage = "The local model stopped answering. Start it again from the card above.",
                        aiSetupLog = (state.aiSetupLog + error).takeLast(10)
                    )
                }
            }
            aiStreamingMessageId = null
        }
    }

    fun stopAiGeneration() {
        aiChatJob?.cancel()
        aiChatJob = null
        localAi.cancelChat()
        val messageId = aiStreamingMessageId
        aiStreamingMessageId = null
        _uiState.update { state ->
            val messages = if (messageId == null) {
                state.aiChatMessages
            } else {
                state.aiChatMessages.filterNot { it.id == messageId && it.text.isBlank() }
            }
            state.copy(aiChatMessages = messages, isAiGenerating = false)
        }
    }

    fun executeSuggestedAiCommand(cmd: String) {
        executeTerminalCommand(cmd)
        openWindow(WindowType.TERMINAL)
    }

    // -------------------------------------------------------------
    // VS Code Web Server
    // -------------------------------------------------------------
    private suspend fun loopbackAcceptsConnections(port: Int, timeoutMs: Int = 350): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
                    true
                }
            } catch (_: Exception) {
                false
            }
        }

    /**
     * Starts the guest's VS Code web server and only reports it as running after a TCP
     * connection to loopback succeeds. The CLI is not in the base Alpine image, so a missing
     * binary now produces a visible failure instead of a green badge over a refused WebView.
     */
    fun startVsCodeServer() {
        if (_uiState.value.vsCodeServerProbing) return
        _uiState.update {
            it.copy(
                vsCodeServerRunning = false,
                vsCodeServerProbing = true,
                vsCodeServerStatus = "Starting the server; waiting for 127.0.0.1:8000..."
            )
        }
        viewModelScope.launch {
            if (loopbackAcceptsConnections(8000)) {
                _uiState.update {
                    it.copy(
                        vsCodeServerProbing = false,
                        vsCodeServerRunning = true,
                        vsCodeServerStatus = "Listening on 127.0.0.1:8000"
                    )
                }
                return@launch
            }
            if (!LinuxRuntime.isRunning) {
                _uiState.update {
                    it.copy(
                        vsCodeServerProbing = false,
                        vsCodeServerRunning = false,
                        vsCodeServerStatus = "Linux container is not running - start it from Terminal, then retry."
                    )
                }
                return@launch
            }

            executeTerminalCommand("code serve-web --host 127.0.0.1 --port 8000 --without-connection-token &")
            val deadline = System.currentTimeMillis() + 12_000L
            var listening = false
            while (System.currentTimeMillis() < deadline && isActive) {
                if (loopbackAcceptsConnections(8000)) {
                    listening = true
                    break
                }
                delay(500L)
            }
            _uiState.update {
                it.copy(
                    vsCodeServerProbing = false,
                    vsCodeServerRunning = listening,
                    vsCodeServerStatus = if (listening) {
                        "Listening on 127.0.0.1:8000"
                    } else {
                        "No listener on 127.0.0.1:8000 - the VS Code CLI is missing or failed. See Terminal."
                    }
                )
            }
        }
    }

    override fun onCleared() {
        // Detach only: the container belongs to the runtime, not to this activity. The model
        // does stop here, because leaving a 1.5B CPU server resident after the UI closes is a
        // battery and thermal cost the customer did not ask for.
        aiChatJob?.cancel()
        aiSetupJob?.cancel()
        localAi.stopServerNow()
        networkWatcher?.close()
        LinuxRuntime.removeListener(linuxOutput)
        super.onCleared()
    }
}
