package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Keeps a user's own photo as the desktop background.
 *
 * The chosen picture is copied into the app's private files directory and downscaled before it is
 * stored, so the desktop never decodes a 12-megapixel camera original on the UI thread and the
 * photo disappears with the app on uninstall. Nothing is uploaded anywhere.
 */
object WallpaperStore {

    private const val TAG = "PhonixWallpaper"
    private const val FILE_NAME = "desktop_custom_wallpaper.jpg"
    private const val MAX_EDGE_PX = 1600

    /** Copies [uri] into private storage. Returns the absolute path, or null if it could not be read. */
    fun import(context: Context, uri: Uri): String? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE_PX || bounds.outHeight / sample > MAX_EDGE_PX) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (bitmap == null) {
            Log.w(TAG, "chosen image could not be decoded")
            null
        } else {
            val target = File(context.filesDir, FILE_NAME)
            FileOutputStream(target).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            bitmap.recycle()
            target.absolutePath
        }
    } catch (e: Exception) {
        Log.w(TAG, "could not import wallpaper", e)
        null
    }

    /** Decodes the stored photo at roughly the size it will be drawn, or null if it is gone. */
    fun load(path: String?, targetWidthPx: Int = 1440): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > targetWidthPx) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) {
            Log.w(TAG, "could not load wallpaper", e)
            null
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
