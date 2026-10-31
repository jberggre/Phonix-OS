package com.example.model

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * A concrete rootfs build on the image mirror, with the timestamp the mirror published it under.
 */
data class DistroImageRef(val url: String, val buildDate: String)

/**
 * Resolves which rootfs archive to download for a distribution.
 *
 * `images.linuxcontainers.org` never serves `/images/debian/bookworm/arm64/default/rootfs.tar.xz`:
 * every build lives under a dated directory, so the real path looks like
 * `/images/debian/bookworm/arm64/default/20261028_04:18/rootfs.tar.xz`. Asking for the undated
 * path returns 404, which is why the catalog could never download anything.
 *
 * The mirror publishes every path it has in `meta/1.0/index-system`, one semicolon-separated row
 * per image:
 *
 * ```
 * debian;bookworm;arm64;default;20261028_04:18;/images/debian/bookworm/arm64/default/20261028_04:18/
 * ```
 *
 * Resolving the newest row at install time (instead of hardcoding one dated URL that rots the next
 * day) keeps the download alive as the mirror rotates builds. This is pure string work so it can be
 * unit-tested without a device, a network or a running guest.
 */
object DistroIndex {

    const val MIRROR = "https://images.linuxcontainers.org"

    /** Plain-text listing of every image build the mirror currently holds. */
    const val INDEX_URL = "$MIRROR/meta/1.0/index-system"

    /** File inside a dated build directory that holds the unpacked userspace. */
    const val ROOTFS_FILE = "rootfs.tar.xz"

    /**
     * Newest build for [name]/[release] on [arch] (the `default` variant unless another is asked
     * for), or null when the mirror carries no such image. Dates are ISO-like and fixed width
     * (`20261028_04:18`), so a plain string comparison orders them correctly.
     */
    /**
     * The mirror publishes a `SHA256SUMS` beside each `rootfs.tar.xz`. Only an archive that
     * matches its published digest is installed: a truncated or substituted tarball must never
     * become the guest the user runs.
     */
    fun checksumFor(sha256sums: String, file: String = ROOTFS_FILE): String? =
        sha256sums.lineSequence()
            .map { line -> line.trim().split(' ').filter { it.isNotEmpty() } }
            .firstOrNull { parts -> parts.size >= 2 && parts.last().trimStart('*') == file }
            ?.firstOrNull()
            ?.takeIf { SHA256.matches(it) }

    /** Streaming SHA-256 of [file], lowercase hex. */
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

    private val SHA256 = Regex("^[0-9a-fA-F]{64}$")

    fun resolve(
        index: String,
        name: String,
        release: String,
        arch: String = "arm64",
        variant: String = "default"
    ): DistroImageRef? {
        var bestDate: String? = null
        var bestPath: String? = null

        for (row in index.lineSequence()) {
            val cols = row.trim().split(';')
            if (cols.size < 6) continue
            if (!cols[0].trim().equals(name, ignoreCase = true)) continue
            if (!cols[1].trim().equals(release, ignoreCase = true)) continue
            if (!cols[2].trim().equals(arch, ignoreCase = true)) continue
            if (!cols[3].trim().equals(variant, ignoreCase = true)) continue

            val date = cols[4].trim()
            val path = cols[5].trim()
            if (date.isEmpty() || path.isEmpty()) continue

            val previous = bestDate
            if (previous == null || date > previous) {
                bestDate = date
                bestPath = path
            }
        }

        val date = bestDate ?: return null
        val path = bestPath ?: return null
        return DistroImageRef(MIRROR + path.trimEnd('/') + "/" + ROOTFS_FILE, date)
    }
}
