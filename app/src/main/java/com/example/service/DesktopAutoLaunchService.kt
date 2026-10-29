package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.util.DesktopAutoLaunch

/**
 * Watches for the HDMI/DeX display and opens the Phonix desktop on it.
 *
 * Why a service: the unit usually boots with the monitor already attached, and DeX can
 * also come up long after boot when a cable is plugged in. Both cases need something
 * alive that can react, which a launcher activity cannot provide (and a display
 * listener registered by an activity dies with it).
 *
 * Why foreground: Android 8+ kills plain background services; this one is meant to live
 * for the whole uptime of the appliance.
 */
class DesktopAutoLaunchService : Service(), DisplayManager.DisplayListener {

    private lateinit var displayManager: DisplayManager
    private val handler = Handler(Looper.getMainLooper())
    private var launchedForDisplayId: Int? = null

    private val launchRunnable = Runnable { launchIfPossible() }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(this, handler)
        Log.i(TAG, "watching for an external display")
        // If the monitor is already attached, open the desktop straight away. The delay
        // gives DeX time to finish setting up its display before we launch into it.
        handler.postDelayed(launchRunnable, INITIAL_LAUNCH_DELAY_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(launchRunnable)
        handler.postDelayed(launchRunnable, INITIAL_LAUNCH_DELAY_MS)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(launchRunnable)
        try {
            displayManager.unregisterDisplayListener(this)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- DisplayManager callbacks -------------------------------------------------

    override fun onDisplayAdded(displayId: Int) {
        if (displayId == android.view.Display.DEFAULT_DISPLAY) return
        Log.i(TAG, "display $displayId attached, launching desktop")
        launchedForDisplayId = null
        handler.removeCallbacks(launchRunnable)
        // DeX reports the display slightly before it is ready to host windows.
        handler.postDelayed(launchRunnable, DISPLAY_SETTLE_DELAY_MS)
    }

    override fun onDisplayRemoved(displayId: Int) {
        if (launchedForDisplayId == displayId) launchedForDisplayId = null
    }

    override fun onDisplayChanged(displayId: Int) {
        // A monitor that switches to STATE_ON after docking shows up here rather than in
        // onDisplayAdded, so treat "external display became usable" as a launch trigger.
        if (displayId == android.view.Display.DEFAULT_DISPLAY) return
        if (launchedForDisplayId == null) handler.postDelayed(launchRunnable, DISPLAY_SETTLE_DELAY_MS)
    }

    // --- internals ----------------------------------------------------------------

    private fun launchIfPossible() {
        val display = DesktopAutoLaunch.externalDisplay(this)
        if (display == null) {
            Log.i(TAG, "no external display yet")
            return
        }
        if (launchedForDisplayId == display.displayId) return
        launchedForDisplayId = display.displayId
        DesktopAutoLaunch.launchDesktop(this)
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Desktop",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps the Phonix desktop ready for the connected monitor"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Phonix desktop ready")
            .setContentText("Waiting for the monitor")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "PhonixAutoLaunch"
        private const val CHANNEL_ID = "phonix_desktop_auto_launch"
        private const val NOTIFICATION_ID = 4701
        private const val INITIAL_LAUNCH_DELAY_MS = 2_500L
        private const val DISPLAY_SETTLE_DELAY_MS = 1_500L

        fun start(context: Context) {
            val intent = Intent(context, DesktopAutoLaunchService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "could not start auto-launch service", e)
            }
        }
    }
}
