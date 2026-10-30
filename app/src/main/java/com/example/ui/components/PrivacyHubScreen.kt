package com.example.ui.components

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WindowType
import com.example.ui.theme.PhonixAmber
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.PhonixRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.PhonixViewModel

/**
 * Privacy Hub.
 *
 * Two things a recycled phone is genuinely good for, plus one it is not:
 *
 *  - Blocking ads on itself, through Android's Private DNS. That is a real system setting and
 *    the app can apply it once provisioning grants WRITE_SECURE_SETTINGS.
 *  - Walking the owner through giving a spare Pi or old PC a second life as the household
 *    DNS filter. The phone cannot do that job, and the screen says so instead of shipping a
 *    toggle that quietly fails.
 *  - Erasing itself cleanly at end of life, but only when Phonix is the device owner. A plain
 *    app has no business wiping a phone, so that path opens Samsung's own reset screen instead.
 */
@Composable
fun PrivacyHubWindowContent(viewModel: PhonixViewModel) {
    val context = LocalContext.current
    val deviceOwner = remember {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        dpm?.isDeviceOwnerApp(context.packageName) == true
    }

    var selectedHost by rememberSaveable { mutableStateOf(ADGUARD_HOST) }
    var customHost by rememberSaveable { mutableStateOf("") }
    var notice by remember { mutableStateOf<Notice?>(null) }
    var currentDns by remember { mutableStateOf(readPrivateDns(context)) }

    var wipeDialogOpen by remember { mutableStateOf(false) }
    var wipeConfirmText by remember { mutableStateOf("") }
    var wipeError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF0C2B1E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Privacy Hub", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Cut the ads, keep the box yours",
                    color = Slate400,
                    fontSize = 12.sp
                )
            }
        }

        // ---------------------------------------------------------------
        // 1. This phone, right now
        // ---------------------------------------------------------------
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Slate800, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ad blocking on this phone", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Android can send every DNS lookup through an encrypted, filtering resolver. " +
                        "It applies to every app and browser on the phone, needs no root, and survives " +
                        "a reboot. This is the fastest thing you can turn on today.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Text(
                    text = "Resolver: $currentDns",
                    color = PhonixCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                HorizontalDivider(color = Slate800)

                PresetRow(
                    label = "AdGuard DNS",
                    detail = "Ads and trackers",
                    selected = selectedHost == ADGUARD_HOST && customHost.isBlank(),
                    onClick = {
                        selectedHost = ADGUARD_HOST
                        customHost = ""
                        notice = null
                    }
                )
                PresetRow(
                    label = "AdGuard Family",
                    detail = "Ads, trackers, adult content",
                    selected = selectedHost == ADGUARD_FAMILY_HOST && customHost.isBlank(),
                    onClick = {
                        selectedHost = ADGUARD_FAMILY_HOST
                        customHost = ""
                        notice = null
                    }
                )

                OutlinedTextField(
                    value = customHost,
                    onValueChange = {
                        customHost = it.trim()
                        notice = null
                    },
                    label = { Text("Or your own resolver hostname", fontSize = 11.sp) },
                    placeholder = { Text("pihole.example.com", fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PhonixCyan,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200,
                        cursorColor = PhonixCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val effectiveHost = customHost.ifBlank { selectedHost }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (!com.example.util.DeviceSetupPreferences.mayApplySystemPrivateDns(context)) {
                                notice = Notice("System-wide Private DNS changes are disabled. Enable them in Device & Display Setup first.", false)
                                return@Button
                            }
                            val applied = tryApplyPrivateDns(context, effectiveHost)
                            currentDns = readPrivateDns(context)
                            notice = if (applied) {
                                Notice("Private DNS set to $effectiveHost.", true)
                            } else {
                                copyToClipboard(context, "Private DNS host", effectiveHost)
                                openPrivateDnsSettings(context)
                                Notice(
                                    "Host copied. Open Connections > More connection settings > Private DNS, " +
                                        "choose Private DNS provider hostname and paste it.",
                                    true
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixCyan),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Block ads now", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { openPrivateDnsSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Connection settings", fontSize = 11.sp)
                    }
                }

                Text(
                    "This blocks ads at the DNS layer only. In-page ads served from the same domain " +
                        "as the content still appear, and the resolver can see your lookups. For a " +
                        "resolver nobody else runs, use the Pi-hole walkthrough below.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                if (notice != null) {
                    NoticeText(notice!!)
                }
            }
        }

        // ---------------------------------------------------------------
        // 2. The household filter
        // ---------------------------------------------------------------
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Slate700, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Filter the whole house with Pi-hole", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B08)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, PhonixAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = PhonixAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("This phone cannot be the DNS server", color = PhonixAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Pi-hole has to listen on port 53. Android does not allow an app, even one " +
                                "running a Linux container, to bind ports below 1024 - that is a kernel rule, " +
                                "not a missing package, and it needs root to change. A phone is also a poor " +
                                "24/7 DNS server because it sleeps.",
                            color = Slate200,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                        Text(
                            "So the honest setup is: Pi-hole runs on a spare Raspberry Pi or an old PC " +
                                "that would otherwise be e-waste, and this phone helps you set it up.",
                            color = Slate300,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                GuideStep(
                    number = 1,
                    title = "Pick the box",
                    body = "A Raspberry Pi 3/4/5, or any x86 machine with Debian, Ubuntu or Raspberry Pi OS. " +
                        "Wire it to the router with Ethernet, and give it a fixed address in your router's " +
                        "DHCP reservation so the DNS address never moves."
                )

                GuideStep(
                    number = 2,
                    title = "Install Pi-hole there",
                    body = "Over SSH, or on its own desktop terminal. The official installer pulls the rest."
                )
                CommandRow(
                    command = "curl -sSL https://install.pi-hole.net | bash",
                    onCopy = {
                        copyToClipboard(context, "Pi-hole install", "curl -sSL https://install.pi-hole.net | bash")
                        notice = Notice("Install command copied.", true)
                    }
                )
                Text(
                    "If it stops for permissions, run it as: curl -sSL https://install.pi-hole.net | sudo bash",
                    color = Slate400,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                GuideStep(
                    number = 3,
                    title = "Point the house at it",
                    body = "In the router's DHCP settings, set the primary DNS server to the Pi's fixed IP. " +
                        "Leave the secondary blank, or point it at a second Pi-hole. A public secondary " +
                        "like 8.8.8.8 is a bypass: whenever it answers, the filtering is gone. Then renew " +
                        "DHCP leases or reboot the clients."
                )

                GuideStep(
                    number = 4,
                    title = "Prove it is filtering",
                    body = "From the Phonix terminal, ask the Pi about a known ad domain. A Pi-hole reply " +
                        "of 0.0.0.0 (or NXDOMAIN) means it is blocking; a real public IP means the client " +
                        "is not using it yet."
                )
                CommandRow(
                    command = "nslookup doubleclick.net <pi-ip>",
                    onCopy = {
                        copyToClipboard(context, "Pi-hole test", "nslookup doubleclick.net <pi-ip>")
                        notice = Notice("Test command copied.", true)
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.toggleWindow(WindowType.TERMINAL) },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Open terminal", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { openUrl(context, "https://docs.pi-hole.net/main/basic-install/") },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Pi-hole docs", fontSize = 11.sp)
                    }
                }

                Text(
                    "Prefer no extra hardware? AdGuard Home and Pi-hole also run in Docker on any " +
                        "always-on Linux box. The same three rules apply: fixed IP, port 53, and no public " +
                        "secondary DNS.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // ---------------------------------------------------------------
        // 3. End of life
        // ---------------------------------------------------------------
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Slate800, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Decommission this unit", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "A prescribed unit is locked down: Phonix is the device owner, the reset option in " +
                        "Settings is blocked, and that is deliberate. When a machine comes back, wipe it " +
                        "from here before it is resold or recycled.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (deviceOwner) PhonixGreen else Slate600)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        if (deviceOwner) "This unit is Phonix-managed (device owner)."
                        else "This unit is not device owner, so Phonix cannot erase it.",
                        color = if (deviceOwner) PhonixGreen else Slate400,
                        fontSize = 10.sp
                    )
                }

                if (deviceOwner && com.example.util.DeviceSetupPreferences.choices(context).allowFactoryReset) {
                    Button(
                        onClick = {
                            wipeConfirmText = ""
                            wipeError = null
                            wipeDialogOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixRed),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Erase and decommission", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "This is a real factory reset: accounts, apps, the Linux container and all of " +
                            "your data go. It cannot be undone from the device.",
                        color = Slate400,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                } else {
                    Text(
                        "Factory-reset controls are disabled in Device & Display Setup. Enable them only for a dedicated appliance; Android will still require confirmation for a normal user.",
                        color = Slate400,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }

                if (wipeError != null) {
                    Text(wipeError!!, color = PhonixRed, fontSize = 10.sp)
                }
            }
        }

        Text(
            "Why this matters: every ad and tracker blocked at the DNS layer is bandwidth and " +
                "battery not spent, and every phone reused here is one less handset on a pallet. " +
                "The box is not a subscription - it is yours.",
            color = Slate600,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (wipeDialogOpen) {
        AlertDialog(
            onDismissRequest = { wipeDialogOpen = false },
            containerColor = Slate900,
            titleContentColor = Color.White,
            textContentColor = Slate200,
            title = { Text("Erase this unit?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Everything on this phone is destroyed: accounts, photos, apps, the Linux " +
                            "container and its files. The unit will reboot to Android setup.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Text("Type ERASE to continue.", color = Slate400, fontSize = 11.sp)
                    OutlinedTextField(
                        value = wipeConfirmText,
                        onValueChange = { wipeConfirmText = it.uppercase() },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PhonixRed,
                            unfocusedBorderColor = Slate700,
                            cursorColor = PhonixRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = wipeConfirmText == "ERASE",
                    onClick = {
                        wipeDialogOpen = false
                        val error = eraseDevice(context)
                        if (error != null) {
                            wipeError = error
                        }
                    }
                ) {
                    Text(
                        "Erase everything",
                        color = if (wipeConfirmText == "ERASE") PhonixRed else Slate600,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { wipeDialogOpen = false }) { Text("Cancel", color = Slate300) }
            }
        )
    }
}

// -------------------------------------------------------------
// Small building blocks
// -------------------------------------------------------------

private data class Notice(val text: String, val ok: Boolean)

@Composable
private fun NoticeText(notice: Notice) {
    Text(
        notice.text,
        color = if (notice.ok) PhonixGreen else PhonixRed,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
}

@Composable
private fun PresetRow(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (selected) PhonixCyan else Slate800,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) PhonixCyan else Slate600)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(detail, color = Slate400, fontSize = 9.sp)
    }
}

