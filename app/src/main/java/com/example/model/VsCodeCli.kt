package com.example.model

/**
 * The VS Code command-line interface, as the guest has to run it.
 *
 * Microsoft publishes two arm64 builds of the CLI: one linked against glibc and one against musl.
 * The wrong one fails at exec time with a loader error, so the build is chosen from the loader the
 * guest actually has rather than from the distribution's name.
 */
enum class GuestLibc(val os: String, val label: String, val supportsCli: Boolean) {
    GNU("cli-linux-arm64", "glibc", false),
    MUSL("cli-alpine-arm64", "musl", true);

    val downloadUrl: String get() = "https://code.visualstudio.com/sha/download?build=stable&os=" + os
}

object VsCodeCli {

    /** The port `serve-web` binds. Loopback only: the client is this phone's own WebView. */
    const val PORT = 8000

    /** Where the CLI is unpacked inside the guest, under the guest user's own home. */
    const val INSTALL_DIR = "\$HOME/.phonix/vscode"

    /**
     * What to tell a glibc guest. Measured 2026-11-02: the download endpoint answers **404** for
     * `os=cli-linux-arm64` and **302** for `os=cli-alpine-arm64`, and `cli-linux-x64` is 404 too,
     * so the musl build is the only arm64 CLI Microsoft publishes. Pretending otherwise would
     * install nothing and blame the network.
     */
    const val NO_ARMV64_GLIBC_CLI =
        "Microsoft publishes no glibc build of the arm64 CLI - the download URL answers 404. " +
            "The musl (Alpine) build is the only one, so switch the Linux guest to Alpine."

    private val SEMVER = Regex("[0-9]+[.][0-9]+[.][0-9]+")

    fun libc(hasMuslLoader: Boolean): GuestLibc = if (hasMuslLoader) GuestLibc.MUSL else GuestLibc.GNU

    /** One line: the runtime shell sends exactly one command per [com.example.engine.LinuxRuntime.run]. */
    fun detectLibcScript(): String =
        "test -e /lib/ld-musl-aarch64.so.1 && echo PHONIX_LIBC=MUSL || echo PHONIX_LIBC=GNU"

    fun libcFromProbe(output: String): GuestLibc = libc(output.contains("PHONIX_LIBC=MUSL"))

    /**
     * Downloads and unpacks the CLI, then prints its version.
     *
     * `--version` is the only evidence this code accepts that a runnable binary now exists: a URL
     * that 404s, an archive that does not unpack, or a binary the loader refuses all leave nothing
     * for it to print, and [parseVersion] then returns null.
     *
     * The bytes are fetched by the guest's own `curl`, not by the app: they never pass through the
     * app's storage, and the trust store doing the TLS check is the one the guest will run under.
     */
    fun installScript(libc: GuestLibc): String =
        "mkdir -p " + INSTALL_DIR + " && cd " + INSTALL_DIR + " && " +
            fetchCommand(libc) + " && " +
            "tar -xzf cli.tar.gz && chmod +x code && ./code --version"

    /**
     * Fetches the archive with whichever tool the guest image actually ships.
     *
     * The bundled Alpine image carries busybox `wget` and no `curl`; a downloaded Debian carries
     * `curl` and no `wget`. Hard-coding either one makes Code Studio work on exactly one of the
     * two guests this product ships, which is how it would have shipped broken on the image that
     * is inside the APK.
     */
    fun fetchCommand(libc: GuestLibc): String =
        "if command -v curl >/dev/null 2>&1; then curl -fsSL '" + libc.downloadUrl + "' -o cli.tar.gz; " +
            "else wget -q -O cli.tar.gz '" + libc.downloadUrl + "'; fi"

    /** Reads the installed CLI's version without fetching anything. */
    fun installedVersionProbe(): String = "cd " + INSTALL_DIR + " && ./code --version 2>/dev/null"

    /** Starts `serve-web` detached, so the shell returns instead of holding the server open. */
    fun serveCommand(port: Int = PORT): String =
        "cd " + INSTALL_DIR + " && nohup ./code serve-web --host 127.0.0.1 --port " + port +
            " --without-connection-token >/tmp/phonix-vscode.log 2>&1 &"

    /**
     * The tail of a command's output, so a refusal carries what the guest actually said.
     *
     * Without this the UI pointed at the Terminal for output the Terminal never printed: the command
     * is sent over the runtime's own shell channel, not typed there.
     */
    fun lastWords(output: String, lines: Int = 3, maxChars: Int = 240): String {
        val tail = output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
            .takeLast(lines)
        if (tail.isEmpty()) return " The guest printed nothing."
        val joined = tail.joinToString(" | ")
        return " Guest said: " + if (joined.length > maxChars) joined.take(maxChars) + "..." else joined
    }

    /** The version `code --version` printed, or null when it printed none. */
    fun parseVersion(output: String): String? =
        output.lineSequence()
            .map { it.trim() }
            .firstOrNull { SEMVER.matches(it) }
}
