package com.example.engine

import android.content.Context
import java.io.File

/**
 * The Phonix look for the Linux guest.
 *
 * IceWM ships light, 1990s-looking themes, which is exactly what a customer would see if we
 * left it alone, so the desktop's palette, fonts, taskbar and wallpaper are written into the
 * container's rootfs like any other part of the product. Everything here is plain files under
 * `/root`, which is what IceWM reads at startup - no shell quoting, no manual setup step.
 *
 * [install] writes only what changed, so calling it on every launch is cheap and an app update
 * that changes the theme is picked up the next time the desktop starts.
 */
object LinuxTheme {

    /** Wallpaper for the X root; also the image the Android side uses, cropped to 16:9. */
    private const val WALLPAPER_ASSET = "linux/wallpaper.jpg"
    const val WALLPAPER_IN_GUEST = "root/phonix-wallpaper.jpg"

    /** What the guest has, keyed by the guest-relative path. */
    private fun files(): Map<String, String> = mapOf(
        "root/.icewm/preferences" to PREFERENCES,
        "root/.icewm/themes/phonix/default.theme" to THEME,
        "root/.icewm/menu" to MENU,
        "root/.icewm/toolbar" to TOOLBAR,
        "root/.icewm/startup" to STARTUP,
        "root/.config/tint2/tint2rc" to TINT2_RC,
        "root/.config/openbox/menu.xml" to OPENBOX_MENU,
        "root/.config/openbox/autostart" to OPENBOX_AUTOSTART,
        "root/.config/lxqt/session.conf" to LXQT_SESSION_CONF,
        "etc/motd" to MOTD,
    )

    /**
     * Writes the theme into the container. Returns true when something changed, which is the
     * signal that the running desktop needs a restart to pick it up.
     */
    fun install(context: Context): Boolean {
        val rootfs = LinuxRuntime.container(context).rootfs
        if (!rootfs.isDirectory) return false
        var changed = false
        for ((path, content) in files()) {
            val target = File(rootfs, path)
            if (writeIfChanged(target, content.toByteArray())) {
                changed = true
                if (path.endsWith("/startup") || path.endsWith("/autostart")) {
                    target.setExecutable(true, false)
                }
            }
        }
        val wallpaper = try {
            context.assets.open(WALLPAPER_ASSET).use { it.readBytes() }
        } catch (_: Throwable) {
            null
        }
        if (wallpaper != null && writeIfChanged(File(rootfs, WALLPAPER_IN_GUEST), wallpaper)) {
            changed = true
        }
        return changed
    }

    /** True when the guest already has the current bundle, used to avoid a needless restart. */
    fun isCurrent(context: Context): Boolean {
        val rootfs = LinuxRuntime.container(context).rootfs
        if (!rootfs.isDirectory) return false
        for ((path, content) in files()) {
            val file = File(rootfs, path)
            if (!file.isFile || file.length() != content.toByteArray().size.toLong()) return false
        }
        return File(rootfs, WALLPAPER_IN_GUEST).isFile
    }

    private fun writeIfChanged(target: File, bytes: ByteArray): Boolean {
        if (target.isFile && target.length() == bytes.size.toLong()) {
            val existing = try {
                target.readBytes()
            } catch (_: Throwable) {
                null
            }
            if (existing != null && existing.contentEquals(bytes)) return false
        }
        return try {
            target.parentFile?.mkdirs()
            target.writeBytes(bytes)
            true
        } catch (_: Throwable) {
            // A half-installed rootfs is not fatal: the desktop still starts, it just looks
            // like stock IceWM until the next launch can write the files.
            false
        }
    }

    private val PREFERENCES = """
        |# Phonix OS desktop defaults - rewritten by the app on update.
        |Theme="phonix"
        |ThemeSettings=""
        |DesktopBackgroundImage="/root/phonix-wallpaper.jpg"
        |DesktopBackgroundScaled=1
        |DesktopBackgroundColor="#0B1220"
        |TaskBarAtTop=0
        |TaskBarShowClock=1
        |TaskBarShowCPU=0
        |TaskBarShowMEMStatus=0
        |TaskBarShowNetStatus=0
        |TaskBarAutoHide=0
        |TaskBarDoubleHeight=0
        |TaskBarShowWindowListMenu=1
        |TaskBarClockLeds=0
        |TaskBarClockFormat="%H:%M"
        |ShowTaskBar=1
        |IconSize=32
        |FocusMode=1
        |AutoRaise=0
        |OpaqueMove=1
        |OpaqueResize=0
        |FontNameXft="sans:size=9"
        |TaskBarFontNameXft="sans:size=9"
        |TitleFontNameXft="sans:size=9:bold"
        |MenuFontNameXft="sans:size=9"
        |DialogFontNameXft="sans:size=9"
        |StatusFontNameXft="sans:size=9"
        |InputFontNameXft="sans:size=9"
        |LabelFontNameXft="sans:size=9"
        |ListBoxFontNameXft="sans:size=9"
        |QuickSwitchFontNameXft="sans:size=9"
        |ToolTipFontNameXft="sans:size=8"
        |ClockFontNameXft="sans:size=9:bold"
    """.trimMargin()

