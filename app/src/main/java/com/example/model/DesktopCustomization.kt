package com.example.model

import androidx.compose.ui.graphics.Color

enum class DesktopWallpaper(val title: String, val description: String) {
    PHOENIX_CYBER_DEFAULT("Phoenix Cybernetic (Default)", "Rising neon phoenix emblem with electric blue and deep black circuitry"),
    MATRIX_DEEP_BLUE("Matrix Circuit Grid", "Futuristic deep blue grid with pulsating neon data tracks"),
    OBSIDIAN_NIGHT("Obsidian Dark AMOLED", "Ultra-clean pure black backdrop for maximum OLED power savings and contrast"),
    RETRO_CLASSIC_TEAL("Retro PC Classic Teal", "Iconic vintage 90s workstation desktop teal"),
    SYNTHWAVE_NEON("Cyber Neon Horizon", "Atmospheric electric indigo and cyan twilight gradient")
}

enum class DesktopAccentColor(val title: String, val color: Color, val hex: String) {
    ELECTRIC_BLUE("Electric Blue", Color(0xFF0284C7), "#0284C7"),
    CYBER_CYAN("Cyber Cyan", Color(0xFF06B6D4), "#06B6D4"),
    EMERALD_LINUX("Emerald Linux", Color(0xFF10B981), "#10B981"),
    AMBER_EMBER("Amber Ember", Color(0xFFF59E0B), "#F59E0B"),
    CRIMSON_FLAME("Crimson Flame", Color(0xFFEF4444), "#EF4444"),
    PURPLE_NEBULA("Purple Nebula", Color(0xFF8B5CF6), "#8B5CF6")
}

enum class IconSizeMode(val title: String, val sizeDp: Int) {
    SMALL("Compact (36dp)", 36),
    MEDIUM("Standard (48dp)", 48),
    LARGE("Comfortable (60dp)", 60)
}

enum class TaskbarPosition {
    BOTTOM,
    TOP
}

enum class TaskbarStyle(val title: String) {
    SOLID_DARK("Deep Black Carbon"),
    GLASS_TRANSLUCENT("Frosted Aero Glass"),
    WINDOWS_CLASSIC("Classic 90s Beveled")
}

enum class MonitorDisplayMode(val title: String, val label: String, val isWidescreen: Boolean) {
    PHONE_PORTRAIT("Phone Portrait", "Handheld Screen", false),
    MONITOR_FHD_16_9("External Monitor (16:9)", "1080p FHD HDMI / USB-C Dock", true),
    ULTRAWIDE_21_9("Ultrawide Workstation (21:9)", "1440p UWQHD Dock", true)
}

data class DesktopShortcut(
    val id: String,
    val title: String,
    val windowType: WindowType,
    val gridCol: Int,
    val gridRow: Int,
    val posX: Float = 0f,
    val posY: Float = 0f
) {
    companion object {
        fun getDefaultShortcuts(): List<DesktopShortcut> = listOf(
            DesktopShortcut("sc_browser", "Web Browser", WindowType.BROWSER, gridCol = 0, gridRow = 0, posX = 16f, posY = 16f),
            DesktopShortcut("sc_distro", "Distro Store", WindowType.DISTRO_STORE, gridCol = 0, gridRow = 1, posX = 16f, posY = 100f),
            DesktopShortcut("sc_terminal", "Terminal", WindowType.TERMINAL, gridCol = 0, gridRow = 2, posX = 16f, posY = 184f),
            DesktopShortcut("sc_personalize", "Personalize", WindowType.PERSONALIZE, gridCol = 0, gridRow = 3, posX = 16f, posY = 268f),
            DesktopShortcut("sc_files", "File Manager", WindowType.FILE_EXPLORER, gridCol = 0, gridRow = 4, posX = 16f, posY = 352f),
            DesktopShortcut("sc_server", "Server Hub", WindowType.SERVER_MANAGER, gridCol = 0, gridRow = 5, posX = 16f, posY = 436f),
            DesktopShortcut("sc_monitor", "Monitor", WindowType.RESOURCE_MONITOR, gridCol = 1, gridRow = 0, posX = 100f, posY = 16f),
            DesktopShortcut("sc_benchmark", "3D Benchmark", WindowType.BENCHMARK, gridCol = 1, gridRow = 1, posX = 100f, posY = 100f),
            DesktopShortcut("sc_notepad", "Notepad", WindowType.NOTEPAD, gridCol = 1, gridRow = 2, posX = 100f, posY = 184f)
        )
    }
}

data class DesktopCustomization(
    val wallpaper: DesktopWallpaper = DesktopWallpaper.PHOENIX_CYBER_DEFAULT,
    val accentColor: DesktopAccentColor = DesktopAccentColor.ELECTRIC_BLUE,
    val iconSize: IconSizeMode = IconSizeMode.MEDIUM,
    val taskbarPosition: TaskbarPosition = TaskbarPosition.BOTTOM,
    val taskbarStyle: TaskbarStyle = TaskbarStyle.SOLID_DARK,
    val displayMode: MonitorDisplayMode = MonitorDisplayMode.PHONE_PORTRAIT,
    val showQuickDistroWidget: Boolean = true,
    val showSystemMonitorWidget: Boolean = true,
    val showDesktopGrid: Boolean = true,
    val showDesktopLabels: Boolean = true,
    val showClockInTaskbar: Boolean = true,
    val showBatteryInTaskbar: Boolean = true,
    val lockIconsToGrid: Boolean = true,
    val shortcuts: List<DesktopShortcut> = DesktopShortcut.getDefaultShortcuts(),
    val areAllWindowsHidden: Boolean = false // Show Desktop toggle state
)
