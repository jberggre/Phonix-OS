package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import com.example.model.ShortcutShape
import com.example.model.ShortcutBgStyle
import com.example.model.DesktopContextMenuData
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import com.example.model.AiChatMessage
import com.example.model.AiEngineState
import com.example.model.UpdateCheckState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.KnowledgeCategory
import com.example.model.KnowledgeLibrary
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.engine.LocalAiEngine
import com.example.engine.TerminalLine
import com.example.model.DesktopAccentColor
import com.example.model.DesktopShortcut
import com.example.model.DesktopWallpaper
import com.example.util.WallpaperStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.model.DesktopWindow
import com.example.model.WindowBounds
import com.example.model.IconSizeMode
import com.example.model.MonitorDisplayMode
import com.example.model.TaskbarPosition
import com.example.model.TaskbarStyle
import com.example.ui.components.TASKBAR_HEIGHT_DP
import com.example.ui.components.TASKBAR_WIDTH_DP
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixBlueLight
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.PhonixRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalGreen
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Work-area insets that match the taskbar's own dimensions, in one place. */
private val WINDOWS_TASKBAR_HEIGHT = TASKBAR_HEIGHT_DP.dp
private val WINDOWS_TASKBAR_WIDTH = TASKBAR_WIDTH_DP.dp

