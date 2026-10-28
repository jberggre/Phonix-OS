package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ServerServiceItem
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

@Composable
fun ServerCenterScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uptimeHours = uiState.uptimeSeconds / 3600
    val uptimeMins = (uiState.uptimeSeconds % 3600) / 60
    val uptimeSecs = uiState.uptimeSeconds % 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Server Overview Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(PhonixGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "24/7 Linux Server Engine Active",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0F223D))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${uptimeHours}h ${uptimeMins}m ${uptimeSecs}s",
                            color = PhonixCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Slate800)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Local Network (LAN) Endpoint:", color = Slate400, fontSize = 11.sp)
                        Text(
                            text = "http://${uiState.localIpAddress}:8080",
                            color = PhonixGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Server URL", "http://${uiState.localIpAddress}:8080")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied URL to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", tint = Slate200)
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${uiState.localIpAddress}:8080"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser", tint = PhonixCyan)
                        }
                    }
                }
            }
        }

        // Quick Preset Configurations
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1-Click Server Presets:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // NAS Preset
                    Button(
                        onClick = {
                            viewModel.toggleService("samba_nas")
                            viewModel.toggleService("nginx_web")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C3256)),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("Home NAS", fontSize = 11.sp)
                    }

                    // Dev Stack
                    Button(
                        onClick = {
                            viewModel.toggleService("node_api")
                            viewModel.toggleService("python_flask")
                            viewModel.toggleService("openssh")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("Dev Stack", fontSize = 11.sp)
                    }

                    // Ad-Blocker
                    Button(
                        onClick = { viewModel.toggleService("pihole_dns") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1D54)),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("Ad-Blocker", fontSize = 11.sp)
                    }
                }
            }
        }

        // Services List
        Text(
            text = "Background Server Daemons (${uiState.serverServices.count { it.isRunning }} Active):",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        uiState.serverServices.forEach { service ->
            ServerServiceCard(
                service = service,
                localIp = uiState.localIpAddress,
                onToggle = { viewModel.toggleService(service.id) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ServerServiceCard(
    service: ServerServiceItem,
    localIp: String,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (service.isRunning) Color(0xFF0F1B2B) else Slate900
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (service.isRunning) Color(0x3338BDF8) else Slate800,
                RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (service.isRunning) PhonixGreen else PhonixRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = service.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ":${service.port}",
                        color = PhonixCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = service.isRunning,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PhonixGreen,
                        uncheckedThumbColor = Slate400,
                        uncheckedTrackColor = Slate800
                    )
                )
            }

            Text(
                text = service.description,
                color = Slate300,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            // Connection instruction snippet
            val connectionSnippet = when (service.id) {
                "openssh" -> "ssh phonix@$localIp -p ${service.port}"
                "samba_nas" -> "\\\\$localIp\\shared  (Map in Windows File Explorer)"
                "nginx_web" -> "http://$localIp:${service.port}"
                "pihole_dns" -> "DNS: $localIp:53  |  Admin: http://$localIp:${service.port}/admin"
                else -> "http://$localIp:${service.port}"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Slate950)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = connectionSnippet,
                    color = if (service.isRunning) PhonixCyan else Slate400,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Connection Command", connectionSnippet)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied connection command!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Slate400,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
