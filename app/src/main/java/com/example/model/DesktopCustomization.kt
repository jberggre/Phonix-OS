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
    PURPLE_NEBULA("Purple Nebula", Color(0xFF8B5CF6), "#8B5CF6"),
    SLATE_MONOCHROME("Slate Titanium", Color(0xFF94A3B8), "#94A3B8")
}

enum class IconSizeMode(val title: String, val sizeDp: Int) {
    SMALL("Compact (36dp)", 36),
    MEDIUM("Standard (48dp)", 48),
    LARGE("Comfortable (60dp)", 60),
    EXTRA_LARGE("Large (76dp)", 76)
}

enum class ShortcutShape(val title: String, val cornerRadiusDp: Int) {
    SQUIRCLE("Fluent Squircle", 14),
    CIRCLE("Circle", 50),
    TILE("Windows 11 Tile", 6),
    HEXAGON("Hexagon", 8),
    GLASS_POD("Glass Pod", 22)
}

enum class ShortcutBgStyle(val title: String) {
    FROSTED_GLASS("Frosted Glass"),
    NEON_RIM("Neon Rim"),
    SOLID_DARK("Solid Carbon"),
    TRANSPARENT("Transparent")
}

enum class TaskbarPosition {
    BOTTOM,
    TOP
}

enum class TaskbarStyle(val title: String) {
    WINDOWS_11_FLUENT("Windows 11 Fluent Center"),
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
    val gridCol: Int = 0,
    val gridRow: Int = 0,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val hasCustomPosition: Boolean = false,
    val customSize: IconSizeMode? = null,
    val customShape: ShortcutShape = ShortcutShape.SQUIRCLE,
    val customColor: DesktopAccentColor? = null,
    val customBackground: ShortcutBgStyle = ShortcutBgStyle.FROSTED_GLASS,
    val launchPackage: String? = null,
    val isPinnedToTaskbar: Boolean = false,
    val isNativeApp: Boolean = false,
    val isAppStore: Boolean = false
) {
    companion object {
        fun createFromInstalledApp(
            app: InstalledAppInfo,
            posX: Float,
            posY: Float
        ): DesktopShortcut = DesktopShortcut(
            id = "app_${app.packageName.replace('.', '_')}",
            title = app.label,
            windowType = WindowType.BROWSER,
            posX = posX,
            posY = posY,
            hasCustomPosition = true,
            launchPackage = app.packageName,
            isNativeApp = true,
            customShape = ShortcutShape.SQUIRCLE
        )

        // Default shortcut layout placed strictly in the BOTTOM-LEFT QUADRANT.
        // Leaves the top half of the screen clear for the Phoenix wallpaper emblem,
        // and keeps the right side open for desktop vitals and quick widgets.
        fun getDefaultShortcuts(): List<DesktopShortcut> = listOf(
            // Column 0 (Left-most, Col 0, posX = 20f)
            DesktopShortcut(
                id = "sc_store",
                title = "App Store",
                windowType = WindowType.APP_STORE,
                gridCol = 0,
                gridRow = 0,
                posX = 20f,
                posY = 280f,
                customColor = DesktopAccentColor.CYBER_CYAN,
                launchPackage = "com.android.vending",
                isPinnedToTaskbar = true,
                isAppStore = true
            ),
            DesktopShortcut(
                id = "sc_browser",
                title = "Phonix Browser",
                windowType = WindowType.BROWSER,
                gridCol = 0,
                gridRow = 1,
                posX = 20f,
                posY = 368f,
                customColor = DesktopAccentColor.CYBER_CYAN,
                isPinnedToTaskbar = true
            ),
            DesktopShortcut(
                id = "sc_files",
                title = "File Explorer",
                windowType = WindowType.FILE_EXPLORER,
                gridCol = 0,
                gridRow = 2,
                posX = 20f,
                posY = 456f,
                customColor = DesktopAccentColor.AMBER_EMBER,
                launchPackage = "com.google.android.documentsui",
                isPinnedToTaskbar = true,
                isNativeApp = true
            ),
            DesktopShortcut(
                id = "sc_photos",
                title = "Photos & Media",
                windowType = WindowType.BROWSER,
                gridCol = 0,
                gridRow = 3,
                posX = 20f,
                posY = 544f,
                customColor = DesktopAccentColor.PURPLE_NEBULA,
                launchPackage = "com.google.android.apps.photos",
                isPinnedToTaskbar = false,
                isNativeApp = true
            ),

            // Column 1 (posX = 110f)
            DesktopShortcut(
                id = "sc_terminal",
                title = "Phonix Terminal",
                windowType = WindowType.TERMINAL,
                gridCol = 1,
                gridRow = 0,
                posX = 110f,
                posY = 280f,
                customColor = DesktopAccentColor.EMERALD_LINUX,
                isPinnedToTaskbar = true
            ),
            DesktopShortcut(
                id = "sc_distro",
                title = "Linux System",
                windowType = WindowType.DISTRO_STORE,
                gridCol = 1,
                gridRow = 1,
                posX = 110f,
                posY = 368f,
                customColor = DesktopAccentColor.CYBER_CYAN,
                isPinnedToTaskbar = true
            ),
            DesktopShortcut(
                id = "sc_server",
                title = "Server Hub",
                windowType = WindowType.SERVER_MANAGER,
                gridCol = 1,
                gridRow = 2,
                posX = 110f,
                posY = 456f,
                customColor = DesktopAccentColor.PURPLE_NEBULA
            ),
            DesktopShortcut(
                id = "sc_monitor",
                title = "System Monitor",
                windowType = WindowType.RESOURCE_MONITOR,
                gridCol = 1,
                gridRow = 3,
                posX = 110f,
                posY = 544f,
                customColor = DesktopAccentColor.CYBER_CYAN
            ),

            // Column 2 (posX = 200f)
            DesktopShortcut(
                id = "sc_ai",
                title = "Local AI",
                windowType = WindowType.AI_ASSISTANT,
                gridCol = 2,
                gridRow = 0,
                posX = 200f,
                posY = 280f,
                customColor = DesktopAccentColor.PURPLE_NEBULA,
                isPinnedToTaskbar = true
            ),
            DesktopShortcut(
                id = "sc_vscode",
                title = "VS Code Web",
                windowType = WindowType.VS_CODE,
                gridCol = 2,
                gridRow = 1,
                posX = 200f,
                posY = 368f,
                customColor = DesktopAccentColor.ELECTRIC_BLUE,
                isPinnedToTaskbar = false
            ),
            DesktopShortcut(
                id = "sc_about",
                title = "About Phonix OS",
                windowType = WindowType.ABOUT,
                gridCol = 2,
                gridRow = 2,
                posX = 200f,
                posY = 456f,
                customColor = DesktopAccentColor.CYBER_CYAN
            ),
            DesktopShortcut(
                id = "sc_privacy",
                title = "Privacy Hub",
                windowType = WindowType.PRIVACY,
                gridCol = 2,
                gridRow = 3,
                posX = 200f,
                posY = 544f,
                customColor = DesktopAccentColor.EMERALD_LINUX
            ),

            // Column 3 (posX = 290f)
            DesktopShortcut(
                id = "sc_settings",
                title = "Settings",
                windowType = WindowType.SETTINGS,
                gridCol = 3,
                gridRow = 0,
                posX = 290f,
                posY = 280f,
                isNativeApp = true
            ),
            DesktopShortcut(
                id = "sc_personalize",
                title = "Personalize",
                windowType = WindowType.PERSONALIZE,
                gridCol = 3,
                gridRow = 1,
                posX = 290f,
                posY = 368f
            ),
            DesktopShortcut(
                id = "sc_phone_screen",
                title = "Phone Screen",
                windowType = WindowType.PHONE_MIRROR,
                gridCol = 3,
                gridRow = 2,
                posX = 290f,
                posY = 456f,
                customColor = DesktopAccentColor.CYBER_CYAN
            )
        )
    }
}

