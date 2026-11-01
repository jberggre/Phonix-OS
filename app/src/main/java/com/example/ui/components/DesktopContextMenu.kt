package com.example.ui.components

import android.content.Intent
import android.os.Build
import com.example.engine.LinuxRuntime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DesktopAccentColor
import com.example.model.DesktopContextMenuData
import com.example.model.DesktopShortcut
import com.example.model.IconSizeMode
import com.example.model.ShortcutBgStyle
import com.example.model.ShortcutShape
import com.example.model.TaskbarPosition
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixBlueLight
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.PhonixRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel
import kotlin.math.roundToInt

@Composable
fun DesktopContextMenu(
    menuData: DesktopContextMenuData,
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when (menuData) {
        is DesktopContextMenuData.ShortcutMenu -> {
            val shortcut = uiState.customization.shortcuts.find { it.id == menuData.shortcutId } ?: return
            ShortcutContextMenuView(
                shortcut = shortcut,
                isPinned = uiState.customization.taskbarPins.any { it.id == shortcut.id },
                posX = menuData.posX,
                posY = menuData.posY,
                onDismiss = { viewModel.closeContextMenu() },
                onLaunch = { viewModel.launchShortcutOrHostApp(context, shortcut) },
                onUpdateStyle = { size, shape, color, bg, artKey ->
                    viewModel.updateShortcutStyle(shortcut.id, size, shape, color, bg, artKey)
                },
                onTogglePin = { viewModel.togglePinShortcutToTaskbar(shortcut.id) },
                onRename = { viewModel.startRename(shortcut.id) },
                onRemove = { viewModel.removeShortcut(shortcut.id) },
                modifier = modifier
            )
        }
        is DesktopContextMenuData.WallpaperMenu -> {
            WallpaperContextMenuView(
                posX = menuData.posX,
                posY = menuData.posY,
                uiState = uiState,
                onDismiss = { viewModel.closeContextMenu() },
                onOpenTerminal = {
                    viewModel.closeContextMenu()
                    viewModel.toggleWindow(WindowType.TERMINAL)
                },
                onAddApp = {
                    viewModel.openAddAppDialog()
                },
                onOpenStore = {
                    viewModel.closeContextMenu()
                    viewModel.openAppStore(context)
                },
                onResetOobe = {
                    viewModel.closeContextMenu()
                    viewModel.resetForNewUser(context)
                },
                onPersonalize = {
                    viewModel.closeContextMenu()
                    viewModel.toggleWindow(WindowType.PERSONALIZE)
                },
                onToggleGrid = {
                    // Same switch as Personalize → Lock Icons to Grid. It used to toggle the
                    // alignment-grid overlay instead, so the label changed but nothing snapped.
                    val current = uiState.customization.lockIconsToGrid
                    viewModel.toggleLockIconsToGrid(!current)
                    viewModel.closeContextMenu()
                },
                onOpenAndroidDeveloperOptions = {
                    viewModel.closeContextMenu()
                    runCatching {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.onFailure {
                        viewModel.copyText(
                            "Open Android Settings > System > Developer options",
                            "developer options path"
                        )
                    }
                },
                onCopyDeviceReport = {
                    viewModel.closeContextMenu()
                    val dm = context.resources.displayMetrics
                    val report = buildString {
                        appendLine("Phonix OS device report")
                        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        appendLine("ABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}")
                        appendLine("Display: ${dm.widthPixels}x${dm.heightPixels} @ ${dm.densityDpi} dpi (${dm.density}x)")
                        appendLine("External displays: ${uiState.deviceCapabilityReport?.externalDisplayCount ?: 0}")
                        appendLine("Display route: ${uiState.deviceCapabilityReport?.route?.name ?: "unknown"}")
                        appendLine("Form factor: ${uiState.deviceCapabilityReport?.formFactor?.name ?: "unknown"}")
                        appendLine("Device owner: ${uiState.canRemoveHostPackages}")
                        appendLine("Linux guest: ${if (LinuxRuntime.isRunning) "running" else "stopped"}")
                        appendLine("App target SDK: ${context.applicationInfo.targetSdkVersion}")
                    }
                    viewModel.copyText(report, "device report")
                },
                onToggleDesktopGrid = {
                    viewModel.toggleDesktopWidget("grid", !uiState.customization.showDesktopGrid)
                    viewModel.closeContextMenu()
                },
                onRestartLinuxGuest = {
                    viewModel.closeContextMenu()
                    viewModel.restartContainer()
                },
                onResetShortcuts = {
                    viewModel.resetShortcutGrid()
                    viewModel.closeContextMenu()
                },
                onResetDesktopDefaults = {
                    viewModel.resetDesktopCustomizations(context)
                    viewModel.closeContextMenu()
                },
                onRunDebloat = {
                    viewModel.toggleWindow(WindowType.DEBLOAT_MANAGER)
                    viewModel.closeContextMenu()
                },
                modifier = modifier
            )

        }
        is DesktopContextMenuData.TaskbarMenu -> {
            TaskbarContextMenuView(
                position = menuData.position,
                onSelect = { docked ->
                    viewModel.updateTaskbarPosition(docked)
                    viewModel.closeContextMenu()
                },
                onDismiss = { viewModel.closeContextMenu() }
            )
        }
    }
}

@Composable
private fun ShortcutContextMenuView(
    shortcut: DesktopShortcut,
    isPinned: Boolean,
    posX: Float,
    posY: Float,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onUpdateStyle: (IconSizeMode?, ShortcutShape, DesktopAccentColor?, ShortcutBgStyle, String?) -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The menu opens under the finger that long-pressed the icon, so the release can land on
    // the first row (observed on the S10: long-press opened "My Files" as well as the menu).
    // Rows ignore input for a moment after the menu appears.
    var armedAt by remember { mutableStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(Unit) { armedAt = System.currentTimeMillis() }
    val isArmed: () -> Boolean = { System.currentTimeMillis() - armedAt > 300L }

    var selectedSize by remember { mutableStateOf(shortcut.customSize) }
    var selectedShape by remember { mutableStateOf(shortcut.customShape) }
    var selectedColor by remember { mutableStateOf(shortcut.customColor) }
    var selectedBg by remember { mutableStateOf(shortcut.customBackground) }
    var selectedArtKey by remember(shortcut.id) { mutableStateOf(shortcut.customArtKey) }

    Surface(
        modifier = modifier
            .offset { IntOffset(posX.roundToInt().coerceIn(10, 500), posY.roundToInt().coerceIn(10, 450)) }
            .widthIn(min = 280.dp, max = 320.dp)
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .border(1.dp, Slate700.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .testTag("shortcut_context_menu"),
        shape = RoundedCornerShape(12.dp),
        color = Slate950.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = shortcut.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (shortcut.launchPackage != null) "Host App (${shortcut.launchPackage})" else "Windowed App",
                        color = Slate400,
                        fontSize = 9.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Primary Launch Option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { if (isArmed()) onLaunch() }
                    .testTag("ctx_menu_open_btn"),
                color = PhonixBlue.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PhonixBlueLight.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(14.dp))
                    Text("Open App", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { if (isArmed()) onRename() }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Slate200, modifier = Modifier.size(14.dp))
                Text("Rename \"${shortcut.title}\"...", color = Color.White, fontSize = 11.sp)
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Slate800)
            Spacer(Modifier.height(8.dp))

            // 0. Icon Sprite Style Picker
            Text("Icon Artwork Style", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isAutoSelected = selectedArtKey == null
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            selectedArtKey = null
                            onUpdateStyle(selectedSize, selectedShape, selectedColor, selectedBg, null)
                        },
                    color = if (isAutoSelected) PhonixBlue.copy(alpha = 0.35f) else Slate800.copy(alpha = 0.4f),
                    border = if (isAutoSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PhonixCyan) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Auto", color = if (isAutoSelected) Color.White else Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                AppIconGlyphs.availableSprites.forEach { sprite ->
                    val isSelected = selectedArtKey == sprite.key
                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                selectedArtKey = sprite.key
                                onUpdateStyle(selectedSize, selectedShape, selectedColor, selectedBg, sprite.key)
                            },
                        color = if (isSelected) PhonixBlue.copy(alpha = 0.35f) else Slate800.copy(alpha = 0.4f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PhonixCyan) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = sprite.resId),
                                contentDescription = sprite.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 1. Icon Shape Picker
            Text("Shortcut Shape", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShortcutShape.entries.forEach { shape ->
                    val isSelected = selectedShape == shape
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                selectedShape = shape
                                onUpdateStyle(selectedSize, shape, selectedColor, selectedBg, selectedArtKey)
                            },
                        color = if (isSelected) PhonixBlue.copy(alpha = 0.3f) else Slate800.copy(alpha = 0.4f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PhonixBlueLight) else null
                    ) {
                        Text(
                            text = shape.title,
                            color = if (isSelected) Color.White else Slate400,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 2. Icon Size Picker
            Text("Icon Size", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconSizeMode.entries.forEach { size ->
                    val isSelected = selectedSize == size
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                selectedSize = size
                                onUpdateStyle(size, selectedShape, selectedColor, selectedBg, selectedArtKey)
                            },
                        color = if (isSelected) PhonixBlue.copy(alpha = 0.3f) else Slate800.copy(alpha = 0.4f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PhonixBlueLight) else null
                    ) {
                        Text(
                            text = size.title.substringBefore(" ("),
                            color = if (isSelected) Color.White else Slate400,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 3. Color Swatches
            Text("Color Tint", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DesktopAccentColor.entries.forEach { color ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color.color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else Slate700,
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColor = color
                                onUpdateStyle(selectedSize, selectedShape, color, selectedBg, selectedArtKey)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 4. Background Style
            Text("Background Glow", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShortcutBgStyle.entries.forEach { bg ->
                    val isSelected = selectedBg == bg
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                selectedBg = bg
                                onUpdateStyle(selectedSize, selectedShape, selectedColor, bg, selectedArtKey)
                            },
                        color = if (isSelected) PhonixBlue.copy(alpha = 0.3f) else Slate800.copy(alpha = 0.4f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PhonixBlueLight) else null
                    ) {
                        Text(
                            text = bg.title,
                            color = if (isSelected) Color.White else Slate400,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Slate800)
            Spacer(Modifier.height(8.dp))

            // Pin / Unpin & Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onTogglePin() },
                    color = Slate800.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(12.dp))
                        Text(
                            text = if (isPinned) "Unpin from Taskbar" else "Pin to Taskbar",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onRemove() },
                    color = PhonixRed.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = PhonixRed, modifier = Modifier.size(12.dp))
                        // "Delete" read as removing the app; it only removes this desktop icon.
                        Text("Remove from Desktop", color = PhonixRed, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperContextMenuView(
    posX: Float,
    posY: Float,
    uiState: PhonixUiState,
    onDismiss: () -> Unit,
    onOpenTerminal: () -> Unit,
    onAddApp: () -> Unit,
    onOpenStore: () -> Unit,
    onResetOobe: () -> Unit,
    onPersonalize: () -> Unit,
    onToggleGrid: () -> Unit,
    onOpenAndroidDeveloperOptions: () -> Unit,
    onCopyDeviceReport: () -> Unit,
    onToggleDesktopGrid: () -> Unit,
    onRestartLinuxGuest: () -> Unit,
    onResetShortcuts: () -> Unit,
    onResetDesktopDefaults: () -> Unit,
    onRunDebloat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .offset { IntOffset(posX.roundToInt().coerceIn(10, 500), posY.roundToInt().coerceIn(10, 450)) }
            .widthIn(min = 230.dp, max = 280.dp)
            .shadow(16.dp, RoundedCornerShape(10.dp))
            .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .testTag("wallpaper_context_menu"),
        shape = RoundedCornerShape(10.dp),
        color = Slate950.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier.padding(6.dp)
        ) {
            ContextMenuItem(
                icon = Icons.Default.Add,
                title = "Add Installed App to Desktop...",
                tint = PhonixCyan,
                onClick = onAddApp
            )
            ContextMenuItem(
                icon = Icons.Default.Store,
                title = "Get Apps (Google Play Store)",
                tint = PhonixAmber,
                onClick = onOpenStore
            )
            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 3.dp))
            ContextMenuItem(
                icon = Icons.Default.Terminal,
                title = "Open Linux Terminal Here",
                tint = PhonixGreen,
                onClick = onOpenTerminal
            )
            ContextMenuItem(
                icon = Icons.Default.GridView,
                title = if (uiState.customization.showDesktopGrid) "Hide Alignment Grid" else "Show Alignment Grid",
                tint = PhonixCyan,
                onClick = onToggleDesktopGrid
            )
            ContextMenuItem(
                icon = Icons.Default.GridView,
                title = if (uiState.customization.lockIconsToGrid) "Disable Grid Snap" else "Enable Grid Snap",
                tint = PhonixCyan,
                onClick = onToggleGrid
            )
            ContextMenuItem(
                icon = Icons.Default.Palette,
                title = "Personalize Desktop...",
                tint = PhonixAmber,
                onClick = onPersonalize
            )
            ContextMenuItem(
                icon = Icons.Default.Security,
                title = "System Debloater (RAM & Storage)...",
                tint = PhonixBlueLight,
                onClick = onRunDebloat
            )

            ContextMenuItem(
                icon = Icons.Default.Refresh,
                title = "Align Shortcuts to Bottom-Left",
                tint = Slate400,
                onClick = onResetShortcuts
            )
            ContextMenuItem(
                icon = Icons.Default.RestartAlt,
                title = "Reset Desktop Layout & Icons to Defaults",
                tint = PhonixAmber,
                onClick = onResetDesktopDefaults
            )
            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 3.dp))
            ContextMenuItem(
                icon = Icons.Default.PersonAdd,
                title = "Reset for New User (OOBE Setup)",
                tint = PhonixCyan,
                onClick = onResetOobe
            )
            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 3.dp))
            Text(
                "DEVELOPER",
                color = Slate400,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp, top = 2.dp, bottom = 2.dp)
            )
            ContextMenuItem(
                icon = Icons.Default.Settings,
                title = "Android Developer Options...",
                tint = PhonixCyan,
                onClick = onOpenAndroidDeveloperOptions
            )
            ContextMenuItem(
                icon = Icons.Default.ContentCopy,
                title = "Copy Device Report to Clipboard",
                tint = PhonixBlueLight,
                onClick = onCopyDeviceReport
            )
            ContextMenuItem(
                icon = Icons.Default.RestartAlt,
                title = "Restart Linux Guest",
                tint = PhonixGreen,
                onClick = onRestartLinuxGuest
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Normal)
    }
}

