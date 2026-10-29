package com.example.model

/**
 * A Linux distribution that Phonix actually has on the device.
 *
 * There is deliberately no catalogue of downloadable distributions here. Only Alpine is
 * bundled, built and tested on this hardware; listing Ubuntu, Kali, Fedora and the rest with
 * invented download sizes and "pre-configured" packages was a promise the app could not keep.
 * When another rootfs is really packaged, it gets an entry here.
 */
data class LinuxDistro(
    val id: String,
    val name: String,
    val version: String,
    val base: String,
    val desktopEnvironment: String,
    val description: String,
    val targetUse: String,
    /** Packages that are present in the bundled rootfs, not aspirations. */
    val bundledPackages: List<String>
)

object DistroCatalog {

    /**
     * The one distribution Phonix ships: an Alpine minirootfs run through proot.
     * [version] is the bundled release; the screen shows the version read from the
     * installed rootfs, so an updated bundle cannot disagree with the UI.
     */
    val alpine = LinuxDistro(
        id = "alpine",
        name = "Alpine Linux",
        version = "3.24.3",
        base = "Alpine Linux aarch64 · musl · busybox",
        desktopEnvironment = "IceWM on Xvfb, served to the desktop over VNC",
        description = "A real Linux userspace running on the phone's own Android kernel through " +
            "proot, inside app storage. No root, no partition, no second operating system: " +
            "Android keeps working underneath, so the phone apps and the Linux side coexist.",
        targetUse = "Terminal, package installs, lightweight desktop",
        bundledPackages = listOf("busybox", "icewm", "xterm")
    )

    /** Every distribution Phonix can currently run. */
    val distros = listOf(alpine)
}
