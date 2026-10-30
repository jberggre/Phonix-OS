package com.example.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import com.example.MainActivity
import com.example.model.DeviceCompatibilityClassifier

/**
 * Opens the Phonix desktop on the external (DeX) display, maximised.
 *
 * These Mini PCs ship without a usable internal panel, so the monitor is the only
 * surface a customer ever sees. That makes "open the desktop on the big screen,
 * filling it" a system function rather than something the user triggers.
 */
object DesktopAutoLaunch {

    private const val TAG = "PhonixAutoLaunch"

    /** Vendor category under which Samsung publishes the DeX desktop workspace. */
    private const val SAMSUNG_DESKTOP_CATEGORY = "com.samsung.android.hardware.display.category.DESKTOP"

    /**
     * The desktop workspace the monitor is showing, or null while it does not exist yet.
     *
     * On Samsung this is NOT the HDMI output. DeX renders its workspace on a virtual display
     * named "Desktop" and presents that onto the HDMI screen, so only the virtual one carries
     * the composed picture. A window placed on the physical HDMI display (which does appear in
     * DISPLAY_CATEGORY_PRESENTATION) is drawn underneath DeX's presentation: measured on the
     * S10, launching there left the monitor showing Samsung's launcher while Phonix ran happily
     * on a display nobody could see.
     *
     * It also does not exist for the first seconds after the cable is plugged in, which is why
     * callers must treat null as "wait", not as "launch somewhere else". Starting Phonix on the
     * HDMI output and moving it later is worse than not starting it: DeX stops the move with a
     * "This app is already running on another display" dialog that needs a mouse click, so the
     * unit would sit at a prompt forever.
     */
    fun workspaceDisplay(context: Context): Display? {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return null

        // Samsung does not expose the DeX workspace in getDisplays(): the plain call only returns
        // the built-in screen and the HDMI output on this S10, which is why launching at the HDMI
        // display looked "successful" while DeX covered it. The workspace is published under a
        // vendor display category instead (Samsung DeX app-optimisation guide).
        runCatching {
            manager.getDisplays(SAMSUNG_DESKTOP_CATEGORY)
                .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && it.state == Display.STATE_ON }
        }.getOrNull()?.let { return it }

        // Fallback: some builds do include it in the normal list under this name.
        return manager.displays
            .filter { it.displayId != Display.DEFAULT_DISPLAY && (it.flags and Display.FLAG_PRIVATE) == 0 }
            .firstOrNull { it.state == Display.STATE_ON && it.name.equals("Desktop", ignoreCase = true) }
    }

    /**
     * The physical monitor output, if one is attached.
     *
     * Only for the case where no workspace ever appears (a phone that simply mirrors to HDMI).
     * Launching here while a desktop exists puts the window under DeX's presentation, invisible.
     */
    fun hasDexWorkspace(context: Context): Boolean = workspaceDisplay(context) != null

    fun externalDisplays(context: Context): List<Display> =
        (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.displays
            ?.filter { it.displayId != Display.DEFAULT_DISPLAY && it.isExternalOutput() }
            .orEmpty()

    fun physicalDisplay(context: Context): Display? {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return null
        return manager.displays
            .filter { it.displayId != Display.DEFAULT_DISPLAY }
            .filter { (it.flags and Display.FLAG_PRIVATE) == 0 }
            .filter { it.isExternalOutput() && !it.name.equals("Desktop", ignoreCase = true) }
            .maxByOrNull(::areaOf)
    }

    /**
     * A second panel of the same phone is not a monitor, whatever its display id says.
     *
     * Measured on a Pixel 9 Pro Fold (Android 17): the cover panel appears as display 3, without
     * FLAG_PRIVATE and without FLAG_PRESENTATION. Only a presentation output (HDMI/DP/wireless)
     * counts here, which is also how DisplayManager filters its presentation category.
     */
    private fun Display.isExternalOutput(): Boolean =
        DeviceCompatibilityClassifier.isPresentationOutput(
            hasPresentationFlag = (flags and Display.FLAG_PRESENTATION) != 0,
            isOn = state == Display.STATE_ON
        )

    /** Workspace if it exists, otherwise the physical output (mirroring phones only). */
    fun externalDisplay(context: Context): Display? =
        workspaceDisplay(context) ?: physicalDisplay(context)

    /** True when a monitor is attached, in any form. */
    fun hasExternalDisplay(context: Context): Boolean = externalDisplay(context) != null

    /** Pixel area of a display, used only to rank candidates. */
    private fun areaOf(display: Display): Long {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        return metrics.widthPixels.toLong() * metrics.heightPixels
    }

    /** Every display this app is allowed to see, for logs when a monitor has no workspace. */
    fun describeDisplays(context: Context): String {
        val category = runCatching {
            val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            manager?.getDisplays(SAMSUNG_DESKTOP_CATEGORY)?.joinToString { "${it.displayId}:${it.name}" }
                ?: "?"
        }.getOrElse { "error ${it.message}" }
        return displayNames(context) + " | dex-category: " + category
    }

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
