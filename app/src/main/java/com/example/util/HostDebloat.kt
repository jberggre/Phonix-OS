package com.example.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.receiver.PhonixDeviceAdminReceiver
import java.io.File

/** What the device actually reports about a host package, as opposed to what the app hopes. */
enum class HostPackageState {
    /** On disk and visible to the launcher. */
    INSTALLED,

    /** On disk but hidden from the launcher and from resolved intents by DevicePolicyManager. */
    HIDDEN,

    /** Not on this device at all. */
    ABSENT
}

/**
 * Removes (hides) host packages for real, but only where Android actually permits it: a
 * device-owner install. On an ordinary app install there is no API that uninstalls another
 * app silently, so [launchUninstall] and [launchAppDetails] open the native Android removal /
 * disable dialogs in 1-click so the user can easily confirm and recover storage/RAM.
 *
 * Hiding is reversible: the same package can be un-hidden with setApplicationHidden(false), and
 * [packageState] asks the platform which of the three states a package is in. Without that query
 * a hidden package kept reading as INSTALLED, because `getPackageInfo` still answers for it.
 */
object HostDebloat {
    private fun dpm(context: Context): DevicePolicyManager? =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    private fun admin(context: Context): ComponentName =
        ComponentName(context, PhonixDeviceAdminReceiver::class.java)

    fun canRemovePackages(context: Context): Boolean = try {
        dpm(context)?.isDeviceOwnerApp(context.packageName) == true
    } catch (_: Throwable) {
        false
    }

    /** Pure mapping of the two platform answers, so the tri-state can be tested off-device. */
    fun stateOf(installed: Boolean, hidden: Boolean): HostPackageState = when {
        !installed -> HostPackageState.ABSENT
        hidden -> HostPackageState.HIDDEN
        else -> HostPackageState.INSTALLED
    }

    /** True only while this install (as device owner) is hiding the package. */
    fun isHidden(context: Context, packageName: String): Boolean = try {
        dpm(context)?.isApplicationHidden(admin(context), packageName) == true
    } catch (_: Throwable) {
        false
    }

    /**
     * Asks the platform for the package's real state. [hiddenSnapshot] lets a caller pass the
     * answer it already has instead of querying DevicePolicyManager once per package per frame.
     */
    fun packageState(
        context: Context,
        packageName: String,
        hiddenSnapshot: Boolean? = null
    ): HostPackageState = stateOf(
        installed = PackageFacts.isInstalled(context, packageName),
        hidden = hiddenSnapshot ?: isHidden(context, packageName)
    )

    /**
     * Returns the package's installed size in bytes, or 0 when it is not on this device.
     *
     * This used to count `sourceDir` only, which is the base APK: every split package (Chrome,
     * Play services, most Samsung apps) was under-reported by the size of its splits.
     */
    fun getPackageApkSize(context: Context, packageName: String): Long =
        PackageFacts.apkBytes(context, packageName)

    /** Launches 1-click native package uninstallation dialog. */
    fun launchUninstall(context: Context, packageName: String): Boolean = try {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Throwable) {
        try {
            val fallback = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            true
        } catch (_: Throwable) {
            launchAppDetails(context, packageName)
        }
    }

    /** Opens the native App Settings screen where user can Disable, Turn Off, or Clear Data. */
    fun launchAppDetails(context: Context, packageName: String): Boolean = try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Throwable) {
        false
    }

    /** Null on success, otherwise a short reason the wizard can log verbatim. */
    fun hidePackage(context: Context, packageName: String): String? {
        val policy = dpm(context) ?: return "no DevicePolicyManager"
        return try {
            if (policy.setApplicationHidden(admin(context), packageName, true)) {
                null
            } else {
                "not hidden (the platform kept it visible)"
            }
        } catch (e: Throwable) {
            val detail = e.message?.take(90)
            if (detail.isNullOrBlank()) e.javaClass.simpleName else "${e.javaClass.simpleName}: $detail"
        }
    }

    /** Undoes [hidePackage]. Null on success, otherwise the platform's own refusal. */
    fun unhidePackage(context: Context, packageName: String): String? {
        val policy = dpm(context) ?: return "no DevicePolicyManager"
        return try {
            if (policy.setApplicationHidden(admin(context), packageName, false)) {
                null
            } else {
                "not restored (the platform kept it hidden)"
            }
        } catch (e: Throwable) {
            val detail = e.message?.take(90)
            if (detail.isNullOrBlank()) e.javaClass.simpleName else "${e.javaClass.simpleName}: $detail"
        }
    }

    /** True when the given package is this app or one of its own components. Never touch those. */
    fun isOwnPackage(context: Context, packageName: String): Boolean =
        packageName == context.packageName

    /** Convenience for logs and diagnostics: the size of a package's APK files on disk. */
    fun describeApkSize(context: Context, packageName: String): String =
        PackageFacts.formatBytes(getPackageApkSize(context, packageName))

    /** Kept for callers that only need the raw path length. */
    fun primaryApkFile(context: Context, packageName: String): File? = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        File(info.sourceDir)
    } catch (_: Throwable) {
        null
    }
}