sealed class DesktopContextMenuData {
    data class ShortcutMenu(
        val shortcutId: String,
        val posX: Float,
        val posY: Float
    ) : DesktopContextMenuData()

    data class WallpaperMenu(
        val posX: Float,
        val posY: Float
    ) : DesktopContextMenuData()
}

data class DesktopCustomization(
    val wallpaper: DesktopWallpaper = DesktopWallpaper.PHOENIX_CYBER_DEFAULT,
    val accentColor: DesktopAccentColor = DesktopAccentColor.ELECTRIC_BLUE,
    val iconSize: IconSizeMode = IconSizeMode.MEDIUM,
    val taskbarPosition: TaskbarPosition = TaskbarPosition.BOTTOM,
    val taskbarStyle: TaskbarStyle = TaskbarStyle.WINDOWS_11_FLUENT,
    val displayMode: MonitorDisplayMode = MonitorDisplayMode.MONITOR_FHD_16_9,
    val showQuickDistroWidget: Boolean = true,
    val showSystemMonitorWidget: Boolean = true,
    val showDesktopGrid: Boolean = false,
    val showDesktopLabels: Boolean = true,
    val showClockInTaskbar: Boolean = true,
    val showBatteryInTaskbar: Boolean = true,
    val lockIconsToGrid: Boolean = false, // Freeform movement enabled by default
    val shortcuts: List<DesktopShortcut> = DesktopShortcut.getDefaultShortcuts(),
    val areAllWindowsHidden: Boolean = false
)
