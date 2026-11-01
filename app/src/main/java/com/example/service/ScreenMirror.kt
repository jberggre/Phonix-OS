package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide bridge between the capture service and the Phone Screen window.
 *
 * The frame is a real MediaProjection bitmap. There is no painted phone UI and no input
 * injection: this is deliberately a read-only view, because Android does not let an ordinary
 * app deliver touches to another app's window without an AccessibilityService.
 */
object ScreenMirror {

    private val _frame = MutableStateFlow<Bitmap?>(null)
    val frame: StateFlow<Bitmap?> = _frame.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** Starts after the system capture-consent activity returned RESULT_OK. */
    fun start(context: Context, resultCode: Int, data: Intent): Boolean {
        val intent = Intent(context, ScreenMirrorService::class.java)
            .putExtra(ScreenMirrorService.EXTRA_RESULT_CODE, resultCode)
            .putExtra(ScreenMirrorService.EXTRA_RESULT_DATA, data)
        return try {
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (e: Throwable) {
            Log.w(TAG, "could not start screen capture service", e)
            false
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, ScreenMirrorService::class.java))
        clear()
        setRunning(false)
    }

    internal fun publish(bitmap: Bitmap) {
        _frame.value = bitmap
    }

    internal fun clear() {
        _frame.value = null
    }

    internal fun setRunning(value: Boolean) {
        _running.value = value
    }

    /**
     * Surface size for a panel of [width]x[height] pixels.
     *
     * Pure so the arithmetic is testable. The defect this replaces used the metrics of whichever
     * display the app was on: with the desktop on a 1920x1080 monitor it produced a 16:9 540x303
     * surface for a 1080x2280 phone panel, so the mirrored picture had the monitor's shape.
     */
    fun captureSize(width: Int, height: Int, targetWidth: Int = CAPTURE_WIDTH): Pair<Int, Int> {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val outWidth = targetWidth.coerceAtLeast(1)
        val outHeight = (h.toFloat() * outWidth / w).toInt().coerceAtLeast(1)
        return outWidth to outHeight
    }

    /** Width the phone panel is scaled to; its height follows the panel's own aspect ratio. */
    const val CAPTURE_WIDTH = 540

    private const val TAG = "PhonixScreenMirror"
}

