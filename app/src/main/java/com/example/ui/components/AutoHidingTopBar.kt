package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.ContainerStatus

@Composable
fun AutoHidingTopBar(
    isExpanded: Boolean,
    activeTab: AppNavTab,
    containerStatus: ContainerStatus,
    isBatteryRemoved: Boolean,
    /** SSID, or ONLINE/OFFLINE when the platform does not expose a name. */
    networkLabel: String,
    isOnline: Boolean,
    onOpenNetwork: () -> Unit,
    onToggleExpand: (Boolean) -> Unit,
    onTabSelected: (AppNavTab) -> Unit,
    onTogglePower: () -> Unit,
    onRestart: () -> Unit,
    onRunDebloat: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        // 1. FULL EXPANDED TELEMETRY & HARDWARE CONTROL BAR
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .shadow(16.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Exit) {
                                onToggleExpand(false)
                            }
                        }
                    }
                }
                .testTag("top_bar_expanded"),
            color = Slate950.copy(alpha = 0.97f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Top Identity & Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.linearGradient(listOf(PhonixAmber, PhonixRed))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("P", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Phonix", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("OS", color = PhonixAmber, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                            Text(
                                "Recycled Mini PC Shell • S8/S9/S10 Motherboard",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Action Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Debloat Button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onRunDebloat() }
                                .testTag("top_bar_debloat_btn"),
                            color = PhonixBlue.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PhonixBlue.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PhonixBlueLight, modifier = Modifier.size(12.dp))
                                Text("Debloat Junk", color = PhonixBlueLight, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Container Power Toggle
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onTogglePower() }
                                .testTag("top_bar_power_btn"),
                            color = if (containerStatus == ContainerStatus.RUNNING) PhonixGreen.copy(alpha = 0.15f) else PhonixRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (containerStatus == ContainerStatus.RUNNING) PhonixGreen else PhonixRed
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (containerStatus == ContainerStatus.RUNNING) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (containerStatus == ContainerStatus.RUNNING) PhonixGreen else PhonixRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    if (containerStatus == ContainerStatus.RUNNING) "RUNNING" else "STOPPED",
                                    color = if (containerStatus == ContainerStatus.RUNNING) PhonixGreen else PhonixRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Restart Button
                        IconButton(
                            onClick = onRestart,
                            modifier = Modifier.size(28.dp).testTag("top_bar_restart_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Slate400, modifier = Modifier.size(16.dp))
                        }

                        // Collapse Button (Retracts up)
                        IconButton(
                            onClick = { onToggleExpand(false) },
                            modifier = Modifier.size(28.dp).testTag("top_bar_collapse_btn")
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Hide", tint = Slate400, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppNavTab.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onTabSelected(tab)
                                    onToggleExpand(false)
                                }
                                .testTag("tab_${tab.name.lowercase()}"),
                            color = if (isSelected) PhonixBlue.copy(alpha = 0.25f) else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PhonixBlueLight) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = when (tab) {
                                        AppNavTab.DESKTOP -> Icons.Default.Computer
                                        AppNavTab.COOLING_POWER -> Icons.Default.Bolt
                                        AppNavTab.GAMING -> Icons.Default.SportsEsports
                                        AppNavTab.SERVERS -> Icons.Default.Dns
                                        AppNavTab.PARTITIONS -> Icons.Default.Storage
                                        AppNavTab.DISTROS -> Icons.Default.Widgets
                                        AppNavTab.APPS -> Icons.Default.Storefront
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) PhonixCyan else Slate400,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) Color.White else Slate400,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // 2. COLLAPSED NOTCH PILL ONLY
        // IMPORTANT: Strictly wrapContentSize() centered at top so it DOES NOT occupy or block
        // any space to the left or right! Windows and tabs near the top have 100% free touch access.
        Surface(
            modifier = modifier
                .statusBarsPadding()
                .padding(top = 2.dp)
                .wrapContentSize()
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Enter) {
                                onToggleExpand(true)
                            }
                        }
                    }
                }
                .clickable { onToggleExpand(true) }
                .testTag("top_notch_trigger"),
            color = Slate900.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mini Emblem
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PhonixAmber, PhonixRed)))
                )

                Text(
                    text = "Phonix Mini PC",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isBatteryRemoved) "AC ⚡" else "BATTERY",
                    color = if (isBatteryRemoved) PhonixAmber else PhonixGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Always reachable with the mouse: when a unit loses its network, the fix
                // must not depend on a phone screen that DeX has switched off.
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onOpenNetwork() }
                        .testTag("network_chip"),
                    color = if (isOnline) PhonixGreen.copy(alpha = 0.15f) else PhonixRed.copy(alpha = 0.22f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isOnline) PhonixGreen else PhonixRed,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = networkLabel,
                            color = if (isOnline) PhonixGreen else PhonixRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand controls",
                    tint = Slate400,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
