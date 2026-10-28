package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppCategory
import com.example.model.DesktopThemePreset
import com.example.model.MiniPcProfile
import com.example.model.OobeSetupConfig
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixBlueLight
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel

@Composable
fun OobeWizardScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4
    val config = uiState.oobeConfig

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp)
                .shadow(32.dp, RoundedCornerShape(20.dp))
                .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("oobe_wizard_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header & Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_phoenix_logo),
                            contentDescription = "Phonix Emblem",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Phonix OS Setup",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PhonixAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Mini PC Edition", color = PhonixAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Step ${currentStep + 1} of $totalSteps",
                        color = Slate400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = PhonixBlue,
                    trackColor = Slate800
                )

                Spacer(Modifier.height(18.dp))

                // Step Content Area with animated slide transitions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }
                        },
                        label = "oobe_step_animation"
                    ) { step ->
                        when (step) {
                            0 -> OobeStepWelcome(config = config, onConfigChanged = { viewModel.updateOobeConfig(it) })
                            1 -> OobeStepZeroWasteApps(config = config, onToggleCategory = { viewModel.toggleOobeCategory(it) })
                            2 -> OobeStepHardwareDebloat(config = config, onConfigChanged = { viewModel.updateOobeConfig(it) })
                            3 -> OobeStepPersonalization(
                                config = config,
                                onConfigChanged = { viewModel.updateOobeConfig(it) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Bottom Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Slate700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                            modifier = Modifier.testTag("oobe_prev_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Back", fontSize = 12.sp)
                        }
                    } else {
                        Spacer(Modifier.width(10.dp))
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = { currentStep++ },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                            modifier = Modifier.testTag("oobe_next_button")
                        ) {
                            Text("Next", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.completeOobeSetup(context) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PhonixGreen),
                            modifier = Modifier.testTag("oobe_finish_button")
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Launch Phonix Desktop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OobeStepWelcome(
    config: OobeSetupConfig,
    onConfigChanged: (OobeSetupConfig) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Welcome to your new Mini PC",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Reclaimed smartphone silicon repurposed in recycled plastic. Choose your use profile:",
            color = Slate400,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))

        // Device Hostname
        OutlinedTextField(
            value = config.deviceName,
            onValueChange = { onConfigChanged(config.copy(deviceName = it)) },
            label = { Text("PC Name / Hostname", color = Slate400, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("oobe_device_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PhonixBlue,
                unfocusedBorderColor = Slate700,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Slate950,
                unfocusedContainerColor = Slate950
            )
        )

        Spacer(Modifier.height(12.dp))

        // Profile Cards
        Text(text = "Select Use Profile:", color = Slate300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MiniPcProfile.entries.toTypedArray()) { profile ->
                val isSelected = config.profile == profile
                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onConfigChanged(config.copy(profile = profile)) }
                        .testTag("profile_${profile.name}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PhonixBlue.copy(alpha = 0.2f) else Slate800.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) PhonixBlue else Slate700.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (profile) {
                                    MiniPcProfile.EVERYDAY_DESKTOP -> Icons.Default.Computer
                                    MiniPcProfile.HOMELAB_SERVER -> Icons.Default.Dns
                                    MiniPcProfile.GAMING_EMULATION -> Icons.Default.Gamepad
                                    MiniPcProfile.MINIMAL_LEAN -> Icons.Default.Speed
                                },
                                contentDescription = null,
                                tint = if (isSelected) PhonixCyan else Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = profile.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = profile.subtitle,
                            color = PhonixAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = profile.description,
                            color = Slate400,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OobeStepZeroWasteApps(
    config: OobeSetupConfig,
    onToggleCategory: (AppCategory) -> Unit
) {
    val totalEstimatedExtraMb = config.selectedCategories.sumOf { it.estStorageMb }
    val savedFlashGb = 4.8f // Average storage saved compared to full desktop re-partition

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Zero-Waste App Selection",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select only what you need. Native phone apps reuse existing flash storage.",
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            // Storage Meter Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate800)
                    .border(1.dp, PhonixGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Flash Used: ~${totalEstimatedExtraMb} MB",
                        color = PhonixGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Saved ~${savedFlashGb} GB internal space",
                        color = Slate300,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(AppCategory.entries.toTypedArray()) { cat ->
                val isChecked = config.selectedCategories.contains(cat)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleCategory(cat) }
                        .testTag("app_cat_${cat.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChecked) Slate800.copy(alpha = 0.7f) else Slate950.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isChecked) PhonixBlue.copy(alpha = 0.6f) else Slate800
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleCategory(cat) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PhonixBlue,
                                uncheckedColor = Slate400
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cat.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                if (cat.isHostPhoneApp) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PhonixCyan.copy(alpha = 0.15f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("Host Phone App (0 MB)", color = PhonixCyan, fontSize = 9.sp)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PhonixAmber.copy(alpha = 0.15f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("+${cat.estStorageMb} MB", color = PhonixAmber, fontSize = 9.sp)
                                    }
                                }
                            }
                            Text(
                                text = cat.subtitle,
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OobeStepHardwareDebloat(
    config: OobeSetupConfig,
    onConfigChanged: (OobeSetupConfig) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Screenless Mini PC Debloater",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Optimizing the salvaged smartphone motherboard for permanent desktop operation.",
            color = Slate400,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, Slate700)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PhonixCyan,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aggressive Mini PC Debloat Engine",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Disables cellular radios, SMS services, carrier diagnostic telemetry, and mobile biometrics to reclaim ~1.5 GB RAM and eliminate heat.",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = config.enableAggressiveDebloat,
                    onCheckedChange = { onConfigChanged(config.copy(enableAggressiveDebloat = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PhonixBlue
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Bullet points of benefits
        val benefits = listOf(
            "Reclaims ~1.5GB RAM for desktop multitasking and browser tabs",
            "Disables cellular radio polling (no SIM or antenna needed in Mini PC)",
            "Bypasses mobile lockscreen and fingerprint scanner background daemons",
            "Reduces idle CPU thermals by 4°C - 8°C on reclaimed smartphone SoCs"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            benefits.forEach { b ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = b, color = Slate300, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun OobeStepPersonalization(
    config: OobeSetupConfig,
    onConfigChanged: (OobeSetupConfig) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Desktop Personalization",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Customize your Windows desktop aesthetic and taskbar preference.",
            color = Slate400,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(12.dp))

        Text("Color & Theme Preset:", color = Slate300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DesktopThemePreset.entries.forEach { theme ->
                val isSelected = config.themePreset == theme
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onConfigChanged(config.copy(themePreset = theme)) }
                        .testTag("theme_${theme.name}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PhonixBlue.copy(alpha = 0.2f) else Slate800.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, if (isSelected) PhonixBlue else Slate700)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = theme.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = theme.description,
                            color = Slate400,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Taskbar Alignment
        Text("Taskbar Alignment:", color = Slate300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onConfigChanged(config.copy(taskbarCentered = true)) },
                colors = CardDefaults.cardColors(
                    containerColor = if (config.taskbarCentered) PhonixBlue.copy(alpha = 0.2f) else Slate800.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (config.taskbarCentered) PhonixBlue else Slate700)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Centered (Windows 11)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Fluent centered icons & flyout", color = Slate400, fontSize = 9.sp)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onConfigChanged(config.copy(taskbarCentered = false)) },
                colors = CardDefaults.cardColors(
                    containerColor = if (!config.taskbarCentered) PhonixBlue.copy(alpha = 0.2f) else Slate800.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (!config.taskbarCentered) PhonixBlue else Slate700)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Computer, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Left Aligned (Classic)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Traditional Start Menu button", color = Slate400, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
