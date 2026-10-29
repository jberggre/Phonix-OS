package com.example.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import com.example.model.InstalledAppInfo

object AppLaunchHelper {

    /**
     * Queries all installed launcher applications on the device.
     */
    fun getInstalledLauncherApps(context: Context): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

            val ownPackage = context.packageName

            resolveInfos
                .mapNotNull { ri ->
                    val activityInfo = ri.activityInfo ?: return@mapNotNull null
                    val pkgName = activityInfo.packageName
                    if (pkgName == ownPackage) return@mapNotNull null // Don't list self

                    val label = ri.loadLabel(pm)?.toString() ?: pkgName
                    val icon = try {
                        ri.loadIcon(pm)
                    } catch (_: Exception) {
                        null
                    }
                    val isSystem = (activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                    InstalledAppInfo(
                        packageName = pkgName,
                        activityName = activityInfo.name,
                        label = label,
                        iconDrawable = icon,
                        isSystemApp = isSystem
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Launches a native phone app in DeX-like freeform multi-task window.
     */
    fun launchNativeApp(
        context: Context,
        packageName: String,
        bounds: Rect? = null
    ): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName) ?: return false

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            val optionsBundle = if (bounds != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val options = ActivityOptions.makeBasic()
                    options.setLaunchBounds(bounds)
                    options.toBundle()
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

            if (optionsBundle != null) {
                context.startActivity(intent, optionsBundle)
            } else {
                context.startActivity(intent)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens the Google Play Store, or alternative open-source store / browser catalog.
     */
    fun openAppStore(context: Context, bounds: Rect? = null): Boolean {
        // 1. Try Google Play Store
        val playStorePackage = "com.android.vending"
        if (isPackageInstalled(context, playStorePackage)) {
            if (launchNativeApp(context, playStorePackage, bounds)) return true
        }

        // 2. Try market:// intent
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            return true
        } catch (_: Exception) {
        }

        // 3. Try F-Droid or Aurora Store
        val alternativeStores = listOf("org.fdroid.fdroid", "com.aurora.store", "com.sec.android.app.samsungapps")
        for (pkg in alternativeStores) {
            if (isPackageInstalled(context, pkg)) {
                if (launchNativeApp(context, pkg, bounds)) return true
            }
        }

        // 4. Fallback to Web Play Store in default browser
        return launchWebBrowser(context, "https://play.google.com/store")
    }

    /**
     * Launches default web browser (e.g., Google Chrome)
     */
    fun launchWebBrowser(context: Context, url: String = "https://www.google.com"): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Launches native File Explorer / DocumentsUI
     */
    fun launchFileManager(context: Context): Boolean {
        val filePackages = listOf(
            "com.google.android.documentsui",
            "com.sec.android.app.myfiles",
            "com.android.documentsui",
            "com.mi.android.globalFileexplorer"
        )
        for (pkg in filePackages) {
            if (isPackageInstalled(context, pkg)) {
                if (launchNativeApp(context, pkg)) return true
            }
        }

        return try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Launches native Photos / Gallery
     */
    fun launchPhotos(context: Context): Boolean {
        val photoPackages = listOf(
            "com.google.android.apps.photos",
            "com.sec.android.gallery3d",
            "com.android.gallery3d",
            "com.miui.gallery"
        )
        for (pkg in photoPackages) {
            if (isPackageInstalled(context, pkg)) {
                if (launchNativeApp(context, pkg)) return true
            }
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Launches native Android System Settings
     */
    fun launchSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens the store page for one package, falling back to the web listing.
     *
     * The Play Store is addressed explicitly: `market://` alone also matches Samsung's Galaxy
     * Store, which turns every Install press into an "Open with" chooser on this hardware.
     */
    fun openPlayListing(context: Context, packageId: String): Boolean {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (isPackageInstalled(context, "com.android.vending")) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageId"))
                        .setPackage("com.android.vending")
                        .addFlags(flags)
                )
                return true
            } catch (_: Exception) {
            }
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageId")).addFlags(flags)
            )
            return true
        } catch (_: Exception) {
        }
        return launchWebBrowser(context, "https://play.google.com/store/apps/details?id=$packageId")
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