@Composable
fun PhonixDesktopScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val customization = uiState.customization
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPos by remember { mutableStateOf(Pair(100f, 100f)) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isHardwareLandscape = screenWidth > screenHeight
        val isWidescreenMonitor = isHardwareLandscape || customization.displayMode.isWidescreen

        val desktopWidth = screenWidth
        // Windows live in the work area the taskbar leaves. Without this the edge of a
        // full-height window (and its resize grip) disappears under the bar at high densities,
        // where the window's 180 dp minimum is the whole desktop. The bar can be on any of the
        // four edges, so the inset follows whichever side it is docked to.
        val barPosition = customization.taskbarPosition
        val barThickness = WINDOWS_TASKBAR_HEIGHT
        val barWidth = WINDOWS_TASKBAR_WIDTH
        val bounds = remember(screenWidth, screenHeight, barPosition) {
            WindowBounds(
                widthDp = (screenWidth - if (barPosition.isVertical) barWidth else 0.dp)
                    .coerceAtLeast(1.dp).value,
                heightDp = (screenHeight - if (barPosition.isVertical) 0.dp else barThickness)
                    .coerceAtLeast(1.dp).value
            )
        }
        LaunchedEffect(bounds) { viewModel.normalizeWindows(bounds) }
        val unfoldedLayout = uiState.foldPosture == com.example.model.FoldPosture.FLAT || screenWidth >= 600.dp
        val availableGridWidth = if (unfoldedLayout && screenWidth >= 680.dp) screenWidth - 240.dp else screenWidth - 24.dp
        val iconColumns = (availableGridWidth / 90.dp).toInt().coerceAtLeast(1)
        // Desktop icons are their own list. Pinning an app to the taskbar adds a separate entry
        // there; it never hides the icon or claims its slot.
        val desktopShortcuts = customization.shortcuts
        val rowStride = 88.dp
        val topPadding = if (barPosition == TaskbarPosition.TOP) 54.dp else 10.dp
        val bottomPadding = if (barPosition == TaskbarPosition.BOTTOM) 54.dp else 12.dp
        // Icons keep clear of a vertical bar too, instead of hiding under it.
        val startPadding = if (barPosition == TaskbarPosition.LEFT) barWidth + 12.dp else 12.dp
        val endPadding = if (barPosition == TaskbarPosition.RIGHT) barWidth + 12.dp else 12.dp
        // The default position is the BOTTOM-left quadrant, which is what the align action and
        // the label both promise. This was pinned to 0.dp, so "Align to Bottom-Left" dropped the
        // whole block in the top-left corner.
        val gridRows = ((desktopShortcuts.size + iconColumns - 1) / iconColumns).coerceAtLeast(1)
        val iconAreaHeight = (screenHeight - topPadding - bottomPadding).coerceAtLeast(rowStride)
        val gridTop = (iconAreaHeight - (rowStride * gridRows) - 6.dp).coerceAtLeast(0.dp)

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val desktopContext = LocalContext.current

                // 1. Customizable Desktop Background with Phoenix Emblem as default
                DesktopWallpaperBackground(
                    wallpaper = customization.wallpaper,
                    customWallpaperPath = customization.customWallpaperPath,
                    accentColor = customization.accentColor.color,
                    showGrid = customization.showDesktopGrid,
                    onDesktopClicked = {
                        viewModel.closeStartMenu()
                        viewModel.closeContextMenu()
                    },
                    onDesktopLongPressed = { x, y ->
                        viewModel.openWallpaperContextMenu(x, y)
                    }
                )

                // 2. Top Monitor & Dock Status Bar (Quick Display Switcher & Dock Telemetry)
                // The monitor/dock status strip used to sit here and covered the top of the
                // desktop. It is gone: on a Mini PC the screen is the product, so nothing may
                // overlay it. The information it carried lives in the Vitals widget instead.

                // 3. Desktop Shortcuts Layer (Freeform / Grid Draggable & Right-Click Customization)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = topPadding,
                            bottom = bottomPadding,
                            start = startPadding,
                            end = endPadding
                        )
                ) {
                    // Reflow the grid from the live window width. The inner fold gets several
                    // columns; the narrow cover display uses fewer columns and more rows.
                    // One lookup table instead of a fresh linear scan per icon on every
                    // recomposition (the desktop redraws whenever a reading or window changes).
                    val iconsByPackage = remember(uiState.installedApps) {
                        uiState.installedApps.associateBy { it.packageName }
                    }
                    // A freeform icon stores its offset inside this area, and the area changes
                    // size when the bar moves to another edge. Clamp instead of translating: an
                    // icon dragged to the very bottom of a right-docked desktop is lifted clear of
                    // a bottom bar rather than drawn on top of it, and returns to exactly where it
                    // was when the bar leaves the bottom edge.
                    val maxIconX = (screenWidth - startPadding - endPadding - 100.dp)
                        .coerceAtLeast(0.dp).value
                    val maxIconY = (iconAreaHeight - 96.dp).coerceAtLeast(0.dp).value
                    desktopShortcuts.forEachIndexed { index, shortcut ->
                        val effectiveShortcut = if (!shortcut.hasCustomPosition) {
                            shortcut.copy(
                                posX = (index % iconColumns) * 90f,
                                posY = gridTop.value + ((index / iconColumns) * rowStride.value)
                            )
                        } else {
                            shortcut.copy(
                                posX = shortcut.posX.coerceIn(0f, maxIconX),
                                posY = shortcut.posY.coerceIn(0f, maxIconY)
                            )
                        }
                        val appIcon = effectiveShortcut.launchPackage
                            ?.let { iconsByPackage[it] }
                            ?.iconDrawable
                        DraggableDesktopShortcut(
                            shortcut = effectiveShortcut,
                            iconSize = customization.iconSize,
                            showLabel = customization.showDesktopLabels,
                            accentColor = customization.accentColor.color,
                            appIconDrawable = appIcon,
                            showFrames = customization.showIconFrames,
                            onMove = { dx, dy -> viewModel.moveShortcut(effectiveShortcut.id, dx, dy) },
                            onDrop = { x, y -> viewModel.dropShortcut(effectiveShortcut.id, x, y) },
                            onClick = {
                                val density = desktopContext.resources.displayMetrics.density
                                val bounds = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    android.graphics.Rect(
                                        (effectiveShortcut.posX * density).toInt(),
                                        (effectiveShortcut.posY * density).toInt(),
                                        ((effectiveShortcut.posX + 380f) * density).toInt(),
                                        ((effectiveShortcut.posY + 320f) * density).toInt()
                                    )
                                } else null
                                viewModel.launchShortcutOrHostApp(desktopContext, effectiveShortcut, bounds)
                            },
                            onRightClick = { x, y -> viewModel.openShortcutContextMenu(effectiveShortcut.id, x, y) }
                        )
                    }

                    // Desktop Widgets & Quick Reopen Controls (Docked on Right Side)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .widthIn(max = if (isWidescreenMonitor) 260.dp else 220.dp)
                            .padding(end = 6.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Quick Re-open pill buttons if one or both widgets are closed
                        if (!customization.showQuickDistroWidget || !customization.showSystemMonitorWidget) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!customization.showQuickDistroWidget) {
                                    Surface(
                                        onClick = { viewModel.toggleDesktopWidget("distro", true) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Slate900.copy(alpha = 0.90f),
                                        border = BorderStroke(1.dp, PhonixAmber.copy(alpha = 0.7f)),
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Apps, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ OS Card", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (!customization.showSystemMonitorWidget) {
                                    Surface(
                                        onClick = { viewModel.toggleDesktopWidget("system", true) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Slate900.copy(alpha = 0.90f),
                                        border = BorderStroke(1.dp, PhonixCyan.copy(alpha = 0.7f)),
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Speed, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Vitals", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (customization.showQuickDistroWidget) {
                            DesktopQuickDistroWidget(uiState, viewModel)
                        }
                        if (customization.showSystemMonitorWidget) {
                            DesktopSystemWidget(uiState, viewModel)
                        }
                    }
                }

                // 4. Desktop Windows Layer (Toggleable on / off)
                // Inset by the docked taskbar, so a window can never be dragged under it or
                // lose its resize grip to it, whichever edge the bar is on.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (barPosition == TaskbarPosition.TOP) barThickness else 0.dp,
                            bottom = if (barPosition == TaskbarPosition.BOTTOM) barThickness else 0.dp,
                            start = if (barPosition == TaskbarPosition.LEFT) barWidth else 0.dp,
                            end = if (barPosition == TaskbarPosition.RIGHT) barWidth else 0.dp
                        )
                ) {
                uiState.windows.filter { !it.isMinimized }.forEach { window ->
                    DesktopWindowFrame(
                        window = window,
                        isActive = uiState.activeWindowId == window.id,
                        accentColor = customization.accentColor.color,
                        uiState = uiState,
                        viewModel = viewModel,
                        bounds = bounds
                    )
                }

                }

                // 5. Windows 11 Desktop Context Menu (Shortcut & Wallpaper)
                uiState.activeContextMenu?.let { menuData ->
                    DesktopContextMenu(
                        menuData = menuData,
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }

                // 5.5 Add App to Desktop Dialog (Installed Phone Apps Picker)
                if (uiState.isAddAppDialogOpen) {
                    DesktopAppPickerDialog(
                        uiState = uiState,
                        viewModel = viewModel,
                        onDismiss = { viewModel.closeAddAppDialog() }
                    )
                }

                // 5.6 Rename dialog: names are per entry, the artwork never changes.
                if (uiState.renameTargetId != null) {
                    RenameEntryDialog(uiState = uiState, viewModel = viewModel)
                }

                // 6. Windows 11 Fluent Acrylic Start Menu, anchored to the bar's edge so it
                // never opens underneath the taskbar.
                if (uiState.isStartMenuOpen) {
                    val menuAlignment = when (barPosition) {
                        TaskbarPosition.TOP -> Alignment.TopCenter
                        TaskbarPosition.LEFT -> Alignment.BottomStart
                        TaskbarPosition.RIGHT -> Alignment.BottomEnd
                        TaskbarPosition.BOTTOM -> Alignment.BottomCenter
                    }
                    val menuOffsetX = when (barPosition) {
                        TaskbarPosition.LEFT -> barWidth
                        TaskbarPosition.RIGHT -> -barWidth
                        else -> 0.dp
                    }
                    val menuOffsetY = when (barPosition) {
                        TaskbarPosition.TOP -> barThickness + 6.dp
                        TaskbarPosition.BOTTOM -> -(barThickness + 6.dp)
                        else -> (-8).dp
                    }
                    Windows11StartMenu(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier
                            .align(menuAlignment)
                            .offset(x = menuOffsetX, y = menuOffsetY)
                    )
                }

                // 7. Taskbar (App Pointers, System Tray & Show Desktop), docked to its edge.
                // Right-click or long-press on the bar opens the dock menu.
                Windows11Taskbar(
                    uiState = uiState,
                    viewModel = viewModel,
                    position = barPosition,
                    modifier = Modifier.align(
                        when (barPosition) {
                            TaskbarPosition.BOTTOM -> Alignment.BottomCenter
                            TaskbarPosition.TOP -> Alignment.TopCenter
                            TaskbarPosition.LEFT -> Alignment.CenterStart
                            TaskbarPosition.RIGHT -> Alignment.CenterEnd
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun MonitorStatusBar(
    customization: com.example.model.DesktopCustomization,
    isHardwareLandscape: Boolean,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Slate900.copy(alpha = 0.75f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = PhonixGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isHardwareLandscape)
                    "External display connected (resolution is set by Android, not by this app)"
                else
                    "No external display: desktop is running in this window",
                color = Slate300,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        // Homescreen Widget & Display Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // OS Distro Card Toggle Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (customization.showQuickDistroWidget) PhonixAmber.copy(alpha = 0.25f) else Slate800)
                    .border(1.dp, if (customization.showQuickDistroWidget) PhonixAmber else Slate700, RoundedCornerShape(4.dp))
                    .clickable { viewModel.toggleDesktopWidget("distro", !customization.showQuickDistroWidget) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Toggle Active Linux Distro Card",
                        tint = if (customization.showQuickDistroWidget) PhonixAmber else Slate400,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (customization.showQuickDistroWidget) "OS Card: ON" else "OS Card: OFF",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // System Vitals Toggle Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (customization.showSystemMonitorWidget) PhonixCyan.copy(alpha = 0.25f) else Slate800)
                    .border(1.dp, if (customization.showSystemMonitorWidget) PhonixCyan else Slate700, RoundedCornerShape(4.dp))
                    .clickable { viewModel.toggleDesktopWidget("system", !customization.showSystemMonitorWidget) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Toggle System Vitals Card",
                        tint = if (customization.showSystemMonitorWidget) PhonixCyan else Slate400,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (customization.showSystemMonitorWidget) "Vitals: ON" else "Vitals: OFF",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Display Mode Toggle Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (customization.displayMode.isWidescreen) PhonixBlue else Slate800)
                    .clickable {
                        val next = when (customization.displayMode) {
                            MonitorDisplayMode.PHONE_PORTRAIT -> MonitorDisplayMode.MONITOR_FHD_16_9
                            MonitorDisplayMode.MONITOR_FHD_16_9 -> MonitorDisplayMode.ULTRAWIDE_21_9
                            MonitorDisplayMode.ULTRAWIDE_21_9 -> MonitorDisplayMode.PHONE_PORTRAIT
                        }
                        viewModel.updateDisplayMode(next)
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = customization.displayMode.title,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopWallpaperBackground(
    wallpaper: DesktopWallpaper,
    customWallpaperPath: String? = null,
    accentColor: Color,
    showGrid: Boolean,
    onDesktopClicked: () -> Unit,
    onDesktopLongPressed: (Float, Float) -> Unit
) {
    val density = LocalDensity.current.density
    Box(
        modifier = Modifier
            .fillMaxSize()
            // detectTapGestures only understands primary taps and long holds, so a mouse
            // right-click on the desktop did nothing: the menu only appeared when a right button
            // happened to be held long enough to be read as a long press, which is why it felt
            // like one success in twenty-five tries.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downAt = down.position
                    val secondary = down.type == PointerType.Mouse &&
                        currentEvent.buttons.isSecondaryPressed

                    if (secondary) {
                        down.consume()
                        onDesktopLongPressed(downAt.x / density, downAt.y / density)
                        return@awaitEachGesture
                    }

                    val slop = viewConfiguration.touchSlop
                    var moved = 0f
                    val releasedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var pressed = true
                        while (pressed && moved <= slop) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                            pressed = change?.pressed == true
                            if (change != null) {
                                moved = maxOf(moved, (change.position - downAt).getDistance())
                            }
                        }
                        true
                    }

                    when {
                        // Timed out while still held: that is the long-press menu.
                        releasedEarly == null -> onDesktopLongPressed(downAt.x / density, downAt.y / density)
                        moved <= slop -> onDesktopClicked()
                        // A swipe across the wallpaper: neither a tap nor a menu.
                        else -> Unit
                    }
                }
            }
    ) {
        when (wallpaper) {
            DesktopWallpaper.PHOENIX_CYBER_DEFAULT -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_phoenix_wallpaper),
                        contentDescription = "Official Phoenix OS Cyber Wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Ambient gradient overlay for enhanced readability
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xAA040711))
                                )
                            )
                    )
                }
            }

            DesktopWallpaper.EMBER_MARK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF07070A))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x33F59E0B), Color.Transparent)
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_phonix_ai),
                            contentDescription = null,
                            modifier = Modifier.size(168.dp),
                            alpha = 0.92f
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "PHONIX",
                            color = Color.White.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 6.sp
                        )
                    }
                }
            }

            DesktopWallpaper.ALPINE_PEAK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF04121A), Color(0xFF062A33), Color(0xFF03121A))
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_phonix_linux),
                            contentDescription = null,
                            modifier = Modifier.size(156.dp),
                            alpha = 0.95f
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "ALPINE LINUX IN A PROOT",
                            color = PhonixCyan.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            DesktopWallpaper.TERMINAL_GREEN -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF030A05))) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            "root@phonix:~#",
                            color = TerminalGreen.copy(alpha = 0.85f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "uname -a  ·  cat /etc/os-release  ·  python3",
                            color = TerminalGreen.copy(alpha = 0.38f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            DesktopWallpaper.OBSIDIAN_NIGHT -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF000000)))
            }

            DesktopWallpaper.RETRO_CLASSIC_TEAL -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF008080)))
            }

            DesktopWallpaper.CUSTOM_PHOTO -> {
                var bitmap by remember(customWallpaperPath) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(customWallpaperPath) {
                    bitmap = withContext(Dispatchers.IO) { WallpaperStore.load(customWallpaperPath) }
                }
                val photo = bitmap
                if (photo != null) {
                    Image(
                        bitmap = photo.asImageBitmap(),
                        contentDescription = "Your desktop photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // A little dimming so white icon captions stay readable over a bright photo.
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0x33000000))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(listOf(Slate950, Slate900, Slate950))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Pick a photo in Personalize > Desktop Wallpaper",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Alignment grid. It is a visual aid, so it is drawn whenever the switch is on - it used
        // to require grid snapping as well, which is off by default, so "Show Alignment Grid"
        // appeared to do nothing at all. The spacing matches the real icon grid (90 x 88 dp).
        if (showGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val columnStep = 90.dp.toPx()
                val rowStep = 88.dp.toPx()
                val dotRadius = 2.4.dp.toPx()
                var x = 12.dp.toPx()
                while (x < size.width) {
                    var y = 10.dp.toPx()
                    while (y < size.height) {
                        // Dark halo under each dot: the grid must stay visible on both a
                        // near-black and a near-white wallpaper.
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.45f),
                            radius = dotRadius + 1.4.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = accentColor.copy(alpha = 0.85f),
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += rowStep
                    }
                    x += columnStep
                }
            }
        }

    }
}

