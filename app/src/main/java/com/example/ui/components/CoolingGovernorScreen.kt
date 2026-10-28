package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CoolingMode
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
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel
import java.util.Locale

@Composable
fun CoolingGovernorScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val cooling = uiState.coolingState
    val t = uiState.telemetry

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Active Cooling & Thermal Governor",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Motherboard heatsink telemetry & sustained clock tuning",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        // Active Sustained Status Hero Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (cooling.activeMode == CoolingMode.ACTIVE_MODDED_COOLING) Color(0xFF0F261C) else Slate900
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, if (cooling.activeMode == CoolingMode.ACTIVE_MODDED_COOLING) PhonixGreen else Slate800, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Motherboard Profile", color = Slate400, fontSize = 11.sp)
                        Text(cooling.activeMode.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (cooling.activeMode == CoolingMode.ACTIVE_MODDED_COOLING) PhonixGreen else Slate700)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (cooling.activeMode == CoolingMode.ACTIVE_MODDED_COOLING) "+${cooling.sustainedPerformanceBoostPercent}% BOOSTED" else "PASSIVE",
                            color = if (cooling.activeMode == CoolingMode.ACTIVE_MODDED_COOLING) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                HorizontalDivider(color = Slate800)

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("SoC Core Temp", color = Slate400, fontSize = 11.sp)
                        Text(
                            text = String.format(Locale.ROOT, "%.1f°C", cooling.socTempC),
                            color = if (cooling.socTempC < 45f) PhonixGreen else PhonixAmber,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text("Sustained Clock", color = Slate400, fontSize = 11.sp)
                        Text(
                            text = "${cooling.currentCpuClockGhz} GHz",
                            color = PhonixCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Active Fan Est.", color = Slate400, fontSize = 11.sp)
                        Text(
                            text = if (cooling.activeMode == CoolingMode.ACTIVE_MODDED_COOLING) "${cooling.fanEstimatedRpm} RPM" else "0 RPM",
                            color = if (cooling.activeMode == CoolingMode.ACTIVE_MODDED_COOLING) PhonixBlueLight else Slate400,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Mode Selector Cards
        Text(
            text = "Select Hardware Cooling Configuration:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        CoolingMode.values().forEach { mode ->
            val isSelected = cooling.activeMode == mode
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
                    .clickable { viewModel.setCoolingMode(mode) }
                    .testTag("cooling_mode_${mode.name.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(mode.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(mode.subtitle, color = PhonixBlueLight, fontSize = 11.sp)
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

                    Text(mode.description, color = Slate300, fontSize = 11.sp, lineHeight = 15.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Clock limit: ${mode.maxClockGhz} GHz", color = PhonixCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Thermal cap: ${mode.thermalLimitC}°C", color = Slate400, fontSize = 10.sp)
                        Text(if (mode.isThrottleBypassed) "Throttle: BYPASSED" else "Throttle: ACTIVE", color = if (mode.isThrottleBypassed) PhonixGreen else PhonixAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Hardware Modding Blueprint & Walkthrough for Samsung S8 / Note 8 / S9 / S10
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Motherboard Modding Guide (S8 / Note 8 / S9 / S10):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "1. Remove Back Glass: Use a hairdryer/heat gun to soften the adhesive and remove the phone rear glass cover.",
                    color = Slate200,
                    fontSize = 11.sp
                )
                Text(
                    text = "2. Locate SoC Shield: Place a 20x20mm copper thermal pad (1.0mm thickness) directly over the motherboard processor shield.",
                    color = Slate200,
                    fontSize = 11.sp
                )
                Text(
                    text = "3. Attach Heatsink: Mount a 25mm-40mm mini aluminum or copper fin heatsink using thermal adhesive tape.",
                    color = Slate200,
                    fontSize = 11.sp
                )
                Text(
                    text = "4. Wire 5V Fan: Power a quiet 5V 30mm/40mm blower fan using the 5V line of a USB-C hub or external 5V wall adapter.",
                    color = Slate200,
                    fontSize = 11.sp
                )
                Text(
                    text = "5. Battery Safety: For 24/7 server racks, use a smart plug timer (cycling charge 40%-80%) or wire a dummy battery connector to eliminate fire hazards.",
                    color = PhonixAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
