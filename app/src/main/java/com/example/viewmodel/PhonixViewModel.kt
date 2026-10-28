package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.LinuxSystemEmulator
import com.example.engine.TerminalLine
import com.example.model.DefaultServerCatalog
import com.example.model.DesktopResolution
import com.example.model.DesktopWindow
import com.example.model.DistroCatalog
import com.example.model.GpuDriverType
import com.example.model.HardwareAccelerationConfig
import com.example.model.LinuxDistro
import com.example.model.PartitionConfig
import com.example.model.ServerServiceItem
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.UUID

enum class AppNavTab(val title: String, val icon: String) {
    DESKTOP("Phonix Desktop", "desktop"),
    COOLING_POWER("Cooling & DC Power", "bolt"),
    GAMING("Gaming & x86", "gamepad"),
    SERVERS("Server Suite", "dns"),
    PARTITIONS("Storage & Boot", "storage"),
    DISTROS("Linux Distros", "apps")
}

enum class ContainerStatus {
    STOPPED, STARTING, RUNNING, RESTARTING
}

data class SystemTelemetry(
    val cpuPercent: Int = 22,
    val ramUsedMb: Int = 1120,
    val ramTotalMb: Int = 3840,
    val swapUsedMb: Int = 210,
    val swapTotalMb: Int = 2048,
    val batteryTempC: Float = 35.4f,
    val batteryLevel: Int = 88,
    val isCharging: Boolean = true,
    val netDownloadKbps: Float = 142.5f,
    val netUploadKbps: Float = 88.2f
)

data class PhonixUiState(
    val activeTab: AppNavTab = AppNavTab.DESKTOP,
    val containerStatus: ContainerStatus = ContainerStatus.RUNNING,
    val availableDistros: List<LinuxDistro> = DistroCatalog.distros,
    val currentDistro: LinuxDistro = DistroCatalog.distros.first(),
    val partitionConfig: PartitionConfig = PartitionConfig(),
    val hwConfig: HardwareAccelerationConfig = HardwareAccelerationConfig(),
    val serverServices: List<ServerServiceItem> = DefaultServerCatalog.getDefaultServices(),
    val telemetry: SystemTelemetry = SystemTelemetry(),
    val uptimeSeconds: Long = 4820L,
    val localIpAddress: String = "192.168.1.105",
    val windows: List<DesktopWindow> = listOf(
        DesktopWindow(
            id = "win_browser",
            type = WindowType.BROWSER,
            title = "Phonix Web Browser (Chromium / Firefox)",
            posX = 20f,
            posY = 20f,
            width = 390f,
            height = 360f,
            zIndex = 2f
        ),
        DesktopWindow(
            id = "win_term",
            type = WindowType.TERMINAL,
            title = "Phonix Terminal (bash)",
            posX = 50f,
            posY = 50f,
            width = 380f,
            height = 350f,
            zIndex = 1f,
            isMinimized = true
        )
    ),
    val activeWindowId: String? = "win_browser",
    val isStartMenuOpen: Boolean = false,
    val startMenuSearchQuery: String = "",
    val terminalLines: List<TerminalLine> = emptyList(),
    val terminalInput: String = "",
    val isBenchmarkRunning: Boolean = false,
    val benchmarkLog: List<String> = emptyList(),
    val benchmarkScore: Int = 0,
    val isPartitioningActive: Boolean = false,
    val partitioningProgress: Float = 0f,
    val partitioningStatusText: String = "",
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
    val customization: DesktopCustomization = DesktopCustomization()
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
    val recommendedGpu: GpuDriverType,
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
            recommendedGpu = GpuDriverType.MALI_DRI3,
            recommendedLinuxGb = 16f,
            recommendedSwapGb = 2f,
            dexDockSupported = true,
            batteryGuidance = "S8/Note 8 batteries are 7+ years old. When running 24/7 as a server, keep phone on a flat surface, avoid thick cases, and use a 5V/2A standard charger rather than Fast Charge to prevent battery heat."
        ),
        DeviceProfile(
            id = "samsung_s8_snapdragon",
            name = "Samsung Galaxy S8 / Note 8 (Snapdragon)",
            chip = "Qualcomm Snapdragon 835 (Adreno 540)",
            ramGb = 4,
            totalStorageGb = 64f,
            recommendedGpu = GpuDriverType.VIRGL_GLES,
            recommendedLinuxGb = 16f,
            recommendedSwapGb = 2f,
            dexDockSupported = true,
            batteryGuidance = "Snapdragon 835 supports VirGL GLES 3.1 hardware acceleration. Supports full Samsung DeX HDMI dock 1080p desktop mode."
        ),
        DeviceProfile(
            id = "samsung_s9_s10",
            name = "Samsung Galaxy S9 / S10 / Note 9 (6-8GB RAM)",
            chip = "Snapdragon 845/855 or Exynos 9810/9820",
            ramGb = 6,
            totalStorageGb = 128f,
            recommendedGpu = GpuDriverType.TURNIP_ZINK,
            recommendedLinuxGb = 24f,
            recommendedSwapGb = 2f,
            dexDockSupported = true,
            batteryGuidance = "High performance headroom. Easily handles full desktop multitasking, 1080p video, and multiple server containers simultaneously."
        ),
        DeviceProfile(
            id = "samsung_s20_newer",
            name = "Samsung Galaxy S20 / Note 20 & Newer",
            chip = "Snapdragon 865+ / Exynos 990+ (8-12GB RAM)",
            ramGb = 8,
            totalStorageGb = 128f,
            recommendedGpu = GpuDriverType.TURNIP_ZINK,
            recommendedLinuxGb = 32f,
            recommendedSwapGb = 4f,
            dexDockSupported = true,
            batteryGuidance = "Modern flagship power. Can run full Docker containers, Nextcloud NAS, and 4K external desktop displays with ease."
        ),
        DeviceProfile(
            id = "generic_older_phone",
            name = "Other Older Android Phone (2-3GB RAM)",
            chip = "Legacy ARMv7/ARMv8 Octa/Quad Core",
            ramGb = 3,
            totalStorageGb = 32f,
            recommendedGpu = GpuDriverType.VIRGL_GLES,
            recommendedLinuxGb = 8f,
            recommendedSwapGb = 2f,
            dexDockSupported = false,
            batteryGuidance = "Use Alpine MiniPC or Phonix Core with zRAM swap to avoid memory exhaustion on older 2GB/3GB phones."
        )
    )

    val defaultProfile = profiles[0]
}

