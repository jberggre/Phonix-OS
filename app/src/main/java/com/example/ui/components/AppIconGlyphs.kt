package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlueLight
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.TerminalGreen

/**
 * One place that decides what a shortcut looks like, so the desktop, taskbar and Start menu can
 * never disagree again. Before this, anything without an explicit case (App Store, Settings) fell
 * through to the generic laptop glyph, and every pinned phone app inherited the browser globe.
 *
 * This is a consistent placeholder set, not the final artwork: the designed icons are specified in
 * docs/ICON-BRIEF.md and can replace [forWindow] / [forApp] without touching the call sites.
 */
object AppIconGlyphs {

    /**
     * The designed artwork, sliced from the approved icon sheet. Every surface on the desktop has
     * one; the Material glyph in [forWindow] is now only the fallback for anything without art.
     */
    @DrawableRes
    fun artFor(type: WindowType): Int = when (type) {
        WindowType.BROWSER -> R.drawable.ic_phonix_browser
        WindowType.LINUX_DESKTOP -> R.drawable.ic_phonix_x11
        WindowType.DISTRO_STORE -> R.drawable.ic_phonix_linux
        WindowType.PERSONALIZE -> R.drawable.ic_phonix_personalize
        WindowType.TERMINAL -> R.drawable.ic_phonix_terminal
        WindowType.SERVER_MANAGER -> R.drawable.ic_phonix_servers
        WindowType.RESOURCE_MONITOR -> R.drawable.ic_phonix_vitals
        WindowType.FILE_EXPLORER -> R.drawable.ic_phonix_files
        WindowType.APP_STORE -> R.drawable.ic_phonix_store
        WindowType.SETTINGS -> R.drawable.ic_phonix_settings
        WindowType.PRIVACY -> R.drawable.ic_phonix_privacy
        WindowType.NOTEPAD -> R.drawable.ic_phonix_notepad
        WindowType.BENCHMARK -> R.drawable.ic_phonix_benchmark
        WindowType.ABOUT -> R.drawable.ic_phonix_about
        WindowType.VS_CODE -> R.drawable.ic_phonix_vscode
        WindowType.PHONE_MIRROR -> R.drawable.ic_phonix_phone
        WindowType.AI_ASSISTANT -> R.drawable.ic_phonix_ai
    }

    /** Fallback artwork for a pinned phone app whose launcher icon could not be read. */
    @DrawableRes
    fun artForApp(label: String, packageName: String = ""): Int {
        val key = (label + " " + packageName).lowercase()
        return if (key.containsAny("tool", "utility", "settings", "config", "manager", "cleaner")) {
            R.drawable.ic_generic_utility
        } else {
            R.drawable.ic_generic_app
        }
    }

    fun forWindow(type: WindowType): ImageVector = when (type) {
        WindowType.BROWSER -> Icons.Default.Language
        WindowType.LINUX_DESKTOP -> Icons.Default.Computer
        WindowType.DISTRO_STORE -> Icons.Default.Apps
        WindowType.PERSONALIZE -> Icons.Default.Palette
        WindowType.TERMINAL -> Icons.Default.Terminal
        WindowType.SERVER_MANAGER -> Icons.Default.Dns
        WindowType.RESOURCE_MONITOR -> Icons.Default.Speed
        WindowType.FILE_EXPLORER -> Icons.Default.Folder
        WindowType.APP_STORE -> Icons.Default.Storefront
        WindowType.SETTINGS -> Icons.Default.Settings
        WindowType.PRIVACY -> Icons.Default.Security
        WindowType.NOTEPAD -> Icons.Default.Brush
        WindowType.BENCHMARK -> Icons.Default.PlayArrow
        WindowType.ABOUT -> Icons.Default.Info
        WindowType.VS_CODE -> Icons.Default.Code
        WindowType.PHONE_MIRROR -> Icons.Default.PhoneAndroid
        WindowType.AI_ASSISTANT -> Icons.Default.SmartToy
    }

    /**
     * A phone app has no WindowType of its own, so pick a glyph from what it is called. This only
     * runs when the launcher icon could not be read; when it can, the real icon is shown.
     */
    fun forApp(label: String, packageName: String = ""): ImageVector {
        val key = (label + " " + packageName).lowercase()
        return when {
            key.containsAny("message", "sms", "chat", "whatsapp", "signal", "telegram", "discord") -> Icons.Default.Chat
            key.containsAny("mail", "gmail", "inbox", "outlook") -> Icons.Default.Email
            key.containsAny("camera", "photo", "gallery", "snapseed", "lens") -> Icons.Default.CameraAlt
            key.containsAny("music", "spotify", "audio", "podcast", "player") -> Icons.Default.MusicNote
            key.containsAny("video", "youtube", "netflix", "movie", "plex") -> Icons.Default.Movie
            key.containsAny("map", "nav", "gps", "waze", "earth") -> Icons.Default.Map
            key.containsAny("game", "steam", "emulator", "retro") -> Icons.Default.SportsEsports
            key.containsAny("bank", "wallet", "pay", "finance", "venmo") -> Icons.Default.AccountBalance
            key.containsAny("calendar", "agenda") -> Icons.Default.CalendarMonth
            key.containsAny("clock", "alarm", "timer") -> Icons.Default.Alarm
            key.containsAny("shop", "amazon", "store", "market") -> Icons.Default.ShoppingCart
            key.containsAny("book", "kindle", "reader", "docs", "pdf", "notes", "keep") -> Icons.Default.Book
            key.containsAny("drive", "file", "folder", "manager", "documents") -> Icons.Default.Folder
            key.containsAny("download", "torrent", "fdroid") -> Icons.Default.Download
            key.containsAny("food", "recipe", "delivery", "doordash") -> Icons.Default.Restaurant
            key.containsAny("car", "uber", "lyft", "drive") -> Icons.Default.DirectionsCar
            key.containsAny("settings", "control", "config") -> Icons.Default.Settings
            key.containsAny("code", "ide", "dev", "termux", "git") -> Icons.Default.Code
            key.containsAny("ai", "assistant", "gpt", "llm") -> Icons.Default.SmartToy
            key.containsAny("widget", "tool", "utility", "launcher") -> Icons.Default.Widgets
            else -> Icons.Default.Apps
        }
    }

    fun accentForWindow(type: WindowType): Color = when (type) {
        WindowType.BROWSER -> PhonixCyan
        WindowType.LINUX_DESKTOP -> PhonixAmber
        WindowType.DISTRO_STORE -> PhonixAmber
        WindowType.PERSONALIZE -> Color(0xFFA855F7)
        WindowType.TERMINAL -> TerminalGreen
        WindowType.SERVER_MANAGER -> PhonixCyan
        WindowType.RESOURCE_MONITOR -> Color(0xFF34D399)
        WindowType.FILE_EXPLORER -> Color(0xFFFBBF24)
        WindowType.APP_STORE -> Color(0xFF38BDF8)
        WindowType.SETTINGS -> Color(0xFF94A3B8)
        WindowType.PRIVACY -> PhonixGreen
        WindowType.NOTEPAD -> Color(0xFFF472B6)
        WindowType.BENCHMARK -> Color(0xFFF43F5E)
        WindowType.ABOUT -> PhonixCyan
        WindowType.VS_CODE -> PhonixBlueLight
        WindowType.PHONE_MIRROR -> Color(0xFF22D3EE)
        WindowType.AI_ASSISTANT -> Color(0xFFA855F7)
    }

    private fun String.containsAny(vararg needles: String): Boolean = needles.any { contains(it) }
}
