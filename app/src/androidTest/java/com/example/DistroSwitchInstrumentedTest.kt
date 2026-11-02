package com.example

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.engine.LinuxContainer
import com.example.model.DistroCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Performs a real distribution switch on the device through the same code the hub's
 * "Install & Switch" button calls.
 *
 * This exists because the hub cannot be driven by synthetic taps: the DeX display's Compose
 * buttons are merged into their parent's semantics node, so `uiautomator dump` reports
 * `bounds="[0,0][0,0]"` for them and `input tap` has nothing to aim at. The switch is the one
 * feature whose whole value is the download-plus-unpack-plus-promote sequence, so it is measured
 * here instead of being claimed from a screenshot.
 *
 * Target distro comes from the instrumentation argument `distro` (default `kali`, the smallest
 * streamed image), so the same test can switch out and back:
 *
 *   gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.distro=kali
 *
 * It writes every progress line to logcat under [TAG] so a long unpack is observable from outside.
 */
@RunWith(AndroidJUnit4::class)
class DistroSwitchInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun switchToRequestedDistro() {
        val requested = InstrumentationRegistry.getArguments().getString("distro") ?: "kali"
        val target = DistroCatalog.getDistro(requested)
        val container = LinuxContainer(context)

        assertFalse("${target.name} is bundled; this test is for the streamed path", target.isBundled)

        val before = container.activeDistroId()
        assertTrue("no guest is installed to unpack with, so the switch cannot run", container.isInstalled)
        Log.i(TAG, "switching $before -> ${target.id}; alpine rescue present=" + alpineRescue().isDirectory)

        val progress = mutableListOf<String>()
        container.switchDistro(
            distro = target,
            debloatPrevious = true,
            onProgress = { line ->
                progress += line
                Log.i(TAG, line)
            }
        )

        // 1. The choice is persisted, not just unpacked.
        assertEquals("active distro after the switch", target.id, container.activeDistroId())
        assertEquals("activeDistro() reads the persisted id", target.id, container.activeDistro().id)

        // 2. The new tree is really a rootfs, and the marker exists so a half-unpacked tree can
        //    never be treated as ready on the next launch.
        val rootfs = container.rootfs
        assertTrue("${rootfs.name} missing", rootfs.isDirectory)
        assertTrue(".phonix-installed marker missing", File(rootfs, LinuxContainer.MARKER).isFile)
        assertTrue("new rootfs has no /bin/sh", File(rootfs, "bin/sh").exists())
        assertTrue("new rootfs has no /etc/os-release", File(rootfs, "etc/os-release").isFile)

        // 3. It boots: run a command inside the guest and read the release it reports itself.
        val output = StringBuilder()
        val status = container.execOnce("cat /etc/os-release; echo ID=\$(id -u); uname -m") { line ->
            output.appendLine(line)
        }
        Log.i(TAG, "guest os-release:\n$output")
        assertEquals("guest shell exited non-zero", 0, status)
        val pretty = output.lines().firstOrNull { it.startsWith("PRETTY_NAME=") }.orEmpty()
        assertTrue(
            "guest reports '$pretty', expected it to identify ${target.name}",
            pretty.contains(EXPECTED_PRETTY[target.id] ?: target.name, ignoreCase = true)
        )

        // 4. Staging and the download cache are gone: they sit inside the guest and the previous
        //    code left up to a few hundred MB behind.
        assertFalse("leftover .phonix-stage", File(rootfs, "root/.phonix-stage").exists())
        assertFalse("leftover .phonix-cache", File(rootfs, "root/.phonix-cache").exists())
        assertFalse(
            "leftover staging sibling",
            File(context.filesDir, "linux/.${target.id}-staging").exists()
        )

        // 5. The pruned guest is gone but the bundled rescue image is not — the promise that a
        //    switch can never leave the app with no userspace at all.
        if (before != target.id) {
            assertFalse(
                "$before should have been pruned after a successful switch",
                File(context.filesDir, "linux/$before").exists()
            )
        }
        assertTrue("the bundled Alpine rescue image was pruned", alpineRescue().isDirectory)
        assertTrue("linux/tmp work dir missing", File(context.filesDir, "linux/tmp").isDirectory)

        Log.i(TAG, "switch OK: ${target.id} ${container.version()}, ${progress.size} progress lines")
    }

    private fun alpineRescue() = File(context.filesDir, "linux/alpine")

    companion object {
        private const val TAG = "PhonixSwitchTest"

        /** What each image calls itself in /etc/os-release. */
        private val EXPECTED_PRETTY = mapOf(
            "kali" to "Kali",
            "debian" to "Debian",
            "ubuntu" to "Ubuntu",
            "arch" to "Arch"
        )
    }
}
