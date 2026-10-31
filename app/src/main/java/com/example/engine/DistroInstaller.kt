package com.example.engine

import android.content.Context
import android.os.StatFs
import com.example.model.LinuxDistro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

/** A distribution could not be installed. [message] is shown to the user verbatim. */
class DistroInstallException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Installs a distribution that is not bundled in the APK.
 *
 * The guest rootfs is fetched from the Linux Containers image server, which publishes a dated
 * directory per build containing `rootfs.tar.xz` plus a `SHA256SUMS` manifest. The install is
 * only accepted when the downloaded archive matches the published SHA-256, and the new rootfs is
 * staged next to the active one so a failed download, a corrupt archive or a full disk leaves the
 * guest the user is already running untouched.
 *
 * Nothing here invents a rootfs. A distribution either arrives as a verified official image or
 * the switch fails with a message the UI can show.
 */
class DistroInstaller(
    private val context: Context,
    private val client: OkHttpClient = defaultClient()
) {

    data class Progress(val message: String, val bytes: Long = 0L, val total: Long = 0L) {
        val fraction: Float
            get() = if (total > 0L) {
                (bytes.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
    }

    data class Image(val date: String, val url: String, val sha256: String, val sizeBytes: Long)

    suspend fun install(distro: LinuxDistro, onProgress: (Progress) -> Unit = {}): File =
        withContext(Dispatchers.IO) {
            if (distro.isBundled) {
                throw DistroInstallException("${distro.name} ships inside the APK; use the bundled installer.")
            }
            if (distro.imageIndexUrl.isBlank()) {
                throw DistroInstallException("No image source is configured for ${distro.name}.")
            }

            val base = File(context.filesDir, "linux")
            val staging = File(base, "${distro.id}.partial")
            val target = File(base, distro.id)
            val archive = File(context.cacheDir, "${distro.id}-rootfs.tar.xz")

            try {
                onProgress(Progress("Resolving the newest ${distro.name} arm64 image…"))
                val image = resolveLatestImage(distro)
                requireFreeSpace(image, distro)

                onProgress(
                    Progress(
                        "Downloading ${distro.name} (${megabytes(image.sizeBytes)})…",
                        0L,
                        image.sizeBytes
                    )
                )
                download(image.url, archive) { read, total ->
                    onProgress(Progress("Downloading ${distro.name}…", read, total))
                }

                onProgress(Progress("Verifying the official SHA-256 manifest…"))
                val actual = DistroIndex.sha256Hex(archive)
                if (!actual.equals(image.sha256, ignoreCase = true)) {
                    throw DistroInstallException(
                        "Refusing to install ${distro.name}: SHA-256 mismatch " +
                            "(expected ${image.sha256.take(12)}…, got ${actual.take(12)}…)."
                    )
                }

                onProgress(Progress("Extracting ${distro.name} (about a minute on a phone)…"))
                if (staging.exists()) staging.deleteRecursively()
                staging.mkdirs()
                val files = extract(archive, staging)
                if (files < 100) {
                    throw DistroInstallException(
                        "The ${distro.name} archive unpacked only $files files; refusing to switch."
                    )
                }
                writeGuestNetworkConfig(context, staging)
                File(staging, ".phonix-installed")
                    .writeText("${distro.id} ${image.date} ${image.sha256.lowercase(Locale.ROOT)}\n")

                // The active rootfs is only replaced once the replacement is complete on disk.
                if (target.exists()) target.deleteRecursively()
                if (!staging.renameTo(target)) {
                    throw DistroInstallException("Could not move the staged ${distro.name} rootfs into place.")
                }
                onProgress(Progress("${distro.name} installed ($files files)."))
                target
            } catch (e: DistroInstallException) {
                staging.deleteRecursively()
                throw e
            } catch (e: Throwable) {
                staging.deleteRecursively()
                throw DistroInstallException(
                    "${distro.name} install failed: ${e.message ?: e.javaClass.simpleName}",
                    e
                )
            } finally {
                archive.delete()
            }
        }

    /** Resolves the newest published build and its checksum. Does no writing. */
    fun resolveLatestImage(distro: LinuxDistro): Image {
        val index = httpGet(distro.imageIndexUrl)
        val date = DistroIndex.latestDate(index)
            ?: throw DistroInstallException("No arm64 build was found for ${distro.name}.")
        val dirUrl = DistroIndex.imageDirUrl(distro.imageIndexUrl, date)
        val contents = httpGet(dirUrl)
        if (!contents.contains("rootfs.tar.xz")) {
            throw DistroInstallException("The ${distro.name} build $date has no rootfs.tar.xz.")
        }
        val sums = httpGet(dirUrl + "SHA256SUMS")
        val sha = DistroIndex.checksumFor(sums, "rootfs.tar.xz")
            ?: throw DistroInstallException(
                "The ${distro.name} build $date publishes no checksum; refusing an unverified install."
            )
        val url = dirUrl + "rootfs.tar.xz"
        return Image(date, url, sha, contentLength(url))
    }

    private fun requireFreeSpace(image: Image, distro: LinuxDistro) {
        val needed = if (image.sizeBytes > 0L) {
            image.sizeBytes * 2L + 128L * 1024L * 1024L
        } else {
            (distro.installedSizeMb.toLong() + 256L) * 1024L * 1024L
        }
        val available = try {
            StatFs(context.filesDir.absolutePath).availableBytes
        } catch (_: Throwable) {
            Long.MAX_VALUE
        }
        if (available < needed) {
            throw DistroInstallException(
                "${distro.name} needs about ${megabytes(needed)} free; this phone has ${megabytes(available)}."
            )
        }
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw DistroInstallException("GET $url returned HTTP ${response.code}.")
            }
            return response.body?.string() ?: ""
        }
    }

    private fun contentLength(url: String): Long {
        val head = try {
            val request = Request.Builder().url(url).head().header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { it.header("Content-Length")?.toLongOrNull() ?: -1L }
        } catch (_: Throwable) {
            -1L
        }
        if (head > 0L) return head
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Range", "bytes=0-0")
                .build()
            client.newCall(request).execute().use { response ->
                val range = response.header("Content-Range")
                range?.substringAfterLast('/')?.trim()?.toLongOrNull() ?: 0L
            }
        } catch (_: Throwable) {
            0L
        }
    }

    private fun download(url: String, destination: File, onProgress: (Long, Long) -> Unit) {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw DistroInstallException("Downloading $url returned HTTP ${response.code}.")
            }
            val body = response.body ?: throw DistroInstallException("The image server sent an empty body.")
            val total = body.contentLength().takeIf { it > 0L } ?: 0L
            var read = 0L
            var lastReported = 0L
            body.byteStream().use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        read += count
                        // Report about every 1% so the UI does not recompose on every chunk.
                        if (total > 0L && (read - lastReported) > total / 100L) {
                            lastReported = read
                            onProgress(read, total)
                        }
                    }
                    output.fd.sync()
                }
            }
            onProgress(read, if (total > 0L) total else read)
            if (read == 0L) throw DistroInstallException("The downloaded ${destination.name} was empty.")
        }
    }

    private fun extract(archive: File, destination: File): Int =
        FileInputStream(archive).use { file ->
            XZInputStream(file.buffered(1 shl 16)).use { xz ->
                TarExtractor(xz, gzip = false).extractTo(destination)
            }
        }

    private fun megabytes(bytes: Long): String = when {
        bytes <= 0L -> "unknown size"
        bytes < 1024L * 1024L * 1024L -> String.format(Locale.ROOT, "%.0f MB", bytes / (1024.0 * 1024.0))
        else -> String.format(Locale.ROOT, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }

    companion object {
        internal const val USER_AGENT = "Phonix-OS/1.3 (distro installer)"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.MINUTES)
            .build()
    }
}