/**
 * The taskbar's own menu: dock it to the bottom, left, right or top.
 *
 * It is placed by alignment instead of pointer offset, and always on the opposite side of the edge
 * the bar occupies, so the menu can neither cover the bar it belongs to nor land off-screen when
 * the bar is docked to the right or the top.
 */
@Composable
private fun TaskbarContextMenuView(
    position: TaskbarPosition,
    onSelect: (TaskbarPosition) -> Unit,
    onDismiss: () -> Unit
) {
    val alignment = when (position) {
        TaskbarPosition.BOTTOM -> Alignment.BottomCenter
        TaskbarPosition.TOP -> Alignment.TopCenter
        TaskbarPosition.LEFT -> Alignment.CenterStart
        TaskbarPosition.RIGHT -> Alignment.CenterEnd
    }
    val dx = when (position) {
        TaskbarPosition.LEFT -> 64.dp
        TaskbarPosition.RIGHT -> (-64).dp
        else -> 0.dp
    }
    val dy = when (position) {
        TaskbarPosition.BOTTOM -> (-60).dp
        TaskbarPosition.TOP -> 60.dp
        else -> 0.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .align(alignment)
                .offset(x = dx, y = dy)
                .widthIn(min = 200.dp, max = 240.dp)
                .shadow(16.dp, RoundedCornerShape(10.dp))
                .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .testTag("taskbar_context_menu"),
            shape = RoundedCornerShape(10.dp),
            color = Slate950.copy(alpha = 0.96f)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    "Taskbar position",
                    color = Slate400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp, top = 2.dp, bottom = 4.dp)
                )
                TaskbarPosition.entries.forEach { option ->
                    val isSelected = option == position
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelect(option) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isSelected) PhonixCyan else Color.Transparent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            option.title,
                            color = if (isSelected) Color.White else Slate200,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 3.dp))
                Text(
                    "Right-click the taskbar, or long-press it on a touch screen, to reopen this.",
                    color = Slate400,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
