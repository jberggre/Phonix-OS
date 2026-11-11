package com.example.model

import java.util.UUID

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val suggestedCommand: String? = null,
    val timestamp: String = "Now",
    /** True only for text produced by the local llama.cpp model, not the notes table. */
    val modelGenerated: Boolean = false,
    /** True when curated offline notes were supplied as retrieval context to the model. */
    val grounded: Boolean = false,
    val tokensPerSecond: Float? = null,
    val tokenCountExact: Boolean = true
)

/** One command the shell agent ran, with the output the guest actually returned. */
data class AgentStep(
    val command: String,
    val output: String,
    val exitNote: String = ""
)

object DefaultAiKnowledge {
    fun getInitialMessages(): List<AiChatMessage> = listOf(
        AiChatMessage(
            isUser = false,
            text = "Welcome to Phonix offline knowledge. The built-in notes work without a model. You can optionally set up a real local model (Qwen2.5 1.5B) that runs on this phone's CPU and never sends your question to a server.\n\nTopics in the notes:\n• 💻 Coding & Shell Scripts (Python, Bash, C, Git, Web)\n• 🌱 Farming & Agriculture (Soil NPK, composting, staple crops, seeds)\n• 🛡️ Grid-Down Survival (Water filtration, emergency radio, solar/DC power, first aid, mesh comms)\n• ⚙️ Hardware & Linux (Thermals, debloat, local servers)\n\nTap Set up local AI for free-form answers, or type a topic to use the notes.\n\nThe local model can still make mistakes. Safety-critical water, medical, electrical and fire instructions should be taken from the curated notes, not from free-form model text.",
            suggestedCommand = "cat /sys/class/thermal/thermal_zone0/temp",
            timestamp = "System"
        )
    )

    /**
     * The curated answer for a prompt, or null when the prompt falls through to the topic list.
     * Used as retrieval context for the local model, not as its answer.
     */
    fun notesFor(prompt: String, coolingModeName: String, tempC: Float): String? {
        val reply = matchResponse(prompt, coolingModeName, tempC).first
        return reply.takeIf { !it.startsWith(FALLBACK_PREFIX) }
    }

    private const val FALLBACK_PREFIX = "This is the rule-based Phonix offline knowledge base"

