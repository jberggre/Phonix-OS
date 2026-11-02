package com.example.engine

import android.content.Context
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * A real Linux userspace running inside the app.
 *
 * There is no root, no kernel module and no second OS install: Alpine's rootfs lives in the
 * app's private storage and `proot` (a pure ptrace-based emulator of chroot(2) and a handful of
 * syscalls) makes it believe it is PID 1's child on its own filesystem. That is the same
 * technique Termux's `proot-distro` uses, and it is why this works on a stock Samsung phone.
 *
 * Two Android rules shape the implementation:
 *  - Since Android 10 an app may not exec a file in its own data directory, so `proot` itself
 *    ships as `libproot.so` in the APK and is exec'd from `nativeLibraryDir`.
 *  - `proot` is dynamically linked against Termux's `libtalloc`/`libandroid-shmem`, so those
 *    ship beside it and are found through LD_LIBRARY_PATH.
 */
class LinuxContainer(
    private val context: Context,
    /** When set, this container always targets that guest instead of the desktop's choice. */
    private val distroOverride: String? = null
) {

    private val prefs = context.getSharedPreferences("phonix_linux_prefs", Context.MODE_PRIVATE)

    fun activeDistroId(): String =
        distroOverride ?: (prefs.getString("active_distro_id", "alpine") ?: "alpine")

    fun activeDistro(): com.example.model.LinuxDistro =
        com.example.model.DistroCatalog.getDistro(activeDistroId())

    /** Extracted active Linux root filesystem (the guest's `/`). */
    val rootfs: File
        get() = File(context.filesDir, "linux/${activeDistroId()}")

    /** proot unpacks its own loader here at startup, so it must exist and be writable. */
    private val workDir = File(context.filesDir, "linux/tmp")

    private val nativeLibs = File(context.applicationInfo.nativeLibraryDir)

    private val proot = File(nativeLibs, "libproot.so")
    private val prootLoader = File(nativeLibs, "libproot_loader.so")

    val isInstalled: Boolean
        get() = File(rootfs, MARKER).isFile ||
                File(rootfs, "etc/alpine-release").isFile ||
                File(rootfs, "etc/os-release").isFile

    /** Release string read from the active rootfs, e.g. `3.24.3` or `12.7`. */
    fun version(): String = try {
        when {
            File(rootfs, "etc/alpine-release").isFile -> File(rootfs, "etc/alpine-release").readText().trim()
            // Bare versions only: the hub prints "<distro name> <version>", and the distro name
            // already carries its release ("Debian 12 (Bookworm)"), so a PRETTY_NAME-style
            // string here rendered as "Debian 12 (Bookworm) Debian 12.15".
            File(rootfs, "etc/debian_version").isFile -> File(rootfs, "etc/debian_version").readText().trim()
            File(rootfs, "etc/os-release").isFile -> {
                val lines = File(rootfs, "etc/os-release").readLines()
                val versionId = lines.firstOrNull { it.startsWith("VERSION_ID=") }
                    ?.substringAfter("=")?.trim('"', '\'')
                versionId?.takeIf { it.isNotBlank() }
                    ?: lines.firstOrNull { it.startsWith("PRETTY_NAME=") }
                        ?.substringAfter("=")?.trim('"', '\'') ?: activeDistro().version
            }
            else -> activeDistro().version
        }
    } catch (_: Throwable) {
        activeDistro().version
    }

    /**
     * Unpacks the bundled Alpine minirootfs on first run. Idempotent and cheap to call;
     * a marker file means later launches return immediately.
     */
    fun install(onProgress: (String) -> Unit = {}) {
        val target = rootfs
        val marker = File(target, ".phonix-installed")
        if (marker.isFile && isInstalled) return

        onProgress("Unpacking Alpine Linux root filesystem (first run, ~4 MB)...")
        val extracted = extractBundled(target)
        onProgress("Unpacked $extracted files.")
        writeGuestConfig(target)
        marker.writeText("alpine-minirootfs-3.24.3-aarch64\n")
    }

    /**
     * Installs [distro] and makes it the active guest.
     *
     * The previous rootfs is deleted only after the new one has been downloaded, unpacked and
     * checked, so a failed switch (no network, mirror moved, disk full) leaves the working guest
     * exactly as it was. The earlier ordering purged first, so one failed switch could leave the
     * app with no usable userspace at all.
     */
    fun switchDistro(
        distro: com.example.model.LinuxDistro,
        debloatPrevious: Boolean = true,
        onProgress: (String) -> Unit = {},
        onBytes: (Long, Long) -> Unit = { _, _ -> }
    ) {
        val previousId = activeDistroId()
        val target = File(context.filesDir, "linux/${distro.id}")
        val marker = File(target, MARKER)

        if (marker.isFile && previousId == distro.id && isInstalled) {
            onProgress("${distro.name} is already installed and ready.")
            return
        }

        val wasInstalled = marker.isFile
        try {
            if (wasInstalled) {
                onProgress("${distro.name} is already on disk; switching to it.")
            } else if (distro.isBundled) {
                installBundled(distro, target, onProgress)
            } else {
                installStreamed(distro, target, onProgress, onBytes)
            }

            writeGuestConfig(target)
            marker.writeText("${distro.id}-${distro.version}-aarch64\n")
        } catch (e: Throwable) {
            // A rootfs promoted but never marked would sit on disk unaccounted for (Debian's tree
            // is ~525 MB) and the next attempt would download the archive again anyway. Remove it
            // so a failed switch costs only the download.
            if (!wasInstalled) target.deleteRecursively()
            throw e
        }

        prefs.edit().putString("active_distro_id", distro.id).apply()
        // The bundled guest is never pruned: it ships inside the APK, so it is the offline
        // rescue image, and the promise that it survives every switch has to hold in code. The
        // prune used to delete it like any other tree, which left one userspace and no fallback.
        val previous = com.example.model.DistroCatalog.getDistro(previousId)
        if (debloatPrevious && previousId != distro.id && !previous.isBundled) {
            onProgress("Debloating $previousId to recover storage...")
            File(context.filesDir, "linux/$previousId").deleteRecursively()
        }
        onProgress("${distro.name} is ready.")
    }

    /**
     * Unpacks the bundled minirootfs into [target] without destroying the guest already there.
     *
     * The tree is built in a sibling staging directory and promoted only after extraction has
     * finished, so a disk-full, OOM or corrupt asset leaves the existing userspace untouched.
     * The old code deleted `target` first, which could leave the app with no userspace at all
     * and no way back in.
     */
    private fun extractBundled(target: File): Int {
        val staging = File(target.parentFile, ".${target.name}-staging")
        staging.deleteRecursively()
        staging.mkdirs()
        workDir.mkdirs()
        val extracted = try {
            TarGzExtractor(context.assets.open(ASSET_ROOTFS)).extractTo(staging)
        } catch (e: Throwable) {
            staging.deleteRecursively()
            throw e
        }
        target.deleteRecursively()
        if (!staging.renameTo(target)) {
            staging.copyRecursively(target, overwrite = true)
            staging.deleteRecursively()
        }
        return extracted
    }

    private fun installBundled(
        distro: com.example.model.LinuxDistro,
        target: File,
        onProgress: (String) -> Unit
    ) {
        onProgress("Extracting bundled ${distro.name} rootfs...")
        val extracted = extractBundled(target)
        onProgress("Extracted $extracted files for ${distro.name}.")
    }

    /**
     * Downloads the official rootfs and unpacks it inside the running guest.
     *
     * The download runs in-process for real progress and redirect handling; the unpacking runs in
     * the guest because busybox already provides `unxz` and `tar`, whereas Android has no xz
     * decoder and this build takes no extra dependency for one. Staging lives under the current
     * rootfs's `/root` (the only place proot and the app both see) and is moved out to
     * `files/linux/<id>` once it validates.
     */
    private fun installStreamed(
        distro: com.example.model.LinuxDistro,
        target: File,
        onProgress: (String) -> Unit,
        onBytes: (Long, Long) -> Unit
    ) {
        check(isInstalled) { "Install the bundled guest first: it provides unxz and tar" }
        workDir.mkdirs()

        // Settled before the download, not after it. The unpacking guest used to be assumed to
        // have `unxz`; Debian 12's minimal image does not (nor does Alpine's busybox), so every
        // streamed switch downloaded ~100 MB, verified its SHA-256 and then died with
        // "unxz: not found" followed by "tar: This does not look like a tar archive".
        ensureGuestUnpacker(distro, onProgress)

        val installer = DistroInstaller()
        val ref = installer.resolve(distro)
        onProgress("Resolved ${distro.name} build ${ref.buildDate}.")

        val cache = File(rootfs, "root/.phonix-cache").apply { mkdirs() }
        val staging = File(rootfs, "root/.phonix-stage").apply {
            deleteRecursively()
            mkdirs()
        }
        val archive = File(cache, "rootfs.tar.xz")

        try {
            installer.download(ref, archive, onProgress, onBytes)
            onProgress("Unpacking ${distro.name} in the guest (this takes a few minutes)...")

            val status = execOnce(
                "unxz -c /root/.phonix-cache/rootfs.tar.xz | tar -xf - -C /root/.phonix-stage",
                timeoutSeconds = 1_800
            ) { line -> if (line.isNotBlank()) onProgress(line) }
            if (status != 0) {
                throw IllegalStateException("tar exited with status $status while unpacking ${distro.name}")
            }

            verifyRootfs(distro, staging)
            onProgress("Verified ${distro.name} rootfs.")

            target.deleteRecursively()
            target.parentFile?.mkdirs()
            if (!staging.renameTo(target)) {
                staging.copyRecursively(target, overwrite = true)
                staging.deleteRecursively()
            }
        } finally {
            // Releases up to a few hundred MB whether the install worked or not.
            staging.deleteRecursively()
            cache.deleteRecursively()
        }
    }

    /**
     * Makes sure the running guest can unpack a `.tar.xz` at all: `unxz` and `tar`.
     *
     * Checks the cheap probe first, so a guest that already has the tools - the normal case after
     * one streamed install - pays nothing. Otherwise the tools come from the guest's own package
     * manager, which is a few hundred KB instead of a wasted rootfs download.
     */
    private fun ensureGuestUnpacker(
        distro: com.example.model.LinuxDistro,
        onProgress: (String) -> Unit
    ) {
        if (hasUnpacker()) return

        val command = distro.installCommand(distro.unpackToolPackages)
        onProgress("${distro.name} has no xz decoder; installing ${distro.unpackToolPackages}...")
        val lastLines = ArrayDeque<String>()
        execOnce(command, timeoutSeconds = 900) { line ->
            if (line.isNotBlank()) {
                if (lastLines.size == 8) lastLines.removeFirst()
                lastLines.addLast(line.trim())
                onProgress(line)
            }
        }
        if (!hasUnpacker()) {
            val tail = lastLines.lastOrNull()?.let { " (last output: $it)" }.orEmpty()
            throw IllegalStateException(
                "${distro.name} cannot unpack the xz archive: `$command` did not provide " +
                    "unxz$tail"
            )
        }
        onProgress("xz decoder ready in ${distro.name}.")
    }

    /**
     * True when the running guest has both halves of the unpack pipeline.
     *
     * The marker echo is the signal, not the status: `execOnce` returns the exit status of the
     * guest's `sh`, and `command -v` failing inside an `&&` chain is indistinguishable from the
     * shell itself failing.
     */
    private fun hasUnpacker(): Boolean {
        val probe = StringBuilder()
        val status = execOnce(
            "command -v unxz >/dev/null 2>&1 && command -v tar >/dev/null 2>&1 && echo PHONIX_UNPACKER_OK",
            timeoutSeconds = 60
        ) { line -> probe.appendLine(line) }
        return status == 0 && probe.contains("PHONIX_UNPACKER_OK")
    }

    /** A rootfs is promoted only if it really contains a shell and its own release file. */
    private fun verifyRootfs(distro: com.example.model.LinuxDistro, root: File) {
        val shell = File(root, "bin/sh").exists() || File(root, "usr/bin/env").exists()
        val release = File(root, "etc/os-release").exists() ||
            File(root, "etc/debian_version").exists() ||
            File(root, "etc/alpine-release").exists()
        if (!shell || !release) {
            throw IllegalStateException(
                "${distro.name} unpacked without a shell or /etc/os-release, so it was not promoted"
            )
        }
    }

    /** resolv.conf and hosts are host files in Android, so the guest gets its own copies. */
    private fun writeGuestConfig(target: File = rootfs) {
        val dns = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val props = cm.getLinkProperties(cm.activeNetwork)
            props?.dnsServers?.mapNotNull { it.hostAddress }?.take(4).orEmpty()
        } catch (_: Throwable) {
            emptyList()
        }.ifEmpty { listOf("1.1.1.1", "8.8.8.8") }

        writeHostFile(
            target,
            "etc/resolv.conf",
            dns.joinToString("\n") { "nameserver $it" } + "\n"
        )
        writeHostFile(
            target,
            "etc/hosts",
            """
            |127.0.0.1   localhost localhost.localdomain
            |::1         localhost ip6-localhost ip6-loopback
            |""".trimMargin()
        )
    }

    /**
     * Writes one of the files Android normally owns, replacing any image-provided link.
     *
     * Debian and Ubuntu ship `/etc/resolv.conf` as a symlink to
     * `/run/systemd/resolve/stub-resolv.conf`. Outside the image's own systemd boot that target
     * does not exist, so writing through it fails with ENOENT — which used to abort the switch
     * after the rootfs had already been promoted. Unlink first, then write a real file.
     */
    private fun writeHostFile(target: File, relative: String, content: String) {
        val file = File(target, relative)
        file.parentFile?.mkdirs()
        // A dangling symlink reports isFile() == false, so this removes the link itself.
        if (!file.isFile) file.delete()
        file.writeText(content)
    }

    /**
     * Boots the container and returns a live shell. Output arrives on a background thread;
     * [onLine] is called once per line.
     */
    fun startShell(onLine: (String) -> Unit, onExit: (Int) -> Unit): LinuxShellSession {
        check(isInstalled) { "Linux rootfs not installed" }
        workDir.mkdirs()

        val process = prootBuilder(guestCommand(listOf("/bin/sh"))).start()
        val session = LinuxShellSession(process, onLine, onExit)
        session.pump()
        return session
    }

    /**
     * Runs one command in the active guest and waits for it to finish.
     *
     * Unlike [startShell] this is for work that must complete before the next step (unpacking a
     * downloaded rootfs), so it blocks until the guest's `sh` exits and hands back its status.
     * It polls `exitValue()` rather than `waitFor(timeout)`, which only exists from API 26 while
     * this app supports 24.
     */
    fun execOnce(
        command: String,
        timeoutSeconds: Long = 900,
        onLine: (String) -> Unit = {}
    ): Int {
        check(isInstalled) { "No guest rootfs is installed to run a command in" }
        workDir.mkdirs()

        val process = prootBuilder(guestCommand(listOf("/bin/sh", "-c", command))).start()
        val reader = Thread {
            try {
                process.inputStream.bufferedReader().forEachLine { line -> onLine(line) }
            } catch (_: Throwable) {
            }
        }.apply {
            isDaemon = true
            start()
        }

        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        while (true) {
            val status = try {
                process.exitValue()
            } catch (_: IllegalThreadStateException) {
                null
            }
            if (status != null) {
                reader.join(2_000)
                return status
            }
            if (System.currentTimeMillis() > deadline) {
                process.destroyForcibly()
                throw IllegalStateException("Guest command timed out after ${timeoutSeconds}s")
            }
            Thread.sleep(150)
        }
    }

    /** argv that runs [command] inside [guestRoot] under proot, with the guest's own PATH. */
    private fun guestCommand(command: List<String>, guestRoot: File = rootfs): List<String> =
        mutableListOf(
            proot.absolutePath,
            // Termux's proot uses a loader injected into the traced process; without it,
            // exec of guest binaries fails on Android.
            "--link2symlink",
            "-0",                         // pretend to be root inside the guest
            "-r", guestRoot.absolutePath, // the guest's /
            // Android has no /dev nodes the guest may use, so borrow the host's.
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "/dev/urandom:/dev/random",
            "-w", "/root",
            "/usr/bin/env", "-i",
            "HOME=/root",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm-256color",
            "LANG=C.UTF-8",
            "SHELL=/bin/sh",
            "PHONIX=1"
        ).apply { addAll(command) }

    /** ProcessBuilder wired with the env proot needs to find its loader and libraries. */
    private fun prootBuilder(argv: List<String>): ProcessBuilder =
        ProcessBuilder(argv)
            .directory(rootfs)
            .redirectErrorStream(true)
            .apply {
                environment().apply {
                    put("PROOT_TMP_DIR", workDir.absolutePath)
                    put("PROOT_LOADER", prootLoader.absolutePath)
                    // Lets the dynamic linker find libtalloc.so.2 / libandroid-shmem.so next to proot.
                    put("LD_LIBRARY_PATH", nativeLibs.absolutePath)
                }
            }

    companion object {
        /** Marks a rootfs that finished installing, so a half-unpacked tree is never treated as ready. */
        const val MARKER = ".phonix-installed"

        // .tgz, not .tar.gz: AAPT2 treats a ".gz" asset as compressed content and strips
        // the suffix on packaging, which would leave a name we never look for.
        const val ASSET_ROOTFS = "alpine-rootfs.tgz"
    }
}

