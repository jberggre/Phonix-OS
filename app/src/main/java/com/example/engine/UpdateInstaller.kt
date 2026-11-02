package com.example.engine

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.DistroIndex
import com.example.model.UpdateManifest
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.Locale

/**
 * Downloads a published update, verifies it, and hands it to Android's package installer.
 *
 * Three gates stand between the feed and the device:
 *
 *  1. the feed must be HTTPS and every path is checked by [com.example.model.UpdateFeed];
 *  2. the downloaded APK must match the feed's published SHA-256 byte for byte;
 *  3. the APK must be signed by the same certificate as the installed app. Android would reject
 *     anything else anyway, but checking first turns a silent installer failure into a message.
 *
 * Nothing here installs silently. The session commits with the system's own confirmation dialog,
 * so a phone in a deck is never updated without someone seeing it.
 */
class UpdateInstaller(private val context: Context, private val userAgent: String = "PhonixOS-Updater") {

    private val appContext: Context = context.applicationContext

    /** Package-manager queries, the APK copy and the session commit run here, never on main. */
    private val io = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "phonix-update").apply { isDaemon = true }
    }

    /** Reads the feed document. Redirects are followed; a non-2xx is an error, not an empty state. */
    fun readFeed(url: String): String {
        // The feed decides which APK is offered, so it gets the same https rule the archive
        // URL has. A plain-http feed could otherwise hand over any build it liked.
        require(url.startsWith("https://", ignoreCase = true)) {
            "The update feed must be served over https"
        }
        val conn = open(url, timeoutMs = 20_000)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("The update feed answered HTTP $code")
            return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    /**
     * Streams the feed's APK into the app cache. The file is written as `.part` and only renamed
     * once its byte count matches `Content-Length` **and** its SHA-256 matches the feed, so a
     * dropped connection cannot leave a half APK that looks installable.
     */
    fun download(
        manifest: UpdateManifest,
        onProgress: (String) -> Unit = {},
        onBytes: (Long, Long) -> Unit = { _, _ -> }
    ): File {
        require(manifest.apkUrl.startsWith("https://", ignoreCase = true)) {
            "The update feed must serve the APK over https"
        }
        val dir = File(appContext.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "phonix-${manifest.versionCode}.apk")
        val part = File(dir, target.name + ".part")
        if (part.exists()) part.delete()

        val conn = open(manifest.apkUrl, timeoutMs = 60_000)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code while downloading the update")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: 0L
            onProgress(
                if (total > 0) "Downloading ${manifest.versionName} (${megabytes(total)})..."
                else "Downloading ${manifest.versionName}..."
            )

            var written = 0L
            var reported = 0L
            conn.inputStream.use { input ->
                FileOutputStream(part).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        written += read
                        if (written - reported >= (1 shl 20)) {
                            reported = written
                            onBytes(written, total)
                        }
                    }
                }
            }
            onBytes(written, total)

            if (written < MIN_APK_BYTES) {
                throw IllegalStateException("The download ended after $written bytes — not an APK")
            }
            if (total > 0 && written != total) {
                throw IllegalStateException("Download truncated at $written of $total bytes")
            }

            val actual = DistroIndex.sha256Hex(part)
            if (!actual.equals(manifest.sha256, ignoreCase = true)) {
                throw IllegalStateException(
                    "SHA-256 mismatch: the feed published ${manifest.sha256.take(12)}…, " +
                        "the download is ${actual.take(12)}…. Nothing was installed."
                )
            }
            onProgress("Verified the published SHA-256.")

            target.delete()
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            return target
        } catch (e: Throwable) {
            runCatching { part.delete() }
            throw e
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    /**
     * Opens a package-install session for an already verified [apk] and reports the outcome of
     * Android's confirmation through [onResult]. Must be called on the main thread.
     */
    fun install(apk: File, onResult: (Boolean, String) -> Unit) {
        val main = Handler(Looper.getMainLooper())
        // Results are posted back to the main thread; every exit below goes through `finish`.
        val finish: (Boolean, String) -> Unit = { ok, message -> main.post { onResult(ok, message) } }

        // Signing checks are two PackageManager queries plus an archive parse, and the session
        // writes the whole APK. Running that on the caller's (main) thread was the stall this
        // class's own documentation warns about.
        io.execute {
            try {
                if (!signersMatch(apk)) {
                    finish(
                        false,
                        "The downloaded APK is not signed by this app's key, so Android would refuse it. " +
                            "Nothing was installed."
                    )
                    return@execute
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !appContext.packageManager.canRequestPackageInstalls()
                ) {
                    main.post { openUnknownSourcesSettings() }
                    finish(
                        false,
                        "Android needs “Install unknown apps” for Phonix before it can apply an update. " +
                            "The setting has been opened for you."
                    )
                    return@execute
                }

                commitSession(apk, finish)
            } catch (e: Exception) {
                Log.w(TAG, "could not start the update install", e)
                finish(
                    false,
                    "Could not hand the update to Android: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    /** Opens Android's per-app "install unknown apps" screen. Main thread only. */
    private fun openUnknownSourcesSettings() {
        runCatching {
            appContext.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${appContext.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * Creates the package-install session and commits it.
     *
     * Each session keeps its own receiver in [pendingReceivers]; the single static slot this
     * replaced meant a second update silently orphaned the first session's result.
     */
    private fun commitSession(apk: File, finish: (Boolean, String) -> Unit) {
        val installer = appContext.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply { setAppPackageName(appContext.packageName) }
        val sessionId = installer.createSession(params)

        installer.openSession(sessionId).use { session ->
            val out = session.openWrite("phonix-update.apk", 0, apk.length())
            try {
                FileInputStream(apk).use { it.copyTo(out) }
                session.fsync(out)
            } finally {
                out.close()
            }

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    runCatching { appContext.unregisterReceiver(this) }
                    pendingReceivers.remove(this)

                    val status = intent?.getIntExtra(
                        PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE
                    ) ?: PackageInstaller.STATUS_FAILURE
                    val message = intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
                    apk.delete()

                    if (status == PackageInstaller.STATUS_SUCCESS) {
                        finish(true, "Android installed the verified update.")
                    } else {
                        finish(
                            false,
                            "The installer refused the update (status $status)" +
                                if (message.isBlank()) "." else ": $message"
                        )
                    }
                }
            }
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(ACTION_INSTALL_RESULT),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            pendingReceivers += receiver

            val intent = Intent(ACTION_INSTALL_RESULT).setPackage(appContext.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = PendingIntent.getBroadcast(appContext, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }

    /** True when the archive is the same package, signed by the certificate currently installed. */
    private fun signersMatch(apk: File): Boolean {
        val pm = appContext.packageManager
        val flags = signingFlags()
        val archive = runCatching { pm.getPackageArchiveInfo(apk.absolutePath, flags) }.getOrNull()
            ?: return false
        if (archive.packageName != appContext.packageName) return false
        val installed = runCatching { pm.getPackageInfo(appContext.packageName, flags) }.getOrNull()
            ?: return false
        val installedDigests = certificateDigests(installed)
        return installedDigests.isNotEmpty() && installedDigests == certificateDigests(archive)
    }

    private fun signingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES
        else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES

    private fun certificateDigests(info: PackageInfo): Set<String> {
        val certs: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return emptySet()
            if (signing.hasMultipleSigners()) signing.apkContentsSigners
            else signing.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION") info.signatures
        }
        return certs.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { String.format(Locale.ROOT, "%02x", it) }
        }.toSet()
    }

    private fun open(url: String, timeoutMs: Int): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", userAgent)
        }

    private fun megabytes(bytes: Long): String =
        String.format(Locale.ROOT, "%.0f MB", bytes / (1024.0 * 1024.0))

    companion object {
        /** Smallest plausible APK; catches an HTML error page saved as `.apk`. */
        const val MIN_APK_BYTES = 256L * 1024

        const val ACTION_INSTALL_RESULT = "com.example.phonix.action.UPDATE_INSTALL_RESULT"

        private const val TAG = "PhonixUpdate"

        /** Receivers for in-flight sessions. Each removes itself when its result arrives. */
        private val pendingReceivers = CopyOnWriteArrayList<BroadcastReceiver>()
    }
}
