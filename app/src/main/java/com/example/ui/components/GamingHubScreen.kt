package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixBlueLight
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel

/**
 * Games, honestly.
 *
 * The previous version listed Fallout: New Vegas, Half-Life 2 and friends with invented frame
 * rates ("Perfect 55-60 FPS") and a launch button that printed fake VirGL logs. None of that
 * was measured, packaged or tested, so this screen now says what is actually true: Android
 * games run natively today, and Linux gaming is whatever you install and verify yourself.
 */
@Composable
fun GamingHubScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Games",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "What runs on this machine today, and what has not been tested",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PhonixGreen.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Android games — works now", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Android games run on the phone's own GPU, exactly as they do on the phone. Install " +
                        "them from Google Play, then open them here or from the Android launcher; on a " +
                        "monitor, DeX opens them in a window. Performance is whatever the handset gives " +
                        "you — Phonix does not change it, and no frame rate is promised.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Button(
                    onClick = { viewModel.openAppStore(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Open Google Play", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Slate900), shape = RoundedCornerShape(10.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Linux games — unproven on this unit", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Alpine ships no game runtime. Emulation layers for Windows/x86 titles (Box64, Wine, " +
                        "DXVK) and even native ARM ports need packages that are not bundled, and none of " +
                        "them have been measured on this phone. Rather than publish a compatibility list " +
                        "that means nothing, Phonix leaves it to you:",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "apk add <package>\napk search <name>",
                    color = PhonixCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "If a title's port exists for aarch64 musl and you get it running, it will run in the " +
                        "Linux desktop like any other X11 application — and it will be your measurement, " +
                        "not ours.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Button(
                    onClick = { viewModel.toggleWindow(WindowType.TERMINAL) },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Open terminal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate700, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verifying a unit", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "For a refurbished build, the useful checks are: a game from Play Store at playable " +
                        "frame rate, the Linux desktop opening and typing cleanly, and the storage benchmark " +
                        "in the Benchmark window returning numbers instead of errors. All three exercise " +
                        "real hardware.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Button(
                    onClick = { viewModel.toggleWindow(WindowType.BENCHMARK) },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Open benchmark", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
