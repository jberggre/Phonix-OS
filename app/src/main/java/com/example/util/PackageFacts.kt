package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

/** A browser the device can actually launch, with a size measured from its APK files. */
data class InstalledBrowserInfo(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val apkBytes: Long
)

/**
 * Reads package facts from the platform instead of assuming them. Nothing here is estimated:
 * [InstalledBrowserInfo.apkBytes] is the sum of the package's APK and split APKs on disk, and
 * [isInstalled] is a real PackageManager lookup that answers false for a package this phone
 * simply does not have (most of the Samsung debloat catalogue is absent on a Pixel).
 */
object PackageFacts {
    fun isInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Throwable) {
        false
    }

    /**
     * Presence that a hidden package still satisfies.
     *
     * `setApplicationHidden` makes the package look uninstalled for this user, so the plain
     * [isInstalled] lookup answers false for a package that is very much still on disk. Without
     * this second query, a hidden package could not be told apart from a stale hidden record
     * left behind by a package that has since been removed.
     */
    fun isInstalledIncludingHidden(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Throwable) {
        false
    }

    fun apkBytes(context: Context, packageName: String): Long = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        val paths = listOfNotNull(info.sourceDir) + (info.splitSourceDirs?.toList() ?: emptyList())
        paths.sumOf { path -> runCatching { File(path).length() }.getOrDefault(0L) }
    } catch (_: Throwable) {
        0L
    }

    fun browserInfo(context: Context, packageName: String): InstalledBrowserInfo? = try {
        val pm = context.packageManager
        val pkg = pm.getPackageInfo(packageName, 0)
        InstalledBrowserInfo(
            packageName = packageName,
            label = pkg.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: packageName,
            versionName = pkg.versionName,
            apkBytes = apkBytes(context, packageName)
        )
    } catch (_: Throwable) {
        null
    }

    /**
     * (app, data, cache) bytes from StorageStatsManager. null when Android refuses the query,
     * which is the normal answer without Usage Access - the UI must say so rather than invent one.
     */
    fun storageBreakdown(context: Context, packageName: String): Triple<Long, Long, Long>? = try {
        val sm = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? android.app.usage.StorageStatsManager
        val stats = sm?.queryStatsForPackage(
            android.os.storage.StorageManager.UUID_DEFAULT,
            packageName,
            android.os.Process.myUserHandle()
        )
        if (stats == null) null else Triple(stats.appBytes, stats.dataBytes, stats.cacheBytes)
    } catch (_: Throwable) {
        null
    }

    /** "91 MB" / "1.2 GB" - a measured size, never a guess. */
    fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
        bytes >= 1024L -> "${bytes / 1024L} KB"
        else -> "$bytes B"
    }
}
