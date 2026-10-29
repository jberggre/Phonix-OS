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
            if (writeIfChanged(File(rootfs, path), content.toByteArray())) changed = true
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
        |prog "Terminal" xterm xterm -fa "DejaVu Sans Mono" -fs 10 -bg "#0B1220" -fg "#DBEAFE" -cr "#F59E0B"
        |separator
        |restart "Restart desktop" icewm
    """.trimMargin()

    private val TOOLBAR = """
        |prog Terminal xterm xterm -fa "DejaVu Sans Mono" -fs 10 -bg "#0B1220" -fg "#DBEAFE"
    """.trimMargin()

    private val STARTUP = """
        |# Run by IceWM on every start.
        |/usr/bin/icewmbg >/dev/null 2>&1 &
    """.trimMargin()

    private val MOTD = """
        |Phonix OS  -  Alpine Linux inside Android
        |
        |You are root in a real Alpine container. Android keeps running behind this window,
        |so the phone's own apps still work. Install software with "apk add <package>".
        |
    """.trimMargin()
}
