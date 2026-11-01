package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DesktopShortcut
import com.example.model.TaskbarPosition
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
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

/**
 * The desktop taskbar.
 *
 * It docks to any edge: [position] decides whether the bar is a horizontal strip along the top or
 * bottom, or a vertical strip down the left or right. The bar owns its own dock gesture
 * (right-click with a mouse, long-press with a finger) so the edge can be changed from where the
 * user is looking, without opening a settings window.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Windows11Taskbar(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier,
    position: TaskbarPosition = TaskbarPosition.BOTTOM
) {
    val context = LocalContext.current
    // The brand block and fixed tray leave too little room for the app strip on a phone or a
    // 640 dpi desktop. Compact mode drops the redundant brand and scrolls the apps instead of
    // clipping them; the wide desktop keeps the centered Windows 11 layout untouched.
    val compactTaskbar = LocalConfiguration.current.screenWidthDp < 600
    val taskbarAppsScroll = rememberScrollState()
    val verticalAppsScroll = rememberScrollState()
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // The clock used to wake every second and format twice to redraw text that can only change
    // once a minute: 86,400 wake-ups a day for 1,440 visible changes. It now sleeps to the next
    // minute boundary, which is 60x less work for exactly the same display.
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(nextMinuteDelayMillis(System.currentTimeMillis()))
        }
    }

    // Build the list of taskbar apps: taskbar pins + active windows.
    // The pins are the taskbar's own list, so deleting a desktop icon never removes an app here.
    val taskbarApps = remember(uiState.customization.taskbarPins, uiState.customization.shortcuts, uiState.windows, uiState.activeWindowId) {
        val pinnedShortcuts = uiState.customization.taskbarPins
        val openTypes = uiState.windows.map { it.type }.toSet()

        // Combine pinned and open
        val allTypes = (pinnedShortcuts.map { it.windowType } + openTypes).distinct()

        allTypes.map { type ->
            val shortcut = uiState.customization.taskbarPins.find { it.windowType == type }
                ?: uiState.customization.shortcuts.find { it.windowType == type }
            val matchingWindow = uiState.windows.find { it.type == type }
            val isRunning = matchingWindow != null
            val isFocused = matchingWindow != null && !matchingWindow.isMinimized && matchingWindow.id == uiState.activeWindowId

            // One icon source for every surface; this used to be a second, drifting table.
            val icon = AppIconGlyphs.forWindow(type)
            val defaultColor = AppIconGlyphs.accentForWindow(type)

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

    // A mouse right-click opens the dock menu directly. detectTapGestures only understands the
    // primary button, so the secondary button is read from the raw pointer event. The press is
    // consumed so it cannot also start an app from the button underneath.
    val dockMenuGesture = Modifier.pointerInput(uiState.customization.taskbarPosition) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val secondary = down.type == PointerType.Mouse && currentEvent.buttons.isSecondaryPressed
            if (secondary) {
                down.consume()
                viewModel.openTaskbarContextMenu(uiState.customization.taskbarPosition)
            }
        }
    }
    // Touch has no second button, so the same menu opens on a long press in the empty bar area.
    val longPressDock = Modifier.combinedClickable(
        onClick = {},
        onLongClick = { viewModel.openTaskbarContextMenu(uiState.customization.taskbarPosition) }
    )

    if (position.isVertical) {
        Surface(
            modifier = modifier
                .width(TASKBAR_VERTICAL_WIDTH)
                .fillMaxHeight()
                .testTag("windows_11_taskbar"),
            color = Slate950.copy(alpha = 0.94f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(dockMenuGesture)
                    .then(longPressDock)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PhonixGreen)
                )

                StartButton(
                    isOpen = uiState.isStartMenuOpen,
                    onClick = { viewModel.toggleStartMenu() }
                )
                SearchButton(onClick = { viewModel.toggleStartMenu() })

                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(1.dp)
                        .background(Slate800)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(verticalAppsScroll),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    taskbarApps.forEach { app ->
                        TaskbarAppIcon(
                            app = app,
                            onClick = {
                                if (app.shortcut != null && !app.isRunning && app.shortcut.launchPackage != null) {
                                    viewModel.launchShortcutOrHostApp(context, app.shortcut)
                                } else {
                                    viewModel.toggleWindow(app.windowType)
                                }
                            },
                            // Long-press opens the rename dialog, which also carries the
                            // unpin action for a bar entry (the bar owns its own pins).
                            onLongClick = { viewModel.startRename(app.id) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(1.dp)
                        .background(Slate800)
                )

                // Vertical tray: the phone cannot spend 170 dp of width on it, so the readings
                // stack above a compact clock.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clickable { viewModel.toggleWindow(WindowType.RESOURCE_MONITOR) }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .testTag("taskbar_clock")
                ) {
                    Text(
                        text = if (uiState.isBatteryRemovedDirectPower) "AC" else "UPS",
                        color = if (uiState.isBatteryRemovedDirectPower) PhonixAmber else PhonixGreen,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentTime.ifEmpty { "10:00 AM" },
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = currentDate,
                        color = Slate400,
                        fontSize = 7.sp
                    )
                }

                // Show-desktop: the bottom sliver of a horizontal bar, rotated to the bar's end.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .clickable { viewModel.toggleAllWindowVisibility() }
                        .background(Slate700.copy(alpha = 0.4f))
                        .testTag("taskbar_show_desktop_edge")
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(TASKBAR_HEIGHT)
            .testTag("windows_11_taskbar"),
        color = Slate950.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .then(dockMenuGesture)
                .then(longPressDock)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compactTaskbar) Arrangement.Start else Arrangement.SpaceBetween
        ) {
            if (!compactTaskbar) {
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
            }

            // CENTER: Windows 11 Centered Dock (Start Button + Search + Apps)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f, fill = compactTaskbar)
                    .then(
                        if (compactTaskbar) Modifier.horizontalScroll(taskbarAppsScroll)
                        else Modifier
                    )
            ) {
                StartButton(
                    isOpen = uiState.isStartMenuOpen,
                    onClick = { viewModel.toggleStartMenu() }
                )
                SearchButton(onClick = { viewModel.toggleStartMenu() })

                Spacer(Modifier.width(2.dp))

                taskbarApps.forEach { app ->
                    TaskbarAppIcon(
                        app = app,
                        onClick = {
                            if (app.shortcut != null && !app.isRunning && app.shortcut.launchPackage != null) {
                                viewModel.launchShortcutOrHostApp(context, app.shortcut)
                            } else {
                                viewModel.toggleWindow(app.windowType)
                            }
                        },
                        // Long-press opens the rename dialog, which also carries the unpin
                        // action for a bar entry (the bar owns its own pins).
                        onLongClick = { viewModel.startRename(app.id) }
                    )
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
                        // Blank until the first tick formats a real date, instead of showing a
                        // hardcoded placeholder date that was briefly wrong.
                        text = currentDate,
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

/** Height of a horizontal bar; the desktop work area subtracts exactly this. */
internal const val TASKBAR_HEIGHT_DP = 50

