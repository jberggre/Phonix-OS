package com.example.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import com.example.R

/**
 * Applies the Phonix wallpaper to the device so the Samsung surfaces behind us
 * (lock screen, phone home, and DeX where the platform mirrors it) match the desktop.
 *
 * This is the only part of "make DeX look like Phonix" an app is allowed to touch:
 * the system wallpaper needs only the normal SET_WALLPAPER permission. DeX's own
 * launcher, taskbar and icon layout are Samsung's and cannot be replaced by an app.
 */
object WallpaperProvisioner {

    private const val TAG = "PhonixWallpaper"
    private const val PREFS = "phonix_provisioning"
    private const val KEY_VERSION = "phonix_wallpaper_version"

    /**
     * Bumped whenever the bundled wallpaper changes. The device keeps our wallpaper until
     * this changes, so an app update refreshes it but day-to-day use never fights the user.
     */
    private const val BUNDLE_VERSION = 3

    /** Applies the wallpaper once per bundle version; later runs leave the user's choice alone. */
    fun applyOnce(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, 0) >= BUNDLE_VERSION) return
        if (apply(context)) {
            prefs.edit().putInt(KEY_VERSION, BUNDLE_VERSION).apply()
        }
    }

    /** Force-applies the bundled Phonix wallpaper. Returns true on success. */
    fun apply(context: Context): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_phoenix_wallpaper)
                ?: run {
                    Log.w(TAG, "wallpaper drawable could not be decoded")
                    return false
                }
            val manager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            } else {
                @Suppress("DEPRECATION")
                manager.setBitmap(bitmap)
            }
            // DeX keeps a separate desktop wallpaper that an app cannot write: passing an
            // extra flag to setBitmap is rejected ("Must specify a valid wallpaper category"),
            // and Samsung exposes no public API for it. The DeX background is therefore a
            // one-time manual step on the unit; this call covers the phone home screen.
            bitmap.recycle()
            Log.i(TAG, "Phonix wallpaper applied")
            true
        } catch (e: Exception) {
            // Missing permission or a vendor restriction must never take the desktop down.
            Log.w(TAG, "could not set wallpaper", e)
            false
        }
    }
}
