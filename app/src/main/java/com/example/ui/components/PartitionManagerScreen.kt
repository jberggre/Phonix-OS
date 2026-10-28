package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun PartitionManagerScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val part = uiState.partitionConfig
    var isOverlayFsEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Storage & OS Partition Manager",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Virtual EXT4 container & zRAM swap allocation",
                    color = Slate400,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { viewModel.openInstallationWizard() },
                colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("launch_wizard_button")
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Setup Wizard", fontSize = 12.sp)
            }
        }

        // Direct PC Boot / Default Launcher Card (User Request!)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F223D)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Boot Directly to Phonix (Genuine PC Experience)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Set Phonix as your phone's default Home App. Whenever you power on or unlock the phone, it boots straight into your custom desktop environment—turning your old phone into a dedicated Mini PC appliance.",
                    color = Slate200,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(fallbackIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as Default Home / PC Boot Screen", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Partition Visual Breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Internal Physical Flash: ${part.totalDeviceStorageGb.toInt()} GB",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Colored Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    val androidWeight = (part.androidSystemUsedGb / part.totalDeviceStorageGb).coerceAtLeast(0.1f)
                    val linuxWeight = (part.allocatedLinuxGb / part.totalDeviceStorageGb).coerceAtLeast(0.1f)
                    val swapWeight = (part.allocatedSwapGb / part.totalDeviceStorageGb).coerceAtLeast(0.04f)
                    val freeWeight = (part.freeStorageGb / part.totalDeviceStorageGb).coerceAtLeast(0.05f)

                    Box(
                        modifier = Modifier
                            .weight(androidWeight)
                            .fillMaxHeight()
                            .background(Slate700),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Android OS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(linuxWeight)
                            .fillMaxHeight()
                            .background(PhonixBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Linux EXT4", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(swapWeight)
                            .fillMaxHeight()
                            .background(Color(0xFFFBBF24)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("zRAM", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(freeWeight)
                            .fillMaxHeight()
                            .background(PhonixGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Free", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Android System: ${part.androidSystemUsedGb} GB", color = Slate400, fontSize = 11.sp)
                    Text("Linux EXT4: ${part.allocatedLinuxGb.toInt()} GB", color = PhonixBlueLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Swap: ${part.allocatedSwapGb.toInt()} GB", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Free: ${part.freeStorageGb.toInt()} GB", color = PhonixGreen, fontSize = 11.sp)
                }
            }
        }

        // Sliders for Partition Config
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Adjust Partition Allocation:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                // Linux size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Linux EXT4 Container Size:", color = Slate200, fontSize = 12.sp)
                        Text("${part.allocatedLinuxGb.toInt()} GB", color = PhonixCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = part.allocatedLinuxGb,
                        onValueChange = { viewModel.updatePartitionSizes(it, part.allocatedSwapGb) },
                        valueRange = 4f..32f,
                        steps = 6,
                        colors = SliderDefaults.colors(thumbColor = PhonixCyan, activeTrackColor = PhonixBlue)
                    )
                }

                // Swap size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("zRAM Compressed Swap Size:", color = Slate200, fontSize = 12.sp)
                        Text("${part.allocatedSwapGb.toInt()} GB", color = Color(0xFFFBBF24), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = part.allocatedSwapGb,
                        onValueChange = { viewModel.updatePartitionSizes(part.allocatedLinuxGb, it) },
                        valueRange = 1f..4f,
                        steps = 2,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFBBF24), activeTrackColor = Color(0xFFD97706))
                    )
                }

                // Flash Wear Flags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Flash Wear Reduction (noatime, barrier=0)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Minimizes flash writes to protect older phone eMMC/UFS memory chips", color = Slate400, fontSize = 10.sp)
                    }
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(20.dp))
                }

                // Re-format / Apply Button
                Button(
                    onClick = { viewModel.runPartitioningRoutine() },
                    enabled = !uiState.isPartitioningActive,
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isPartitioningActive) "Formatting & Partitioning..." else "Apply & Format EXT4 Container")
                }

                if (uiState.isPartitioningActive) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(uiState.partitioningStatusText, color = PhonixCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        LinearProgressIndicator(
                            progress = { uiState.partitioningProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = PhonixCyan,
                            trackColor = Slate800
                        )
                    }
                }
            }
        }

        // Active Mounts Table
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Active Linux Mount Points:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                val mounts = listOf(
                    Pair("/dev/rootfs -> /", "ext4 (rw,noatime,barrier=0)"),
                    Pair("/dev/shm -> /dev/shm", "tmpfs (rw,nosuid,nodev)"),
                    Pair("/storage/emulated/0 -> /sdcard/Share", "fuse/sdcardfs (rw,shared)"),
                    Pair("/dev/dri/renderD128", "VirGL GLES hardware acceleration node"),
                    Pair("/dev/zram0 -> [SWAP]", "zRAM compressed paging active")
                )

                mounts.forEach { (path, flags) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Slate950)
                            .padding(8.dp)
                    ) {
                        Text(path, color = PhonixCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(flags, color = Slate400, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
