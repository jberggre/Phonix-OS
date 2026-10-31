package com.example.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.receiver.PhonixDeviceAdminReceiver
import java.io.File

/**
 * Removes (hides) host packages for real, but only where Android actually permits it: a
 * device-owner install. On an ordinary app install there is no API that uninstalls another
 * app silently, so [launchUninstall] and [launchAppDetails] open the native Android removal /
 * disable dialogs in 1-click so the user can easily confirm and recover storage/RAM.
 *
 * Hiding is reversible: the same package can be un-hidden with setApplicationHidden(false).
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

    /** Returns APK size in bytes, or 0 if package is not found. */
    fun getPackageApkSize(context: Context, packageName: String): Long = try {
        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        File(appInfo.sourceDir).length()
    } catch (_: Throwable) {
        0L
    }

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
}

