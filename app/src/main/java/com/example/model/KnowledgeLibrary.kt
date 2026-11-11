package com.example.model

/**
 * The offline library. Everything here is written to be usable with no model, no network and
 * no second device: it is the fallback that still works in an emergency, and it is also the
 * retrieval corpus the local model grounds its answers in.
 *
 * Scope matters for a 1.5B model: a handful of relevant documents produces a better answer than
 * the entire corpus, so [retrieve] narrows to one category when the user has picked one.
 */
enum class KnowledgeCategory(val title: String, val glyph: String) {
    WATER("Water", "💧"),
    FIRST_AID("First aid", "🩺"),
    SURVIVAL("Shelter & fire", "🛡️"),
    FARMING("Food & farming", "🌱"),
    RADIO("Comms", "📻"),
    POWER("DC power", "⚡"),
    CODING("Code & shell", "💻"),
    HARDWARE("Hardware & Linux", "⚙️")
}

/**
 * Library sizes are measured from the documents themselves ([KnowledgeLibrary.sizeLabel]), never
 * written by hand: the old `approxSize` strings claimed 800 KB / 2.8 MB / 7.5 MB for a corpus that
 * is a few hundred kilobytes in total.
 */
enum class KnowledgeTier(val title: String, val subtitle: String) {
    LIGHTWEIGHT("Lightweight (Essential)", "Core survival, emergency triage, water & shell"),
    MEDIUM("Standard (Civilization Starter)", "Adds crop cycles, animal husbandry, forge, offline Python/C & 12V solar"),
    LARGE("Comprehensive (Rebuild Vault)", "Full encyclopedia: seed vaults, micro-hydro, packet radio, kernel internals & offline algorithms")
}

data class KnowledgeDoc(
    val id: String,
    val category: KnowledgeCategory,
    val title: String,
    val summary: String,
    val body: String,
    val keywords: List<String>,
    /** A command worth running in the guest, offered with a Run button rather than typed by hand. */
    val command: String? = null,
    val tier: KnowledgeTier = KnowledgeTier.LIGHTWEIGHT
)


object KnowledgeLibrary {

