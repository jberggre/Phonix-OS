package com.example.model

import java.net.URI
import java.util.Locale

/**
 * A pack the app may fetch: bytes, the digest of those exact bytes, and where they belong.
 *
 * The digest table in [PackTrust] is the root of trust, exactly as the F-Droid note describes for
 * the bundled rootfs. Nothing in a manifest, a URL or an HTTP response can supply it: a pack is
 * installed only when the bytes that arrived hash to the value compiled in here.
 */
data class PackRelease(
    val id: String,
    val title: String,
    val url: String,
    /** Lowercase or uppercase hex; compared case-insensitively, must be 64 characters. */
    val sha256: String,
    /** Exact byte count. A shorter or longer body is refused rather than clamped. */
    val bytes: Long,
    /** Path inside the active guest rootfs, relative, no `..`, no leading slash. */
    val guestPath: String,
    val note: String = ""
)

/** What one fetch produced, before any trust decision is made. */
data class PackReceipt(
    val status: Int,
    val bytes: Long,
    val sha256: String
)

sealed interface PackVerdict {
    data object Verified : PackVerdict
    data class Refused(val reason: String) : PackVerdict
}

/**
 * The pinned pack catalogue.
 *
 * Adding a pack is a reviewable act with a documented order:
 *
 *  1. Put the exact bytes somewhere stable and public (this project's own repository is the channel
 *     the roadmap chose, so a release asset or a file under `tools/packs/`).
 *  2. Compute `sha256sum` and `stat -c%s` of the file you are shipping - not of a file that merely
 *     has the same name.
 *  3. Add one [PackRelease] here with that digest, that byte count and the URL that serves those
 *     bytes.
 *  4. Point a manifest at it with `pack = <id>`.
 *
 * There is deliberately no "fetch the digest from the server" path: a digest that arrives over the
 * same connection as the bytes protects against corruption, not against a hostile server.
 */
object PackTrust {

    const val LICENSE_ID = "phonix-license"

    /** Debug-only negative control; see [benchControls]. */
    const val BENCH_MISMATCH_ID = "bench-digest-control"

    val shipped: List<PackRelease> = listOf(
        PackRelease(
            id = LICENSE_ID,
            title = "Phonix OS licence text",
            url = "https://raw.githubusercontent.com/jberggre/Phonix-OS/main/LICENSE",
            // SHA-256 of LICENSE at the commit that added this table; the URL serves that file.
            sha256 = "cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30",
            bytes = 11_358L,
            guestPath = "root/packs/phonix-license.txt",
            note = "The repository's own licence file, installed into the guest as text."
        )
    )

    /**
     * Controls that exist to be refused, and are only offered in debug builds.
     *
     * The bench needs to prove the refusal path with real bytes over a real connection, and a
     * mismatched digest is the only way to do that honestly. The digest below is deliberately not
     * the digest of [PackRelease.url]'s bytes, so `PackTrust.judge` must reject it; a release build
     * never sees this entry.
     */
    val benchControls: List<PackRelease> = listOf(
        PackRelease(
            id = BENCH_MISMATCH_ID,
            title = "Digest control (bench only)",
            url = "https://raw.githubusercontent.com/jberggre/Phonix-OS/main/LICENSE",
            sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
            bytes = 11_358L,
            guestPath = "root/packs/bench-control.txt",
            note = "Negative control: these bytes cannot match this digest."
        )
    )

    fun catalogue(includeBenchControls: Boolean = false): List<PackRelease> =
        if (includeBenchControls) shipped + benchControls else shipped

    fun find(id: String, includeBenchControls: Boolean = false): PackRelease? =
        catalogue(includeBenchControls).firstOrNull { it.id == id.trim().lowercase(Locale.ROOT) }

    private val ID = Regex("[a-z0-9][a-z0-9._-]{0,63}")
    private val HEX = Regex("[0-9a-f]{64}")

    /**
     * Null when [release] is usable; otherwise the reason it is not, phrased for the user.
     *
     * Every branch here is a refusal rather than a default, because a pack that is missing a digest
     * or a byte count cannot be verified, and "cannot be verified" must never read as "fine".
     */
    fun validate(release: PackRelease): String? {
        if (!ID.matches(release.id)) return "\"${release.id}\" is not a pack id."
        val digest = release.sha256.trim().lowercase(Locale.ROOT)
        if (digest.isEmpty()) return "Pack \"${release.id}\" pins no SHA-256, so it cannot be trusted."
        if (!HEX.matches(digest)) return "Pack \"${release.id}\" pins a SHA-256 that is not 64 hex characters."
        if (release.bytes <= 0L) return "Pack \"${release.id}\" pins no byte count."
        val uri = runCatching { URI(release.url) }.getOrNull()
            ?: return "Pack \"${release.id}\" has no usable URL."
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        val loopback = host == "127.0.0.1" || host == "localhost" || host == "::1"
        if (scheme != "https" && !(scheme == "http" && loopback)) {
            return "Pack \"${release.id}\" must be fetched over HTTPS."
        }
        val path = release.guestPath
        if (path.isBlank() || path.startsWith("/") || path.split('/').any { it.isEmpty() || it == ".." }) {
            return "Pack \"${release.id}\" declares an unsafe guest path \"$path\"."
        }
        return null
    }

    /**
     * The trust decision, separated from the socket so both halves can be tested.
     *
     * Order matters: an unknown pack is refused before its receipt is looked at, and a receipt is
     * only "verified" when the status, the byte count and the digest all agree with the pin.
     */
    fun judge(release: PackRelease?, receipt: PackReceipt): PackVerdict {
        if (release == null) {
            return PackVerdict.Refused("No pack by that id is pinned in this build, so nothing was installed.")
        }
        validate(release)?.let { return PackVerdict.Refused(it) }
        if (receipt.status !in 200..299) {
            return PackVerdict.Refused("The server answered HTTP ${receipt.status}; nothing was installed.")
        }
        if (receipt.bytes != release.bytes) {
            val expected = release.bytes
            return PackVerdict.Refused(
                "Got ${receipt.bytes} of $expected bytes" +
                    (if (receipt.bytes < expected) " (truncated)" else " (larger than declared)") +
                    "; nothing was installed."
            )
        }
        if (!receipt.sha256.equals(release.sha256, ignoreCase = true)) {
            return PackVerdict.Refused(
                "SHA-256 mismatch: got ${receipt.sha256.take(12)}..., pinned ${release.sha256.take(12)}...; " +
                    "nothing was installed."
            )
        }
        return PackVerdict.Verified
    }
}
