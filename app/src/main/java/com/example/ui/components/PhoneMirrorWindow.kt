package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.Display
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ScreenMirror
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

/**
 * Phone Screen, restored as a real view rather than the old painted mock.
 *
 * The window asks Android for capture consent, then shows the actual default display. It is
 * read-only on purpose: an ordinary app cannot inject touches into another app without an
 * AccessibilityService. The copy says that instead of pretending taps work.
 */
@Composable
fun PhoneMirrorWindowContent() {
    val context = LocalContext.current
    val projectionManager = remember(context) {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }
    val running by ScreenMirror.running.collectAsState()
    val frame by ScreenMirror.frame.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }

    // The capture always mirrors the phone panel (display 0). When the desktop itself is hosted
    // there, the feed can only ever contain Phonix - and then this window inside Phonix - so the
    // honest thing is to say that instead of drawing a recursive screenshot.
    val hostDisplayId = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { (context as? Activity)?.display?.displayId }.getOrNull()
                ?: Display.DEFAULT_DISPLAY
        } else {
            Display.DEFAULT_DISPLAY
        }
    }
    val desktopIsOnPhonePanel = hostDisplayId == Display.DEFAULT_DISPLAY

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode != Activity.RESULT_OK || data == null) {
            error = "Android did not grant screen-capture consent. The mirror stays off."
            return@rememberLauncherForActivityResult
        }
        val started = ScreenMirror.start(context, result.resultCode, data)
        if (!started) {
            error = "The capture service could not start. Check notification permission and try again."
        } else {
            error = null
        }
    }

    // Closing the window stops the foreground capture. A hidden service that keeps recording
    // the screen would be a privacy problem, not a convenience.
    DisposableEffect(Unit) {
        onDispose { ScreenMirror.stop(context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = PhonixCyan,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Phone Screen", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (desktopIsOnPhonePanel) {
                        "The phone display is running Phonix"
                    } else {
                        "Live photo of the phone panel - read-only"
                    },
                    color = Slate400,
                    fontSize = 8.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (running) Color(0xFF064E3B) else Color(0xFF1E293B),
                border = BorderStroke(
                    1.dp,
                    if (running) Color(0xFF10B981) else Slate600
                )
            ) {
                Text(
                    if (running) "LIVE" else "OFF",
                    color = if (running) Color(0xFFD1FAE5) else Slate300,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if (running) {
                Text(
                    "Stop",
                    color = Color(0xFFFCA5A5),
                    fontSize = 9.sp,
                    modifier = Modifier
                        .clickable { ScreenMirror.stop(context) }
                        .padding(horizontal = 4.dp, vertical = 3.dp)
                )
            }
        }

        if (desktopIsOnPhonePanel) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        "The phone screen is the desktop right now",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Android is showing Phonix on this display, so capturing it can only return " +
                            "Phonix again - the recursive picture this window used to draw.",
                        color = Slate300,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    Text(
                        "Run the desktop on an external display (DeX or a USB-C monitor) and this " +
                            "window shows the phone's real screen while you work on the monitor.",
                        color = Slate400,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        } else if (!running) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        "See the real screen, even when the panel is cracked",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Android will show its own screen-capture consent dialog. After you allow it, " +
                            "this window receives the actual display image. Nothing is painted or guessed.",
                        color = Slate300,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    Text(
                        "It captures the phone's own panel, not this desktop - so the picture stays the " +
                            "cracked screen even while you work on a monitor.",
                        color = PhonixCyan,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    Text(
                        "It cannot tap or type on the phone. That needs an AccessibilityService, which is " +
                            "a separate opt-in; this window does not pretend to have it.",
                        color = Slate400,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    Button(
                        onClick = {
                            // From API 34 the consent dialog can offer "a single app" or another
                            // display; Phonix wants the built-in panel, so it asks the system for
                            // exactly that. On older builds the service binds the projection to
                            // display 0 by requesting it from that display's context.
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                projectionManager?.createScreenCaptureIntent(
                                    android.media.projection.MediaProjectionConfig
                                        .createConfigForDefaultDisplay()
                                )
                            } else {
                                projectionManager?.createScreenCaptureIntent()
                            }
                            if (intent == null) {
                                error = "This Android build does not expose screen capture."
                            } else {
                                error = null
                                consentLauncher.launch(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E7490)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Start read-only mirror", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    error?.let {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(it, color = Color(0xFFFCA5A5), fontSize = 9.sp, lineHeight = 12.sp)
                        }
                    }
                    Text(
                        "Headless setup: approve the system dialog once with DeX, a USB mouse, or " +
                            "`adb shell input tap <x> <y>`. Consent is per capture session and does not " +
                            "survive a reboot.",
                        color = Slate500,
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black, RoundedCornerShape(6.dp))
                    .border(1.dp, Slate700, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (frame == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = PhonixAmber,
                            strokeWidth = 2.dp
                        )
                        Text("Waiting for the first captured frame...", color = Slate300, fontSize = 10.sp)
                    }
                } else {
                    Image(
                        bitmap = frame!!.asImageBitmap(),
                        contentDescription = "Live read-only view of the phone display",
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Text(
                "This is the phone display itself, not a mock, and it stays read-only.",
                color = Slate400,
                fontSize = 8.sp,
                lineHeight = 11.sp
            )
        }
    }
}