    val docs: List<KnowledgeDoc> = EXTRA_DOCS + listOf(
        // ---------------- WATER ----------------
        KnowledgeDoc(
            id = "water_boil",
            category = KnowledgeCategory.WATER,
            title = "Making water safe to drink",
            summary = "Boiling, SODIS and chlorine dosing, with the numbers.",
            keywords = listOf("water", "boil", "drink", "purif", "sodis", "chlorine", "bleach", "safe"),
            body = """
                BOILING (the only method that reliably kills everything)
                • Bring to a full rolling boil. Hold 1 minute below 1000 m / 3300 ft.
                • Hold 3 minutes above 1000 m, because water boils cooler there.
                • Cloudy water: filter through cloth first, then boil. Cloudiness protects microbes from heat.
                • Stored boiled water re-contaminates fast. Keep it covered and use it within a day.

                SODIS (sunlight, plastic bottles)
                • Clear PET bottles only, never glass or cloudy/coloured plastic.
                • Fill with water that is already fairly clear (filter the silt out first).
                • Lay flat in full sun, 6 hours minimum; 2 full days if overcast.
                • Water above roughly 50 C disinfects much faster than the UV alone does.

                CHLORINE (unscented household bleach, 5% sodium hypochlorite)
                • 2 drops per litre of clear water, 4 drops if cloudy. Stir, wait 30 minutes.
                • It should smell faintly of chlorine. If it does not, dose again and wait 15 minutes.
                • Never use scented, colour-safe or thickened bleach.

                WHAT DOES NOT WORK
                • A sand/charcoal filter removes silt and some chemicals, not viruses. Filter, then boil.
                • Running water through a cloth, letting it stand in the sun in a bucket, or adding salt.
            """.trimIndent(),
            command = "echo 'Boil 1 min (3 min above 1000 m) | bleach 2 drops/L, 30 min wait'"
        ),
        KnowledgeDoc(
            id = "water_filter",
            category = KnowledgeCategory.WATER,
            title = "Building a gravity filter",
            summary = "Sand, charcoal and gravel layers, and what they do not remove.",
            keywords = listOf("filter", "sand", "charcoal", "gravel", "gravity", "build", "rain", "catchment"),
            body = """
                BOTTOM TO TOP (a clean bucket or cut bottle, small hole in the base)
                1. Cloth plug over the outlet.
                2. 5 cm crushed charcoal, rinsed until the rinse water runs clear.
                3. 10-15 cm fine clean sand.
                4. 5 cm coarse sand.
                5. 5 cm gravel or pebbles to stop the sand being churned up.

                HOW TO USE IT
                • Pour slowly. The first few litres come out grey; discard them.
                • Never let the top layer dry out between uses.
                • Rebuild the sand and charcoal roughly monthly, or whenever flow slows badly.

                WHAT IT BUYS YOU
                • Removes silt, most protozoan cysts and a good share of dissolved odours and some chemicals.
                • It does NOT reliably remove viruses, nitrates, salt or heavy metals.
                • Always boil or chlorinate the output. Filter first, disinfect second.

                RAIN CATCHMENT, IF YOU HAVE A ROOF
                • Litres per cm of rain = roof area in square metres x 10 x 0.8 (runoff loss).
                • First flush of 1-2 mm off a roof carries most of the bird droppings; divert it.
            """.trimIndent(),
            command = "echo 'Litres per cm rain = roof m2 * 10 * 0.8'"
        ),

        // ---------------- FIRST AID ----------------
        KnowledgeDoc(
            id = "aid_bleeding",
            category = KnowledgeCategory.FIRST_AID,
            title = "Severe bleeding",
            summary = "Direct pressure, packing and when a tourniquet is the right call.",
            keywords = listOf("bleed", "bleeding", "tourniquet", "wound", "pressure", "artery", "cut"),
            body = """
                ORDER OF OPERATIONS
                1. Direct pressure. Firm, continuous, both hands, on the wound. Ten full minutes without
                   lifting to peek. Most bleeding stops here.
                2. Pack the wound. For a deep wound in a limb, push gauze or clean cloth right down into
                   it and keep pressing. A dressing on the surface does nothing for a deep bleed.
                3. Tourniquet only for a limb bleed that pressure and packing will not stop.
                   • 5-7 cm (2-3 inches) above the wound, never over a joint.
                   • Tighten until the bleeding stops and the pulse beyond it disappears.
                   • Write the time it went on, on the patient, in permanent marker or tape.
                   • Do not loosen it. A tourniquet that stays on for hours is still better than bleeding out.
                4. Keep the patient warm and still. Shock kills quietly: pale, cold, clammy, fast weak pulse.

                DO NOT
                • Do not pull out an embedded object. Pack around it and press.
                • Do not use a tourniquet on a neck, head or torso wound. Press and pack.
                • Do not raise a badly fractured limb to "slow the bleeding".

                THIS IS A FIELD AID, NOT A SUBSTITUTE FOR A CLINICIAN
                Get real medical help the moment it exists.
            """.trimIndent()
        ),
        KnowledgeDoc(
            id = "aid_ors",
            category = KnowledgeCategory.FIRST_AID,
            title = "Rehydration (WHO ORS) and burns",
            summary = "The oral rehydration recipe, and what to do with a burn.",
            keywords = listOf("ors", "rehydrat", "dehydrat", "diarrhea", "vomit", "burn", "scald", "salt", "sugar"),
            body = """
                ORAL REHYDRATION SALTS (WHO formula, one litre)
                • 1 litre clean or boiled-then-cooled water
                • 6 level teaspoons sugar (about 25 g)
                • 1/2 level teaspoon salt (about 2.5 g)
                • Stir until dissolved. Sip continuously, small amounts often.
                • Too salty to drink means too much salt; add water until it is tolerable. It should taste
                  like a tear, not like seawater.

                DEHYDRATION SIGNS: dark urine, no urine for 8 hours, sunken eyes, skin that stays tented
                when pinched, confusion. Confusion and inability to drink is a hospital problem, not a
                recipe problem.

                BURNS
                • Cool with clean running water for 10-20 minutes. Start immediately; later helps less.
                • Remove rings, watches and clothing near the burn before swelling starts, if not stuck to it.
                • Cover loosely with clean dry non-fluffy cloth or cling film laid on, not wrapped tight.
                • Do NOT apply ice, butter, oil, toothpaste or any ointment to an open burn. Do not pop blisters.

                GO TO A CLINIC FOR: burns bigger than the patient's palm, any burn on face, hands, feet,
                genitals or across a joint, electrical or chemical burns, or anything with black/white
                leathery skin.
            """.trimIndent()
        ),

        // ---------------- SURVIVAL ----------------
        KnowledgeDoc(
            id = "surv_rule3",
            category = KnowledgeCategory.SURVIVAL,
            title = "The rule of 3s and shelter",
            summary = "What actually kills you first, and insulation that works.",
            keywords = listOf("surviv", "shelter", "cold", "hypothermia", "rule of 3", "exposure", "emergency"),
            body = """
                THE ORDER OF NEEDS
                • 3 minutes without air, or with a severe arterial bleed.
                • 3 hours in harsh weather without shelter.
                • 3 days without water.
                • 3 weeks without food.

                SHELTER
                • Insulate underneath you first. Bare ground conducts heat away roughly 25x faster than
                  still air. A 10 cm bed of dry leaves, cardboard or pine boughs matters more than the roof.
                • Stay dry. Wet clothing loses most of its insulation.
                • Keep the shelter small. You are heating your body, not the room.
                • Ventilation: never seal a shelter with a live flame inside. Carbon monoxide kills people
                  who felt warm and fine ten minutes earlier.

                HYPOTHERMIA
                • Early: shivering, clumsy hands, poor judgement. Get out of the wind and wet now.
                • Late: shivering stops, speech slurs, the patient stops feeling cold. Handle gently, insulate,
                  warm the trunk first (not the limbs), and give warm sweet drinks only if fully conscious.

                HEAT
                • Shade, then water, then salt. Pouring water on the skin and fanning works by evaporation.
                • Heat stroke (hot skin, confusion, no sweating) is a medical emergency: cool aggressively
                  and get help.
            """.trimIndent()
        ),
        KnowledgeDoc(
            id = "surv_fire",
            category = KnowledgeCategory.SURVIVAL,
            title = "Fire, water and direction",
            summary = "Tinder that lights when wet, and finding north without a compass.",
            keywords = listOf("fire", "tinder", "fatwood", "flint", "navigation", "north", "polaris", "shelter", "signal"),
            body = """
                FIRE
                • Three things: fuel, air, heat. Most failures are air or wet fuel, not a weak spark.
                • Tinder that works damp: birch bark (oils inside), pine fatwood, dry inner bark, char cloth,
                  dryer lint, a cotton ball with petroleum jelly.
                • Build small first. A teepee of pencil-thick sticks, then finger-thick, then wrist-thick.
                • Wet ground: lay a platform of dry sticks, otherwise the fire pulls moisture from below.

                DIRECTION, NORTHERN HEMISPHERE
                • Polaris sits in line with the two outer stars of the Big Dipper's pointer edge.
                • Shadow stick: plant a stick, mark the tip of its shadow, wait 15 minutes, mark again.
                  The line between the marks runs roughly east-west; the first mark is the west end.

                SIGNALLING
                • Three of anything is the distress signal: three fires in a triangle, three whistle blasts,
                  three flashes.
                • A signal mirror can be seen for tens of kilometres. Aim the reflected spot at the aircraft.
            """.trimIndent()
        ),

        // ---------------- FARMING ----------------
        KnowledgeDoc(
            id = "farm_staples",
            category = KnowledgeCategory.FARMING,
            title = "Calorie crops, compost and seed saving",
            summary = "What to plant when the goal is calories, not variety.",
            keywords = listOf("farm", "crop", "potato", "bean", "corn", "soil", "compost", "seed", "garden", "grow"),
            body = """
                CALORIES PER AREA, roughly
                • Potatoes and sweet potatoes: highest yield per square metre in most climates. 90-120 days.
                • Dry beans: protein plus storability, and they fix nitrogen. 50-60 days to green, longer to dry.
                • Winter squash: 6+ months of storage without refrigeration.
                • Corn: plant in blocks, not single rows, or pollination is poor.
                • Kale and collards: cut-and-come-again greens across a long season.

                COMPOST
                • 3 parts brown (dry leaves, straw, cardboard) to 1 part green (kitchen scraps, fresh grass).
                • Moisture like a wrung-out sponge, air by turning every 7-14 days.
                • Hot pile (turned, balanced) finishes in weeks; a cold pile takes months.
                • Do not compost meat, dairy or pet waste in a home pile.

                SEED SAVING
                • Save only from open-pollinated or heirloom varieties. F1 hybrids do not breed true.
                • Ferment tomato seeds in water for 3 days, rinse, then dry on paper away from direct sun.
                • Beans and peas: let pods dry on the plant, shell, and store cool and dark.

                ROTATION: legumes (fix nitrogen) -> leafy brassicas (heavy feeders) -> roots (light feeders).
            """.trimIndent(),
            command = "echo 'Compost 3 brown : 1 green | save seed only from open-pollinated'"
        ),

        // ---------------- RADIO ----------------
        KnowledgeDoc(
            id = "radio_freq",
            category = KnowledgeCategory.RADIO,
            title = "Frequencies worth knowing",
            summary = "Calling channels, weather, CB and local mesh.",
            keywords = listOf("radio", "freq", "frequency", "ham", "vhf", "uhf", "cb", "noaa", "walkie", "mesh", "comms"),
            body = """
                MONITORING
                • 146.520 MHz - 2 m national simplex calling frequency (no repeater needed).
                • 446.000 MHz - 70 cm national simplex calling frequency.
                • 462.5625 MHz - FRS/GMRS channel 1, the default handheld channel.
                • 27.065 MHz - CB channel 9, emergency and motorist assistance.
                • 162.400 / 162.425 / 162.450 / 162.475 / 162.500 / 162.525 / 162.550 MHz - NOAA weather radio.

                PRACTICAL NOTES
                • VHF is line-of-sight. Height beats power; get the antenna up.
                • A roll-up J-pole hung in a tree outperforms a rubber duck antenna by a wide margin.
                • Transmitting requires a licence in most countries. Listening does not.

                LOCAL MESH WITHOUT TOWERS
                • An Android phone can run a Wi-Fi hotspot that other devices join; that alone gives you a
                  local chat and file transfer with no infrastructure.
                • Range is tens of metres indoors, a few hundred outdoors with clear line of sight.
            """.trimIndent(),
            command = "echo 'VHF 146.520 | UHF 446.000 | FRS 1 462.5625 | CB9 27.065 | WX 162.400-162.550'"
        ),

        // ---------------- POWER ----------------
        KnowledgeDoc(
            id = "power_buck",
            category = KnowledgeCategory.POWER,
            title = "Running a phone board from 12 V",
            summary = "Buck converters, solar sizing and safe battery-free operation.",
            keywords = listOf("power", "solar", "12v", "buck", "battery", "watt", "volt", "lifepo4", "amp", "dc"),
            body = """
                THE ELECTRICAL PART
                • A phone expects a regulated 5 V supply over USB-C. Feeding it 12 V directly destroys it.
                • Use a buck converter rated for at least 3 A continuous (LM2596 modules are common, MP1584 is
                  smaller). Set the output to 5.1 V with a multimeter BEFORE connecting the phone.
                • Current draw: idle roughly 0.4 A at 5 V (~2 W), peaks around 1.5-2 A (~8-10 W) under load.
                  A 3 A supply leaves headroom for charging behaviour and transients.

                BATTERY-FREE CAUTION
                • Removing the battery removes the phone's own buffer. Any supply dropout powers the board off,
                  and a dirty brownout can corrupt storage. Add a large capacitor or a small buffer battery.
                • Keep the board on a heatsink. Without a battery there is no thermal mass and no charge heat,
                  but the SoC still throttles in still air.

                SOLAR SIZING, 24/7 LOAD
                • Daily energy = average watts x 24. At 8 W that is 192 Wh.
                • Panel watts = daily Wh / (peak sun hours x 0.7 system loss). 192 / (5 x 0.7) = ~55 W.
                • LiFePO4 buffer: 12.8 V nominal, 14.4 V full, 10.0 V cutoff, 2000-4000 cycles.
                • Fuse the positive lead at the battery. Size the fuse for the wire, not the load.
            """.trimIndent(),
            command = "echo 'Panel W = daily Wh / (peak sun h * 0.7) | buck out 5.1V, >=3A'"
        ),

        // ---------------- CODING ----------------
        KnowledgeDoc(
            id = "code_snake",
            category = KnowledgeCategory.CODING,
            title = "Write and run a terminal game",
            summary = "A working snake in plain Python, no curses, no installs.",
            keywords = listOf("snake", "game", "python", "terminal", "code", "write", "program", "curses", "play"),
            body = """
                The guest already has python3. A terminal game needs no window system, so it works over
                SSH and inside this desktop without X11.

                The trick for a game without curses: use ANSI escape codes. "\033[2J" clears the screen,
                "\033[H" homes the cursor, and "\033[?25l" hides it. Reading a key without pressing Enter
                needs the terminal in raw mode, which is what tty.setcbreak does.

                A complete snake, about 40 lines, works in any 80x24 terminal:

                python3 - <<'EOF'
                import os, random, sys, time, tty, termios
                W, H = 30, 15
                snake = [(5, 7)]; direction = (1, 0); food = (10, 7); score = 0
                def draw():
                    os.write(1, b"\033[2J\033[H")
                    grid = [["." for _ in range(W)] for _ in range(H)]
                    for x, y in snake: grid[y][x] = "#"
                    fx, fy = food; grid[fy][fx] = "*"
                    os.write(1, ("score %d  (q to quit)\r\n" % score + "\r\n".join("".join(r) for r in grid)).encode())
                fd = sys.stdin.fileno(); old = termios.tcgetattr(fd)
                try:
                    tty.setcbreak(fd)
                    while True:
                        draw()
                        import select
                        r, _, _ = select.select([sys.stdin], [], [], 0.15)
                        if r:
                            k = sys.stdin.read(1)
                            if k == "q": break
                            if k == "w" and direction != (0, 1): direction = (0, -1)
                            if k == "s" and direction != (0, -1): direction = (0, 1)
                            if k == "a" and direction != (1, 0): direction = (-1, 0)
                            if k == "d" and direction != (-1, 0): direction = (1, 0)
                        head = (snake[0][0] + direction[0], snake[0][1] + direction[1])
                        if head[0] < 0 or head[0] >= W or head[1] < 0 or head[1] >= H or head in snake:
                            break
                        snake.insert(0, head)
                        if head == food:
                            score += 1
                            while True:
                                food = (random.randrange(W), random.randrange(H))
                                if food not in snake: break
                        else:
                            snake.pop()
                finally:
                    termios.tcsetattr(fd, termios.TCSADRAIN, old)
                    os.write(1, b"\033[?25h\r\n")
                EOF

                HOW TO CHANGE IT
                • Bigger board: raise W and H. Faster: lower the select timeout (0.15 seconds).
                • WASD maps to the four direction tuples; keep the "not opposite" guard or the snake reverses
                  into itself.
                • The score line prints before the grid, so the game never scrolls out of view.

                WHY NOT CURSES: the Alpine base image may not have the curses module until you run
                "apk add python3 ncurses". ANSI codes need nothing.
            """.trimIndent(),
            command = "python3 -c 'import sys; print(sys.version.split()[0])'"
        ),
        KnowledgeDoc(
            id = "code_basics",
            category = KnowledgeCategory.CODING,
            title = "Python, shell and git essentials",
            summary = "The commands that do the most work per keystroke.",
            keywords = listOf("code", "coding", "python", "bash", "shell", "script", "git", "gcc", "compile", "loop"),
            body = """
                PYTHON
                • Serve a folder: python3 -m http.server 8080
                • Read a file: with open("log.txt") as f: text = f.read()
                • Run a command: import subprocess; print(subprocess.getoutput("uname -a"))
                • JSON: import json; data = json.load(open("x.json"))
                • One-liner HTTP fetch: python3 -c "import urllib.request;print(urllib.request.urlopen('http://localhost:8080').status)"

                SHELL (Alpine uses busybox ash; most bash syntax works, bash itself needs apk add bash)
                • Loop: for f in *.txt; do echo "== ${'$'}f"; done
                • Search: grep -rn "pattern" /etc/
                • Field cut: cut -d' ' -f1,3 /proc/meminfo
                • Replace in place: sed -i 's/old/new/g' config.conf
                • Disk usage: du -sh * | sort -h | tail
                • Free memory: free -m

                C ON ARM64
                • apk add gcc musl-dev, then: gcc -O2 -Wall main.c -o prog && ./prog

                GIT OFFLINE
                • git init && git add . && git commit -m "checkpoint"
                • git log --oneline, git diff, git checkout -- file
                • A local repo is a real backup only if it lives on a different disk or is pushed elsewhere.

                PACKAGES (Multi-distro: Alpine apk, Debian/Ubuntu apt, Arch pacman)
                • Alpine (bundled default): apk update && apk add <pkg>, apk del <pkg>, apk search <term>
                • Debian 12 / Ubuntu 24.04 / Kali: apt-get update && apt-get install -y <pkg>, apt-cache search <term>
                • Arch Linux ARM: pacman -Sy --noconfirm <pkg>, pacman -Ss <term>

                PINNING SHORTCUTS TO PHONIX DESKTOP (phonix-pin)
                • CLI command shortcut: phonix-pin "My Script" "python3 /root/app.py"
                • X11 GUI app shortcut: phonix-pin "Firefox" "firefox" --gui
            """.trimIndent(),
            command = "python3 -m http.server 8080"
        ),

        // ---------------- HARDWARE ----------------
        KnowledgeDoc(
            id = "hw_phonix_workstation",
            category = KnowledgeCategory.HARDWARE,
            title = "Phonix Linux distros, X11 desktops & terminal shortcuts",
            summary = "Switching Alpine/Debian/Ubuntu/Arch, IceWM/XFCE4/Openbox/LXQt on DISPLAY=:1, and phonix-pin.",
            keywords = listOf("distro", "alpine", "debian", "ubuntu", "arch", "kali", "apk", "apt", "pacman", "x11", "xfce", "icewm", "openbox", "lxqt", "pin", "shortcut", "phonix-pin", "terminal", "help"),
            body = """
                MULTI-DISTRO PROOT RUNTIME
                • Alpine Linux 3.24 ships bundled inside the APK (assets/alpine-rootfs.tgz) and extracts offline.
                • Debian 12, Ubuntu 24.04 LTS, Arch Linux ARM, and Kali Linux stream over HTTPS on demand when selected in the Setup Wizard or Linux System Hub, extracting into filesDir/linux/<distroId>.
                • Package managers: Alpine uses apk, Debian/Ubuntu/Kali use apt-get, and Arch uses pacman.

                X11 DESKTOP ENVIRONMENTS (DISPLAY=:1)
                • Phonix runs Xvfb (:1, 1280x720x24) + x11vnc (:5900) + websockify (:6080) inside the Linux Desktop window.
                • Switch window managers live in the Linux Desktop header:
                  - IceWM (~35 MB RAM, lightweight taskbar)
                  - XFCE4 (~140 MB RAM, full desktop panel & session)
                  - Openbox (~25 MB RAM, minimal window manager)
                  - LXQt (~110 MB RAM, modern Qt desktop)

                TERMINAL TABS, EXTRA KEYS & PIPE BOUNDARY
                • Use '+' in the Terminal header to open multiple independent shell tabs.
                • Extra keys bar: ↑/↓ history, TAB path/command completion, Ctrl+C (pkill active job), Ctrl+D (exit/close tab).
                • The main Terminal runs /bin/sh over standard pipes; run full-screen curses TUIs (htop, vim, nano) inside '+ xterm' in the Linux Desktop (X11) window.

                PINNING DESKTOP SHORTCUTS
                • Run 'phonix-pin "<Title>" "<cmd>" [--gui]' in Terminal, click the auto-detected Pin banner after apk/apt/pacman install, or right-click the desktop -> '+ Pin App to Desktop'.
            """.trimIndent(),
            command = "phonix-pin --help",
            tier = KnowledgeTier.LIGHTWEIGHT
        ),
        KnowledgeDoc(
            id = "hw_thermal",
            category = KnowledgeCategory.HARDWARE,
            title = "Thermals, throttling and local servers",
            summary = "Reading the real temperature, and binding servers above port 1024.",
            keywords = listOf("thermal", "temp", "heat", "throttle", "server", "nginx", "port", "cpu", "ram", "htop"),
            body = """
                READING THE REAL TEMPERATURE
                • Hottest zone: cat /sys/class/thermal/thermal_zone0/temp (millidegrees; 45000 = 45.0 C).
                • All zones with names: for z in /sys/class/thermal/thermal_zone*; do echo "${'$'}z ${'$'}(cat ${'$'}z/type) ${'$'}(cat ${'$'}z/temp)"; done
                • Sustained above roughly 70 C means throttling is imminent; above 85 C stop the load.

                WHAT ACTUALLY HELPS
                • Ambient air, not fan noise: 10 C cooler room beats any software tweak.
                • A copper shim or graphite pad between SoC and heatsink, plus airflow across the back.
                • Do not stack the board on fabric; the back panel is part of the heatsink.

                SERVERS IN THE GUEST
                • The Linux userland is unrooted, so bind to ports >= 1024: 8088 for nginx, 2222 for Dropbear SSH, 8000 for Python HTTP.
                • Check it from outside: curl -I http://127.0.0.1:8088
                • A port answering on 127.0.0.1 is not the same as reachable from the LAN; bind 0.0.0.0 for that.

                CPU AND MEMORY
                • top -b -n1 | head -20 (or run interactive htop inside '+ xterm' on the X11 Linux Desktop)
                • Per-core load: cat /proc/loadavg
            """.trimIndent(),
            command = "cat /sys/class/thermal/thermal_zone0/temp",
            tier = KnowledgeTier.LIGHTWEIGHT
        ),
        KnowledgeDoc(
            id = "hw_docker_pihole_smb",
            category = KnowledgeCategory.HARDWARE,
            title = "Docker, Pi-hole Adblock & Samba SMB on unrooted Android",
            summary = "Running phonix-smb (:4445), phonix-adblock (:5353), udocker, and QEMU Linux VM.",
            keywords = listOf("docker", "udocker", "qemu", "pihole", "pi-hole", "adblock", "dns", "dnsmasq", "smb", "samba", "nas", "share", "termux", "container"),
            body = """
                1. SAMBA SMB FILE SHARING (phonix-smb on port 4445)
                • Standard SMB port 445 is privileged (<1024). Phonix binds real smbd to port 4445 and shares /root/shared.
                • Install & start: install 'samba' in Server Center (or 'apk add samba') and run 'phonix-smb start'.
                • Connect from Windows/macOS/Linux/Android: smb://<LAN-IP>:4445/phonix (guest access enabled).

                2. PI-HOLE DNS SINKHOLE ENGINE (phonix-adblock on port 5353)
                • Standard Pi-hole's curl|bash installer requires systemd and port 53, which unrooted PRoot does not provide.
                • However, Pi-hole's actual DNS sinkhole engine is dnsmasq + a hosts blocklist, which runs natively in PRoot via 'phonix-adblock':
                  - 'phonix-adblock start' starts dnsmasq on port 5353 using /etc/phonix-adblock.hosts.
                  - 'phonix-adblock update' downloads 130,000+ ad/tracker domains from StevenBlack's unified hosts list and reloads dnsmasq.

                3. DOCKER IN UNROOTED PROOT (phonix-docker: udocker vs QEMU VM)
                • Native 'dockerd' requires Linux kernel cgroups, mount namespaces, and overlayfs (blocked on unrooted Android).
                • Option A — Rootless OCI Container Runner (udocker):
                  Run 'phonix-docker setup-udocker' to install udocker inside PRoot, then pull & run Docker Hub images without a kernel daemon:
                  'udocker pull alpine:latest && udocker run alpine:latest uname -a'
                • Option B — Full Linux Kernel VM (qemu-system-aarch64):
                  Run 'phonix-docker setup-qemu' to install QEMU ARM64 and create /root/docker-vm/start-vm.sh with port forwarding (2223->22, 8090->80, 9000->9000) for native dockerd and docker compose.
            """.trimIndent(),
            command = "phonix-docker --help",
            tier = KnowledgeTier.LIGHTWEIGHT
        ),

        // =================================================================
        // MEDIUM TIER: CIVILIZATION STARTER (Agriculture, Food Storage, Solar, Offline Coding)
        // =================================================================
        KnowledgeDoc(
            id = "farm_crop_cycles",
            category = KnowledgeCategory.FARMING,
            title = "Crop rotation & soil fertility",
            summary = "Nitrogen fixing legumes, crop rotation cycles, and compost thermal piles.",
            keywords = listOf("farming", "crops", "soil", "rotation", "nitrogen", "compost", "legumes", "seeds", "heirloom"),
            body = """
                THE 4-YEAR ROTATION SYSTEM
                Year 1: Heavy feeders (Corn, Squash, Brassicas, Tomatoes) — require rich, composted soil.
                Year 2: Legumes (Beans, Peas, Clover) — bacteria (Rhizobium) in root nodules fix atmospheric nitrogen.
                Year 3: Light feeders (Root crops: Carrots, Beets, Onions, Turnips) — excess nitrogen causes split roots.
                Year 4: Cover crop or Green Manure (Rye, Buckwheat, Winter Vetch) — tilled under to build humus.

                COMPOST THERMAL PILE (140°F - 160°F / 60°C - 70°C)
                • Carbon (Browns) to Nitrogen (Greens) ratio: 30:1 by volume.
                • Browns: Dry leaves, straw, untreated wood chips, shredded cardboard.
                • Greens: Kitchen scraps, fresh grass, manure (chicken, rabbit, goat, horse — NEVER dog/cat/pig).
                • Turn every 7-10 days when internal core temperature drops below 110°F.
                • High heat (above 135°F for 3 consecutive days) kills weed seeds and plant pathogens.

                SEED SAVING & HEIRLOOM PRESERVATION
                • Save only open-pollinated (heirloom) seeds; F1 hybrids will not breed true in generation F2.
                • Beans & Peas: Let pods dry completely brown and brittle on the vine before shelling.
                • Tomatoes: Ferment seeds in their pulp for 3 days to digest the germination-inhibiting gel, then rinse and air dry.
                • Moisture test: A dry seed will shatter under a hammer, not smash flat. Store below 10% humidity in airtight jars with desiccant or wood ash.
            """.trimIndent(),
            command = "echo 'Rotation: Heavy Feeders -> Legumes -> Roots -> Cover Crop'",
            tier = KnowledgeTier.MEDIUM
        ),
        KnowledgeDoc(
            id = "surv_food_preserve",
            category = KnowledgeCategory.SURVIVAL,
            title = "Long-term food preservation (No power)",
            summary = "Root cellaring, dry salting, vinegar pickling, and wood smoke curing.",
            keywords = listOf("preserve", "curing", "smoke", "salt", "cellar", "root", "storage", "food", "shelf"),
            body = """
                ROOT CELLARING (32°F - 40°F / 0°C - 4°C, 85-95% humidity)
                • Unwashed root crops: Potatoes, Carrots, Beets, Parsnips.
                • Pack in dry sand or damp sawdust inside dark, ventilated wooden bins.
                • Keep potatoes and apples separated: apples emit ethylene gas which induces potato sprouting.
                • Winter squash and pumpkins prefer dry storage (50°F - 55°F, 60% humidity).

                SALT CURING (Meat preservation)
                • Dry salt cure ratio: 3% - 4% salt by weight of fresh meat (minimum 35 g non-iodized salt per 1 kg meat).
                • Rub salt into all muscle creases and around bones; bone souring occurs if salt penetrates too slowly.
                • Rule of thumb: 2 days of curing per inch of meat thickness at cool temperature (< 40°F / 4°C).

                SMOKING FOR PRESERVATION
                • Cold smoke (< 90°F / 32°C): Does not cook meat; deposits antimicrobial phenols and dries tissue.
                • Requires hardwood sawdust (Oak, Hickory, Apple, Alder). Avoid all resinous conifers (Pine, Cedar).
                • Cure in salt first, smoke for 24-72 hours until surface develops a dark, leathery pellicle.
            """.trimIndent(),
            command = "echo 'Meat cure: 35g non-iodized salt per 1kg meat; chill < 4C'",
            tier = KnowledgeTier.MEDIUM
        ),
        KnowledgeDoc(
            id = "power_solar_sizing",
            category = KnowledgeCategory.POWER,
            title = "Off-grid 12V/24V solar & battery sizing",
            summary = "Watt-hour budgeting, battery chemistry, MPPT vs PWM, wire gauge AWG selection.",
            keywords = listOf("solar", "battery", "12v", "24v", "inverter", "mppt", "amp", "watt", "wire", "gauge"),
            body = """
                DAILY WATT-HOUR (Wh) BUDGET
                • Wh = Watts × Hours of daily runtime. Example: 15W phone/mini-PC × 24h = 360 Wh.
                • Inverter / charge controller efficiency losses: multiply total Wh by 1.25. (360 × 1.25 = 450 Wh).

                BATTERY CAPACITY (Amp-Hours @ 12V)
                • Lead-Acid (AGM/Flooded): Max 50% Depth of Discharge (DoD) for longevity.
                  Capacity needed = (Daily Wh ÷ 12V) × 2. (450 Wh ÷ 12V × 2 = 75 Ah).
                • Lithium Iron Phosphate (LiFePO4): 80% DoD, 3000+ cycles.
                  Capacity needed = (Daily Wh ÷ 12V) × 1.25. (450 Wh ÷ 12V × 1.25 = 47 Ah).

                SOLAR ARRAY SIZING
                • Average peak sun hours: 4.0 hours per winter day in temperate zones.
                • Array Watts needed = Daily Wh ÷ Peak Sun Hours ÷ 0.75 (dust/temp derate).
                  450 Wh ÷ 4.0h ÷ 0.75 ≈ 150 Watts of PV panels.

                WIRE GAUGE (AWG) FOR 12V DC (Limit voltage drop to < 3%)
                • 10 Amps at 10 feet: 12 AWG
                • 20 Amps at 10 feet: 10 AWG (or 8 AWG for 20 feet)
                • 50 Amps at 10 feet: 4 AWG
                • Formula: Voltage Drop (V) = 2 × Length(ft) × Current(A) × Ohms_per_1000ft ÷ 1000.
            """.trimIndent(),
            command = "echo 'Wh = W * hrs; LiFePO4 Ah = Wh / 12 * 1.25; Array W = Wh / (SunHours * 0.75)'",
            tier = KnowledgeTier.MEDIUM
        ),
        KnowledgeDoc(
            id = "aid_wound_closure",
            category = KnowledgeCategory.FIRST_AID,
            title = "Wound closure, splints & burn triage",
            summary = "Irrigation volume, butterfly closure vs suturing, fracture immobilization, burn degrees.",
            keywords = listOf("wound", "suture", "splint", "burn", "infection", "closure", "iodine", "bandage"),
            body = """
                WOUND IRRIGATION (The golden rule)
                • The solution to pollution is dilution: Irrigate with 500 mL - 1000 mL of sterile saline or boiled clean water.
                • Use a 20 mL syringe with an 18-gauge catheter to generate ~8 psi of pressure.
                • Never pour full-strength hydrogen peroxide or rubbing alcohol into deep tissue; it destroys healthy granulating cells.

                CLOSURE CRITERIA
                • Close only clean, fresh wounds (< 6 to 8 hours old on the body, < 12 hours on the face).
                • DO NOT CLOSE: Animal or human bites, puncture wounds, contaminated wounds, or wounds older than 8 hours. Pack open with sterile wet-to-dry gauze to drain.
                • Closure methods: Steri-Strips or butterfly bandages placed across tension lines. Cyanoacrylate (superglue) works on clean, shallow superficial incisions (avoid eye area).

                BURNS
                • 1st Degree: Red, painful, dry (sunburn). Cool clean water, aloe vera.
                • 2nd Degree: Blisters, intense pain, weeping. Do not pop blisters. Cover with sterile non-adherent dressing.
                • 3rd Degree: Leathery, white or charred, painless in center (nerve endings destroyed). Medical emergency. Cover sterile, hydrate orally with ORS if patient can swallow.
            """.trimIndent(),
            command = "echo 'Irrigate with 500-1000ml sterile water. Do NOT close animal bites or old wounds.'",
            tier = KnowledgeTier.MEDIUM
        ),
        KnowledgeDoc(
            id = "code_python_offline",
            category = KnowledgeCategory.CODING,
            title = "Offline Python stdlib reference",
            summary = "Built-in HTTP server, socket networking, SQLite3 database, file parsing.",
            keywords = listOf("python", "coding", "offline", "server", "socket", "sqlite", "http", "script"),
            body = """
                INSTANT LOCAL HTTP SERVER
                ${'$'} python3 -m http.server 8080 --bind 0.0.0.0

                SQLITE3 DATABASE IN 6 LINES (No dependencies)
                import sqlite3
                con = sqlite3.connect("vault.db")
                cur = con.cursor()
                cur.execute("CREATE TABLE IF NOT EXISTS inventory(item TEXT, qty INT)")
                cur.execute("INSERT INTO inventory VALUES ('Seeds - Bean', 500)")
                con.commit()
                for row in cur.execute("SELECT * FROM inventory"): print(row)

                TCP SOCKET SERVER (Raw networking)
                import socket
                s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                s.bind(('0.0.0.0', 9999))
                s.listen(1)
                conn, addr = s.accept()
                conn.sendall(b"PHONIX OFFLINE NODE READY\n")
                conn.close()
            """.trimIndent(),
            command = "python3 -c \"import sys; print(f'Python {sys.version}')\"",
            tier = KnowledgeTier.MEDIUM
        ),
        KnowledgeDoc(
            id = "code_c_minimal",
            category = KnowledgeCategory.CODING,
            title = "Minimal C on Linux (GCC / Clang)",
            summary = "Compilation, POSIX file descriptors, syscalls, raw terminal IO.",
            keywords = listOf("c", "clang", "gcc", "compile", "posix", "syscall", "memory", "terminal"),
            body = """
                COMPILE WITHOUT EXTERNAL LIBS
                ${'$'} gcc -O2 -Wall -o tool main.c
                ${'$'} apk add build-base  # Installs gcc, musl-dev, make

                DIRECT FILE IO VIA SYSCALLS
                #include <unistd.h>
                #include <fcntl.h>
                int main() {
                    int fd = open("log.txt", O_WRONLY | O_CREAT | O_APPEND, 0644);
                    const char msg[] = "Device rebooted\n";
                    write(fd, msg, sizeof(msg) - 1);
                    close(fd);
                    return 0;
                }
            """.trimIndent(),
            command = "echo -e '#include <stdio.h>\nint main(){printf(\"C OK\\n\");return 0;}' > /tmp/t.c && gcc /tmp/t.c -o /tmp/t && /tmp/t",
            tier = KnowledgeTier.MEDIUM
        ),

        // =================================================================
        // LARGE TIER: COMPREHENSIVE VAULT (End-of-World Rebuild Manual)
        // =================================================================
        KnowledgeDoc(
            id = "farm_caloric_yield",
            category = KnowledgeCategory.FARMING,
            title = "Calorie & protein yield per acre (Survival agriculture)",
            summary = "Potatoes vs grain vs legumes, companion guild planting, goat/chicken feed ratios.",
            keywords = listOf("calories", "yield", "acre", "protein", "survival", "potatoes", "wheat", "chickens", "livestock"),
            body = """
                TOP CALORIC DENSITY CROPS (Per 1000 sq ft / ~93 sq meters)
                1. Potatoes (White/Russet): ~1,200,000 kcal / 1000 sq ft. High Vitamin C, potassium, protein balance.
                2. Sweet Potatoes: ~1,100,000 kcal. Excellent storage in warm climates, leaves are edible greens.
                3. Corn (Flint / Dent for meal): ~600,000 kcal. Requires nitrogen; stores for years as dry grain.
                4. Winter Squash (Butternut/Hubbard): ~450,000 kcal. Stores 6+ months without processing.
                5. Dry Beans (Pinto/Black/Navy): ~350,000 kcal. Crucial plant protein (21% protein by dry weight).

                THE THREE SISTERS GUILD
                • Plant Corn first. When 6 inches high, plant Pole Beans around stalk, and Winter Squash in basins between.
                • Corn provides living trellis for beans.
                • Beans fix atmospheric nitrogen for high-nitrogen corn.
                • Squash broad prickly leaves shade ground to suppress weeds and retain soil moisture.

                LIVESTOCK CONVERSION RATIOS
                • Chickens: 2 kg grain produces ~1 kg eggs or meat. Feed kitchen scraps and free-range insects.
                • Dairy Goats: 1 acre browse supports 2-3 dairy goats, producing 2-4 liters milk per day on forage.
                • Rabbits: 1 doe produces 40-50 lbs dressed meat per year on greens and hay.
            """.trimIndent(),
            command = "echo 'Top survival calories: Potatoes (1.2M kcal/1000sqft) > Sweet Potatoes > Corn > Squash > Beans'",
            tier = KnowledgeTier.LARGE
        ),
        KnowledgeDoc(
            id = "surv_water_turbine",
            category = KnowledgeCategory.POWER,
            title = "Micro-hydro power generation (Pelton wheels)",
            summary = "Head height vs flow rate equations, alternator conversion, penstock pipe friction.",
            keywords = listOf("hydro", "turbine", "pelton", "water", "head", "generator", "alternator", "watts"),
            body = """
                HYDROELECTRIC POWER FORMULA
                Watts = Head (meters) × Flow Rate (litres per second) × 9.81 × System Efficiency (typically 0.50 to 0.60)
                Example: 10 meters head (vertical drop) with 5 L/sec stream flow:
                Watts = 10 × 5 × 9.81 × 0.55 ≈ 270 Watts continuous (6.48 kWh per day!).

                KEY COMPONENTS
                1. Intake & Settling Box: Screen out leaves, gravel, and silt before penstock pipe.
                2. Penstock (Pipe): Smooth PVC or HDPE. Friction loss drops available head; size pipe so water velocity stays under 1.5 m/s.
                3. Jet Nozzle: Converts water pressure into high-velocity water jet directed at turbine cups.
                4. Pelton Runner: Spoon-shaped split buckets split jet in half and reverse momentum.
                5. Permanent Magnet Alternator (PMA): Low RPM car alternator rewound or brushless stepper/servo motor rectified with 3-phase diode bridge.
            """.trimIndent(),
            command = "echo 'Watts = Head(m) * Flow(L/s) * 9.81 * 0.55'",
            tier = KnowledgeTier.LARGE
        ),
        KnowledgeDoc(
            id = "radio_packet_lora",
            category = KnowledgeCategory.RADIO,
            title = "Off-grid packet radio & LoRa mesh networks",
            summary = "Meshtastic/AX.25 packet frequencies, field dipole calculation, solar repeater nodes.",
            keywords = listOf("radio", "lora", "meshtastic", "mesh", "packet", "ax25", "antenna", "dipole", "aprs"),
            body = """
                MESHTASTIC & LORA FREQUENCIES
                • North America: 906.875 MHz - 915 MHz (Ch 0: 906.875 MHz)
                • Europe / UK: 869.525 MHz / 433 MHz
                • Line of sight range: 5 km ground-to-ground; 50-100+ km mountain-to-valley.

                CALCULATING WIRE ANTENNA LENGTHS (Half-wave Dipole)
                • Total length in feet = 468 ÷ Frequency in MHz
                • Total length in meters = 143 ÷ Frequency in MHz
                • Example for 146.520 MHz (2m VHF National Calling):
                  143 ÷ 146.520 = 0.976 meters total (each leg of the dipole is 48.8 cm).

                STANDALONE SOLAR REPEATER NODE
                • 1× ESP32/nRF52 LoRa module (draws ~15-25 mA idle, 120 mA transmit).
                • 1× 18650 Li-ion cell (3.7V 3000 mAh = 11.1 Wh).
                • 1× 5V 5W solar panel with TP4056 or BQ25180 charge controller.
                • Run indefinitely on high elevation / roof trees as autonomous store-and-forward mesh repeater.
            """.trimIndent(),
            command = "echo 'Half-wave dipole meters = 143 / Freq_MHz (each leg = half)'",
            tier = KnowledgeTier.LARGE
        ),
        KnowledgeDoc(
            id = "aid_wilderness_pharm",
            category = KnowledgeCategory.FIRST_AID,
            title = "Emergency wilderness medicine & botany",
            summary = "Willow bark salicin, activated charcoal dosing, hyperthermia/hypothermia resuscitation.",
            keywords = listOf("medicine", "botany", "charcoal", "willow", "salicin", "hypothermia", "rehydration", "poison"),
            body = """
                ACTIVATED CHARCOAL (Ingested poisons)
                • Dose: 1 gram per kg body weight (approx. 50g to 100g for adults) mixed into clean water.
                • Must be given within 1 to 2 hours of toxin ingestion to bind chemicals in GI tract.
                • Ineffective for: Strong acids, alkalis, heavy metals (iron/lead), petroleum distillates, and ethanol.

                WHITE WILLOW BARK (Salix spp. - Natural analgesic / antipyretic)
                • Contains salicin, metabolized in the liver to salicylic acid (aspirin precursor).
                • Harvest inner bark (greenish cambium layer) in spring.
                • Decoction: Simmer 2-3 teaspoons of crushed inner bark in 1 cup water for 15 minutes. Strain and drink up to 3 times daily.
                • Contraindications: Same as aspirin (never give to children under 16 with viral fevers due to risk of Reye's syndrome).

                HYPOTHERMIA RESUSCITATION
                • Mild (32°C - 35°C): Shivering present. Dry clothes, warm sweet fluids, gentle active rewarming.
                • Severe (< 28°C): Shivering stops, altered mental state, rigid muscles.
                • Handle very gently: rough movement can trigger fatal ventricular fibrillation.
                • Rewarm core first (chest, neck, groin with warm water bottles). Never massage cold extremities (causes cold shock to heart).
            """.trimIndent(),
            command = "echo 'Activated charcoal: 1g/kg within 1-2 hours of toxin ingestion'",
            tier = KnowledgeTier.LARGE
        ),
        KnowledgeDoc(
            id = "code_linux_kernel",
            category = KnowledgeCategory.CODING,
            title = "Linux kernel, sysfs & proot internals",
            summary = "Proot ptrace interception, rootfs layout, sysfs hardware control, loop mounts.",
            keywords = listOf("linux", "kernel", "proot", "ptrace", "sysfs", "proc", "chroot", "syscall"),
            body = """
                HOW PROOT WORKS ON ANDROID
                • Proot uses the ptrace(2) system call to trace system calls made by guest binaries.
                • When a binary calls open("/etc/passwd", ...), proot traps the syscall before the Android kernel sees it,
                  rewrites the path argument in registers to "/data/user/0/.../files/linux/alpine/etc/passwd",
                  and lets the kernel execute the translated call.
                • Result: Full guest Linux filesystem without root privileges, loop mounts, or kernel modifications.

                SYSFS HARDWARE TELEMETRY PATHS
                • Thermal zones: /sys/class/thermal/thermal_zone*/temp (divide value by 1000 for °C).
                • Battery status: /sys/class/power_supply/battery/status, current_now, voltage_now.
                • CPU frequency: /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq.
                • Display brightness: /sys/class/backlight/*/brightness.
            """.trimIndent(),
            command = "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null || cat /proc/cpuinfo | head -10",
            tier = KnowledgeTier.LARGE
        ),
        KnowledgeDoc(
            id = "code_algorithms_vault",
            category = KnowledgeCategory.CODING,
            title = "Core algorithms & crypto primitives",
            summary = "Dijkstra shortest path, binary search, SHA-256 bitwise rotations, Huffman compression.",
            keywords = listOf("algorithms", "dijkstra", "sha256", "crypto", "huffman", "binary", "search", "math"),
            body = """
                BINARY SEARCH (O(log n))
                def binary_search(arr, target):
                    low, high = 0, len(arr) - 1
                    while low <= high:
                        mid = (low + high) // 2
                        if arr[mid] == target: return mid
                        elif arr[mid] < target: low = mid + 1
                        else: high = mid - 1
                    return -1

                DIJKSTRA SHORTEST PATH (Graph routing)
                import heapq
                def dijkstra(graph, start):
                    distances = {node: float('inf') for node in graph}
                    distances[start] = 0
                    pq = [(0, start)]
                    while pq:
                        curr_dist, curr_node = heapq.heappop(pq)
                        if curr_dist > distances[curr_node]: continue
                        for neighbor, weight in graph[curr_node].items():
                            distance = curr_dist + weight
                            if distance < distances[neighbor]:
                                distances[neighbor] = distance
                                heapq.heappush(pq, (distance, neighbor))
                    return distances
            """.trimIndent(),
            command = "echo 'Binary Search O(log n) | Dijkstra O((V + E) log V)'",
            tier = KnowledgeTier.LARGE
        )
    )

