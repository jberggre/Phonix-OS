package com.example.engine

import com.example.model.PackReceipt
import com.example.model.PackRelease
import com.example.model.PackTrust
import com.example.model.PackVerdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fetches a pinned pack and installs it into the guest tree only after its bytes verify.
 *
 * The rules, and why each exists:
 *
 *  - **Nothing is written into the guest tree until the digest matches.** The download lands in
 *    app-private `packs/tmp`, outside every rootfs, so a truncated or hostile body never sits where
 *    a guest command could execute it.
 *  - **A partial file never survives.** The temporary file is deleted in `finally`, on success and on
 *    every failure path, and the guest write itself goes through a `.part` name that is renamed only
 *    once the full verified byte count is on disk.
 *  - **Status, length and digest are all checked.** Redirects are followed by the client (so a mirror
 *    move works), but a final non-2xx answer, a short body, a body larger than the pin, or a digest
 *    mismatch all refuse instead of installing something almost right.
 *  - **The receipt is judged by [PackTrust.judge], not by this class.** The socket and the decision
 *    are separate so the refusal table is unit-testable without a network.
 */
class PackDownloader(
    private val privateRoot: File,
    private val guestRoot: () -> File?,
    private val client: OkHttpClient = defaultClient()
) {

    private val tmpDir: File get() = File(privateRoot, "tmp")
    private val markerDir: File get() = File(privateRoot, "verified")

    /**
     * Installs [release], or refuses with a reason. A null [release] means the caller named a pack
     * this build has no digest for, which is a refusal and not a lookup failure to ignore.
     */
    suspend fun install(release: PackRelease?): Result<File> = withContext(Dispatchers.IO) {
        if (release == null) {
            return@withContext Result.failure(
                IllegalStateException("No pack by that id is pinned in this build, so nothing was installed.")
            )
        }
        PackTrust.validate(release)?.let { return@withContext Result.failure(IllegalStateException(it)) }
        val root = guestRoot() ?: return@withContext Result.failure(
            IllegalStateException("Start the Linux guest first; a pack is installed into its tree.")
        )
        val destination = resolve(root, release.guestPath) ?: return@withContext Result.failure(
            IllegalStateException("Pack \"${release.id}\" declares a guest path outside the guest.")
        )

        tmpDir.mkdirs()
        val part = File(tmpDir, release.id + ".part")
        part.delete()
        try {
            val receipt = fetch(release, part)
            when (val verdict = PackTrust.judge(release, receipt)) {
                is PackVerdict.Refused -> return@withContext Result.failure(IllegalStateException(verdict.reason))
                is PackVerdict.Verified -> Unit
            }
            val installed = place(release, part, destination)
            writeMarker(release)
            Result.success(installed)
        } catch (error: Throwable) {
            Result.failure(
                IllegalStateException(
                    "Pack \"${release.id}\" was not installed: " + (error.message ?: error.javaClass.simpleName),
                    error
                )
            )
        } finally {
            // Outside the guest tree, and removed whatever happened, so nothing half downloaded can
            // be mistaken for a usable pack or run by a guest command.
            part.delete()
        }
    }

    /** Downloads [release] into [part] and reports what arrived, without judging it. */
    private fun fetch(release: PackRelease, part: File): PackReceipt {
        val request = Request.Builder()
            .url(release.url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return PackReceipt(response.code, 0L, "")
            val body = response.body ?: return PackReceipt(response.code, 0L, "")
            val digest = MessageDigest.getInstance("SHA-256")
            var written = 0L
            FileOutputStream(part).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        written += read
                        if (written > release.bytes) {
                            // Stop immediately: a body bigger than the pin is already wrong, and
                            // writing it out would waste the user's storage on unverifiable bytes.
                            return PackReceipt(response.code, written, "")
                        }
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                    }
                }
                output.fd.sync()
            }
            return PackReceipt(response.code, written, digest.digest().toHex())
        }
    }

    /** Copies verified bytes into the guest through a staged name, so an interrupted write is inert. */
    private fun place(release: PackRelease, verified: File, destination: File): File {
        destination.parentFile?.mkdirs()
        val staged = File(destination.parentFile, destination.name + ".part")
        staged.deleteRecursively()
        try {
            if (!verified.renameTo(staged) &&
                !runCatching { verified.copyTo(staged, overwrite = true) }.isSuccess
            ) {
                throw IllegalStateException("The verified bytes could not be staged in the guest.")
            }
            // Deliberately no `destination.delete()` first: a POSIX rename replaces the old file
            // atomically, so deleting first would destroy a pack that already works for nothing.
            if (!staged.renameTo(destination)) {
                staged.copyTo(destination, overwrite = true)
            }
            if (!destination.isFile || destination.length() != release.bytes) {
                destination.delete()
                throw IllegalStateException("The guest file did not keep the verified byte count.")
            }
            return destination
        } finally {
            // A staging name must not outlive the attempt, whatever failed: a guest command must
            // never find a half-written `*.part` where the pack belongs.
            staged.deleteRecursively()
        }
    }

    /**
     * Records the digest that was installed.
     *
     * [verified] uses this marker instead of rehashing the guest file on every screen, and it still
     * checks the pinned byte count, so a file that was truncated or replaced later stops counting as
     * verified.
     */
    private fun writeMarker(release: PackRelease) {
        markerDir.mkdirs()
        val marker = File(markerDir, release.id + ".sha256")
        val part = File(markerDir, release.id + ".sha256.part")
        part.writeText(release.sha256.lowercase(Locale.ROOT))
        if (!part.renameTo(marker)) {
            part.copyTo(marker, overwrite = true)
            part.delete()
        }
    }

    /**
     * Packs whose marker matches the pin and whose guest file is present at the pinned length.
     *
     * Defaults to the shipped catalogue: a control only a debug build can even name must not be
     * reported as installed by a build that cannot verify it. Callers that can name more pass the
     * same catalogue they resolved the release from.
     */
    fun verified(releases: List<PackRelease> = PackTrust.shipped): Set<String> {
        val root = guestRoot() ?: return emptySet()
        return releases.filter { release ->
            val recorded = runCatching { File(markerDir, release.id + ".sha256").readText().trim() }.getOrNull()
            val file = resolve(root, release.guestPath)
            recorded != null &&
                recorded.equals(release.sha256, ignoreCase = true) &&
                file != null && file.isFile && file.length() == release.bytes
        }.map { it.id }.toSet()
    }

    /** Resolves a guest-relative path, refusing anything that escapes the guest root. */
    private fun resolve(root: File, path: String): File? {
        val canonicalRoot = runCatching { root.canonicalFile }.getOrNull() ?: return null
        val canonical = runCatching { File(root, path).canonicalFile }.getOrNull() ?: return null
        return if (canonical.path.startsWith(canonicalRoot.path + File.separator)) canonical else null
    }

    private fun ByteArray.toHex(): String {
        val chars = "0123456789abcdef".toCharArray()
        val out = CharArray(size * 2)
        for (i in indices) {
            val value = this[i].toInt() and 0xff
            out[i * 2] = chars[value ushr 4]
            out[i * 2 + 1] = chars[value and 0x0f]
        }
        return String(out)
    }

    companion object {
        private const val USER_AGENT = "PhonixOS/1.5 (verified pack fetch)"

        /** A stalled transfer must fail rather than hold a coroutine open forever. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