/**
 * Pure parsing for the image listing. Kept separate from the network so it can be unit tested:
 * the listing is plain HTML with hrefs, and the build directories are named `YYYYMMDD_HH:MM`
 * (the colon arrives percent-encoded).
 */
internal object DistroIndex {

    private val HREF = Regex("href=\"([^\"]+)\"")
    private val DATED = Regex("^\\d{8}_\\d{2}:\\d{2}$")
    private val SHA256 = Regex("^[0-9a-fA-F]{64}$")

    fun hrefs(listing: String): List<String> =
        HREF.findAll(listing).map { decode(it.groupValues[1]) }.toList()

    fun decode(raw: String): String = try {
        java.net.URLDecoder.decode(raw, "UTF-8")
    } catch (_: Throwable) {
        raw
    }

    fun latestDate(listing: String): String? =
        hrefs(listing)
            .map { it.trimEnd('/') }
            .filter { DATED.matches(it) }
            .maxOrNull()

    fun imageDirUrl(indexUrl: String, date: String): String =
        indexUrl.trimEnd('/') + "/" + date.replace(":", "%3A") + "/"

    fun checksumFor(sha256sums: String, file: String): String? =
        sha256sums.lineSequence()
            .map { it.trim().split(WHITESPACE) }
            .firstOrNull { parts -> parts.size >= 2 && parts.last().trimStart('*') == file }
            ?.firstOrNull()
            ?.takeIf { SHA256.matches(it) }

    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { String.format(Locale.ROOT, "%02x", it) }
    }

    private val WHITESPACE = Regex("\\s+")
}