    private val THEME = """
        |# Phonix OS IceWM theme
        |ThemeAuthor="Phonix OS"
        |ThemeDescription="Phonix OS - dark slate desktop"
        |
        |Look=flat
        |Gradients=0
        |BorderSizeX=5
        |BorderSizeY=4
        |CornerSizeX=8
        |CornerSizeY=8
        |DlgBorderSizeX=4
        |DlgBorderSizeY=4
        |TitleBarHeight=24
        |TitleButtonsLeft=""
        |TitleButtonsRight="msx"
        |TitleButtonsSupported="msx"
        |TitleBarJustify=20
        |SmallIconSize=16
        |IconSize=32
        |LargeIconSize=32
        |HugeIconSize=48
        |MenuIconSize=16
        |ScrollBarX=8
        |ScrollBarY=12
        |
        |# Windows
        |ColorActiveBorder="#22D3EE"
        |ColorNormalBorder="#1E293B"
        |ColorActiveTitleBar="#0E1B2E"
        |ColorActiveTitleBarText="#F8FAFC"
        |ColorActiveTitleBarShadow="#0B1220"
        |ColorNormalTitleBar="#0B1220"
        |ColorNormalTitleBarText="#94A3B8"
        |ColorNormalTitleBarShadow="#0B1220"
        |ColorActiveButton="#22D3EE"
        |ColorActiveButtonText="#04121F"
        |ColorNormalButton="#111C2E"
        |ColorNormalButtonText="#CBD5E1"
        |ColorNormalTitleButton="#111C2E"
        |ColorNormalTitleButtonText="#CBD5E1"
        |ColorActiveMinimizedWindow="#0B1220"
        |ColorActiveMinimizedWindowText="#E2E8F0"
        |ColorNormalMinimizedWindow="#0B1220"
        |ColorNormalMinimizedWindowText="#94A3B8"
        |
        |# Menus and dialogs
        |ColorNormalMenu="#0F172A"
        |ColorMenuText="#CBD5E1"
        |ColorActiveMenuItem="#22D3EE"
        |ColorActiveMenuItemText="#04121F"
        |ColorNormalMenuItemText="#CBD5E1"
        |ColorDisabledMenuItemText="#475569"
        |ColorDialog="#0F172A"
        |ColorLabel="#0F172A"
        |ColorLabelText="#E2E8F0"
        |ColorInput="#111C2E"
        |ColorInputText="#E2E8F0"
        |ColorInputSelection="#22D3EE"
        |ColorInputSelectionText="#04121F"
        |ColorListBox="#0F172A"
        |ColorListBoxText="#E2E8F0"
        |ColorListBoxSelection="#22D3EE"
        |ColorListBoxSelectionText="#04121F"
        |ColorToolButton="#111C2E"
        |ColorToolButtonText="#CBD5E1"
        |ColorToolTip="#0F172A"
        |ColorToolTipText="#E2E8F0"
        |ColorMoveSizeStatus="#0F172A"
        |ColorMoveSizeStatusText="#67E8F9"
        |ColorQuickSwitch="#0F172A"
        |ColorQuickSwitchActive="#22D3EE"
        |ColorQuickSwitchText="#E2E8F0"
        |
        |# Taskbar
        |ColorTaskBar="#0B1220"
        |ColorTaskBarText="#E2E8F0"
        |ColorDefaultTaskBar="#0B1220"
        |ColorActiveTaskBarApp="#0E4C5B"
        |ColorActiveTaskBarAppText="#E6FEFF"
        |ColorNormalTaskBarApp="#111C2E"
        |ColorNormalTaskBarAppText="#CBD5E1"
        |ColorMinimizedTaskBarApp="#0B1220"
        |ColorMinimizedTaskBarAppText="#94A3B8"
        |ColorInvisibleTaskBarApp="#0B1220"
        |ColorInvisibleTaskBarAppText="#64748B"
        |ColorActiveWorkspaceButton="#22D3EE"
        |ColorActiveWorkspaceButtonText="#04121F"
        |ColorNormalWorkspaceButton="#111C2E"
        |ColorNormalWorkspaceButtonText="#94A3B8"
        |ColorClock="#0B1220"
        |ColorClockText="#67E8F9"
        |
        |# Scrollers
        |ColorScrollBar="#111C2E"
        |ColorScrollBarButton="#1E293B"
        |ColorScrollBarArrow="#94A3B8"
        |ColorScrollBarButtonArrow="#CBD5E1"
        |ColorScrollBarInactiveArrow="#475569"
        |ColorScrollBarSlider="#0E4C5B"
    """.trimMargin()

