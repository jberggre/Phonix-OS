package com.example.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.example.receiver.PhonixDeviceAdminReceiver

/**
 * Removes (hides) host packages for real, but only where Android actually permits it: a
 * device-owner install. On an ordinary app install there is no API that uninstalls another
 * app, so [canRemovePackages] answers false and the caller must fall back to the adb paste
 * path instead of claiming a debloat happened.
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
