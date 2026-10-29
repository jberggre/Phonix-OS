package com.example.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import com.example.MainActivity

/**
 * Opens the Phonix desktop on the external (DeX) display, maximised.
 *
 * These Mini PCs ship without a usable internal panel, so the monitor is the only
 * surface a customer ever sees. That makes "open the desktop on the big screen,
 * filling it" a system function rather than something the user triggers.
 */
object DesktopAutoLaunch {

    private const val TAG = "PhonixAutoLaunch"

    /**
     * The DeX / HDMI display, or null when nothing but the built-in display exists.
     *
     * Must be the physical external screen, not any non-default display: DeX also creates
     * a VIRTUAL display called "Desktop" (its own workspace) and launching there produces
     * a window nobody can ever see. DISPLAY_CATEGORY_PRESENTATION is the system's own
     * answer to "which display is a real screen I can present onto", which is exactly the
     * HDMI output.
     */
    fun externalDisplay(context: Context): Display? {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return null

        val candidates = manager
            .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .filter { it.displayId != Display.DEFAULT_DISPLAY }

        // DeX also exposes a private VIRTUAL display called "Desktop"; it appears in the
        // presentation category but is not a real screen, and launching there produces a
        // window nobody can see. Public (non-private) displays are the physical outputs.
        val public = candidates.filter { (it.flags and Display.FLAG_PRIVATE) == 0 }

        val chosen = public.firstOrNull { it.state == Display.STATE_ON }
            ?: public.firstOrNull()
        if (chosen != null) return chosen

        // Fallback: nothing public was reported, so take any non-default, non-private display.
        return manager.displays
            .filter { it.displayId != Display.DEFAULT_DISPLAY }
            .filter { (it.flags and Display.FLAG_PRIVATE) == 0 }
            .firstOrNull { it.state == Display.STATE_ON }
    }

    fun hasExternalDisplay(context: Context): Boolean = externalDisplay(context) != null

    private fun displayNames(context: Context): String {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return "?"
        return manager.displays.joinToString { d ->
            "${d.displayId}:${d.name}:0x${Integer.toHexString(d.flags)}"
        }
    }

    /**
     * Launches MainActivity sized to the external display.
     *
     * Note on windowing: DeX forces freeform mode and refuses true fullscreen, so a
     * launch whose bounds equal the whole display is the closest equivalent - it is
     * visually indistinguishable from fullscreen to the customer.
     *
     * @return true when the activity was started.
     */
    fun launchDesktop(context: Context): Boolean {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        val display = externalDisplay(context)
        Log.i(TAG, "candidate displays: " + displayNames(context))
        try {
            val options = ActivityOptions.makeBasic()
            if (display != null) {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                display.getRealMetrics(metrics)
                options.setLaunchBounds(Rect(0, 0, metrics.widthPixels, metrics.heightPixels))
                // Deliberately NOT setLaunchDisplayId(): DeX display ids are not stable
                // across sessions (the HDMI output has been both 2 and 11), and targeting a
                // stale id creates a task that is never visible. DeX's own policy already
                // routes new activities to the external display, so bounds are enough.
            }
            context.startActivity(intent, options.toBundle())
            Log.i(TAG, "desktop launched on display ${display?.displayId ?: "default"}")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "maximised launch failed, falling back to default launch", e)
        }

        return try {
            context.startActivity(intent)
            Log.i(TAG, "desktop launched with default options")
            true
        } catch (e: Exception) {
            Log.w(TAG, "could not launch desktop", e)
            false
        }
    }
}