// -------------------------------------------------------------
// DRAGGABLE APP SHORTCUTS (LOCKED TO GRID)
// -------------------------------------------------------------
@Composable
fun DraggableDesktopShortcut(
    shortcut: DesktopShortcut,
    iconSize: IconSizeMode,
    showLabel: Boolean,
    accentColor: Color,
    // Real launcher icon of the pinned phone app, when the shortcut points at one.
    appIconDrawable: Drawable? = null,
    // Off draws only the glyph and caption, with no tile or outline behind it.
    showFrames: Boolean = true,
    onMove: (Float, Float) -> Unit,
    onDrop: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onRightClick: (Float, Float) -> Unit
) {
    val effectiveSize = shortcut.customSize ?: iconSize
    val sizeDp = effectiveSize.sizeDp.dp
    val shapeRadius = shortcut.customShape.cornerRadiusDp.dp

    // A launched phone app has no WindowType of its own, so it gets a glyph derived from its
    // label instead of every pinned app inheriting the browser globe.
    val icon = if (shortcut.isNativeApp && shortcut.launchPackage != null && shortcut.windowType == WindowType.BROWSER) {
        AppIconGlyphs.forApp(shortcut.title, shortcut.launchPackage)
    } else {
        AppIconGlyphs.forWindow(shortcut.windowType)
    }

    val defaultIconTint = when (shortcut.windowType) {
        WindowType.BROWSER -> PhonixCyan
        WindowType.DISTRO_STORE -> PhonixAmber
        WindowType.PERSONALIZE -> Color(0xFFA855F7)
        WindowType.TERMINAL -> TerminalGreen
        WindowType.SERVER_MANAGER -> PhonixCyan
        WindowType.RESOURCE_MONITOR -> Color(0xFF34D399)
        WindowType.FILE_EXPLORER -> Color(0xFFFBBF24)
        WindowType.BENCHMARK -> Color(0xFFF43F5E)
        WindowType.PRIVACY -> PhonixGreen
        WindowType.ABOUT -> PhonixCyan
        WindowType.VS_CODE -> PhonixBlueLight
        WindowType.PHONE_MIRROR -> Color(0xFF22D3EE)
        WindowType.AI_ASSISTANT -> Color(0xFFA855F7)
        else -> PhonixBlueLight
    }

    val iconTint = shortcut.customColor?.color ?: defaultIconTint

    val density = LocalDensity.current.density
    var isDragging by remember(shortcut.id) { mutableStateOf(false) }
    var dragOffsetX by remember(shortcut.id) { mutableFloatStateOf(0f) }
    var dragOffsetY by remember(shortcut.id) { mutableFloatStateOf(0f) }

    val shortcutWidth = (sizeDp + 26.dp).coerceAtLeast(76.dp)

    // A phone app's own launcher icon gets a tile behind it. Phonix's designed artwork is
    // already a finished tile, so a card behind it doubles the border: the artwork is drawn
    // bare and the frames switch only affects real app icons and the glyph fallback.
    val phoneIcon = appIconDrawable
    val customArt = AppIconGlyphs.artForKey(shortcut.customArtKey)
    val artRes = when {
        customArt != null -> customArt
        // When frames are off, do not use the badged container artwork for File Explorer so all apps have flat, uniform glyphs
        !showFrames && shortcut.windowType == WindowType.FILE_EXPLORER -> null
        // Pre-loaded Phonix apps always use their crafted Phonix artwork directly
        shortcut.id.startsWith("sc_") || !shortcut.isNativeApp || shortcut.windowType != WindowType.BROWSER ->
            AppIconGlyphs.artFor(shortcut.windowType)
        phoneIcon != null -> null
        shortcut.isNativeApp && shortcut.launchPackage != null ->
            AppIconGlyphs.artForApp(shortcut.title, shortcut.launchPackage)
        else -> AppIconGlyphs.artFor(shortcut.windowType)
    }


    Box(
        modifier = Modifier
            // The layout position never changes during a drag. The pointer delta is applied to a
            // graphics layer, which is a draw-time translation: no relayout of the desktop layer,
            // no re-rasterising the background and border on every event. Moving the icon with
            // Modifier.offset was doing exactly that, which is where the stutter came from.
            .offset {
                IntOffset(
                    x = (shortcut.posX * density).roundToInt(),
                    y = (shortcut.posY * density).roundToInt()
                )
            }
            .pointerInput(shortcut.id, shortcut.posX, shortcut.posY) {
                try {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        val startY = down.position.y
                        val isMouseSecondary = down.type == PointerType.Mouse &&
                                currentEvent.buttons.isSecondaryPressed

                        if (isMouseSecondary) {
                            down.consume()
                            onRightClick(
                                shortcut.posX + (startX / density),
                                shortcut.posY + (startY / density)
                            )
                            return@awaitEachGesture
                        }

                        var dragStarted = false
                        val touchSlop = viewConfiguration.touchSlop
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        val startTime = System.currentTimeMillis()

                        while (true) {
                            val elapsed = System.currentTimeMillis() - startTime
                            val remainingTime = if (!dragStarted) {
                                (longPressTimeout - elapsed).coerceAtLeast(1L)
                            } else {
                                30000L
                            }

                            val event = withTimeoutOrNull(remainingTime) {
                                awaitPointerEvent(PointerEventPass.Main)
                            }

                            if (event == null && !dragStarted) {
                                // Long-press context menu triggered
                                onRightClick(
                                    shortcut.posX + (startX / density),
                                    shortcut.posY + (startY / density)
                                )
                                while (true) {
                                    val nextEvent = awaitPointerEvent(PointerEventPass.Main)
                                    if (nextEvent.changes.all { !it.pressed }) break
                                }
                                break
                            }

                            if (event == null) break

                            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                // Pointer lifted/released
                                if (dragStarted) {
                                    val finalX = (shortcut.posX + (dragOffsetX / density)).coerceAtLeast(0f)
                                    val finalY = (shortcut.posY + (dragOffsetY / density)).coerceAtLeast(0f)
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    isDragging = false
                                    onDrop(finalX, finalY)
                                } else {
                                    onClick()
                                }
                                break
                            }

                            val currentDist = hypot(change.position.x - startX, change.position.y - startY)
                            // A stream of move events (a real finger jitters, and a synthesised
                            // press sends moves continuously) keeps resetting the withTimeoutOrNull
                            // window, so a long press could never fire. Check the deadline against
                            // the clock after every event instead.
                            if (!dragStarted &&
                                currentDist <= touchSlop &&
                                System.currentTimeMillis() - startTime >= longPressTimeout
                            ) {
                                change.consume()
                                onRightClick(
                                    shortcut.posX + (startX / density),
                                    shortcut.posY + (startY / density)
                                )
                                while (true) {
                                    val releaseEvent = awaitPointerEvent(PointerEventPass.Main)
                                    if (releaseEvent.changes.all { !it.pressed }) break
                                }
                                break
                            }
                            if (!dragStarted) {
                                if (currentDist > touchSlop) {
                                    dragStarted = true
                                    isDragging = true
                                    change.consume()
                                    dragOffsetX = change.position.x - startX
                                    dragOffsetY = change.position.y - startY
                                }
                            } else {
                                change.consume()
                                dragOffsetX = change.position.x - startX
                                dragOffsetY = change.position.y - startY
                            }
                        }
                    }
                } finally {
                    if (isDragging) {
                        val finalX = (shortcut.posX + (dragOffsetX / density)).coerceAtLeast(0f)
                        val finalY = (shortcut.posY + (dragOffsetY / density)).coerceAtLeast(0f)
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        isDragging = false
                        onDrop(finalX, finalY)
                    }
                }
            }
            .graphicsLayer {
                translationX = dragOffsetX
                translationY = dragOffsetY
            }
            .zIndex(if (isDragging) 100f else 1f)
            .then(
                if (showFrames) {
                    Modifier
                        .shadow(
                            elevation = if (isDragging) 12.dp else 1.dp,
                            shape = RoundedCornerShape(shapeRadius),
                            spotColor = if (isDragging) PhonixCyan else iconTint.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(shapeRadius))
                        .background(
                            when (shortcut.customBackground) {
                                ShortcutBgStyle.FROSTED_GLASS -> Slate900.copy(alpha = if (isDragging) 0.95f else 0.76f)
                                ShortcutBgStyle.NEON_RIM -> Slate950.copy(alpha = 0.94f)
                                ShortcutBgStyle.SOLID_DARK -> Color(0xFF070B14)
                                ShortcutBgStyle.TRANSPARENT -> Color.Transparent
                            }
                        )
                        .border(
                            width = if (shortcut.customBackground == ShortcutBgStyle.NEON_RIM) 2.dp else 1.dp,
                            color = if (isDragging) PhonixCyan else iconTint.copy(
                                alpha = if (shortcut.customBackground == ShortcutBgStyle.NEON_RIM) 0.85f else 0.40f
                            ),
                            shape = RoundedCornerShape(shapeRadius)
                        )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .width(shortcutWidth)
            .testTag("shortcut_${shortcut.id}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .then(
                        if (showFrames && (phoneIcon != null || artRes != null)) {
                            Modifier
                                .clip(RoundedCornerShape(shapeRadius))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(shapeRadius))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (artRes != null) {
                    Image(
                        painter = painterResource(id = artRes),
                        contentDescription = shortcut.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(if (showFrames) sizeDp * 0.88f else sizeDp)
                    )
                } else if (phoneIcon != null) {
                    Image(
                        bitmap = remember(phoneIcon) {
                            phoneIcon.toBitmap(width = 96, height = 96).asImageBitmap()
                        },
                        contentDescription = shortcut.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(if (showFrames) sizeDp * 0.72f else sizeDp)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = shortcut.title,
                        tint = iconTint,
                        modifier = Modifier.size(sizeDp * 0.75f)
                    )
                }
            }

            if (showLabel) {
                Spacer(modifier = Modifier.height(4.dp))
                // Windows/Linux desktops wrap an icon caption to two lines; one line clipped
                // names like "Phonix Browser" to "Phonix Brows...".
                Text(
                    text = shortcut.title,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 88.dp)
                )
            }
        }
    }
}

@Composable
fun DesktopQuickDistroWidget(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    val context = LocalContext.current
    // The guest that is actually installed names itself; hardcoding "Alpine Linux" here is how
    // this card read "Alpine Linux 12.15" while a Debian tree was the active rootfs.
    val distro = uiState.currentDistro
    val version = remember(uiState.containerStatus, distro.id) {
        runCatching { com.example.engine.LinuxRuntime.container(context).version() }
            .getOrDefault(distro.version)
    }
    val running = uiState.containerStatus == com.example.viewmodel.ContainerStatus.RUNNING

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.90f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(PhonixBlue, Slate800)))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Linux system", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (running) "running" else "stopped",
                        color = if (running) PhonixGreen else Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.toggleDesktopWidget("distro", false) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Linux card", tint = Slate400, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (version.isBlank() || distro.name.contains(version)) distro.name
                       else distro.name + " · " + version,
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black
            )
            Text(
                text = "proot userspace, no root, no partition",
                color = Slate400,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.toggleWindow(WindowType.LINUX_DESKTOP) },
                modifier = Modifier.fillMaxWidth().height(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Open Linux desktop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DesktopSystemWidget(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.90f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Slate800, PhonixCyan)))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "System Vitals", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${uiState.telemetry.hottestZoneC.toInt()}°C",
                        color = if (uiState.telemetry.hottestZoneC >= 75f) PhonixRed else PhonixGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.toggleDesktopWidget("system", false) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close System Vitals", tint = Slate400, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CPU", color = Slate400, fontSize = 10.sp)
                Text("${uiState.telemetry.cpuPercent}%", color = PhonixBlueLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { uiState.telemetry.cpuPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = PhonixBlueLight,
                trackColor = Slate800
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RAM", color = Slate400, fontSize = 10.sp)
                Text("${uiState.telemetry.ramUsedMb}MB", color = PhonixCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { (uiState.telemetry.ramUsedMb.toFloat() / uiState.telemetry.ramTotalMb).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = PhonixCyan,
                trackColor = Slate800
            )
        }
    }
}

