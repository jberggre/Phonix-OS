package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import com.example.R
import com.example.model.MiniPcProfile
import com.example.model.PreferredBrowser
import com.example.model.SetupPersona
import com.example.model.KnowledgeTier
import com.example.util.PackageFacts
import com.example.util.HostDebloat
import com.example.model.DeviceChangeChoices

import com.example.model.DeviceUseMode
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    onDismiss: () -> Unit,
    onDeviceChoicesSaved: () -> Unit
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
                            5 -> Step5DeviceChanges(uiState, viewModel)
                            6 -> Step5AutomatedDeployment(uiState, viewModel, onDismiss)
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
                            onDeviceChoicesSaved()
                            onDismiss()
                        }
                    },
                    onSaveChoices = {
                        viewModel.setDeviceSetupSaved()
                        viewModel.applySavedDeviceChoices()
                        onDeviceChoicesSaved()
                        onDismiss()
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
        "Step 1: Welcome & Who This Is For",
        "Step 2: Choose Your Primary Use Case",
        "Step 3: Linux Subsystem (Keep vs Lean)",
        "Step 4: Default Web Browser",
        "Step 5: Privacy Debloat & Chrome",
        "Step 6: Device, Display & Fold Setup",
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
    val context = LocalContext.current
    // The wizard used to open with "Storage: reading..." forever: refreshStorage() is only
    // called from the storage screens, so nothing ever sampled it here. Read it on entry.
    LaunchedEffect(Unit) { viewModel.refreshSetupFacts(context) }
    val persona = uiState.setupPersona

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PhoenixIntro()

        Text(
            text = "Who is setting this up?",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "This decides the starting point, not the ending one: mode, workload and trim list are all editable in the next steps.",
            color = Slate300,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        SetupPersona.entries.forEach { option ->
            val selected = persona == option
            val icon = when (option) {
                SetupPersona.DAILY_PHONE -> Icons.Default.Home
                SetupPersona.CYBERDECK_BUILD -> Icons.Default.Computer
                SetupPersona.MINI_PC_OWNER -> Icons.Default.Memory
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFF0C3256) else Slate900
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) PhonixCyan else Slate800,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { viewModel.selectSetupPersona(option) }
                    .testTag("persona_${option.name.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(icon, contentDescription = null, tint = if (selected) PhonixCyan else Slate300, modifier = Modifier.size(20.dp))
                            Column {
                                Text(option.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(option.subtitle, color = PhonixCyan, fontSize = 10.sp)
                            }
                        }
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(option.outcome, color = Slate300, fontSize = 10.sp, lineHeight = 14.sp)
                }
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
                    text = if (telemetry.ramTotalMb > 0) "RAM: ${telemetry.ramTotalMb} MB total" else "RAM: not sampled yet",
                    color = Slate400,
                    fontSize = 11.sp
                )
                Text(
                    text = if (uiState.storage.totalBytes > 0)
                        "Storage: ${PackageFacts.formatBytes(uiState.storage.freeBytes)} free of ${PackageFacts.formatBytes(uiState.storage.totalBytes)}"
                    else "Storage: sampling the filesystem...",
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

        // Only a build that is losing its battery needs this note. A daily driver is never asked
        // to pull hardware apart, and the switch changes nothing on the device either way.
        if (persona != null && persona.applianceMode) {
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
                                text = "Build note: battery-free DC power",
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
                        text = "This switch is a note in your setup record: it changes nothing in Android and " +
                            "touches no hardware. Leaving it off is harmless and just marks the build as still " +
                            "battery-powered.",
                        color = PhonixCyan,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "★ Recycled Mini PC Shell: Motherboards harvested from phones with cracked or unusable screens are mounted into custom recycled plastic shells with passive/active heatsinks, giving e-waste high-performance second lives!",
                        color = PhonixAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * The opening beat: a phoenix climbing out of dead hardware. Drawn rather than shipped as a
 * video, so it costs nothing extra and animates on any device that can run the app at all.
 */
@Composable
fun PhoenixIntro() {
    val transition = rememberInfiniteTransition(label = "phoenix_intro")
    val emberPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "embers"
    )
    val glow by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF351000), Color(0xFF140A05), Slate950)
                )
            )
            .border(1.dp, Color(0x33FF7A18), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0 until 18) {
                val phase = (emberPhase + i / 18f) % 1f
                val lane = ((i * 37) % 100) / 100f
                val x = size.width * (0.10f + lane * 0.80f)
                val y = size.height * (1f - phase)
                val radius = 1.2f + 2.4f * phase
                val alpha = (1f - phase) * 0.85f
                drawCircle(color = Color(0xFFFF7A18).copy(alpha = alpha), radius = radius, center = Offset(x, y))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(150.dp)
                .background(
                    Brush.radialGradient(
                        listOf(PhonixAmber.copy(alpha = 0.30f * glow), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_phoenix_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        scaleX = 0.94f + 0.06f * glow
                        scaleY = 0.94f + 0.06f * glow
                    }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "PHONIX OS",
                color = Color(0xFFFFC46B),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Old hardware, second flight",
                color = Slate300,
                fontSize = 10.sp
            )
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
            text = "What is the primary mission for this machine?",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your choice sets the cooling profile, the background services and which bloat packages are pre-ticked in the trim step. Nothing is applied yet.",
            color = Slate400,
            fontSize = 11.sp
        )
        Text(
            text = "Trim list for this profile: ${uiState.debloatPackages.count { it.isEnabled }} of ${uiState.debloatPackages.size} packages pre-ticked.",
            color = PhonixCyan,
            fontSize = 10.sp
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
            // A real PackageManager hit, so "installed" and the size below are measurements.
            // Nothing is claimed for a browser this phone has never had.
            val installed = browser.packageName?.let { pkg ->
                uiState.installedBrowsers.firstOrNull { it.packageName == pkg }
            }
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
                    when {
                        browser.packageName == null -> Text(
                            "Built into Phonix · no separate install, no extra storage",
                            color = PhonixGreen,
                            fontSize = 10.sp
                        )
                        installed != null -> Text(
                            "Installed on this phone · ${PackageFacts.formatBytes(installed.apkBytes)} of APK on disk" +
                                (installed.versionName?.let { " · v$it" } ?: ""),
                            color = PhonixGreen,
                            fontSize = 10.sp
                        )
                        else -> Text(
                            "Not installed on this phone · install it after the trim step",
                            color = PhonixAmber,
                            fontSize = 10.sp
                        )
                    }
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
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Debloat Google Chrome", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Reclaim ~180MB storage & background RAM. Routes web requests to your chosen browser.", color = Slate300, fontSize = 10.sp)
                    }
                    Switch(
                        checked = uiState.oobeConfig.debloatChrome,
                        onCheckedChange = { viewModel.toggleOobeDebloatChrome(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PhonixGreen)
                    )
                }

                if (uiState.oobeConfig.debloatChrome) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { HostDebloat.launchUninstall(context, "com.android.chrome") },
                            colors = ButtonDefaults.buttonColors(containerColor = PhonixRed.copy(alpha = 0.85f)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("1-Click Uninstall Chrome", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { HostDebloat.launchAppDetails(context, "com.android.chrome") },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("App Info (Disable)", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // OFFLINE AI KNOWLEDGE LIBRARY TIER SELECTION
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, PhonixCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Offline AI Knowledge Library Size", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Select how extensive the offline vault and AI grounding corpus should be. All tiers run 100% locally with zero internet.",
                    color = Slate300,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                KnowledgeTier.entries.forEach { tier ->
                    val isSelected = uiState.oobeConfig.offlineLibraryTier == tier
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF0F261C) else Slate950
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateOobeConfig(uiState.oobeConfig.copy(offlineLibraryTier = tier))
                            }
                            .border(1.dp, if (isSelected) PhonixGreen else Slate800, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tier.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Text(com.example.model.KnowledgeLibrary.sizeLabel(tier), color = PhonixCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                Text(tier.subtitle, color = Slate400, fontSize = 9.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Host packages: what is here, what pressing this button really does, and proof of copy.
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Host packages on this phone", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                val present = uiState.debloatPackages.filter { uiState.debloatInstalled[it.packageName] != false }
                val notHere = uiState.debloatPackages.size - present.size
                Text(
                    "Selected: ${selected.size} · this phone has ${present.size} of the ${uiState.debloatPackages.size} listed" +
                        if (notHere > 0) " ($notHere are Samsung-only and simply absent here)" else "",
                    color = Slate300,
                    fontSize = 10.sp
                )

                Text(
                    text = if (uiState.canRemoveHostPackages)
                        "This install is device owner, so Phonix can remove these itself when you press Deploy. Removal is a hide, and it is reversible."
                    else
                        "This install is not device owner, so Phonix cannot uninstall another app silently. Tap individual packages to trigger 1-click removal or copy PC commands.",
                    color = if (uiState.canRemoveHostPackages) PhonixGreen else PhonixAmber,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Button(
                    onClick = { viewModel.copyDebloatCommand() },
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier.height(34.dp).testTag("copy_debloat_button")
                ) {
                    Text("Copy adb commands for these ${selected.size} packages", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                val copied = uiState.debloatCopiedCommand
                if (copied != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2018)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, PhonixGreen, RoundedCornerShape(8.dp))
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "✔ Copied. It is in your clipboard right now, ready to paste into a terminal on your PC:",
                                color = PhonixGreen,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                            Text(
                                copied.lines().take(4).joinToString("\n") +
                                    if (copied.lines().size > 4) "\n… ${copied.lines().size - 4} more" else "",
                                color = Slate200,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Text("Ticked packages are the trim list:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        val context = androidx.compose.ui.platform.LocalContext.current
        uiState.debloatPackages.forEach { pkg ->
            val onDevice = uiState.debloatInstalled[pkg.packageName]
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        onDevice == false -> Color(0xFF141414)
                        pkg.isEnabled -> Color(0xFF0F1B2B)
                        else -> Slate900
                    }
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, if (pkg.isEnabled && onDevice != false) PhonixCyan else Slate800, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                pkg.displayName,
                                color = if (onDevice == false) Slate400 else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(pkg.packageName, color = PhonixBlueLight, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                when (onDevice) {
                                    false -> "Not on this phone - nothing to remove"
                                    true -> "${pkg.category} • ~${pkg.estimatedStorageMb}MB disk • ~${pkg.estimatedRamMb}MB RAM"
                                    null -> "${pkg.category} - checking…"
                                },
                                color = if (onDevice == false) PhonixAmber else Slate400,
                                fontSize = 9.sp
                            )
                        }
                        Switch(
                            checked = pkg.isEnabled && onDevice != false,
                            onCheckedChange = { viewModel.toggleDebloatPackage(pkg.packageName) },
                            enabled = onDevice != false,
                            colors = SwitchDefaults.colors(checkedTrackColor = PhonixGreen)
                        )
                    }

                    if (onDevice == true) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { HostDebloat.launchUninstall(context, pkg.packageName) },
                                colors = ButtonDefaults.buttonColors(containerColor = PhonixRed.copy(alpha = 0.8f)),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("1-Click Uninstall", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { HostDebloat.launchAppDetails(context, pkg.packageName) },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("App Info (Disable)", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }


        // The assistant reviews the real list, but it does not remove anything: its shell is the
        // Alpine guest, which has no adb and no authority over Android packages.
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Review this list with the assistant", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "It reads the ticked packages and what this phone actually has, then flags anything risky. It cannot perform the removal - the Linux guest has no adb - so Deploy (device owner) or the copied commands remain the only paths that change the phone.",
                    color = Slate400,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.requestDebloatAdvice() },
                        enabled = !uiState.debloatAdviceBusy && selected.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (uiState.debloatAdviceBusy) "Reviewing..." else "Review \u0024{selected.size} packages", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (uiState.debloatAdviceBusy) {
                        LinearProgressIndicator(modifier = Modifier.weight(1f).height(4.dp), color = PhonixCyan, trackColor = Slate800)
                    }
                }
                uiState.debloatAdvice?.let { advice ->
                    Text(
                        if (uiState.debloatAdviceFromModel) "From the local model" else "Built-in summary (no model running)",
                        color = if (uiState.debloatAdviceFromModel) Color(0xFFC084FC) else Slate400,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(advice, color = Slate200, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                    Text(
                        "Copy advice",
                        color = PhonixCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.copyText(advice, "advice") }
                    )
                }
            }
        }

        Text(
            "Chrome stays installed; this only stops Phonix from routing your links into it. Nothing in this step changes anything on the device by itself.",
            color = Slate400,
            fontSize = 9.sp,
            lineHeight = 13.sp
        )
    }
}

// -------------------------------------------------------------
// STEP 5: Device behavior, display route and fold posture
// -------------------------------------------------------------
@Composable
fun Step5DeviceChanges(uiState: PhonixUiState, viewModel: PhonixViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val widthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    LaunchedEffect(widthDp, uiState.foldPosture) {
        viewModel.refreshDeviceCapabilityReport(context, widthDp)
    }
    val choices = uiState.deviceChanges
    val report = uiState.deviceCapabilityReport
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Choose where Phonix will live", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (uiState.isOobeCompleted) {
            Text(
                "Review the requested changes and choose Save & Exit or Continue. Nothing in this step is applied before saving.",
                color = PhonixAmber, fontSize = 10.sp, lineHeight = 14.sp
            )
        }
        Text(
            "Daily driver keeps automatic host-device changes off. Cyberdeck opts into appliance behavior. You can change each option below.",
            color = Slate300, fontSize = 11.sp, lineHeight = 15.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(DeviceUseMode.DAILY_DRIVER, DeviceUseMode.CYBERDECK).forEach { mode ->
                val selected = choices.useMode == mode
                Card(
                    modifier = Modifier.weight(1f).clickable { viewModel.selectDeviceUseMode(mode) },
                    colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF0C3256) else Slate900),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PhonixCyan else Slate700)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (mode == DeviceUseMode.DAILY_DRIVER) "Daily driver" else "Cyberdeck / Mini-PC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (mode == DeviceUseMode.DAILY_DRIVER) "On-demand; preserve phone settings" else "Appliance-style startup and display watch", color = Slate400, fontSize = 9.sp)
                    }
                }
            }
        }

        // The two modes differ in exactly five behaviours. Spelling them out is the point of
        // this step; the switches below are still the truth and can override either preset.
        Card(colors = CardDefaults.cardColors(containerColor = Slate900), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("What actually differs", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth()) {
                    Text("", modifier = Modifier.weight(1.1f))
                    Text("Daily driver", modifier = Modifier.weight(1f), color = PhonixCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("Cyberdeck", modifier = Modifier.weight(1f), color = PhonixAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                ModeDiffRow("On a reboot", "Nothing starts", "Linux + display watcher start")
                ModeDiffRow("Wallpaper", "Untouched", "Set once, then never again")
                ModeDiffRow("Plug in a monitor", "Nothing happens", "Desktop opens by itself")
                ModeDiffRow("Battery in 24/7 use", "Normal phone behaviour", "Background service holds CPU/Wi-Fi locks")
                ModeDiffRow("Host packages", "Copy only, nothing runs", if (uiState.canRemoveHostPackages) "Removed here, reversible" else "Copy only (not device owner here)")
                Text(
                    "Neither mode enrols device owner, writes Private DNS, or enables erase. Those stay individual switches.",
                    color = Slate400,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Slate900), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Read-only compatibility check", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${report?.manufacturer ?: android.os.Build.MANUFACTURER} ${report?.model ?: android.os.Build.MODEL} · Android ${report?.androidRelease ?: android.os.Build.VERSION.RELEASE} (API ${report?.apiLevel ?: android.os.Build.VERSION.SDK_INT})", color = Slate300, fontSize = 10.sp)
                Text("Display route: ${report?.route?.name?.replace('_', ' ') ?: "checking"}", color = PhonixCyan, fontSize = 10.sp)
                Text("Window/posture: ${report?.formFactor?.name?.replace('_', ' ') ?: "checking"}", color = PhonixCyan, fontSize = 10.sp)
                Text("External displays: ${report?.externalDisplayCount ?: 0} · app target SDK: ${report?.appTargetSdk ?: "checking"}", color = Slate400, fontSize = 9.sp)
                Text("Device owner: ${if (report?.isDeviceOwner == true) "yes" else "no"} · Secure settings grant: ${if (report?.canWriteSecureSettings == true) "present" else "not granted"}", color = Slate400, fontSize = 9.sp)
                Text(report?.recommendedCheck ?: "Checking connected displays. The app cannot identify DP Alt Mode without a compatible external display attached.", color = Slate200, fontSize = 10.sp, lineHeight = 14.sp)
                Text("This check never changes display size, density, orientation, device-owner state, or system settings.", color = PhonixGreen, fontSize = 9.sp)
            }
        }

        Text("Optional device changes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        DeviceChoiceRow("Set Phonix system wallpaper", "Replaces the Android home-screen wallpaper once. You can restore your own wallpaper in Android settings.", choices.applySystemWallpaper) { viewModel.updateDeviceChanges(choices.copy(applySystemWallpaper = it)) }
        DeviceChoiceRow("Start Linux when Phonix opens", "Unpacks/starts the app-private guest on launch. It does not install a separate Android OS.", choices.startLinuxOnLaunch) { viewModel.updateDeviceChanges(choices.copy(startLinuxOnLaunch = it)) }
        DeviceChoiceRow("Keep Linux running in background", "Runs a persistent foreground service with CPU/Wi-Fi locks while enabled; can use more battery.", choices.keepLinuxInBackground) { viewModel.updateDeviceChanges(choices.copy(keepLinuxInBackground = it)) }
        DeviceChoiceRow("Start Linux automatically at boot", "Starts the Linux service after reboot and requires Keep Linux running in background to be enabled.", choices.startLinuxAtBoot) { viewModel.updateDeviceChanges(choices.copy(startLinuxAtBoot = it, keepLinuxInBackground = choices.keepLinuxInBackground || it)) }
        DeviceChoiceRow("Watch for external displays", "Keeps a foreground display watcher and can open Phonix when a monitor appears.", choices.watchExternalDisplays) { viewModel.updateDeviceChanges(choices.copy(watchExternalDisplays = it)) }
        DeviceChoiceRow("Hide system bars on external displays", "Uses immersive fullscreen only while Phonix is shown on an external display. Phone-screen bars remain visible.", choices.immersiveExternalDisplay) { viewModel.updateDeviceChanges(choices.copy(immersiveExternalDisplay = it)) }
        DeviceChoiceRow("Allow device-owner network management", "Only affects a device already provisioned as device owner; may grant Wi-Fi SSID readback and let Phonix save Wi-Fi networks. No device-owner enrollment is performed.", choices.allowDeviceOwnerNetworkManagement) { viewModel.updateDeviceChanges(choices.copy(allowDeviceOwnerNetworkManagement = it)) }
        DeviceChoiceRow("Allow Private DNS changes from Privacy Hub", "Allows the Privacy Hub to change Android-wide Private DNS when the secure-settings grant is present. Off by default.", choices.allowSystemPrivateDns) { viewModel.updateDeviceChanges(choices.copy(allowSystemPrivateDns = it)) }
        DeviceChoiceRow("Allow copying debloat commands", "Only copies adb commands to clipboard; commands do not run automatically. Never run these on your daily-driver without reviewing them.", choices.allowDebloatCommands) { viewModel.updateDeviceChanges(choices.copy(allowDebloatCommands = it)) }
        DeviceChoiceRow("Enable factory-reset control", "Exposes the existing ERASE action only on device-owner installs. A reset erases all data; leave off on a personal phone.", choices.allowFactoryReset) { viewModel.updateDeviceChanges(choices.copy(allowFactoryReset = it)) }
        Text("Device-owner setup is never done by this wizard. Keep provision.sh and dpm set-device-owner off a personal phone.", color = PhonixAmber, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun ModeDiffRow(label: String, daily: String, cyberdeck: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.weight(1.1f), color = Slate200, fontSize = 9.sp, lineHeight = 13.sp)
        Text(daily, modifier = Modifier.weight(1f), color = Slate400, fontSize = 9.sp, lineHeight = 13.sp)
        Text(cyberdeck, modifier = Modifier.weight(1f), color = Slate300, fontSize = 9.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun DeviceChoiceRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Slate900), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = Slate400, fontSize = 9.sp, lineHeight = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// -------------------------------------------------------------
// STEP 6: Automated Deployment
// -------------------------------------------------------------
@Composable
fun Step5AutomatedDeployment(
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
            text = "Finish setup",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // State what the button will actually do before it is pressed, including the case where
        // nothing can be removed. The old copy promised a debloat that could not happen.
        val selectedPackages = uiState.debloatPackages.filter { it.isEnabled }
        val removable = selectedPackages.filter { uiState.debloatInstalled[it.packageName] != false }
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("What this button will do", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "· Linux guest: " + if (uiState.oobeConfig.enableLinuxDesktop) "started now and left running" else "not installed or started",
                    color = Slate300, fontSize = 10.sp, lineHeight = 14.sp
                )
                Text(
                    "· Host packages: " + when {
                        removable.isEmpty() -> "nothing selected, nothing removed"
                        uiState.canRemoveHostPackages -> "${removable.size} hidden on this device now (reversible)"
                        else -> "nothing removed; ${removable.size} adb commands copied for you to run on a PC"
                    },
                    color = Slate300, fontSize = 10.sp, lineHeight = 14.sp
                )
                Text(
                    "· Device choices: " + buildString {
                        val c = uiState.deviceChanges
                        append(if (c.applySystemWallpaper) "wallpaper once, " else "wallpaper untouched, ")
                        append(if (c.watchExternalDisplays) "display watcher on, " else "display watcher off, ")
                        append(if (c.keepLinuxInBackground) "Linux kept alive" else "no background service")
                    },
                    color = Slate300, fontSize = 10.sp, lineHeight = 14.sp
                )
                Text(
                    "No wipe, no device-owner enrolment, and no Private DNS change happens from here.",
                    color = PhonixGreen, fontSize = 9.sp, lineHeight = 13.sp
                )
            }
        }

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
                        color = when {
                            log.startsWith("OK") || log.startsWith("✔") || log.startsWith("★") -> PhonixGreen
                            log.startsWith("x") || log.startsWith("×") -> PhonixRed
                            log.startsWith("-") -> Slate400
                            else -> Slate300
                        },
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
    onDeploy: () -> Unit,
    onSaveChoices: () -> Unit
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

            if (uiState.wizardStep == 5 && !uiState.isOobeCompleted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSaveChoices,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Save & Exit")
                    }
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                        modifier = Modifier.height(44.dp).testTag("wizard_next_button")
                    ) {
                        Text("Continue")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (uiState.wizardStep < 6) {
                Button(
                    onClick = if (uiState.wizardStep == 5) {
                        onSaveChoices
                    } else onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("wizard_next_button")
                ) {
                    Text(if (uiState.wizardStep == 5) "Save Choices" else "Next Step")
                    if (uiState.wizardStep != 5) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
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
                    Text(
                        if (uiState.isWizardDeploying) {
                            "Working..."
                        } else {
                            val removable = uiState.debloatPackages.count { it.isEnabled && uiState.debloatInstalled[it.packageName] != false }
                            when {
                                removable == 0 -> "Save choices & finish"
                                uiState.canRemoveHostPackages -> "Remove $removable & finish"
                                else -> "Copy $removable commands & finish"
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
