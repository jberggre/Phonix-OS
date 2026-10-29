package com.example.engine

import android.content.Context
import java.io.File

/**
 * Starts the Linux GUI stack inside the Alpine container and exposes it as a local web page.
 *
 * Xvfb draws a virtual X11 screen, x11vnc exports it over VNC, and websockify + noVNC turn
 * that into a page the desktop window can show in a WebView. Everything binds to 127.0.0.1:
 * the guest has no network identity of its own, and the phone is the only client.
 *
 * This is software rendering: Xvfb paints into RAM and VNC encodes the frames. It is honest
 * for terminals, editors, dev tools and lightweight browsers; it is not a GPU desktop.
 */
object LinuxDesktop {

    private const val PORT = 6080

    /** Served by websockify out of the guest's noVNC web root. */
    const val VNC_URL = "http://127.0.0.1:$PORT/phonix.html"

    /** The viewer page, bundled in the APK so the guest never has to be edited by hand. */
    private const val PAGE_ASSET = "novnc/phonix.html"
    private const val PAGE_IN_GUEST = "usr/share/novnc/phonix.html"

    /**
     * Idempotent: every block inspects the guest first and only acts when something is missing,
     * so this is cheap to re-send while the window is open. It is also the recovery path — a
     * crashed daemon or a restarted container is repaired by the next call.
     */
    private val START_SCRIPT = """
        |started=
        |if ! apk info -e icewm >/dev/null 2>&1; then
        |  echo '[x11] installing X11, VNC and noVNC inside the container (first run, ~450 MB)...'
        |  apk add --no-cache xvfb x11vnc novnc icewm xterm font-misc-misc xsetroot
        |  started="${'$'}started x11-packages"
        |fi
        |# DejaVu is what gives the desktop readable text at this size; IceWM and xterm both draw
        |# through Xft, and without a real font they fall back to bitmap glyphs that look broken.
        |if ! apk info -e font-dejavu >/dev/null 2>&1; then
        |  apk add --no-cache font-dejavu fontconfig
        |  started="${'$'}started fonts"
        |fi
        |mkdir -p /tmp/.X11-unix && chmod 1777 /tmp/.X11-unix
        |if ! pidof Xvfb >/dev/null; then
        |  nohup Xvfb :1 -screen 0 1280x720x24 -nolisten tcp >/tmp/xvfb.log 2>&1 &
        |  sleep 2
        |  started="${'$'}started Xvfb"
        |fi
        |if ! pidof x11vnc >/dev/null; then
        |  nohup env DISPLAY=:1 HOME=/root x11vnc -display :1 -noshm -forever -shared -localhost -rfbport 5901 -nopw >/tmp/x11vnc.log 2>&1 &
        |  started="${'$'}started x11vnc"
        |fi
        |if ! pidof websockify >/dev/null; then
        |  nohup websockify --web=/usr/share/novnc 127.0.0.1:$PORT 127.0.0.1:5901 >/tmp/ws.log 2>&1 &
        |  started="${'$'}started websockify"
        |fi
        |if ! pidof icewm >/dev/null; then
        |  nohup env DISPLAY=:1 HOME=/root icewm >/tmp/icewm.log 2>&1 &
        |  sleep 1
        |  started="${'$'}started icewm"
        |fi
        |# The Phonix wallpaper lives on the X root. icewmbg reads DesktopBackgroundImage from
        |# ~/.icewm/preferences, and the solid colour is the fallback if the image cannot load.
        |xsetroot -solid '#0B1220' 2>/dev/null
        |if ! pidof icewmbg >/dev/null; then
        |  nohup env DISPLAY=:1 HOME=/root icewmbg >/tmp/icewmbg.log 2>&1 &
        |  started="${'$'}started icewmbg"
        |else
        |  # Reload the image: an app update can replace the wallpaper while the desktop runs.
        |  icewmbg -r >/tmp/icewmbg.log 2>&1
        |fi
        |# Alpine's xterm has no Xft, and without font-misc-misc it cannot open a window at all, so
        |# a core font keeps the terminal usable. It is started whenever none is running, which
        |# also heals a terminal that died with the container.
        |if ! pidof xterm >/dev/null; then
        |  nohup env DISPLAY=:1 HOME=/root xterm -fa 'DejaVu Sans Mono' -fs 10 -bg '#0B1220' -fg '#DBEAFE' -cr '#F59E0B' -geometry 100x28+24+16 -e /bin/sh >/tmp/xterm.log 2>&1 &
        |  # Give the process time to appear in pidof, so a second ensureStarted() a few seconds
        |  # later does not open a duplicate terminal.
        |  sleep 2
        |  started="${'$'}started xterm"
        |fi
        |[ -z "${'$'}started" ] || echo "[x11] started:${'$'}started"
        |echo '[x11] display :1 ready on http://127.0.0.1:$PORT'
    """.trimMargin()

