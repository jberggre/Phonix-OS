package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.service.DesktopAutoLaunchService
import com.example.service.LinuxServerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // 1. Immediately start 24/7 background Linux Server WakeLock service
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

            // 2. Open the desktop on whatever monitor is attached.
            //    Starting MainActivity straight from a boot broadcast is blocked on
            //    Android 10+, so hand off to the service, which reacts both to a monitor
            //    that is already connected and to one plugged in later.
            DesktopAutoLaunchService.start(context)
        }
    }
}
