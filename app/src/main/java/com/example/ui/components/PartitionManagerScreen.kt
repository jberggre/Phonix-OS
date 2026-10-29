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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
 * Storage, as the device reports it.
 *
 * This tab used to show a 32 GB phone with a 12 GB "EXT4 rootfs image" and a 2 GB "zRAM swap
 * partition", plus a partitioning routine that animated progress bars for filesystems that
 * were never created. Phonix does not partition anything, so the screen now reports the real
 * free space and the real size of the guest directory.
 */
@Composable
fun PartitionManagerScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { viewModel.refreshStorage() }
    val storage = uiState.storage
    val usedBytes = (storage.totalBytes - storage.freeBytes).coerceAtLeast(0L)
    val usedFraction = if (storage.totalBytes > 0)
        (usedBytes.toFloat() / storage.totalBytes).coerceIn(0f, 1f) else 0f

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
                Text(
                    text = "Storage",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Free space on this device and what the Linux guest uses",
                    color = Slate400,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { viewModel.refreshStorage() },
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Refresh", fontSize = 11.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Slate900), shape = RoundedCornerShape(10.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("App storage", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                if (storage.totalBytes <= 0L) {
                    Text("Reading the filesystem...", color = Slate400, fontSize = 11.sp)
                } else {
                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = PhonixCyan,
                        trackColor = Slate800
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Used ${formatBytes(usedBytes)}", color = Slate300, fontSize = 11.sp)
                        Text("Free ${formatBytes(storage.freeBytes)}", color = PhonixGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Total ${formatBytes(storage.totalBytes)}", color = Slate400, fontSize = 10.sp)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Slate900), shape = RoundedCornerShape(10.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Linux guest", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "On disk: ${if (storage.guestBytes > 0) formatBytes(storage.guestBytes) else "-"}",
                    color = PhonixCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (storage.guestPath.isNotEmpty()) {
                    Text(
                        text = storage.guestPath,
                        color = Slate400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    "The guest is a directory in app storage. Uninstalling Phonix removes it completely; " +
                        "there is no image file, no partition and no swap file to clean up afterwards.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
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
                    Icon(Icons.Default.Info, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("No partitioning", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Reserving space for a second OS means repartitioning the phone, which risks the boot " +
                        "chain and wastes space on drivers Android already provides. Phonix instead runs the " +
                        "guest on top of Android, so the phone's storage stays exactly as Samsung shipped it.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Text(
                    "Growth is limited by free app storage: pkg installs and container files count against it.",
                    color = Slate400,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "-"
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) String.format(Locale.ROOT, "%.1f GB", gb)
    else String.format(Locale.ROOT, "%.0f MB", bytes / (1024.0 * 1024.0))
}