    @Volatile
    private var startRequested = false

    /** Which container boot the current request belongs to; a reboot invalidates it. */
    @Volatile
    private var startedGeneration = -1

    /**
     * Makes sure the guest is serving the display page and that Xvfb/x11vnc/websockify are up.
     * Safe to call repeatedly and from any thread.
     */
    fun ensureStarted(context: Context): Boolean {
        installPage(context)
        // Theme files are part of the product, not a manual setup step: a fresh container gets
        // them before the window manager starts, and an app update that changes them restarts
        // IceWM so the change is visible without the customer doing anything.
        if (LinuxTheme.install(context)) {
            startRequested = false
            if (LinuxRuntime.isRunning) LinuxRuntime.send(RESTART_WM_SCRIPT)
        }
        if (startRequested && startedGeneration == LinuxRuntime.generation) return true
        val sent = LinuxRuntime.send(START_SCRIPT)
        startRequested = sent
        if (sent) startedGeneration = LinuxRuntime.generation
        return sent
    }

    /** Replaces a running window manager with the current theme; open windows survive. */
    private const val RESTART_WM_SCRIPT =
        "if pidof Xvfb >/dev/null; then " +
            "pkill icewm >/dev/null 2>&1; sleep 1; " +
            "nohup env DISPLAY=:1 HOME=/root icewm >/tmp/icewm.log 2>&1 & " +
            "sleep 1; pidof icewmbg >/dev/null || nohup env DISPLAY=:1 HOME=/root icewmbg " +
            ">/tmp/icewmbg.log 2>&1 & fi"

    /** Explicit user-facing recovery: forget the "already started" state and start again. */
    fun restart(context: Context) {
        startRequested = false
        startedGeneration = -1
        ensureStarted(context)
    }

    /** Kills the display stack inside the guest; the container itself keeps running. */
    fun stop(): Boolean = LinuxRuntime.send(
        "pkill -f websockify; pkill x11vnc; pkill Xvfb; echo '[x11] display stopped'"
    )

    /**
     * Websockify serves the viewer straight from the guest's noVNC directory, which is where
     * our page goes. Rewritten only when the bundle changed, so a rebuild is what updates it.
     */
    private fun installPage(context: Context) {
        val rootfs = LinuxRuntime.container(context).rootfs
        if (!rootfs.isDirectory) return
        val bytes = try {
            context.assets.open(PAGE_ASSET).use { it.readBytes() }
        } catch (_: Throwable) {
            return
        }
        val target = File(rootfs, PAGE_IN_GUEST)
        if (target.isFile && target.length() == bytes.size.toLong()) {
            val existing = try {
                target.readBytes()
            } catch (_: Throwable) {
                null
            }
            if (existing != null && existing.contentEquals(bytes)) return
        }
        try {
            target.parentFile?.mkdirs()
            target.writeBytes(bytes)
        } catch (_: Throwable) {
            // A read-only or half-installed rootfs is not fatal: the display still starts,
            // it just falls back to whatever page the guest already has.
        }
    }
}
