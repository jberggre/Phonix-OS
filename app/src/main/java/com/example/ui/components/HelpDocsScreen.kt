package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiEngineState
import com.example.model.WindowType
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
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.ContainerStatus
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel

enum class HelpTopicCategory(val label: String) {
    ALL("All Topics"),
    QUICK_START("Quick Start"),
    TERMINAL("Terminal & Shell"),
    DISTROS("Distros & Packages"),
    PINNING("Desktop Shortcuts"),
    X11_GUI("Linux Desktop (X11)"),
    AI_SERVERS("Local AI & Servers"),
    HONEST_LIMITS("System Boundaries & FAQ")
}

data class HelpArticle(
    val id: String,
    val category: HelpTopicCategory,
    val title: String,
    val badge: String,
    val badgeColor: Color,
    val icon: ImageVector,
    val summary: String,
    val bullets: List<String>,
    val commands: List<Pair<String, String>> = emptyList(),
    val primaryActionLabel: String? = null,
    val onPrimaryAction: ((PhonixViewModel) -> Unit)? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelpDocsScreen(
    uiState: PhonixUiState,
    viewModel: PhonixViewModel
) {
    var selectedCategory by remember { mutableStateOf(HelpTopicCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val articles = remember(uiState.currentDistro, uiState.activeDesktopEnvironment) {
        buildHelpArticles(
            activeDistroName = uiState.currentDistro.name,
            activePkgManager = uiState.currentDistro.packageManager,
            activeDeName = uiState.activeDesktopEnvironment.title
        )
    }

    val filteredArticles = remember(articles, selectedCategory, searchQuery) {
        articles.filter { article ->
            val categoryMatch = selectedCategory == HelpTopicCategory.ALL || article.category == selectedCategory
            val query = searchQuery.trim().lowercase()
            val textMatch = query.isEmpty() ||
                article.title.lowercase().contains(query) ||
                article.summary.lowercase().contains(query) ||
                article.bullets.any { it.lowercase().contains(query) } ||
                article.commands.any { it.first.lowercase().contains(query) || it.second.lowercase().contains(query) }
            categoryMatch && textMatch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("help_docs_screen")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PhonixCyan.copy(alpha = 0.15f))
                        .border(1.dp, PhonixCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Help & Documentation",
                        tint = PhonixCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Phonix OS Help & Technical Guide",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Accurate architecture guide, terminal reference, multi-distro commands & system boundaries",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.setActiveTab(AppNavTab.DESKTOP) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, PhonixBlueLight),
                modifier = Modifier.testTag("help_back_to_desktop_btn")
            ) {
                Icon(Icons.Default.Computer, contentDescription = null, tint = PhonixBlueLight, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back to Desktop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Live Runtime Truth Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.containerStatus == ContainerStatus.RUNNING) PhonixGreen else PhonixAmber
                                )
                        )
                        Text(
                            text = "LIVE ENVIRONMENT SNAPSHOT",
                            color = PhonixCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Unrooted PRoot Userland • Android Sandbox",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HelpStatusChip(
                        label = "Active Distro",
                        value = "${uiState.currentDistro.name} (${uiState.currentDistro.packageManager})",
                        color = PhonixAmber
                    )
                    HelpStatusChip(
                        label = "Linux Container",
                        value = uiState.containerStatus.name,
                        color = if (uiState.containerStatus == ContainerStatus.RUNNING) PhonixGreen else Slate400
                    )
                    HelpStatusChip(
                        label = "X11 Desktop (:1)",
                        value = "${uiState.activeDesktopEnvironment.title} (${uiState.activeDesktopEnvironment.processName})",
                        color = PhonixBlueLight
                    )
                    HelpStatusChip(
                        label = "Local AI (:8080)",
                        value = uiState.aiEngineState.name,
                        color = if (uiState.aiEngineState == AiEngineState.READY) PhonixGreen else Slate400
                    )
                    HelpStatusChip(
                        label = "Terminal Tabs",
                        value = "${uiState.terminalTabs.size} open",
                        color = PhonixCyan
                    )
                }

                // Quick Launch Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HelpQuickActionChip("Open Terminal", Icons.Default.Terminal, PhonixGreen) {
                        viewModel.setActiveTab(AppNavTab.DESKTOP)
                        viewModel.openWindow(WindowType.TERMINAL)
                    }
                    HelpQuickActionChip("Open Linux Desktop (X11)", Icons.Default.Computer, PhonixCyan) {
                        viewModel.setActiveTab(AppNavTab.DESKTOP)
                        viewModel.openWindow(WindowType.LINUX_DESKTOP)
                    }
                    HelpQuickActionChip("Distro & Package Hub", Icons.Default.Widgets, PhonixAmber) {
                        viewModel.setActiveTab(AppNavTab.DISTROS)
                    }
                    HelpQuickActionChip("Offline Knowledge Vault", Icons.Default.MenuBook, Color(0xFFA855F7)) {
                        viewModel.setActiveTab(AppNavTab.DESKTOP)
                        viewModel.openWindow(WindowType.AI_ASSISTANT)
                        if (!uiState.showKnowledgeLibrary) {
                            viewModel.toggleKnowledgeLibrary()
                        }
                    }
                    HelpQuickActionChip("Run Setup Wizard", Icons.Default.Build, PhonixBlueLight) {
                        viewModel.openInstallationWizard()
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Search + Category Filter Row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("help_search_input"),
            placeholder = {
                Text(
                    "Search help topics, commands (phonix-pin, apk, apt, pacman, xfce4, proot)...",
                    color = Slate400,
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search Help", tint = PhonixCyan, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PhonixCyan,
                unfocusedBorderColor = Slate800,
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HelpTopicCategory.entries.forEach { category ->
                val selected = selectedCategory == category
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedCategory = category }
                        .testTag("help_cat_${category.name.lowercase()}"),
                    color = if (selected) PhonixBlue.copy(alpha = 0.35f) else Slate900,
                    border = BorderStroke(1.dp, if (selected) PhonixCyan else Slate800)
                ) {
                    Text(
                        text = category.label,
                        color = if (selected) Color.White else Slate400,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Help Articles List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredArticles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        border = BorderStroke(1.dp, Slate800)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No help articles matched \"$searchQuery\"", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Try searching for 'terminal', 'distro', 'pin', 'x11', 'ai', or clear the filter.", color = Slate400, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(filteredArticles, key = { it.id }) { article ->
                    HelpArticleCard(
                        article = article,
                        onRunCommand = { cmd ->
                            viewModel.setActiveTab(AppNavTab.DESKTOP)
                            viewModel.openWindow(WindowType.TERMINAL)
                            viewModel.executeTerminalCommand(cmd)
                        },
                        onPrimaryAction = {
                            article.onPrimaryAction?.invoke(viewModel)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpStatusChip(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Slate950)
            .border(1.dp, Slate800, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "$label:", color = Slate400, fontSize = 10.sp)
        Text(
            text = value,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun HelpQuickActionChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HelpArticleCard(
    article: HelpArticle,
    onRunCommand: (String) -> Unit,
    onPrimaryAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("help_article_${article.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(article.badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, article.badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(article.icon, contentDescription = null, tint = article.badgeColor, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = article.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = article.summary,
                            color = Slate300,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(article.badgeColor.copy(alpha = 0.16f))
                        .border(1.dp, article.badgeColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = article.badge,
                        color = article.badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = Slate800)

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                article.bullets.forEach { bullet ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("•", color = article.badgeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = bullet,
                            color = Slate200,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (article.commands.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "EXECUTABLE TERMINAL EXAMPLES (TAP TO RUN IN TERMINAL)",
                    color = Slate400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    article.commands.forEach { (description, cmd) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate950)
                                .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = description, color = Slate400, fontSize = 9.sp)
                                Text(
                                    text = "$ $cmd",
                                    color = PhonixGreen,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onRunCommand(cmd) },
                                colors = ButtonDefaults.buttonColors(containerColor = PhonixGreen.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PhonixGreen, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Run", color = PhonixGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (article.primaryActionLabel != null && article.onPrimaryAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onPrimaryAction,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, article.badgeColor)
                    ) {
                        Text(
                            text = article.primaryActionLabel,
                            color = article.badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun buildHelpArticles(
    activeDistroName: String,
    activePkgManager: String,
    activeDeName: String
): List<HelpArticle> = listOf(
    HelpArticle(
        id = "quickstart_workstation",
        category = HelpTopicCategory.QUICK_START,
        title = "Quick Start: How Phonix OS Works",
        badge = "WORKSTATION",
        badgeColor = PhonixCyan,
        icon = Icons.Default.Computer,
        summary = "Turn any Android 8.0+ phone, foldable, or DeX display into an unrooted Linux & Android workstation.",
        bullets = listOf(
            "Desktop Environment: Phonix renders a multi-window desktop canvas with draggable/resizable windows, a Windows 11-style Start Menu, taskbar pins, and freeform or grid-snapped desktop icons.",
            "Top Control Pill: Tap the compact 'Phonix Mini PC' pill at the top center of the screen at any time to switch between Desktop, Thermals & Power, Games, Servers, Storage, Linux System, PC Apps, and this Help & Docs tab.",
            "Setup Modes: In the Setup Wizard, 'Daily Phone' leaves your Android wallpaper, DNS, and boot behavior untouched; 'Foldable Workstation' adapts window sizes to fold posture; 'Dedicated Cyberdeck / Mini-PC' enables external display auto-launch and persistent Linux service options.",
            "No Root or Bootloader Unlock: Phonix runs inside Android's standard user sandbox using PRoot (ptrace system call translation). Nothing modifies your device's boot partition or system image."
        ),
        primaryActionLabel = "Open Setup & Device Wizard",
        onPrimaryAction = { vm -> vm.openInstallationWizard() }
    ),
    HelpArticle(
        id = "terminal_multitab_extrakeys",
        category = HelpTopicCategory.TERMINAL,
        title = "Multi-Tab Terminal & Termux-Style Extra Keys",
        badge = "SHELL & KEYS",
        badgeColor = PhonixGreen,
        icon = Icons.Default.Terminal,
        summary = "Use multiple independent shell tabs, command history, TAB auto-completion, and persistent Linux filesystem changes.",
        bullets = listOf(
            "Persistent Changes: Every file you create in /root or /home and every package you install via apk, apt, or pacman is stored inside your app's private Linux rootfs on disk and survives app restarts.",
            "Multi-Tab Sessions: Click '+' in the Terminal tab bar to create independent shell tabs (each with its own scrollback buffer and command history) or '×' to close a tab.",
            "Extra Keys Bar: Use ↑ and ↓ to cycle through your per-tab command history, TAB to auto-complete filesystem paths or binaries from the live Linux guest, Ctrl+C to interrupt running child processes in PRoot, and Ctrl+D to exit/close a tab.",
            "Honest Pipe-Shell Boundary: The main Terminal window communicates with /bin/sh over standard pipes (stdin/stdout) rather than a raw kernel PTY. Line-oriented CLI tools (apk, apt, pacman, git, python3, curl, phonix-pin) run directly here. For full-screen curses TUIs (htop, vim, nano, tmux), launch '+ xterm' inside the Linux Desktop (X11) window."
        ),
        commands = listOf(
            "Check active Linux kernel, architecture & guest release" to "uname -a && cat /etc/os-release | head -n 5",
            "List installed binaries and disk usage inside guest" to "df -h && ls -la /root"
        ),
        primaryActionLabel = "Open Terminal Window",
        onPrimaryAction = { vm ->
            vm.setActiveTab(AppNavTab.DESKTOP)
            vm.openWindow(WindowType.TERMINAL)
        }
    ),
    HelpArticle(
        id = "distros_package_managers",
        category = HelpTopicCategory.DISTROS,
        title = "Linux Distributions & Package Managers (apk, apt, pacman)",
        badge = activeDistroName.uppercase(),
        badgeColor = PhonixAmber,
        icon = Icons.Default.Widgets,
        summary = "Start offline with bundled Alpine Linux or download Debian 12, Ubuntu 24.04 LTS, Arch Linux ARM, or Kali Linux on demand.",
        bullets = listOf(
            "Alpine Linux 3.24 (Bundled Default): Ships inside the APK (assets/alpine-rootfs.tgz, ~3.4 MB compressed). Extracts offline in seconds and uses 'apk add <package>'. Ideal for low-RAM phones and fast container boot.",
            "Debian 12 (Bookworm) & Ubuntu 24.04 LTS (Noble): Streamed on demand over HTTPS from official LXC / Canonical ARM64 mirrors when selected in the Setup Wizard or Linux System Hub. Uses 'apt-get update && apt-get install -y <package>'.",
            "Arch Linux ARM & Kali Linux Rolling: Streamed over HTTPS into filesDir/linux/<distroId>. Arch uses 'pacman -Sy --noconfirm <package>'; Kali uses Debian ARM64 base + apt.",
            "Streaming Rootfs Extraction: Phonix extracts .tar.gz archives via a streaming TarGzExtractor (preserving symlinks and executable permissions) and .tar.xz archives via the bundled Alpine proot + busybox tar -xJf helper so large rootfs archives never exhaust JVM memory."
        ),
        commands = listOf(
            "Update package index in your active distro ($activePkgManager)" to when (activePkgManager) {
                "apt" -> "apt-get update"
                "pacman" -> "pacman -Sy"
                else -> "apk update"
            },
            "Install Python 3 and Git in your active distro" to when (activePkgManager) {
                "apt" -> "apt-get update && apt-get install -y python3 git"
                "pacman" -> "pacman -Sy --noconfirm python git"
                else -> "apk add python3 git"
            }
        ),
        primaryActionLabel = "Open Linux System & Distro Hub",
        onPrimaryAction = { vm -> vm.setActiveTab(AppNavTab.DISTROS) }
    ),
    HelpArticle(
        id = "pinning_shortcuts_phonix_pin",
        category = HelpTopicCategory.PINNING,
        title = "Creating Desktop Shortcuts & Using phonix-pin",
        badge = "SHORTCUTS",
        badgeColor = PhonixBlueLight,
        icon = Icons.Default.PushPin,
        summary = "Pin Android apps, Linux X11 GUI programs, or custom CLI scripts directly to your Phonix Desktop.",
        bullets = listOf(
            "Method 1 — Terminal CLI (phonix-pin): Inside any Terminal session, run 'phonix-pin <Title> <command> [--gui]'. Omit --gui for terminal commands, or pass --gui for X11 graphical apps (which automatically start DISPLAY=:1 and open the Linux Desktop window).",
            "Method 2 — Automatic Terminal Banner: Whenever you install a package in Terminal (e.g., 'apk add firefox-esr' or 'apt install gimp'), Phonix detects the installed package and displays a 1-click 'Pin to Desktop' banner above the input bar.",
            "Method 3 — Desktop Context Menu & App Picker: Right-click (or long-press) an empty spot on the Phonix Desktop and select '+ Pin App to Desktop'. Choose from Android Apps, Linux GUI (X11) apps, or a Custom CLI Command.",
            "Method 4 — PC Apps & Distro Hub: Any installed package in the PC Apps or Linux System tabs shows a 1-click 'Pin to Desktop' button."
        ),
        commands = listOf(
            "Show phonix-pin usage inside the Linux container" to "phonix-pin --help",
            "Pin a custom System Info CLI shortcut to the Phonix Desktop" to "phonix-pin \"System Info\" \"uname -a && free -m\"",
            "Pin X11 xterm as a graphical desktop icon" to "phonix-pin \"XTerm GUI\" \"xterm\" --gui"
        ),
        primaryActionLabel = "Open PC Apps Catalog",
        onPrimaryAction = { vm -> vm.setActiveTab(AppNavTab.APPS) }
    ),
    HelpArticle(
        id = "x11_desktop_environments",
        category = HelpTopicCategory.X11_GUI,
        title = "Linux Desktop (X11) & Desktop Environment Switcher",
        badge = activeDeName.uppercase(),
        badgeColor = Color(0xFFA855F7),
        icon = Icons.Default.Computer,
        summary = "Run real graphical Linux applications and switch between IceWM, XFCE4, Openbox, and LXQt on DISPLAY=:1.",
        bullets = listOf(
            "Real X11 Pipeline: Clicking 'Start Desktop' installs Xvfb, x11vnc, websockify, and xterm if missing, starts a virtual framebuffer on DISPLAY=:1 (1280x720x24), launches your chosen window manager, and bridges VNC (127.0.0.1:5900 -> 127.0.0.1:6080) into an interactive noVNC canvas.",
            "Live DE Switcher: Use the selector chips in the Linux Desktop window header to switch between IceWM (~35 MB RAM, classic taskbar), XFCE4 (~140 MB RAM, full panel & window manager), Openbox (~25 MB RAM, ultra-lightweight), and LXQt (~110 MB RAM, modern Qt desktop).",
            "Auto-Install on Switch: If you pick XFCE4, Openbox, or LXQt and its packages aren't installed yet, Phonix automatically installs them using your active distro's package manager (apk, apt-get, or pacman) and restarts the X11 session.",
            "Running Full-Screen Curses Apps: Click '+ xterm' in the Linux Desktop header to open a graphical terminal with a true PTY for htop, vim, nano, or midnight commander."
        ),
        commands = listOf(
            "Launch an xterm window on X11 DISPLAY=:1" to "DISPLAY=:1 xterm &"
        ),
        primaryActionLabel = "Open Linux Desktop (X11)",
        onPrimaryAction = { vm ->
            vm.setActiveTab(AppNavTab.DESKTOP)
            vm.openWindow(WindowType.LINUX_DESKTOP)
        }
    ),
    HelpArticle(
        id = "local_ai_and_servers",
        category = HelpTopicCategory.AI_SERVERS,
        title = "Local AI Engine, Offline Knowledge & Server Center",
        badge = "LOCAL-ONLY",
        badgeColor = PhonixGreen,
        icon = Icons.Default.SmartToy,
        summary = "On-device Qwen2.5-1.5B inference via llama.cpp, instant offline survival/coding reference shelves, and local Linux daemons.",
        bullets = listOf(
            "Local AI Assistant (Qwen2.5-1.5B-Instruct Q4_K_M): Runs llama-server inside the guest on 127.0.0.1:8080. Nothing is sent to external cloud APIs. The status badge only reports READY after HTTP GET http://127.0.0.1:8080/health returns 200.",
            "Shell Agent Mode: Toggle the 'Shell Agent' chip in the Local AI window to allow the local model to emit <cmd>...</cmd> tags, execute them in the unprivileged PRoot shell, and summarize real stdout/stderr.",
            "Offline Knowledge Library: Works immediately with zero downloads. Includes curated reference shelves for Survival, Medical, Coding & Linux, Hardware & Repair, Power & Solar, and Radio & Comms (Switchable between Essential, Standard, and Comprehensive vault tiers in Step 2 of the Setup Wizard).",
            "Server Center: Launch real Linux daemons on unprivileged ports (>= 1024): Nginx (:8088), Dropbear SSH (:2222), Samba SMB File Share (:4445), Adblock DNS Sinkhole / Pi-hole Engine (:5353), Python HTTP (:8000), Redis (:6379), MariaDB (:3306), and Mosquitto MQTT (:1883). Green status indicators are backed by real TCP socket probes."
        ),
        primaryActionLabel = "Open Server Center",
        onPrimaryAction = { vm -> vm.setActiveTab(AppNavTab.SERVERS) }
    ),
    HelpArticle(
        id = "docker_pihole_smb",
        category = HelpTopicCategory.AI_SERVERS,
        title = "Docker, Pi-hole DNS Adblock & Samba SMB File Sharing (Termux Style)",
        badge = "NAS / DOCKER / DNS",
        badgeColor = PhonixAmber,
        icon = Icons.Default.Dns,
        summary = "How to run SMB LAN shares, Pi-hole's DNS sinkhole engine, and Docker containers on unrooted Android.",
        bullets = listOf(
            "Samba SMB File Server (phonix-smb on port 4445): Standard SMB port 445 is blocked below 1024 on unrooted Android, so 'phonix-smb start' configures /etc/samba/smb.conf on port 4445 and shares /root/shared across your LAN at smb://<LAN-IP>:4445/phonix.",
            "Pi-hole DNS Sinkhole Engine (phonix-adblock on port 5353): Standard Pi-hole's curl|bash installer requires systemd and port 53, which unrooted PRoot lacks. Instead, Phonix runs Pi-hole's core engine (dnsmasq + hosts sinkhole) natively via 'phonix-adblock start' on port 5353. Run 'phonix-adblock update' to pull 130,000+ ad/tracker domains from StevenBlack's unified hosts list.",
            "Docker Option 1 — udocker Rootless Engine (phonix-docker setup-udocker): Native dockerd requires kernel cgroups and overlayfs (blocked without root). Like Termux, Phonix supports 'udocker' to pull and execute OCI / Docker Hub images ('udocker pull alpine && udocker run alpine uname -a') directly inside PRoot.",
            "Docker Option 2 — QEMU Full Linux Kernel VM (phonix-docker setup-qemu): Installs 'qemu-system-aarch64' to boot a dedicated virtual kernel with cgroups, namespaces, and port forwarding (2223->22, 8090->80, 9000->9000) when you need native dockerd and docker compose."
        ),
        commands = listOf(
            "Show phonix-smb, phonix-adblock & phonix-docker CLI usage" to "phonix-docker --help && phonix-smb status && phonix-adblock status",
            "Pull 130,000+ Pi-hole / StevenBlack ad-block domains into /etc/phonix-adblock.hosts" to "phonix-adblock update",
            "Install udocker rootless Docker container engine in PRoot" to "phonix-docker setup-udocker"
        ),
        primaryActionLabel = "Open Server Suite",
        onPrimaryAction = { vm -> vm.setActiveTab(AppNavTab.SERVERS) }
    ),
    HelpArticle(
        id = "honest_system_boundaries",
        category = HelpTopicCategory.HONEST_LIMITS,
        title = "Honest System Boundaries & FAQ (No Lies Policy)",
        badge = "VERIFIED FACTS",
        badgeColor = PhonixRed,
        icon = Icons.Default.VerifiedUser,
        summary = "Exact architectural boundaries: what Phonix OS executes natively vs. what Android's unrooted sandbox prohibits.",
        bullets = listOf(
            "Can Phonix run Docker, Pi-hole, and SMB like Termux? Yes, with honest unrooted adaptations: SMB runs natively via 'phonix-smb' on port 4445; Pi-hole's dnsmasq sinkhole runs natively via 'phonix-adblock' on port 5353 (with 130k+ StevenBlack domains); and Docker images run either rootlessly in PRoot via 'phonix-docker setup-udocker' or inside a full kernel VM via 'phonix-docker setup-qemu'.",
            "Can Phonix run Windows .exe games or Steam PC titles? No. Phonix runs native ARM64 (aarch64) Linux packages inside PRoot. Running x86-64 Windows .exe binaries requires Box64 + Wine or a cloud streaming service (GeForce NOW, Xbox Cloud, Moonlight, Steam Link), which are linked in the Gaming Hub.",
            "Does Linux X11 have hardware 3D GPU acceleration? No. X11 apps render via Xvfb software framebuffer over loopback VNC. This is ideal for desktop productivity (Chromium, LibreOffice, GIMP, VS Code, Inkscape, xterm) and 2D tools, not 3D OpenGL/Vulkan Linux games.",
            "Why are servers on ports 8088, 2222, 4445, and 5353 instead of 80, 22, 445, and 53? Android runs apps as unprivileged UIDs without CAP_NET_BIND_SERVICE, so the Linux kernel forbids binding ports below 1024.",
            "Can I click inside the Phone Screen mirror window? The Phone Screen window captures live frames via Android's MediaProjection API. Because Android restricts cross-app touch injection to signed system/Accessibility services, the preview is view-only; use 'Open on This Screen' to interact with an app directly.",
            "Can Phonix uninstall preloaded carrier bloatware by itself? Only if granted ADB Device Owner ('dpm set-device-owner'). Otherwise, Phonix opens the system App Info screen for manual disable or generates an exact 'adb shell pm uninstall -k --user 0' script you can run from a PC."
        ),
        primaryActionLabel = "Open Privacy & Ad-Block Hub",
        onPrimaryAction = { vm ->
            vm.setActiveTab(AppNavTab.DESKTOP)
            vm.openWindow(WindowType.PRIVACY)
        }
    )
)
