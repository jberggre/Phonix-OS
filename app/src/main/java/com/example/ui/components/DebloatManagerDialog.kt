package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DebloatPackage
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
import com.example.util.HostDebloat
import com.example.util.HostPackageState
import com.example.util.PackageFacts
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel

/**
 * Interactive Debloat Walkthrough:
 * Inspects native host packages, displays real storage footprint on disk and RAM consumption estimates,
 * and allows direct 1-click uninstall or jumping to the native system App Settings to Disable and Clear Data.
 */
@Composable
fun DebloatManagerDialog(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 340.dp, max = 540.dp)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = Slate950,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
        ) {
            DebloatManagerContent(uiState, viewModel, onDismiss)
        }
    }
}

@Composable
fun DebloatManagerContent(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // The same list the wizard edits. This screen used to re-read the raw catalogue, so a tick
    // made in the wizard and the presence map here disagreed, and a hidden package kept showing
    // as INSTALLED because getPackageInfo answers for hidden packages too.
    val bloatPackages = uiState.debloatPackages.map { pkg ->
        val isHidden = pkg.packageName in uiState.debloatHidden
        val isInstalled = uiState.debloatInstalled[pkg.packageName]
            ?: PackageFacts.isInstalled(context, pkg.packageName)
        Triple(
            pkg,
            HostDebloat.stateOf(isInstalled, isHidden),
            PackageFacts.apkBytes(context, pkg.packageName)
        )
    }

    val installedBloat = bloatPackages.filter { it.second != HostPackageState.ABSENT }
    val totalReclaimableBytes = installedBloat.sumOf { it.third }
    val totalReclaimableRamMb = installedBloat.sumOf { it.first.estimatedRamMb }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(20.dp))
                        Column {
                            Text("System Debloater & Optimizer", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Walkthrough native preloads to recover RAM & Storage", color = Slate400, fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = { onDismiss?.invoke() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }

                // Summary Stats Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PhonixGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Installed Bloat", color = Slate400, fontSize = 10.sp)
                            Text("${installedBloat.size} apps", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Reclaimable Storage", color = Slate400, fontSize = 10.sp)
                            val storageStr = if (totalReclaimableBytes > 0) PackageFacts.formatBytes(totalReclaimableBytes) else "~${installedBloat.sumOf { it.first.estimatedStorageMb }} MB"
                            Text(storageStr, color = PhonixCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Est. Free RAM", color = Slate400, fontSize = 10.sp)
                            Text("+$totalReclaimableRamMb MB", color = PhonixGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = if (uiState.canRemoveHostPackages)
                        "Device Owner active: 'Hide' removes a package for real and is reversible. 'Uninstall' opens Android's own prompt."
                    else
                        "Tap 'Uninstall' for Android's removal prompt, or 'App Info' to Disable and clear its data.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                // Package List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bloatPackages) { entry ->
                        val (pkg, state, apkBytes) = entry
                        val isInstalled = state != HostPackageState.ABSENT
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isInstalled) Color(0xFF0F1B2B) else Color(0xFF141923)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isInstalled) Slate700 else Slate800.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val appIcon = remember(pkg.packageName, isInstalled) {
                                        if (!isInstalled) null else runCatching {
                                            context.packageManager.getApplicationIcon(pkg.packageName)
                                        }.getOrNull()
                                    }
                                    val iconBitmap = remember(appIcon) { drawableToImageBitmap(appIcon) }
                                    if (iconBitmap != null) {
                                        Image(
                                            bitmap = iconBitmap,
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pkg.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(pkg.packageName, color = PhonixBlueLight, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }

                                    val badgeColor = when (state) {
                                        HostPackageState.INSTALLED -> PhonixAmber
                                        HostPackageState.HIDDEN -> PhonixCyan
                                        HostPackageState.ABSENT -> Slate400
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when (state) {
                                                HostPackageState.INSTALLED -> "INSTALLED"
                                                HostPackageState.HIDDEN -> "HIDDEN"
                                                HostPackageState.ABSENT -> "ABSENT"
                                            },
                                            color = badgeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(pkg.description, color = Slate300, fontSize = 10.sp, lineHeight = 14.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val breakdown = remember(pkg.packageName, isInstalled) {
                                        if (isInstalled) PackageFacts.storageBreakdown(context, pkg.packageName) else null
                                    }
                                    Text(
                                        text = buildString {
                                            append("APK ")
                                            append(if (apkBytes > 0) PackageFacts.formatBytes(apkBytes) else "~${pkg.estimatedStorageMb} MB")
                                            if (breakdown != null) {
                                                append(" • data ")
                                                append(PackageFacts.formatBytes(breakdown.second + breakdown.third))
                                            }
                                            append(" • RAM ~${pkg.estimatedRamMb} MB")
                                        },
                                        color = PhonixCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    if (isInstalled) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (uiState.canRemoveHostPackages) {
                                                // Hiding is reversible, so the same button becomes the undo. There
                                                // was no way back before: a hidden package kept reporting INSTALLED
                                                // and the app offered Hide again instead of Restore.
                                                Button(
                                                    onClick = {
                                                        if (state == HostPackageState.HIDDEN) {
                                                            viewModel.unhideHostPackage(pkg.packageName)
                                                        } else {
                                                            viewModel.hideHostPackage(pkg.packageName)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PhonixCyan.copy(alpha = 0.30f)),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(11.dp))
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        if (state == HostPackageState.HIDDEN) "Restore" else "Hide",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // Direct App Settings button
                                            Button(
                                                onClick = {
                                                    HostDebloat.launchAppDetails(context, pkg.packageName)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(11.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("App Info", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Direct Uninstall button
                                            Button(
                                                onClick = {
                                                    HostDebloat.launchUninstall(context, pkg.packageName)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PhonixRed.copy(alpha = 0.8f)),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(11.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("Uninstall", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.refreshSetupFacts(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rescan Packages", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.copyDebloatCommand()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Copy PC ADB Script", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
}

