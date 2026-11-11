package com.example.model

/**
 * A Linux distribution supported by Phonix on ARM64 Android kernels through PRoot.
 */
data class LinuxDistro(
    val id: String,
    val name: String,
    val version: String,
    val base: String,
    val desktopEnvironment: String,
    val description: String,
    val targetUse: String,
    val downloadSizeMb: Int,
    val installedSizeMb: Int,
    val packageManager: String,
    val downloadUrl: String,
    val isBundled: Boolean = false,
    val badgeText: String = "",
    val bundledPackages: List<String>
) {
    val downloadSizeEstimate: String get() = if (isBundled) "${downloadSizeMb} MB (Bundled)" else "${downloadSizeMb} MB"
    val installedDiskFootprint: String get() = "${installedSizeMb} MB"
}

/**
 * Graphical X11 Desktop Environments & Window Managers supported on DISPLAY=:1 over loopback VNC.
 */
enum class LinuxDesktopEnvironment(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val estRamMb: Int,
    val apkPackages: String,
    val aptPackages: String,
    val pacmanPackages: String,
    val processName: String,
    val startCommand: String
) {
    ICEWM(
        id = "icewm",
        title = "IceWM (Ultra-Lean)",
        subtitle = "~28 MB RAM • Instant startup",
        description = "Classic lightweight C++ window manager with taskbar, start menu, and Phonix dark theme.",
        estRamMb = 28,
        apkPackages = "icewm xterm font-dejavu",
        aptPackages = "icewm xterm fonts-dejavu-core",
        pacmanPackages = "icewm xterm ttf-dejavu",
        processName = "icewm",
        startCommand = "nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root icewm --replace >/tmp/icewm.log 2>&1 &"
    ),
    XFCE4(
        id = "xfce4",
        title = "XFCE4 Desktop",
        subtitle = "~145 MB RAM • Full Linux Workstation",
        description = "Complete desktop environment with xfwm4 window manager, xfce4-panel taskbar, Thunar file manager, and xfdesktop.",
        estRamMb = 145,
        apkPackages = "xfce4 xfwm4 xfce4-panel xfdesktop xfce4-session xfce4-terminal thunar dbus dbus-x11 adwaita-icon-theme font-dejavu",
        aptPackages = "xfce4 xfwm4 xfce4-panel xfdesktop4 xfce4-session xfce4-terminal thunar dbus-x11 adwaita-icon-theme fonts-dejavu-core",
        pacmanPackages = "xfce4 xfwm4 xfce4-panel xfdesktop xfce4-session xfce4-terminal thunar dbus adwaita-icon-theme ttf-dejavu",
        processName = "xfwm4",
        startCommand = "nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root xfwm4 --replace >/tmp/xfwm4.log 2>&1 & " +
            "nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root xfce4-panel --disable-wm-check >/tmp/xfce4-panel.log 2>&1 & " +
            "nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root xfdesktop >/tmp/xfdesktop.log 2>&1 &"
    ),
    OPENBOX(
        id = "openbox",
        title = "Openbox + Tint2",
        subtitle = "~25 MB RAM • Floating WM + Taskbar",
        description = "Fast standards-compliant Openbox window manager paired with a Phonix dark-slate tint2 taskbar, launcher buttons, and root menu.",
        estRamMb = 25,
        apkPackages = "openbox tint2 xterm font-dejavu",
        aptPackages = "openbox tint2 xterm fonts-dejavu-core",
        pacmanPackages = "openbox tint2 xterm ttf-dejavu",
        processName = "openbox",
        startCommand = "nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root openbox --replace >/tmp/openbox.log 2>&1 & " +
            "nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root tint2 -c /root/.config/tint2/tint2rc >/tmp/tint2.log 2>&1 &"
    ),
    LXQT(
        id = "lxqt",
        title = "LXQt Desktop",
        subtitle = "~110 MB RAM • Modern Qt Workstation",
        description = "Lightweight Qt desktop environment backed by Openbox window management, PCManFM-Qt file manager, QTerminal, and bottom panel.",
        estRamMb = 110,
        apkPackages = "openbox tint2 pcmanfm-qt qterminal dbus dbus-x11 adwaita-icon-theme font-dejavu",
        aptPackages = "openbox tint2 lxqt-core lxqt-panel lxqt-session pcmanfm-qt qterminal dbus-x11 adwaita-icon-theme fonts-dejavu-core",
        pacmanPackages = "openbox tint2 lxqt lxqt-panel lxqt-session pcmanfm-qt qterminal dbus adwaita-icon-theme ttf-dejavu",
        processName = "openbox",
        startCommand = "nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root openbox --replace >/tmp/lxqt-wm.log 2>&1 & " +
            "if command -v lxqt-panel >/dev/null 2>&1; then nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root lxqt-panel >/tmp/lxqt-panel.log 2>&1 & " +
            "else nohup env DISPLAY=:1 HOME=/root XDG_RUNTIME_DIR=/tmp/runtime-root tint2 -c /root/.config/tint2/tint2rc >/tmp/tint2.log 2>&1 & fi"
    );

    val shortLabel: String get() = title.substringBefore(" ")
    val ramEstimate: String get() = "~${estRamMb} MB RAM"

    companion object {
        fun fromId(id: String?): LinuxDesktopEnvironment =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ICEWM
    }
}

