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
class LinuxContainer(private val context: Context) {

    private val prefs = context.getSharedPreferences("phonix_linux_prefs", Context.MODE_PRIVATE)

    fun activeDistroId(): String = prefs.getString("active_distro_id", "alpine") ?: "alpine"

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
        get() = File(rootfs, ".phonix-installed").isFile ||
                File(rootfs, "etc/alpine-release").isFile ||
                File(rootfs, "etc/os-release").isFile

    /** Release string read from the active rootfs, e.g. `3.24.3` or `12.7`. */
    fun version(): String = try {
        when {
            File(rootfs, "etc/alpine-release").isFile -> File(rootfs, "etc/alpine-release").readText().trim()
            File(rootfs, "etc/debian_version").isFile -> "Debian " + File(rootfs, "etc/debian_version").readText().trim()
            File(rootfs, "etc/os-release").isFile -> {
                val lines = File(rootfs, "etc/os-release").readLines()
                lines.firstOrNull { it.startsWith("PRETTY_NAME=") }
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

        // A previous run may have died mid-extraction; start from a clean tree.
        if (target.exists()) target.deleteRecursively()
        target.mkdirs()
        workDir.mkdirs()

        onProgress("Unpacking Alpine Linux root filesystem (first run, ~4 MB)...")
        val extracted = TarExtractor(context.assets.open(ASSET_ROOTFS)).extractTo(target)
        onProgress("Unpacked $extracted files.")
        writeGuestConfig(target)
        marker.writeText("alpine-minirootfs-3.24.3-aarch64\n")
    }

    /**
     * Installs (if needed) and activates a distribution.
     *
     * A distribution is only activated after its rootfs is complete on disk: the previous guest
     * stays exactly as it was if the download fails, the checksum does not match or the disk is
     * full. Bundled Alpine is always kept as the offline rescue image and is never pruned.
     *
     * Throws [DistroInstallException] with a user-readable message on failure.
     */
    suspend fun switchDistro(
        distro: com.example.model.LinuxDistro,
        onProgress: (String) -> Unit = {}
    ): File {
        val base = File(context.filesDir, "linux")
        val target = File(base, distro.id)
        val marker = File(target, ".phonix-installed")

        if (marker.isFile) {
            prefs.edit().putString("active_distro_id", distro.id).apply()
            onProgress("${distro.name} is already installed; switching to it.")
            return target
        }

        if (distro.isBundled) {
            onProgress("Extracting bundled ${distro.name} rootfs…")
            target.deleteRecursively()
            target.mkdirs()
            workDir.mkdirs()
            val extracted = TarExtractor(context.assets.open(ASSET_ROOTFS)).extractTo(target)
            if (extracted < 100) throw DistroInstallException("The bundled rootfs unpacked only $extracted files.")
            writeGuestConfig(target)
            marker.writeText("${distro.id}-${distro.version}-aarch64\n")
            onProgress("Extracted $extracted files for ${distro.name}.")
        } else {
            target.deleteRecursively()
            workDir.mkdirs()
            DistroInstaller(context).install(distro) { progress ->
                val percent = if (progress.total > 0L) " ${(progress.fraction * 100).toInt()}%" else ""
                onProgress(progress.message + percent)
            }
            if (!marker.isFile) {
                throw DistroInstallException("${distro.name} did not finish installing; nothing was changed.")
            }
        }

        // Activation happens last, so a failure above leaves the previous distro running.
        prefs.edit().putString("active_distro_id", distro.id).apply()
        pruneOtherDistros(keep = distro.id, onProgress = onProgress)
        onProgress("${distro.name} is active.")
        return target
    }

    /**
     * Removes rootfs trees for distros that are neither active nor the bundled rescue image.
     * Called only after a successful switch; never deletes the image the user is running.
     */
    fun pruneOtherDistros(keep: String, onProgress: (String) -> Unit = {}) {
        val base = File(context.filesDir, "linux")
        val keepIds = setOf(keep, "alpine")
        // Only real distro trees are candidates. `linux/` also holds the proot work directory and
        // any staging tree, and those must never be swept up by a "free some storage" pass.
        val known = com.example.model.DistroCatalog.distros.map { it.id }.toSet()
        base.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name in known && dir.name !in keepIds) {
                onProgress("Removing the previous ${dir.name} rootfs to free storage…")
                dir.deleteRecursively()
            }
        }
        File(base, "$keep.partial").deleteRecursively()
    }

    /** resolv.conf and hosts are host files in Android, so the guest gets its own copies. */
    private fun writeGuestConfig(target: File = rootfs) = writeGuestNetworkConfig(context, target)

    /**
     * Boots the container and returns a live shell. Output arrives on a background thread;
     * [onLine] is called once per line.
     */
    fun startShell(onLine: (String) -> Unit, onExit: (Int) -> Unit): LinuxShellSession {
        check(isInstalled) { "Linux rootfs not installed" }
        workDir.mkdirs()

        val argv = mutableListOf(
            proot.absolutePath,
            // Termux's proot uses a loader injected into the traced process; without it,
            // exec of guest binaries fails on Android.
            "--link2symlink",
            "-0",                       // pretend to be root inside the guest
            "-r", rootfs.absolutePath,  // the guest's /
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
            "PHONIX=1",
            "/bin/sh"
        )

        val builder = ProcessBuilder(argv)
            .directory(rootfs)
            .redirectErrorStream(true)
        builder.environment().apply {
            put("PROOT_TMP_DIR", workDir.absolutePath)
            put("PROOT_LOADER", prootLoader.absolutePath)
            // Lets the dynamic linker find libtalloc.so.2 / libandroid-shmem.so next to proot.
            put("LD_LIBRARY_PATH", nativeLibs.absolutePath)
        }

        val process = builder.start()
        val session = LinuxShellSession(process, onLine, onExit)
        session.pump()
        return session
    }

    companion object {
        // .tgz, not .tar.gz: AAPT2 treats a ".gz" asset as compressed content and strips
        // the suffix on packaging, which would leave a name we never look for.
        const val ASSET_ROOTFS = "alpine-rootfs.tgz"
    }
}

/**
 * Writes the guest's own `resolv.conf` and `hosts`, seeded from the phone's current network.
 * Shared by the bundled installer and the streaming installer in [DistroInstaller].
 */
internal fun writeGuestNetworkConfig(context: Context, target: File) {
    val dns = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val props = cm.getLinkProperties(cm.activeNetwork)
        props?.dnsServers?.mapNotNull { it.hostAddress }?.take(4).orEmpty()
    } catch (_: Throwable) {
        emptyList()
    }.ifEmpty { listOf("1.1.1.1", "8.8.8.8") }

    File(target, "etc").mkdirs()
    File(target, "etc/resolv.conf").writeText(
        dns.joinToString("\n") { "nameserver $it" } + "\n"
    )
    File(target, "etc/hosts").writeText(
        """
        |127.0.0.1   localhost localhost.localdomain
        |::1         localhost ip6-localhost ip6-loopback
        |""".trimMargin()
    )
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
class TarExtractor(private val stream: InputStream, private val gzip: Boolean = true) {

    fun extractTo(destination: File): Int {
        var files = 0
        val raw: InputStream = if (gzip) GZIPInputStream(stream, 1 shl 16) else stream
        raw.buffered(1 shl 16).use { gz ->
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
