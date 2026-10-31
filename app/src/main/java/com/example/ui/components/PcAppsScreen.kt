package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LinuxAppEntry
import com.example.model.LinuxAppStatus
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.ContainerStatus
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel

/**
 * PC software for the Linux guest.
 *
 * This is the honest answer to "can I download PC apps": yes, as native ARM64 Linux packages
 * from Alpine's repositories, installed into the proot guest and run on the X11 desktop. No, for
 * x86-64 Windows installers — the guest is aarch64 and has no emulation layer installed.
 *
 * The install button runs the live guest's own package manager and prints its output. "Installed" is only
 * shown after the package manager confirms it, and the download size is the real compressed size
 * from the Alpine 3.24 index.
 */
@Composable
fun PcAppsScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { viewModel.refreshLinuxApps() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("PC apps", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Desktop Linux programs, installed into this machine and opened on its Linux desktop",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PhonixCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("What these are", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Native ARM64 Linux builds from Alpine 3.24, installed with apk inside the " +
                        "container and shown on the Linux desktop window. They share the phone's CPU and " +
                        "use software rendering.",
                    color = Slate300,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text(
                    text = "x86-64 Windows .exe programs are not supported: the guest is aarch64 and has " +
                        "no Windows or x86 emulation layer installed. Sizes below are the real compressed " +
                        "download from Alpine's index.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }

        if (uiState.containerStatus != ContainerStatus.RUNNING) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "The Linux container is stopped; installs need it running.",
                    color = PhonixAmber,
                    fontSize = 11.sp
                )
                Button(
                    onClick = { viewModel.toggleContainerPower() },
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start container", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.refreshLinuxApps() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Slate300, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Check installed", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (uiState.linuxAppsProbing) {
                CircularProgressIndicator(color = PhonixCyan, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            }
            OutlinedButton(
                onClick = { viewModel.toggleWindow(WindowType.TERMINAL) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = Slate300, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Terminal", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        uiState.linuxApps.forEach { status ->
            PcAppCard(
                status = status,
                busy = uiState.linuxAppBusyPackage == status.entry.packageName,
                anyBusy = uiState.linuxAppBusyPackage != null,
                onInstall = { viewModel.installLinuxApp(status.entry) }
            )
        }

        if (uiState.linuxAppLog.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("package manager output", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    uiState.linuxAppLog.forEach { line ->
                        Text(
                            text = line,
                            color = if (line.contains("ERROR", true) || line.contains("unable", true)) {
                                PhonixAmber
                            } else {
                                Slate300
                            },
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PcAppCard(
    status: LinuxAppStatus,
    busy: Boolean,
    anyBusy: Boolean,
    onInstall: () -> Unit
) {
    val entry: LinuxAppEntry = status.entry
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (status.installed == true) PhonixGreen.copy(alpha = 0.35f) else Slate800, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(entry.category, color = PhonixCyan, fontSize = 9.sp)
                }
                Text(entry.blurb, color = Slate300, fontSize = 10.sp, lineHeight = 14.sp)
                Text(
                    text = "${entry.packageName} • ${entry.downloadMb} MB download",
                    color = Slate400,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.width(10.dp))
            when (status.installed) {
                true -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Installed", color = PhonixGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                else -> Button(
                    onClick = onInstall,
                    enabled = !anyBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = Slate300
                    ),
                    shape = RoundedCornerShape(8.dp),
                    // A 30dp button with Material's default 8dp vertical padding clips its label
                    // at this font size, so the padding is set explicitly.
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(color = PhonixCyan, strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Installing", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Slate300, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Install", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
