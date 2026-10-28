package com.example.model

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val activityName: String?,
    val label: String,
    val iconDrawable: Drawable? = null,
    val isSystemApp: Boolean = false,
    val versionName: String? = null
)

enum class MiniPcProfile(val title: String, val subtitle: String, val description: String) {
    EVERYDAY_DESKTOP(
        title = "Everyday Workstation",
        subtitle = "Office, Web & Media",
        description = "Balanced CPU frequency, hardware-accelerated Chrome, file management, and streaming media."
    ),
    HOMELAB_SERVER(
        title = "Homelab & Linux Server",
        subtitle = "Headless 24/7 Appliance",
        description = "Stripped GUI overhead, automated service restarts, SSH/FTP server stacks, and PRoot containers."
    ),
    GAMING_EMULATION(
        title = "Retro Gaming & Emulation",
        subtitle = "Peak GPU Clocks",
        description = "Unthrottled GPU governor, Turnip/VirGL drivers, gamepad mapping, and active cooling fan profile."
    ),
    MINIMAL_LEAN(
        title = "Ultra-Lean Zero Bloat",
        subtitle = "Max Storage & Free RAM",
        description = "Bypasses all non-essential phone services to maximize flash longevity and RAM for heavy multitasking."
    )
}

enum class AppCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val estStorageMb: Int,
    val isDefault: Boolean,
    val isHostPhoneApp: Boolean
) {
    WEB_BROWSER(
        id = "cat_browser",
        title = "Google Chrome / Web Browser",
        subtitle = "Uses phone's native browser (Zero extra flash storage)",
        estStorageMb = 0,
        isDefault = true,
        isHostPhoneApp = true
    ),
    FILE_MANAGER(
        id = "cat_files",
        title = "File Explorer & Storage",
        subtitle = "Browse external USB, microSD, and internal downloads",
        estStorageMb = 0,
        isDefault = true,
        isHostPhoneApp = true
    ),
    APP_STORE(
        id = "cat_store",
        title = "App Store (Google Play / F-Droid)",
        subtitle = "Download new apps, utilities, and updates on demand",
        estStorageMb = 0,
        isDefault = true,
        isHostPhoneApp = true
    ),
    MEDIA_GALLERY(
        id = "cat_media",
        title = "Photos, Camera & Media",
        subtitle = "Launch phone's native gallery and media player",
        estStorageMb = 0,
        isDefault = true,
        isHostPhoneApp = true
    ),
    LINUX_TERMINAL(
        id = "cat_terminal",
        title = "Phonix Linux Terminal",
        subtitle = "Bash command line with apt/dpkg package manager",
        estStorageMb = 35,
        isDefault = true,
        isHostPhoneApp = false
    ),
    HARDWARE_MONITOR(
        id = "cat_monitor",
        title = "Hardware & Cooling Governor",
        subtitle = "Live SoC temperature, CPU frequency, and DC power control",
        estStorageMb = 12,
        isDefault = true,
        isHostPhoneApp = false
    ),
    LINUX_DISTROS(
        id = "cat_distros",
        title = "Linux Distro Hub",
        subtitle = "1-Click Ubuntu, Debian, Arch & Kali container downloads",
        estStorageMb = 180,
        isDefault = false,
        isHostPhoneApp = false
    ),
    SERVER_CENTER(
        id = "cat_server",
        title = "Server Hub & Homelab Daemons",
        subtitle = "Web server, FTP, SSH, and Docker-compatible server stacks",
        estStorageMb = 45,
        isDefault = false,
        isHostPhoneApp = false
    )
}

enum class DesktopThemePreset(val title: String, val description: String) {
    WINDOWS_11_DARK("Windows 11 Dark Fluent", "Deep slate acrylic panels with electric blue accents"),
    CYBER_PHOENIX("Cyber Phoenix Neon", "Vibrant amber-orange glow inspired by reclaimed silicon"),
    CLEAN_SLATE("Clean Slate Minimal", "Pure titanium grayscale palette for focused productivity"),
    OLED_OBSIDIAN("AMOLED Obsidian", "Pure black background for maximum contrast and zero backlight bleed")
}

data class OobeSetupConfig(
    val deviceName: String = "Phonix-MiniPC",
    val profile: MiniPcProfile = MiniPcProfile.EVERYDAY_DESKTOP,
    val selectedCategories: Set<AppCategory> = setOf(
        AppCategory.WEB_BROWSER,
        AppCategory.FILE_MANAGER,
        AppCategory.APP_STORE,
        AppCategory.MEDIA_GALLERY,
        AppCategory.LINUX_TERMINAL,
        AppCategory.HARDWARE_MONITOR
    ),
    val enableAggressiveDebloat: Boolean = true,
    val themePreset: DesktopThemePreset = DesktopThemePreset.WINDOWS_11_DARK,
    val taskbarCentered: Boolean = true
)