    var activeTier: KnowledgeTier = KnowledgeTier.LIGHTWEIGHT

    fun setTier(tier: KnowledgeTier) {
        activeTier = tier
    }

    /**
     * Packs imported from the phone. Empty until [attachPacks] runs, which the view model does on
     * construction so the shelves are complete before anything reads them.
     */
    private var packStore: LibraryPackStore? = null
    private var packDocs: List<KnowledgeDoc> = emptyList()

    fun attachPacks(store: LibraryPackStore) {
        packStore = store
        reloadPacks()
    }

    /** Re-reads the installed packs. Called after an import or a delete, never on a timer. */
    fun reloadPacks() {
        packDocs = runCatching { packStore?.docs()?.map { doc -> doc.toKnowledgeDoc() } ?: emptyList() }
            .getOrDefault(emptyList())
    }

    fun installedPacks(): List<InstalledPack> =
        runCatching { packStore?.packs() ?: emptyList() }.getOrDefault(emptyList())

    /** Built-in documents plus whatever packs are installed; the order keeps built-ins first. */
    private fun allDocs(): List<KnowledgeDoc> = if (packDocs.isEmpty()) docs else docs + packDocs

    private fun isDocInTier(doc: KnowledgeDoc, tier: KnowledgeTier): Boolean = when (tier) {
        KnowledgeTier.LIGHTWEIGHT -> doc.tier == KnowledgeTier.LIGHTWEIGHT
        KnowledgeTier.MEDIUM -> doc.tier in setOf(KnowledgeTier.LIGHTWEIGHT, KnowledgeTier.MEDIUM)
        KnowledgeTier.LARGE -> true
    }

