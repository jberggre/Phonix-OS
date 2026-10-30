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

data class KnowledgeDoc(
    val id: String,
    val category: KnowledgeCategory,
    val title: String,
    val summary: String,
    val body: String,
    val keywords: List<String>,
    /** A command worth running in the guest, offered with a Run button rather than typed by hand. */
    val command: String? = null
)

object KnowledgeLibrary {

    val docs: List<KnowledgeDoc> = listOf(
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

                PACKAGES (Alpine)
                • apk update, apk add <pkg>, apk del <pkg>, apk search <term>
            """.trimIndent(),
            command = "python3 -m http.server 8080"
        ),

        // ---------------- HARDWARE ----------------
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
                • The Alpine userland is unrooted, so bind to ports >= 1024: 8080 for nginx, 3000 for Node.
                • apk add nginx, then nginx -c /etc/nginx/nginx.conf
                • Check it from outside: curl -I http://127.0.0.1:8080
                • A port answering on 127.0.0.1 is not the same as reachable from the LAN; bind 0.0.0.0 for that.

                CPU AND MEMORY
                • htop, or: top -b -n1 | head -20
                • Per-core load: cat /proc/loadavg
            """.trimIndent(),
            command = "cat /sys/class/thermal/thermal_zone0/temp"
        )
    )

    fun docsFor(category: KnowledgeCategory?): List<KnowledgeDoc> =
        if (category == null) docs else docs.filter { it.category == category }

    fun document(id: String): KnowledgeDoc? = docs.firstOrNull { it.id == id }

    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "for", "to", "of", "in", "on", "is", "it", "how", "what",
        "do", "does", "i", "you", "my", "me", "we", "can", "with", "that", "this", "be", "are"
    )

    /**
     * Ranks documents for a question. With a category selected only that shelf is searched, which
     * keeps a 1.5B model's context short and on-topic; "All" searches everything and is the slower,
     * broader option.
     */
    fun retrieve(query: String, category: KnowledgeCategory?, limit: Int = 2): List<KnowledgeDoc> {
        val terms = query.lowercase(java.util.Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
        if (terms.isEmpty()) return emptyList()

        return docsFor(category)
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
    fun contextFor(query: String, category: KnowledgeCategory?): String? {
        val hits = retrieve(query, category)
        if (hits.isEmpty()) return null
        return hits.joinToString("\n\n") { doc ->
            "[${doc.category.title}] ${doc.title}\n${doc.body}"
        }
    }
}