class ScreenMirrorService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var captureWidth = 0
    private var captureHeight = 0
    private var lastFrameAt = 0L
    private var loggedFirstFrame = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection stopped by the system or the user")
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        handlerThread = HandlerThread("phonix-screen-mirror").apply { start() }
        handler = Handler(handlerThread!!.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (projection != null) return START_NOT_STICKY

        // Android 14+ requires the mediaProjection foreground service to start before the
        // projection token is used. Doing it here is also what keeps capture alive while the
        // Phonix window is not focused.
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "could not enter the mediaProjection foreground state", e)
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data = getProjectionData(intent)
        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "no capture consent token")
            stopSelf()
            return START_NOT_STICKY
        }

        // Ask for the projection from a context that belongs to the phone panel. On Samsung DeX
        // the framework binds a capture to the display of the requesting context, which is why a
        // projection requested from the desktop returned the desktop. `MediaProjectionConfig`
        // states the target display explicitly, but it only exists from API 34, so on this API 31
        // unit the display-0 context is the lever. Measured, not assumed: see the log line below.
        val mediaProjection = try {
            projectionManagerFor(phonePanel())?.getMediaProjection(resultCode, data)
        } catch (e: Throwable) {
            Log.w(TAG, "getMediaProjection failed", e)
            null
        }
        if (mediaProjection == null) {
            Log.w(TAG, "the system did not return a MediaProjection")
            stopSelf()
            return START_NOT_STICKY
        }

        projection = mediaProjection
        mediaProjection.registerCallback(projectionCallback, handler)
        startCapture()
        ScreenMirror.setRunning(true)
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun getProjectionData(intent: Intent?): Intent? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
    }

    /** The built-in panel (display 0) as the system reports it, independent of this app's display. */
    private fun phonePanel(): Display? = try {
        getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
    } catch (_: Throwable) {
        null
    }

    /** A MediaProjectionManager reached through a display-0 context, when one is available. */
    private fun projectionManagerFor(display: Display?): MediaProjectionManager? {
        val context = if (display != null) createDisplayContext(display) else this
        return context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    private fun startCapture() {
        val mediaProjection = projection ?: return
        releaseVirtualDisplay()

        // Measure the PHONE panel, not whichever display this app happens to be on. Measured on
        // the SM-G973U with DeX on its HDMI monitor (2026-11-01): the service's
        // WindowManager.defaultDisplay answered the 1920x1080 desktop, so the old code built a
        // 16:9 540x303 surface and the window showed the desktop instead of the phone screen.
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        phonePanel()?.getRealMetrics(metrics)
        if (metrics.widthPixels <= 0 || metrics.heightPixels <= 0) {
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                .defaultDisplay.getRealMetrics(metrics)
        }
        val sourceWidth = metrics.widthPixels.coerceAtLeast(1)
        val sourceHeight = metrics.heightPixels.coerceAtLeast(1)
        val size = ScreenMirror.captureSize(sourceWidth, sourceHeight)
        captureWidth = size.first
        captureHeight = size.second

        val reader = ImageReader.newInstance(
            captureWidth,
            captureHeight,
            PixelFormat.RGBA_8888,
            2
        )
        reader.setOnImageAvailableListener({ source -> onImageAvailable(source) }, handler)
        imageReader = reader
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "phonix-phone-screen",
            captureWidth,
            captureHeight,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )
        Log.i(
            TAG,
            "panel ${sourceWidth}x$sourceHeight @${metrics.densityDpi}dpi -> " +
                "capturing ${captureWidth}x$captureHeight"
        )
    }

    private fun onImageAvailable(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (e: Throwable) {
            Log.w(TAG, "acquireLatestImage failed", e)
            null
        } ?: return

        try {
            val now = SystemClock.elapsedRealtime()
            // Screen capture produces frames at display rate. A read-only status view does not
            // need 60 fps, and throttling keeps CPU and allocation pressure down on the S10.
            if (now - lastFrameAt < FRAME_INTERVAL_MS) return
            lastFrameAt = now

            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride.coerceAtLeast(1)
            val rowStride = plane.rowStride
            val rowPadding = (rowStride - pixelStride * captureWidth).coerceAtLeast(0)
            val paddedWidth = captureWidth + rowPadding / pixelStride
            val raw = Bitmap.createBitmap(paddedWidth, captureHeight, Bitmap.Config.ARGB_8888)
            raw.copyPixelsFromBuffer(buffer)
            val frame = if (paddedWidth == captureWidth) {
                raw
            } else {
                Bitmap.createBitmap(raw, 0, 0, captureWidth, captureHeight).also { raw.recycle() }
            }
            if (!loggedFirstFrame) {
                loggedFirstFrame = true
                Log.i(TAG, "first captured frame ${frame.width}x${frame.height}")
            }
            ScreenMirror.publish(frame)
        } catch (e: Throwable) {
            Log.w(TAG, "could not convert a captured frame", e)
        } finally {
            image.close()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // The capture surface does not follow rotation by itself; rebuild it with the new
        // display metrics while keeping the same MediaProjection token.
        handler?.post { startCapture() }
    }

    override fun onDestroy() {
        ScreenMirror.setRunning(false)
        ScreenMirror.clear()
        releaseVirtualDisplay()
        try {
            projection?.unregisterCallback(projectionCallback)
            projection?.stop()
        } catch (_: Throwable) {
        }
        projection = null
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
        super.onDestroy()
    }

    private fun releaseVirtualDisplay() {
        try {
            virtualDisplay?.release()
        } catch (_: Throwable) {
        }
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Throwable) {
        }
        imageReader = null
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Phone screen mirrored")
            .setContentText("Live, read-only capture of this device's display")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Phone screen mirror",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that the phone display is being captured for the Phonix window"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "com.example.service.EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "com.example.service.EXTRA_RESULT_DATA"
        const val ACTION_STOP = "com.example.service.STOP_SCREEN_MIRROR"

        private const val TAG = "PhonixScreenMirror"
        private const val CHANNEL_ID = "phonix_screen_mirror"
        private const val NOTIFICATION_ID = 4711
        private const val FRAME_INTERVAL_MS = 300L
    }
}
