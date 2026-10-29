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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import com.example.model.ShortcutShape
import com.example.model.ShortcutBgStyle
import com.example.model.DesktopContextMenuData
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.engine.TerminalLine
import com.example.model.DesktopAccentColor
import com.example.model.DesktopShortcut
import com.example.model.DesktopWallpaper
import com.example.model.DesktopWindow
import com.example.model.IconSizeMode
import com.example.model.MonitorDisplayMode
import com.example.model.TaskbarPosition
import com.example.model.TaskbarStyle
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
                    accentColor = customization.accentColor.color,
                    showGrid = customization.showDesktopGrid,
                    lockToGrid = customization.lockIconsToGrid,
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
                            top = if (customization.taskbarPosition == TaskbarPosition.TOP) 54.dp else 10.dp,
                            bottom = if (customization.taskbarPosition == TaskbarPosition.BOTTOM) 54.dp else 12.dp,
                            start = 12.dp,
                            end = 12.dp
                        )
                ) {
                    // Draggable icons with per-shortcut shape, size, color, background & context menu
                    customization.shortcuts.forEach { shortcut ->
                        val appIcon = uiState.installedApps.find { it.packageName == shortcut.launchPackage }?.iconDrawable
                        DraggableDesktopShortcut(
                            shortcut = shortcut,
                            iconSize = customization.iconSize,
                            showLabel = customization.showDesktopLabels,
                            accentColor = customization.accentColor.color,
                            appIconDrawable = appIcon,
                            onMove = { dx, dy -> viewModel.moveShortcut(shortcut.id, dx, dy) },
                            onDrop = { x, y -> viewModel.dropShortcut(shortcut.id, x, y) },
                            onClick = {
                                val density = desktopContext.resources.displayMetrics.density
                                val bounds = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    android.graphics.Rect(
                                        (shortcut.posX * density).toInt(),
                                        (shortcut.posY * density).toInt(),
                                        ((shortcut.posX + 380f) * density).toInt(),
                                        ((shortcut.posY + 320f) * density).toInt()
                                    )
                                } else null
                                viewModel.launchShortcutOrHostApp(desktopContext, shortcut, bounds)
                            },
                            onRightClick = { x, y -> viewModel.openShortcutContextMenu(shortcut.id, x, y) }
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
                uiState.windows.filter { !it.isMinimized }.forEach { window ->
                    DesktopWindowFrame(
                        window = window,
                        isActive = uiState.activeWindowId == window.id,
                        accentColor = customization.accentColor.color,
                        uiState = uiState,
                        viewModel = viewModel
                    )
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

                // 6. Windows 11 Fluent Acrylic Start Menu (Centered Floating Card)
                if (uiState.isStartMenuOpen) {
                    Windows11StartMenu(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-56).dp)
                    )
                }

                // 7. Windows 11 Centered Taskbar (App Pointers, System Tray & Show Desktop)
                Windows11Taskbar(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.BottomCenter)
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
    accentColor: Color,
    showGrid: Boolean,
    lockToGrid: Boolean,
    onDesktopClicked: () -> Unit,
    onDesktopLongPressed: (Float, Float) -> Unit
) {
    val density = LocalDensity.current.density
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onDesktopClicked() },
                    onLongPress = { offset ->
                        onDesktopLongPressed(offset.x / density, offset.y / density)
                    }
                )
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

            DesktopWallpaper.MATRIX_DEEP_BLUE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF030712), Color(0xFF0B192C), Color(0xFF030712))))
                ) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = PhonixCyan.copy(alpha = 0.35f), modifier = Modifier.size(90.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("MATRIX ARM64 SHELL", color = PhonixCyan.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            DesktopWallpaper.OBSIDIAN_NIGHT -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(150.dp)
                            .border(1.dp, Slate800, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PHONIX", color = Slate700, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 4.sp)
                    }
                }
            }

            DesktopWallpaper.RETRO_CLASSIC_TEAL -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF008080)))
            }

            DesktopWallpaper.SYNTHWAVE_NEON -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0xFF1E1035), Color(0xFF0B1B3D), Color(0xFF050515)))
                    )
                )
            }
        }

        // Snapping Grid Overlay (Faint dot alignment for locking shortcuts)
        if (showGrid && lockToGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 84.dp.toPx()
                val dotRadius = 1.2.dp.toPx()
                var x = 16.dp.toPx()
                while (x < size.width) {
                    var y = 16.dp.toPx()
                    while (y < size.height) {
                        drawCircle(
                            color = Color(0x2238BDF8),
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += step
                    }
                    x += step
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
    onMove: (Float, Float) -> Unit,
    onDrop: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onRightClick: (Float, Float) -> Unit
) {
    val effectiveSize = shortcut.customSize ?: iconSize
    val sizeDp = effectiveSize.sizeDp.dp
    val shapeRadius = shortcut.customShape.cornerRadiusDp.dp

    val icon = when (shortcut.windowType) {
        WindowType.BROWSER -> Icons.Default.Language
        WindowType.LINUX_DESKTOP -> Icons.Default.Computer
        WindowType.DISTRO_STORE -> Icons.Default.Apps
        WindowType.PERSONALIZE -> Icons.Default.Palette
        WindowType.TERMINAL -> Icons.Default.Terminal
        WindowType.SERVER_MANAGER -> Icons.Default.Dns
        WindowType.RESOURCE_MONITOR -> Icons.Default.Speed
        WindowType.FILE_EXPLORER -> Icons.Default.Folder
        WindowType.BENCHMARK -> Icons.Default.PlayArrow
        WindowType.NOTEPAD -> Icons.Default.Code
        else -> Icons.Default.Computer
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
        else -> PhonixBlueLight
    }

    val iconTint = shortcut.customColor?.color ?: defaultIconTint

    val density = LocalDensity.current.density
    var isDragging by remember(shortcut.id) { mutableStateOf(false) }
    var dragOffsetX by remember(shortcut.id) { mutableFloatStateOf(0f) }
    var dragOffsetY by remember(shortcut.id) { mutableFloatStateOf(0f) }

    val shortcutWidth = (sizeDp + 26.dp).coerceAtLeast(76.dp)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = ((shortcut.posX * density) + dragOffsetX).roundToInt(),
                    y = ((shortcut.posY * density) + dragOffsetY).roundToInt()
                )
            }
            .zIndex(if (isDragging) 100f else 1f)
            .shadow(
                elevation = if (isDragging) 14.dp else 2.dp,
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
                    .clip(RoundedCornerShape(shapeRadius))
                    .background(iconTint.copy(alpha = 0.16f))
                    .border(1.dp, iconTint.copy(alpha = 0.55f), RoundedCornerShape(shapeRadius)),
                contentAlignment = Alignment.Center
            ) {
                val realIcon = appIconDrawable
                if (realIcon != null) {
                    Image(
                        bitmap = remember(realIcon) {
                            realIcon.toBitmap(width = 96, height = 96).asImageBitmap()
                        },
                        contentDescription = shortcut.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(sizeDp * 0.72f)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = shortcut.title,
                        tint = iconTint,
                        modifier = Modifier.size(sizeDp * 0.55f)
                    )
                }
            }
            if (showLabel) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = shortcut.title,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    val version = remember(uiState.containerStatus) {
        runCatching { com.example.engine.LinuxRuntime.container(context).version() }
            .getOrDefault(com.example.model.DistroCatalog.alpine.version)
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
            Text(text = "Alpine Linux " + version, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
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
    viewModel: PhonixViewModel
) {
    Box(
        modifier = Modifier
            .offset {
                if (window.isMaximized) IntOffset(0, 0)
                else IntOffset(window.posX.toInt(), window.posY.toInt())
            }
            .then(
                if (window.isMaximized) Modifier.fillMaxSize().padding(bottom = 48.dp)
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
                        .pointerInput(window.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.moveWindow(window.id, dragAmount.x, dragAmount.y)
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
                    WindowType.DISTRO_STORE -> LinuxSystemWindowContent(uiState, viewModel)
                    WindowType.PERSONALIZE -> PersonalizeWindowContent(uiState, viewModel)
                    WindowType.TERMINAL -> TerminalWindowContent(uiState, viewModel)
                    WindowType.SERVER_MANAGER -> ServerManagerWindowContent(uiState, viewModel)
                    WindowType.RESOURCE_MONITOR -> ResourceMonitorWindowContent(uiState)
                    WindowType.FILE_EXPLORER -> FileExplorerWindowContent()
                    WindowType.BENCHMARK -> BenchmarkWindowContent(uiState, viewModel)
                    WindowType.NOTEPAD -> NotepadWindowContent()
                    WindowType.ABOUT -> AboutWindowContent(uiState)
                    else -> TerminalWindowContent(uiState, viewModel)
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
                Text("apk add <package>   |   apk search <name>", color = PhonixCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
                        Text("Lock Icons to Grid (84dp)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
            .shadow(20.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(PhonixBlue, Slate800)))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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

    LaunchedEffect(uiState.terminalLines.size) {
        if (uiState.terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.terminalLines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBlack)
            .padding(8.dp)
    ) {
        val quickCmds = listOf("uname -a", "apk info", "df -h", "free -m", "ls /", "uptime", "clear")
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
                text = "root@alpine:~# ",
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
            "These paths exist inside the Alpine container. Browsing happens in the Terminal (busybox ls), " +
                "or install a file manager with apk add.",
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
fun AboutWindowContent(uiState: PhonixUiState) {
    val context = LocalContext.current
    val guest = remember {
        runCatching { com.example.engine.LinuxRuntime.container(context).version() }.getOrNull()
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
                Text("This device", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                AboutRow("Model", android.os.Build.MODEL)
                AboutRow("Android", android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")")
                AboutRow("ABI", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "-")
                AboutRow("Kernel", System.getProperty("os.version") ?: "-")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate900)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Linux guest", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                AboutRow("Distribution", if (guest != null) "Alpine " + guest else "not installed")
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

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Slate400, fontSize = 10.sp)
        Text(value, color = PhonixCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
