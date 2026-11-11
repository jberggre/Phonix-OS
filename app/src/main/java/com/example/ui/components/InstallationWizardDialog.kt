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
import com.example.model.LinuxDesktopEnvironment
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
        "Step 1: Welcome & Hardware Role",
        "Step 2: Workload & Knowledge Vault",
        "Step 3: Linux Subsystem & Desktop",
        "Step 4: Default Web Browser",
        "Step 5: Host Android Package Trim",
        "Step 6: Host & External Display Policies",
        "Step 7: Review & Launch Desktop"
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
                            "touches no hardware. Leaving it off is harmless and marks the build as battery-powered.",
                        color = PhonixCyan,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
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

        Spacer(modifier = Modifier.height(4.dp))

        // Offline AI Knowledge Library Tier Selection (fits naturally alongside the machine's workload profile)
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PhonixCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Offline AI Knowledge Vault Scope", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Choose how many offline reference shelves (Survival, Medical, Repair, Coding, Linux & Civic) are active in the Local AI & Vault window. All tiers are bundled in the APK and work 100% offline.",
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
                                    Text(tier.approxSize, color = PhonixCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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

        // Option A: Keep Full Linux
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
                        Text("Enable Full Linux Subsystem (Active)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (keepLinux) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(18.dp))
                    }
                }
                Text("Bundled Alpine (0 MB extra download) or stream Debian / Ubuntu / Arch / Kali over Wi-Fi", color = PhonixAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Provides a real PRoot Linux userland, package manager (apk / apt / pacman), compilers, X11 desktop window, and Phonix desktop shortcut pinning.",
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
                        Text("Ultra-Lean Android Mode (Skip Linux Guest)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!keepLinux) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                    }
                }
                Text("Storage Impact: 0 MB unpacked (saves ~9 MB for Alpine or 80–250 MB for full distros)", color = PhonixGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Skips unpacking the Linux container during setup to keep storage usage minimal. You can still unpack Alpine or download Debian, Ubuntu, Arch, or Kali at any time from the Linux System Hub.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        if (keepLinux) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose your starting Linux distribution:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Alpine Linux is bundled inside the APK for instant offline boot. Choosing Debian, Ubuntu, Arch, or Kali downloads the official ARM64 rootfs archive over Wi-Fi during deployment (and falls back to bundled Alpine if offline).",
                color = Slate400,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            DistroCatalog.distros.forEach { distro ->
                val isSelected = uiState.oobeConfig.selectedDistroId == distro.id
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
                        .clickable { viewModel.selectOobeDistro(distro.id) }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(distro.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = distro.packageManager.uppercase(),
                                        color = PhonixCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (distro.badgeText.isNotBlank()) {
                                    Text(
                                        text = distro.badgeText,
                                        color = if (distro.id == "alpine") PhonixGreen else PhonixAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(distro.description, color = Slate300, fontSize = 10.sp, lineHeight = 14.sp)
                        Text(
                            text = "Download: ${distro.downloadSizeEstimate} • Disk: ${distro.installedDiskFootprint}",
                            color = Slate400,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose default X11 Desktop Environment:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Controls which window manager starts inside the Linux Desktop window (DISPLAY=:1). You can also switch this live at any time.",
                color = Slate400,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            LinuxDesktopEnvironment.entries.forEach { de ->
                val isSelected = uiState.oobeConfig.selectedDe == de
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
                        .clickable { viewModel.selectOobeDe(de) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(de.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(de.ramEstimate, color = PhonixAmber, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                            Text(de.subtitle, color = Slate300, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
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

        Text(
            text = "Tip: Selecting a browser here configures the Phonix Desktop shortcut and web link routing. If you also want to disable or trim preinstalled Google Chrome (com.android.chrome) on the host phone, toggle it in Step 5.",
            color = Slate400,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

// -------------------------------------------------------------
// STEP 4: Host Android Package Trim (No duplicate Chrome card)
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
                    Text("Host Android Package Trim List", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                val present = uiState.debloatPackages.filter { uiState.debloatInstalled[it.packageName] != false }
                val notHere = uiState.debloatPackages.size - present.size
                Text(
                    "Selected: ${selected.size} · this phone has ${present.size} of the ${uiState.debloatPackages.size} listed" +
                        if (notHere > 0) " ($notHere are vendor-specific and absent on this device)" else "",
                    color = Slate300,
                    fontSize = 10.sp
                )

                Text(
                    text = if (uiState.canRemoveHostPackages)
                        "This install is device owner, so Phonix can hide the selected packages itself when you press Finish in Step 7. Hiding is reversible."
                    else
                        "This install is not device owner, so Android does not allow silent background uninstallation. Use the 1-Click Uninstall / App Info buttons on any present package below, or copy the ADB script for a PC.",
                    color = if (uiState.canRemoveHostPackages) PhonixGreen else PhonixAmber,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.copyDebloatCommand() },
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                        modifier = Modifier.height(34.dp).testTag("copy_debloat_button")
                    ) {
                        Text("Copy adb commands for these ${selected.size} packages", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
                                "✔ Copied to clipboard — ready to paste into an ADB terminal on your PC:",
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

        Text("Ticked packages in your trim list:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

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
                                    false -> "Not on this phone — nothing to remove"
                                    true -> "${pkg.category} • ~${pkg.estimatedStorageMb}MB disk • ~${pkg.estimatedRamMb}MB RAM"
                                    null -> "${pkg.category} — checking…"
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

        // Assistant safety review of the selected trim list
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Review this list with the assistant", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Checks the ticked packages against what is installed on this phone and summarizes what each category does.",
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
                        Text(if (uiState.debloatAdviceBusy) "Reviewing..." else "Review ${selected.size} packages", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    }
}

// -------------------------------------------------------------
// STEP 5: Host Device & External Display Policies
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
        Text("Host Device & Display Policies", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (uiState.isOobeCompleted) {
            Text(
                "Review or adjust host-device policies below and choose Save Choices. Nothing is changed until you save.",
                color = PhonixAmber, fontSize = 10.sp, lineHeight = 14.sp
            )
        } else {
            Text(
                "Initialized from your Step 1 role (${if (choices.useMode == DeviceUseMode.DAILY_DRIVER) "Daily Driver — all background/host changes off" else "Cyberdeck / Mini-PC — appliance startup & display watcher on"}). Fine-tune any individual policy switch below.",
                color = Slate300, fontSize = 11.sp, lineHeight = 15.sp
            )
        }

        // Compact preset bar (replaces the duplicate full-screen role selector and 5-row table)
        Card(colors = CardDefaults.cardColors(containerColor = Slate900), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quick Policy Preset:", color = Slate300, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(DeviceUseMode.DAILY_DRIVER, DeviceUseMode.CYBERDECK).forEach { mode ->
                        val selected = choices.useMode == mode
                        Surface(
                            color = if (selected) Color(0xFF0C3256) else Slate950,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PhonixCyan else Slate700),
                            modifier = Modifier.clickable { viewModel.selectDeviceUseMode(mode) }
                        ) {
                            Text(
                                text = if (mode == DeviceUseMode.DAILY_DRIVER) "Daily Driver (Safe)" else "Cyberdeck Appliance",
                                color = if (selected) PhonixCyan else Slate300,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Slate900), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Live Display & Capability Probe (Read-Only)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${report?.manufacturer ?: android.os.Build.MANUFACTURER} ${report?.model ?: android.os.Build.MODEL} · Android ${report?.androidRelease ?: android.os.Build.VERSION.RELEASE} (API ${report?.apiLevel ?: android.os.Build.VERSION.SDK_INT})", color = Slate300, fontSize = 10.sp)
                Text("Display route: ${report?.route?.name?.replace('_', ' ') ?: "checking"} · Form factor: ${report?.formFactor?.name?.replace('_', ' ') ?: "checking"}", color = PhonixCyan, fontSize = 10.sp)
                Text("External displays attached: ${report?.externalDisplayCount ?: 0} · Device owner: ${if (report?.isDeviceOwner == true) "yes" else "no"} · Secure settings grant: ${if (report?.canWriteSecureSettings == true) "present" else "not granted"}", color = Slate400, fontSize = 9.sp)
                Text(report?.recommendedCheck ?: "Checking connected displays...", color = Slate200, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }

        Text("Startup, Linux & External Display", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        DeviceChoiceRow("Set Phonix system wallpaper", "Replaces the Android home-screen wallpaper once. You can restore your own wallpaper in Android settings.", choices.applySystemWallpaper) { viewModel.updateDeviceChanges(choices.copy(applySystemWallpaper = it)) }
        DeviceChoiceRow("Start Linux when Phonix opens", "Unpacks and starts the app-private Linux guest on app launch.", choices.startLinuxOnLaunch) { viewModel.updateDeviceChanges(choices.copy(startLinuxOnLaunch = it)) }
        DeviceChoiceRow("Keep Linux running in background", "Runs a persistent foreground service with CPU/Wi-Fi locks while enabled; uses more battery.", choices.keepLinuxInBackground) { viewModel.updateDeviceChanges(choices.copy(keepLinuxInBackground = it)) }
        DeviceChoiceRow("Start Linux automatically at boot", "Starts the Linux foreground service after device reboot (requires Keep Linux in background).", choices.startLinuxAtBoot) { viewModel.updateDeviceChanges(choices.copy(startLinuxAtBoot = it, keepLinuxInBackground = choices.keepLinuxInBackground || it)) }
        DeviceChoiceRow("Watch for external displays", "Runs a foreground display listener and opens Phonix when an HDMI/DP monitor is connected.", choices.watchExternalDisplays) { viewModel.updateDeviceChanges(choices.copy(watchExternalDisplays = it)) }
        DeviceChoiceRow("Hide system bars on external displays", "Uses immersive fullscreen only when Phonix is displayed on an external monitor.", choices.immersiveExternalDisplay) { viewModel.updateDeviceChanges(choices.copy(immersiveExternalDisplay = it)) }

        Text("Device-Owner & Advanced Host Controls", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        DeviceChoiceRow("Auto-copy ADB trim commands on Finish", "Copies adb uninstall commands for your ticked Step 5 packages to the clipboard on setup completion.", choices.allowDebloatCommands) { viewModel.updateDeviceChanges(choices.copy(allowDebloatCommands = it)) }
        DeviceChoiceRow("Allow device-owner network management", "Only affects installs already provisioned as device owner; grants Wi-Fi SSID readback and network saving.", choices.allowDeviceOwnerNetworkManagement) { viewModel.updateDeviceChanges(choices.copy(allowDeviceOwnerNetworkManagement = it)) }
        DeviceChoiceRow("Allow Private DNS changes from Privacy Hub", "Lets the Privacy Hub write Android Private DNS when WRITE_SECURE_SETTINGS is granted. Off by default.", choices.allowSystemPrivateDns) { viewModel.updateDeviceChanges(choices.copy(allowSystemPrivateDns = it)) }
        DeviceChoiceRow("Enable factory-reset control", "Exposes the ERASE action only on device-owner installs. Leave off on a personal phone.", choices.allowFactoryReset) { viewModel.updateDeviceChanges(choices.copy(allowFactoryReset = it)) }
        Text("Device-owner enrollment is never performed by this wizard. Keep provision.sh and dpm set-device-owner off a personal phone.", color = PhonixAmber, fontSize = 10.sp, lineHeight = 14.sp)
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
// STEP 6: Review Configuration & Live Provisioning Console
// -------------------------------------------------------------
@Composable
fun Step5AutomatedDeployment(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    onFinished: () -> Unit
) {
    val cfg = uiState.oobeConfig
    val targetDistro = DistroCatalog.getDistro(cfg.selectedDistroId)
    val selectedPackages = uiState.debloatPackages.filter { it.isEnabled }
    val removable = selectedPackages.filter { uiState.debloatInstalled[it.packageName] != false }
    val c = uiState.deviceChanges

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Review Setup & Provision Workstation",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Card 1: Structured Review Summary of Steps 1-6
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(10.dp))
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Configuration Summary", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                SummaryRow(
                    label = "Device Role & Profile",
                    value = "${uiState.setupPersona?.title ?: "Everyday Phone"} · ${cfg.profile.title}"
                )
                SummaryRow(
                    label = "Linux Guest & X11",
                    value = if (cfg.enableLinuxDesktop) {
                        "${targetDistro.name} (${targetDistro.packageManager.uppercase()}) + ${cfg.selectedDe.title}"
                    } else {
                        "Skipped (Ultra-Lean Android Mode)"
                    }
                )
                SummaryRow(
                    label = "Browser & AI Vault",
                    value = "${cfg.preferredBrowser.title} · ${cfg.offlineLibraryTier.title}"
                )
                SummaryRow(
                    label = "Host Package Trim",
                    value = when {
                        removable.isEmpty() -> "None selected (0 packages)"
                        uiState.canRemoveHostPackages -> "${removable.size} package(s) hidden on Finish (reversible)"
                        else -> "${removable.size} package(s) — ADB script copied to clipboard"
                    }
                )
                SummaryRow(
                    label = "Host Policies",
                    value = buildString {
                        append(if (c.applySystemWallpaper) "Wallpaper once · " else "Wallpaper untouched · ")
                        append(if (c.watchExternalDisplays) "Display watcher ON · " else "Display watcher OFF · ")
                        append(if (c.keepLinuxInBackground) "24/7 service ON" else "On-demand mode")
                    }
                )
                Text(
                    "No factory wipe, no device-owner enrollment, and no Private DNS change happens during setup.",
                    color = PhonixGreen,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }

        // Card 2: Clearly Labeled Provisioning & Rootfs Extraction Console
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalBlack),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (uiState.isWizardDeploying) PhonixCyan else Slate700,
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Console Header Bar so the purpose of this window is immediately obvious
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate900)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = PhonixCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Provisioning & Rootfs Extraction Console",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val badgeText = when {
                        uiState.isWizardDeploying -> "RUNNING"
                        uiState.wizardLogs.isEmpty() -> "READY"
                        uiState.wizardLogs.any { it.startsWith("x") } -> "FINISHED (WITH WARNINGS)"
                        else -> "COMPLETE"
                    }
                    val badgeColor = when {
                        uiState.isWizardDeploying -> PhonixCyan
                        uiState.wizardLogs.isEmpty() -> PhonixAmber
                        uiState.wizardLogs.any { it.startsWith("x") } -> PhonixAmber
                        else -> PhonixGreen
                    }
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.isWizardDeploying) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate950)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = uiState.wizardStatusMessage,
                            color = PhonixCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { uiState.wizardProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PhonixGreen,
                            trackColor = Slate800
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .padding(12.dp)
                ) {
                    if (uiState.wizardLogs.isEmpty()) {
                        // Informative pre-flight checklist instead of an empty black rectangle
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Pre-flight queue (runs when you press the green Finish button below):",
                                color = PhonixCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (cfg.enableLinuxDesktop) {
                                    "1. Unpack & verify ${targetDistro.name} rootfs (${if (targetDistro.isBundled) "bundled offline archive in APK" else "official ARM64 rootfs download"})"
                                } else {
                                    "1. Skip Linux rootfs extraction (Ultra-Lean Android Mode active)"
                                },
                                color = Slate300,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = if (cfg.enableLinuxDesktop) {
                                    "2. Write guest /etc/resolv.conf, /etc/hosts, phonix-pin CLI & start ${cfg.selectedDe.title} on :1"
                                } else {
                                    "2. Configure Offline AI Knowledge Vault (${cfg.offlineLibraryTier.title})"
                                },
                                color = Slate300,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = "3. Create desktop shortcuts (${cfg.preferredBrowser.title}, File Explorer, Terminal, Settings) & save host policies",
                                color = Slate300,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Waiting for confirmation — tap the green button below to begin.",
                                color = PhonixGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
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
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = Slate400,
            fontSize = 10.sp,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            color = Slate200,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.62f)
        )
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
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 12.dp),
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
                            "Provisioning..."
                        } else {
                            val removable = uiState.debloatPackages.count { it.isEnabled && uiState.debloatInstalled[it.packageName] != false }
                            when {
                                removable == 0 -> "Provision & Launch Desktop"
                                uiState.canRemoveHostPackages -> "Remove $removable & Launch Desktop"
                                else -> "Copy $removable Commands & Launch"
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
