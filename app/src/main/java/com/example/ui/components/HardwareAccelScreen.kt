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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.model.DesktopResolution
import com.example.model.GpuDriverType
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
import com.example.ui.theme.TerminalBlack
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel
import java.util.Locale

@Composable
fun HardwareAccelScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val hw = uiState.hwConfig
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
                text = "Hardware Acceleration & GPU Pipeline",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "VirGL GLES 3.1, Turnip Vulkan, and Samsung DeX Docking",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        // Active GPU Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active GPU Driver", color = Slate400, fontSize = 11.sp)
                        Text(hw.gpuDriver.label, color = PhonixCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF065F46))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("60 FPS DRI3 VSYNC", color = Color(0xFF6EE7B7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = hw.gpuDriver.rendererName,
                    color = PhonixBlueLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // GPU Driver Selection
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select GPU Rendering Engine:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                GpuDriverType.values().forEach { driver ->
                    val isSelected = hw.gpuDriver == driver
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF0C3256) else Slate950)
                            .border(1.dp, if (isSelected) PhonixCyan else Slate800, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setGpuDriver(driver) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(driver.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(driver.description, color = Slate400, fontSize = 10.sp)
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PhonixGreen)
                                    .size(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // Display Resolution & Samsung DeX Dock Mode
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Display & Samsung DeX HDMI Station:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                DesktopResolution.values().forEach { res ->
                    val isSelected = hw.resolution == res
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Slate800 else Slate950)
                            .clickable { viewModel.setResolution(res) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(res.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(res.notes, color = Slate400, fontSize = 10.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 3D GPU Benchmark Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("glmark2-es2 3D Benchmark", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Verifies shader compiling and VirGL 3D frame rates", color = Slate400, fontSize = 11.sp)
                    }
                    if (uiState.benchmarkScore > 0) {
                        Text("${uiState.benchmarkScore} pts", color = PhonixGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }

                Button(
                    onClick = { viewModel.run3dGpuBenchmark() },
                    enabled = !uiState.isBenchmarkRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isBenchmarkRunning) "Executing Benchmark Scenes..." else "Run 3D GPU Benchmark")
                }

                if (uiState.isBenchmarkRunning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PhonixCyan)
                }

                if (uiState.benchmarkLog.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(TerminalBlack)
                            .padding(8.dp)
                    ) {
                        Column {
                            uiState.benchmarkLog.takeLast(5).forEach { log ->
                                Text(log, color = if (log.contains("Score")) PhonixGreen else Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Thermal Guardian & Battery Protection
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF291B07)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thermal Guardian & 24/7 Battery Protection", color = Color(0xFFFDE68A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Current Battery Temp: ${String.format(Locale.ROOT, "%.1f°C", t.batteryTempC)} (Auto-Throttled at 42°C)",
                    color = if (t.batteryTempC > 40f) PhonixRed else PhonixGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Text(
                    text = "When turning older Samsung phones into 24/7 servers, avoid keeping batteries permanently at 100% in hot environments. Using a smart plug or low-wattage 5V charger prevents battery degradation.",
                    color = Color(0xFFFCE7F3),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
