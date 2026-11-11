package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixBlueLight
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.PhonixRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.ContainerStatus

@Composable
fun PhonixTopBar(
    activeTab: AppNavTab,
    containerStatus: ContainerStatus,
    isBatteryRemoved: Boolean = true,
    onTabSelected: (AppNavTab) -> Unit,
    onTogglePower: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Slate900,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Header Row: App Identity & Container Power Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(PhonixAmber, PhonixRed)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "P",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Phonix",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OS",
                                color = PhonixAmber,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "Hardware-Accelerated Mini PC & Server",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                // Power & Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Battery-free DC badge
                    if (isBatteryRemoved) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F261C))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "AC DIRECT ⚡",
                                color = PhonixGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Container Status Chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Slate800)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusColor = when (containerStatus) {
                            ContainerStatus.RUNNING -> PhonixGreen
                            ContainerStatus.STOPPED -> PhonixRed
                            ContainerStatus.STARTING, ContainerStatus.RESTARTING -> Color(0xFFFBBF24)
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (containerStatus) {
                                ContainerStatus.RUNNING -> "ACTIVE"
                                ContainerStatus.STOPPED -> "STOPPED"
                                ContainerStatus.STARTING -> "STARTING"
                                ContainerStatus.RESTARTING -> "REBOOT"
                            },
                            color = Slate200,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Restart Button
                    IconButton(
                        onClick = onRestart,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("restart_container_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Container",
                            tint = Slate200,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Toggle Power Button
                    IconButton(
                        onClick = onTogglePower,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (containerStatus == ContainerStatus.RUNNING) Slate800 else PhonixGreen)
                            .testTag("power_container_button")
                    ) {
                        Icon(
                            imageVector = if (containerStatus == ContainerStatus.RUNNING) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Power",
                            tint = if (containerStatus == ContainerStatus.RUNNING) PhonixRed else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Navigation Tabs Row
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppNavTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    val tabIcon = when (tab) {
                        AppNavTab.DESKTOP -> Icons.Default.Computer
                        AppNavTab.COOLING_POWER -> Icons.Default.Bolt
                        AppNavTab.GAMING -> Icons.Default.SportsEsports
                        AppNavTab.SERVERS -> Icons.Default.Dns
                        AppNavTab.PARTITIONS -> Icons.Default.Storage
                        AppNavTab.DISTROS -> Icons.Default.Widgets
                        AppNavTab.APPS -> Icons.Default.Storefront
                        AppNavTab.HELP -> Icons.Default.Info
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PhonixBlue else Slate800)
                            .clickable { onTabSelected(tab) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color.White else Slate200,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