    private val MENU = """
        |prog "Terminal (xterm)" xterm xterm -fa "DejaVu Sans Mono" -fs 10 -bg "#0B1220" -fg "#DBEAFE" -cr "#F59E0B"
        |prog "File Manager (Thunar)" folder sh -c "thunar /root 2>/dev/null || pcmanfm-qt /root 2>/dev/null || pcmanfm /root 2>/dev/null || xterm -e sh -c 'ls -la /root; exec /bin/sh'"
        |prog "Top Process Viewer" utilities-system-monitor xterm -fa "DejaVu Sans Mono" -fs 10 -bg "#0B1220" -fg "#DBEAFE" -e sh -c "htop 2>/dev/null || top"
        |separator
        |restart "Restart IceWM" icewm
    """.trimMargin()

    private val TOOLBAR = """
        |prog "Terminal" xterm xterm -fa "DejaVu Sans Mono" -fs 10 -bg "#0B1220" -fg "#DBEAFE"
        |prog "Files" folder sh -c "thunar /root 2>/dev/null || pcmanfm-qt /root 2>/dev/null || pcmanfm /root 2>/dev/null || xterm -e sh -c 'ls -la /root; exec /bin/sh'"
    """.trimMargin()

    private val STARTUP = """
        |#!/bin/sh
        |# Run by IceWM on every start.
        |/usr/bin/icewmbg >/dev/null 2>&1 &
    """.trimMargin()

    /**
     * Phonix dark slate taskbar configuration for tint2 (used by Openbox and as a reliable
     * panel fallback whenever a standalone window manager is running).
     */
    private val TINT2_RC = """
        |# Phonix OS dark slate tint2 panel configuration
        |# Background 1: Panel bar
        |rounded = 0
        |border_width = 1
        |border_sides = T
        |background_color = #0B1220 96
        |border_color = #1E293B 100
        |
        |# Background 2: Default task button
        |rounded = 4
        |border_width = 1
        |border_sides = TBLR
        |background_color = #111C2E 90
        |border_color = #1E293B 100
        |
        |# Background 3: Active task button
        |rounded = 4
        |border_width = 1
        |border_sides = TBLR
        |background_color = #0E4C5B 100
        |border_color = #22D3EE 100
        |
        |# Panel layout
        |panel_items = ETSC
        |panel_size = 100% 30
        |panel_margin = 0 0
        |panel_padding = 6 3 6
        |panel_background_id = 1
        |wm_menu = 1
        |panel_dock = 0
        |panel_position = bottom center horizontal
        |panel_layer = top
        |panel_monitor = all
        |autohide = 0
        |strut_policy = follow_size
        |panel_window_name = tint2
        |
        |# Executor 1: Quick Terminal button on taskbar
        |execp = new
        |execp_command = echo "[ + Terminal ]"
        |execp_interval = 0
        |execp_has_icon = 0
        |execp_cache_icon = 0
        |execp_font = DejaVu Sans Bold 9
        |execp_font_color = #22D3EE 100
        |execp_padding = 6 2
        |execp_background_id = 2
        |execp_centered = 1
        |execp_lclick_command = xterm -fa 'DejaVu Sans Mono' -fs 10 -bg '#0B1220' -fg '#DBEAFE' -cr '#F59E0B'
        |
        |# Executor 2: Quick File Manager button on taskbar
        |execp = new
        |execp_command = echo "[ Files ]"
        |execp_interval = 0
        |execp_has_icon = 0
        |execp_cache_icon = 0
        |execp_font = DejaVu Sans Bold 9
        |execp_font_color = #E2E8F0 100
        |execp_padding = 6 2
        |execp_background_id = 2
        |execp_centered = 1
        |execp_lclick_command = sh -c "thunar /root 2>/dev/null || pcmanfm-qt /root 2>/dev/null || pcmanfm /root 2>/dev/null || xterm -e sh -c 'ls -la /root; exec /bin/sh'"
        |
        |# Taskbar / Window list
        |taskbar_mode = single_desktop
        |taskbar_hide_if_empty = 0
        |taskbar_padding = 4 2 4
        |taskbar_background_id = 0
        |taskbar_active_background_id = 0
        |taskbar_name = 1
        |taskbar_hide_inactive_tasks = 0
        |taskbar_hide_different_monitor = 0
        |taskbar_always_show_all_desktop_tasks = 1
        |taskbar_name_padding = 6 2
        |taskbar_name_background_id = 2
        |taskbar_name_active_background_id = 3
        |taskbar_name_font = DejaVu Sans Bold 9
        |taskbar_name_font_color = #94A3B8 100
        |taskbar_name_active_font_color = #22D3EE 100
        |taskbar_distribute_size = 0
        |taskbar_sort_order = none
        |task_align = left
        |
        |# Task buttons
        |task_text = 1
        |task_icon = 1
        |task_centered = 0
        |urgent_nb_of_blink = 3
        |task_maximum_size = 180 26
        |task_padding = 6 3 4
        |task_font = DejaVu Sans 9
        |task_tooltip = 1
        |task_font_color = #CBD5E1 100
        |task_active_font_color = #F8FAFC 100
        |task_background_id = 2
        |task_active_background_id = 3
        |task_urgent_background_id = 3
        |task_iconified_background_id = 2
        |mouse_left = toggle_iconify
        |mouse_middle = none
        |mouse_right = close
        |
        |# System tray
        |systray_padding = 4 2 4
        |systray_background_id = 2
        |systray_sort = ascending
        |systray_icon_size = 18
        |systray_icon_asb = 100 0 0
        |
        |# Clock
        |time1_format = %H:%M
        |time1_font = DejaVu Sans Bold 9
        |clock_font_color = #67E8F9 100
        |clock_padding = 8 2
        |clock_background_id = 2
        |clock_lclick_command = xterm -fa 'DejaVu Sans Mono' -fs 10 -bg '#0B1220' -fg '#DBEAFE' -e sh -c 'cal; date; read -r _'
    """.trimMargin()

