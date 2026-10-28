package com.example.model

enum class GpuDriverType(val label: String, val rendererName: String, val description: String) {
    VIRGL_GLES("VirGL (GLES 3.1)", "VirGL 3D Renderer over OpenGL ES", "Translates Linux OpenGL calls directly into Android OpenGL ES 3.1 GPU context. Best general compatibility."),
    TURNIP_ZINK("Turnip + Zink", "Adreno Open-Source Vulkan Driver", "Direct hardware Vulkan acceleration for Qualcomm Snapdragon GPUs. Up to 3.5x higher FPS."),
    MALI_DRI3("Mali DRI3", "ARM Mali Direct Rendering", "Direct DRI3 buffer sharing for MediaTek & Exynos Mali GPUs."),
    LLVMPIPE_SOFTWARE("LLVMpipe (CPU)", "Mesa Software Rasterizer", "CPU-based software rendering fallback. Useful for extremely old devices without GLES 3.0.")
}

enum class DesktopResolution(val label: String, val width: Int, val height: Int, val notes: String) {
    PHONE_NATIVE("Phone Native (Auto)", 0, 0, "Matches phone screen aspect ratio"),
    HD_720P("720p HD (1280x720)", 1280, 720, "Fastest performance on low-spec chips"),
    FHD_1080P("1080p FHD (1920x1080)", 1920, 1080, "Optimal for HDMI/USB-C Docks & Monitors"),
    QHD_1440P("1440p 2K (2560x1440)", 2560, 1440, "High DPI desktop experience"),
    UHD_4K("4K UHD (3840x2160)", 3840, 2160, "Requires Snapdragon 845+ or high-end chip")
}

data class HardwareAccelerationConfig(
    val gpuDriver: GpuDriverType = GpuDriverType.VIRGL_GLES,
    val resolution: DesktopResolution = DesktopResolution.PHONE_NATIVE,
    val isHardwareDecodersEnabled: Boolean = true,
    val isVSyncEnabled: Boolean = true,
    val cpuGovernor: String = "schedutil", // schedutil, performance, powersave
    val thermalLimitC: Int = 42,
    val isWakeLockActive: Boolean = true,
    val isWifiLockActive: Boolean = true,
    val batteryHealthProtectMode: Boolean = true, // Notifies when battery is kept at 100% 24/7
    val externalDisplayDockMode: Boolean = false,
    val simulateGlMarkScore: Int = 1845
)