/** A running guest shell: line-oriented stdin/stdout over the proot process. */
class LinuxShellSession(
    private val process: Process,
    private val onLine: (String) -> Unit,
    private val onExit: (Int) -> Unit
) {
    private val stdin = process.outputStream

    @Volatile
    private var closed = false

    /** Sends one command line to the guest shell. */
    fun send(command: String) {
        if (closed) throw IllegalStateException("shell has exited")
        stdin.write((command + "\n").toByteArray(Charsets.UTF_8))
        stdin.flush()
    }

    /** Starts the reader thread that forwards guest output to [onLine]. */
    fun pump() {
        Thread({
            val buffer = StringBuilder()
            fun flush() {
                if (buffer.isNotEmpty()) {
                    onLine(stripAnsi(buffer.toString()))
                    buffer.setLength(0)
                }
            }
            try {
                val input = process.inputStream
                val chunk = ByteArray(4096)
                while (true) {
                    val read = input.read(chunk)
                    if (read < 0) break
                    val text = String(chunk, 0, read, Charsets.UTF_8)
                    for (ch in text) {
                        when (ch) {
                            '\n' -> flush()
                            '\r' -> Unit
                            '\u0000' -> Unit
                            else -> buffer.append(ch)
                        }
                    }
                    // Programs that print without a trailing newline (prompts, progress bars)
                    // should not be swallowed waiting for one.
                    if (input.available() == 0) {
                        Thread.sleep(60)
                        if (input.available() == 0) flush()
                    }
                }
            } catch (_: Throwable) {
            } finally {
                flush()
            }
            val code = try {
                process.waitFor()
            } catch (_: Throwable) {
                -1
            }
            closed = true
            onExit(code)
        }, "phonix-linux-output").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Asks the guest shell to exit, then makes sure the proot tracer is gone before returning.
     *
     * `Process.destroy()` only sends SIGTERM and returns without waiting, so it is a request, not
     * a guarantee: a guest that ignores it, or is still busy, outlives the "stopped" runtime and
     * the next start adds a second shell. The short wait first lets `exit` shut down cleanly, then
     * the forced kill makes it certain.
     */
    fun close() {
        closed = true
        try {
            stdin.write("exit\n".toByteArray())
            stdin.flush()
        } catch (_: Throwable) {
        }
        val exited = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(2, TimeUnit.SECONDS)
            } else {
                // Older devices have no timed wait; destroy() is all we have.
                process.destroy()
                true
            }
        } catch (_: Throwable) {
            false
        }
        if (!exited) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    process.destroyForcibly()
                    process.waitFor(2, TimeUnit.SECONDS)
                } else {
                    process.destroy()
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun stripAnsi(text: String): String =
        text.replace(Regex("\u001B\\[[0-9;?]*[ -/]*[@-~]"), "").replace(Regex("\u001B[@-_]"), "")
}