    private val OPENBOX_MENU = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<openbox_menu xmlns="http://openbox.org/3.4/menu">
        |  <menu id="root-menu" label="Phonix Openbox">
        |    <item label="Terminal (xterm)">
        |      <action name="Execute"><command>xterm -fa 'DejaVu Sans Mono' -fs 10 -bg '#0B1220' -fg '#DBEAFE' -cr '#F59E0B'</command></action>
        |    </item>
        |    <item label="File Manager">
        |      <action name="Execute"><command>sh -c "thunar /root 2>/dev/null || pcmanfm-qt /root 2>/dev/null || pcmanfm /root 2>/dev/null || xterm -e sh -c 'ls -la /root; exec /bin/sh'"</command></action>
        |    </item>
        |    <item label="System Monitor (top/htop)">
        |      <action name="Execute"><command>xterm -fa 'DejaVu Sans Mono' -fs 10 -bg '#0B1220' -fg '#DBEAFE' -e sh -c "htop 2>/dev/null || top"</command></action>
        |    </item>
        |    <separator />
        |    <item label="Reconfigure Openbox">
        |      <action name="Reconfigure" />
        |    </item>
        |    <item label="Restart Openbox">
        |      <action name="Restart" />
        |    </item>
        |  </menu>
        |</openbox_menu>
    """.trimMargin()

    private val OPENBOX_AUTOSTART = """
        |#!/bin/sh
        |xsetroot -solid '#0B1220' 2>/dev/null &
        |if command -v icewmbg >/dev/null 2>&1; then
        |  icewmbg >/dev/null 2>&1 &
        |fi
        |if command -v tint2 >/dev/null 2>&1 && ! pidof tint2 >/dev/null 2>&1; then
        |  tint2 >/tmp/tint2.log 2>&1 &
        |fi
    """.trimMargin()

    private val LXQT_SESSION_CONF = """
        |[General]
        |__userfile__=true
        |window_manager=openbox
    """.trimMargin()

    private val MOTD = """
        |Phonix OS  -  Linux inside Android
        |
        |You are root in a real Linux userland. Android keeps running behind this window,
        |so the phone's own apps still work. Install software with this distro's package
        |manager: "apk add <package>", "apt-get install <package>" or "pacman -S <package>".
        |
    """.trimMargin()
}

