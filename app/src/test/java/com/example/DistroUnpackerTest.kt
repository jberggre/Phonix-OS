package com.example

import com.example.model.DistroCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The streamed installer unpacks `rootfs.tar.xz` inside the running guest, because Android has no
 * xz decoder of its own. That makes `unxz` a hard prerequisite, and almost no minimal image ships
 * one: Debian 12's guest has `tar` and no `xz`/`unxz`/`xzcat` at all, and the bundled Alpine
 * busybox was built without an xz applet.

 * Missing it is not a cosmetic fault - the switch downloaded and SHA-256-verified a 74 MB archive
 * and only then failed with `unxz: not found` and `tar: This does not look like a tar archive`.
 * These assertions pin the package names the fix installs, per package manager.
 */
class DistroUnpackerTest {

    @Test
    fun `apt guests are told to install xz-utils`() {
        val apt = DistroCatalog.distros.filter { it.packageManager == "apt" }
        assertTrue("the catalog should carry apt guests", apt.isNotEmpty())
        apt.forEach { distro ->
            assertEquals(
                "${distro.name} must install xz-utils, not xz",
                "xz-utils tar",
                distro.unpackToolPackages
            )
        }
    }

    @Test
    fun `non-apt guests use the xz package name`() {
        val others = DistroCatalog.distros.filter { it.packageManager != "apt" }
        assertTrue(others.isNotEmpty())
        others.forEach { distro ->
            assertEquals("${distro.name} package names", "xz tar", distro.unpackToolPackages)
        }
    }

    @Test
    fun `apt and pacman guests refresh their index before installing`() {
        assertEquals("apt-get update -q", DistroCatalog.debian.indexSyncCommand)
        assertEquals("apt-get update -q", DistroCatalog.ubuntu.indexSyncCommand)
        assertEquals("pacman -Sy --noconfirm", DistroCatalog.arch.indexSyncCommand)
        // `apk add --no-cache` refreshes by itself, so Alpine needs no separate sync.
        assertEquals(null, DistroCatalog.alpine.indexSyncCommand)
    }

    @Test
    fun `the install command matches the guest package manager`() {
        assertEquals(
            "apt-get install -y -q xz-utils tar",
            DistroCatalog.debian.installCommand(DistroCatalog.debian.unpackToolPackages)
        )
        assertEquals(
            "apk add --no-cache xz tar",
            DistroCatalog.alpine.installCommand(DistroCatalog.alpine.unpackToolPackages)
        )
        assertEquals(
            "pacman -S --noconfirm --needed xz tar",
            DistroCatalog.arch.installCommand(DistroCatalog.arch.unpackToolPackages)
        )
    }
}
