package com.example.engine

import com.example.model.DistroImageRef
import com.example.model.DistroIndex
import com.example.model.LinuxDistro
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Downloads a distribution's official rootfs archive.
 *
 * The archive streams straight to disk — it is never held in memory — and is written to a
 * `<name>.part` file that is only renamed once the byte count matches the `Content-Length` the
 * server announced. A dropped connection therefore cannot leave a truncated tarball that later
 * looks like a finished install.
 *
 * Nothing here decompresses. Android ships no xz decoder and this project takes no extra
 * dependency for one, so the installed guest (`unxz` + `tar` in busybox) does the unpacking; see
 * [LinuxContainer.switchDistro].
 */
class DistroInstaller(private val userAgent: String = "PhonixOS/1.2 (Android; proot)") {

    /** Mirrors no longer accept downloads without a UA, and some 403 the default one. */
    private fun open(url: String, timeoutMs: Int): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", userAgent)
        }

    /** Asks the mirror which dated build is current for [distro]. */
    fun resolve(distro: LinuxDistro): DistroImageRef {
        require(distro.imageName.isNotEmpty() && distro.imageRelease.isNotEmpty()) {
            "${distro.name} has no streaming image configured"
        }
        val index = try {
            fetchText(DistroIndex.INDEX_URL)
        } catch (e: Throwable) {
            // Surface the cause. Swallowing it once turned a NetworkOnMainThreadException into a
            // generic "could not read the index" and hid the real bug for a whole debug round.
            throw IllegalStateException(
                "Could not read the image index at ${DistroIndex.INDEX_URL}: " +
                    (e.message ?: e.javaClass.simpleName),
                e
            )
        } ?: throw IllegalStateException("The mirror returned an empty image index at ${DistroIndex.INDEX_URL}")
        return DistroIndex.resolve(index, distro.imageName, distro.imageRelease)
            ?: throw IllegalStateException("The mirror lists no arm64 build for ${distro.name}")
    }

    /**
     * Streams [ref] into [destination]. [onProgress] receives human-readable milestones and
     * [onBytes] receives (written, total-or-zero) roughly once per megabyte so the UI can show a
     * real percentage instead of a fake spinner.
     */
    fun download(
        ref: DistroImageRef,
        destination: File,
        onProgress: (String) -> Unit = {},
        onBytes: (Long, Long) -> Unit = { _, _ -> }
    ): File {
        destination.parentFile?.mkdirs()
        // The partial file survives a failed attempt so the next run resumes instead of
        // restarting a 95 MB download from zero on a phone connection.
        val part = File(destination.parentFile, destination.name + ".part")
        val already = if (part.exists()) part.length() else 0L

        val conn = open(ref.url, timeoutMs = 60_000)
        if (already > 0) conn.setRequestProperty("Range", "bytes=$already-")
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code while fetching ${ref.url}")

            // Only 206 means the server honoured the range. On a plain 200 it is resending the
            // whole archive, so the partial bytes are dropped rather than appended twice.
            val resuming = code == 206 && already > 0
            if (!resuming && part.exists()) part.delete()
            val sent = conn.contentLengthLong.takeIf { it > 0 } ?: 0L
            val total = if (resuming && sent > 0) already + sent else sent
            val label = ref.url.substringAfterLast('/')
            onProgress(
                if (total > 0) "Downloading $label (${megabytes(total)})..."
                else "Downloading $label..."
            )

            var written = if (resuming) already else 0L
            var reportedAt = written
            conn.inputStream.use { input ->
                FileOutputStream(part, resuming).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        written += read
                        if (written - reportedAt >= (1 shl 20)) {
                            reportedAt = written
                            onBytes(written, total)
                        }
                    }
                }
            }
            onBytes(written, total)

            if (written < MIN_ARCHIVE_BYTES) {
                throw IllegalStateException("Download ended after $written bytes — not a rootfs archive")
            }
            if (total > 0 && written != total) {
                throw IllegalStateException("Download truncated at $written of $total bytes")
            }

            // Verify against the mirror's own manifest before the archive is allowed to replace
            // the running guest. No manifest entry means no install.
            val sumsUrl = ref.url.substringBeforeLast('/') + "/SHA256SUMS"
            val manifest = fetchText(sumsUrl)
                ?: throw IllegalStateException("The mirror published no SHA256SUMS beside ${ref.url}")
            val expected = DistroIndex.checksumFor(manifest)
                ?: throw IllegalStateException(
                    "SHA256SUMS has no ${DistroIndex.ROOTFS_FILE} entry; refusing an unverified install"
                )
            val actual = DistroIndex.sha256Hex(part)
            if (!actual.equals(expected, ignoreCase = true)) {
                throw IllegalStateException(
                    "SHA-256 mismatch for ${ref.url}: expected ${expected.take(12)}…, got ${actual.take(12)}…"
                )
            }
            onProgress("Verified the official SHA-256.")

            destination.delete()
            if (!part.renameTo(destination)) {
                part.copyTo(destination, overwrite = true)
                part.delete()
            }
            onProgress("Downloaded ${megabytes(written)}.")
            return destination
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    private fun fetchText(url: String): String? {
        val conn = open(url, timeoutMs = 30_000)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    private fun megabytes(bytes: Long): String =
        String.format(Locale.ROOT, "%.0f MB", bytes / (1024.0 * 1024.0))

    companion object {
        /** Smallest plausible compressed rootfs; catches an HTML error page saved as `.tar.xz`. */
        const val MIN_ARCHIVE_BYTES = 1L shl 20
    }
}
