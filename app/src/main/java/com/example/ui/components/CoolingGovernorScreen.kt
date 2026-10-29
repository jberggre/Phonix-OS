package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.model.CoolingMode
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.PhonixRed
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel
import java.util.Locale

/**
 * Thermals and power, told straight.
 *
 * An unrooted Android app cannot set a CPU governor, lift thermal throttling, read a fan or
 * read the SoC's own temperature. The numbers on this screen are the ones Android does expose
 * - the battery temperature and the CPU core frequencies - and the rest is build guidance.
 */
@Composable
fun CoolingGovernorScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry = uiState.telemetry

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Thermals & Power",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Live readings from this phone, plus build guidance",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = Slate900), shape = RoundedCornerShape(10.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Thermostat, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                if (!telemetry.hasReading) {
                    Text("Reading the sensors...", color = Slate400, fontSize = 11.sp)
                } else {
                    val temp = telemetry.hottestZoneC
                    Text(
                        text = String.format(Locale.ROOT, "%.1f°C", temp),
                        color = when {
                            temp <= 0f -> Slate400
                            temp >= 42f -> PhonixRed
                            temp >= 38f -> PhonixAmber
                            else -> PhonixGreen
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Hottest thermal zone Android exposes (/sys/class/thermal). Above roughly 75-80°C the " +
                            "phone is throttling; above 42°C on the battery sensor alone means the cell is hot.",
                        color = Slate400,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("CPU core frequency utilisation: ${telemetry.cpuPercent}%", color = Slate300, fontSize = 11.sp)
                    LinearProgressIndicator(
                        progress = { telemetry.cpuPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = PhonixCyan,
                        trackColor = Slate800
                    )
                    Text(
                        "Average of each core's current clock against its maximum. Android does not " +
                            "expose /proc/stat to apps, so this is the honest alternative.",
                        color = Slate400,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )

                    Text(
                        "Battery: ${if (telemetry.batteryLevel >= 0) "${telemetry.batteryLevel}%" else "-"}" +
                            if (telemetry.isCharging) " · charging" else "",
                        color = Slate300,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Text(
            text = "How do you plan to run this unit?",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        CoolingMode.entries.forEach { mode ->
            val isSelected = uiState.coolingState.activeMode == mode
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF0F2530) else Slate900
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
                    .testTag("cooling_mode_${mode.name}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(mode.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(mode.subtitle, color = PhonixCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text(mode.description, color = Slate300, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate700, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Power and the battery", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "A phone that stays plugged in and running is a battery that is charging constantly, " +
                        "which is what ages these cells and, in the worst case, puffs them. If you are " +
                        "building a 24/7 unit, plan for the battery: limit charging, run cooler, or remove " +
                        "the cell and power the board directly with a regulator rated for it.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Battery physically removed from this unit",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Switch(
                        checked = uiState.isBatteryRemovedDirectPower,
                        onCheckedChange = { viewModel.toggleBatteryRemovedDirectPower(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PhonixGreen)
                    )
                }
                Text(
                    "This only labels your build; the app cannot tell whether a battery is present. With a " +
                        "battery in place, check the reading above rather than trusting a stored setting.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Button(
                    onClick = { viewModel.setActiveTab(com.example.viewmodel.AppNavTab.SERVERS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Server settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate700, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("What this app cannot do", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Setting a CPU governor, disabling thermal throttling, reading SoC temperature or " +
                        "controlling a fan all require root or a kernel interface Android does not expose. " +
                        "Phonix runs unrooted, so it shows you the sensors it does have and stays out of " +
                        "the way otherwise.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
