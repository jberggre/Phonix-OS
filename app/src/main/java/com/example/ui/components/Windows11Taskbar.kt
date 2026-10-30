package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DesktopShortcut
import com.example.model.WindowType
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
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TaskbarAppEntry(
    val id: String,
    val title: String,
    val windowType: WindowType,
    val icon: ImageVector,
    val defaultColor: Color,
    val isRunning: Boolean,
    val isFocused: Boolean,
    val shortcut: DesktopShortcut?
)

@Composable
fun Windows11Taskbar(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000L)
        }
    }

    // Build the list of taskbar apps: pinned shortcuts + active windows
    val taskbarApps = remember(uiState.customization.shortcuts, uiState.windows, uiState.activeWindowId) {
        val pinnedShortcuts = uiState.customization.shortcuts.filter { it.isPinnedToTaskbar }
        val openTypes = uiState.windows.map { it.type }.toSet()

        // Combine pinned and open
        val allTypes = (pinnedShortcuts.map { it.windowType } + openTypes).distinct()

        allTypes.map { type ->
            val shortcut = uiState.customization.shortcuts.find { it.windowType == type }
            val matchingWindow = uiState.windows.find { it.type == type }
            val isRunning = matchingWindow != null
            val isFocused = matchingWindow != null && !matchingWindow.isMinimized && matchingWindow.id == uiState.activeWindowId

            val icon = when (type) {
                WindowType.BROWSER -> Icons.Default.Language
                WindowType.DISTRO_STORE -> Icons.Default.Widgets
                WindowType.TERMINAL -> Icons.Default.Terminal
                WindowType.FILE_EXPLORER -> Icons.Default.Folder
                WindowType.SERVER_MANAGER -> Icons.Default.Dns
                WindowType.RESOURCE_MONITOR -> Icons.Default.Speed
                WindowType.BENCHMARK -> Icons.Default.Speed
                WindowType.NOTEPAD -> Icons.Default.Code
                WindowType.PERSONALIZE -> Icons.Default.Palette
                WindowType.PRIVACY -> Icons.Default.Security
                WindowType.ABOUT -> Icons.Default.Info
                WindowType.VS_CODE -> Icons.Default.Code
                WindowType.PHONE_MIRROR -> Icons.Default.PhoneAndroid
                WindowType.AI_ASSISTANT -> Icons.Default.SmartToy
                else -> Icons.Default.Computer
            }

            val defaultColor = when (type) {
                WindowType.BROWSER -> PhonixCyan
                WindowType.DISTRO_STORE -> PhonixAmber
                WindowType.TERMINAL -> PhonixGreen
                WindowType.FILE_EXPLORER -> Color(0xFFFBBF24)
                WindowType.SERVER_MANAGER -> Color(0xFFA855F7)
                WindowType.BENCHMARK -> PhonixRed
                WindowType.PRIVACY -> PhonixGreen
                WindowType.ABOUT -> PhonixCyan
                WindowType.VS_CODE -> PhonixBlueLight
                WindowType.PHONE_MIRROR -> Color(0xFF22D3EE)
                WindowType.AI_ASSISTANT -> Color(0xFFA855F7)
                else -> PhonixBlueLight
            }

            TaskbarAppEntry(
                id = type.name,
                title = shortcut?.title ?: type.defaultTitle,
                windowType = type,
                icon = icon,
                defaultColor = shortcut?.customColor?.color ?: defaultColor,
                isRunning = isRunning,
                isFocused = isFocused,
                shortcut = shortcut
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("windows_11_taskbar"),
        color = Slate950.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Spacer to balance layout if needed
            Box(modifier = Modifier.width(120.dp), contentAlignment = Alignment.CenterStart) {
                // Mini PC Brand / Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PhonixGreen)
                    )
                    Text(
                        text = "Phonix OS",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // CENTER: Windows 11 Centered Dock (Start Button + Search + Apps)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // 1. Windows 11 Start Button (Phonix Flame Emblem)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggleStartMenu() }
                        .background(if (uiState.isStartMenuOpen) Slate800 else Color.Transparent)
                        .border(
                            1.dp,
                            if (uiState.isStartMenuOpen) PhonixBlueLight else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .testTag("taskbar_start_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(listOf(PhonixAmber, PhonixRed))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 13.sp
                        )
                    }
                }

                // 2. Search Pill
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.toggleStartMenu()
                        }
                        .background(Color.Transparent)
                        .testTag("taskbar_search_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(2.dp))

                // 3. Centered App Icons with Running Indicator Dots
                taskbarApps.forEach { app ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (app.shortcut != null && !app.isRunning && app.shortcut.launchPackage != null) {
                                    viewModel.launchShortcutOrHostApp(context, app.shortcut)
                                } else {
                                    viewModel.toggleWindow(app.windowType)
                                }
                            }
                            .background(
                                if (app.isFocused) Slate800.copy(alpha = 0.8f) else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (app.isFocused) PhonixBlueLight.copy(alpha = 0.6f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .testTag("taskbar_app_${app.id.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = app.icon,
                            contentDescription = app.title,
                            tint = app.defaultColor,
                            modifier = Modifier.size(22.dp)
                        )

                        // Running Dot Indicator
                        if (app.isRunning) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 2.dp)
                                    .width(if (app.isFocused) 16.dp else 5.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (app.isFocused) PhonixCyan else Slate400)
                            )
                        }
                    }
                }
            }

            // RIGHT: System Tray & Clock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(170.dp)
            ) {
                // Network IP Badge
                Text(
                    text = uiState.localIpAddress.ifEmpty { "10.0.2.15" },
                    color = PhonixCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                // AC Direct Power Badge
                Text(
                    text = if (uiState.isBatteryRemovedDirectPower) "AC ⚡" else "UPS",
                    color = if (uiState.isBatteryRemovedDirectPower) PhonixAmber else PhonixGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Clock & Date Stack
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clickable { viewModel.toggleWindow(WindowType.RESOURCE_MONITOR) }
                        .padding(horizontal = 4.dp)
                        .testTag("taskbar_clock")
                ) {
                    Text(
                        text = currentTime.ifEmpty { "10:00 AM" },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = currentDate.ifEmpty { "10/28/2026" },
                        color = Slate400,
                        fontSize = 9.sp
                    )
                }

                // Show Desktop Edge Sliver (Classic Windows far-right line)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .clickable { viewModel.toggleAllWindowVisibility() }
                        .background(Slate700.copy(alpha = 0.4f))
                        .testTag("taskbar_show_desktop_edge")
                )
            }
        }
    }
}
