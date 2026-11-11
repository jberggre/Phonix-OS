package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.collectAsState
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
    val uiState by viewModel.uiState.collectAsState()
    var connected by remember { mutableStateOf(false) }
    var reconnectToken by remember { mutableIntStateOf(0) }
    var switchingDeBanner by remember { mutableStateOf<String?>(null) }

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
            // A load issued before websockify is listening fails with connection refused, and a
            // page that never loaded can never report itself connected, so the splash used to stay
            // up forever on a first run. Log the failure instead of hiding it.
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    Log.d("PhonixVnc", "load error ${error.errorCode} ${error.description} ${request.url}")
                }
            }
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

    LaunchedEffect(switchingDeBanner) {
        if (switchingDeBanner != null) {
            delay(5_000)
            switchingDeBanner = null
        }
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
        var lastLoad = lastRepair
        while (true) {
            delay(2_000)
            connected = evaluate(webView, VNC_STATE_JS)?.contains("connected") == true
            val now = android.os.SystemClock.elapsedRealtime()
            // Until the page is actually showing a framebuffer, keep asking for it: the first
            // run spends minutes installing X11, and the single load below happens before that.
            if (!connected && now - lastLoad > 8_000) {
                webView.loadUrl(LinuxDesktop.VNC_URL)
                lastLoad = now
            }
            val sinceRepair = now - lastRepair
            val repairDue = if (connected) sinceRepair > 120_000 else sinceRepair > 12_000
            if (repairDue && switchingDeBanner == null) {
                LinuxDesktop.ensureStarted(context)
                lastRepair = now
            }
        }
    }

    val activeDe = uiState.activeDesktopEnvironment

    Column(modifier = Modifier.fillMaxSize().background(Slate950)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (connected) PhonixGreen else PhonixAmber)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        switchingDeBanner != null -> switchingDeBanner!!
                        connected -> "X11 (${activeDe.title}) — :1 (1280×720)"
                        else -> "Starting ${activeDe.title} on :1…"
                    },
                    color = if (switchingDeBanner != null) PhonixCyan else Slate200,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Button(
                    onClick = {
                        LinuxDesktop.launchGuiApp(
                            context,
                            "xterm -fa 'DejaVu Sans Mono' -fs 10 -bg '#0B1220' -fg '#DBEAFE' -cr '#F59E0B'"
                        )
                    },
                    modifier = Modifier.height(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = PhonixCyan
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Text("+ xterm", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        LinuxDesktop.launchGuiApp(
                            context,
                            "sh -c \"thunar /root 2>/dev/null || pcmanfm-qt /root 2>/dev/null || pcmanfm /root 2>/dev/null || xterm -fa 'DejaVu Sans Mono' -fs 10 -bg '#0B1220' -fg '#DBEAFE' -e sh -c 'ls -la /root; exec /bin/sh'\""
                        )
                    },
                    modifier = Modifier.height(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = Slate200
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Text("+ Files", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        switchingDeBanner = "Restoring ${activeDe.shortLabel} taskbar & window manager…"
                        LinuxDesktop.restart(context)
                    },
                    modifier = Modifier.height(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = PhonixAmber
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Restore WM/Bar", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        LinuxDesktop.restart(context)
                        reconnectToken++
                    },
                    modifier = Modifier.height(24.dp),
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

        // X11 Desktop Environment Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B1220))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "DE:",
                color = Slate400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            com.example.model.LinuxDesktopEnvironment.entries.forEach { de ->
                val selected = activeDe == de
                Button(
                    onClick = {
                        if (activeDe != de) {
                            switchingDeBanner = "Switching to ${de.title} on :1 (keeping windows & VNC live)…"
                            viewModel.switchDesktopEnvironment(de)
                            if (!connected) {
                                reconnectToken++
                            }
                        } else {
                            // Tapping the already-selected DE re-asserts its WM & taskbar in place
                            switchingDeBanner = "Refreshing ${de.title} window manager & panel…"
                            LinuxDesktop.restart(context)
                        }
                    },
                    modifier = Modifier.height(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) PhonixCyan.copy(alpha = 0.22f) else Slate800.copy(alpha = 0.65f),
                        contentColor = if (selected) PhonixCyan else Slate200
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = de.id.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
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
                        text = "Starting ${activeDe.title} on X11 (:1)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "First run installs ${activeDe.title}, VNC and noVNC inside the container.\n" +
                            "Watch the Terminal window for live package progress.",
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