@Composable
fun DesktopContextMenu(
    posX: Float,
    posY: Float,
    lockToGrid: Boolean,
    areWindowsHidden: Boolean,
    showQuickDistroWidget: Boolean,
    showSystemMonitorWidget: Boolean,
    onDismiss: () -> Unit,
    onToggleLockGrid: () -> Unit,
    onResetGrid: () -> Unit,
    onToggleDistroWidget: () -> Unit,
    onToggleSystemWidget: () -> Unit,
    onToggleAllWindows: () -> Unit,
    onCloseAllWindows: () -> Unit,
    onToggleMonitorMode: () -> Unit,
    onPersonalize: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenDistroStore: () -> Unit,
    onOpenTerminal: () -> Unit
) {
    Card(
        modifier = Modifier
            .offset { IntOffset(posX.toInt().coerceAtMost(300), posY.toInt().coerceAtMost(380)) }
            .width(250.dp)
            .shadow(16.dp, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PhonixBlue, Slate700)))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            ContextMenuItem(
                title = if (areWindowsHidden) "Restore Windows (Turn On)" else "Show Desktop (Turn Off Windows)",
                icon = if (areWindowsHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                onClick = onToggleAllWindows
            )
            ContextMenuItem(
                title = if (showQuickDistroWidget) "Close Active Distro Card" else "Open Active Distro Card",
                icon = Icons.Default.Apps,
                onClick = onToggleDistroWidget
            )
            ContextMenuItem(
                title = if (showSystemMonitorWidget) "Close System Vitals Card" else "Open System Vitals Card",
                icon = Icons.Default.Speed,
                onClick = onToggleSystemWidget
            )
            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 3.dp))
            ContextMenuItem(
                title = if (lockToGrid) "✓ Lock Shortcuts to Grid" else "Lock Shortcuts to Grid",
                icon = Icons.Default.GridView,
                onClick = onToggleLockGrid
            )
            ContextMenuItem(
                title = "Align Shortcuts to Default Grid",
                icon = Icons.Default.Refresh,
                onClick = onResetGrid
            )
            ContextMenuItem(
                title = "Toggle 16:9 External Monitor View",
                icon = Icons.Default.Tv,
                onClick = onToggleMonitorMode
            )
            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 3.dp))
            ContextMenuItem(title = "Personalize Desktop & Colors", icon = Icons.Default.Palette, onClick = onPersonalize)
            ContextMenuItem(title = "Linux system (Alpine)", icon = Icons.Default.Apps, onClick = onOpenDistroStore)
            ContextMenuItem(title = "Open Web Browser", icon = Icons.Default.Language, onClick = onOpenBrowser)
            ContextMenuItem(title = "Open Terminal", icon = Icons.Default.Terminal, onClick = onOpenTerminal)
            ContextMenuItem(title = "Close All Windows", icon = Icons.Default.Close, onClick = onCloseAllWindows)
            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 3.dp))
            ContextMenuItem(title = "Close Menu", icon = Icons.Default.Close, onClick = onDismiss)
        }
    }
}

