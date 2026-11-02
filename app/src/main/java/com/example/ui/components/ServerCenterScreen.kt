package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.ContainerStatus
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel

/**
 * Server suite.
 *
 * Every service used to be listed as running with a switch that only flipped a boolean in the
 * UI. Now the state comes from the guest (does the package exist, is the process alive) and the
 * action installs the real Alpine package. Starting a daemon is left to the user, in the
 * terminal, where they can see what it prints.
 */
@Composable
fun ServerCenterScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { viewModel.refreshServerStates() }
    val ip = uiState.localIpAddress
    val uptimeHours = uiState.uptimeSeconds / 3600
    val uptimeMins = (uiState.uptimeSeconds % 3600) / 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Server Suite", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Packages and processes inside the Alpine guest", color = Slate400, fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.refreshServerStates() },
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Refresh", fontSize = 11.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Slate900), shape = RoundedCornerShape(10.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.containerStatus == ContainerStatus.RUNNING) PhonixGreen else Slate600)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Container: " + when (uiState.containerStatus) {
                            ContainerStatus.RUNNING -> "running"
                            ContainerStatus.STARTING -> "starting"
                            ContainerStatus.RESTARTING -> "restarting"
                            ContainerStatus.STOPPED -> "stopped"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "LAN address: " + if (ip.isBlank()) "no IPv4 interface found" else ip,
                    color = Slate300,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (uiState.uptimeSeconds > 0)
                        "Device uptime: ${uptimeHours}h ${uptimeMins}m"
                    else "Device uptime: reading...",
                    color = Slate400,
                    fontSize = 11.sp
                )
                Text(
                    "The guest shares the phone's network. Anything you start on a port is reachable at " +
                        "the address above from the same LAN, once Android's firewall allows it.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }

        uiState.serverServices.forEach { service ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            service.isRunning -> PhonixGreen
                                            service.isInstalled -> PhonixCyan
                                            else -> Slate600
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(service.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = when {
                                service.isRunning -> "running"
                                service.isInstalled -> "installed"
                                else -> "not installed"
                            },
                            color = when {
                                service.isRunning -> PhonixGreen
                                service.isInstalled -> PhonixCyan
                                else -> Slate400
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "port ${service.port} · ${service.category} · apk package: ${service.apkPackage}",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(service.description, color = Slate300, fontSize = 11.sp, lineHeight = 15.sp)

                    // Supervision is opt-in and its state is probe-derived: the line says what was
                    // observed, and tapping it starts or stops the restart policy.
                    val supervisionLine = uiState.supervision[service.id]
                    Text(
                        text = supervisionLine?.let { "$it - tap to stop supervising" }
                            ?: "Not supervised - tap to keep it running",
                        color = if (supervisionLine == null) Slate400 else PhonixCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            viewModel.toggleServiceSupervision(service.id)
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.toggleServerService(service.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (service.isInstalled) Slate800 else PhonixAmber
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (service.isInstalled) "Show start command" else "Install package",
                                color = if (service.isInstalled) Color.White else Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { viewModel.toggleWindow(WindowType.TERMINAL) },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Terminal", fontSize = 11.sp)
                        }
                    }

                    if (service.isInstalled) {
                        Text(
                            text = service.startHint,
                            color = PhonixCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B08)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PhonixAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Some servers cannot run here", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Pi-hole (DNS port 53) and SMB file sharing (port 445) are missing from this list " +
                        "on purpose. Both need a privileged port below 1024, and Android does not let an " +
                        "app - even one running this container - bind those without root. Installing " +
                        "the package would look like it worked and then fail to listen. The Privacy Hub " +
                        "walks through running Pi-hole on a spare Pi or old PC instead.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Button(
                    onClick = { viewModel.toggleWindow(WindowType.PRIVACY) },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Open Privacy Hub", fontSize = 11.sp)
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
                    Icon(Icons.Default.Info, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Why install instead of toggle", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "There is no service manager in this container: proot has no init, so a daemon is " +
                        "just a process you start. Installing the package here runs the real apk command; " +
                        "starting it is yours to do, and the state shown above comes from actually finding " +
                        "the process.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
