package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Enrolment target for Phonix bench provisioning.
 *
 * `provision.sh` promotes this component to device owner with
 * `dpm set-device-owner`. That is what unlocks the "brand new PC" behaviour that
 * a plain launcher cannot do: forcing Phonix as the permanent HOME activity,
 * disabling the keyguard on a machine with no usable panel, starting the desktop
 * from BOOT_COMPLETED (Android 10+ otherwise blocks background activity starts),
 * hiding packages that get sideloaded later, and blocking a factory reset.
 */
class PhonixDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
