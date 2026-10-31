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
    ),
    CYBERDECK_DEV(
        title = "Cyberdeck & Developer Station",
        subtitle = "Terminal, VS Code, Python & AI",
        description = "Full Linux development toolchain with offline AI assistant and Alpine musl compilers."
    )
}

/**
 * Who is actually setting this up. The answer is not cosmetic: it picks the starting use mode,
 * the workload profile and which bloat categories are pre-selected, all of which stay editable
 * in the later steps.
 */
enum class SetupPersona(
    val title: String,
    val subtitle: String,
    val outcome: String,
    val profile: MiniPcProfile,
    val applianceMode: Boolean,
    val trimHostPackages: Boolean
) {
    DAILY_PHONE(
        title = "This is my everyday phone",
        subtitle = "Daily driver",
        outcome = "Nothing on Android changes until you open Phonix: no wallpaper swap, no background service, no boot start. Only analytics and carrier-tracking packages are pre-ticked.",
        profile = MiniPcProfile.EVERYDAY_DESKTOP,
        applianceMode = false,
        trimHostPackages = false
    ),
    CYBERDECK_BUILD(
        title = "I'm building a cyberdeck",
        subtitle = "Dock, monitor, Linux",
        outcome = "Phonix becomes the machine: desktop appears on the monitor when it is plugged in, the Linux guest is ready to open, and the build gets the aggressive trim list. Every item stays untickable below.",
        profile = MiniPcProfile.CYBERDECK_DEV,
        applianceMode = true,
        trimHostPackages = true
    ),
    MINI_PC_OWNER(
        title = "I'm setting up a mini-PC",
        subtitle = "Screenless or reclaimed board",
        outcome = "This board is now a computer: it starts the desktop by itself, needs no phone UI, and the trim list removes everything the phone shell was only keeping alive.",
        profile = MiniPcProfile.HOMELAB_SERVER,
        applianceMode = true,
        trimHostPackages = true
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
        title = "Phonix Browser",
        subtitle = "The desktop's own private browser window (DuckDuckGo home, no Chrome needed)",
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
        subtitle = "Alpine Linux inside the app: no partition, no root",
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
    FLUENT_DARK("Fluent Dark", "Deep slate panels with electric blue accents"),
    CYBER_PHOENIX("Cyber Phoenix Neon", "Vibrant amber-orange glow inspired by reclaimed silicon"),
    CLEAN_SLATE("Clean Slate Minimal", "Pure titanium grayscale palette for focused productivity"),
    OLED_OBSIDIAN("AMOLED Obsidian", "Pure black background: an OLED panel draws no light for those pixels")
}

enum class PreferredBrowser(val title: String, val subtitle: String, val packageName: String?) {
    PHONIX_BROWSER("Phonix Browser", "Desktop WebView window with no account and no telemetry of its own. The sites you open still see you", null),
    FIREFOX("Mozilla Firefox", "Full extensions, tracking protection & open web engine", "org.mozilla.firefox"),
    CHROMIUM("Chromium", "Open-source Chromium engine for web apps", "org.chromium.chrome")
}

data class OobeSetupConfig(
    val deviceName: String = "Phonix-MiniPC",
    val profile: MiniPcProfile = MiniPcProfile.EVERYDAY_DESKTOP,
    // Media gallery, distro store and hardware monitor are deliberately absent: those surfaces
    // are the top-right OS card and Vitals widgets, so a desktop icon for them was a duplicate.
    val selectedCategories: Set<AppCategory> = setOf(
        AppCategory.WEB_BROWSER,
        AppCategory.FILE_MANAGER,
        AppCategory.APP_STORE,
        AppCategory.LINUX_TERMINAL
    ),
    val enableLinuxDesktop: Boolean = false,
    val preferredBrowser: PreferredBrowser = PreferredBrowser.PHONIX_BROWSER,
    val debloatChrome: Boolean = false,
    val enableAggressiveDebloat: Boolean = true,
    val themePreset: DesktopThemePreset = DesktopThemePreset.FLUENT_DARK,
    val taskbarCentered: Boolean = true,
    val deviceChanges: DeviceChangeChoices = DeviceChangeChoices(),
    val offlineLibraryTier: KnowledgeTier = KnowledgeTier.LIGHTWEIGHT
)

