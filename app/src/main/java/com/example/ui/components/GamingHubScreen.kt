package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DefaultGamingCatalog
import com.example.model.GameCategory
import com.example.model.GameTitleItem
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixBlueLight
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
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
fun GamingHubScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<GameCategory?>(null) }

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
                text = "Gaming & x86 Emulation Hub",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Box64 + Wine translation, native ARM titles, and RetroArch",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        // Box64 + Wine Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("x86 Emulation Engine: Box64 0.2.8 + Wine 9.0", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF065F46))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("DYNAREC JIT ACTIVE", color = Color(0xFF6EE7B7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Translates x86 Windows game binaries into ARM64 instructions at runtime. Combined with Turnip Vulkan / VirGL GPU hardware acceleration, classic 2000s-era PC games run smoothly.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("• DXVK 2.3: Active", color = PhonixBlueLight, fontSize = 10.sp)
                    Text("• Gamepad: USB/BT Connected", color = PhonixGreen, fontSize = 10.sp)
                    Text("• Audio: PulseAudio Low-Latency", color = Slate400, fontSize = 10.sp)
                }
            }
        }

        // Category Filter Chips
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedCategory == null) PhonixBlue else Slate800)
                    .clickable { selectedCategory = null }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("All Titles", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            val categories = listOf(
                Pair(GameCategory.X86_WINDOWS, "x86 Windows (Box64)"),
                Pair(GameCategory.NATIVE_LINUX, "Native ARM64"),
                Pair(GameCategory.RETRO_CONSOLE, "RetroArch Emulation"),
                Pair(GameCategory.CLOUD_STREAM, "Game Streaming")
            )

            categories.forEach { (cat, label) ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PhonixBlue else Slate800)
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, color = if (isSelected) Color.White else Slate300, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Game Titles List
        val filteredGames = DefaultGamingCatalog.items.filter {
            selectedCategory == null || it.category == selectedCategory
        }

        filteredGames.forEach { game ->
            GameItemCard(
                game = game,
                onLaunch = {
                    Toast.makeText(context, "Launching ${game.name} with ${game.engineRunner}...", Toast.LENGTH_SHORT).show()
                    viewModel.launchGame(game)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GameItemCard(
    game: GameTitleItem,
    onLaunch: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Slate800, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(game.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(game.engineRunner, color = PhonixBlueLight, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                    modifier = Modifier.height(34.dp).testTag("launch_game_${game.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Launch", fontSize = 11.sp)
                }
            }

            Text(game.description, color = Slate300, fontSize = 11.sp, lineHeight = 15.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rating: ${game.compatibilityRating}",
                    color = if (game.compatibilityRating.contains("Flawless") || game.compatibilityRating.contains("Perfect")) PhonixGreen else PhonixAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "RAM: ${game.ramRequirementMb} MB",
                    color = Slate400,
                    fontSize = 10.sp
                )
            }
        }
    }
}