/**
 * Minimal ustar reader. Android has no `tar`, and shelling out to one would defeat the point
 * of a self-contained installer, so the rootfs is unpacked here.
 */
class TarGzExtractor(private val stream: InputStream) {

    fun extractTo(destination: File): Int {
        var files = 0
        GZIPInputStream(stream, 1 shl 16).buffered(1 shl 16).use { gz ->
            val header = ByteArray(512)
            var pendingLongName: String? = null
            var pendingLongLink: String? = null

            while (true) {
                if (!readFully(gz, header)) break
                if (header.all { it == 0.toByte() }) break

                val name = pendingLongName ?: readString(header, 0, 100)
                pendingLongName = null
                val mode = readOctal(header, 100, 8)
                val size = readOctal(header, 124, 12).toLong()
                val type = header[156].toInt().toChar()
                var linkName = pendingLongLink ?: readString(header, 157, 100)
                pendingLongLink = null
                val prefix = readString(header, 345, 155)
                val fullName = if (prefix.isNotEmpty()) "$prefix/$name" else name

                when (type) {
                    'x', 'g' -> {
                        // pax extended header: read and ignore the payload, the path we get
                        // from the normal header is enough for minirootfs.
                        skip(gz, alignedSize(size))
                        continue
                    }
                    'L' -> {
                        val data = gz.readNBytes(size.toInt())
                        pendingLongName = data.toString(Charsets.UTF_8).trimEnd('\u0000', '\n')
                        skip(gz, alignedSize(size) - size)
                        continue
                    }
                    'K' -> {
                        val data = gz.readNBytes(size.toInt())
                        pendingLongLink = data.toString(Charsets.UTF_8).trimEnd('\u0000', '\n')
                        skip(gz, alignedSize(size) - size)
                        continue
                    }
                }

                val target = safeResolve(destination, fullName)
                if (target == null) {
                    skip(gz, alignedSize(size))
                    continue
                }

                when (type) {
                    '5' -> target.mkdirs()
                    '2' -> {
                        target.parentFile?.mkdirs()
                        if (target.exists() || isSymlink(target)) target.delete()
                        try {
                            Os.symlink(linkName, target.absolutePath)
                        } catch (_: ErrnoException) {
                        }
                    }
                    '1' -> {
                        target.parentFile?.mkdirs()
                        val source = safeResolve(destination, linkName)
                        if (source != null && source.isFile) {
                            try {
                                Os.link(source.absolutePath, target.absolutePath)
                            } catch (_: ErrnoException) {
                                source.copyTo(target, overwrite = true)
                            }
                        }
                        skip(gz, alignedSize(size))
                    }
                    '0', '\u0000', '7' -> {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { out ->
                            copyN(gz, out, size)
                        }
                        applyMode(target, mode)
                        files++
                        skip(gz, alignedSize(size) - size)
                    }
                    else -> skip(gz, alignedSize(size))
                }
            }
        }
        return files
    }

