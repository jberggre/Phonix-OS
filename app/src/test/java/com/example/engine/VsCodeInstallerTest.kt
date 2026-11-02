package com.example.engine

import com.example.model.GuestLibc
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The installer's decisions, with the guest replaced by a scripted shell.
 *
 * What matters here is that nothing short of a printed version counts as installed, and that the
 * build asked for matches the loader the probe found.
 */
class VsCodeInstallerTest {

    private class FakeShell(vararg answers: String?) {
        private val responses = answers.toList()
        val commands = mutableListOf<String>()
        suspend fun run(command: String, timeoutMs: Long): String? {
            commands += command
            return responses.getOrNull(commands.size - 1)
        }
    }

    @Test
    fun aPrintedVersionMeansTheCliIsInstalled() = runBlocking {
        val shell = FakeShell("PHONIX_LIBC=MUSL", "1.109.5" + System.lineSeparator() + "abc" + System.lineSeparator() + "arm64")
        val outcome = VsCodeInstaller(shell::run).install()
        assertEquals(VsCodeInstall.Installed("1.109.5", GuestLibc.MUSL), outcome)
    }

    @Test
    fun aMuslGuestIsAskedForTheAlpineBuild() = runBlocking {
        val shell = FakeShell("PHONIX_LIBC=MUSL", "1.109.5\n0e1a5c\narm64\n")
        VsCodeInstaller(shell::run).install()
        assertEquals(2, shell.commands.size)
        assertTrue(shell.commands[1], shell.commands[1].contains("os=cli-alpine-arm64"))
    }

    @Test
    fun aShellThatNeverAnswersIsRefusedRatherThanAssumed() = runBlocking {
        val outcome = VsCodeInstaller { _, _ -> null }.install()
        assertTrue(outcome is VsCodeInstall.Refused)
        assertTrue((outcome as VsCodeInstall.Refused).reason.contains("guest shell did not answer"))
    }

    @Test
    fun aFailedDownloadIsRefusedWithAReasonAndNothingInstalled() = runBlocking {
        val shell = FakeShell("PHONIX_LIBC=MUSL", "curl: (22) The requested URL returned error: 404")
        val outcome = VsCodeInstaller(shell::run).install()
        assertTrue(outcome is VsCodeInstall.Refused)
        assertTrue((outcome as VsCodeInstall.Refused).reason.contains("did not report a version"))
    }

    @Test
    fun aGlibcGuestIsRefusedBeforeAnyDownload() = runBlocking {
        val shell = FakeShell("PHONIX_LIBC=GNU")
        val outcome = VsCodeInstaller(shell::run).install()
        assertTrue(outcome is VsCodeInstall.Refused)
        assertTrue((outcome as VsCodeInstall.Refused).reason.contains("no glibc build"))
        assertEquals("the probe must be the only command run", 1, shell.commands.size)
    }

    @Test
    fun serveReportsWhetherTheShellTookTheCommand() = runBlocking {
        assertTrue(VsCodeInstaller { _, _ -> "" }.serve())
        assertFalse(VsCodeInstaller { _, _ -> null }.serve())
    }
}
