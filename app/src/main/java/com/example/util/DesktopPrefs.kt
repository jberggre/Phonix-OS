package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.DesktopAccentColor
import com.example.model.DesktopCustomization
import com.example.model.DesktopShortcut
import com.example.model.DesktopWallpaper
import com.example.model.IconSizeMode
import com.example.model.MonitorDisplayMode
import com.example.model.ShortcutBgStyle
import com.example.model.ShortcutShape
import com.example.model.TaskbarPosition
import com.example.model.TaskbarStyle
import com.example.model.WindowType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Remembers the desktop the user arranged: wallpaper, accent, icon size, toggles, every icon
 * position and rename, and the separate taskbar pins.
 *
 * Before this existed, every personalisation was lost the moment the activity was recreated - a
 * renamed icon or a moved wallpaper came back as the default on the next launch, which made the
 * whole "customize your desktop" promise untrue.
 */
object DesktopPrefs {

    private const val TAG = "PhonixDesktopPrefs"
    private const val PREFS = "phonix_desktop_prefs"
    private const val KEY_STATE = "desktop_state_v1"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, customization: DesktopCustomization) {
        try {
            val json = JSONObject().apply {
                put("wallpaper", customization.wallpaper.name)
                customization.customWallpaperPath?.let { put("customWallpaperPath", it) }
                put("accentColor", customization.accentColor.name)
                put("iconSize", customization.iconSize.name)
                put("taskbarPosition", customization.taskbarPosition.name)
                put("taskbarStyle", customization.taskbarStyle.name)
                put("displayMode", customization.displayMode.name)
                put("showQuickDistroWidget", customization.showQuickDistroWidget)
                put("showSystemMonitorWidget", customization.showSystemMonitorWidget)
                put("showDesktopGrid", customization.showDesktopGrid)
                put("showDesktopLabels", customization.showDesktopLabels)
                put("showIconFrames", customization.showIconFrames)
                put("showClockInTaskbar", customization.showClockInTaskbar)
                put("showBatteryInTaskbar", customization.showBatteryInTaskbar)
                put("lockIconsToGrid", customization.lockIconsToGrid)
                put("shortcuts", shortcutsToJson(customization.shortcuts))
                put("taskbarPins", shortcutsToJson(customization.taskbarPins))
            }
            prefs(context).edit().putString(KEY_STATE, json.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "could not save desktop layout", e)
        }
    }

    /** The saved desktop, or null when nothing has been saved yet. */
    fun load(context: Context): DesktopCustomization? {
        val raw = prefs(context).getString(KEY_STATE, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val defaults = DesktopCustomization()
            DesktopCustomization(
                wallpaper = enumOr(json.optString("wallpaper"), DesktopWallpaper.PHOENIX_CYBER_DEFAULT),
                customWallpaperPath = json.optString("customWallpaperPath").takeIf { it.isNotBlank() && it != "null" },
                accentColor = enumOr(json.optString("accentColor"), defaults.accentColor),
                iconSize = enumOr(json.optString("iconSize"), defaults.iconSize),
                taskbarPosition = enumOr(json.optString("taskbarPosition"), defaults.taskbarPosition),
                taskbarStyle = enumOr(json.optString("taskbarStyle"), defaults.taskbarStyle),
                displayMode = enumOr(json.optString("displayMode"), defaults.displayMode),
                showQuickDistroWidget = json.optBoolean("showQuickDistroWidget", defaults.showQuickDistroWidget),
                showSystemMonitorWidget = json.optBoolean("showSystemMonitorWidget", defaults.showSystemMonitorWidget),
                showDesktopGrid = json.optBoolean("showDesktopGrid", defaults.showDesktopGrid),
                showDesktopLabels = json.optBoolean("showDesktopLabels", defaults.showDesktopLabels),
                showIconFrames = json.optBoolean("showIconFrames", defaults.showIconFrames),
                showClockInTaskbar = json.optBoolean("showClockInTaskbar", defaults.showClockInTaskbar),
                showBatteryInTaskbar = json.optBoolean("showBatteryInTaskbar", defaults.showBatteryInTaskbar),
                lockIconsToGrid = json.optBoolean("lockIconsToGrid", defaults.lockIconsToGrid),
                shortcuts = shortcutsFromJson(json.optJSONArray("shortcuts"), defaults.shortcuts),
                taskbarPins = shortcutsFromJson(json.optJSONArray("taskbarPins"), defaults.taskbarPins)
            )
        } catch (e: Exception) {
            Log.w(TAG, "saved desktop layout unreadable, using defaults", e)
            null
        }
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_STATE).apply()
    }

    private fun shortcutsToJson(list: List<DesktopShortcut>): JSONArray = JSONArray().apply {
        list.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("windowType", s.windowType.name)
                s.customSize?.let { put("customSize", it.name) }
                put("customShape", s.customShape.name)
                s.customColor?.let { put("customColor", it.name) }
                put("customBackground", s.customBackground.name)
                s.launchPackage?.let { put("launchPackage", it) }
                put("isNativeApp", s.isNativeApp)
                put("isAppStore", s.isAppStore)
                put("gridCol", s.gridCol)
                put("gridRow", s.gridRow)
                put("posX", s.posX.toDouble())
                put("posY", s.posY.toDouble())
                put("hasCustomPosition", s.hasCustomPosition)
            })
        }
    }

    private fun shortcutsFromJson(array: JSONArray?, fallback: List<DesktopShortcut>): List<DesktopShortcut> {
        if (array == null || array.length() == 0) return fallback
        val parsed = ArrayList<DesktopShortcut>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val type = enumOr<WindowType>(o.optString("windowType"), WindowType.FILE_EXPLORER)
            parsed += DesktopShortcut(
                id = o.optString("id").ifBlank { "sc_${i}" },
                title = o.optString("title").ifBlank { type.name },
                windowType = type,
                gridCol = o.optInt("gridCol", 0),
                gridRow = o.optInt("gridRow", 0),
                posX = o.optDouble("posX", 0.0).toFloat(),
                posY = o.optDouble("posY", 0.0).toFloat(),
                hasCustomPosition = o.optBoolean("hasCustomPosition", false),
                customSize = o.optString("customSize").takeIf { it.isNotBlank() }
                    ?.let { runCatching { IconSizeMode.valueOf(it) }.getOrNull() },
                customShape = enumOr(o.optString("customShape"), ShortcutShape.SQUIRCLE),
                customColor = o.optString("customColor").takeIf { it.isNotBlank() }
                    ?.let { runCatching { DesktopAccentColor.valueOf(it) }.getOrNull() },
                customBackground = enumOr(o.optString("customBackground"), ShortcutBgStyle.FROSTED_GLASS),
                launchPackage = o.optString("launchPackage").takeIf { it.isNotBlank() && it != "null" },
                isNativeApp = o.optBoolean("isNativeApp", false),
                isAppStore = o.optBoolean("isAppStore", false)
            )
        }
        return parsed
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
        runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
}
