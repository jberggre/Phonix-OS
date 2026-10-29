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
     * The display the customer is actually looking at, or null when no monitor is attached.
     *
     * On Samsung this is NOT the HDMI output. DeX renders its workspace on a virtual display
     * named "Desktop" and presents that onto the HDMI screen, so only the virtual one carries
     * the composed picture. A window placed on the physical HDMI display (which does appear in
     * DISPLAY_CATEGORY_PRESENTATION) is drawn underneath DeX's presentation and is invisible:
     * measured on the S10, launching there left the monitor showing Samsung's launcher while
     * Phonix ran happily on a display nobody could see. So the "Desktop" workspace wins.
     *
     * On phones with a different desktop implementation the name will not match, so the fallback
     * is the largest app-visible display that is on. That is still the desktop workspace rather
     * than the raw output, because the workspace is the display apps are composed onto.
     */
    fun externalDisplay(context: Context): Display? {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return null

        val candidates = manager.displays.filter { it.displayId != Display.DEFAULT_DISPLAY }
        if (candidates.isEmpty()) return null

        candidates.firstOrNull { it.name.equals("Desktop", ignoreCase = true) && it.state == Display.STATE_ON }
            ?.let { return it }

        val public = candidates.filter { (it.flags and Display.FLAG_PRIVATE) == 0 }
        return public
            .filter { it.state == Display.STATE_ON }
            .maxByOrNull(::areaOf)
            ?: public.maxByOrNull(::areaOf)
    }

    /** Pixel area of a display, used only to rank candidates. */
    private fun areaOf(display: Display): Long {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        return metrics.widthPixels.toLong() * metrics.heightPixels
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
                // Resolved fresh on every launch, by name, precisely because DeX renumbers its
                // workspace between sessions. Bounds alone are not enough: without an explicit
                // display the window can land on the raw HDMI output, which DeX covers with its
                // own presentation, so the customer sees the launcher instead of Phonix.
                options.setLaunchDisplayId(display.displayId)
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
