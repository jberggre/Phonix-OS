package com.example.engine

import com.example.model.GuestLibc
import com.example.model.VsCodeCli

/** What one install attempt produced. */
sealed interface VsCodeInstall {
    data class Installed(val version: String, val libc: GuestLibc) : VsCodeInstall
    data class Refused(val reason: String) : VsCodeInstall
}

/**
 * Installs Microsoft's VS Code CLI into the live guest.
 *
 * The work itself is a shell script the guest runs; this class decides what to ask, how long to
 * wait, and what the answer means, so the whole decision table is testable without a phone. A
 * download that fails, an archive that does not unpack and a binary the loader rejects all end the
 * same way: no version line, and the caller is told nothing is installed.
 */
class VsCodeInstaller(
    private val run: suspend (String, Long) -> String?
) {

    /** Reads the build the guest can execute, installs that one, and reports the version it prints. */
    suspend fun install(): VsCodeInstall {
        val probe = run(VsCodeCli.detectLibcScript(), PROBE_TIMEOUT_MS)
            ?: return VsCodeInstall.Refused(SHELL_ABSENT)
        val libc = VsCodeCli.libcFromProbe(probe)
        if (!libc.supportsCli) return VsCodeInstall.Refused(VsCodeCli.NO_ARMV64_GLIBC_CLI)
        val output = run(VsCodeCli.installScript(libc), DOWNLOAD_TIMEOUT_MS)
            ?: return VsCodeInstall.Refused(SHELL_ABSENT)
        val version = VsCodeCli.parseVersion(output)
            ?: return VsCodeInstall.Refused(
                "The CLI did not report a version, so nothing is installed." + VsCodeCli.lastWords(output)
            )
        return VsCodeInstall.Installed(version, libc)
    }

    /** Starts the web server. False when the guest shell would not take the command. */
    suspend fun serve(): Boolean = run(VsCodeCli.serveCommand(), SERVE_TIMEOUT_MS) != null

    companion object {
        const val PROBE_TIMEOUT_MS = 20_000L
        const val DOWNLOAD_TIMEOUT_MS = 900_000L
        const val SERVE_TIMEOUT_MS = 20_000L
        const val SHELL_ABSENT = "The guest shell did not answer; start the Linux guest and try again."
    }
}
