package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.service.DesktopAutoLaunchService
import com.example.service.LinuxServerService
import com.example.util.DeviceSetupPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // Automatic boot-time CPU/Wi-Fi locks and Linux startup are opt-in. Upgraded
            // installs keep these behaviors off until the owner saves explicit choices.
            if (DeviceSetupPreferences.shouldStartLinuxAtBoot(context)) {
                val serviceIntent = Intent(context, LinuxServerService::class.java).apply {
                    action = LinuxServerService.ACTION_START_SERVER
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (_: Exception) {
                }
            }

            // 2. Open the desktop only when external-display watching was selected.
            //    Starting MainActivity straight from a boot broadcast is blocked on
            //    Android 10+, so hand off to the service, which reacts both to a monitor
            //    that is already connected and to one plugged in later.
            if (DeviceSetupPreferences.shouldWatchExternalDisplays(context)) {
                DesktopAutoLaunchService.start(context)
            }
        }
    }
}
