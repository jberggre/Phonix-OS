package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NetworkLinkState
import com.example.model.NetworkTransport
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixBlue
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.PhonixRed
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

/**
 * First-run networking for a machine with no phone screen.
 *
 * Deliberately plain: one status block, two fields, and two ways out. The second way out
 * ("Open Android Wi-Fi settings") is not a fallback for a broken feature — it is the honest
 * route for anyone who would rather pick from the system's list of nearby networks, which
 * needs location permission that Phonix does not want.
 */
@Composable
fun NetworkSetupScreen(
    state: NetworkLinkState,
    busy: Boolean,
    message: String?,
    canDismiss: Boolean,
    onConnect: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val submit = {
        if (!busy && ssid.isNotBlank()) onConnect(ssid.trim(), password)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth()) {

                Text(
                    text = "Connect Phonix",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "One network setup per unit. After this it stays connected and boots straight to the desktop.",
                    color = Slate400,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(16.dp))

                StatusCard(state = state, onRefresh = onRefresh)

                Spacer(Modifier.height(14.dp))

                if (!state.online) {
                    Surface(
                        color = Slate900,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate700, RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Join a Wi-Fi network",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Type the network name exactly as it appears on the router label. " +
                                    "Phonix hands the details to Android, which saves them; the app keeps no copy.",
                                color = Slate400,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = ssid,
                                onValueChange = { ssid = it },
                                singleLine = true,
                                label = { Text("Wi-Fi network name (SSID)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next,
                                    autoCorrectEnabled = false
                                ),
                                colors = darkFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                singleLine = true,
                                label = { Text("Password", fontSize = 11.sp) },
                                visualTransformation = if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { submit() }),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = if (passwordVisible) {
                                                "Hide password"
                                            } else {
                                                "Show password"
                                            },
                                            tint = Slate400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = darkFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = submit,
                                    enabled = !busy && ssid.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PhonixBlue),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    if (busy) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = if (busy) "Connecting..." else "Connect",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Open network? Leave the password empty.",
                                    color = Slate400,
                                    fontSize = 10.sp
                                )
                            }

                            if (message != null) {
                                Text(
                                    text = message,
                                    color = PhonixAmber,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }

                // Always available, whether or not the form above works on this unit.
                Surface(
                    color = Slate900.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = PhonixCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Prefer Android's own picker?",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Android's Wi-Fi screen lists every nearby network, so you pick from a list " +
                                "instead of typing. It opens here on the monitor as a DeX window and the " +
                                "network it saves is the same one Phonix uses.",
                            color = Slate400,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Open Android Wi-Fi settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SettingsEthernet,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Have the USB-C hub with Ethernet? Plug it in and this screen is unnecessary — " +
                            "Phonix uses the wired link automatically and will not ask for Wi-Fi again.",
                        color = Slate400,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }

                if (canDismiss) {
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            if (state.online) "Continue to the desktop" else "Continue without a network",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(state: NetworkLinkState, onRefresh: () -> Unit) {
    val accent = when {
        state.online -> PhonixGreen
        state.transport != NetworkTransport.NONE -> PhonixAmber
        else -> PhonixRed
    }
    Surface(
        color = Slate900,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    state.online -> Icons.Default.CheckCircle
                    state.transport == NetworkTransport.ETHERNET -> Icons.Default.SettingsEthernet
                    state.transport == NetworkTransport.NONE -> Icons.Default.WifiOff
                    else -> Icons.Default.Wifi
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.online) "Online" else state.transport.label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.summary,
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                if (state.localIpv4 != null) {
                    Text(
                        text = "This machine: ${state.localIpv4}",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Check the connection again",
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = PhonixCyan,
    unfocusedBorderColor = Slate700,
    focusedLabelColor = PhonixCyan,
    unfocusedLabelColor = Slate400,
    cursorColor = PhonixCyan,
    focusedContainerColor = Slate950,
    unfocusedContainerColor = Slate950
)