object DistroCatalog {

    /**
     * The ultra-lean default distribution shipped inside the APK.
     */
    val alpine = LinuxDistro(
        id = "alpine",
        name = "Alpine Linux",
        version = "3.24.3",
        base = "Alpine Linux aarch64 · musl · busybox",
        desktopEnvironment = "IceWM / XFCE4 on Xvfb, served over loopback VNC",
        description = "Minimalist security-oriented userspace. Bundled inside the APK for zero-download instant startup and ultra-low RAM overhead.",
        targetUse = "Instant boot default, lightweight terminal, scripts, minimal desktop, homelab daemons",
        downloadSizeMb = 4,
        installedSizeMb = 9,
        packageManager = "apk",
        downloadUrl = "",
        isBundled = true,
        badgeText = "BUNDLED DEFAULT • INSTANT BOOT",
        bundledPackages = listOf("busybox", "icewm", "xterm", "apk-tools")
    )

    /**
     * Debian 12 (Bookworm) Minimal - Universal OS with APT repositories.
     */
    val debian = LinuxDistro(
        id = "debian",
        name = "Debian 12 (Bookworm)",
        version = "12.7",
        base = "Debian GNU/Linux aarch64 · glibc · APT",
        desktopEnvironment = "XFCE4 / IceWM / Openbox on Xvfb",
        description = "Official Debian Bookworm arm64 rootfs from LinuxContainers. Full Debian APT repositories and glibc compatibility (downloaded on demand over Wi-Fi).",
        targetUse = "Full software development, Termux proot-distro style APT packages, stable servers",
        downloadSizeMb = 85,
        installedSizeMb = 260,
        packageManager = "apt",
        downloadUrl = "https://images.linuxcontainers.org/images/debian/bookworm/arm64/default/",
        isBundled = false,
        badgeText = "RECOMMENDED FOR APT & GLIBC",
        bundledPackages = listOf("bash", "apt", "coreutils", "dpkg")
    )

    /**
     * Ubuntu 24.04 LTS (Noble Numbat) Base.
     */
    val ubuntu = LinuxDistro(
        id = "ubuntu",
        name = "Ubuntu 24.04 LTS",
        version = "24.04.1 LTS",
        base = "Ubuntu Base aarch64 · glibc · Canonical APT",
        desktopEnvironment = "XFCE4 / LXQt / Openbox on Xvfb",
        description = "Official Canonical Ubuntu Base 24.04.1 arm64 rootfs (.tar.gz). Includes apt/dpkg and glibc userspace (downloaded on demand over Wi-Fi).",
        targetUse = "Modern developer toolchain, Ubuntu APT packages, Python & Node.js",
        downloadSizeMb = 30,
        installedSizeMb = 110,
        packageManager = "apt",
        downloadUrl = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.1-base-arm64.tar.gz",
        isBundled = false,
        badgeText = "OFFICIAL UBUNTU BASE • APT",
        bundledPackages = listOf("bash", "apt", "dpkg", "coreutils", "tar")
    )

    /**
     * Arch Linux ARM - Bleeding-edge rolling release.
     */
    val arch = LinuxDistro(
        id = "arch",
        name = "Arch Linux ARM",
        version = "Rolling",
        base = "Arch Linux aarch64 · glibc · Pacman",
        desktopEnvironment = "Openbox / XFCE4 / LXQt on Xvfb",
        description = "Rolling-release Arch Linux arm64 rootfs from LinuxContainers with the Pacman package manager (downloaded on demand over Wi-Fi).",
        targetUse = "Bleeding-edge Linux packages, custom builds, Pacman repository",
        downloadSizeMb = 140,
        installedSizeMb = 420,
        packageManager = "pacman",
        downloadUrl = "https://images.linuxcontainers.org/images/archlinux/current/arm64/default/",
        isBundled = false,
        badgeText = "ROLLING RELEASE • PACMAN",
        bundledPackages = listOf("bash", "pacman", "coreutils", "grep")
    )

    /**
     * Kali Linux Minimal - Security & Network Diagnostics.
     */
    val kali = LinuxDistro(
        id = "kali",
        name = "Kali Linux Minimal",
        version = "Rolling",
        base = "Kali Linux aarch64 · Debian rolling · APT",
        desktopEnvironment = "XFCE4 / CLI security shell",
        description = "Kali Linux arm64 minimal rootfs from LinuxContainers with APT security and network diagnostic repositories (downloaded on demand over Wi-Fi).",
        targetUse = "Network diagnostics, wireless audits, forensic packet inspection",
        downloadSizeMb = 115,
        installedSizeMb = 360,
        packageManager = "apt",
        downloadUrl = "https://images.linuxcontainers.org/images/kali/current/arm64/default/",
        isBundled = false,
        badgeText = "SECURITY & DIAGNOSTICS",
        bundledPackages = listOf("bash", "apt", "dpkg", "coreutils")
    )

    /** Every distribution supported for on-demand streaming and switching. */
    val distros = listOf(alpine, debian, ubuntu, arch, kali)

    fun getDistro(id: String): LinuxDistro = distros.firstOrNull { it.id == id } ?: alpine
}