@Composable
private fun GuideStep(number: Int, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Slate800),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = PhonixCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(body, color = Slate300, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun CommandRow(command: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0B1220))
            .border(1.dp, Slate800, RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            command,
            color = PhonixGreen,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ContentCopy,
            contentDescription = "Copy command",
            tint = Slate300,
            modifier = Modifier
                .size(15.dp)
                .clickable(onClick = onCopy)
        )
    }
}

// -------------------------------------------------------------
// Real system actions
// -------------------------------------------------------------

private const val ADGUARD_HOST = "dns.adguard-dns.com"
private const val ADGUARD_FAMILY_HOST = "family.adguard-dns.com"

private fun readPrivateDns(context: Context): String {
    if (Build.VERSION.SDK_INT < 28) return "unsupported on this Android version"
    return try {
        val mode = Settings.Global.getString(context.contentResolver, "private_dns_mode").orEmpty()
        val host = Settings.Global.getString(context.contentResolver, "private_dns_specifier").orEmpty()
        when {
            mode == "hostname" && host.isNotBlank() ->        "hostname/$host"
            mode == "opportunistic" -> "automatic (unfiltered)"
            mode.isBlank() || mode == "off" -> "off (carrier DNS)"
            else -> mode
        }
    } catch (_: Exception) {
        "unknown"
    }
}

