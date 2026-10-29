package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CoolingMode
import com.example.model.DistroCatalog
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
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.TerminalBlack
import com.example.viewmodel.DeviceProfileCatalog
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel

@Composable
fun InstallationWizardDialog(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!uiState.isWizardDeploying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Slate950
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar with Step Title and Close
                WizardHeader(
                    currentStep = uiState.wizardStep,
                    totalSteps = 7,
                    onClose = { if (!uiState.isWizardDeploying) onDismiss() }
                )

                // Step Progress Bar
                LinearProgressIndicator(
                    progress = { (uiState.wizardStep + 1) / 7f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = PhonixCyan,
                    trackColor = Slate800
                )

                // Scrollable Content per Step
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    AnimatedContent(
                        targetState = uiState.wizardStep,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "wizard_step_anim"
                    ) { step ->
                        when (step) {
                            0 -> Step0DeviceAssessment(uiState, viewModel)
                            1 -> Step1DebloatOptimization(uiState, viewModel)
                            2 -> Step2DistroSelection(uiState, viewModel)
                            3 -> Step3StoragePartitioning(uiState, viewModel)
                            4 -> Step4HardwareAcceleration(uiState, viewModel)
                            5 -> Step5ServerSuite(uiState, viewModel)
                            6 -> Step6AutomatedDeployment(uiState, viewModel, onDismiss)
                            else -> Step0DeviceAssessment(uiState, viewModel)
                        }
                    }
                }

                // Bottom Navigation Footer (Back, Next, Deploy)
                WizardFooter(
                    uiState = uiState,
                    onPrev = { viewModel.prevWizardStep() },
                    onNext = { viewModel.nextWizardStep() },
                    onDeploy = {
                        viewModel.runWizardDeployment {
                            viewModel.selectTab(com.example.viewmodel.AppNavTab.DESKTOP)
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun WizardHeader(
    currentStep: Int,
    totalSteps: Int,
    onClose: () -> Unit
) {
    val stepTitles = listOf(
        "Step 1: This phone and its power",
        "Step 2: Cleanup",
        "Step 3: Choose a Linux system",
        "Step 4: Storage",
        "Step 5: Cooling and graphics",
        "Step 6: Servers and games",
        "Step 7: Finish setup"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate900)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PhonixBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${currentStep + 1}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stepTitles.getOrElse(currentStep) { "Setup Wizard" },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Phonix OS Hand-holding Walkthrough",
                    color = Slate400,
                    fontSize = 11.sp
                )
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Wizard",
                tint = Slate400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 0: Phone Model & Battery-Free DC Power Setup
// -------------------------------------------------------------
@Composable
fun Step0DeviceAssessment(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Battery-Free Direct USB-C Power Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isBatteryRemovedDirectPower) Color(0xFF0F261C) else Color(0xFF0F223D)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, if (uiState.isBatteryRemovedDirectPower) PhonixGreen else PhonixCyan, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Battery-Free Direct USB-C Dock Power",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Switch(
                        checked = uiState.isBatteryRemovedDirectPower,
                        onCheckedChange = { viewModel.toggleBatteryRemovedDirectPower(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PhonixGreen)
                    )
                }

                Text(
                    text = "A battery that is always charging is the part most likely to age badly in a 24/7 " +
                        "build. Removing it and feeding the board from a regulated supply avoids that, and a " +
                        "good copper pad and heatsink keep the SoC from throttling. Do this only if you are " +
                        "comfortable working on the hardware.",
                    color = Slate200,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Text(
                    text = "★ Recycled Mini PC Shell: Motherboards harvested from phones with cracked or unusable screens are mounted into custom recycled plastic shells with passive/active heatsinks, giving e-waste high-performance second lives!",
                    color = PhonixAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Text(
            text = "This device",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // Read from the phone rather than asking the user to pick a model: the app can see the
        // real model, RAM and free storage, so it shows those instead of a spec sheet.
        val telemetry = uiState.telemetry
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Android " + android.os.Build.VERSION.RELEASE + " · " + (android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64"),
                    color = PhonixBlueLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (telemetry.ramTotalMb > 0) "RAM: ${telemetry.ramTotalMb} MB" else "RAM: reading...",
                    color = Slate400,
                    fontSize = 11.sp
                )
                Text(
                    text = if (uiState.storage.totalBytes > 0)
                        "Storage: ${(uiState.storage.freeBytes / (1024L * 1024L * 1024L))} GB free of ${(uiState.storage.totalBytes / (1024L * 1024L * 1024L))} GB"
                    else "Storage: reading...",
                    color = Slate400,
                    fontSize = 11.sp
                )
                Text(
                    "If this is not the phone you thought you were provisioning, stop now.",
                    color = PhonixAmber,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: System Debloat & RAM Reclamation (NEW!)
// -------------------------------------------------------------
@Composable
fun Step1DebloatOptimization(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    val selected = uiState.debloatPackages.filter { it.isEnabled }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F223D)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, PhonixCyan, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pre-installation cleanup", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Android does not let one app uninstall or disable another. Pick what you want " +
                        "gone here and Phonix copies the exact adb commands for a technician to run from a " +
                        "PC with USB debugging; you can also remove them in Settings > Apps.",
                    color = Slate200,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                HorizontalDivider(color = Slate800)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Selected to remove:", color = Slate400, fontSize = 12.sp)
                    Text(
                        text = "${selected.size} packages",
                        color = PhonixCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = { viewModel.copyDebloatCommand() },
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Copy adb uninstall command", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Phonix never claims to have removed an app it did not touch.",
                    color = Slate400,
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = "Packages to remove:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        uiState.debloatPackages.forEach { pkg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = if (pkg.isEnabled) Color(0xFF0F1B2B) else Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, if (pkg.isEnabled) PhonixCyan else Slate800, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pkg.displayName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(pkg.packageName, color = PhonixBlueLight, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(pkg.description, color = Slate400, fontSize = 10.sp)
                    }

                    Switch(
                        checked = pkg.isEnabled,
                        onCheckedChange = { viewModel.toggleDebloatPackage(pkg.packageName) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PhonixGreen)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: Linux Distribution Selection
// -------------------------------------------------------------
@Composable
fun Step2DistroSelection(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Choose Your Linux Subsystem:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        DistroCatalog.distros.forEach { distro ->
            val isSelected = uiState.currentDistro.id == distro.id
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF0C3256) else Slate900
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PhonixCyan else Slate800,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { viewModel.selectDistro(distro) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = distro.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PhonixGreen)
                                    .size(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Target: ${distro.targetUse}", color = PhonixBlueLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = distro.description, color = Slate300, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: Storage Partitioning & Swap
// -------------------------------------------------------------
@Composable
fun Step3StoragePartitioning(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    LaunchedEffect(Unit) { viewModel.refreshStorage() }
    val storage = uiState.storage
    val usedBytes = (storage.totalBytes - storage.freeBytes).coerceAtLeast(0L)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Storage",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Nothing is repartitioned - here is what is actually on the device",
                color = PhonixCyan,
                fontSize = 10.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (storage.totalBytes <= 0L) {
                    Text("Reading the filesystem...", color = Slate400, fontSize = 11.sp)
                } else {
                    Text(
                        text = "App storage: ${formatBytesWizard(usedBytes)} used of ${formatBytesWizard(storage.totalBytes)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        val usedWeight = if (storage.totalBytes > 0) (usedBytes.toFloat() / storage.totalBytes).coerceAtLeast(0.05f) else 0.05f
                        val freeWeight = (1f - usedWeight).coerceAtLeast(0.05f)
                        Box(modifier = Modifier.weight(usedWeight).fillMaxHeight().background(Slate700), contentAlignment = Alignment.Center) {
                            Text("used", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.weight(freeWeight).fillMaxHeight().background(PhonixGreen), contentAlignment = Alignment.Center) {
                            Text("free", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Free: ${formatBytesWizard(storage.freeBytes)}",
                        color = PhonixGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Linux guest: " + if (storage.guestBytes > 0) formatBytesWizard(storage.guestBytes) else "not installed yet",
                        color = PhonixCyan,
                        fontSize = 11.sp
                    )
                    if (storage.guestPath.isNotEmpty()) {
                        Text(
                            text = storage.guestPath,
                            color = Slate400,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Why there is no allocation step", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Phonix keeps the Linux guest in a directory inside app storage. No partition table is " +
                        "touched, no ext4 image is created, no swap partition is made, and uninstalling the " +
                        "app removes all of it. Earlier versions of this screen offered sliders for those " +
                        "things; they were never real, so they are gone.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Text(
                    "Growth is limited by the free space shown above - packages you install count against it.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

private fun formatBytesWizard(bytes: Long): String {
    if (bytes <= 0L) return "-"
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) String.format(java.util.Locale.ROOT, "%.1f GB", gb)
    else String.format(java.util.Locale.ROOT, "%.0f MB", bytes / (1024.0 * 1024.0))
}

// -------------------------------------------------------------
// STEP 4: Active Cooling & GPU Acceleration
// -------------------------------------------------------------
@Composable
fun Step4HardwareAcceleration(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Cooling and graphics",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Pick the build you are making. This is guidance, not a setting: an unrooted app cannot " +
                "change CPU clocks, bypass throttling or swap GPU drivers, and the Linux guest draws " +
                "with software rendering (Xvfb) rather than the phone GPU.",
            color = Slate400,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )

        CoolingMode.entries.forEach { mode ->
            val isSelected = uiState.coolingState.activeMode == mode
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF0C3256) else Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isSelected) PhonixCyan else Slate800, RoundedCornerShape(10.dp))
                    .clickable { viewModel.setCoolingMode(mode) }
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(mode.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(mode.subtitle, color = PhonixCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text(mode.description, color = Slate300, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 5: Server Suite & x86 Gaming
// -------------------------------------------------------------
@Composable
fun Step5ServerSuite(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Select Server Daemons & Gaming Services:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        uiState.serverServices.forEach { service ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate900)
                    .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(service.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Port :${service.port} (${service.protocol})", color = PhonixCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { viewModel.toggleServerService(service.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (service.isInstalled) Slate800 else PhonixAmber
                    ),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = when {
                            service.isRunning -> "running"
                            service.isInstalled -> "installed"
                            else -> "install"
                        },
                        color = if (service.isInstalled) Color.White else Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 6: Automated Deployment
// -------------------------------------------------------------
@Composable
fun Step6AutomatedDeployment(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    onFinished: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Ready to debloat and set up Phonix?",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        if (uiState.isWizardDeploying) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = uiState.wizardStatusMessage,
                    color = PhonixCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { uiState.wizardProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = PhonixGreen,
                    trackColor = Slate800
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalBlack)
                .border(1.dp, Slate700, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.wizardLogs) { log ->
                    Text(
                        text = log,
                        color = if (log.startsWith("✔") || log.startsWith("★")) PhonixGreen else if (log.startsWith("➜")) PhonixCyan else Slate400,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// FOOTER: Back / Next / Deploy Buttons
// -------------------------------------------------------------
@Composable
fun WizardFooter(
    uiState: PhonixUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDeploy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Slate900,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.wizardStep > 0 && !uiState.isWizardDeploying) {
                OutlinedButton(
                    onClick = onPrev,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (uiState.wizardStep < 6) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("wizard_next_button")
                ) {
                    Text("Next Step")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            } else {
                Button(
                    onClick = onDeploy,
                    enabled = !uiState.isWizardDeploying,
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixGreen),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("wizard_deploy_button")
                ) {
                    Text(if (uiState.isWizardDeploying) "Debloating & Deploying..." else "Debloat & Deploy Phonix OS")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
