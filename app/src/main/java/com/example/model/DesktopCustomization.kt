package com.example.model

import androidx.compose.ui.graphics.Color

enum class DesktopWallpaper(val title: String, val description: String) {
    PHOENIX_CYBER_DEFAULT("Phoenix Cybernetic (Default)", "The bundled Phonix wallpaper: the phoenix rising over dark circuitry"),
    EMBER_MARK("Ember Mark", "Near-black backdrop, ember heat glow and the Phonix head mark"),
    ALPINE_PEAK("Alpine Peak", "A nod to Alpine Linux: the mountain cut into a chip package"),
    TERMINAL_GREEN("Terminal Green", "Green on black, the way a shell looked before wallpapers existed"),
    RETRO_CLASSIC_TEAL("Retro PC Classic Teal", "Flat vintage 90s workstation teal"),
    OBSIDIAN_NIGHT("Obsidian Black", "Pure black. The cheapest wallpaper on an OLED panel"),

    /**
     * A picture the user picked from their gallery. The file lives in this app's private storage;
     * see [com.example.util.WallpaperStore].
     */
    CUSTOM_PHOTO("Your Photo", "A picture from your gallery, copied into this app and never uploaded")
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
    TILE("Fluent Tile", 6),
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
    FLUENT_CENTER("Fluent Center"),
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
            // A conventional desktop, in the spirit of "This PC / Home, Terminal, Browser,
            // Settings". Everything else is one click away in the taskbar or the Start menu, so
            // the desktop does not read like an app drawer.
            DesktopShortcut(
                id = "sc_files",
                title = "File Explorer",
                windowType = WindowType.FILE_EXPLORER,
                gridCol = 0,
                gridRow = 0,
                posX = 20f,
                posY = 280f,
                customColor = DesktopAccentColor.AMBER_EMBER,
                launchPackage = "com.google.android.documentsui",
                isNativeApp = true
            ),
            DesktopShortcut(
                id = "sc_terminal",
                title = "Terminal",
                windowType = WindowType.TERMINAL,
                gridCol = 1,
                gridRow = 0,
                posX = 110f,
                posY = 280f,
                customColor = DesktopAccentColor.EMERALD_LINUX
            ),
            DesktopShortcut(
                id = "sc_browser",
                title = "Phonix Browser",
                windowType = WindowType.BROWSER,
                gridCol = 2,
                gridRow = 0,
                posX = 200f,
                posY = 280f,
                customColor = DesktopAccentColor.CYBER_CYAN
            ),
            DesktopShortcut(
                id = "sc_settings",
                title = "Settings",
                windowType = WindowType.SETTINGS,
                gridCol = 3,
                gridRow = 0,
                posX = 290f,
                posY = 280f,
                isNativeApp = true
            )
        )

        /**
         * The taskbar keeps its own list, deliberately independent of the desktop icons.
         *
         * Deleting a desktop shortcut must not drop the app out of the bar, and unpinning from the
         * bar must not delete the icon - the two used to share one flag, so removing an icon also
         * removed the app from the taskbar (measured: deleting Local AI removed it entirely).
         */
        fun getDefaultTaskbarPins(): List<DesktopShortcut> = listOf(
            DesktopShortcut(
                id = "sc_store",
                title = "App Store",
                windowType = WindowType.APP_STORE,
                customColor = DesktopAccentColor.CYBER_CYAN,
                launchPackage = "com.android.vending",
                isAppStore = true
            ),
            DesktopShortcut(
                id = "sc_browser",
                title = "Phonix Browser",
                windowType = WindowType.BROWSER,
                customColor = DesktopAccentColor.CYBER_CYAN
            ),
            DesktopShortcut(
                id = "sc_files",
                title = "File Explorer",
                windowType = WindowType.FILE_EXPLORER,
                customColor = DesktopAccentColor.AMBER_EMBER,
                launchPackage = "com.google.android.documentsui",
                isNativeApp = true
            ),
            DesktopShortcut(
                id = "sc_terminal",
                title = "Terminal",
                windowType = WindowType.TERMINAL,
                customColor = DesktopAccentColor.EMERALD_LINUX
            ),
            DesktopShortcut(
                id = "sc_ai",
                title = "Local AI",
                windowType = WindowType.AI_ASSISTANT,
                customColor = DesktopAccentColor.PURPLE_NEBULA
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
    /** Absolute path of an imported photo; only meaningful when [wallpaper] is CUSTOM_PHOTO. */
    val customWallpaperPath: String? = null,
    val accentColor: DesktopAccentColor = DesktopAccentColor.ELECTRIC_BLUE,
    val iconSize: IconSizeMode = IconSizeMode.MEDIUM,
    val taskbarPosition: TaskbarPosition = TaskbarPosition.BOTTOM,
    val taskbarStyle: TaskbarStyle = TaskbarStyle.FLUENT_CENTER,
    val displayMode: MonitorDisplayMode = MonitorDisplayMode.MONITOR_FHD_16_9,
    val showQuickDistroWidget: Boolean = true,
    val showSystemMonitorWidget: Boolean = true,
    val showDesktopGrid: Boolean = false,
    val showDesktopLabels: Boolean = true,
    /**
     * Draw the tile and outline behind a desktop icon. Off gives the flat "just the glyph and the
     * caption" look some desktops prefer; the icons stay hit-testable either way.
     */
    val showIconFrames: Boolean = true,
    val showClockInTaskbar: Boolean = true,
    val showBatteryInTaskbar: Boolean = true,
    val lockIconsToGrid: Boolean = false, // Freeform movement enabled by default
    val shortcuts: List<DesktopShortcut> = DesktopShortcut.getDefaultShortcuts(),
    /** Taskbar contents. Independent of [shortcuts]: one can change without touching the other. */
    val taskbarPins: List<DesktopShortcut> = DesktopShortcut.getDefaultTaskbarPins(),
    val areAllWindowsHidden: Boolean = false
)
