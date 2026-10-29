package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LinuxRuntime
import com.example.model.WindowType
import com.example.model.DistroCatalog
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
import java.util.Locale

/**
 * The Linux screen: what is installed, where it lives, and how to open it.
 *
 * It used to be a storefront for eight distributions with invented download sizes, download
 * speeds and "pre-configured" package lists, and a button that faked a boot. Only Alpine is
 * really here, so only Alpine is shown.
 */
@Composable
fun DistroHubScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val distro = remember { DistroCatalog.alpine }
    val installedVersion = remember(uiState.containerStatus) {
        runCatching { LinuxRuntime.container(context).version() }.getOrDefault(distro.version)
    }

    LaunchedEffect(Unit) { viewModel.refreshStorage() }

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
                text = "Linux System",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "One distribution, running for real inside the app",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PhonixGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Installed", color = PhonixGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${distro.name} ${installedVersion}",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Text(distro.base, color = PhonixBlueLight, fontSize = 12.sp)
                Text(distro.description, color = Slate300, fontSize = 11.sp, lineHeight = 16.sp)

                if (uiState.storage.guestPath.isNotEmpty()) {
                    Text(
                        text = "Rootfs: ${uiState.storage.guestPath}",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "On disk: ${formatMb(uiState.storage.guestBytes)}",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.toggleWindow(WindowType.LINUX_DESKTOP) },
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open desktop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.toggleWindow(WindowType.TERMINAL) },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terminal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Slate900), shape = RoundedCornerShape(10.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How it runs", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "proot makes the guest believe it is root on its own filesystem. Nothing on the phone " +
                        "is repartitioned and no bootloader is touched, so Android and its apps keep working " +
                        "underneath. The desktop is IceWM on a virtual X11 display, streamed into the window " +
                        "over VNC on loopback.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "Bundled in the rootfs: " + distro.bundledPackages.joinToString(", "),
                    color = Slate400,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
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
                    Text("Other distributions", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Ubuntu, Debian, Arch, Kali, Fedora, DietPi and Pi-hole images are not packaged for " +
                        "Phonix yet. They are a lot of work each - a tested rootfs, a working package " +
                        "manager, a display stack - and shipping a download button before that exists would " +
                        "only produce a broken system. This list will grow when the images are real.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "Packages can be installed inside the current guest: apk add <name>",
                    color = PhonixCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatMb(bytes: Long): String {
    if (bytes <= 0L) return "-"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 1000) String.format(Locale.ROOT, "%.0f MB", mb)
    else String.format(Locale.ROOT, "%.1f GB", mb / 1024.0)
}
