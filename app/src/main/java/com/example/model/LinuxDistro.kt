package com.example.model

/**
 * A Linux distribution supported by Phonix on ARM64 Android kernels through PRoot.
 *
 * `image*` name a build on `images.linuxcontainers.org`; the exact archive URL is resolved at
 * install time from that mirror's index (see [DistroIndex]) because every build lives under a dated
 * directory. The bundled Alpine image has no mirror identity.
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
    val isBundled: Boolean = false,
    val imageName: String = "",
    val imageRelease: String = "",
    val bundledPackages: List<String>
) {
    /**
     * Install command that actually exists in this userland.
     *
     * The app used to print `apk add` everywhere, which on Debian fails with "apk: not found" -
     * the same fault that stopped the AI installer after the distro switch.
     */
    fun installCommand(packages: String): String = when (packageManager) {
        "apt" -> "apt-get install -y -q $packages"
        "pacman" -> "pacman -S --noconfirm --needed $packages"
        else -> "apk add --no-cache $packages"
    }

    /** Query that succeeds only when the package is really installed. */
    fun installedQuery(pkg: String): String = when (packageManager) {
        "apt" -> "dpkg -s $pkg >/dev/null 2>&1"
        "pacman" -> "pacman -Q $pkg >/dev/null 2>&1"
        else -> "apk info -e $pkg >/dev/null 2>&1"
    }

    /**
     * Packages that provide `unxz` and `tar` in this userland.
     *
     * The mirror only publishes `rootfs.tar.xz`, and Android ships no xz decoder, so the guest
     * does the unpacking. Almost every minimal image leaves the decoder out: Debian's `bin` and
     * `usr/bin` carry `tar` but no `xz`/`unxz`/`xzcat`, and the bundled Alpine busybox has
     * `gunzip`/`zcat` but was built without an xz applet. The package is `xz-utils` on apt
     * systems and `xz` everywhere else.
     */
    val unpackToolPackages: String get() = when (packageManager) {
        "apt" -> "xz-utils tar"
        else -> "xz tar"
    }

    /**
     * Command that makes this guest's package index usable, or null when it needs none.
     *
     * A freshly streamed image carries no usable index. Ubuntu's `apt-get install xz-utils tar`
     * failed with `E: Unable to fetch some archives, maybe run apt-get update or try with
     * --fix-missing?`, and pacman will not install from unsynced databases at all. `apk add
     * --no-cache` refreshes on its own, so Alpine needs nothing here.
     */
    val indexSyncCommand: String? get() = when (packageManager) {
        "apt" -> "apt-get update -q"
        "pacman" -> "pacman -Sy --noconfirm"
        else -> null
    }

    /** Approximate until the download starts, then replaced by the server's Content-Length. */
    val downloadSizeEstimate: String get() = "~${downloadSizeMb} MB"
    val installedDiskFootprint: String get() = "~${installedSizeMb} MB"
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
        desktopEnvironment = "IceWM on Xvfb, served over loopback VNC",
        description = "Minimalist security-oriented userspace. Uses musl libc and busybox for near-zero memory footprint and instantaneous startup.",
        targetUse = "Lightweight terminal, scripts, minimal desktop, homelab daemons",
        downloadSizeMb = 4,
        installedSizeMb = 9,
        packageManager = "apk",
        isBundled = true,
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
        desktopEnvironment = "Openbox / XFCE4 minimal on Xvfb",
        description = "The universal operating system with full Debian APT repositories, GCC, Python 3, and unmatched binary package compatibility.",
        targetUse = "Full software development, APT repository packages, stable servers",
        downloadSizeMb = 95,
        installedSizeMb = 300,
        packageManager = "apt",
        imageName = "debian",
        imageRelease = "bookworm",
        bundledPackages = listOf("bash", "apt", "coreutils", "curl", "dpkg")
    )

    /**
     * Ubuntu 24.04 LTS (Noble Numbat) Base.
     */
    val ubuntu = LinuxDistro(
        id = "ubuntu",
        name = "Ubuntu 24.04 LTS",
        version = "24.04 LTS",
        base = "Ubuntu Noble aarch64 · glibc · Canonical ecosystem",
        desktopEnvironment = "Openbox / X11 desktop on Xvfb",
        description = "Modern Canonical Ubuntu userspace with up-to-date developer toolchains, Node.js, Python, and cloud tooling.",
        targetUse = "Modern developer toolchain, cloud CLI tools, Node.js, Docker client",
        downloadSizeMb = 127,
        installedSizeMb = 420,
        packageManager = "apt",
        imageName = "ubuntu",
        imageRelease = "noble",
        bundledPackages = listOf("bash", "apt", "git", "curl", "tar")
    )

    /**
     * Arch Linux ARM - Bleeding-edge rolling release.
     */
    val arch = LinuxDistro(
        id = "arch",
        name = "Arch Linux ARM",
        version = "Rolling",
        base = "Arch Linux aarch64 · glibc · Pacman",
        desktopEnvironment = "i3wm / Openbox on Xvfb",
        description = "Rolling-release minimalist Linux userspace with the bleeding-edge Pacman package ecosystem and latest compilers.",
        targetUse = "Bleeding-edge Linux packages, custom builds, Pacman repository",
        downloadSizeMb = 178,
        installedSizeMb = 560,
        packageManager = "pacman",
        imageName = "archlinux",
        imageRelease = "current",
        bundledPackages = listOf("bash", "pacman", "coreutils", "which", "grep")
    )

    /**
     * Kali Linux Minimal - Security & Network Diagnostics.
     */
    val kali = LinuxDistro(
        id = "kali",
        name = "Kali Linux Minimal",
        version = "Rolling",
        base = "Kali Linux aarch64 · Debian rolling · Security",
        desktopEnvironment = "CLI / X11 security shell",
        description = "Security auditing and network diagnostics distribution equipped with standard network telemetry and diagnostic suites.",
        targetUse = "Network diagnostics, wireless audits, forensic packet inspection",
        downloadSizeMb = 78,
        installedSizeMb = 250,
        packageManager = "apt",
        imageName = "kali",
        imageRelease = "current",
        bundledPackages = listOf("bash", "apt", "nmap", "tcpdump", "net-tools")
    )

    /** Every distribution supported for on-demand streaming and switching. */
    val distros = listOf(alpine, debian, ubuntu, arch, kali)

    fun getDistro(id: String): LinuxDistro = distros.firstOrNull { it.id == id } ?: alpine

    /**
     * Looks a distribution up by the id an intent or a manifest names.
     *
     * The ids are the ones the hub and the store already use; an unknown id returns null so a
     * caller can say so instead of quietly switching to something else.
     */
    fun byId(id: String): LinuxDistro? = when (id.trim().lowercase(java.util.Locale.ROOT)) {
        "alpine" -> alpine
        "debian" -> debian
        "ubuntu" -> ubuntu
        "arch" -> arch
        else -> null
    }
}
