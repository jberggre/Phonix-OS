package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.LinuxDesktop
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate950
import com.example.viewmodel.PhonixViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A real Linux desktop, rendered from the container into a window.
 *
 * The page is our own noVNC viewer on localhost ([com.example.engine.LinuxDesktop] copies it
 * into the guest); the WebView is just a screen and a keyboard. First open installs X11
 * (~450 MB) inside the guest, so the window shows progress instead of a blank rectangle while
 * that happens.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LinuxDesktopWindowContent(viewModel: PhonixViewModel) {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(false) }
    var reconnectToken by remember { mutableIntStateOf(0) }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.rgb(11, 18, 32))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            // The viewer page carries its own viewport meta tag. Without this WebView ignores
            // it and lays the page out at Chromium's 980px fallback, which makes noVNC size the
            // canvas to 980px inside a narrower window - the framebuffer is then cropped
            // instead of scaled. Zooming is disabled so the desktop cannot drift.
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            // The page logs its connection state and window size; this is the only way to see
            // inside a WebView from adb (logcat tag: PhonixVnc).
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.d("PhonixVnc", message.message())
                    return true
                }
            }
        }
    }

    DisposableEffect(webView) {
        LinuxDesktop.ensureStarted(context)
        onDispose { webView.destroy() }
    }

    // "Page loaded" is not "VNC connected": the page reports the real state itself and
    // reconnects on its own. This loop mirrors that state, and only touches the guest when the
    // page says it cannot connect (or as an occasional health check) - re-running the launcher
    // script every few seconds for no reason is work the phone does not need to do.
    LaunchedEffect(reconnectToken) {
        connected = false
        LinuxDesktop.ensureStarted(context)
        delay(300)
        webView.loadUrl(LinuxDesktop.VNC_URL)

        var lastRepair = android.os.SystemClock.elapsedRealtime()
        while (true) {
            delay(2_000)
            connected = evaluate(webView, VNC_STATE_JS)?.contains("connected") == true
            val now = android.os.SystemClock.elapsedRealtime()
            val sinceRepair = now - lastRepair
            val repairDue = if (connected) sinceRepair > 120_000 else sinceRepair > 10_000
            if (repairDue) {
                LinuxDesktop.ensureStarted(context)
                lastRepair = now
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Slate950)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (connected) PhonixGreen else PhonixAmber)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (connected) "Linux desktop — display :1, 1280×720" else "Starting X11 on :1…",
                    color = Slate200,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        reconnectToken++
                        LinuxDesktop.restart(context)
                    },
                    modifier = Modifier.height(24.dp),
                    // The default content colour is the theme's primary, which is close to the
                    // header background and left the label almost unreadable.
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = Slate200
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reconnect", fontSize = 10.sp)
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )

            if (!connected) {
                Column(
                    modifier = Modifier.fillMaxSize().background(Slate950),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = PhonixCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Starting the Linux desktop",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "First run installs X11, VNC and noVNC inside the container.\n" +
                            "Watch the Terminal window for progress.",
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/** Reads the state the viewer page publishes; returns a JSON string, e.g. `"connected"`. */
private const val VNC_STATE_JS =
    "(function(){try{return (window.__phonix && window.__phonix.state) || 'loading';}" +
        "catch(e){return 'error';}})()"

private suspend fun evaluate(web: WebView, script: String): String? =
    suspendCancellableCoroutine { continuation ->
        web.evaluateJavascript(script) { value -> continuation.resume(value) }
    }