/**
 * Returns true when the value was actually written. Without WRITE_SECURE_SETTINGS (granted once
 * by provision.sh) there is no public API for this, so the caller falls back to the Settings
 * screen with the hostname on the clipboard.
 */
private fun tryApplyPrivateDns(context: Context, host: String): Boolean {
    if (!com.example.util.DeviceSetupPreferences.mayApplySystemPrivateDns(context)) return false
    if (Build.VERSION.SDK_INT < 28) return false
    if (host.isBlank()) return false
    val granted = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) return false
    return try {
        Settings.Global.putString(context.contentResolver, "private_dns_mode", "hostname")
        Settings.Global.putString(context.contentResolver, "private_dns_specifier", host)
        true
    } catch (_: SecurityException) {
        false
    }
}

/**
 * Android exposes no public activity action for the Private DNS screen, so this opens the
 * Connections page where it lives (Samsung: Connections > More connection settings > Private DNS)
 * rather than guessing an undocumented intent.
 */
private fun openPrivateDnsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { openUrl(context, "https://adguard-dns.io/en/public-dns.html") }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "No browser available", Toast.LENGTH_SHORT).show() }
}

/** Device-owner only. Returns an error string, or null once the platform has accepted the wipe. */
private fun eraseDevice(context: Context): String? {
    if (!com.example.util.DeviceSetupPreferences.choices(context).allowFactoryReset) {
        return "Factory-reset control is disabled in Device & Display Setup."
    }
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        ?: return "Device policy service unavailable."
    if (!dpm.isDeviceOwnerApp(context.packageName)) {
        return "Phonix is not the device owner; use the Android reset screen instead."
    }
    return try {
        dpm.wipeData(0)
        null
    } catch (e: SecurityException) {
        "Android refused the wipe: ${e.message ?: "missing wipe-data policy"}"
    } catch (e: Exception) {
        "Wipe failed: ${e.message ?: e.javaClass.simpleName}"
    }
}

/**
 * Samsung exposes its reset screen as an activity. Launching it is not the same as wiping:
 * the system still asks the person in front of the monitor to confirm.
 */
private fun openSystemReset(context: Context): Boolean {
    val samsung = Intent().apply {
        component = ComponentName(
            "com.android.settings",
            "com.android.settings.Settings\$FactoryResetActivity"
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(samsung)
        true
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
