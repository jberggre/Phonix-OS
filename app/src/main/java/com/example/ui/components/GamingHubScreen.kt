package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameEntry
import com.example.model.GameKind
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.AppLaunchHelper
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel
import com.example.model.GameCatalog

/**
 * Games, honestly.
 *
 * The previous version listed Fallout: New Vegas, Half-Life 2 and friends with invented frame
 * rates ("Perfect 55-60 FPS") and a launch button that printed fake VirGL logs. None of that
 * was measured, packaged or tested. This screen now shows a catalogue that can be checked:
 * every title is a real store listing, the button reflects what the package manager reports,
 * and nothing here promises a frame rate.
 */
@Composable
fun GamingHubScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installed = uiState.installedApps.map { it.packageName }.toSet()

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
                text = "Games",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Free titles you can install now, plus what this hardware honestly cannot do",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PhonixGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install and play", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "These run natively on Android, on the phone's own GPU, exactly as they do on " +
                        "the phone. Pressing Install opens the store listing; Play only appears once the " +
                        "package is actually installed. No frame rate is promised.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Text(
            text = "Games",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        GameCatalog.games.forEach { entry ->
            GameCard(
                entry = entry,
                isInstalled = installed.contains(entry.packageId),
                onInstall = { AppLaunchHelper.openPlayListing(context, entry.packageId) },
                onPlay = { AppLaunchHelper.launchNativeApp(context, entry.packageId) }
            )
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = "Emulation",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Phonix bundles no ROMs and no BIOS files. Install an emulator, then supply games you " +
                "own or freely licensed homebrew. That is a legal requirement, not a limitation of the app.",
            color = Slate400,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
        GameCatalog.emulators.forEach { entry ->
            GameCard(
                entry = entry,
                isInstalled = installed.contains(entry.packageId),
                onInstall = { AppLaunchHelper.openPlayListing(context, entry.packageId) },
                onPlay = { AppLaunchHelper.launchNativeApp(context, entry.packageId) }
            )
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = "Play in the browser — no install",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        GameCatalog.browserGames.forEach { game ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = PhonixCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(game.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(game.blurb, color = Slate300, fontSize = 10.sp, lineHeight = 14.sp)
                    OutlinedButton(
                        onClick = { AppLaunchHelper.launchWebBrowser(context, game.url) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Open in browser", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
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
                    Icon(Icons.Default.Memory, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hardware limits", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "External GPU: the Galaxy S10's USB-C port is USB 3.1 with DisplayPort Alt Mode. " +
                        "It has no Thunderbolt or USB4, so there is no PCIe tunnel and an eGPU cannot be " +
                        "attached to this unit. Faster graphics on this hardware means a newer phone, not " +
                        "an accessory.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "Controller: Bluetooth and USB gamepads pair with Android normally and work in " +
                        "Android games. Linux games inside the guest do not see them yet.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.openAppStore(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Open app store", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.toggleWindow(com.example.model.WindowType.BENCHMARK) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Benchmark", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("What has not been tested", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "This unit currently has no active cooling, so the catalog was verified by " +
                        "listing (store IDs resolve, package names exist) and not by playing each title " +
                        "through a long session. Long-session thermals are still an open measurement.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun GameCard(
    entry: GameEntry,
    isInstalled: Boolean,
    onInstall: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.genre,
                        color = PhonixCyan,
                        fontSize = 9.sp
                    )
                }
                Text(entry.blurb, color = Slate300, fontSize = 10.sp, lineHeight = 14.sp)
                Text(
                    text = "${entry.price} • ${entry.licence}",
                    color = Slate400,
                    fontSize = 9.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            if (isInstalled) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = PhonixGreen.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play", color = PhonixGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onInstall,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Slate300, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Install", color = Slate300, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