    fun matchResponse(prompt: String, coolingModeName: String, tempC: Float): Pair<String, String?> {
        val lower = prompt.lowercase(java.util.Locale.ROOT)
        return when {
            // --- FARMING & AGRICULTURE ---
            lower.contains("farm") || lower.contains("crop") || lower.contains("plant") || lower.contains("grow") || lower.contains("soil") || lower.contains("compost") -> {
                Pair(
                    "🌱 [OFFLINE FARMING KNOWLEDGE]\n\n" +
                    "1. High-Calorie Survival Staples:\n" +
                    "• Potatoes & Sweet Potatoes: Highest calorie yield per sq ft. Grows in marginal soil, ready in 90-120 days.\n" +
                    "• Bush Beans: Nitrogen fixers, high protein, quick harvest (50-60 days). Dry for multi-year storage.\n" +
                    "• Winter Squash: Keeps 6+ months in cool dry storage without refrigeration.\n" +
                    "• Corn (Maize): Plant in blocks (not rows) for wind pollination.\n\n" +
                    "2. Composting Ratio:\n" +
                    "• 3 parts 'Brown' (carbon: dry leaves, cardboard, straw) to 1 part 'Green' (nitrogen: food scraps, fresh grass, manure). Keep moist like a wrung-out sponge, turn every 7-14 days.\n\n" +
                    "3. Seed Saving Rule:\n" +
                    "• Only save seeds from 'Open-Pollinated' or 'Heirloom' varieties. F1 Hybrids do not breed true. Ferment tomato seeds in water 3 days before drying.",
                    "echo 'Crop rotation: Legumes (fix N) -> Brassicas (heavy feeders) -> Roots (light feeders)'"
                )
            }

            // --- WATER PURIFICATION & HARVESTING ---
            lower.contains("water") || lower.contains("filter") || lower.contains("purif") || lower.contains("drink") || lower.contains("thirst") -> {
                Pair(
                    "💧 [OFFLINE SURVIVAL: WATER PURIFICATION]\n\n" +
                    "1. Boiling:\n" +
                    "• 1 full rolling boil minute kills all viruses, bacteria, and protozoa (Cryptosporidium/Giardia). At altitudes above 1000m (3300ft), boil for 3 minutes.\n\n" +
                    "2. DIY Bio-Sand & Charcoal Gravity Filter (Layers from bottom to top):\n" +
                    "• Layer 1 (Bottom): Clean cloth or cotton plug over small drain hole.\n" +
                    "• Layer 2: Crushed wood charcoal/activated charcoal (removes toxins, chemicals, odors).\n" +
                    "• Layer 3: Fine clean sand (10-15 cm, traps micro-particles).\n" +
                    "• Layer 4: Coarse gravel/pebbles (catches large debris).\n" +
                    "*Note: Mechanical filtration does not kill all viruses. Always boil after filtering!\n\n" +
                    "3. SODIS (Solar Water Disinfection):\n" +
                    "• Fill clear PET plastic bottles with moderately clear water and place horizontally on corrugated metal roof in full sun for 6 hours (or 2 days if cloudy). UV-A rays pasteurize pathogens.",
                    "echo 'Rain harvesting formula: Catchment Sq Ft * Rain Inches * 0.623 = Gallons collected'"
                )
            }

            // --- SURVIVAL & GRID-DOWN / APOCALYPSE ---
            lower.contains("surviv") || lower.contains("apocal") || lower.contains("disaster") || lower.contains("grid") || lower.contains("shelter") || lower.contains("emergency") -> {
                Pair(
                    "🛡️ [OFFLINE CRISIS & SURVIVAL PROTOCOL: RULE OF 3s]\n\n" +
                    "• 3 Minutes without oxygen/severe arterial bleed.\n" +
                    "• 3 Hours without shelter in harsh extreme climates (hypothermia/hyperthermia).\n" +
                    "• 3 Days without potable water.\n" +
                    "• 3 Weeks without caloric intake.\n\n" +
                    "Key Directives:\n" +
                    "1. Shelter: Insulate underneath your body first—conduction into bare ground robs body heat 25x faster than still air.\n" +
                    "2. Fire Triangle: Oxygen + Heat + Fuel (Tinder -> Kindling -> Fuel logs). Dry birch bark, pine resin (fatwood), and char cloth ignite readily with sparks.\n" +
                    "3. Navigation: Polaris (North Star) sits directly in line with the two outer stars of the Big Dipper pointer edge. At solar noon, stick shadow points true North in the Northern hemisphere.",
                    "echo 'Survival priority: 1. Bleeding/Airway -> 2. Core Body Temp -> 3. Water -> 4. Signaling'"
                )
            }

            // --- FIRST AID & MEDICAL ---
            lower.contains("first aid") || lower.contains("bleed") || lower.contains("tourniquet") || lower.contains("wound") || lower.contains("burn") || lower.contains("rehydrat") || lower.contains("saline") -> {
                Pair(
                    "🩺 [OFFLINE EMERGENCY FIRST AID]\n\n" +
                    "1. Severe Arterial Bleeding (Pulsing bright red blood):\n" +
                    "• Immediate direct pressure with sterile dressing.\n" +
                    "• Apply Tourniquet (CAT / Windlass) 2-3 inches above the wound (never over a joint). Tighten windlass until pulsing stops completely. Write time applied on patient's forehead (T:HH:MM).\n\n" +
                    "2. WHO Oral Rehydration Salts (ORS) - Saves Lives in Severe Dehydration / Dysentery:\n" +
                    "• 1 Litre boiled or clean water\n" +
                    "• 6 level teaspoons Sugar (approx 25g)\n" +
                    "• 1/2 level teaspoon Salt (approx 2.5g)\n" +
                    "• Mix until dissolved. Sip slowly.\n\n" +
                    "3. Burn Treatment:\n" +
                    "• Cool with clean ambient/cool water for 10-20 mins. NEVER apply ice, butter, or ointment to open burns. Do NOT pop blister membranes. Cover loosely with sterile dry gauze.",
                    "echo 'First aid rule: Stop the bleed, protect the airway, prevent hypothermia.'"
                )
            }

            // --- EMERGENCY COMMS & RADIO FREQUENCIES ---
            lower.contains("radio") || lower.contains("freq") || lower.contains("ham") || lower.contains("walkie") || lower.contains("comms") || lower.contains("mesh") -> {
                Pair(
                    "📻 [OFFLINE EMERGENCY RADIO & COMMS DIRECTORY]\n\n" +
                    "Standard Emergency Monitoring Frequencies:\n" +
                    "• 146.520 MHz (2m VHF) - National Amateur Radio Calling Frequency (Simplex, no repeater needed).\n" +
                    "• 446.000 MHz (70cm UHF) - National UHF Calling Frequency.\n" +
                    "• 462.5625 MHz (FRS/GMRS Channel 1) - Common handheld walkie-talkie channel.\n" +
                    "• 27.065 MHz (CB Channel 9) - Official Civilian Emergency & Motorist Assistance.\n" +
                    "• 162.400 - 162.550 MHz - NOAA All-Hazards Weather Broadcasts (7 national channels).\n\n" +
                    "Off-Grid Cyberdeck Comms:\n" +
                    "This S10 board can create an independent Wi-Fi Access Point (AP) or join local 802.11 ad-hoc mesh networks for local disaster chat without cell towers or internet.",
                    "echo 'VHF Simplex: 146.520 MHz | UHF Calling: 446.000 MHz | FRS Ch 1: 462.5625 MHz'"
                )
            }

            // --- OFF-GRID POWER & HARDWARE DC WIRING ---
            lower.contains("power") || lower.contains("solar") || lower.contains("battery") || lower.contains("12v") || lower.contains("volt") || lower.contains("watt") || lower.contains("buck") -> {
                Pair(
                    "⚡ [OFF-GRID DC POWER & S10 MOTHERBOARD SPECIFICATIONS]\n\n" +
                    "1. Battery-less Direct Power Conversion:\n" +
                    "• Standard USB-C requires 5.0V regulated input (2.0A - 3.0A). The S10 Snapdragon 855 motherboard idles at ~1.8W - 2.5W and peaks at ~7.5W under full 8-core CPU load.\n" +
                    "• When powering from a 12V automotive battery or solar bank, use an LM2596 or MP1584 DC-DC Step-Down Buck Converter tuned to exactly 5.1V with 3A continuous capability.\n\n" +
                    "2. Solar Panel Sizing Rule:\n" +
                    "• Daily Energy Needed = Peak Watts (8W) * 24h = 192 Watt-Hours.\n" +
                    "• With 5 hours peak sun and 70% system loss: 192 / (5 * 0.70) = ~55W minimum solar panel size for 24/7 autonomous cyberdeck operation with a buffer battery.\n\n" +
                    "3. Battery Chemistries:\n" +
                    "• LiFePO4: 2000-4000 cycles, safe thermal runaway limit, standard 12.8V nominal (14.4V full charge, 10.0V cutoff).",
                    "echo 'Ohm\\'s Law: V = I * R | Watts = Volts * Amps'"
                )
            }

            // --- BASIC CODING & SCRIPTING ---
            lower.contains("code") || lower.contains("coding") || lower.contains("python") || lower.contains("bash") || lower.contains("script") || lower.contains("c++") || lower.contains("gcc") -> {
                Pair(
                    "💻 [OFFLINE CODING & SCRIPTING CHEATSHEET]\n\n" +
                    "1. Python Quick Reference:\n" +
                    "• Read file: with open('log.txt') as f: content = f.read()\n" +
                    "• HTTP Server: python3 -m http.server 8080\n" +
                    "• System exec: import subprocess; res = subprocess.getoutput('uname -a')\n\n" +
                    "2. Bash Essentials for Alpine Linux:\n" +
                    "• Loop through files: for f in *.txt; do echo \"Process: \" " + '$' + "f; done\n" +
                    "• Search text: grep -rn 'pattern' /etc/\n" +
                    "• Filter streams: cut -d' ' -f1,3 /proc/meminfo\n" +
                    "• Substitute text: sed -i 's/old/new/g' config.conf\n\n" +
                    "3. Compile C on ARM64:\n" +
                    "• gcc -O2 -Wall main.c -o myprogram && ./myprogram\n\n" +
                    "4. Offline Git Commits:\n" +
                    "• git init && git add . && git commit -m 'Offline checkpoint'",
                    "python3 -c 'import sys; print(\"Python \" + sys.version.split()[0] + \" on S10 Snapdragon 855\")'"
                )
            }

            // --- THERMALS & SENSORS ---
            lower.contains("thermal") || lower.contains("temp") || lower.contains("heat") -> {
                val tempStr = if (tempC > 0f) "${tempC.toInt()}°C" else "nominal"
                Pair(
                    "Motherboard telemetry reports hottest zone at $tempStr under $coolingModeName profile. With direct DC power and the custom heatsink mod, throttling is prevented even under sustained load.",
                    "cat /sys/class/thermal/thermal_zone0/temp"
                )
            }
            lower.contains("distro") || lower.contains("alpine") || lower.contains("debian") || lower.contains("ubuntu") || lower.contains("arch") || lower.contains("kali") || lower.contains("apk") || lower.contains("apt") || lower.contains("pacman") -> {
                Pair(
                    "🐧 [MULTI-DISTRO LINUX & PACKAGE MANAGERS]\n\n" +
                    "• Alpine Linux 3.24 (Bundled Default): Ships inside the APK for instant offline extraction. Package manager: 'apk update && apk add <pkg>'.\n" +
                    "• Debian 12 (Bookworm) & Ubuntu 24.04 LTS (Noble): Streamed over HTTPS on demand when selected in Setup Wizard or Linux System Hub. Package manager: 'apt-get update && apt-get install -y <pkg>'.\n" +
                    "• Arch Linux ARM: Streamed over HTTPS on demand. Package manager: 'pacman -Sy --noconfirm <pkg>'.\n" +
                    "• Kali Linux Rolling: Uses Debian ARM64 base + apt.",
                    "cat /etc/os-release | head -n 6"
                )
            }
            lower.contains("pin") || lower.contains("shortcut") || lower.contains("phonix-pin") || lower.contains("x11") || lower.contains("xfce") || lower.contains("icewm") || lower.contains("openbox") || lower.contains("lxqt") -> {
                Pair(
                    "🖥️ [DESKTOP SHORTCUTS & X11 DESKTOP ENVIRONMENTS]\n\n" +
                    "1. Pinning Shortcuts to Phonix Desktop:\n" +
                    "• Terminal CLI: run 'phonix-pin \"My App\" \"<command>\" [--gui]'.\n" +
                    "• Auto-Banner: after installing a package with apk/apt/pacman, click 'Pin to Desktop' on the banner above the terminal input.\n" +
                    "• Desktop Menu: right-click (or long-press) the desktop -> '+ Pin App to Desktop'.\n\n" +
                    "2. X11 Desktop Environments (DISPLAY=:1):\n" +
                    "• Switch between IceWM (~35 MB RAM), XFCE4 (~140 MB RAM), Openbox (~25 MB RAM), and LXQt (~110 MB RAM) using the selector chips in the Linux Desktop window header.",
                    "phonix-pin --help"
                )
            }
            lower.contains("docker") || lower.contains("udocker") || lower.contains("qemu") || lower.contains("pihole") || lower.contains("pi-hole") || lower.contains("adblock") || lower.contains("smb") || lower.contains("samba") || lower.contains("nas") -> {
                Pair(
                    "🐳 [DOCKER, PI-HOLE DNS SINKHOLE & SAMBA SMB ON UNROOTED ANDROID]\n\n" +
                    "1. Samba SMB File Sharing (Port 4445):\n" +
                    "• Real 'smbd' runs natively inside PRoot on unprivileged port 4445 sharing '/root/shared'.\n" +
                    "• Start from Server Suite or run: 'phonix-smb start' -> connect to smb://<LAN-IP>:4445/phonix.\n\n" +
                    "2. Pi-hole DNS Sinkhole Engine (Port 5353):\n" +
                    "• Standard Pi-hole's installer requires systemd + port 53, but its core engine (dnsmasq + hosts sinkhole) runs natively via 'phonix-adblock start' on port 5353.\n" +
                    "• Pull 130,000+ ad/tracker domains from StevenBlack's unified list with: 'phonix-adblock update'.\n\n" +
                    "3. Docker on Unrooted Android (Two Termux-Style Approaches):\n" +
                    "• Fast Rootless Containers (udocker): Run 'phonix-docker setup-udocker' to pull & run Docker Hub images directly in PRoot ('udocker run hello-world').\n" +
                    "• Full Kernel Docker Daemon (QEMU VM): Run 'phonix-docker setup-qemu' to install 'qemu-system-aarch64' and boot a virtual Linux kernel with cgroups for native 'dockerd' and 'docker compose'.",
                    "phonix-docker --help"
                )
            }
            lower.contains("server") || lower.contains("nginx") || lower.contains("port") || lower.contains("http") -> {
                Pair(
                    "Linux userland runs unrooted via PRoot, so servers bind to ports >= 1024: Nginx on :8088, Dropbear SSH on :2222, Samba SMB on :4445, Adblock DNS Sinkhole on :5353, Python HTTP on :8000, Redis on :6379, MariaDB on :3306, and Mosquitto on :1883.",
                    "python3 -m http.server 8000"
                )
            }
            lower.contains("debloat") || lower.contains("chrome") || lower.contains("samsung") -> {
                Pair(
                    "To debloat OEM background packages without root, use System Debloater to open Android App Info or run the generated 'adb shell pm uninstall -k --user 0 <pkg>' script from a computer (or directly if Phonix is Device Owner).",
                    "echo 'Use System Debloater window or ADB: pm uninstall -k --user 0 <package>'"
                )
            }
            lower.contains("fan") || lower.contains("cooling") -> {
                Pair(
                    "Active cooling profile is set to $coolingModeName. In Thermals & Power, you can monitor live thermal zones and configure direct DC power behavior.",
                    "cat /sys/class/thermal/thermal_zone0/temp"
                )
            }
            lower.contains("vscode") || lower.contains("ide") -> {
                Pair(
                    "VS Code Web needs Microsoft's ARM64 CLI (or Python HTTP fallback) listening on port 8000. The VS Code window probes 127.0.0.1:8000 and only turns green when a socket answers.",
                    "code serve-web --host 127.0.0.1 --port 8000 --without-connection-token &"
                )
            }
            lower.contains("htop") || lower.contains("top") || lower.contains("cpu") || lower.contains("ram") -> {
                Pair(
                    "Monitor CPU and RAM in the main pipe terminal with 'top -b -n1 | head -20' or 'free -m'. For interactive full-screen htop, launch '+ xterm' inside the Linux Desktop (X11) window.",
                    "top -b -n1 | head -n 15"
                )
            }
            else -> {
                Pair(
                    "This is the rule-based Phonix offline knowledge base, not a language model. It matches topics against built-in notes on this device.\n\nAvailable topics:\n• 'distro' - Alpine (apk), Debian/Ubuntu (apt), Arch (pacman) & switching\n• 'pin' / 'x11' - phonix-pin desktop shortcuts & IceWM/XFCE4/Openbox/LXQt\n• 'survival' - Rule of 3s, shelter, fire, navigation\n• 'water' - Boiling, DIY sand/charcoal filtration, SODIS\n• 'first aid' - Bleeding, tourniquets, ORS rehydration, burns\n• 'farming' - Calorie crops, composting 3:1, seed saving\n• 'radio' - Emergency VHF/UHF/CB frequencies, mesh comms\n• 'power' - 12V to 5V buck converters, solar sizing\n• 'coding' - Python, Bash, C compiler, offline git\n• 'thermal' - Motherboard temps & cooling status",
                    "uname -a"
                )
            }
        }
    }
}