class PhonixViewModel(application: Application) : AndroidViewModel(application) {

    private val emulator = LinuxSystemEmulator()
    private val _uiState = MutableStateFlow(PhonixUiState())
    val uiState: StateFlow<PhonixUiState> = _uiState.asStateFlow()

    private var telemetryJob: Job? = null
    private var maxZIndex: Float = 3f

    init {
        detectLocalIp()
        initTerminalWelcome()
        startTelemetryLoop()
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
                        " Phonix OS 12.4 (Debian Bookworm ARM64) [Live Desktop]",
                        TerminalLine.LineType.ACCENT
                    ),
                    TerminalLine(
                        " Kernel: 5.15.148-phonix-arm64 | VirGL 3D Acceleration: Active",
                        TerminalLine.LineType.SYSTEM
                    ),
                    TerminalLine(
                        " Type 'help' for built-in tools or 'distros' for Linux OS catalog.",
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
            var counter = 0
            while (isActive) {
                delay(1500L)
                counter++
                _uiState.update { state ->
                    val jitterCpu = (18 + (Math.sin(counter * 0.4) * 12).toInt()).coerceIn(5, 95)
                    val jitterRam = 1100 + (counter * 3 % 180)
                    val baseTemp = if (state.coolingState.activeMode == CoolingMode.ACTIVE_MODDED_COOLING) 34.2f else 39.8f
                    val jitterTemp = (baseTemp + Math.sin(counter * 0.2).toFloat() * 1.5f)

                    state.copy(
                        telemetry = state.telemetry.copy(
                            cpuPercent = jitterCpu,
                            ramUsedMb = jitterRam,
                            batteryTempC = jitterTemp,
                            netDownloadKbps = (80f + (counter % 5) * 45f),
                            netUploadKbps = (30f + (counter % 3) * 22f)
                        ),
                        uptimeSeconds = state.uptimeSeconds + 1L
                    )
                }
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
                width = if (type == WindowType.BROWSER) 400f else 360f,
                height = if (type == WindowType.BROWSER) 380f else 340f,
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
                            posX = (win.posX + deltaX).coerceAtLeast(0f),
                            posY = (win.posY + deltaY).coerceAtLeast(0f)
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

        val state = _uiState.value
        val response = emulator.executeCommand(
            cmdRaw = trimmed,
            ipAddress = state.localIpAddress,
            partitionConfig = state.partitionConfig,
            hwConfig = state.hwConfig,
            services = state.serverServices,
            uptimeSeconds = state.uptimeSeconds
        )

        _uiState.update { curr ->
            curr.copy(
                terminalLines = curr.terminalLines + response,
                terminalInput = ""
            )
        }
    }

    fun clearTerminal() {
        _uiState.update { it.copy(terminalLines = emptyList()) }
    }

    // Server Management
    fun toggleServerService(serviceId: String) {
        _uiState.update { state ->
            val updated = state.serverServices.map { item ->
                if (item.id == serviceId) {
                    item.copy(isRunning = !item.isRunning)
                } else item
            }
            state.copy(
                serverServices = updated,
                notificationMessage = "${updated.find { it.id == serviceId }?.name} ${if (updated.find { it.id == serviceId }?.isRunning == true) "started" else "stopped"}"
            )
        }
    }

    fun toggleService(serviceId: String) = toggleServerService(serviceId)

    fun toggleAutoStart(serviceId: String) {
        _uiState.update { state ->
            state.copy(
                serverServices = state.serverServices.map {
                    if (it.id == serviceId) it.copy(autoStartOnBoot = !it.autoStartOnBoot) else it
                }
            )
        }
    }

    // Hardware Acceleration Settings
    fun updateGpuDriver(driver: GpuDriverType) {
        _uiState.update { state ->
            state.copy(
                hwConfig = state.hwConfig.copy(gpuDriver = driver),
                notificationMessage = "GPU Driver switched to ${driver.label}"
            )
        }
    }

    fun setGpuDriver(driver: GpuDriverType) = updateGpuDriver(driver)
    fun selectTab(tab: AppNavTab) = setActiveTab(tab)

    fun updateResolution(res: DesktopResolution) {
        _uiState.update { state ->
            state.copy(
                hwConfig = state.hwConfig.copy(resolution = res),
                notificationMessage = "Resolution adjusted to ${res.label}"
            )
        }
    }

    fun toggleHardwareSetting(key: String, enabled: Boolean) {
        _uiState.update { state ->
            val hw = state.hwConfig
            val updated = when (key) {
                "wakelock" -> hw.copy(isWakeLockActive = enabled)
                "wifilock" -> hw.copy(isWifiLockActive = enabled)
                "dock" -> hw.copy(externalDisplayDockMode = enabled)
                "vsync" -> hw.copy(isVSyncEnabled = enabled)
                else -> hw
            }
            state.copy(hwConfig = updated)
        }
        if (key == "wakelock") {
            if (enabled) {
                startBackgroundServiceIfActive()
            } else {
                stopBackgroundService()
            }
        }
    }

    fun updateGovernor(gov: String) {
        _uiState.update { state ->
            state.copy(
                hwConfig = state.hwConfig.copy(cpuGovernor = gov),
                notificationMessage = "CPU Governor set to $gov"
            )
        }
    }

    fun setResolution(res: DesktopResolution) = updateResolution(res)

    fun toggleBackgroundService(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                hwConfig = state.hwConfig.copy(isWakeLockActive = enabled),
                notificationMessage = if (enabled) "24/7 background mode enabled" else "Background mode disabled"
            )
        }
        if (enabled) {
            startBackgroundServiceIfActive()
        } else {
            stopBackgroundService()
        }
    }

    fun run3dGpuBenchmark() {
        if (_uiState.value.isBenchmarkRunning) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBenchmarkRunning = true,
                    benchmarkLog = listOf("Initializing glmark2-es2 VirGL OpenGL ES 3.1 Benchmark..."),
                    benchmarkScore = 0
                )
            }

            val testSteps = listOf(
                "Running Scene [build]: shader=phong, bump=high -> 60.1 FPS",
                "Running Scene [texture]: filter=mipmap, aniso=16x -> 58.4 FPS",
                "Running Scene [shading]: gouraud+blinn-phong -> 59.9 FPS",
                "Running Scene [bump]: heightmap=parallax, normal=true -> 57.2 FPS",
                "Running Scene [effect2d]: kernel=blur, edge-detect -> 61.0 FPS",
                "Running Scene [pulsar]: quads=4096, virgl-instancing -> 58.8 FPS",
                "Running Scene [terrain]: frustum-cull, tessellation -> 54.6 FPS"
            )

            for (step in testSteps) {
                delay(600L)
                _uiState.update {
                    it.copy(benchmarkLog = it.benchmarkLog + step)
                }
            }

            val baseScore = when (_uiState.value.hwConfig.gpuDriver) {
                GpuDriverType.VIRGL_GLES -> 1840
                GpuDriverType.TURNIP_ZINK -> 2450
                GpuDriverType.MALI_DRI3 -> 1720
                GpuDriverType.LLVMPIPE_SOFTWARE -> 420
            }
            val finalScore = baseScore + (Math.random() * 80).toInt()

            _uiState.update {
                it.copy(
                    isBenchmarkRunning = false,
                    benchmarkScore = finalScore,
                    benchmarkLog = it.benchmarkLog + "=========================================" +
                            "glmark2-es2 Score: $finalScore" +
                            "Hardware Acceleration: PASSED (60 FPS VSync stable)"
                )
            }
        }
    }

    // Partition Manager
    fun updatePartitionSizes(linuxGb: Float, swapGb: Float) {
        _uiState.update { state ->
            val total = state.partitionConfig.totalDeviceStorageGb
            val sysUsed = state.partitionConfig.androidSystemUsedGb
            val maxAvail = total - sysUsed - 1.0f // reserve 1GB for Android
            val clampedLinux = linuxGb.coerceIn(2.0f, maxAvail - swapGb)
            val clampedSwap = swapGb.coerceIn(0.5f, 4.0f)

            state.copy(
                partitionConfig = state.partitionConfig.copy(
                    allocatedLinuxGb = clampedLinux,
                    allocatedSwapGb = clampedSwap
                )
            )
        }
    }

    fun runPartitioningRoutine() {
        if (_uiState.value.isPartitioningActive) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPartitioningActive = true,
                    partitioningProgress = 0.05f,
                    partitioningStatusText = "Unmounting old containers..."
                )
            }
            delay(500L)
            _uiState.update {
                it.copy(
                    partitioningProgress = 0.25f,
                    partitioningStatusText = "Allocating sparse rootfs container (${it.partitionConfig.allocatedLinuxGb.toInt()} GB)..."
                )
            }
            delay(700L)
            _uiState.update {
                it.copy(
                    partitioningProgress = 0.55f,
                    partitioningStatusText = "Formatting EXT4 filesystem with flash wear-leveling (barrier=0)..."
                )
            }
            delay(800L)
            _uiState.update {
                it.copy(
                    partitioningProgress = 0.80f,
                    partitioningStatusText = "Creating zRAM Swap partition (${it.partitionConfig.allocatedSwapGb.toInt()} GB)..."
                )
            }
            delay(600L)
            _uiState.update {
                it.copy(
                    partitioningProgress = 1.0f,
                    partitioningStatusText = "Mounting /dev/rootfs and /sdcard/PhonixShare bind points. Ready!",
                    isPartitioningActive = false,
                    partitionConfig = it.partitionConfig.copy(isPartitionMounted = true, isExt4Formatted = true),
                    notificationMessage = "Storage partitioned and mounted successfully!"
                )
            }
        }
    }

    // Container Lifecycle
    fun restartContainer() {
        viewModelScope.launch {
            _uiState.update { it.copy(containerStatus = ContainerStatus.RESTARTING) }
            delay(1200L)
            _uiState.update {
                it.copy(
                    containerStatus = ContainerStatus.RUNNING,
                    uptimeSeconds = 0L,
                    notificationMessage = "Phonix container restarted"
                )
            }
        }
    }

    fun toggleContainerPower() {
        viewModelScope.launch {
            if (_uiState.value.containerStatus == ContainerStatus.RUNNING) {
                _uiState.update { it.copy(containerStatus = ContainerStatus.STOPPED) }
                stopBackgroundService()
            } else {
                _uiState.update { it.copy(containerStatus = ContainerStatus.STARTING) }
                delay(900L)
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

    // 1-Click Distro Downloader Routine
    fun downloadDistro(distroId: String) {
        viewModelScope.launch {
            val distro = _uiState.value.availableDistros.find { it.id == distroId } ?: return@launch
            if (distro.isInstalled || distro.isDownloading) return@launch

            // Mark downloading
            _uiState.update { state ->
                val updatedList = state.availableDistros.map {
                    if (it.id == distroId) it.copy(isDownloading = true, downloadProgress = 0.05f, downloadSpeedMbps = 24.2f)
                    else it
                }
                state.copy(
                    availableDistros = updatedList,
                    notificationMessage = "Downloading rootfs for ${distro.name}..."
                )
            }

            // Simulate progress chunks
            val steps = listOf(0.20f, 0.45f, 0.70f, 0.90f, 1.0f)
            for (step in steps) {
                delay(400L)
                _uiState.update { state ->
                    val speed = (20f + (Math.random() * 15)).toFloat()
                    val updatedList = state.availableDistros.map {
                        if (it.id == distroId) it.copy(downloadProgress = step, downloadSpeedMbps = speed)
                        else it
                    }
                    state.copy(availableDistros = updatedList)
                }
            }

            delay(300L)

            // Mark completed
            _uiState.update { state ->
                val updatedList = state.availableDistros.map {
                    if (it.id == distroId) it.copy(
                        isInstalled = true,
                        isDownloading = false,
                        downloadProgress = 1.0f,
                        downloadSpeedMbps = 0f
                    ) else it
                }
                val downloaded = updatedList.first { it.id == distroId }
                state.copy(
                    availableDistros = updatedList,
                    currentDistro = downloaded,
                    notificationMessage = "✓ ${downloaded.name} rootfs verified & installed! Ready to boot."
                )
            }
        }
    }

    // Switch and immediately boot into a distro
    fun switchAndBootDistro(distro: LinuxDistro) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    currentDistro = distro,
                    isBootSplashActive = true,
                    bootLogs = listOf(
                        "[  0.000000] Linux version 5.15.148-phonix-arm64 (gcc 12.2.0) #1 SMP PREEMPT",
                        "[  0.001240] Loading rootfs: ${distro.base} (${distro.desktopEnvironment})",
                        "[  0.014502] CPU Cores: 8x ARM64 online | Thermal Throttling: Bypassed",
                        "[  0.082104] VirGL 3D GLES Hardware Acceleration Context: Bound",
                        "[  0.154210] Mounting /dev/block/loop0 (${distro.name}) to /",
                        "[  0.281402] Initializing ${distro.desktopEnvironment}...",
                        "[  0.640120] Network interfaces online: eth0, lo, virbr0",
                        "[  1.120042] Reached target Graphical Interface. Welcome to ${distro.name}!"
                    )
                )
            }
            delay(2200L)
            _uiState.update { state ->
                state.copy(
                    isBootSplashActive = false,
                    activeTab = AppNavTab.DESKTOP,
                    containerStatus = ContainerStatus.RUNNING,
                    notificationMessage = "Booted directly into ${distro.name}"
                )
            }
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
            val updatedPartition = state.partitionConfig.copy(
                totalDeviceStorageGb = profile.totalStorageGb,
                allocatedLinuxGb = profile.recommendedLinuxGb,
                allocatedSwapGb = profile.recommendedSwapGb
            )
            val updatedHw = state.hwConfig.copy(
                gpuDriver = profile.recommendedGpu
            )
            state.copy(
                selectedDeviceProfile = profile,
                partitionConfig = updatedPartition,
                hwConfig = updatedHw,
                notificationMessage = "Configured presets for ${profile.name}"
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

    fun setCoolingMode(mode: CoolingMode) {
        _uiState.update { state ->
            val updatedCooling = state.coolingState.copy(
                activeMode = mode,
                currentCpuClockGhz = mode.maxClockGhz,
                socTempC = if (mode == CoolingMode.ACTIVE_MODDED_COOLING) 34.5f else 41.2f
            )
            state.copy(
                coolingState = updatedCooling,
                hwConfig = state.hwConfig.copy(
                    cpuGovernor = if (mode.isThrottleBypassed) "performance" else "schedutil"
                ),
                notificationMessage = "Cooling governor tuned to: ${mode.title}"
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

    fun runWizardDeployment(onFinished: () -> Unit) {
        if (_uiState.value.isWizardDeploying) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isWizardDeploying = true,
                    wizardProgress = 0.05f,
                    wizardStatusMessage = "Debloating background OEM & carrier packages...",
                    wizardLogs = listOf(
                        "➜ Initializing Phonix OS Engine for ${it.selectedDeviceProfile.name}...",
                        "✔ Disabling Samsung Game Optimizing Service (GOS throttle removed)",
                        "✔ Disabling Bixby Suite & Wakeup daemons (+310 MB RAM freed)",
                        "✔ Disabling Facebook Analytics & Carrier Telemetry (+420 MB RAM freed)",
                        "★ Reclaimed 1,350 MB physical RAM for Linux subsystem!"
                    )
                )
            }
            delay(650L)

            _uiState.update {
                it.copy(
                    wizardProgress = 0.25f,
                    wizardStatusMessage = "Partitioning sparse EXT4 container (${it.partitionConfig.allocatedLinuxGb.toInt()} GB)...",
                    wizardLogs = it.wizardLogs + "✔ Created sparse virtual rootfs container image at /data/rootfs.img"
                )
            }
            delay(700L)

            _uiState.update {
                it.copy(
                    wizardProgress = 0.50f,
                    wizardStatusMessage = "Applying eMMC wear reduction (noatime, barrier=0)...",
                    wizardLogs = it.wizardLogs + "✔ Formatted EXT4 with flash endurance flags (barrier=0 wear-leveling)"
                )
            }
            delay(600L)

            _uiState.update {
                it.copy(
                    wizardProgress = 0.70f,
                    wizardStatusMessage = "Allocating ${it.partitionConfig.allocatedSwapGb.toInt()} GB zRAM swap partition...",
                    wizardLogs = it.wizardLogs + "✔ zRAM compressed memory paging activated (safeguard against 4GB RAM OOM crash)"
                )
            }
            delay(700L)

            _uiState.update {
                it.copy(
                    wizardProgress = 0.88f,
                    wizardStatusMessage = "Binding GPU acceleration context (${it.hwConfig.gpuDriver.label})...",
                    wizardLogs = it.wizardLogs + "✔ VirGL / DRI3 3D graphics hardware pipeline verified (60 FPS vsync)"
                )
            }
            delay(600L)

            _uiState.update {
                it.copy(
                    wizardProgress = 1.0f,
                    wizardStatusMessage = "Setup complete! Starting 24/7 background WakeLock server...",
                    wizardLogs = it.wizardLogs +
                            "✔ Nginx, SSH (:2222), and Samba servers started" +
                            "★ Installation successfully completed for ${it.selectedDeviceProfile.name}!",
                    isWizardDeploying = false,
                    partitionConfig = it.partitionConfig.copy(isPartitionMounted = true, isExt4Formatted = true),
                    containerStatus = ContainerStatus.RUNNING,
                    notificationMessage = "Phonix OS successfully installed!"
                )
            }
            delay(400L)
            onFinished()
        }
    }

    // Direct Linux Distro Boot Methods
    fun triggerLinuxRebootAnimation() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBootSplashActive = true,
                    bootLogs = listOf(
                        "[  0.000000] Linux version 5.15.148-phonix-arm64 (gcc 12.2.0) #1 SMP PREEMPT",
                        "[  0.001240] Machine model: ${it.selectedDeviceProfile.name}",
                        "[  0.014502] CPU: ARMv8 (8 cores online) @ 2.40 GHz",
                        "[  0.082104] Memory: ${it.selectedDeviceProfile.ramGb}GB physical + ${it.partitionConfig.allocatedSwapGb.toInt()}GB zRAM paging",
                        "[  0.154210] virglrenderer: Hardware 3D GLES 3.1 context bound to host GPU",
                        "[  0.281402] EXT4-fs (rootfs.img): mounted filesystem with ordered data mode",
                        "[  0.412091] Systemd-udevd: USB mouse and keyboard detected",
                        "[  0.640120] Starting OpenSSH server daemon on port 2222...",
                        "[  0.781040] Starting Nginx HTTP web server on port 8080...",
                        "[  0.920401] Starting Samba Windows Network Share on port 4455...",
                        "[  1.120042] Reached target Graphical Interface. Initializing Phonix Desktop..."
                    )
                )
            }
            delay(2400L)
            _uiState.update {
                it.copy(
                    isBootSplashActive = false,
                    activeTab = AppNavTab.DESKTOP,
                    containerStatus = ContainerStatus.RUNNING,
                    notificationMessage = "Booted directly into ${it.currentDistro.name}"
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
                        posY = (sc.posY + deltaY).coerceAtLeast(0f)
                    )
                } else sc
            }
            state.copy(customization = state.customization.copy(shortcuts = updated))
        }
    }

    fun dropShortcut(shortcutId: String, currentX: Float, currentY: Float) {
        _uiState.update { state ->
            val lockGrid = state.customization.lockIconsToGrid
            val updated = state.customization.shortcuts.map { sc ->
                if (sc.id == shortcutId) {
                    if (lockGrid) {
                        val col = Math.round((currentX - 16f) / 84f).coerceIn(0, 5).toInt()
                        val row = Math.round((currentY - 16f) / 84f).coerceIn(0, 7).toInt()
                        val snappedX = 16f + (col * 84f)
                        val snappedY = 16f + (row * 84f)
                        sc.copy(
                            gridCol = col,
                            gridRow = row,
                            posX = snappedX,
                            posY = snappedY
                        )
                    } else {
                        sc.copy(
                            posX = currentX.coerceAtLeast(0f),
                            posY = currentY.coerceAtLeast(0f)
                        )
                    }
                } else sc
            }
            state.copy(
                customization = state.customization.copy(shortcuts = updated),
                notificationMessage = if (lockGrid) "Shortcut locked to grid" else "Shortcut moved"
            )
        }
    }

    fun toggleLockIconsToGrid(enabled: Boolean) {
        _uiState.update { state ->
            val shortcuts = if (enabled) {
                state.customization.shortcuts.map { sc ->
                    val col = Math.round((sc.posX - 16f) / 84f).coerceIn(0, 5).toInt()
                    val row = Math.round((sc.posY - 16f) / 84f).coerceIn(0, 7).toInt()
                    sc.copy(
                        gridCol = col,
                        gridRow = row,
                        posX = 16f + (col * 84f),
                        posY = 16f + (row * 84f)
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
                notificationMessage = if (enabled) "Grid Snapping: Locked to Grid (84dp)" else "Grid Snapping: Freeform Movement"
            )
        }
    }

    fun resetShortcutGrid() {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(
                    shortcuts = DesktopShortcut.getDefaultShortcuts()
                ),
                notificationMessage = "Desktop shortcuts reset to default grid"
            )
        }
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
}
