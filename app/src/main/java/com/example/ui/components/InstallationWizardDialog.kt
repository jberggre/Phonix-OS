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
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import com.example.model.MiniPcProfile
import com.example.model.PreferredBrowser
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
                            1 -> Step1ProfileSelection(uiState, viewModel)
                            2 -> Step2LinuxChoice(uiState, viewModel)
                            3 -> Step3BrowserSelection(uiState, viewModel)
                            4 -> Step4DebloatOptimization(uiState, viewModel)
                            5 -> Step5ScreenRescue(uiState, viewModel)
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
        "Step 1: Hardware & Direct Power",
        "Step 2: Choose Your Primary Use Case",
        "Step 3: Linux Subsystem (Keep vs Lean)",
        "Step 4: Default Web Browser",
        "Step 5: Privacy Debloat & Chrome",
        "Step 6: Display 0 Rescue (Shattered Screen)",
        "Step 7: Finish Setup & Launch Desktop"
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
// STEP 1: What will you use this mini PC for? (Profile Selection)
// -------------------------------------------------------------
@Composable
fun Step1ProfileSelection(
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
            text = "What is the primary mission for this mini PC?",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your choice automatically configures cooling, background services, and storage recommendations.",
            color = Slate400,
            fontSize = 11.sp
        )

        MiniPcProfile.entries.forEach { profile ->
            val isSelected = uiState.oobeConfig.profile == profile
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
                    .clickable { viewModel.selectOobeProfile(profile) }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = profile.title,
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

                    Text(text = profile.subtitle, color = PhonixCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = profile.description, color = Slate300, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: Linux Subsystem Choice (Keep vs. Lean Mode)
// -------------------------------------------------------------
@Composable
fun Step2LinuxChoice(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    val keepLinux = uiState.oobeConfig.enableLinuxDesktop

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Do you want the Linux subsystem installed?",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Phonix can run an isolated Alpine Linux userland with a full command line, compilers, and X11 apps. You can keep it or select Ultra-Lean Mode to maximize phone storage.",
            color = Slate300,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        // Option A: Keep Full Linux (~550 MB)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (keepLinux) Color(0xFF0F261C) else Slate900
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (keepLinux) 2.dp else 1.dp,
                    color = if (keepLinux) PhonixGreen else Slate800,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { viewModel.toggleOobeLinuxSubsystem(true) }
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Computer, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(22.dp))
                        Text("Keep Alpine Linux Subsystem (Active)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (keepLinux) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(18.dp))
                    }
                }
                Text("Storage Impact: ~550 MB", color = PhonixAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Provides full Alpine Linux PRoot userland, apk package manager, terminal compilers (Python/GCC/Rust), local VS Code Web, and background servers.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // Option B: Ultra-Lean Android Mode (Strip Linux)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (!keepLinux) Color(0xFF1E293B) else Slate900
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (!keepLinux) 2.dp else 1.dp,
                    color = if (!keepLinux) PhonixCyan else Slate800,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { viewModel.toggleOobeLinuxSubsystem(false) }
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(22.dp))
                        Text("Ultra-Lean Android Mode (Save 550 MB)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!keepLinux) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                    }
                }
                Text("Storage Impact: 0 MB (Reclaims ~550 MB Flash)", color = PhonixGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Purges guest Linux files to maximize internal storage for your files and Android games. You can always re-fetch and re-install Linux later from the App Store.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: Default Web Browser Selection
// -------------------------------------------------------------
@Composable
fun Step3BrowserSelection(
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
            text = "Select your default web browser for Phonix OS:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Phonix acts as your private workstation. Choose which browser powers your web links and desktop shortcuts.",
            color = Slate400,
            fontSize = 11.sp
        )

        PreferredBrowser.entries.forEach { browser ->
            val isSelected = uiState.oobeConfig.preferredBrowser == browser
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
                    .clickable { viewModel.selectOobeBrowser(browser) }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(20.dp))
                            Text(browser.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(browser.subtitle, color = Slate300, fontSize = 11.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: Privacy Debloat & Chrome Options
// -------------------------------------------------------------
@Composable
fun Step4DebloatOptimization(
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
        // Google Chrome Debloat Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.oobeConfig.debloatChrome) Color(0xFF0F261C) else Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, if (uiState.oobeConfig.debloatChrome) PhonixGreen else PhonixCyan, RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Debloat Google Chrome", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Disable Chrome from the desktop and route all web queries to your chosen private browser.", color = Slate300, fontSize = 10.sp)
                }
                Switch(
                    checked = uiState.oobeConfig.debloatChrome,
                    onCheckedChange = { viewModel.toggleOobeDebloatChrome(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = PhonixGreen)
                )
            }
        }

        // OEM Bloatware Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OEM & Carrier Bloatware", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Selected: ${selected.size} packages. Generates safe adb command to disable carrier telemetry and background Samsung daemons.",
                    color = Slate300,
                    fontSize = 10.sp
                )
                Button(
                    onClick = { viewModel.copyDebloatCommand() },
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Copy adb Command to Clipboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Select bloat packages to disable:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        uiState.debloatPackages.take(5).forEach { pkg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = if (pkg.isEnabled) Color(0xFF0F1B2B) else Slate900),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, if (pkg.isEnabled) PhonixCyan else Slate800, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pkg.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(pkg.packageName, color = PhonixBlueLight, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
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
// STEP 5: Shattered Screen Rescue Walkthrough
// -------------------------------------------------------------
@Composable
fun Step5ScreenRescue(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, PhonixAmber, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(24.dp))
                    Text("Display 0 Rescue for Shattered Screens", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Galaxy S10 motherboards harvested from phones with unusable cracked screens are enclosed inside this mini PC shell. Phonix includes a dedicated rescue window so you never get locked out.",
                    color = Slate200,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How to Control the Broken Screen:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1.", color = PhonixCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Permission Popups: Any Android dialogs that appear strictly on the internal phone display can be viewed and tapped inside the rescue window.", color = Slate300, fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2.", color = PhonixCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Direct Touch Forwarding: Clicking the virtual phone screen forwards touch coordinates (0 to 1080x2280) to the physical digitizer.", color = Slate300, fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3.", color = PhonixCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Virtual Hardware Keys: Enclosed physical buttons (Power, Home, Back, Recents) are available right on the window's bottom navigation bar.", color = Slate300, fontSize = 11.sp)
                }
            }
        }

        Text(
            "Access the Phone Screen window anytime from the 'Phone Screen' desktop shortcut or from the Phonix Start Menu.",
            color = PhonixAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
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