@Composable
fun ContextMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PhonixBlueLight, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DesktopWindowFrame(
    window: DesktopWindow,
    isActive: Boolean,
    accentColor: Color,
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    bounds: WindowBounds
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset {
                if (window.isMaximized) IntOffset(0, 0)
                else IntOffset(
                    (window.posX * density.density).roundToInt(),
                    (window.posY * density.density).roundToInt()
                )
            }
            .then(
                // The windows layer is already inset by the docked taskbar on every edge, so a
                // maximized window fills exactly that area. This used to reserve a fixed 50 dp at
                // the bottom, which double-inset a bottom bar and, with the bar on the left or
                // right, lifted the window off the bottom edge for no reason at all.
                if (window.isMaximized) Modifier.fillMaxSize()
                else Modifier.size(width = window.width.dp, height = window.height.dp)
            )
            .zIndex(window.zIndex)
            .shadow(
                elevation = if (isActive) 16.dp else 6.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) accentColor else Slate700,
                shape = RoundedCornerShape(8.dp)
            )
            .background(Slate950)
            .clickable { viewModel.focusWindow(window.id) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(
                        if (isActive) Brush.horizontalGradient(listOf(Slate900, accentColor.copy(alpha = 0.4f)))
                        else Brush.horizontalGradient(listOf(Slate900, Slate850))
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Draggable Window Header (Separated from buttons so dragging never swallows clicks)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // Keyed on the container bounds, not the window size: a resize or rotation
                        // must hand the gesture the new clamp, but a drag in progress must not
                        // restart just because the window itself moved.
                        .pointerInput(window.id, bounds) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.moveWindow(window.id, dragAmount.x / density.density, dragAmount.y / density.density, bounds)
                            }
                        }
                        .clickable { viewModel.focusWindow(window.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val windowIcon = when (window.type) {
                        WindowType.BROWSER -> Icons.Default.Language
                        WindowType.DISTRO_STORE -> Icons.Default.Apps
                        WindowType.PERSONALIZE -> Icons.Default.Palette
                        WindowType.TERMINAL -> Icons.Default.Terminal
                        WindowType.SERVER_MANAGER -> Icons.Default.Dns
                        WindowType.RESOURCE_MONITOR -> Icons.Default.Speed
                        WindowType.FILE_EXPLORER -> Icons.Default.Folder
                        WindowType.BENCHMARK -> Icons.Default.PlayArrow
                        WindowType.PRIVACY -> Icons.Default.Security
                        WindowType.ABOUT -> Icons.Default.Info
                        WindowType.VS_CODE -> Icons.Default.Code
                        WindowType.AI_ASSISTANT -> Icons.Default.SmartToy
                        else -> Icons.Default.Computer
                    }
                    Icon(
                        imageVector = windowIcon,
                        contentDescription = null,
                        tint = if (isActive) accentColor else Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = window.title,
                        color = if (isActive) Color.White else Slate300,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 2. Window Control Buttons (Dedicated click scope with generous touch targets)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimize
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Slate800)
                            .clickable { viewModel.minimizeWindow(window.id) }
                            .testTag("win_btn_min_${window.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "–", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    // Maximize / Restore
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Slate800)
                            .clickable { viewModel.toggleMaximizeWindow(window.id) }
                            .testTag("win_btn_max_${window.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "□", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    // Close Window (Vivid Red X)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEF4444))
                            .clickable { viewModel.closeWindow(window.id) }
                            .testTag("win_btn_close_${window.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Window", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Window Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate950)
            ) {
                when (window.type) {
                    WindowType.BROWSER -> BrowserWindowContent(uiState, viewModel)
                    WindowType.LINUX_DESKTOP -> LinuxDesktopWindowContent(viewModel)
                    WindowType.DISTRO_STORE -> DistroHubScreen(uiState, viewModel)
                    WindowType.PERSONALIZE -> PersonalizeWindowContent(uiState, viewModel)
                    WindowType.TERMINAL -> TerminalWindowContent(uiState, viewModel)
                    WindowType.SERVER_MANAGER -> ServerManagerWindowContent(uiState, viewModel)
                    WindowType.RESOURCE_MONITOR -> ResourceMonitorWindowContent(uiState)
                    WindowType.FILE_EXPLORER -> FileExplorerWindowContent()
                    WindowType.BENCHMARK -> BenchmarkWindowContent(uiState, viewModel)
                    WindowType.NOTEPAD -> NotepadWindowContent()
                    WindowType.PRIVACY -> PrivacyHubWindowContent(viewModel)
                    WindowType.ABOUT -> AboutWindowContent(uiState, viewModel)
                    WindowType.VS_CODE -> VsCodeWindowContent(uiState, viewModel)
                    WindowType.PHONE_MIRROR -> PhoneMirrorWindowContent()
                    WindowType.AI_ASSISTANT -> OfflineKnowledgeWindowContent(uiState, viewModel)
                    WindowType.DEBLOAT_MANAGER -> DebloatManagerContent(uiState, viewModel)
                    else -> TerminalWindowContent(uiState, viewModel)
                }

                if (!window.isMaximized) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(RoundedCornerShape(bottomEnd = 8.dp))
                            .background(accentColor.copy(alpha = 0.72f))
                            // Do not key this on window.width/height: every resize would
                            // recompose, restart this detector and cancel the drag after one
                            // frame. The container bounds are the only input that matters.
                            .pointerInput(window.id, bounds) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    viewModel.resizeWindow(
                                        window.id,
                                        dragAmount.x / density.density,
                                        dragAmount.y / density.density,
                                        bounds
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↘", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BROWSER WINDOW CONTENT
// -------------------------------------------------------------
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWindowContent(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    val context = LocalContext.current
    val startUrl = "https://duckduckgo.com"
    var urlInput by remember { mutableStateOf(startUrl) }
    var loading by remember { mutableStateOf(false) }

    // A real browser: the window is a WebView, so it renders the page it says it is on.
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    loading = true
                    if (url != null) urlInput = url
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    loading = false
                    if (url != null) urlInput = url
                }
            }
            loadUrl(startUrl)
        }
    }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    fun navigateTo(raw: String) {
        val target = when {
            raw.isBlank() -> startUrl
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.contains(".") && !raw.contains(" ") -> "https://" + raw
            else -> "https://duckduckgo.com/?q=" + android.net.Uri.encode(raw)
        }
        urlInput = target
        webView.loadUrl(target)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (webView.canGoBack()) webView.goBack() }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate300, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = { if (webView.canGoForward()) webView.goForward() }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = Slate300, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = { webView.reload() }, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = if (loading) PhonixGreen else Slate300,
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(onClick = { navigateTo(startUrl) }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = PhonixCyan, modifier = Modifier.size(15.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f).height(40.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    focusedBorderColor = PhonixBlueLight,
                    unfocusedBorderColor = Slate700
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { navigateTo(urlInput) })
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Go",
                color = PhonixBlueLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { navigateTo(urlInput) }.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        AndroidView(
            factory = { webView },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

// -------------------------------------------------------------
// DISTRO DOWNLOADER & MANAGER WINDOW
// -------------------------------------------------------------
@Composable
fun LinuxSystemWindowContent(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    val context = LocalContext.current
    val version = remember(uiState.containerStatus) {
        runCatching { com.example.engine.LinuxRuntime.container(context).version() }
            .getOrDefault(com.example.model.DistroCatalog.alpine.version)
    }

    Column(modifier = Modifier.fillMaxSize().background(Slate950).padding(12.dp)) {
        Text("Linux system", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            "Alpine " + version + " aarch64 - the distribution Phonix actually ships",
            color = Slate400,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2530)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(PhonixCyan, Slate800)))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How it runs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "proot gives the guest its own root filesystem inside app storage. No partition, no " +
                        "bootloader change, no root: Android keeps running underneath and the phone apps " +
                        "still work.",
                    color = Slate300,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text("Bundled: busybox, icewm, xterm", color = PhonixCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate900)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Other systems", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "Not available yet. Ubuntu, Debian, Arch, Kali, Fedora and the rest would each need " +
                        "their own tested rootfs and display stack, so Phonix does not offer a download " +
                        "button for them. Packages for this guest are real, though:",
                    color = Slate300,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text("apk add <pkg>  ·  apt-get install <pkg>  ·  pacman -S <pkg>", color = PhonixCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.toggleWindow(WindowType.TERMINAL) },
                colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                modifier = Modifier.height(30.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
            ) {
                Text("Terminal", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.toggleWindow(WindowType.LINUX_DESKTOP) },
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                modifier = Modifier.height(30.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
            ) {
                Text("Linux desktop", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// PERSONALIZATION & APPEARANCE WINDOW (WITH MONITOR MODE & WINDOW TOGGLES)
// -------------------------------------------------------------
@Composable
fun PersonalizeWindowContent(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    val customization = uiState.customization

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Desktop Personalization & Display Settings", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Customize desktop layout, monitor mode, grid snapping, and window visibility", color = Slate400, fontSize = 11.sp)

        // 1. External Monitor & Display Mode
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Display & Monitor Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                MonitorDisplayMode.values().forEach { mode ->
                    val isSelected = customization.displayMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PhonixBlue.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { viewModel.updateDisplayMode(mode) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(mode.title, color = if (isSelected) PhonixBlueLight else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(mode.label, color = Slate400, fontSize = 10.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = PhonixBlueLight, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 2. Window Visibility & Home Screen Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Toggle Windows On / Off", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleAllWindowVisibility() },
                        modifier = Modifier.weight(1f).height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (customization.areAllWindowsHidden) "Restore Windows" else "Show Desktop", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { viewModel.closeAllWindows() },
                        modifier = Modifier.weight(1f).height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Close All Windows", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Open Windows Toggles:", color = Slate300, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                uiState.windows.forEach { win ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(win.title, color = if (!win.isMinimized) Color.White else Slate400, fontSize = 11.sp)
                        Switch(
                            checked = !win.isMinimized,
                            onCheckedChange = { viewModel.toggleWindowById(win.id) },
                            colors = SwitchDefaults.colors(checkedTrackColor = PhonixGreen)
                        )
                    }
                }
            }
        }

        // Shortcut style. This used to be a right-click item, but that menu had grown past the
        // screen, so the tile is an opt-in setting here and the menu keeps only desktop actions.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shortcut Style", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Icon Tiles", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Rounded plate behind each icon. Off (default) is flat artwork only.", color = Slate400, fontSize = 10.sp)
                    }
                    Switch(
                        checked = customization.showIconFrames,
                        onCheckedChange = { viewModel.toggleIconFrames() },
                        colors = SwitchDefaults.colors(checkedTrackColor = PhonixCyan)
                    )
                }
            }
        }

        // 3. Grid Snapping & App Shortcut Layout
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("App Shortcuts Grid Snapping", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Lock Icons to Grid", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("Snaps dragged shortcuts to alignment slots", color = Slate400, fontSize = 10.sp)
                    }
                    Switch(
                        checked = customization.lockIconsToGrid,
                        onCheckedChange = { viewModel.toggleLockIconsToGrid(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PhonixGreen)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.resetShortcutGrid() },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Shortcuts to Default Grid", fontSize = 10.sp)
                }
            }
        }

        // 4. Wallpaper Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brush, contentDescription = null, tint = PhonixBlueLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Desktop Wallpaper", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))

                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) viewModel.setCustomWallpaper(uri)
                }

                DesktopWallpaper.values().forEach { wp ->
                    val isSelected = customization.wallpaper == wp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PhonixBlue.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { viewModel.updateWallpaper(wp) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wp.title, color = if (isSelected) PhonixBlueLight else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(wp.description, color = Slate400, fontSize = 10.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = PhonixBlueLight, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (customization.customWallpaperPath == null) "Choose a Photo from Gallery"
                        else "Choose a Different Photo",
                        fontSize = 10.sp
                    )
                }
                Text(
                    "The picture is copied into this app's private storage and downscaled. It is never uploaded, and it is removed when you uninstall.",
                    color = Slate400,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // 5. Accent Color Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("System Accent Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DesktopAccentColor.values().forEach { ac ->
                        val isSelected = customization.accentColor == ac
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ac.color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Slate700,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.updateAccentColor(ac) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TASKBAR & START MENU
// -------------------------------------------------------------
@Composable
fun PhonixTaskbar(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    isWidescreen: Boolean,
    modifier: Modifier = Modifier
) {
    val currentTime = remember { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        color = Slate950,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, color = Slate800)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Start Button & Active Window Pills (Toggles)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Phoenix Start Menu Button
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (uiState.isStartMenuOpen) SolidColor(PhonixBlue)
                            else Brush.horizontalGradient(listOf(Color(0xFF0F2038), Color(0xFF162D4E)))
                        )
                        .border(1.dp, if (uiState.isStartMenuOpen) PhonixBlueLight else PhonixCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { viewModel.toggleStartMenu() }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "Start", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Window Taskbar Pills with On/Off Toggle Behavior
                uiState.windows.forEach { window ->
                    val isFocused = uiState.activeWindowId == window.id && !window.isMinimized
                    val isVisible = !window.isMinimized

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isFocused) Slate800 else if (isVisible) Color(0xFF16253B) else Color(0xFF0D1522))
                            .border(1.dp, if (isFocused) PhonixBlueLight else if (isVisible) Slate700 else Slate850, RoundedCornerShape(4.dp))
                            .clickable {
                                // Toggle window on / off
                                viewModel.toggleWindowById(window.id)
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isVisible) PhonixGreen else Slate600)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = window.title.take(16),
                                color = if (isFocused) Color.White else if (isVisible) Slate200 else Slate500,
                                fontSize = 10.sp,
                                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Right: System Tray & Show Desktop Toggle Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isWidescreen) {
                    Text(text = "1080p 60Hz", color = PhonixGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Text(text = uiState.localIpAddress, color = PhonixCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                Text(
                    text = if (uiState.isBatteryRemovedDirectPower) "AC ⚡" else "${uiState.telemetry.batteryLevel}%",
                    color = PhonixGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(text = currentTime, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                // Show Desktop (Win+D) Peek Toggle Bar (far right corner like Windows PC)
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (uiState.customization.areAllWindowsHidden) PhonixBlue else Slate800)
                        .border(1.dp, if (uiState.customization.areAllWindowsHidden) PhonixBlueLight else Slate700, RoundedCornerShape(2.dp))
                        .clickable { viewModel.toggleAllWindowVisibility() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.customization.areAllWindowsHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Show Desktop (Toggle all windows)",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

val Slate500 = Color(0xFF64748B)

@Composable
fun PhonixStartMenu(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .heightIn(max = 480.dp)
            .shadow(20.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(PhonixBlue, Slate800)))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = R.drawable.img_phoenix_logo), contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Phonix Workstation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Current OS: ${uiState.currentDistro.name}", color = PhonixCyan, fontSize = 10.sp)
                }
            }

            OutlinedTextField(
                value = uiState.startMenuSearchQuery,
                onValueChange = { viewModel.updateStartMenuSearch(it) },
                placeholder = { Text("Search apps & tools...", color = Slate400, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                },
                trailingIcon = {
                    if (uiState.startMenuSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateStartMenuSearch("") }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(12.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    focusedBorderColor = PhonixBlueLight,
                    unfocusedBorderColor = Slate800
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Slate800)
            Spacer(modifier = Modifier.height(6.dp))

            val allMenuItems = listOf(
                Triple("Linux Desktop (real X11 apps)", Icons.Default.Computer, PhonixGreen) to WindowType.LINUX_DESKTOP,
                Triple("Web Browser (Chromium/Firefox)", Icons.Default.Language, PhonixBlueLight) to WindowType.BROWSER,
                Triple("Linux system", Icons.Default.Apps, PhonixAmber) to WindowType.DISTRO_STORE,
                Triple("Personalize Desktop & Displays", Icons.Default.Palette, Color(0xFFA855F7)) to WindowType.PERSONALIZE,
                Triple("Terminal", Icons.Default.Terminal, TerminalGreen) to WindowType.TERMINAL,
                Triple("Server Suite Hub", Icons.Default.Dns, PhonixCyan) to WindowType.SERVER_MANAGER,
                Triple("Resource Monitor (htop)", Icons.Default.Speed, PhonixGreen) to WindowType.RESOURCE_MONITOR,
                Triple("VirGL 3D Benchmark", Icons.Default.PlayArrow, Color(0xFFF43F5E)) to WindowType.BENCHMARK,
                Triple("File Manager", Icons.Default.Folder, Color(0xFFFBBF24)) to WindowType.FILE_EXPLORER,
                Triple("Notepad Editor", Icons.Default.Code, Slate200) to WindowType.NOTEPAD,
                Triple("Phone Screen (read-only)", Icons.Default.PhoneAndroid, PhonixCyan) to WindowType.PHONE_MIRROR,
                Triple("Local AI", Icons.Default.SmartToy, Color(0xFFA855F7)) to WindowType.AI_ASSISTANT,
                Triple("VS Code Web IDE", Icons.Default.Code, PhonixBlueLight) to WindowType.VS_CODE,
                Triple("About Phonix OS", Icons.Default.Info, PhonixCyan) to WindowType.ABOUT
            )

            val query = uiState.startMenuSearchQuery.trim().lowercase(Locale.ROOT)
            val filteredMenuItems = if (query.isEmpty()) allMenuItems else allMenuItems.filter {
                it.first.first.lowercase(Locale.ROOT).contains(query)
            }

            filteredMenuItems.forEach { (meta, winType) ->
                StartMenuItem(title = meta.first, icon = meta.second, tint = meta.third) {
                    viewModel.toggleWindow(winType)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Slate800)
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = { viewModel.restartContainer() },
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reboot", fontSize = 10.sp)
                }
                Button(
                    onClick = { viewModel.toggleContainerPower() },
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shutdown", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun StartMenuItem(title: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// -------------------------------------------------------------
// TERMINAL WINDOW
// -------------------------------------------------------------
@Composable
fun TerminalWindowContent(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    val listState = rememberLazyListState()

    // Instant scroll, not animated: shell output arrives line by line, and an animation that is
    // restarted on every line spends frames competing with the output it is trying to follow.
    LaunchedEffect(uiState.terminalLines.size) {
        if (uiState.terminalLines.isNotEmpty()) {
            listState.scrollToItem(uiState.terminalLines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBlack)
            .padding(8.dp)
    ) {
        val quickCmds = listOf("uname -a", "cat /etc/os-release", "df -h", "free -m", "ls /", "uptime", "clear")
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCmds.forEach { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Slate800)
                        .clickable { viewModel.executeTerminalCommand(cmd) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cmd,
                        color = PhonixBlueLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(uiState.terminalLines) { line ->
                val textColor = when (line.type) {
                    TerminalLine.LineType.INPUT -> Color.White
                    TerminalLine.LineType.OUTPUT -> Color(0xFFCBD5E1)
                    TerminalLine.LineType.ERROR -> PhonixRed
                    TerminalLine.LineType.SUCCESS -> TerminalGreen
                    TerminalLine.LineType.SYSTEM -> PhonixCyan
                    TerminalLine.LineType.ACCENT -> PhonixBlueLight
                }
                Text(
                    text = line.text,
                    color = textColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                // The prompt names the guest that is actually running; it used to say
                // "root@alpine" even after switching to Debian.
                text = "root@${uiState.currentDistro.id}:~# ",
                color = TerminalGreen,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            TextField(
                value = uiState.terminalInput,
                onValueChange = { viewModel.updateTerminalInput(it) },
                modifier = Modifier.weight(1f).height(44.dp).testTag("terminal_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    viewModel.executeTerminalCommand(uiState.terminalInput)
                })
            )
            IconButton(
                onClick = { viewModel.executeTerminalCommand(uiState.terminalInput) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send Command", tint = PhonixCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// SERVER MANAGER WINDOW
// -------------------------------------------------------------
@Composable
fun ServerManagerWindowContent(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(uiState.serverServices) { service ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate900)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (service.isRunning) PhonixGreen else Slate600))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(service.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("Port :${service.port} • ${service.category}", color = Slate400, fontSize = 10.sp)
                    }
                    Switch(checked = service.isRunning, onCheckedChange = { viewModel.toggleServerService(service.id) })
                }
            }
        }
    }
}

// -------------------------------------------------------------
// RESOURCE MONITOR WINDOW
// -------------------------------------------------------------
@Composable
fun ResourceMonitorWindowContent(uiState: PhonixUiState) {
    val t = uiState.telemetry
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Live device readings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        if (!t.hasReading) {
            Text("Reading the sensors...", color = Slate400, fontSize = 11.sp)
        } else {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate900)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("CPU core frequency utilisation: ${t.cpuPercent}%", color = PhonixBlueLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { t.cpuPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = PhonixBlueLight,
                        trackColor = Slate800
                    )
                    Text(
                        "Average of each core's current clock against its maximum; Android does not expose /proc/stat to apps.",
                        color = Slate400,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate900)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("RAM: ${t.ramUsedMb} MB / ${t.ramTotalMb} MB", color = PhonixCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { if (t.ramTotalMb > 0) (t.ramUsedMb.toFloat() / t.ramTotalMb).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = PhonixCyan,
                        trackColor = Slate800
                    )
                    Text("From /proc/meminfo (MemTotal minus MemAvailable).", color = Slate400, fontSize = 9.sp)
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate900)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = if (t.hottestZoneC > 0f) "Hottest thermal zone: ${t.hottestZoneC.toInt()}°C" else "Hottest thermal zone: -",
                        color = if (t.hottestZoneC >= 75f) PhonixRed else PhonixGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Read from /sys/class/thermal. Vendors label these zones differently, so this is the warmest one, not a specific core.",
                        color = Slate400,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BENCHMARK & NOTEPAD & ABOUT WINDOWS
// -------------------------------------------------------------
@Composable
fun BenchmarkWindowContent(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Container benchmark", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(
            "Runs dd inside the Alpine guest and times a shell loop. Real numbers, no score.",
            color = Slate400,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )

        Button(
            onClick = { viewModel.runContainerBenchmark() },
            enabled = !uiState.isBenchmarkRunning,
            colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue)
        ) {
            Text(if (uiState.isBenchmarkRunning) "Measuring..." else "Run benchmark")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalBlack)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            uiState.benchmarkLog.forEach { line ->
                Text(line, color = Slate300, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun FileExplorerWindowContent() {
    val paths = listOf("/", "/etc", "/root", "/tmp", "/usr/share/novnc", "/var")
    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Text("Guest filesystem", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(
            "These paths exist inside the active Linux guest. Browsing happens in the Terminal, " +
                "or install a file manager with the guest's own package manager.",
            color = Slate400,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(paths) { path ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(path, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun NotepadWindowContent() {
    var text by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Scratch pad for this session. It is not saved anywhere yet.",
            color = Slate400,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text("Type here...", color = Slate600, fontSize = 12.sp) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TerminalBlack,
                unfocusedContainerColor = TerminalBlack,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun AboutWindowContent(uiState: PhonixUiState, viewModel: PhonixViewModel) {
    val context = LocalContext.current
    val guest = remember {
        runCatching { com.example.engine.LinuxRuntime.container(context).version() }.getOrNull()
    }
    val appVersion = remember {
        runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
            "${pi.versionName} (build $code)"
        }.getOrDefault("1.5 (build 6)")
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = R.drawable.img_phoenix_logo), contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Phonix OS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("A desktop for recycled Android phones", color = Slate400, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate900)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("This system & app", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                AboutRow("Version", appVersion)
                AboutRow("Package", context.packageName)
                AboutRow("Model", android.os.Build.MODEL)
                AboutRow("Android", android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")")
                AboutRow("ABI", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "-")
                AboutRow("Kernel", System.getProperty("os.version") ?: "-")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Software Updates Card (User-Controlled GitHub Releases)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_software_updates"),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(16.dp))
                        Text("Software Updates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text("Manual Only", color = PhonixGreen, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Manual update check. Nothing installs in the background.",
                    color = Slate400,
                    fontSize = 10.sp
                )
                val feedHost = runCatching {
                    android.net.Uri.parse(com.example.BuildConfig.UPDATE_FEED_URL).host
                }.getOrNull()
                AboutRow("Update feed", feedHost ?: "not configured in this build")

                when (uiState.updateState) {
                    UpdateCheckState.IDLE -> {
                        Button(
                            onClick = { viewModel.checkAppUpdates() },
                            modifier = Modifier.fillMaxWidth().testTag("btn_check_updates"),
                            colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check for Updates", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    UpdateCheckState.CHECKING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PhonixCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking the update feed...", color = Slate300, fontSize = 11.sp)
                        }
                    }
                    UpdateCheckState.UP_TO_DATE -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Slate950, RoundedCornerShape(6.dp)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                uiState.updateInfo?.releaseNotes ?: "System is up to date (v1.2).",
                                color = Slate200,
                                fontSize = 10.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.checkAppUpdates() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Check Again", fontSize = 10.sp)
                        }
                    }
                    UpdateCheckState.UPDATE_AVAILABLE -> {
                        val update = uiState.updateInfo
                        Column(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF0F2942), RoundedCornerShape(8.dp)).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Version Available: ${update?.latestVersionName ?: "Update"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Text(update?.releaseNotes ?: "Release updates and improvements.", color = Slate300, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.downloadAndInstallUpdate(context) },
                                modifier = Modifier.fillMaxWidth().testTag("btn_install_update"),
                                colors = ButtonDefaults.buttonColors(containerColor = PhonixGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download & Install", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "The APK is checked against the feed's SHA-256 and must carry this " +
                                    "app's signing certificate. Android asks before installing.",
                                color = Slate400,
                                fontSize = 9.sp
                            )
                        }
                    }
                    UpdateCheckState.DOWNLOADING, UpdateCheckState.READY_TO_INSTALL -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Slate950, RoundedCornerShape(6.dp)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.updateState == UpdateCheckState.DOWNLOADING) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PhonixCyan, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                uiState.updateInfo?.releaseNotes ?: "Preparing the update...",
                                color = Slate200,
                                fontSize = 10.sp
                            )
                        }
                    }
                    else -> {
                        Text(
                            uiState.updateInfo?.releaseNotes ?: "Could not check for updates.",
                            color = Slate400,
                            fontSize = 10.sp
                        )
                        OutlinedButton(
                            onClick = { viewModel.checkAppUpdates() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate900)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Linux guest", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                // The active guest, not a hardcoded Alpine: a switch to Debian must change this line.
                AboutRow(
                    "Distribution",
                    uiState.currentDistro.name + if (guest != null) " · " + guest else " · not running"
                )
                AboutRow(
                    "Container",
                    if (uiState.containerStatus == com.example.viewmodel.ContainerStatus.RUNNING) "running" else "stopped"
                )
                AboutRow("Method", "proot, unrooted")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Phonix is an interface over Android, not a replacement for it. Nothing here modifies the " +
                "bootloader or the partition table.",
            color = Slate400,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

// -------------------------------------------------------------
// VS CODE WEB WINDOW CONTENT (Local ARM64 Alpine loopback)
// -------------------------------------------------------------
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VsCodeWindowContent(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    val context = LocalContext.current
    val ideUrl = "http://127.0.0.1:8000"
    var isReloading by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    isReloading = false
                }
            }
            loadUrl(ideUrl)
        }
    }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    // The first load can happen before the server exists. Reload only after the socket probe
    // has actually connected, so a successful start replaces the connection-refused page.
    LaunchedEffect(uiState.vsCodeServerRunning) {
        if (uiState.vsCodeServerRunning) webView.reload()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        // VS Code Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252526))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Code, contentDescription = null, tint = PhonixBlueLight, modifier = Modifier.size(18.dp))
                Text("VS Code Web IDE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .background(
                            when {
                                uiState.vsCodeServerRunning -> PhonixGreen.copy(alpha = 0.2f)
                                uiState.vsCodeServerProbing -> PhonixAmber.copy(alpha = 0.2f)
                                else -> Slate800
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        when {
                            uiState.vsCodeServerRunning -> "Listening on 127.0.0.1:8000"
                            uiState.vsCodeServerProbing -> "Waiting for :8000..."
                            else -> "Not listening on :8000"
                        },
                        color = when {
                            uiState.vsCodeServerRunning -> PhonixGreen
                            uiState.vsCodeServerProbing -> PhonixAmber
                            else -> Slate400
                        },
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!uiState.vsCodeServerRunning && !uiState.vsCodeServerProbing) {
                    Button(
                        onClick = { viewModel.startVsCodeServer() },
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Start & Probe", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (uiState.vsCodeServerProbing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = PhonixAmber,
                        strokeWidth = 2.dp
                    )
                }
                IconButton(
                    onClick = {
                        isReloading = true
                        webView.reload()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Slate300, modifier = Modifier.size(16.dp))
                }
            }
        }

        // The badge is a measured fact, not a request. Show why it is not green.
        if (!uiState.vsCodeServerRunning) {
            Text(
                uiState.vsCodeServerStatus,
                color = if (uiState.vsCodeServerProbing) PhonixAmber else Color(0xFFFCA5A5),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A1A1A))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        // Web View IDE Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// -------------------------------------------------------------
// LOCAL AI WINDOW CONTENT
// -------------------------------------------------------------
@Composable
fun OfflineKnowledgeWindowContent(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    var promptInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val lastMessageLength = uiState.aiChatMessages.lastOrNull()?.text?.length ?: 0
    val ready = uiState.aiEngineState == AiEngineState.READY
    var agentMode by remember { mutableStateOf(false) }
    // The notes and replies render at 8-13sp, which is unreadable on a 1080p monitor from couch
    // distance. Zoom is a real scale of the content, not just the font, so taps stay proportional.
    var zoom by remember { mutableStateOf(1f) }

    // Same reason as the terminal: the reply streams a token at a time, so an animated scroll
    // would restart dozens of times a second. A jump per update is cheaper and steadier.
    LaunchedEffect(uiState.aiChatMessages.size, lastMessageLength) {
        if (uiState.aiChatMessages.isNotEmpty()) {
            listState.scrollToItem(uiState.aiChatMessages.lastIndex)
        }
    }
    LaunchedEffect(Unit) { viewModel.refreshLocalAi() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .clipToBounds()
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth(1f / zoom)
            .fillMaxHeight(1f / zoom)
            .graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .background(Slate950)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(18.dp))
                Column {
                    Text(
                        if (ready) "Local AI Assistant" else "Offline Knowledge Base",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        if (ready) "Qwen2.5 1.5B - CPU - 127.0.0.1:8081" else "Built-in notes work now; the local model is opt-in",
                        color = Slate400,
                        fontSize = 8.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "A-",
                    color = Slate300,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { zoom = (zoom - 0.15f).coerceAtLeast(0.75f) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
                Text(
                    "${(zoom * 100).toInt()}%",
                    color = Slate400,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .clickable { zoom = 1f }
                        .padding(horizontal = 2.dp, vertical = 3.dp)
                )
                Text(
                    "A+",
                    color = Slate300,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { zoom = (zoom + 0.15f).coerceAtMost(2f) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            AiStatePill(uiState.aiEngineState)
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (ready) {
            AiReadyStrip(uiState, viewModel)
        } else {
            AiSetupCard(uiState, viewModel)
        }

        Spacer(modifier = Modifier.height(6.dp))

        AiToolbar(uiState, viewModel, agentMode) { agentMode = it }

        Spacer(modifier = Modifier.height(6.dp))

        if (agentMode) {
            AgentPanel(uiState, viewModel, Modifier.weight(1f))
        } else {
        // The notes remain the fallback and the grounding source for the model.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "💧 Water Filtration",
                "🛡️ Rule of 3s Survival",
                "🌱 Farming & Calorie Crops",
                "📻 Emergency Radio Frequencies",
                "💻 Offline Coding Cheatsheet",
                "⚡ 12V Buck & Solar Sizing",
                "🩺 First Aid & ORS Salts",
                "🌡️ Check Thermals"
            ).forEach { query ->
                Box(
                    modifier = Modifier
                        .background(Slate900, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                        .clickable { viewModel.sendAiMessage(query) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(query, color = Slate300, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.aiChatMessages) { msg ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            when {
                                msg.isUser -> "You"
                                msg.modelGenerated -> "Local model"
                                else -> "Offline notes"
                            },
                            color = if (msg.isUser) PhonixCyan else Color(0xFFC084FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (msg.grounded) {
                            Text("· notes", color = Slate300, fontSize = 10.sp)
                        }
                        msg.tokensPerSecond?.let { speed ->
                            Text(
                                "· " + String.format(Locale.US, "%.1f", speed) +
                                    if (msg.tokenCountExact) " tok/s" else " chunks/s",
                                color = Slate300,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .widthIn(max = 380.dp)
                            .background(
                                if (msg.isUser) PhonixBlue else Slate900,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (msg.isUser) PhonixBlueLight else Slate800,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Selectable, because the useful thing to do with a generated
                            // program is copy it into the terminal.
                            SelectionContainer {
                                Text(
                                    if (msg.text.isBlank() && msg.modelGenerated) "▍" else msg.text,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            if (!msg.isUser && msg.text.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Copy reply",
                                        color = PhonixCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { viewModel.copyText(msg.text, "reply") }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    if (msg.suggestedCommand != null) {
                                        Text(
                                            "Copy command",
                                            color = PhonixGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { viewModel.copyText(msg.suggestedCommand, "command") }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            msg.suggestedCommand?.let { cmd ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = TerminalBlack)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Recommended Terminal Command:", color = Slate400, fontSize = 9.sp)
                                        Text(
                                            cmd,
                                            color = PhonixGreen,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Button(
                                            onClick = { viewModel.executeSuggestedAiCommand(cmd) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("⚡ Run in Terminal", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isAiGenerating) {
                item {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFFA855F7), strokeWidth = 2.dp)
                        Text(
                            if (ready) "Generating locally..." else "Looking up notes...",
                            color = Slate300,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (ready) "Ask the local model..." else "Ask the notes...",
                        color = Slate500,
                        fontSize = 11.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedBorderColor = Color(0xFFA855F7),
                    unfocusedBorderColor = Slate800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            IconButton(
                onClick = {
                    if (uiState.isAiGenerating) {
                        viewModel.stopAiGeneration()
                    } else if (promptInput.isNotBlank()) {
                        viewModel.sendAiMessage(promptInput)
                        promptInput = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (uiState.isAiGenerating) Color(0xFFB45309) else Color(0xFFA855F7),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    if (uiState.isAiGenerating) Icons.Default.Close else Icons.Default.Send,
                    contentDescription = if (uiState.isAiGenerating) "Stop" else "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        }
        if (uiState.showKnowledgeLibrary) {
            KnowledgeLibraryDialog(uiState, viewModel)
        }
    }
    }
}

// -------------------------------------------------------------
// AI TOOLBAR: chat vs agent, library, and the knowledge scope
// -------------------------------------------------------------
@Composable
private fun AiToolbar(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    agentMode: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(false to "Chat", true to "Agent").forEach { (isAgent, label) ->
                val selected = agentMode == isAgent
                Box(
                    modifier = Modifier
                        .background(if (selected) Color(0xFF4C1D95) else Slate900, RoundedCornerShape(8.dp))
                        .border(1.dp, if (selected) Color(0xFFA855F7) else Slate800, RoundedCornerShape(8.dp))
                        .clickable { onModeChange(isAgent) }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (isAgent) "⌘ Agent" else "Chat",
                        color = if (selected) Color.White else Slate300,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .background(if (uiState.showKnowledgeLibrary) Color(0xFF0C3256) else Slate900, RoundedCornerShape(8.dp))
                    .border(1.dp, if (uiState.showKnowledgeLibrary) PhonixCyan else Slate800, RoundedCornerShape(8.dp))
                    .clickable { viewModel.toggleKnowledgeLibrary() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("📚 Library", color = PhonixCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Scope decides which shelf the model may read. "All" is the deliberately slower option.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Knowledge:", color = Slate400, fontSize = 9.sp)
            Box(
                modifier = Modifier
                    .background(if (uiState.aiKnowledgeScope == null) Color(0xFF312E81) else Slate900, RoundedCornerShape(6.dp))
                    .border(1.dp, if (uiState.aiKnowledgeScope == null) Color(0xFF818CF8) else Slate800, RoundedCornerShape(6.dp))
                    .clickable { viewModel.setAiKnowledgeScope(null) }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("All shelves", color = Slate200, fontSize = 9.sp)
            }
            KnowledgeCategory.entries.forEach { category ->
                val selected = uiState.aiKnowledgeScope == category
                Box(
                    modifier = Modifier
                        .background(if (selected) Color(0xFF312E81) else Slate900, RoundedCornerShape(6.dp))
                        .border(1.dp, if (selected) Color(0xFF818CF8) else Slate800, RoundedCornerShape(6.dp))
                        .clickable { viewModel.setAiKnowledgeScope(category) }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("${category.glyph} ${category.title}", color = if (selected) Color.White else Slate400, fontSize = 9.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SHELL AGENT: one command per turn, approved by the user
// -------------------------------------------------------------
@Composable
private fun AgentPanel(uiState: PhonixUiState, viewModel: PhonixViewModel, modifier: Modifier = Modifier) {
    var task by remember { mutableStateOf(uiState.agentTask) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "The agent asks the local model for exactly one shell command at a time, runs it in the Linux guest, and reads the output before choosing the next step. Nothing runs until you press Run.",
            color = Slate400,
            fontSize = 9.sp,
            lineHeight = 13.sp
        )

        OutlinedTextField(
            value = task,
            onValueChange = { task = it; viewModel.setAgentTask(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp, max = 80.dp),
            placeholder = {
                Text("e.g. write a snake game to ~/snake.py and run it once", color = Slate500, fontSize = 11.sp)
            },
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedBorderColor = Color(0xFFA855F7),
                unfocusedBorderColor = Slate800
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { viewModel.startAgentTask(task) },
                enabled = !uiState.agentRunning && uiState.agentPendingCommand == null && task.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { viewModel.stopAgent() },
                enabled = uiState.agentRunning || uiState.agentPendingCommand != null,
                modifier = Modifier.height(32.dp)
            ) {
                Text("Stop", fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = uiState.agentAutoRun,
                    onCheckedChange = { viewModel.setAgentAutoRun(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = PhonixAmber)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Auto-run", color = if (uiState.agentAutoRun) PhonixAmber else Slate400, fontSize = 9.sp)
            }
        }

        if (uiState.agentStatus.isNotBlank()) {
            Text(uiState.agentStatus, color = PhonixCyan, fontSize = 10.sp, lineHeight = 14.sp)
        }

        val pending = uiState.agentPendingCommand
        if (pending != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalBlack),
                modifier = Modifier.fillMaxWidth().border(1.dp, PhonixAmber, RoundedCornerShape(8.dp))
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Next command", color = PhonixAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    SelectionContainer {
                        Text(pending, color = PhonixGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.approveAgentCommand() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Run it", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { viewModel.rejectAgentCommand() }, modifier = Modifier.height(30.dp)) {
                            Text("Reject", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.copyText(pending, "command") },
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Copy", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(uiState.agentSteps) { step ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SelectionContainer {
                            Text("$ ${step.command}", color = PhonixCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        SelectionContainer {
                            Text(step.output, color = Slate300, fontSize = 9.sp, fontFamily = FontFamily.Monospace, lineHeight = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// LIBRARY: browse the offline shelves, then read a document
// -------------------------------------------------------------
@Composable
private fun KnowledgeLibraryDialog(uiState: PhonixUiState, viewModel: PhonixViewModel) {
    val doc = uiState.openKnowledgeDocId?.let { KnowledgeLibrary.document(it) }

    Dialog(onDismissRequest = { viewModel.toggleKnowledgeLibrary() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
            color = Slate950,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        doc?.title ?: "Offline library",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.toggleKnowledgeLibrary() }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }

                if (doc == null) {
                    var selectedTier by remember { mutableStateOf(KnowledgeLibrary.activeTier) }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.model.KnowledgeTier.entries.forEach { tier ->
                            val isSel = selectedTier == tier
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) PhonixCyan.copy(alpha = 0.25f) else Slate900)
                                    .border(1.dp, if (isSel) PhonixCyan else Slate800, RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedTier = tier
                                        KnowledgeLibrary.setTier(tier)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "${tier.title} (${com.example.model.KnowledgeLibrary.sizeLabel(tier)})",
                                    color = if (isSel) PhonixCyan else Slate400,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Text(
                        "Everything here works with no model, no network and no second device. ${KnowledgeLibrary.docsFor(null, selectedTier).size} active documents on ${KnowledgeCategory.entries.size} shelves.",
                        color = Slate400,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KnowledgeCategory.entries.forEach { category ->
                            val shelfDocs = KnowledgeLibrary.docsFor(category, selectedTier)
                            if (shelfDocs.isNotEmpty()) {
                                item {
                                    Text(
                                        "${category.glyph} ${category.title} (${shelfDocs.size})",
                                        color = PhonixAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                items(shelfDocs) { entry ->

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Slate900),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openKnowledgeDoc(entry.id) }
                                ) {
                                    Column(Modifier.padding(9.dp)) {
                                        Text(entry.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(entry.summary, color = Slate400, fontSize = 9.sp, lineHeight = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                    }
                } else {
                    Text("${doc.category.glyph} ${doc.category.title}", color = PhonixAmber, fontSize = 9.sp)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        item {
                            SelectionContainer {
                                Text(doc.body, color = Slate200, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.openKnowledgeDoc(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Back", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.copyText(doc.body, "document") },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Copy", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.askAboutDocument(doc) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Ask the model", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiStatePill(state: AiEngineState) {
    val label: String
    val background: Color
    val border: Color
    when (state) {
        AiEngineState.READY -> {
            label = "MODEL READY"; background = Color(0xFF064E3B); border = Color(0xFF10B981)
        }
        AiEngineState.DOWNLOADING -> {
            label = "DOWNLOADING"; background = Color(0xFF3B2A12); border = PhonixAmber
        }
        AiEngineState.INSTALLING, AiEngineState.STARTING, AiEngineState.CHECKING -> {
            label = "SETTING UP"; background = Color(0xFF3B2A12); border = PhonixAmber
        }
        AiEngineState.ERROR -> {
            label = "ERROR"; background = Color(0xFF3B1717); border = Color(0xFFEF4444)
        }
        AiEngineState.STOPPED -> {
            label = "STOPPED"; background = Slate800; border = Slate500
        }
        AiEngineState.NOT_SET_UP -> {
            label = "OFFLINE NOTES"; background = Slate800; border = Slate500
        }
    }
    Surface(shape = RoundedCornerShape(10.dp), color = background, border = BorderStroke(1.dp, border)) {
        Text(
            label,
            color = if (state == AiEngineState.ERROR) Color(0xFFFCA5A5) else Color(0xFFFEF3C7),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AiSetupCard(uiState: PhonixUiState, viewModel: PhonixViewModel) {
    val busy = uiState.aiSetupRunning
    val state = uiState.aiEngineState
    val title = when (state) {
        AiEngineState.CHECKING -> "Checking the local model"
        AiEngineState.INSTALLING -> "Installing the model runtime"
        AiEngineState.DOWNLOADING -> "Downloading the model"
        AiEngineState.STARTING -> "Loading the model"
        AiEngineState.ERROR -> "Local model needs attention"
        AiEngineState.STOPPED -> "Local model installed"
        else -> "Run a real model on this phone"
    }
    val percent = (uiState.aiDownloadProgress * 100).toInt().coerceIn(0, 100)
    val canStart = uiState.aiModelPresent && uiState.aiServerPackagePresent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111827), RoundedCornerShape(8.dp))
            .border(1.dp, Slate800, RoundedCornerShape(8.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(uiState.aiEngineMessage, color = Slate300, fontSize = 9.sp, lineHeight = 12.sp)
            }
            if (state == AiEngineState.ERROR) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(15.dp))
            }
        }

        if (busy) {
            if (state == AiEngineState.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { uiState.aiDownloadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFA855F7),
                    trackColor = Slate800
                )
                Text(
                    "${uiState.aiDownloadedBytes / (1024L * 1024L)} of " +
                        "${uiState.aiDownloadTotalBytes / (1024L * 1024L)} MB - $percent%",
                    color = Slate400,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFA855F7),
                    trackColor = Slate800
                )
            }
        }

        Text(
            "About 1.0 GB download and 1.3 GB RAM while loaded. CPU inference; nothing leaves the phone.",
            color = Slate500,
            fontSize = 8.sp,
            lineHeight = 11.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { viewModel.setupLocalAi() },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    when {
                        busy -> "Working..."
                        canStart -> "Start local AI"
                        state == AiEngineState.ERROR -> "Retry setup"
                        else -> "Set up local AI"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (busy) {
                OutlinedButton(onClick = { viewModel.cancelLocalAiSetup() }, shape = RoundedCornerShape(6.dp)) {
                    Text("Cancel", fontSize = 10.sp)
                }
            } else if (state == AiEngineState.ERROR) {
                OutlinedButton(onClick = { viewModel.refreshLocalAi() }, shape = RoundedCornerShape(6.dp)) {
                    Text("Check again", fontSize = 10.sp)
                }
            }
        }

        if (state == AiEngineState.ERROR && uiState.aiSetupLog.isNotEmpty()) {
            Text(
                uiState.aiSetupLog.takeLast(3).joinToString("\n"),
                color = Color(0xFFFCA5A5),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 11.sp,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun AiReadyStrip(uiState: PhonixUiState, viewModel: PhonixViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF052E1B), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF166534), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                LocalAiEngine.MODEL_DISPLAY_NAME,
                color = Color(0xFFD1FAE5),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                uiState.aiModelTokensPerSecond?.let {
                    String.format(Locale.US, "Health passed; measured %.1f tok/s", it)
                } ?: "Health check passed; server is listening on loopback",
                color = Color(0xFF86EFAC),
                fontSize = 8.sp
            )
            Text(
                "AI can err; safety-critical steps belong to the curated notes.",
                color = Color(0xFFFDE68A),
                fontSize = 7.sp,
                lineHeight = 9.sp
            )
        }
        Text(
            "Stop",
            color = Color(0xFFFCA5A5),
            fontSize = 9.sp,
            modifier = Modifier
                .clickable { viewModel.stopLocalAi() }
                .padding(horizontal = 5.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Slate400, fontSize = 10.sp)
        Text(value, color = PhonixCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