    private fun applyMode(file: File, mode: Int) {
        file.setReadable(true, false)
        file.setWritable(true, true)
        file.setExecutable(mode and 0b001001001 != 0, false)
    }

    /** Rejects absolute paths and `..` traversal so a hostile archive cannot escape the rootfs. */
    private fun safeResolve(root: File, name: String): File? {
        val clean = name.trimStart('/')
        if (clean.isEmpty()) return null
        val candidate = File(root, clean).canonicalFile
        val rootPath = root.canonicalPath
        return if (candidate.path == rootPath || candidate.path.startsWith(rootPath + File.separator)) {
            candidate
        } else {
            null
        }
    }

    private fun isSymlink(file: File): Boolean = try {
        Os.lstat(file.absolutePath).st_mode and 0xF000 == 0xA000
    } catch (_: ErrnoException) {
        false
    }

    private fun alignedSize(size: Long): Long = (size + 511) / 512 * 512

    private fun readFully(input: InputStream, buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read < 0) return false
            offset += read
        }
        return true
    }

    private fun readString(header: ByteArray, offset: Int, length: Int): String {
        var end = offset
        val limit = offset + length
        while (end < limit && header[end] != 0.toByte()) end++
        return String(header, offset, end - offset, Charsets.UTF_8)
    }

    private fun readOctal(header: ByteArray, offset: Int, length: Int): Int {
        val text = readString(header, offset, length).trim()
        return if (text.isEmpty()) 0 else text.toIntOrNull(8) ?: 0
    }

    private fun copyN(input: InputStream, out: FileOutputStream, size: Long) {
        var remaining = size
        val buffer = ByteArray(1 shl 16)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) break
            out.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun skip(input: InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                if (input.read() < 0) return
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
    }
}