    fun docsFor(category: KnowledgeCategory?, tier: KnowledgeTier = activeTier): List<KnowledgeDoc> =
        allDocs().filter { doc ->
            isDocInTier(doc, tier) && (category == null || doc.category == category)
        }

    fun document(id: String): KnowledgeDoc? = allDocs().firstOrNull { it.id == id }

    /** How many documents a tier actually carries, counting the lower tiers it includes. */
    fun docCount(tier: KnowledgeTier): Int = docsFor(null, tier).size

    /** Real UTF-8 size of the text a tier carries, summed from the documents themselves. */
    fun approxBytes(tier: KnowledgeTier): Long = docsFor(null, tier).sumOf { doc ->
        (doc.title.length + doc.summary.length + doc.body.length + doc.keywords.sumOf { it.length })
            .toLong()
    }

    /** Honest label for the UI: doc count plus the measured size of that text. */
    fun sizeLabel(tier: KnowledgeTier): String =
        "${docCount(tier)} docs · ${bytesLabel(approxBytes(tier))}"

    fun bytesLabel(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(java.util.Locale.ROOT, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "for", "to", "of", "in", "on", "is", "it", "how", "what",
        "do", "does", "i", "you", "my", "me", "we", "can", "with", "that", "this", "be", "are"
    )

    /**
     * Ranks documents for a question. With a category selected only that shelf is searched, which
     * keeps a 1.5B model's context short and on-topic; "All" searches everything and is the slower,
     * broader option.
     */
    fun retrieve(query: String, category: KnowledgeCategory?, tier: KnowledgeTier = activeTier, limit: Int = 2): List<KnowledgeDoc> {
        val terms = query.lowercase(java.util.Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
        if (terms.isEmpty()) return emptyList()

        return docsFor(category, tier)
            .map { doc ->
                val haystack = (doc.title + " " + doc.summary + " " + doc.keywords.joinToString(" ") + " " + doc.body)
                    .lowercase(java.util.Locale.ROOT)
                var score = 0
                for (term in terms) {
                    if (doc.keywords.any { it.equals(term, ignoreCase = true) }) score += 6
                    else if (doc.title.lowercase(java.util.Locale.ROOT).contains(term)) score += 4
                    if (haystack.contains(term)) score += 1
                }
                doc to score
            }
            .filter { it.second >= 4 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /** The block handed to the model as reference material, with the shelf it came from. */
    fun contextFor(query: String, category: KnowledgeCategory?, tier: KnowledgeTier = activeTier): String? {
        val hits = retrieve(query, category, tier)
        if (hits.isEmpty()) return null
        return hits.joinToString("\n\n") { doc ->
            "[${doc.category.title}] ${doc.title}\n${doc.body}"
        }
    }
}

