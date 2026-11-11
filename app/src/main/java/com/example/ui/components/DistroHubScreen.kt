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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.example.engine.LinuxContainer
import com.example.engine.LinuxDesktop
import com.example.engine.LinuxRuntime
import com.example.model.DistroCatalog
import com.example.model.LinuxDesktopEnvironment
import com.example.model.LinuxDistro
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixBlueLight
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.PhonixRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Linux System Hub: Inspect the active distribution or stream & switch to Debian,
 * Ubuntu, Arch, or Kali with automatic debloating of the previous rootfs.
 */
@Composable
fun DistroHubScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val container = remember { LinuxRuntime.container(context) }
    val activeDistroId = remember(uiState.containerStatus, uiState.currentDistro) {
        container.activeDistroId()
    }
    val activeDistro = remember(activeDistroId) {
        container.activeDistro()
    }
    val installedVersion = remember(uiState.containerStatus, activeDistroId) {
        runCatching { container.version() }.getOrDefault(activeDistro.version)
    }

    var pendingSwitchDistro by remember { mutableStateOf<LinuxDistro?>(null) }
    var isSwitching by remember { mutableStateOf(false) }
    var switchProgressMessage by remember { mutableStateOf("") }
    var switchErrorMessage by remember { mutableStateOf<String?>(null) }

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
                text = "Linux System & Distributions",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "PRoot container userspace on Android kernel • Stream & switch distros on demand",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        // Active Distro Hero Card
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
                    Text("ACTIVE LINUX ENVIRONMENT", color = PhonixGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${activeDistro.name} ${installedVersion}",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Text(activeDistro.base, color = PhonixBlueLight, fontSize = 12.sp)
                Text(activeDistro.description, color = Slate300, fontSize = 11.sp, lineHeight = 16.sp)

                val guestPath = container.rootfs.absolutePath
                Text(
                    text = "Rootfs: $guestPath",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Package manager: ${activeDistro.packageManager} • X11 DE: ${uiState.activeDesktopEnvironment.title} • On disk: ${formatMb(uiState.storage.guestBytes)}",
                    color = PhonixCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

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

        if (isSwitching) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PhonixCyan, RoundedCornerShape(10.dp))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Distro Migration in Progress...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(switchProgressMessage, color = PhonixCyan, fontSize = 11.sp)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = PhonixCyan, trackColor = Slate800)
                }
            }
        }

        switchErrorMessage?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1215)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PhonixRed, RoundedCornerShape(10.dp))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Distribution Switch Failed", color = PhonixRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(err, color = Slate200, fontSize = 11.sp, lineHeight = 15.sp)
                    Text(
                        "Your existing ${activeDistro.name} rootfs was kept intact.",
                        color = PhonixAmber,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // X11 Desktop Environment Switcher Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("X11 Desktop Environment (DISPLAY=:1)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Choose which window manager or desktop environment runs inside the Linux Desktop window. Missing packages are installed automatically via ${activeDistro.packageManager} when the X11 session starts.",
                color = Slate400,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinuxDesktopEnvironment.entries.forEach { de ->
                    val isSelected = uiState.activeDesktopEnvironment == de
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF0C3256) else Slate900
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PhonixCyan else Slate800,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.switchDesktopEnvironment(de) }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = de.shortLabel,
                                color = if (isSelected) PhonixCyan else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = de.ramEstimate,
                                color = PhonixAmber,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isSelected) "Active" else "Switch",
                                color = if (isSelected) PhonixGreen else Slate400,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Distro Catalog Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Available Linux Distributions", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Alpine Linux is bundled inside the APK for offline restore. Debian, Ubuntu, Arch, and Kali download their official ARM64 rootfs archives over HTTPS before replacing the previous rootfs.",
                color = Slate400,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        DistroCatalog.distros.forEach { targetDistro ->
            val isCurrent = targetDistro.id == activeDistro.id

            Card(
                colors = CardDefaults.cardColors(containerColor = if (isCurrent) Color(0xFF0F1B2B) else Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isCurrent) PhonixCyan else Slate800, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                targetDistro.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                targetDistro.base,
                                color = Slate400,
                                fontSize = 10.sp
                            )
                            if (targetDistro.badgeText.isNotBlank()) {
                                Text(
                                    targetDistro.badgeText,
                                    color = if (targetDistro.id == "alpine") PhonixGreen else PhonixAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PhonixGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("CURRENT", color = PhonixGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { pendingSwitchDistro = targetDistro },
                                colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                                modifier = Modifier.height(28.dp),
                                enabled = !isSwitching
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Install & Switch", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        targetDistro.description,
                        color = Slate300,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Download: ${targetDistro.downloadSizeEstimate}",
                            color = PhonixAmber,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Disk: ${targetDistro.installedDiskFootprint}",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Pkg: ${targetDistro.packageManager}",
                            color = PhonixCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Architecture Card
        Card(colors = CardDefaults.cardColors(containerColor = Slate900), shape = RoundedCornerShape(10.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How PRoot isolation works", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "proot makes guest distributions believe they are root on their own filesystem via ptrace syscall interception. " +
                    "Nothing on the phone is repartitioned and no bootloader is unlocked. The desktop environment streams into the " +
                    "Phonix window over loopback VNC, seamlessly integrating with Android.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // Confirmation Dialog to switch and debloat previous distro
    pendingSwitchDistro?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!isSwitching) pendingSwitchDistro = null },
            title = {
                Text("Switch from ${activeDistro.name} to ${target.name}?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (target.id == "alpine") {
                            "Switching will unpack the bundled Alpine Linux archive from the APK and replace the current ${activeDistro.name} rootfs."
                        } else {
                            "Switching will download the official ${target.name} ARM64 rootfs archive (${target.downloadSizeEstimate}) over HTTPS, verify it, and then replace the current ${activeDistro.name} rootfs."
                        },
                        color = Slate300,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        "If the download fails or you are offline, your existing ${activeDistro.name} environment is kept untouched.",
                        color = PhonixGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toInstall = target
                        pendingSwitchDistro = null
                        isSwitching = true
                        switchErrorMessage = null
                        switchProgressMessage = "Stopping active Linux services..."
                        scope.launch {
                            viewModel.stopContainer()
                            val result = runCatching {
                                container.switchDistro(
                                    distro = toInstall,
                                    debloatPrevious = true,
                                    onProgress = { progress -> switchProgressMessage = progress }
                                )
                            }
                            result.onSuccess {
                                viewModel.selectDistro(toInstall)
                                viewModel.restartContainer()
                                viewModel.refreshStorage()
                            }.onFailure { err ->
                                switchErrorMessage = err.message ?: "Failed to download or extract ${toInstall.name}."
                                viewModel.restartContainer()
                            }
                            isSwitching = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue)
                ) {
                    Text("Proceed with Switch")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingSwitchDistro = null },
                    enabled = !isSwitching
                ) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900,
            titleContentColor = Color.White
        )
    }
}

private fun formatMb(bytes: Long): String {
    if (bytes <= 0L) return "-"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 1000) String.format(Locale.ROOT, "%.0f MB", mb)
    else String.format(Locale.ROOT, "%.1f GB", mb / 1024.0)
}
