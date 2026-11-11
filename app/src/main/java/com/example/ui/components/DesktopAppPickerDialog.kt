package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import com.example.model.LinuxAppCatalog
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
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

@Composable
fun DesktopAppPickerDialog(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val query = uiState.addAppSearchQuery
    var selectedTab by remember { mutableStateOf(0) } // 0 = Android Apps, 1 = Linux Apps & CLI
    var customLinuxTitle by remember { mutableStateOf("") }
    var customLinuxCommand by remember { mutableStateOf("") }
    var customLinuxIsGui by remember { mutableStateOf(false) }

    val filteredApps = remember(query, uiState.installedApps) {
        if (query.isBlank()) uiState.installedApps
        else uiState.installedApps.filter {
            it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(min = 360.dp, max = 540.dp)
                .fillMaxWidth(0.92f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(1.dp, Slate700.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .testTag("desktop_app_picker_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.98f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pin Shortcut to Phonix Desktop",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pin installed Android apps or Linux CLI / X11 GUI commands",
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Mode Switcher Tabs: Android Apps vs Linux Apps & CLI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = 0 },
                        color = if (selectedTab == 0) PhonixBlue.copy(alpha = 0.35f) else Slate800.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (selectedTab == 0) PhonixCyan else Slate700)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Widgets, contentDescription = null, tint = if (selectedTab == 0) PhonixCyan else Slate400, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Android Apps", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = 1 },
                        color = if (selectedTab == 1) PhonixBlue.copy(alpha = 0.35f) else Slate800.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (selectedTab == 1) PhonixCyan else Slate700)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = if (selectedTab == 1) PhonixGreen else Slate400, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Linux Apps & CLI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // Search Bar
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.updateAddAppSearchQuery(it) },
                        placeholder = { Text("Search installed apps...", color = Slate400, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PhonixBlue,
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    // Quick Play Store Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate800.copy(alpha = 0.4f))
                            .clickable {
                                onDismiss()
                                viewModel.openAppStore(context)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Need a new app?", color = Slate300, fontSize = 11.sp)
                        }
                        Text("Get from Google Play Store ->", color = PhonixCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Slate800)
                    Spacer(Modifier.height(6.dp))

                    // Installed Apps List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredApps.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No installed apps match '$query'", color = Slate400, fontSize = 11.sp)
                                }
                            }
                        } else {
                            items(filteredApps) { app ->
                                val isAlreadyPinned = uiState.customization.shortcuts.any { it.launchPackage == app.packageName }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.45f)),
                                    border = BorderStroke(1.dp, Slate700.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // App Icon
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Slate900),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val bmp = remember(app.iconDrawable) {
                                                drawableToImageBitmap(app.iconDrawable)
                                            }
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp,
                                                    contentDescription = app.label,
                                                    modifier = Modifier.size(26.dp)
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
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
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

                                        Spacer(Modifier.width(8.dp))

                                        Button(
                                            onClick = { viewModel.pinInstalledApp(context, app) },
                                            enabled = !isAlreadyPinned,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isAlreadyPinned) Slate700 else PhonixBlue,
                                                disabledContainerColor = Slate800
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            if (isAlreadyPinned) {
                                                Text("Pinned", color = Slate400, fontSize = 10.sp)
                                            } else {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Pin", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Linux Apps & Custom Command Shortcut Builder
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate950),
                        border = BorderStroke(1.dp, Slate800),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Create Custom Linux Command / App Shortcut",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customLinuxTitle,
                                    onValueChange = { customLinuxTitle = it },
                                    placeholder = { Text("Label (e.g. htop)", color = Slate400, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PhonixCyan,
                                        unfocusedBorderColor = Slate800,
                                        focusedContainerColor = Slate900,
                                        unfocusedContainerColor = Slate900,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = customLinuxCommand,
                                    onValueChange = { customLinuxCommand = it },
                                    placeholder = { Text("Command (e.g. htop)", color = Slate400, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PhonixCyan,
                                        unfocusedBorderColor = Slate800,
                                        focusedContainerColor = Slate900,
                                        unfocusedContainerColor = Slate900,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { customLinuxIsGui = false },
                                        color = if (!customLinuxIsGui) PhonixGreen.copy(alpha = 0.25f) else Slate900,
                                        border = BorderStroke(1.dp, if (!customLinuxIsGui) PhonixGreen else Slate700)
                                    ) {
                                        Text(
                                            text = "Terminal CLI",
                                            color = if (!customLinuxIsGui) PhonixGreen else Slate400,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { customLinuxIsGui = true },
                                        color = if (customLinuxIsGui) PhonixCyan.copy(alpha = 0.25f) else Slate900,
                                        border = BorderStroke(1.dp, if (customLinuxIsGui) PhonixCyan else Slate700)
                                    ) {
                                        Text(
                                            text = "X11 GUI (DISPLAY=:1)",
                                            color = if (customLinuxIsGui) PhonixCyan else Slate400,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val cmd = customLinuxCommand.trim()
                                        val title = customLinuxTitle.trim().ifBlank { cmd.substringBefore(" ") }
                                        if (cmd.isNotBlank()) {
                                            viewModel.pinLinuxCommandToDesktop(title, cmd, customLinuxIsGui)
                                            customLinuxTitle = ""
                                            customLinuxCommand = ""
                                        }
                                    },
                                    enabled = customLinuxCommand.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Pin Command", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tip: Inside the Linux terminal you can also run `phonix-pin <Name> <cmd> [--gui]`.",
                        color = PhonixAmber,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(LinuxAppCatalog.apps) { entry ->
                            val isGui = entry.category != "Development"
                            val isAlreadyPinned = uiState.customization.shortcuts.any {
                                it.linuxCommand?.equals(entry.packageName, ignoreCase = true) == true
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.45f)),
                                border = BorderStroke(1.dp, Slate700.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isGui) Icons.Default.DesktopWindows else Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = if (isGui) PhonixCyan else PhonixGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${entry.packageName} • ${if (isGui) "X11 GUI" else "Terminal CLI"}",
                                            color = Slate400,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.pinLinuxAppEntry(entry) },
                                        enabled = !isAlreadyPinned,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAlreadyPinned) Slate700 else PhonixBlue,
                                            disabledContainerColor = Slate800
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = if (isAlreadyPinned) "Pinned" else "+ Pin",
                                            color = if (isAlreadyPinned) Slate400 else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
