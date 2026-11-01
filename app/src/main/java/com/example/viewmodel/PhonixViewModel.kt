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
import com.example.BuildConfig
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
import com.example.model.DesktopIconGrid
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
import com.example.model.DeviceCapabilityReport
import com.example.model.DeviceChangeChoices
import com.example.model.DeviceFormFactor
import com.example.model.DeviceUseMode
import com.example.model.DisplayRoute
import com.example.model.FoldPosture
import com.example.model.DeviceCompatibilityClassifier
import com.example.model.WindowBounds
import com.example.model.WindowGeometry
import com.example.model.AppUpdateInfo
import com.example.model.UpdateCheckState
import com.example.model.UpdateFeed
import com.example.model.UpdateManifest
import com.example.engine.UpdateInstaller
import com.example.model.AiChatMessage
import com.example.model.AiEngineState
import com.example.model.DefaultAiKnowledge
import com.example.model.AgentStep
import com.example.model.KnowledgeCategory
import com.example.model.KnowledgeLibrary
import com.example.model.PreferredBrowser
import com.example.model.SetupPersona
import com.example.util.AppLaunchHelper
import com.example.util.HostDebloat
import com.example.util.PackageFacts
import com.example.util.DeviceTelemetry
import com.example.util.DeviceSetupPreferences
import com.example.util.DesktopAutoLaunch
import com.example.util.DesktopPrefs
import com.example.util.NetworkOnboarding
import com.example.util.WallpaperStore
import android.graphics.Rect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val containerStatus: ContainerStatus = ContainerStatus.STOPPED,
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
    val deviceChanges: DeviceChangeChoices = DeviceChangeChoices(),
    val deviceCapabilityReport: DeviceCapabilityReport? = null,
    val foldPosture: FoldPosture = FoldPosture.UNKNOWN,
    val oobeConfig: OobeSetupConfig = OobeSetupConfig(),
    /** null until the user answers the welcome question; no silent default persona. */
    val setupPersona: SetupPersona? = null,
    /** Browsers that really answer a PackageManager lookup on this phone, with measured sizes. */
    val installedBrowsers: List<com.example.util.InstalledBrowserInfo> = emptyList(),
    /** package name -> whether this device actually has it, so the trim list stops guessing. */
    val debloatInstalled: Map<String, Boolean> = emptyMap(),
    /** Packages this install has hidden as device owner, so the list can offer an undo. */
    val debloatHidden: Set<String> = emptySet(),
    /** The exact command text copied last, so the UI can confirm the paste really exists. */
    val debloatCopiedCommand: String? = null,
    /** Advice about the ticked packages, and whether a model or the built-in summary wrote it. */
    val debloatAdvice: String? = null,
    val debloatAdviceBusy: Boolean = false,
    val debloatAdviceFromModel: Boolean = false,
    /** True only on a device-owner install, where hiding a package is a real operation. */
    val canRemoveHostPackages: Boolean = false,
    /** Non-null while the rename dialog is open; the id is a shortcut or a taskbar pin. */
    val renameTargetId: String? = null,
    val renameDraft: String = "",
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
    /** null = search every shelf. One shelf keeps a small model's context short and accurate. */
    val aiKnowledgeScope: KnowledgeCategory? = null,
    /** Non-null while the library reader is showing a document. */
    val openKnowledgeDocId: String? = null,
    val showKnowledgeLibrary: Boolean = false,
    // Shell agent: the model proposes one command at a time, the user approves each one.
    val agentTask: String = "",
    val agentSteps: List<AgentStep> = emptyList(),
    val agentPendingCommand: String? = null,
    val agentRunning: Boolean = false,
    val agentStatus: String = "",
    val agentAutoRun: Boolean = false,
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
    /** Null until the desktop reports its first measured size; each size has its own layout. */
    private val windowBounds = MutableStateFlow<WindowBounds?>(null)
    private val localAi = LocalAiEngine(application)
    private var aiSetupJob: Job? = null
    private var aiChatJob: Job? = null
    private var aiStreamingMessageId: String? = null

    init {
        val prefs = application.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
        val oobeDone = prefs.getBoolean("oobe_completed", false)
        val savedChanges = DeviceSetupPreferences.choices(application)
        val setupConfig = OobeSetupConfig(
            deviceChanges = savedChanges,
            enableLinuxDesktop = savedChanges.startLinuxOnLaunch
        )
        // Personalisation is remembered across launches: wallpaper, accent, toggles, icon
        // positions, renames and the taskbar pins.
        val savedDesktop = DesktopPrefs.load(application)
        _uiState.update {
            it.copy(
                isOobeCompleted = oobeDone,
                deviceChanges = savedChanges,
                oobeConfig = setupConfig,
                customization = savedDesktop ?: it.customization
            )
        }
        persistDesktopLayout()
        persistWindowLayout()

        // The persisted guest is the truth: currentDistro defaulted to the first catalog entry
        // (Alpine), so the terminal prompt and boot splash named Alpine after switching to Debian.
        viewModelScope.launch(Dispatchers.IO) {
            val active = runCatching { LinuxRuntime.container(getApplication<Application>()).activeDistro() }.getOrNull()
            if (active != null) _uiState.update { it.copy(currentDistro = active) }
        }

        detectLocalIp()
        initTerminalWelcome()
        startTelemetryLoop()
        loadInstalledApps(application)
        LinuxRuntime.addListener(linuxOutput)
        if (DeviceSetupPreferences.shouldStartLinuxOnLaunch(application)) bootstrapLinux()
        if (DeviceSetupPreferences.mayManageDeviceOwnerNetwork(application)) {
            NetworkOnboarding.allowSsidReadback(application)
        }
        refreshNetwork(allowFirstRunPrompt = true)
        // Every link change (join, drop, Ethernet plugged in) refreshes the header and screen.
        networkWatcher = NetworkOnboarding.watch(application) { refreshNetwork() }
        refreshLinuxApps()
    }

    /**
     * Writes the desktop layout back to disk whenever it changes.
     *
     * Debounced because a drag writes a position on every pointer move; without it the app would
     * serialise the whole layout dozens of times per second while an icon is being dragged.
     */
    private fun persistDesktopLayout() {
        viewModelScope.launch {
            _uiState
                .map { it.customization }
                .distinctUntilChanged()
                // collectLatest cancels the pending write when another change arrives, so a drag
                // that fires sixty positions a second still writes once, when the finger stops.
                .collectLatest { customization ->
                    delay(600L)
                    withContext(Dispatchers.IO) {
                        DesktopPrefs.save(getApplication(), customization)
                    }
                }
        }
    }

    /**
     * Writes the open windows under the current desktop size. Restoring happens once, when the
     * first measured bounds arrive, so a live rotation or dock/undock still normalises the
     * windows the user has now instead of swapping in an old layout.
     */
    private fun persistWindowLayout() {
        viewModelScope.launch {
            combine(_uiState.map { it.windows }.distinctUntilChanged(), windowBounds) { windows, bounds ->
                windows to bounds
            }
                .distinctUntilChanged()
                .collectLatest { (windows, bounds) ->
                    if (bounds == null) return@collectLatest
                    delay(600L)
                    withContext(Dispatchers.IO) {
                        DesktopPrefs.saveWindows(getApplication(), WindowGeometry.layoutKey(bounds), windows)
                    }
                }
        }
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
        if (!DeviceSetupPreferences.mayManageDeviceOwnerNetwork(getApplication())) {
            openAndroidWifiSettings()
            _uiState.update { it.copy(notificationMessage = "Use Android Wi-Fi settings unless device-owner network management is enabled in setup.") }
            return
        }
        if (_uiState.value.networkBusy) return
        _uiState.update { it.copy(networkBusy = true, networkMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            if (DeviceSetupPreferences.mayManageDeviceOwnerNetwork(app)) {
                NetworkOnboarding.allowSsidReadback(app)
            }
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

    /** The guest mounted right now; package commands follow its own manager, not a hardcoded apk. */
    private fun activeGuestDistro(): com.example.model.LinuxDistro =
        runCatching { LinuxRuntime.container(getApplication()).activeDistro() }
            .getOrDefault(com.example.model.DistroCatalog.alpine)

    /** Runs the real install in the guest and reports its own output, including failures. */
    fun installLinuxApp(entry: LinuxAppEntry) {
        if (_uiState.value.linuxAppBusyPackage != null) return
        _uiState.update {
            it.copy(
                linuxAppBusyPackage = entry.packageName,
                linuxAppLog = listOf(activeGuestDistro().installCommand(entry.packageName))
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!LinuxRuntime.isRunning) LinuxRuntime.start(getApplication())
                val output = LinuxRuntime.run(
                    "${activeGuestDistro().installCommand(entry.packageName)} 2>&1",
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
            "${activeGuestDistro().installedQuery(entry.packageName)} && echo $HAVE_PREFIX${entry.packageName}"
        }

    private fun bootstrapLinux() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                LinuxRuntime.start(getApplication())
                // A foreground service is only started when the user opted into keeping
                // Linux alive beyond the visible app session.
                if (DeviceSetupPreferences.shouldKeepLinuxInBackground(getApplication())) {
                    startBackgroundServiceIfActive()
                }
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
        // The banner has to name the guest that is actually installed; it used to say Alpine
        // unconditionally, so a Debian install opened to "Alpine Linux terminal" and an `apk add`
        // hint that the guest cannot run.
        val guest = runCatching { LinuxRuntime.container(getApplication<Application>()).activeDistro() }
            .getOrDefault(DistroCatalog.alpine)
        val sampleInstall = if (guest.isBundled) "apk add nginx" else "${guest.packageManager} install nginx"
        _uiState.update { state ->
            state.copy(
                terminalLines = listOf(
                    TerminalLine(
                        "===========================================================",
                        TerminalLine.LineType.ACCENT
                    ),
                    TerminalLine(
                        " Phonix OS Desktop — ${guest.name} terminal (aarch64)",
                        TerminalLine.LineType.ACCENT
                    ),
                    TerminalLine(
                        " Container: proot userspace, no root required | SoC telemetry: live",
                        TerminalLine.LineType.SYSTEM
                    ),
                    TerminalLine(
                        " Once booted, commands run for real: $sampleInstall, ps, df -h",
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
        val closedType = _uiState.value.windows.firstOrNull { it.id == windowId }?.type
        _uiState.update { state ->
            val updated = state.windows.filter { it.id != windowId }
            state.copy(
                windows = updated,
                activeWindowId = updated.maxByOrNull { it.zIndex }?.id
            )
        }
        // Closing is a reset, minimising is not. A closed window comes back fresh; a minimised
        // one keeps exactly what was in it, the way a desktop app behaves.
        if (closedType == WindowType.AI_ASSISTANT) endAiSession()
    }

    /**
     * Drops the assistant's conversation and library state. Minimising keeps it; closing the
     * window, or leaving the desktop, throws it away, so the next question starts with a clean
     * context instead of yesterday's topic leaking into today's answer.
     */
    private fun endAiSession() {
        aiChatJob?.cancel()
        aiChatJob = null
        aiStreamingMessageId = null
        _uiState.update { state ->
            state.copy(
                aiChatMessages = DefaultAiKnowledge.getInitialMessages(),
                aiKnowledgeScope = null,
                openKnowledgeDocId = null,
                showKnowledgeLibrary = false,
                agentTask = "",
                agentSteps = emptyList(),
                agentPendingCommand = null,
                agentRunning = false,
                agentStatus = "",
                isAiGenerating = false
            )
        }
    }

    /** Public entry point for "New conversation" in the assistant header. */
    fun clearAiConversation() = endAiSession()

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

    fun normalizeWindows(bounds: WindowBounds) {
        val firstBounds = windowBounds.value == null
        val currentWindows = _uiState.value.windows
        // Only the first size is restored. Later bounds changes are live drags/rotations; loading
        // another size's saved windows there would make a window the user just closed reappear.
        val restored = if (firstBounds && currentWindows.isEmpty()) {
            DesktopPrefs.loadWindows(getApplication(), WindowGeometry.layoutKey(bounds))
        } else {
            null
        }
        val baseWindows = restored ?: currentWindows
        _uiState.update { state ->
            state.copy(
                windows = baseWindows.map { if (it.isMaximized) it else WindowGeometry.normalize(it, bounds) },
                activeWindowId = if (restored != null) {
                    baseWindows.filter { !it.isMinimized }.maxByOrNull { it.zIndex }?.id
                } else {
                    state.activeWindowId
                }
            )
        }
        if (restored != null && restored.isNotEmpty()) {
            maxZIndex = maxOf(maxZIndex, restored.maxOf { it.zIndex }) + 1f
        }
        windowBounds.value = bounds
    }

    fun moveWindow(windowId: String, deltaX: Float, deltaY: Float, bounds: WindowBounds? = null) {
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map { win ->
                    if (win.id == windowId && !win.isMaximized) {
                        if (bounds != null) WindowGeometry.move(win, deltaX, deltaY, bounds)
                        else win.copy(posX = win.posX + deltaX, posY = win.posY + deltaY)
                    } else win
                }
            )
        }
    }

    fun resizeWindow(windowId: String, deltaWidth: Float, deltaHeight: Float, bounds: WindowBounds? = null) {
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map { win ->
                    if (win.id == windowId && !win.isMaximized) {
                        if (bounds != null) WindowGeometry.resize(win, deltaWidth, deltaHeight, bounds)
                        else win.copy(
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
            // Shutdown must not toggle: on a stopped container that booted the guest instead.
            stopContainer()
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
                appendTerminalLine(TerminalLine(activeGuestDistro().installCommand(service.apkPackage), TerminalLine.LineType.INPUT))
                _uiState.update { it.copy(notificationMessage = "Installing ${service.apkPackage} in the guest - watch the terminal") }
                LinuxRuntime.send(activeGuestDistro().installCommand(service.apkPackage))
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
                "${activeGuestDistro().installedQuery(s.apkPackage)} && echo pkg:${s.id}=1 || echo pkg:${s.id}=0; " +
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
        if (enabled && !DeviceSetupPreferences.shouldKeepLinuxInBackground(getApplication())) {
            _uiState.update { it.copy(notificationMessage = "Enable background Linux in Device & Display Setup first.") }
            return
        }
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
            // Measure whichever distro is active: this used to hardcode linux/alpine, so after a
            // switch the hub reported "On disk: -" and a path to a directory that no longer exists.
            val (usedByGuest, guestDir) = withContext(Dispatchers.IO) {
                val activeId = runCatching { LinuxRuntime.container(app).activeDistroId() }
                    .getOrDefault("alpine")
                val dir = app.filesDir.resolve("linux/$activeId")
                // walkFileTree does not follow links. A rootfs is full of them (/bin -> usr/bin,
                // /lib -> usr/lib), and walking through them counted whole subtrees twice: Debian
                // reported 815 MB for a 525 MB tree.
                val bytes = runCatching {
                    var total = 0L
                    java.nio.file.Files.walkFileTree(
                        dir.toPath(),
                        object : java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                            override fun visitFile(
                                file: java.nio.file.Path,
                                attrs: java.nio.file.attribute.BasicFileAttributes
                            ): java.nio.file.FileVisitResult {
                                total += attrs.size()
                                return java.nio.file.FileVisitResult.CONTINUE
                            }
                        }
                    )
                    total
                }.getOrDefault(0L)
                bytes to dir
            }
            _uiState.update {
                it.copy(
                    storage = it.storage.copy(
                        totalBytes = total,
                        freeBytes = free,
                        guestBytes = usedByGuest,
                        guestPath = guestDir.absolutePath
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
                val started = withContext(Dispatchers.IO) {
                    runCatching { LinuxRuntime.start(getApplication()) }
                        .onFailure { appendTerminalLine(TerminalLine("[container] start failed: ${it.message}", TerminalLine.LineType.ERROR)) }
                        .isSuccess
                }
                // A start that threw is not a running guest; the status tile must not claim otherwise.
                _uiState.update {
                    it.copy(containerStatus = if (started) ContainerStatus.RUNNING else ContainerStatus.STOPPED)
                }
                if (started) startBackgroundServiceIfActive()
            }
        }
    }

    /** Stops the guest. Idempotent, unlike the power toggle, so `shutdown` can call it safely. */
    fun stopContainer() {
        viewModelScope.launch {
            _uiState.update { it.copy(containerStatus = ContainerStatus.STOPPED) }
            withContext(Dispatchers.IO) { LinuxRuntime.stop() }
            stopBackgroundService()
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
    }    // Hand-holding Walkthrough Installation Wizard Methods
    fun openInstallationWizard() {

        val completed = _uiState.value.isOobeCompleted
        _uiState.update {
            it.copy(isWizardOpen = true, wizardStep = if (completed) 5 else 0, isWizardDeploying = false)
        }
    }


    fun shouldPromptDeviceSetup(): Boolean =
        !DeviceSetupPreferences.hasSavedChoices(getApplication<Application>())



    fun setDeviceSetupSaved() {
        syncOobeDeviceChoices()
        saveSetupChoices()
        _uiState.update { it.copy(notificationMessage = "Device choices saved.") }
    }

    fun saveSetupChoices() {
        val app = getApplication<Application>()
        DeviceSetupPreferences.save(app, _uiState.value.deviceChanges)
        app.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("oobe_completed", true).apply()
        _uiState.update { it.copy(isOobeCompleted = true) }
    }


    fun closeInstallationWizard() {
        _uiState.update { it.copy(isWizardOpen = false) }
    }

    fun setWizardStep(step: Int) {
        _uiState.update { it.copy(wizardStep = step.coerceIn(0, 6)) }
    }

    fun nextWizardStep() {
        _uiState.update { it.copy(wizardStep = (it.wizardStep + 1).coerceAtMost(6)) }
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

    fun updateDeviceChanges(choices: DeviceChangeChoices) {
        _uiState.update { it.copy(deviceChanges = choices, oobeConfig = it.oobeConfig.copy(deviceChanges = choices)) }
    }

    private fun syncOobeDeviceChoices() {
        _uiState.update { state ->
            state.copy(oobeConfig = state.oobeConfig.copy(deviceChanges = state.deviceChanges))
        }
    }

    fun refreshDeviceCapabilityReport(context: Context, windowWidthDp: Int) {
        val foldPosture = _uiState.value.foldPosture
        val hasDexWorkspace = DesktopAutoLaunch.hasDexWorkspace(context)
        val externalCount = DesktopAutoLaunch.externalDisplays(context).size
        val manufacturer = Build.MANUFACTURER
        val route = DeviceCompatibilityClassifier.displayRoute(
            manufacturer,
            hasDexWorkspace,
            externalCount > 0
        )
        val formFactor = DeviceCompatibilityClassifier.formFactor(foldPosture, windowWidthDp)
        val report = DeviceCapabilityReport(
            manufacturer = manufacturer,
            model = Build.MODEL,
            androidRelease = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            appTargetSdk = context.applicationInfo.targetSdkVersion,
            route = route,
            formFactor = formFactor,
            externalDisplayCount = externalCount,
            hasDexWorkspace = hasDexWorkspace,
            isDeviceOwner = NetworkOnboarding.isDeviceOwner(context),
            canWriteSecureSettings = context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED,
            recommendedCheck = DeviceCompatibilityClassifier.recommendation(route, manufacturer, Build.VERSION.SDK_INT) +
                if (context.applicationInfo.targetSdkVersion >= 37 && Build.VERSION.SDK_INT >= 37) {
                    " Android 17 large-screen resizability restrictions apply; keep the activity resizable."
                } else ""
        )
        _uiState.update { it.copy(deviceCapabilityReport = report) }
    }

    fun updateFoldPosture(posture: FoldPosture, windowWidthDp: Int) {
        _uiState.update { it.copy(foldPosture = posture) }
        refreshDeviceCapabilityReport(getApplication(), windowWidthDp)
    }

    fun applySavedDeviceChoices() {
        val app = getApplication<Application>()
        val wasKeepingLinuxInBackground = DeviceSetupPreferences.shouldKeepLinuxInBackground(app)
        val choices = _uiState.value.deviceChanges
        DeviceSetupPreferences.save(app, choices)
        if (choices.applySystemWallpaper) {
            Thread { com.example.util.WallpaperProvisioner.applyOnce(app) }.start()
        }
        if (choices.keepLinuxInBackground) {
            if (LinuxRuntime.isRunning || choices.startLinuxOnLaunch) startBackgroundServiceIfActive()
        } else {
            if (wasKeepingLinuxInBackground) stopBackgroundService()
            if (choices.startLinuxOnLaunch && !LinuxRuntime.isRunning) bootstrapLinux()
        }
        if (choices.allowDeviceOwnerNetworkManagement && NetworkOnboarding.isDeviceOwner(app)) {
            NetworkOnboarding.allowSsidReadback(app)
        }
        if (choices.watchExternalDisplays) {
            com.example.service.DesktopAutoLaunchService.start(app)
        } else {
            app.stopService(Intent(app, com.example.service.DesktopAutoLaunchService::class.java))
        }
        _uiState.update {
            it.copy(notificationMessage = "Device choices applied. System-wide DNS, device-owner, erase, and debloat actions remain individually gated.")
        }
    }

    fun selectDeviceUseMode(mode: DeviceUseMode) {
        val enabled = mode == DeviceUseMode.CYBERDECK
        val choices = _uiState.value.deviceChanges.copy(
            useMode = mode,
            applySystemWallpaper = enabled,
            startLinuxOnLaunch = enabled,
            keepLinuxInBackground = enabled,
            startLinuxAtBoot = enabled,
            watchExternalDisplays = enabled,
            immersiveExternalDisplay = enabled,
            allowSystemPrivateDns = false,
            allowDeviceOwnerNetworkManagement = enabled,
            allowDebloatCommands = enabled,
            allowFactoryReset = false
        )
        _uiState.update {
            it.copy(
                deviceChanges = choices,
                oobeConfig = it.oobeConfig.copy(deviceChanges = choices)
            )
        }
    }

    fun setDeviceChangeChoice(update: (DeviceChangeChoices) -> DeviceChangeChoices) {
        _uiState.update { state ->
            val choices = update(state.deviceChanges)
            state.copy(deviceChanges = choices, oobeConfig = state.oobeConfig.copy(deviceChanges = choices))
        }
    }


    fun updateCapabilityReport(report: DeviceCapabilityReport) {
        _uiState.update { it.copy(deviceCapabilityReport = report) }
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
            val cfg = _uiState.value.oobeConfig
            // Selecting Linux in this explicit deployment step starts it for this session;
            // persistent launch/background behavior remains governed by the saved choices.
            val startLinux = cfg.enableLinuxDesktop
            if (startLinux) {
                progress(0.05f, "Starting the optional Alpine container...")
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
            } else {
                progress(0.05f, "Skipping Linux guest setup (not selected).")
                log("-   Linux guest was not installed or started")
            }

            if (startLinux) {
                progress(0.55f, "Installing the X11 desktop inside the guest (first run downloads ~450 MB)...")
                LinuxDesktop.ensureStarted(getApplication())
                log("OK  X11 desktop requested (progress appears in the Terminal window)")
            } else {
                progress(0.55f, "Optional Linux guest setup skipped.")
            }

            // Host package removal happens here, and only where Android allows it. A device-owner
            // install can hide packages for real; an ordinary install can only hand over commands.
            val host = getApplication<Application>()
            val targets = debloatTargets()
            if (targets.isNotEmpty() && HostDebloat.canRemovePackages(host)) {
                progress(0.75f, "Removing ${targets.size} selected package(s) from this device...")
                val removed = hideSelectedPackages(host) { log(it) }
                log("OK  $removed of ${targets.size} package(s) removed; hiding is reversible")
            } else if (targets.isNotEmpty()) {
                log("-   ${targets.size} package(s) selected, but this install is not device owner")
                log("-   Android gives a normal app no way to uninstall another app; nothing was removed")
            } else {
                log("-   nothing selected to remove from the host")
            }

            progress(0.9f, "Finalizing setup choices and desktop layout...")
            completeOobeSetup(host) {
                // OOBE has persisted the opt-in choices before we place anything on the clipboard.
                if (cfg.deviceChanges.allowDebloatCommands && debloatTargets().isNotEmpty()) {
                    val selectedCount = copyDebloatCommand()
                    if (selectedCount > 0) {
                        log("OK  copied $selectedCount adb uninstall command(s) to the clipboard")
                        log("-   they stay inert until you paste them into a PC terminal")
                    } else {
                        log("x   clipboard copy failed; the command is previewed in the trim step")
                    }
                } else {
                    log("-   automatic command copy is off; use Copy in the trim step if you want it")
                }
                _uiState.update {
                    it.copy(
                        isWizardDeploying = false,
                        wizardProgress = 1f,
                        wizardStatusMessage = "Setup complete",
                        containerStatus = if (startLinux) ContainerStatus.RUNNING else ContainerStatus.STOPPED,
                        notificationMessage = "Phonix setup finished"
                    )
                }
                onFinished()
            }
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
        val hasPhoto = _uiState.value.customization.customWallpaperPath != null
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(wallpaper = wallpaper),
                notificationMessage = when {
                    wallpaper == DesktopWallpaper.CUSTOM_PHOTO && !hasPhoto ->
                        "Choose a photo to use as the wallpaper"
                    else -> "Wallpaper set to ${wallpaper.title}"
                }
            )
        }
    }

    /**
     * Copies a gallery picture into the app and switches the desktop to it.
     * The import runs off the main thread because a camera original is tens of megabytes.
     */
    fun setCustomWallpaper(uri: android.net.Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                WallpaperStore.import(getApplication(), uri)
            }
            if (path == null) {
                _uiState.update {
                    it.copy(notificationMessage = "That image could not be read. Try a different one.")
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        customization = state.customization.copy(
                            wallpaper = DesktopWallpaper.CUSTOM_PHOTO,
                            customWallpaperPath = path
                        ),
                        notificationMessage = "Wallpaper set to your photo"
                    )
                }
            }
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
                "frames" -> state.customization.copy(showIconFrames = show)
                else -> state.customization
            }
            state.copy(customization = updated)
        }
    }

    /**
     * Renaming is per entry, not per app type: the desktop icon, the taskbar pin and the Start
     * menu tile share an id, so one rename updates all of them and the artwork is untouched.
     */
    fun startRename(id: String) {
        val state = _uiState.value
        val current = state.customization.shortcuts.firstOrNull { it.id == id }?.title
            ?: state.customization.taskbarPins.firstOrNull { it.id == id }?.title
            ?: return
        _uiState.update {
            it.copy(renameTargetId = id, renameDraft = current, activeContextMenu = null)
        }
    }

    fun updateRenameDraft(text: String) {
        _uiState.update { it.copy(renameDraft = text) }
    }

    fun cancelRename() {
        _uiState.update { it.copy(renameTargetId = null, renameDraft = "") }
    }

    fun commitRename() {
        val state = _uiState.value
        val id = state.renameTargetId ?: return
        val newTitle = state.renameDraft.trim()
        if (newTitle.isEmpty()) {
            _uiState.update { it.copy(renameTargetId = null, renameDraft = "", notificationMessage = "Name cannot be empty") }
            return
        }
        val rename: (DesktopShortcut) -> DesktopShortcut = { it.copy(title = newTitle) }
        _uiState.update {
            it.copy(
                customization = it.customization.copy(
                    shortcuts = it.customization.shortcuts.map { sc -> if (sc.id == id) rename(sc) else sc },
                    taskbarPins = it.customization.taskbarPins.map { pin -> if (pin.id == id) rename(pin) else pin }
                ),
                renameTargetId = null,
                renameDraft = "",
                notificationMessage = "Renamed to \"$newTitle\""
            )
        }
    }

    fun toggleIconFrames() {
        _uiState.update { state ->
            val show = !state.customization.showIconFrames
            state.copy(
                customization = state.customization.copy(showIconFrames = show),
                notificationMessage = if (show) "Icon tiles on" else "Icon tiles off - glyphs only"
            )
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
                val targetCol = DesktopIconGrid.nearestColumn(currentX, iconColumns)
                val targetRow = DesktopIconGrid.nearestRow(currentY, iconAreaHeightDp)
                val (targetX, targetY) =
                    DesktopIconGrid.snap(targetCol, targetRow, iconAreaHeightDp)

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
                        findNearestFreeGridSlot(
                            targetCol,
                            targetRow,
                            occupied,
                            maxCol = iconColumns - 1,
                            maxRow = DesktopIconGrid.maxRow(iconAreaHeightDp)
                        )
                    }

                    currentShortcuts.map { sc ->
                        when (sc.id) {
                            shortcutId -> sc.copy(
                                gridCol = targetCol,
                                gridRow = targetRow,
                                posX = targetX,
                                posY = targetY,
                                hasCustomPosition = true
                            )
                            occupant.id -> {
                                val (occupantX, occupantY) = DesktopIconGrid.snap(
                                    occupantNewCol,
                                    occupantNewRow,
                                    iconAreaHeightDp
                                )
                                sc.copy(
                                    gridCol = occupantNewCol,
                                    gridRow = occupantNewRow,
                                    posX = occupantX,
                                    posY = occupantY,
                                    hasCustomPosition = true
                                )
                            }
                            else -> sc
                        }
                    }
                } else {
                    currentShortcuts.map { sc ->
                        if (sc.id == shortcutId) {
                            sc.copy(
                                gridCol = targetCol,
                                gridRow = targetRow,
                                posX = targetX,
                                posY = targetY,
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

    // ---- Desktop icon grid ---------------------------------------------------------------

    /**
     * The icon area the desktop is laying out into, reported by the desktop screen whenever it
     * changes. Snapping works in this space instead of a second set of guessed numbers, which is
     * why "Lock Icons to Grid" cannot drop an icon somewhere the desktop never draws.
     */
    private var iconColumns = 6
    private var iconAreaHeightDp = 720f

    fun setIconGrid(columns: Int, areaHeightDp: Float) {
        iconColumns = columns.coerceAtLeast(1)
        iconAreaHeightDp = areaHeightDp.coerceAtLeast(1f)
    }

    /**
     * Moves freeform icons by [deltaDp] when the icon area changes height, which is what happens
     * when the taskbar docks to another edge. Icon positions are offsets inside that area, so
     * without this an icon dragged to the bottom row is left behind under the bar - or stranded
     * above it - instead of following the work area.
     */
    fun reanchorShortcuts(deltaDp: Float, areaHeightDp: Float) {
        if (deltaDp == 0f) return
        _uiState.update { state ->
            val lowest = DesktopIconGrid.bottomRowTop(areaHeightDp)
            val updated = state.customization.shortcuts.map { sc ->
                if (!sc.hasCustomPosition) {
                    sc
                } else {
                    sc.copy(posY = (sc.posY + deltaDp).coerceIn(0f, lowest))
                }
            }
            state.copy(customization = state.customization.copy(shortcuts = updated))
        }
    }

    fun toggleLockIconsToGrid(enabled: Boolean) {
        _uiState.update { state ->
            val shortcuts = if (enabled) {
                snapShortcutsToGrid(state.customization.shortcuts)
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

    /**
     * Pins every icon to its nearest free grid slot.
     *
     * All of them, not only the dragged ones: an icon that has never been moved is drawn from
     * [DesktopIconGrid.autoPosition] and has to be pinned to the slot it is drawn on, otherwise
     * locking the grid appears to do nothing at all. Slots are the renderer's (90 x 88 dp, bottom
     * anchored), so an icon already on the grid keeps its position to the pixel.
     */
    private fun snapShortcutsToGrid(shortcuts: List<DesktopShortcut>): List<DesktopShortcut> {
        val columns = iconColumns
        val areaHeight = iconAreaHeightDp
        val occupied = mutableSetOf<Pair<Int, Int>>()
        return shortcuts.mapIndexed { index, sc ->
            val current = if (sc.hasCustomPosition) {
                Pair(sc.posX, sc.posY)
            } else {
                DesktopIconGrid.autoPosition(index, shortcuts.size, columns, areaHeight)
            }
            val wanted = Pair(
                DesktopIconGrid.nearestColumn(current.first, columns),
                DesktopIconGrid.nearestRow(current.second, areaHeight)
            )
            val slot = if (wanted in occupied) {
                findNearestFreeGridSlot(
                    wanted.first,
                    wanted.second,
                    occupied,
                    maxCol = columns - 1,
                    maxRow = DesktopIconGrid.maxRow(areaHeight)
                )
            } else {
                wanted
            }
            occupied.add(slot)
            val (x, y) = DesktopIconGrid.snap(slot.first, slot.second, areaHeight)
            sc.copy(
                gridCol = slot.first,
                gridRow = slot.second,
                posX = x,
                posY = y,
                hasCustomPosition = true
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

    /** Right-click or long-press on the taskbar: dock it to any of the four edges. */
    fun openTaskbarContextMenu(position: TaskbarPosition) {
        _uiState.update {
            it.copy(
                activeContextMenu = DesktopContextMenuData.TaskbarMenu(position),
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
        bgStyle: ShortcutBgStyle,
        artKey: String? = null
    ) {
        _uiState.update { state ->
            val updateSc = { sc: DesktopShortcut ->
                sc.copy(
                    customSize = size,
                    customShape = shape,
                    customColor = color,
                    customBackground = bgStyle,
                    customArtKey = artKey
                )
            }
            val updated = state.customization.shortcuts.map { sc ->
                if (sc.id == shortcutId) updateSc(sc) else sc
            }
            val updatedPins = state.customization.taskbarPins.map { pin ->
                if (pin.id == shortcutId) updateSc(pin) else pin
            }
            state.copy(
                customization = state.customization.copy(
                    shortcuts = updated,
                    taskbarPins = updatedPins
                ),
                activeContextMenu = null,
                notificationMessage = "Updated shortcut appearance"
            )
        }
    }

    fun resetDesktopCustomizations(context: Context? = null) {
        val defaults = DesktopCustomization()
        _uiState.update { state ->
            state.copy(
                customization = defaults,
                activeContextMenu = null,
                notificationMessage = "Desktop layout and icon themes reset to defaults"
            )
        }
        context?.let { DesktopPrefs.clear(it) }
    }

    /**
     * Adds or removes a taskbar entry. This touches only the taskbar's own list, so the desktop
     * icon is never affected, and a pinned app keeps working when its icon is deleted.
     */
    fun togglePinShortcutToTaskbar(shortcutId: String) {
        _uiState.update { state ->
            val pins = state.customization.taskbarPins
            val existing = pins.find { it.id == shortcutId }
            if (existing != null) {
                state.copy(
                    customization = state.customization.copy(taskbarPins = pins - existing),
                    activeContextMenu = null,
                    notificationMessage = "Unpinned from taskbar"
                )
            } else {
                val source = state.customization.shortcuts.find { it.id == shortcutId }
                    ?: return@update state
                state.copy(
                    customization = state.customization.copy(taskbarPins = pins + source),
                    activeContextMenu = null,
                    notificationMessage = "Pinned to taskbar"
                )
            }
        }
    }

    /** Long-press on a taskbar icon: drop the pin only, leaving any desktop icon untouched. */
    fun unpinFromTaskbar(shortcutId: String) {
        _uiState.update { state ->
            val pins = state.customization.taskbarPins
            if (pins.none { it.id == shortcutId }) return@update state
            state.copy(
                customization = state.customization.copy(
                    taskbarPins = pins.filterNot { it.id == shortcutId }
                ),
                notificationMessage = "Removed from taskbar"
            )
        }
    }

    /** Removes a desktop icon only. The taskbar has its own list and is left alone. */
    fun removeShortcut(shortcutId: String) {
        _uiState.update { state ->
            val updated = state.customization.shortcuts.filter { it.id != shortcutId }
            val stillPinned = state.customization.taskbarPins.any { it.id == shortcutId }
            state.copy(
                customization = state.customization.copy(shortcuts = updated),
                activeContextMenu = null,
                notificationMessage = if (stillPinned) {
                    "Removed from the desktop - still in the taskbar"
                } else {
                    "Shortcut removed from desktop"
                }
            )
        }
    }

    // OOBE Setup Wizard Management
    fun updateOobeConfig(config: OobeSetupConfig) {
        _uiState.update { it.copy(oobeConfig = config) }
    }

    /**
     * The welcome answer. It pre-configures real state (use mode, workload profile, trim list)
     * rather than only setting a label, and every piece of it stays editable in the later steps.
     */
    fun selectSetupPersona(persona: SetupPersona) {
        val previous = _uiState.value.setupPersona
        _uiState.update { state ->
            state.copy(
                setupPersona = persona,
                oobeConfig = state.oobeConfig.copy(profile = persona.profile)
            )
        }
        if (previous == persona) return
        applyDebloatPreset(trimEverything = persona.trimHostPackages)
        selectDeviceUseMode(if (persona.applianceMode) DeviceUseMode.CYBERDECK else DeviceUseMode.DAILY_DRIVER)
    }

    /**
     * A daily driver keeps only the tracking packages ticked; a dedicated build starts with the
     * whole catalogue. Either way the list is a suggestion the user can flip in the trim step.
     */
    fun applyDebloatPreset(trimEverything: Boolean) {
        val dailySafe = setOf("Analytics & Tracking", "Carrier Telemetry")
        _uiState.update { state ->
            state.copy(
                debloatPackages = state.debloatPackages.map { pkg ->
                    pkg.copy(isEnabled = trimEverything || pkg.category in dailySafe)
                },
                debloatCopiedCommand = null
            )
        }
    }

    fun selectOobeProfile(profile: MiniPcProfile) {
        _uiState.update { state ->
            state.copy(oobeConfig = state.oobeConfig.copy(profile = profile))
        }
        // A workstation profile and a headless appliance want very different trim lists.
        applyDebloatPreset(trimEverything = profile != MiniPcProfile.EVERYDAY_DESKTOP)
    }

    /**
     * Reads the facts the setup screens show: storage, which browsers are really installed (with
     * their measured APK size), which trim targets exist on this device, and whether hiding a
     * package is possible at all on this install.
     */
    fun refreshSetupFacts(context: Context) {
        refreshStorage()
        val canRemove = HostDebloat.canRemovePackages(context)
        _uiState.update { it.copy(canRemoveHostPackages = canRemove) }
        viewModelScope.launch {
            val browsers = withContext(Dispatchers.IO) {
                PreferredBrowser.entries
                    .mapNotNull { it.packageName }
                    .mapNotNull { pkg -> PackageFacts.browserInfo(context, pkg) }
            }
            _uiState.update { it.copy(installedBrowsers = browsers) }
            refreshDebloatPresence(context)
        }
    }

    /**
     * Re-reads what this device really has and what this install has hidden, in one place.
     *
     * The two maps are always written together: a hidden package must never be offered as a
     * removal target a second time, and the debloater needs the hidden set to offer an undo.
     */
    private suspend fun refreshDebloatPresence(context: Context) {
        val packages = _uiState.value.debloatPackages.map { it.packageName }
        val (present, hidden) = withContext(Dispatchers.IO) {
            val hiddenSet = packages.filter { HostDebloat.isHidden(context, it) }.toSet()
            // Presence means "on disk", not "safe to remove": a hidden package is still installed,
            // and reporting it absent left it with no Restore button and no way back.
            val presentMap = packages.associate { pkg ->
                pkg to (PackageFacts.isInstalled(context, pkg) || pkg in hiddenSet)
            }
            presentMap to hiddenSet
        }
        _uiState.update { it.copy(debloatInstalled = present, debloatHidden = hidden) }
    }

    /**
     * Asks the assistant to review the ticked list against what this phone actually has.
     *
     * The assistant cannot perform the removal: its shell is the proot Alpine guest, which has
     * no adb and no authority over Android packages. Removal stays with the deploy step, which
     * uses DevicePolicyManager on a device-owner install. This is advice, clearly labelled.
     */
    fun requestDebloatAdvice() {
        val state = _uiState.value
        val ticked = state.debloatPackages.filter { it.isEnabled }
        if (ticked.isEmpty()) {
            _uiState.update { it.copy(debloatAdvice = "Nothing is ticked, so nothing would be removed.", debloatAdviceFromModel = false) }
            return
        }

        // Deterministic summary, used immediately when no model is running and as the fallback
        // if the model fails. It only states measured facts about this list.
        val onDevice = ticked.filter { state.debloatInstalled[it.packageName] != false }
        val absent = ticked.size - onDevice.size
        val fallback = buildString {
            appendLine("${ticked.size} ticked, ${onDevice.size} present on this device" + if (absent > 0) ", $absent absent and therefore a no-op." else ".")
            appendLine()
            ticked.groupBy { it.category }.forEach { (category, items) ->
                appendLine("• $category: " + items.joinToString(", ") { it.displayName })
            }
            appendLine()
            append(
                if (state.canRemoveHostPackages)
                    "This install is device owner, so Deploy hides these packages on this device. Hiding is reversible."
                else
                    "This install is not device owner, so Phonix cannot remove anything itself; Deploy copies the adb commands for a PC."
            )
        }

        if (state.aiEngineState != AiEngineState.READY) {
            _uiState.update { it.copy(debloatAdvice = fallback, debloatAdviceFromModel = false, debloatAdviceBusy = false) }
            return
        }

        _uiState.update { it.copy(debloatAdviceBusy = true) }
        viewModelScope.launch {
            val list = ticked.joinToString("\n") { pkg ->
                val where = when (state.debloatInstalled[pkg.packageName]) {
                    false -> "not installed on this device"
                    true -> "installed"
                    null -> "presence unknown"
                }
                "- ${pkg.packageName} - ${pkg.displayName} ($where)"
            }
            val prompt = """
                Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}.
                These Android packages are ticked for removal:
                $list
                For each one, say in a single short line what it does and whether removing it could break something the user may still want. Finish with the single biggest risk. Do not mention packages that are not in this list.
            """.trimIndent()
            val result = localAi.streamChat(
                systemPrompt = buildLocalAiSystemPrompt(),
                referenceNotes = null,
                history = listOf(LocalAiEngine.ChatTurn(isUser = true, text = prompt))
            ) { }
            val reply = result.getOrNull()?.text?.trim().orEmpty()
            _uiState.update {
                it.copy(
                    debloatAdvice = reply.ifBlank { fallback },
                    debloatAdviceFromModel = reply.isNotBlank(),
                    debloatAdviceBusy = false
                )
            }
        }
    }

    fun updateDebloatPresence(presence: Map<String, Boolean>) {
        _uiState.update { it.copy(debloatInstalled = presence) }
    }

    /**
     * Hides one package for real from the walkthrough. Only a device-owner install can do this,
     * and [HostDebloat.hidePackage] returns the platform's own refusal rather than a guess.
     */
    fun hideHostPackage(packageName: String) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val failure = withContext(Dispatchers.IO) { HostDebloat.hidePackage(app, packageName) }
            appendTerminalLine(
                TerminalLine(
                    if (failure == null) "hid $packageName" else "could not hide $packageName: $failure",
                    if (failure == null) TerminalLine.LineType.SUCCESS else TerminalLine.LineType.ERROR
                )
            )
            refreshDebloatPresence(app)
        }
    }

    /** Undoes [hideHostPackage]: puts a hidden package back on the launcher. */
    fun unhideHostPackage(packageName: String) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val failure = withContext(Dispatchers.IO) { HostDebloat.unhidePackage(app, packageName) }
            appendTerminalLine(
                TerminalLine(
                    if (failure == null) "restored $packageName" else "could not restore $packageName: $failure",
                    if (failure == null) TerminalLine.LineType.SUCCESS else TerminalLine.LineType.ERROR
                )
            )
            refreshDebloatPresence(app)
        }
    }

    /** Hides the selected packages for real. Only meaningful on a device-owner install. */
    private suspend fun hideSelectedPackages(context: Context, log: (String) -> Unit): Int {
        val selected = _uiState.value.debloatPackages.filter { it.isEnabled }
        var removed = 0
        for (pkg in selected) {
            if (!PackageFacts.isInstalled(context, pkg.packageName)) {
                log("-   ${pkg.packageName} is not on this device, skipped")
                continue
            }
            val failure = withContext(Dispatchers.IO) { HostDebloat.hidePackage(context, pkg.packageName) }
            if (failure == null) {
                removed++
                log("OK  removed ${pkg.packageName}")
            } else {
                log("x   ${pkg.packageName}: $failure")
            }
        }
        if (removed > 0) {
            // A hidden package still answers getPackageInfo, so presence comes from the tri-state.
            refreshDebloatPresence(context)
            _uiState.update { state ->
                state.copy(
                    debloatPackages = state.debloatPackages.map { pkg ->
                        if (pkg.isEnabled && state.debloatInstalled[pkg.packageName] == false) {
                            pkg.copy(isEnabled = false)
                        } else {
                            pkg
                        }
                    }
                )
            }
        }
        return removed
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

    fun completeOobeSetup(context: Context, onComplete: () -> Unit = {}) {
        syncOobeDeviceChoices()
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("oobe_completed", true).apply()
                DeviceSetupPreferences.save(context, _uiState.value.deviceChanges)
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

            // Handle Chrome debloat if requested by user in OOBE
            if (cfg.debloatChrome) {
                if (HostDebloat.canRemovePackages(context)) {
                    HostDebloat.hidePackage(context, "com.android.chrome")
                } else {
                    HostDebloat.launchUninstall(context, "com.android.chrome")
                }
            }

            // Configure offline knowledge library tier
            KnowledgeLibrary.setTier(cfg.offlineLibraryTier)

            // Configure browser shortcut according to user's chosen browser
            val browserShortcut = when (cfg.preferredBrowser) {
                PreferredBrowser.CHROMIUM -> DesktopShortcut(
                    id = "sc_browser",
                    title = "Chromium",
                    windowType = WindowType.BROWSER,
                    launchPackage = "org.chromium.chrome",
                    isNativeApp = true,
                    customColor = DesktopAccentColor.CYBER_CYAN
                )
                PreferredBrowser.FIREFOX -> DesktopShortcut(
                    id = "sc_browser",
                    title = "Mozilla Firefox",
                    windowType = WindowType.BROWSER,
                    launchPackage = "org.mozilla.firefox",
                    isNativeApp = true,
                    customColor = DesktopAccentColor.AMBER_EMBER
                )
                PreferredBrowser.PHONIX_BROWSER -> DesktopShortcut(
                    id = "sc_browser",
                    title = "Phonix Browser",
                    windowType = WindowType.BROWSER,
                    customColor = DesktopAccentColor.CYBER_CYAN
                )
            }
            addShortcut(browserShortcut)


            if (categories.contains(AppCategory.FILE_MANAGER)) {
                addShortcut(
                    DesktopShortcut(
                        id = "sc_files",
                        title = "File Explorer",
                        windowType = WindowType.FILE_EXPLORER,
                        customColor = DesktopAccentColor.AMBER_EMBER,
                    launchPackage = "com.google.android.documentsui",
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
                        customColor = DesktopAccentColor.EMERALD_LINUX
                    )
                )
            }

            // Settings is always on the desktop; other tools live in the Start menu and taskbar.
            addShortcut(
                DesktopShortcut(
                    id = "sc_settings",
                    title = "Settings",
                    windowType = WindowType.SETTINGS,
                    isNativeApp = true
                )
            )

            val (themeAccent, themeWallpaper) = when (cfg.themePreset) {
                DesktopThemePreset.FLUENT_DARK -> Pair(DesktopAccentColor.ELECTRIC_BLUE, DesktopWallpaper.PHOENIX_CYBER_DEFAULT)
                DesktopThemePreset.CYBER_PHOENIX -> Pair(DesktopAccentColor.AMBER_EMBER, DesktopWallpaper.EMBER_MARK)
                DesktopThemePreset.CLEAN_SLATE -> Pair(DesktopAccentColor.SLATE_MONOCHROME, DesktopWallpaper.ALPINE_PEAK)
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

            saveSetupChoices()
            applySavedDeviceChoices()
            onComplete()
        }
    }

    fun resetForNewUser(context: Context) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("oobe_completed", false).apply()
                DesktopPrefs.clear(context)
            } catch (_: Exception) {
            }

            _uiState.update { state ->
                state.copy(
                    isOobeCompleted = false,
                    oobeConfig = OobeSetupConfig(),
                    customization = DesktopCustomization(),
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
        val packages = debloatTargets()
        return packages.joinToString("\n") { "adb shell pm uninstall --user 0 ${it.packageName}" }
    }

    /**
     * Ticked packages that this device actually has. A Samsung-only package on a Pixel is not a
     * removal target, and listing it in a command would just produce a no-op error at the PC.
     */
    fun debloatTargets(): List<DebloatPackage> {
        val state = _uiState.value
        val presence = state.debloatInstalled
        return state.debloatPackages.filter { pkg ->
            pkg.isEnabled &&
                (presence.isEmpty() || presence[pkg.packageName] != false) &&
                pkg.packageName !in state.debloatHidden
        }
    }

    /**
     * Copies the debloat commands and says so. Pressing copy is itself the intent - the text is
     * inert until someone pastes it - so this no longer hides behind the Cyberdeck opt-in; that
     * switch now only governs the automatic copy at the end of setup.
     */
    fun copyDebloatCommand(): Int {
        val selected = debloatTargets()
        val command = selected.joinToString("\n") { "adb shell pm uninstall --user 0 ${it.packageName}" }
        val copied = if (selected.isEmpty()) false else try {
            val clipboard = getApplication<Application>()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Phonix debloat", command))
            true
        } catch (_: Throwable) {
            false
        }
        _uiState.update {
            it.copy(
                debloatCopiedCommand = if (copied) command else it.debloatCopiedCommand,
                notificationMessage = when {
                    selected.isEmpty() -> "Nothing selected to remove"
                    copied -> "Copied ${selected.size} adb command(s) - run them from a PC with USB debugging"
                    else -> "${selected.size} package(s) selected (clipboard unavailable)"
                }
            )
        }
        return if (copied) selected.size else 0
    }

    // Screenless Mini PC Debloat Batch: selects the suggested set, then hands over the commands.
    fun runScreenlessMiniPcDebloat() {
        val choices = DeviceSetupPreferences.choices(getApplication())
        if (choices.useMode != DeviceUseMode.CYBERDECK || !choices.allowDebloatCommands) {
            _uiState.update { it.copy(notificationMessage = "Debloat commands require Cyberdeck mode and explicit permission in Device & Display Setup.") }
            return
        }
        _uiState.update { state ->
            val updatedPkgs = state.debloatPackages.map { pkg ->
                pkg.copy(isEnabled = pkg.category in DebloatCatalog.applianceCategories)
            }
            state.copy(debloatPackages = updatedPkgs)
        }

        copyDebloatCommand()
    }

    private fun startBackgroundServiceIfActive() {
        if (!DeviceSetupPreferences.shouldKeepLinuxInBackground(getApplication())) return
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
    // Manual software updates (user-initiated, verified, never silent)
    // -------------------------------------------------------------
    fun checkAppUpdates() {
        val app = getApplication<Application>()
        val packageInfo = runCatching {
            app.packageManager.getPackageInfo(app.packageName, 0)
        }.getOrNull()
        val currentVersionName = packageInfo?.versionName ?: BuildConfig.VERSION_NAME
        val currentVersionCode = packageInfo?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt()
            else @Suppress("DEPRECATION") it.versionCode
        } ?: BuildConfig.VERSION_CODE

        fun unknownState(note: String) = AppUpdateInfo(
            currentVersionName = currentVersionName,
            currentVersionCode = currentVersionCode,
            latestVersionName = currentVersionName,
            latestVersionCode = currentVersionCode,
            releaseNotes = note,
            releaseUrl = "",
            isUpdateAvailable = false
        )

        // A build with no feed has checked nothing. Say that instead of implying it is current.
        val feedUrl = BuildConfig.UPDATE_FEED_URL.trim()
        if (feedUrl.isEmpty()) {
            _uiState.update {
                it.copy(
                    updateState = UpdateCheckState.ERROR,
                    updateInfo = unknownState(
                        "No update feed is configured for this build. Nothing was checked and no update status is known."
                    )
                )
            }
            return
        }

        _uiState.update { it.copy(updateState = UpdateCheckState.CHECKING) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val document = UpdateInstaller(app).readFeed(feedUrl)
                val manifest = UpdateFeed.parse(document) ?: throw IllegalStateException(
                    "The update feed is malformed: versionCode, versionName, an https apkUrl and a SHA-256 are required."
                )
                val available = UpdateFeed.isNewer(manifest, currentVersionCode)
                val info = AppUpdateInfo(
                    currentVersionName = currentVersionName,
                    currentVersionCode = currentVersionCode,
                    latestVersionName = manifest.versionName,
                    latestVersionCode = manifest.versionCode,
                    releaseUrl = feedUrl,
                    apkDownloadUrl = manifest.apkUrl,
                    sha256 = manifest.sha256,
                    releaseNotes = if (available) {
                        manifest.notes.ifBlank { "${manifest.versionName} is published." }
                    } else {
                        "This unit runs the newest published build (${manifest.versionName})."
                    },
                    publishedDate = manifest.published,
                    isUpdateAvailable = available
                )
                _uiState.update {
                    it.copy(
                        updateState = if (available) UpdateCheckState.UPDATE_AVAILABLE else UpdateCheckState.UP_TO_DATE,
                        updateInfo = info
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        updateState = UpdateCheckState.ERROR,
                        updateInfo = unknownState(
                            "The update check failed: ${e.message ?: e.javaClass.simpleName}. No update status is known."
                        )
                    )
                }
            }
        }
    }

    /**
     * Downloads the feed's APK, verifies it, and hands it to Android's installer. The user sees
     * the system's confirmation; this method never installs anything on its own.
     */
    fun downloadAndInstallUpdate(context: Context) {
        val info = _uiState.value.updateInfo ?: return
        if (info.apkDownloadUrl.isBlank() || info.sha256.isBlank()) {
            _uiState.update {
                it.copy(
                    updateState = UpdateCheckState.ERROR,
                    updateInfo = info.copy(
                        releaseNotes = "The feed published no APK with a SHA-256, so this build cannot install it itself."
                    )
                )
            }
            return
        }

        val manifest = UpdateManifest(
            versionCode = info.latestVersionCode,
            versionName = info.latestVersionName,
            apkUrl = info.apkDownloadUrl,
            sha256 = info.sha256,
            notes = info.releaseNotes,
            published = info.publishedDate
        )

        _uiState.update {
            it.copy(
                updateState = UpdateCheckState.DOWNLOADING,
                updateInfo = info.copy(releaseNotes = "Downloading ${info.latestVersionName}...")
            )
        }

        viewModelScope.launch {
            try {
                val installer = UpdateInstaller(getApplication())
                val apk = withContext(Dispatchers.IO) {
                    installer.download(
                        manifest,
                        onProgress = { line ->
                            _uiState.update { state ->
                                state.updateInfo?.let { current ->
                                    state.copy(updateInfo = current.copy(releaseNotes = line))
                                } ?: state
                            }
                        },
                        onBytes = { written, total ->
                            if (total > 0) {
                                val percent = (written * 100 / total).toInt()
                                val megabytes = written / (1024 * 1024)
                                val totalMegabytes = total / (1024 * 1024)
                                _uiState.update { state ->
                                    state.updateInfo?.let { current ->
                                        state.copy(
                                            updateInfo = current.copy(
                                                releaseNotes = "Downloading $percent% ($megabytes of $totalMegabytes MB)..."
                                            )
                                        )
                                    } ?: state
                                }
                            }
                        }
                    )
                }

                _uiState.update {
                    it.copy(
                        updateState = UpdateCheckState.READY_TO_INSTALL,
                        updateInfo = info.copy(
                            releaseNotes = "Downloaded ${apk.length() / (1024 * 1024)} MB and matched the " +
                                "published SHA-256. Android will ask you to confirm the install."
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    installer.install(apk) { installed, message ->
                        _uiState.update { state ->
                            state.copy(
                                updateState = if (installed) UpdateCheckState.IDLE else UpdateCheckState.ERROR,
                                updateInfo = state.updateInfo?.copy(releaseNotes = message)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        updateState = UpdateCheckState.ERROR,
                        updateInfo = info.copy(
                            releaseNotes = "The update was not installed: ${e.message ?: e.javaClass.simpleName}. " +
                                "Nothing was changed."
                        )
                    )
                }
            }
        }
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
     * Opt-in setup: boot the guest, install a llama.cpp server for the active distro if missing,
     * download and
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
                    failAiSetup("The Linux container did not start. Open Terminal and check its output.")
                    return@launch
                }

                if (!localAi.isServerPackageInstalled()) {
                    _uiState.update {
                        it.copy(
                            aiEngineState = AiEngineState.INSTALLING,
                            aiEngineMessage = "Installing the llama.cpp server in the active guest..."
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
            Answer accurately and concisely; use as many words as the question needs, and never pad. You run
            entirely offline on the phone's CPU. You cannot browse the web or call external APIs, and you must not
            claim to have done so. You CAN write code: when asked for a program, give complete runnable code in a
            single block, with no placeholder comments like "your code here", and say how to run it in two lines
            or fewer. The Alpine guest has python3, sh, and gcc after apk add gcc musl-dev.
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

        // The library is the primary grounding source and honours the chosen shelf; the old
        // topic table stays as a fallback for questions the library has no document for.
        val notes = KnowledgeLibrary.contextFor(trimmed, stateBefore.aiKnowledgeScope)
            ?: DefaultAiKnowledge.notesFor(
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

    // -------------------------------------------------------------
    // Offline library
    // -------------------------------------------------------------

    fun setAiKnowledgeScope(category: KnowledgeCategory?) {
        _uiState.update {
            it.copy(
                aiKnowledgeScope = category,
                notificationMessage = "Knowledge scope: ${category?.title ?: "all shelves"}"
            )
        }
    }

    fun toggleKnowledgeLibrary() {
        _uiState.update { it.copy(showKnowledgeLibrary = !it.showKnowledgeLibrary, openKnowledgeDocId = null) }
    }

    fun openKnowledgeDoc(id: String?) {
        _uiState.update { it.copy(openKnowledgeDocId = id) }
    }

    fun copyText(text: String, label: String) {
        val copied = try {
            val clipboard = getApplication<Application>()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        } catch (_: Throwable) {
            false
        }
        _uiState.update {
            it.copy(notificationMessage = if (copied) "Copied $label" else "Clipboard unavailable")
        }
    }

    /** Closes the reader and asks the model about the document, on that shelf. */
    fun askAboutDocument(doc: com.example.model.KnowledgeDoc) {
        _uiState.update { it.copy(openKnowledgeDocId = null, showKnowledgeLibrary = false, aiKnowledgeScope = doc.category) }
        sendAiMessage("Using the offline notes on \"${doc.title}\", answer: how do I do this on this device, step by step?")
        _uiState.update { it.copy(aiKnowledgeScope = null) }
    }

    // -------------------------------------------------------------
    // Shell agent: one command per turn, approved by the user
    // -------------------------------------------------------------

    private val agentSystemPrompt: String
        get() {
        val guest = runCatching { LinuxRuntime.container(getApplication<Application>()).activeDistro() }
            .getOrDefault(DistroCatalog.alpine)
        return """
        You are Phonix Shell, a command-line agent working inside a ${guest.name} guest on this phone.
        Packages come from ${guest.packageManager}; do not suggest another distro's package manager.
        You guide a user through a task by issuing one shell command at a time and reading its output.
        Reply with exactly ONE line, in one of these two forms:
        RUN <command>
        DONE <one sentence summary of what was achieved>
        Rules: no explanation, no markdown, no code fences, one command per reply. Prefer python3 or sh.
        Use relative paths in the guest home. Never run rm -rf /, mkfs, dd to a device, or anything that
        reboots or wipes the device. Check before you create: use ls and cat first.
    """.trimIndent()
        }

    fun setAgentTask(task: String) {
        _uiState.update { it.copy(agentTask = task) }
    }

    fun setAgentAutoRun(enabled: Boolean) {
        _uiState.update { it.copy(agentAutoRun = enabled) }
    }

    fun stopAgent() {
        agentJob?.cancel()
        agentJob = null
        _uiState.update { it.copy(agentRunning = false, agentPendingCommand = null, agentStatus = "Stopped") }
    }

    fun startAgentTask(task: String = _uiState.value.agentTask) {
        val trimmed = task.trim()
        if (trimmed.isEmpty()) return
        if (!LinuxRuntime.isRunning) {
            _uiState.update {
                it.copy(
                    agentTask = trimmed,
                    agentStatus = "Start the Linux guest first (Terminal power button); the agent runs commands inside it."
                )
            }
            return
        }
        if (_uiState.value.aiEngineState != AiEngineState.READY) {
            _uiState.update { it.copy(agentTask = trimmed, agentStatus = "The agent needs the local model running.") }
            return
        }
        _uiState.update {
            it.copy(
                agentTask = trimmed,
                agentSteps = emptyList(),
                agentPendingCommand = null,
                agentRunning = true,
                agentStatus = "Thinking about step 1..."
            )
        }
        requestNextAgentCommand()
    }

    private var agentJob: kotlinx.coroutines.Job? = null

    private fun requestNextAgentCommand() {
        val state = _uiState.value
        if (state.agentSteps.size >= 8) {
            _uiState.update { it.copy(agentRunning = false, agentStatus = "Stopped after 8 steps. Start a new task to continue.") }
            return
        }
        agentJob = viewModelScope.launch {
            val transcript = buildString {
                appendLine("Task: ${state.agentTask}")
                if (state.agentSteps.isEmpty()) {
                    appendLine("Nothing has run yet.")
                } else {
                    for ((index, step) in state.agentSteps.withIndex()) {
                        appendLine("Step ${index + 1}: RUN ${step.command}")
                        appendLine("Output: ${step.output.takeLast(1500)}")
                    }
                    appendLine("That is ${state.agentSteps.size} of at most 8 steps. Continue, or answer DONE.")
                }
            }
            _uiState.update { it.copy(agentStatus = "Step ${it.agentSteps.size + 1} of 8: asking the model...") }

            val result = localAi.streamChat(
                systemPrompt = agentSystemPrompt,
                referenceNotes = KnowledgeLibrary.contextFor(state.agentTask, KnowledgeCategory.CODING),
                history = listOf(LocalAiEngine.ChatTurn(isUser = true, text = transcript))
            ) { }

            val reply = result.getOrNull()?.text?.trim().orEmpty()
            if (reply.isBlank()) {
                _uiState.update {
                    it.copy(
                        agentRunning = false,
                        agentStatus = "The model did not answer (${result.exceptionOrNull()?.message ?: "empty reply"})."
                    )
                }
                return@launch
            }

            val runLine = reply.lines().firstOrNull { it.trim().uppercase().startsWith("RUN ") }
            val doneLine = reply.lines().firstOrNull { it.trim().uppercase().startsWith("DONE") }
            when {
                runLine != null -> {
                    val command = runLine.trim().removePrefix("RUN ").removePrefix("run ").trim()
                    _uiState.update {
                        it.copy(
                            agentRunning = false,
                            agentPendingCommand = command,
                            agentStatus = "Proposed step ${it.agentSteps.size + 1}: review and run it."
                        )
                    }
                    if (_uiState.value.agentAutoRun) approveAgentCommand()
                }
                doneLine != null -> _uiState.update {
                    it.copy(agentRunning = false, agentPendingCommand = null, agentStatus = doneLine.trim())
                }
                else -> _uiState.update {
                    it.copy(
                        agentRunning = false,
                        agentStatus = "The model replied without a RUN line: ${reply.take(120)}"
                    )
                }
            }
        }
    }

    fun approveAgentCommand() {
        val command = _uiState.value.agentPendingCommand ?: return
        _uiState.update { it.copy(agentPendingCommand = null, agentRunning = true, agentStatus = "Running: $command") }
        viewModelScope.launch {
            val output = withContext(Dispatchers.IO) {
                runCatching { LinuxRuntime.run(command, 60_000) }.getOrNull()
            }
            val text = output?.trim().takeUnless { it.isNullOrBlank() }
                ?: "(no output)"
            appendTerminalLine(TerminalLine("$ $command", TerminalLine.LineType.INPUT))
            appendTerminalLine(TerminalLine(text, TerminalLine.LineType.SYSTEM))
            _uiState.update {
                it.copy(agentSteps = it.agentSteps + AgentStep(command = command, output = text))
            }
            requestNextAgentCommand()
        }
    }

    fun rejectAgentCommand() {
        _uiState.update { it.copy(agentPendingCommand = null, agentRunning = false, agentStatus = "Rejected that command.") }
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
