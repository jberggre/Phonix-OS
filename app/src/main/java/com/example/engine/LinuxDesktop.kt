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
    private const val PREFS_NAME = "phonix_linux_prefs"
    private const val KEY_ACTIVE_DE = "active_de_id"

    /** Served by websockify out of the guest's noVNC web root. */
    const val VNC_URL = "http://127.0.0.1:$PORT/phonix.html"

    /** The viewer page, bundled in the APK so the guest never has to be edited by hand. */
    private const val PAGE_ASSET = "novnc/phonix.html"
    private const val PAGE_IN_GUEST = "usr/share/novnc/phonix.html"

    fun activeDesktopEnvironment(context: Context): com.example.model.LinuxDesktopEnvironment {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return com.example.model.LinuxDesktopEnvironment.fromId(prefs.getString(KEY_ACTIVE_DE, "icewm"))
    }

    fun setPreferredDesktopEnvironment(
        context: Context,
        de: com.example.model.LinuxDesktopEnvironment
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_DE, de.id)
            .apply()
    }

    /**
     * Builds the idempotent X11 + VNC + Desktop Environment start/switch script for the selected DE,
     * using whichever real package manager (apk, apt-get, or pacman) the active guest provides.
     *
     * Crucial design rules for switching away from IceWM while the desktop is running:
     * 1. Never kill Xvfb, x11vnc, websockify, or user X11 apps (xterm, firefox, thunar, etc.).
     * 2. Install any missing packages for the target DE *before* stopping the current window manager,
     *    so the desktop never loses window borders or taskbar while packages download (or if offline).
     * 3. Initialize D-Bus machine-id and XDG_RUNTIME_DIR=/tmp/runtime-root inside PRoot so XFCE4
     *    and LXQt panels/window managers do not exit immediately.
     * 4. Always start both a Window Manager (icewm, xfwm4, or openbox) AND a Taskbar/Panel
     *    (icewm taskbar, xfce4-panel, lxqt-panel, or tint2) + desktop background.
     * 5. If the target DE binary is still unavailable (e.g. offline install failure), keep/restore
     *    IceWM so the Linux desktop never loses its functions.
     */
    fun buildStartScript(
        de: com.example.model.LinuxDesktopEnvironment,
        forceSwitch: Boolean = false
    ): String {
        val deCheckCondition = when (de) {
            com.example.model.LinuxDesktopEnvironment.ICEWM ->
                "command -v icewm >/dev/null 2>&1"
            com.example.model.LinuxDesktopEnvironment.XFCE4 ->
                "command -v xfwm4 >/dev/null 2>&1 && command -v xfce4-panel >/dev/null 2>&1"
            com.example.model.LinuxDesktopEnvironment.OPENBOX ->
                "command -v openbox >/dev/null 2>&1 && command -v tint2 >/dev/null 2>&1"
            com.example.model.LinuxDesktopEnvironment.LXQT ->
                "command -v openbox >/dev/null 2>&1 && (command -v lxqt-panel >/dev/null 2>&1 || command -v pcmanfm-qt >/dev/null 2>&1)"
        }

        val lxqtExtraApk = if (de == com.example.model.LinuxDesktopEnvironment.LXQT) {
            "apk add --no-cache lxqt-panel lxqt-session >/dev/null 2>&1 || apk add --no-cache lxpanel pcmanfm >/dev/null 2>&1 || true"
        } else {
            "true"
        }

        val launchTargetDeBlock = when (de) {
            com.example.model.LinuxDesktopEnvironment.ICEWM -> """
                |  nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root icewm --replace >/tmp/icewm.log 2>&1 &
                |  sleep 1
                |  if ! pidof icewmbg >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root icewmbg >/tmp/icewmbg.log 2>&1 &
                |  else
                |    icewmbg -r >/tmp/icewmbg.log 2>&1 || true
                |  fi
            """.trimMargin()

            com.example.model.LinuxDesktopEnvironment.XFCE4 -> """
                |  if command -v icewmbg >/dev/null 2>&1 && ! pidof icewmbg >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root icewmbg >/tmp/icewmbg.log 2>&1 &
                |  fi
                |  nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root xfwm4 --replace >/tmp/xfwm4.log 2>&1 &
                |  sleep 1
                |  nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root xfce4-panel --disable-wm-check >/tmp/xfce4-panel.log 2>&1 &
                |  nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root xfdesktop >/tmp/xfdesktop.log 2>&1 &
                |  sleep 1
                |  if ! pidof xfce4-panel >/dev/null 2>&1 && command -v tint2 >/dev/null 2>&1 && ! pidof tint2 >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root tint2 -c /root/.config/tint2/tint2rc >/tmp/tint2.log 2>&1 &
                |  fi
            """.trimMargin()

            com.example.model.LinuxDesktopEnvironment.OPENBOX -> """
                |  if command -v icewmbg >/dev/null 2>&1 && ! pidof icewmbg >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root icewmbg >/tmp/icewmbg.log 2>&1 &
                |  fi
                |  nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root openbox --replace >/tmp/openbox.log 2>&1 &
                |  sleep 1
                |  if command -v tint2 >/dev/null 2>&1 && ! pidof tint2 >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root tint2 -c /root/.config/tint2/tint2rc >/tmp/tint2.log 2>&1 &
                |  fi
            """.trimMargin()

            com.example.model.LinuxDesktopEnvironment.LXQT -> """
                |  if command -v icewmbg >/dev/null 2>&1 && ! pidof icewmbg >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root icewmbg >/tmp/icewmbg.log 2>&1 &
                |  fi
                |  nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root openbox --replace >/tmp/lxqt-wm.log 2>&1 &
                |  sleep 1
                |  if command -v pcmanfm-qt >/dev/null 2>&1 && ! pidof pcmanfm-qt >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root pcmanfm-qt --desktop >/tmp/pcmanfm-qt.log 2>&1 &
                |  fi
                |  if command -v lxqt-panel >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root lxqt-panel >/tmp/lxqt-panel.log 2>&1 &
                |  elif command -v lxpanel >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root lxpanel >/tmp/lxpanel.log 2>&1 &
                |  fi
                |  sleep 1
                |  if ! pidof lxqt-panel >/dev/null 2>&1 && ! pidof lxpanel >/dev/null 2>&1 && command -v tint2 >/dev/null 2>&1 && ! pidof tint2 >/dev/null 2>&1; then
                |    nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root tint2 -c /root/.config/tint2/tint2rc >/tmp/tint2.log 2>&1 &
                |  fi
            """.trimMargin()
        }

        val forceFlag = if (forceSwitch) "1" else "0"

        return """
        |started=
        |mkdir -p /tmp/.X11-unix /tmp/runtime-root /var/run/dbus /run/dbus
        |chmod 1777 /tmp/.X11-unix
        |chmod 700 /tmp/runtime-root
        |export DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root
        |if ! command -v Xvfb >/dev/null 2>&1 || ! command -v x11vnc >/dev/null 2>&1; then
        |  echo '[x11] installing X11, VNC and noVNC inside the container (first run)...'
        |  if command -v apk >/dev/null 2>&1; then
        |    apk add --no-cache xvfb x11vnc novnc icewm xterm font-misc-misc xsetroot font-dejavu fontconfig
        |  elif command -v apt-get >/dev/null 2>&1; then
        |    DEBIAN_FRONTEND=noninteractive apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends xvfb x11vnc novnc websockify icewm xterm x11-xserver-utils fonts-dejavu-core fontconfig
        |  elif command -v pacman >/dev/null 2>&1; then
        |    pacman -Sy --noconfirm xorg-server-xvfb x11vnc novnc websockify icewm xterm xorg-xsetroot ttf-dejavu fontconfig
        |  fi
        |  started="${'$'}started x11-base"
        |fi
        |if ! ($deCheckCondition); then
        |  echo '[x11] installing ${de.title} packages (keeping current desktop active during install)...'
        |  if command -v apk >/dev/null 2>&1; then
        |    apk add --no-cache ${de.apkPackages} || true
        |    $lxqtExtraApk
        |  elif command -v apt-get >/dev/null 2>&1; then
        |    DEBIAN_FRONTEND=noninteractive apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ${de.aptPackages} || true
        |  elif command -v pacman >/dev/null 2>&1; then
        |    pacman -Sy --noconfirm ${de.pacmanPackages} || true
        |  fi
        |  started="${'$'}started ${de.id}-packages"
        |fi
        |if command -v dbus-uuidgen >/dev/null 2>&1; then
        |  dbus-uuidgen --ensure=/etc/machine-id 2>/dev/null || true
        |  dbus-uuidgen --ensure 2>/dev/null || true
        |fi
        |if command -v dbus-daemon >/dev/null 2>&1 && ! pidof dbus-daemon >/dev/null 2>&1; then
        |  rm -f /var/run/dbus/pid /run/dbus/pid
        |  dbus-daemon --system --fork 2>/dev/null || true
        |fi
        |if ! pidof Xvfb >/dev/null 2>&1; then
        |  nohup Xvfb :1 -screen 0 1280x720x24 -nolisten tcp >/tmp/xvfb.log 2>&1 &
        |  sleep 2
        |  started="${'$'}started Xvfb"
        |fi
        |if ! pidof x11vnc >/dev/null 2>&1; then
        |  nohup env DISPLAY=:1 HOME=/root x11vnc -display :1 -noshm -forever -shared -localhost -rfbport 5901 -nopw >/tmp/x11vnc.log 2>&1 &
        |  started="${'$'}started x11vnc"
        |fi
        |if ! pidof websockify >/dev/null 2>&1; then
        |  nohup websockify --web=/usr/share/novnc 127.0.0.1:$PORT 127.0.0.1:5901 >/tmp/ws.log 2>&1 &
        |  started="${'$'}started websockify"
        |fi
        |xsetroot -solid '#0B1220' 2>/dev/null || true
        |if [ "$forceFlag" = "1" ] || ! pidof ${de.processName} >/dev/null 2>&1; then
        |  if ($deCheckCondition) || command -v ${de.processName} >/dev/null 2>&1; then
        |    for proc in icewm icewm-session icewmtray xfce4-session xfwm4 xfce4-panel xfdesktop openbox tint2 lxqt-session lxqt-panel lxpanel pcmanfm-qt; do
        |      pkill -x "${'$'}proc" >/dev/null 2>&1 || killall "${'$'}proc" >/dev/null 2>&1 || true
        |    done
        |    sleep 1
        |$launchTargetDeBlock
        |    started="${'$'}started ${de.id}"
        |  else
        |    echo '[x11] ${de.title} packages are not installed (check network); keeping IceWM active so desktop functions remain intact.'
        |  fi
        |fi
        |sleep 1
        |if ! pidof icewm >/dev/null 2>&1 && ! pidof xfwm4 >/dev/null 2>&1 && ! pidof openbox >/dev/null 2>&1; then
        |  echo '[x11] restoring IceWM fallback window manager & taskbar...'
        |  nohup env DISPLAY=:1 HOME=/root icewm --replace >/tmp/icewm.log 2>&1 &
        |  nohup env DISPLAY=:1 HOME=/root icewmbg >/tmp/icewmbg.log 2>&1 &
        |  started="${'$'}started icewm-fallback"
        |fi
        |if ! pidof xterm >/dev/null 2>&1 && ! pidof xfce4-terminal >/dev/null 2>&1 && ! pidof qterminal >/dev/null 2>&1; then
        |  nohup env DISPLAY=:1 HOME=/root xterm -fa 'DejaVu Sans Mono' -fs 10 -bg '#0B1220' -fg '#DBEAFE' -cr '#F59E0B' -geometry 100x28+24+16 -e /bin/sh >/tmp/xterm.log 2>&1 &
        |  sleep 1
        |  started="${'$'}started xterm"
        |fi
        |[ -z "${'$'}started" ] || echo "[x11] started:${'$'}started"
        |echo '[x11] ${de.title} ready on display :1 (http://127.0.0.1:$PORT)'
        """.trimMargin()
    }

    @Volatile
    private var startRequested = false

    /** Which container boot the current request belongs to; a reboot invalidates it. */
    @Volatile
    private var startedGeneration = -1

    /** Which DE is currently running in the guest session. */
    @Volatile
    private var runningDeId: String? = null

    /**
     * Makes sure the guest is serving the display page and that Xvfb/x11vnc/websockify are up.
     * Safe to call repeatedly and from any thread.
     */
    @Synchronized
    fun ensureStarted(context: Context): Boolean {
        installPage(context)
        val de = activeDesktopEnvironment(context)
        // Theme files are part of the product, not a manual setup step: a fresh container gets
        // them before the window manager starts, and an app update that changes them restarts
        // IceWM so the change is visible without the customer doing anything.
        if (LinuxTheme.install(context)) {
            startRequested = false
            if (LinuxRuntime.isRunning && de == com.example.model.LinuxDesktopEnvironment.ICEWM && runningDeId == "icewm") {
                LinuxRuntime.send(RESTART_WM_SCRIPT)
            }
        }
        val needSwitch = runningDeId != null && runningDeId != de.id
        if (!needSwitch && startRequested && startedGeneration == LinuxRuntime.generation && runningDeId == de.id) {
            return true
        }
        val sent = LinuxRuntime.send(buildStartScript(de, forceSwitch = needSwitch))
        startRequested = sent
        if (sent) {
            startedGeneration = LinuxRuntime.generation
            runningDeId = de.id
        }
        return sent
    }

    /**
     * Switches the active X11 Desktop Environment on DISPLAY=:1 without tearing down Xvfb, VNC,
     * or any running X11 client windows.
     */
    @Synchronized
    fun switchDesktopEnvironment(
        context: Context,
        de: com.example.model.LinuxDesktopEnvironment
    ): Boolean {
        setPreferredDesktopEnvironment(context, de)
        installPage(context)
        LinuxTheme.install(context)
        val sent = LinuxRuntime.send(buildStartScript(de, forceSwitch = true))
        startRequested = sent
        if (sent) {
            startedGeneration = LinuxRuntime.generation
            runningDeId = de.id
        }
        return sent
    }

    /**
     * Launches a graphical X11 application onto DISPLAY=:1 inside the Linux Desktop window.
     */
    fun launchGuiApp(context: Context, command: String): Boolean {
        ensureStarted(context)
        val clean = command.trim()
        if (clean.isEmpty()) return false
        return LinuxRuntime.send(
            "nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root $clean >/tmp/x11-app.log 2>&1 & " +
                "echo '[x11] launched $clean on DISPLAY=:1'"
        )
    }

    /** Replaces a running window manager with the current theme; open windows survive. */
    private const val RESTART_WM_SCRIPT =
        "if pidof Xvfb >/dev/null 2>&1; then " +
            "pkill -x icewm >/dev/null 2>&1; sleep 1; " +
            "nohup env DISPLAY=:1 HOME=/root icewm --replace >/tmp/icewm.log 2>&1 & " +
            "sleep 1; pidof icewmbg >/dev/null 2>&1 || nohup env DISPLAY=:1 HOME=/root icewmbg " +
            ">/tmp/icewmbg.log 2>&1 & fi"

    /** Explicit user-facing recovery: re-assert the active DE window manager and taskbar on :1. */
    @Synchronized
    fun restart(context: Context) {
        startRequested = false
        startedGeneration = -1
        runningDeId = null
        val de = activeDesktopEnvironment(context)
        installPage(context)
        LinuxTheme.install(context)
        val sent = LinuxRuntime.send(buildStartScript(de, forceSwitch = true))
        startRequested = sent
        if (sent) {
            startedGeneration = LinuxRuntime.generation
            runningDeId = de.id
        }
    }

    /** Kills the display stack inside the guest; the container itself keeps running. */
    @Synchronized
    fun stop(): Boolean {
        startRequested = false
        startedGeneration = -1
        runningDeId = null
        return LinuxRuntime.send(
            "pkill -f websockify; pkill x11vnc; pkill Xvfb; echo '[x11] display stopped'"
        )
    }

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