/** Width of a vertical bar; the desktop work area subtracts exactly this. */
internal const val TASKBAR_WIDTH_DP = 56

private val TASKBAR_HEIGHT = TASKBAR_HEIGHT_DP.dp
private val TASKBAR_VERTICAL_WIDTH = TASKBAR_WIDTH_DP.dp

@Composable
private fun StartButton(isOpen: Boolean, onClick: () -> Unit) {
    // 1. Windows 11 Start Button (Phonix Flame Emblem)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (isOpen) Slate800 else Color.Transparent)
            .border(
                1.dp,
                if (isOpen) PhonixBlueLight else Color.Transparent,
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
}

@Composable
private fun SearchButton(onClick: () -> Unit) {
    // 2. Search Pill
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskbarAppIcon(
    app: TaskbarAppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // 3. App Icon with Running Indicator Dot
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
        val artRes = app.shortcut?.customArtKey?.let { AppIconGlyphs.artForKey(it) }
            ?: AppIconGlyphs.artFor(app.windowType)

        if (artRes != null) {
            Image(
                painter = painterResource(id = artRes),
                contentDescription = app.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                imageVector = app.icon,
                contentDescription = app.title,
                tint = app.defaultColor,
                modifier = Modifier.size(22.dp)
            )
        }

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

/**
 * Milliseconds until the wall clock reaches the next whole minute.
 *
 * The taskbar clock only changes on a minute boundary, so waking on any other cadence is wasted
 * work - and on a 2019 phone every avoidable wake-up is battery. Kept as a pure function so the
 * boundary handling (including the last millisecond of a minute) can be unit tested without a
 * device.
 */
internal fun nextMinuteDelayMillis(nowMillis: Long): Long {
    val millisIntoMinute = nowMillis % 60_000L
    val remaining = 60_000L - millisIntoMinute
    return if (remaining <= 0L) 60_000L else remaining
}
