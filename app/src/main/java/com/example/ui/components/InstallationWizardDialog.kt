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
import com.example.model.DesktopResolution
import com.example.model.DistroCatalog
import com.example.model.GpuDriverType
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
        "Step 1: Phone Model & Battery-Free Power",
        "Step 2: System Debloat & RAM Reclamation",
        "Step 3: Choose Linux Distribution",
        "Step 4: Storage Partitioning & Swap",
        "Step 5: Active Cooling & GPU Setup",
        "Step 6: 24/7 Server & Gaming Suite",
        "Step 7: One-Click Automated Deploy"
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
                    text = "Removing the internal battery completely eliminates 100% of thermal swelling and fire hazards for permanent 24/7 server and mini PC use. It also frees up physical space inside the phone chassis for larger copper heatsinks!",
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
            text = "Select Your Target Phone Model:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        DeviceProfileCatalog.profiles.forEach { profile ->
            val isSelected = uiState.selectedDeviceProfile.id == profile.id
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
                    .clickable { viewModel.selectDeviceProfile(profile) }
                    .testTag("device_profile_${profile.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (isSelected) PhonixCyan else Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                    Text(text = "SoC: ${profile.chip}", color = PhonixBlueLight, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "RAM: ${profile.ramGb} GB | Storage: ${profile.totalStorageGb.toInt()} GB", color = Slate400, fontSize = 11.sp)
                }
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
    val totalRamReclaimed = uiState.debloatPackages.filter { it.isEnabled }.sumOf { it.estimatedRamSavedMb }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // RAM Reclamation Hero
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2B1E)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, PhonixGreen, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pre-Installation System Debloat", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Disables background OEM and carrier bloatware before installing Linux. This reclaims crucial physical RAM and permanently disables background thermal throttlers like Samsung Game Optimizing Service (GOS).",
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
                    Text("Estimated RAM Reclaimed for Linux:", color = Slate400, fontSize = 12.sp)
                    Text(
                        text = "+$totalRamReclaimed MB",
                        color = PhonixGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Text(
            text = "Select Background Bloatware to Strip:",
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pkg.displayName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF065F46))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+${pkg.estimatedRamSavedMb}MB", color = Color(0xFF6EE7B7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
    val part = uiState.partitionConfig
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "DeX-Style Container Storage & zRAM Memory:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Zero-Partition Safe Overlay • Directly reuses installed Chrome & Files to save 4GB+ storage",
                color = PhonixCyan,
                fontSize = 10.sp
            )
        }

        // Visual Partition Breakdown Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Internal Storage: ${part.totalDeviceStorageGb.toInt()} GB",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    val androidWeight = (part.androidSystemUsedGb / part.totalDeviceStorageGb).coerceAtLeast(0.1f)
                    val linuxWeight = (part.allocatedLinuxGb / part.totalDeviceStorageGb).coerceAtLeast(0.1f)
                    val swapWeight = (part.allocatedSwapGb / part.totalDeviceStorageGb).coerceAtLeast(0.04f)
                    val freeWeight = (part.freeStorageGb / part.totalDeviceStorageGb).coerceAtLeast(0.05f)

                    Box(modifier = Modifier.weight(androidWeight).fillMaxHeight().background(Slate700), contentAlignment = Alignment.Center) {
                        Text("Android", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Box(modifier = Modifier.weight(linuxWeight).fillMaxHeight().background(PhonixBlue), contentAlignment = Alignment.Center) {
                        Text("Linux EXT4", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Box(modifier = Modifier.weight(swapWeight).fillMaxHeight().background(PhonixAmber), contentAlignment = Alignment.Center) {
                        Text("zRAM", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Box(modifier = Modifier.weight(freeWeight).fillMaxHeight().background(PhonixGreen), contentAlignment = Alignment.Center) {
                        Text("Free", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        // Linux Container Size Slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Linux Container Size:", color = Color.White, fontSize = 13.sp)
                Text("${part.allocatedLinuxGb.toInt()} GB", color = PhonixCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = part.allocatedLinuxGb,
                onValueChange = { viewModel.updatePartitionSizes(it, part.allocatedSwapGb) },
                valueRange = 4f..32f,
                steps = 6,
                colors = SliderDefaults.colors(thumbColor = PhonixCyan, activeTrackColor = PhonixBlue)
            )
        }

        // Swap Allocation Slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("zRAM Compressed Swap:", color = Color.White, fontSize = 13.sp)
                Text("${part.allocatedSwapGb.toInt()} GB", color = PhonixAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = part.allocatedSwapGb,
                onValueChange = { viewModel.updatePartitionSizes(part.allocatedLinuxGb, it) },
                valueRange = 1f..4f,
                steps = 2,
                colors = SliderDefaults.colors(thumbColor = PhonixAmber, activeTrackColor = Color(0xFFD97706))
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 4: Active Cooling & GPU Acceleration
// -------------------------------------------------------------
@Composable
fun Step4HardwareAcceleration(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    val hw = uiState.hwConfig
    val cooling = uiState.coolingState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Active Cooling & GPU Setup:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // Motherboard Cooling Mode
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F261C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, PhonixGreen, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Motherboard Heatsink Mode: ACTIVE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Enables sustained 2.45 GHz maximum clocks without thermal throttling. Perfect for your custom copper heatsink and 5V fan setup.",
                    color = Slate200,
                    fontSize = 11.sp
                )
            }
        }

        // GPU Driver Cards
        GpuDriverType.values().forEach { driver ->
            val isSelected = hw.gpuDriver == driver
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF0C3256) else Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, if (isSelected) PhonixCyan else Slate800, RoundedCornerShape(10.dp)).clickable { viewModel.setGpuDriver(driver) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(driver.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(driver.description, color = Slate400, fontSize = 10.sp)
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

                Switch(
                    checked = service.isRunning,
                    onCheckedChange = { viewModel.toggleService(service.id) },
                    colors = SwitchDefaults.colors(checkedTrackColor = PhonixGreen)
                )
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
            text = "Ready to Debloat, Partition & Deploy Phonix OS!",
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
