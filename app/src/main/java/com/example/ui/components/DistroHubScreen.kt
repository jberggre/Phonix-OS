package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixBlueLight
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel

@Composable
fun DistroHubScreen(
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
        // Header
        Column {
            Text(
                text = "Linux Distro Hub & Package Library",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Download, install, and switch between 8 official ARM64 Linux distributions",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        // Active Distro Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Subsystem", color = Slate400, fontSize = 11.sp)
                        Text(uiState.currentDistro.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF065F46))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("ACTIVE ROOTFS", color = Color(0xFF6EE7B7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text("Base: ${uiState.currentDistro.base}", color = PhonixBlueLight, fontSize = 12.sp)
                Text("Desktop: ${uiState.currentDistro.desktopEnvironment}", color = Slate300, fontSize = 11.sp)
            }
        }

        // Distro Catalog
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Linux Distros (8 Total):",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ARM64 Pre-built",
                color = PhonixCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        uiState.availableDistros.forEach { distro ->
            val isCurrent = uiState.currentDistro.id == distro.id
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) Color(0xFF0F1B2B) else Slate900
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isCurrent) PhonixCyan else Slate800,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(distro.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(distro.base, color = PhonixBlueLight, fontSize = 11.sp)
                        }

                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PhonixGreen)
                                    .size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        } else if (distro.isInstalled) {
                            Button(
                                onClick = {
                                    viewModel.switchAndBootDistro(distro)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PhonixGreen),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Boot This OS", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (distro.isDownloading) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PhonixCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("DOWNLOADING", color = PhonixCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.downloadDistro(distro.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download (${distro.downloadSizeMb}M)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(distro.description, color = Slate300, fontSize = 11.sp, lineHeight = 15.sp)

                    // Download Progress Bar
                    if (distro.isDownloading) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fetching ARM64 RootFS archive...", color = PhonixCyan, fontSize = 10.sp)
                            Text("${(distro.downloadProgress * 100).toInt()}% • ${distro.downloadSpeedMbps.toInt()} MB/s", color = PhonixCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { distro.downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = PhonixCyan,
                            trackColor = Slate800
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("RAM req: ${distro.minRamMb} MB", color = Slate400, fontSize = 11.sp)
                        Text("Disk req: ${distro.diskReqGb} GB", color = Slate400, fontSize = 11.sp)
                        Text("Size: ${distro.downloadSizeMb} MB", color = PhonixAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Packages: ${distro.preinstalledPackages.size} core", color = PhonixCyan, fontSize = 11.sp)
                    }
                }
            }
        }

        // Backup & Export Disk Image
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup & Export EXT4 Image:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Save your entire configured Linux server rootfs container (including installed Docker, Nginx configs, and files) to an external MicroSD card or USB OTG drive.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Exporting /data/rootfs.img to /sdcard/Phonix_Backup.img.gz...", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Rootfs Image (.img.gz)")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
