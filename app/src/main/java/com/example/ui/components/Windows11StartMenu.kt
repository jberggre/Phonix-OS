package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import com.example.model.DesktopShortcut
import com.example.model.InstalledAppInfo
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
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
import com.example.util.AppLaunchHelper
import com.example.util.HostDebloat
import com.example.util.PackageFacts
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel
import com.example.model.DeviceUseMode


private data class StartMenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val onClick: () -> Unit
)

enum class AppViewMode(val label: String) {
    CATEGORIES("Category Tabs"),
    COMPACT_LIST("Compact List"),
    STORAGE_WEIGHT("Storage Weight")
}

@Composable
fun Windows11StartMenu(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPowerMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAllApps by remember { mutableStateOf(false) }
    var appViewMode by remember { mutableStateOf(AppViewMode.CATEGORIES) }
    var selectedCategoryTab by remember { mutableStateOf("All") }

    val allMenuItems = remember(uiState.customization.shortcuts) {
        listOf(
            StartMenuItem(
                "app_store", "App Store", "Google Play / F-Droid",
                Icons.Default.Store, PhonixCyan
            ) {
                viewModel.openAppStore(context)
            },
            StartMenuItem(
                "app_chrome", "Google Chrome / Web", "Native Web Browser",
                Icons.Default.Language, PhonixBlueLight
            ) {
                val sc = uiState.customization.shortcuts.find { it.id == "sc_browser" }
                if (sc != null) viewModel.launchShortcutOrHostApp(context, sc)
                else AppLaunchHelper.launchWebBrowser(context)
            },
            StartMenuItem(
                "app_files", "File Explorer", "Native Files & USB Storage",
                Icons.Default.Folder, Color(0xFFFBBF24)
            ) {
                val sc = uiState.customization.shortcuts.find { it.id == "sc_files" }
                if (sc != null) viewModel.launchShortcutOrHostApp(context, sc)
                else AppLaunchHelper.launchFileManager(context)
            },
            StartMenuItem(
                "app_photos", "Photos & Media", "Native Gallery & Player",
                Icons.Default.Image, Color(0xFFA855F7)
            ) {
                AppLaunchHelper.launchPhotos(context)
            },
            StartMenuItem(
                "app_distro", "Linux System", "Multi-distro runtime & packages",
                Icons.Default.Widgets, PhonixAmber
            ) {
                viewModel.toggleWindow(WindowType.DISTRO_STORE)
            },
            StartMenuItem(
                "app_terminal", "Terminal", "Linux shell, proot, no root",
                Icons.Default.Terminal, PhonixGreen
            ) {
                viewModel.toggleWindow(WindowType.TERMINAL)
            },
            StartMenuItem(
                "app_linux_desktop", "Linux Desktop", "Real X11 apps in a window",
                Icons.Default.Computer, PhonixGreen
            ) {
                viewModel.toggleWindow(WindowType.LINUX_DESKTOP)
            },
            StartMenuItem(
                "app_server", "Server Hub", "Nginx, SSH & Samba NAS",
                Icons.Default.Dns, Color(0xFFA855F7)
            ) {
                viewModel.toggleWindow(WindowType.SERVER_MANAGER)
            },
            StartMenuItem(
                "app_privacy", "Privacy Hub", "Ad blocking & Pi-hole guide",
                Icons.Default.Security, PhonixGreen
            ) {
                viewModel.toggleWindow(WindowType.PRIVACY)
            },
            StartMenuItem(
                "app_monitor", "Resource Monitor", "Live battery & CPU readings",
                Icons.Default.Speed, PhonixCyan
            ) {
                viewModel.toggleWindow(WindowType.RESOURCE_MONITOR)
            },
            StartMenuItem(
                "app_settings", "Settings", "Android System Settings",
                Icons.Default.Settings, Slate300
            ) {
                AppLaunchHelper.launchSettings(context)
            },
            StartMenuItem(
                "app_debloat", "System Debloater", "Walkthrough preloads to recover RAM & disk",
                Icons.Default.Security, PhonixBlueLight
            ) {
                viewModel.toggleWindow(WindowType.DEBLOAT_MANAGER)
            },
            StartMenuItem(
                "app_personalize", "Personalize", "Wallpapers, Icons & Shapes",
                Icons.Default.Palette, Color(0xFFEC4899)
            ) {
                viewModel.toggleWindow(WindowType.PERSONALIZE)
            },
            StartMenuItem(
                "app_notepad", "Notepad Editor", "Quick Notes & Scripts",
                Icons.Default.Code, Slate300
            ) {
                viewModel.toggleWindow(WindowType.NOTEPAD)
            },
            StartMenuItem(
                "app_device_setup", "Device & Display Setup", "Daily driver, mini-PC choices and compatibility check",
                Icons.Default.Settings, PhonixCyan
            ) {
                viewModel.openInstallationWizard()
            },
            StartMenuItem(
                "app_about", "About Phonix OS", "Version, updates & device info",
                Icons.Default.Info, PhonixCyan
            ) {
                viewModel.openWindow(WindowType.ABOUT)
            }
        )
    }

    val filteredItems = remember(searchQuery, allMenuItems) {
        if (searchQuery.isBlank()) allMenuItems
        else allMenuItems.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    val rawFilteredInstalledApps = remember(searchQuery, uiState.installedApps) {
        if (searchQuery.isBlank()) uiState.installedApps
        else uiState.installedApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    // App categories
    val categoryTabs = listOf("All", "Tools & System", "Browsers & Web", "Media", "Debloat Candidates")

    fun matchesCategory(app: InstalledAppInfo, tab: String): Boolean {
        if (tab == "All") return true
        val pkg = app.packageName.lowercase()
        val lbl = app.label.lowercase()
        return when (tab) {
            "Tools & System" -> pkg.contains("settings") || pkg.contains("file") || pkg.contains("tool") || pkg.contains("android") || lbl.contains("files") || lbl.contains("settings")
            "Browsers & Web" -> pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox") || pkg.contains("web") || lbl.contains("chrome") || lbl.contains("browser")
            "Media" -> pkg.contains("camera") || pkg.contains("gallery") || pkg.contains("photo") || pkg.contains("media") || pkg.contains("music") || pkg.contains("player")
            "Debloat Candidates" -> pkg.contains("samsung") || pkg.contains("facebook") || pkg.contains("bixby") || pkg.contains("carrier") || pkg.contains("telemetry") || pkg.contains("diag")
            else -> true
        }
    }

    val filteredInstalledApps = remember(rawFilteredInstalledApps, selectedCategoryTab, appViewMode) {
        if (appViewMode == AppViewMode.STORAGE_WEIGHT) {
            rawFilteredInstalledApps.sortedByDescending { HostDebloat.getPackageApkSize(context, it.packageName) }
        } else if (appViewMode == AppViewMode.CATEGORIES) {
            rawFilteredInstalledApps.filter { matchesCategory(it, selectedCategoryTab) }
        } else {
            rawFilteredInstalledApps
        }
    }

    Surface(
        modifier = modifier
            .widthIn(min = 320.dp, max = 560.dp)
            .heightIn(max = 560.dp)
            .shadow(24.dp, RoundedCornerShape(16.dp))
            .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("windows_11_start_menu"),
        shape = RoundedCornerShape(16.dp),
        color = Slate900.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Search Bar (Windows 11 Centered Pill)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_menu_search_input"),
                placeholder = {
                    Text("Search apps, phone tools, or web...", color = Slate400, fontSize = 12.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Slate400, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PhonixBlue,
                    unfocusedBorderColor = Slate800,
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(14.dp))

            // 2. Section Header: Pinned vs All Apps Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showAllApps) "App Collection (${filteredInstalledApps.size})" else "Pinned",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Slate800.copy(alpha = 0.6f))
                        .clickable { showAllApps = !showAllApps }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showAllApps) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Back to Pinned", color = PhonixCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("All apps", color = Slate300, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // View Mode Selectors (When viewing All Apps)
            if (showAllApps) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppViewMode.entries.forEach { mode ->
                        val isSelected = appViewMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PhonixBlue.copy(alpha = 0.4f) else Slate800)
                                .border(1.dp, if (isSelected) PhonixCyan else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { appViewMode = mode }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSelected) Color.White else Slate400,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Category Sub-tabs if CATEGORIES mode is active
                if (appViewMode == AppViewMode.CATEGORIES) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categoryTabs.forEach { tab ->
                            val isTabSelected = selectedCategoryTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isTabSelected) PhonixAmber.copy(alpha = 0.25f) else Slate900)
                                    .border(1.dp, if (isTabSelected) PhonixAmber else Slate800, RoundedCornerShape(4.dp))
                                    .clickable { selectedCategoryTab = tab }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isTabSelected) PhonixAmber else Slate400,
                                    fontSize = 9.sp,
                                    fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3. Main Body: Pinned Grid OR All Apps Views
            AnimatedContent(
                targetState = showAllApps,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "start_menu_view_transition"
            ) { isAllApps ->
                if (isAllApps) {
                    when (appViewMode) {
                        AppViewMode.COMPACT_LIST -> {
                            // 2-Column Compact Grid
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredInstalledApps) { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Slate800.copy(alpha = 0.5f))
                                            .clickable {
                                                viewModel.closeStartMenu()
                                                AppLaunchHelper.launchNativeApp(context, app.packageName)
                                            }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val bmp = remember(app.iconDrawable) { drawableToImageBitmap(app.iconDrawable) }
                                        if (bmp != null) {
                                            Image(bitmap = bmp, contentDescription = app.label, modifier = Modifier.size(20.dp))
                                        } else {
                                            Icon(Icons.Default.Widgets, contentDescription = app.label, tint = PhonixCyan, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = app.label,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        AppViewMode.STORAGE_WEIGHT -> {
                            // Apps sorted by APK size with size badges and direct settings jump
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredInstalledApps) { app ->
                                    val apkBytes = remember(app.packageName) { HostDebloat.getPackageApkSize(context, app.packageName) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Slate800.copy(alpha = 0.4f))
                                            .clickable {
                                                viewModel.closeStartMenu()
                                                AppLaunchHelper.launchNativeApp(context, app.packageName)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val bmp = remember(app.iconDrawable) { drawableToImageBitmap(app.iconDrawable) }
                                        if (bmp != null) {
                                            Image(bitmap = bmp, contentDescription = app.label, modifier = Modifier.size(24.dp))
                                        } else {
                                            Icon(Icons.Default.Widgets, contentDescription = app.label, tint = PhonixCyan, modifier = Modifier.size(20.dp))
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(app.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(app.packageName, color = Slate400, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }

                                        // Size badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(PhonixAmber.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (apkBytes > 0) PackageFacts.formatBytes(apkBytes) else "< 1 MB",
                                                color = PhonixAmber,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Spacer(Modifier.width(6.dp))

                                        // App Settings / Disable
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Slate800)
                                                .clickable { HostDebloat.launchAppDetails(context, app.packageName) }
                                                .padding(horizontal = 5.dp, vertical = 3.dp)
                                        ) {
                                            Text("Info", color = Slate300, fontSize = 8.sp)
                                        }
                                    }
                                }
                            }
                        }

                        AppViewMode.CATEGORIES -> {
                            // Categorized list
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (filteredInstalledApps.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No matching apps in this category", color = Slate400, fontSize = 11.sp)
                                        }
                                    }
                                } else {
                                    items(filteredInstalledApps) { app ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.closeStartMenu()
                                                    AppLaunchHelper.launchNativeApp(context, app.packageName)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // App Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Slate800),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val bmp = remember(app.iconDrawable) {
                                                    drawableToImageBitmap(app.iconDrawable)
                                                }
                                                if (bmp != null) {
                                                    Image(
                                                        bitmap = bmp,
                                                        contentDescription = app.label,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.Widgets,
                                                        contentDescription = app.label,
                                                        tint = PhonixCyan,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.label,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = app.packageName,
                                                    color = Slate400,
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Pin to Desktop Action
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Slate800)
                                                    .clickable {
                                                        viewModel.pinInstalledApp(context, app)
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(10.dp))
                                                    Spacer(Modifier.width(2.dp))
                                                    Text("Pin", color = PhonixCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Pinned Apps Grid (Windows 11 3-column / 4-column layout)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 90.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),

                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredItems) { item ->
                            Card(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        item.onClick()
                                        viewModel.closeStartMenu()
                                    }
                                    .testTag("start_item_${item.id}"),
                                colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.45f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(item.iconTint.copy(alpha = 0.15f))
                                            .border(1.dp, item.iconTint.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = item.iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Slate800)
            Spacer(Modifier.height(10.dp))

            // 4. Quick App Store / Distro Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate800.copy(alpha = 0.3f))
                    .clickable {
                        viewModel.openAppStore(context)
                        viewModel.closeStartMenu()
                    }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PhonixCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Store, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Get More Apps & Games", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Download from Google Play Store or APK", color = Slate400, fontSize = 10.sp)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Slate800)
            Spacer(Modifier.height(10.dp))

            // 5. Windows 11 Bottom Profile & Power Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // User Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PhonixAmber, PhonixRed))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column {
                        Text(uiState.oobeConfig.deviceName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(uiState.oobeConfig.profile.title, color = Slate400, fontSize = 9.sp)
                    }
                }

                // Power Button with Flyout
                Box {
                    IconButton(
                        onClick = { showPowerMenu = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Slate800.copy(alpha = 0.6f))
                            .testTag("start_menu_power_btn")
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = showPowerMenu,
                        onDismissRequest = { showPowerMenu = false },
                        modifier = Modifier
                            .background(Slate950)
                            .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                    ) {
                        // The user requested: "Each user should get an experience like running a laptop for the first time when the app is reset for a new user"
                        DropdownMenuItem(
                            text = { Text("Reset for New User (OOBE Setup)", color = PhonixCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PhonixCyan) },
                            onClick = {
                                showPowerMenu = false
                                viewModel.closeStartMenu()
                                viewModel.resetForNewUser(context)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restart Phonix Container", color = Color.White, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Slate300) },
                            onClick = {
                                showPowerMenu = false
                                viewModel.closeStartMenu()
                                viewModel.restartContainer()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Direct Boot to Linux Distro", color = Color.White, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Widgets, contentDescription = null, tint = PhonixAmber) },
                            onClick = {
                                showPowerMenu = false
                                viewModel.closeStartMenu()
                                viewModel.triggerLinuxRebootAnimation()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Shut Down / Standby", color = PhonixRed, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = PhonixRed) },
                            onClick = {
                                showPowerMenu = false
                                viewModel.closeStartMenu()
                                viewModel.toggleContainerPower()
                            }
                        )
                    }
                }
            }
        }
    }
}

fun drawableToImageBitmap(drawable: Drawable?): androidx.compose.ui.graphics.ImageBitmap? {
    if (drawable == null) return null
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap.asImageBitmap()
        } else {
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    } catch (_: Exception) {
        null
    }
}
