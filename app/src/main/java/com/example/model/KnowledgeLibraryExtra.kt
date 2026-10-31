package com.example.model

/**
 * Additional Medium and Large tier documents.
 *
 * These live in their own file so the original corpus stays readable, and so the tier counts in the
 * UI keep being measured from real documents ([KnowledgeLibrary.docCount]) rather than from a
 * hand-written label. Every entry here is reference material that works with no network: numbers,
 * procedures and formulas that can be followed on paper.
 */
val EXTRA_DOCS: List<KnowledgeDoc> = listOf(

    // =====================================================================
    // MEDIUM TIER
    // =====================================================================
    KnowledgeDoc(
        id = "water_rain_catch",
        category = KnowledgeCategory.WATER,
        title = "Roof rainwater catchment sizing",
        summary = "How much rain a roof actually yields, and how big the tank must be.",
        keywords = listOf("rain", "catchment", "roof", "tank", "water", "harvest", "gutter"),
        body = """
            THE YIELD FORMULA
            • Litres per storm = roof footprint (m2) x rainfall (mm) x 0.8 run-off coefficient.
            • A 50 m2 roof in a 20 mm storm yields 50 x 20 x 0.8 = 800 litres.
            • Use the horizontal footprint, not the sloped area, for a pitched roof.

            FIRST-FLUSH DIVERSION
            • The first 0.5 mm of rain washes the roof. Divert roughly 0.5 L per m2 of roof, then switch to the tank.
            • A simple standpipe with a floating ball, or a 20 L bucket that fills and tips, does this without power.

            TANK SIZING
            • Dry-season demand (L/day) x longest dry spell (days) = minimum storage.
            • 4 people at 15 L/day for 60 dry days needs 3,600 L, so a 4,000 L tank with headroom.
            • Keep the outlet 15 cm above the floor so silt does not flow out; add a bottom drain for cleaning.
            • Screen every inlet and overflow with 1 mm mesh: mosquitoes breed in any open water surface.
        """.trimIndent(),
        command = "echo 'yield L = roof m2 x rain mm x 0.8'",
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "water_biosand",
        category = KnowledgeCategory.WATER,
        title = "Building a slow sand (biosand) filter",
        summary = "Concrete-and-sand filter that removes turbidity and most pathogens, with no power.",
        keywords = listOf("filter", "sand", "biosand", "turbidity", "water", "slow sand", "build"),
        body = """
            LAYERS, BOTTOM TO TOP (in a 200 L drum or concrete box)
            • 5 cm gravel 20-40 mm - drainage, keeps sand out of the outlet.
            • 5 cm gravel 5-10 mm.
            • 5 cm coarse sand 2-5 mm.
            • 55-60 cm washed fine sand, grain 0.15-0.35 mm. This is the actual filter; do not shorten it.
            • 5 cm gravel on top to stop the sand being disturbed when refilling.

            PLUMBING
            • Outlet pipe rises inside the box to 5 cm above the sand surface. That standing water layer keeps the biological layer alive.
            • Flow: roughly 0.1-0.3 L per minute per m2 of sand surface. A 0.25 m2 drum gives about 3-7 L/hour.
            • Keep the raw water inlet covered. Sunlight grows algae and clogs the top layer.

            RUNNING IT
            • Wet it slowly, then run to waste for the first week until the outflow clears.
            • The schmutzdecke (biological layer) forms in 1-3 weeks and is what removes pathogens. Do not scrape it early.
            • When flow drops below about 0.1 L/min, drain 2-3 cm of sand, wash it, and refill.
            • This is not a steriliser: follow with boiling, chlorine or SODIS for drinking water.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "aid_wound_infection",
        category = KnowledgeCategory.FIRST_AID,
        title = "Wound care and recognising infection",
        summary = "Cleaning, dressing and the signs that mean the wound needs more than field care.",
        keywords = listOf("wound", "infection", "cut", "dressing", "pus", "tetanus", "first aid"),
        body = """
            CLEANING (the step that matters most)
            • Irrigate with clean running water under pressure for 1-2 minutes. Pressure removes dirt that wiping cannot.
            • Boiled and cooled water or 0.9% saline. Do not pour alcohol or hydrogen peroxide into an open wound; both damage tissue.
            • Remove visible debris with clean tweezers. Do not dig for deeply embedded material.
            • Apply a thin layer of petroleum jelly to the dressing, not the wound, so it does not stick.

            DRESSING
            • Cover with a clean, dry, non-fluffy dressing. Change daily, and immediately if wet or dirty.
            • Leave a clean surgical wound alone for 48 hours if it is not soaking through.
            • Immobilise and elevate a limb wound for the first 24 hours to limit swelling.

            INFECTION: THE THRESHOLD TO ESCALATE
            • Spreading redness beyond 1 cm from the edge, increasing pain after day 2, heat, foul smell, pus, fever above 38 C, or red streaks running toward the body.
            • Those signs need antibiotics and assessment. A wound that is only dirty and painful is not automatically infected.
            • Any deep, dirty or bite wound needs tetanus cover; the disease is not treatable once it starts.
            • Bites from humans, cats and dogs: irrigate hard and seek care. Cat bites inoculate deep and infect often.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "aid_burns",
        category = KnowledgeCategory.FIRST_AID,
        title = "Burn depth, area and field treatment",
        summary = "Cooling, the rule of nines, and which burns must not be dressed in the field.",
        keywords = listOf("burn", "scald", "rule of nines", "blister", "first aid", "cooling"),
        body = """
            IMMEDIATE
            • Cool with running water at about 15-25 C for 20 minutes. This is the single intervention that limits depth, and it still helps up to 3 hours after the burn.
            • Remove rings, watches and clothing that are not stuck to the skin before swelling starts.
            • Do not use ice, butter, toothpaste or oil. Cover with cling film laid on, not wrapped tight.

            DEPTH
            • Superficial: red, dry, painful, no blisters. Heals in about a week.
            • Partial thickness: blisters, wet, very painful. 2-3 weeks, scarring possible.
            • Full thickness: white, brown or charred, leathery, and often painless because the nerves are destroyed.

            AREA: RULE OF NINES (adults)
            • Each arm 9%, each leg 18%, front trunk 18%, back trunk 18%, head 9%, groin 1%.
            • The palm of the patient's hand, fingers included, is about 1%.
            • Escalate: partial thickness over 10% of body area, any full thickness burn, burns to face, hands, feet, joints or genitals, electrical and chemical burns, and any burn with inhaled smoke (singed nose hairs, hoarse voice, soot in the mouth).

            FIELD DRESSING
            • Clean linen or sterile gauze, lightly moistened with cooled boiled water if sticking.
            • Give fluids by mouth if the patient is awake and not vomiting: small sips, frequently.
            • Keep the patient warm; burned patients lose heat fast through the wound.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "farm_fertility_math",
        category = KnowledgeCategory.FARMING,
        title = "Reading a soil test and correcting pH",
        summary = "Target pH per crop, and how much lime or sulfur actually moves it.",
        keywords = listOf("soil", "ph", "lime", "sulfur", "fertility", "npk", "test"),
        body = """
            TARGET pH BY CROP
            • Most vegetables 6.0-7.0. Potatoes 5.0-5.5 (scab). Blueberries 4.5-5.5. Brassicas 6.5-7.5.
            • Below 5.5, phosphorus locks up and aluminium becomes toxic. Above 7.5, iron, manganese, zinc and boron become unavailable.

            RAISING pH WITH LIME
            • Agricultural lime (calcium carbonate) raises pH slowly, over months. Rough guide for a loam: 1 tonne per hectare raises pH about 0.5.
            • Sandy soil needs less, clay needs more; always go by the soil test recommendation, and split applications.

            LOWERING pH WITH SULFUR
            • Elemental sulfur is converted by soil bacteria, so it needs warm, moist, living soil to work.
            • Rough guide: 100-150 g per m2 lowers pH about 0.5 in loam. It takes 3-6 months.
            • Aluminum sulfate acts faster but adds aluminium; avoid on food beds.

            NPK READING
            • The three numbers are percent N, P2O5 and K2O by weight. A 10-10-10 bag is 10% of each.
            • Nitrogen moves with water and volatilises; split it across the season. Phosphorus and potassium are largely immobile, so work them into the root zone before planting.
            • Legumes fix 50-200 kg N per hectare per year, but only if the right Rhizobium is present in the soil.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "farm_grain_storage",
        category = KnowledgeCategory.FARMING,
        title = "Storing grain and legumes without spoiling",
        summary = "Moisture limits, hermetic storage and the insect control that does not need chemicals.",
        keywords = listOf("grain", "storage", "moisture", "weevil", "hermetic", "legumes", "silo"),
        body = """
            MOISTURE IS THE WHOLE GAME
            • Safe storage moisture: wheat and maize 13%, rice 14%, beans and peas 12-13%, sunflower 9%.
            • Test by weight: weigh a sample, dry it thoroughly in a warm dry place, weigh again. Percent loss approximates moisture content.
            • One percent too wet is enough for mould and mycotoxins. Dry before storage, not during.

            HERMETIC STORAGE
            • An airtight drum, sealed silo or heavy-duty bag stops insect development by removing oxygen.
            • Fill completely to leave minimal headspace. Seal the lid with grease or a gasket.
            • Insects already inside consume oxygen and die; the grain keeps for years if it went in dry.

            TEMPERATURE AND HANDLING
            • Below 20 C insects barely reproduce; below 15 C they stop. Shade and night aeration help.
            • Never mix new grain with old: one infested lot contaminates the store.
            • Clean the store and burn or bury sweepings before filling it. Weevils live in cracks, not in the air.

            WITHOUT CHEMICALS
            • Diatomaceous earth at 0.5-1 g per kg of grain abrades insect cuticle and dehydrates them. Food grade only.
            • Wood ash mixed at 1-2% works in small seed lots but is alkaline and can affect germination.
            • Freeze small seed lots at -18 C for 48 hours to kill all stages, then store dry and sealed.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "power_panel_math",
        category = KnowledgeCategory.POWER,
        title = "Sizing a solar panel and charge controller",
        summary = "From daily watt-hours to panel watts, controller amps and cable size.",
        keywords = listOf("solar", "panel", "charge controller", "mppt", "pwm", "watt", "sizing"),
        body = """
            STEP 1: DAILY LOAD
            • watts x hours = watt-hours (Wh) per day. A 10 W router for 24 h is 240 Wh; a 5 W light for 5 h is 25 Wh.
            • Add them, then divide by 0.8 for inverter and wiring losses.

            STEP 2: PANEL WATTS
            • Panel W = daily Wh / peak sun hours / 0.75.
            • Peak sun hours, not daylight hours: 4-5 h in temperate summer, 2-3 h in winter, 5-6 h near the equator.
            • Example: 300 Wh needs 300 / 4 / 0.75 = 100 W of panel.

            STEP 3: CONTROLLER
            • PWM controller amps = panel watts / battery volts. A 100 W panel on 12 V needs 8.3 A, so a 10 A PWM is the minimum.
            • MPPT delivers roughly 10-30% more in cold or partial-light conditions and accepts a higher panel voltage. Worth it above about 200 W.
            • Always size the controller for the panel's short-circuit current plus 25%.

            STEP 4: WIRE
            • Voltage drop under 3%: for 12 V systems keep runs short and thick. 4 mm2 copper is good for about 20 A at 5 m.
            • Fuse at the battery positive terminal, not at the load: a short in the cable is what starts fires.
            • A panel in shade behaves like a resistor, not a smaller panel. One shadowed cell can halve output; use bypass diodes or split the array.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "power_battery_chem",
        category = KnowledgeCategory.POWER,
        title = "Lead-acid versus LiFePO4 in an off-grid bank",
        summary = "Depth of discharge, charge voltages and the rules that keep each chemistry alive.",
        keywords = listOf("battery", "lifepo4", "lead acid", "agm", "charging", "depth of discharge", "bms"),
        body = """
            USABLE CAPACITY
            • Flooded lead-acid: never below 50% state of charge, ideally 70%. A 100 Ah battery gives about 50 Ah.
            • AGM/gel: 50-60% usable, and they tolerate less overcharge.
            • LiFePO4: 80-90% usable, and it does not care about partial states of charge.
            • Compare on usable watt-hours and cycle life, not on the label.

            CHARGE VOLTAGES (12 V nominal)
            • Flooded: absorption 14.4-14.8 V, float 13.2-13.4 V, equalise 15.5 V occasionally.
            • AGM: absorption 14.4-14.7 V, float 13.5 V. Never equalise.
            • LiFePO4: absorption 14.0-14.6 V, float 13.5 V, no equalisation. Charge at 0 C and above only.
            • Temperature compensation matters for lead-acid: about -30 mV per degree C above 25 C.

            RULES THAT PREVENT EARLY DEATH
            • Do not mix old and new batteries, or different capacities, in one string.
            • Keep all connections equal length and torque them; a loose terminal heats and corrodes.
            • Vent flooded batteries: they release hydrogen while charging. Never charge in a sealed box with electronics.
            • Lithium needs a BMS that can disconnect on over-voltage, under-voltage, over-current and temperature.
            • Store lead-acid charged. A battery left flat for months sulfates and cannot be recovered.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "radio_vhf_basics",
        category = KnowledgeCategory.RADIO,
        title = "VHF/UHF range, antennas and legal bands",
        summary = "Line of sight maths, antenna length and where you may legally transmit.",
        keywords = listOf("radio", "vhf", "uhf", "antenna", "range", "walkie", "license", "pmr"),
        body = """
            RANGE
            • Radio horizon (km) is roughly 4.12 x square root of antenna height in metres, per end.
            • Two 1.5 m hand-helds see about 5 km over flat ground; a 9 m mast raises one end to about 12 km.
            • Power is not the limiting factor: doubling power adds about 40% range, doubling height adds more.
            • Buildings, hills and wet foliage absorb UHF far more than VHF. Higher is better than stronger.

            ANTENNA LENGTH
            • Quarter wave in metres = 75 / frequency in MHz. A 446 MHz quarter wave is 0.168 m.
            • Half wave dipole total length = 143 / MHz. At 446 MHz that is 0.32 m, each leg 0.16 m.
            • A ground plane needs three or four radials at least a quarter wave long, angled down about 45 degrees.

            LEGAL, AND PRACTICAL
            • Licence-free UHF varies by country: PMR446 in Europe, FRS/GMRS in the US. Transmitting elsewhere is illegal and can jam emergency traffic.
            • A licence-free radio on a rooftop antenna usually breaks the rules (fixed antenna, power limits).
            • Keep the antenna vertical for hand-helds: cross-polarisation costs 20 dB, which is most of your range.
            • In an emergency, listening is always legal. Know the local emergency frequency before you need it.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "code_bash_essentials",
        category = KnowledgeCategory.CODING,
        title = "Bash scripting that survives set -e",
        summary = "Quoting, error handling, loops and the pitfalls that silently break scripts.",
        keywords = listOf("bash", "shell", "script", "quoting", "loop", "sh", "automation"),
        body = """
            THE HEADER
            • Start with: set -euo pipefail
            • -e exits on error, -u errors on unset variables, -o pipefail makes a pipeline fail if any part fails.
            • Add IFS=newline and tab when reading file lists so spaces in names do not split fields.

            QUOTING
            • Always quote: "${'$'}var", "${'$'}{arr[@]}", "${'$'}(command)". Unquoted variables undergo word splitting and globbing.
            • Use single quotes when the text must be literal, double quotes when variables must expand.
            • Arithmetic needs double parentheses: total=${'$'}((a + b)).

            STRUCTURE
            • for f in *.txt; do ... done - if there are no matches the literal *.txt is passed; guard with shopt -s nullglob.
            • while read -r line; do ... done < file - -r stops backslash mangling, and reading from the file (not a pipe) keeps variables in scope.
            • Functions: name() { local x="${'$'}1"; ... } and check arguments with ${'$'}{1:?usage: name arg}.

            SAFETY
            • Use mktemp for temporary files and trap 'rm -f "${'$'}tmp"' EXIT.
            • Check before deleting: rm -rf -- "${'$'}{dir:?dir is empty}" refuses to run on an empty variable.
            • Prefer find . -name '*.log' -mtime +7 -delete over shell globs for anything destructive.
        """.trimIndent(),
        command = "printf '%s\\n' \"${'$'}SHELL\" \"${'$'}0\"",
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "code_python_files",
        category = KnowledgeCategory.CODING,
        title = "Python for logs, CSV and batch file work",
        summary = "Reading, filtering and summarising text without a spreadsheet.",
        keywords = listOf("python", "csv", "file", "script", "log", "parse", "offline"),
        body = """
            READ AND FILTER A LOG
            • with open('app.log', errors='replace') as f: then for line in f: if 'ERROR' in line: print(line.rstrip())
            • errors='replace' matters: one bad byte should not abort a whole report.
            • Count instead of printing: from collections import Counter; c = Counter(line.split()[0][:10] for line in f).

            CSV WITHOUT PANDAS
            • import csv, then rows = list(csv.DictReader(open('data.csv', newline='')))
            • Numeric columns arrive as strings. total = sum(float(r['watts']) for r in rows) - convert explicitly.
            • Write with csv.DictWriter and newline='' to avoid blank lines on Windows.

            WALK A TREE
            • from pathlib import Path; for p in Path('.').rglob('*.jpg'): print(p, p.stat().st_size)
            • Sort by size: sorted(paths, key=lambda p: p.stat().st_size, reverse=True)[:20]

            RULES THAT PREVENT DATA LOSS
            • Never write to the file you are reading. Write to name.tmp, then os.replace(tmp, name).
            • Use pathlib and UTF-8 explicitly; locale-encoded text is the usual cause of mojibake.
            • Wrap the entry point in if __name__ == '__main__': so the module can be imported and tested.
        """.trimIndent(),
        command = "python3 -c \"import sys; print(sys.version)\"",
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "hardware_disk_facts",
        category = KnowledgeCategory.HARDWARE,
        title = "Finding what filled the disk",
        summary = "du, df, deleted-but-open files and the inode limit.",
        keywords = listOf("disk", "storage", "du", "df", "inode", "space", "linux"),
        body = """
            THE TWO QUESTIONS
            • df -h answers "which filesystem is full". df -i answers "are we out of inodes", which reports as full while df -h shows space.
            • du -x -h --max-depth=1 / | sort -h shows the biggest directories on one filesystem. -x stops it wandering into mounted disks.

            FIND THE FILES
            • find / -xdev -type f -size +100M -printf '%s %p\\n' 2>/dev/null | sort -n | tail -20
            • Large deleted files still held open by a process do not appear in du but keep the space: lsof +L1, or ls -l /proc/*/fd | grep deleted.

            COMMON CULPRITS ON A GUEST
            • Package caches: apt clean, apk cache clean, pacman -Sc.
            • Logs: journalctl --vacuum-size=100M, or truncate a log with : > file (not rm, which does not free the space while a process holds it).
            • A rootfs unpacked twice: check for both linux/<distro> and linux/<distro>.old.

            BEFORE DELETING
            • Confirm the mount: findmnt -T /path prints the filesystem and device a path really lives on.
            • Prefer moving to a staging directory over rm when unsure; space can be reclaimed later, deleted data cannot.
        """.trimIndent(),
        command = "df -h; df -i",
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "surv_shelter_siting",
        category = KnowledgeCategory.SURVIVAL,
        title = "Siting a shelter that stays dry and warm",
        summary = "Drainage, wind, deadfall risk and the insulation that actually matters.",
        keywords = listOf("shelter", "tent", "camp", "drainage", "wind", "insulation", "survival"),
        body = """
            SITE SELECTION, IN ORDER
            • Above the valley floor but off the ridge: cold air pools low, wind and lightning hit high.
            • Look up before you pitch: dead branches overhead are the most common camping injury.
            • Check drainage: a dry channel is dry until it rains. Dig a shallow trench uphill of the tent, or pick higher ground.
            • Water within 100 m but not within 30 m: convenience versus insects, flooding and contamination.

            ORIENTATION
            • Door away from prevailing wind, and away from the fire.
            • In the northern hemisphere a south-facing entrance gets morning sun and dries the shelter.
            • Use a natural windbreak (rock, dense scrub) rather than building one from cut vegetation.

            INSULATION: THE GROUND
            • More heat is lost downward than through the walls. 10 cm of dry leaf litter, bracken or pine boughs under the sleeping area beats a thicker blanket.
            • Never sleep directly on bare ground, stone or concrete.

            FIRE SAFETY
            • 3 m clearance, no overhanging branches, cleared ground within 1 m, water or sand beside it.
            • A fire inside a closed shelter is a carbon monoxide death. Ventilate, or keep it outside.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),
    KnowledgeDoc(
        id = "surv_fire_wet",
        category = KnowledgeCategory.SURVIVAL,
        title = "Fire in wet conditions",
        summary = "Finding dry fuel, feather sticks and the fire lays that survive rain.",
        keywords = listOf("fire", "wet", "tinder", "feather stick", "ferro rod", "survival", "rain"),
        body = """
            DRY WOOD EXISTS IN THE RAIN
            • Standing dead branches, the underside of fallen logs, and the inner wood of a wet branch are dry.
            • Split wet wood: the inside is dry. A knife and a baton get you there without an axe.
            • Look under dense conifers and inside hollow trees. Birch bark burns wet because of its oils.

            PREPARATION IS THE FIRE
            • Feather sticks: shave curls that stay attached, so the shavings cannot blow away and the stick becomes the match.
            • Three sizes: tinder (fluff, birch bark, charred cloth), kindling (pencil to finger thickness), fuel (wrist thickness).
            • Gather twice what you think you need before you strike anything.

            IGNITION WITHOUT MATCHES
            • Ferro rod: scrape the coating off, hold the striker still and pull the rod back, aiming sparks into the tinder.
            • A 9 V battery and steel wool is a reliable last resort if the wool is dry.
            • Charred cotton or punk wood takes a spark even damp; carry it in a sealed bag.

            FIRE LAYS
            • Teepee for quick heat and light, log cabin for a stable cooking bed, platform fire on wet ground (green logs as a base) to keep the fuel off the mud.
            • Build the fire on a platform of green wood or flat stones when the ground is soaked; a fire sunk in mud loses its heat to water.
        """.trimIndent(),
        tier = KnowledgeTier.MEDIUM
    ),

    // =====================================================================
    // LARGE TIER
    // =====================================================================
    KnowledgeDoc(
        id = "water_irrigation_drip",
        category = KnowledgeCategory.WATER,
        title = "Designing a gravity drip irrigation system",
        summary = "Head pressure, emitter counts and pipe sizing for a tank-fed drip line.",
        keywords = listOf("irrigation", "drip", "gravity", "emitter", "water", "garden", "pressure"),
        body = """
            PRESSURE FROM HEIGHT
            • 1 m of head = 0.098 bar = about 9.8 kPa. A tank 2 m above the beds gives 0.2 bar.
            • Most drip tape wants 0.5-1.0 bar, so gravity alone suits low-pressure tape and soaker hose, not standard drip.
            • Raise the tank, or use short runs with low-flow emitters (2 L/h), which work at 0.2-0.3 bar.

            FLOW MATH
            • Total demand = number of emitters x emitter flow. 100 emitters at 2 L/h = 200 L/h.
            • A 20 mm (3/4 inch) main carries about 1,000-1,500 L/h at low pressure over 30 m; a 13 mm (1/2 inch) line about half that.
            • Keep each lateral under 30 m; friction loss grows with length and with the square of flow.

            LAYOUT
            • Emitters every 30 cm on vegetables, 50-60 cm on tomatoes, 20 cm on salad greens.
            • Run laterals along the contour, not down the slope, so the low end does not get everything.
            • One filter (120 mesh) at the tank outlet. Drip clogging is the usual failure, and it is always debris or algae.

            SCHEDULE
            • Water to field capacity in the root zone, then let the top 2-3 cm dry: roots need air.
            • Early morning watering loses least to evaporation and lets foliage dry before night, which limits fungal disease.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "water_solar_still",
        category = KnowledgeCategory.WATER,
        title = "Solar still and transpiration bag yields",
        summary = "What these devices really produce, and when they are worth the effort.",
        keywords = listOf("solar still", "distill", "transpiration", "survival", "water", "condense"),
        body = """
            REALISTIC OUTPUT
            • A pit solar still (1 m across): 0.5-1.5 L per sunny day. Setup cost is an hour of digging.
            • A transpiration bag on a leafy branch: 0.2-0.5 L per day, and it kills the branch within days.
            • Neither will keep a group alive. They buy time and morale, and they produce genuinely sterile water.

            BUILDING THE PIT STILL
            • Dig 1 m wide, 0.6 m deep, in full sun. Put a container in the centre.
            • Add green vegetation around the container - not in it - for extra moisture.
            • Cover with clear plastic, seal the edges with soil, and weight the centre so it slopes to a point above the cup.
            • A hose from the cup to the edge lets you drink without dismantling it.

            WHY IT IS SLOW
            • Yield is limited by solar energy and by how much water the soil can give up, not by the size of the hole.
            • The still distils: it removes salt and microbes but not volatile chemicals (fuel, pesticide). Do not use contaminated ground.
            • Urine in the pit adds ammonia and does not increase potable yield. Skip it.

            BETTER USE OF THE SAME EFFORT
            • A clear plastic bag over leafy non-toxic vegetation, or a covered trench of wet cloth in the sun, gives similar water for a fraction of the labour.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "farm_seed_viability",
        category = KnowledgeCategory.FARMING,
        title = "Seed viability, storage life and germination testing",
        summary = "How long each family keeps, the float test, and a real germination protocol.",
        keywords = listOf("seed", "viability", "germination", "storage", "vault", "heirloom", "test"),
        body = """
            STORAGE LIFE (cool, dry, dark, sealed)
            • 1-2 years: onions, leeks, parsnips, sweet corn.
            • 3-4 years: beans, peas, carrots, peppers, spinach.
            • 5+ years: tomatoes, brassicas, cucumbers, squash, radish, beetroot.
            • Rule of thumb: every 10 C cooler and every 1% drier roughly doubles storage life. Silica gel in a sealed jar is enough for household quantities.

            GERMINATION TEST (do this before planting a whole field)
            • Count 10 seeds. Place on damp paper towel, in a sealed bag, at 20-25 C, out of light.
            • Check daily from day 5; count seedlings with a visible root at day 14 (21 for peppers and parsnips).
            • 8/10 or better: plant normally. 5-7/10: sow double density. Under 5/10: use for sprouts or discard.

            FLOAT TEST IS NOT RELIABLE
            • It only detects seeds that are hollow or already waterlogged. Many dead seeds sink, and some viable ones float.
            • The germination test is the only answer worth trusting.

            SAVING YOUR OWN
            • Only open-pollinated varieties breed true. F1 hybrids segregate and disappoint.
            • Keep isolation distance: 3 m for self-pollinating beans and tomatoes, 500 m+ for wind-pollinated corn and beets, and 1 km+ for brassicas.
            • Ferment tomato seed 3 days to remove the germination-inhibiting gel, then rinse and dry on glass, never on paper towel.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "farm_animal_water",
        category = KnowledgeCategory.FARMING,
        title = "Livestock feed and water requirements",
        summary = "Daily water, feed conversion and the numbers behind a small holding.",
        keywords = listOf("livestock", "water", "feed", "goat", "chicken", "rabbit", "conversion"),
        body = """
            DAILY WATER PER ANIMAL (litres, temperate climate)
            • Chicken 0.25-0.5, rabbit 0.3-0.6, goat 4-10, sheep 3-6, pig 10-20, dairy cow 40-80.
            • Heat roughly doubles intake. A goat at 35 C can drink 15 L. Water shortage stops milk and growth before feed shortage does.

            FEED CONVERSION (kg feed per kg gain)
            • Chicken 1.6-2.0, rabbit 3.0-4.0, pig 3.0-3.5, sheep 4.0-5.0, beef cattle 6.0-8.0.
            • Rabbits and chickens are the most efficient small-scale protein; a rabbit doe with kits can produce 20-30 kg of meat a year.

            ROUGH DAILY FEED
            • Laying hen 110-130 g layer ration plus grit and calcium (oyster shell).
            • Goat 1.5-2.5 kg dry matter, roughly 3-5% of body weight; browse counts, but not as a full ration.
            • Rabbit 100-150 g pellets plus unlimited hay. Hay is the gut, pellets are the supplement.

            HEALTH RULES THAT PREVENT LOSSES
            • Clean water in a raised container, emptied daily; wet bedding is where coccidiosis starts.
            • Quarantine any new animal for 3-4 weeks before it meets the herd.
            • Rotate grazing: larvae reinfect within days on the same small paddock. Rest a paddock 30+ days.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "aid_triage_mass",
        category = KnowledgeCategory.FIRST_AID,
        title = "Mass-casualty triage in four categories",
        summary = "The 30-second assessment that decides who is treated first.",
        keywords = listOf("triage", "mass casualty", "disaster", "first aid", "emergency", "tag"),
        body = """
            THE FOUR CATEGORIES
            • Immediate (red): life threat fixable now - severe bleeding, blocked airway, tension pneumothorax, shock.
            • Delayed (yellow): serious but stable - fractures, moderate blood loss, burns under 20%.
            • Minimal (green): walking wounded - sprains, small wounds, minor burns. They can help others.
            • Expectant (black): no breathing after opening the airway, or injuries incompatible with survival given the resources available.

            THE 30-SECOND SWEEP
            • Can they walk? If yes, green, unless obviously otherwise.
            • Breathing? Open the airway. Not breathing after that: black.
            • Breathing rate above 30 or below 10, or capillary refill over 2 seconds, or unable to follow commands: red.
            • Otherwise yellow.

            WHAT CHANGES UNDER PRESSURE
            • Stop the bleeding before moving anyone. Direct pressure, then a tourniquet high and tight on a limb that will otherwise kill them.
            • Do not spend time on a casualty you cannot save while a red casualty waits. That is the entire point of triage.
            • Re-triage every 15-30 minutes: yellow becomes red quietly.

            DOCUMENTATION
            • Write the category, time and what was done on the patient's forehead or a tied tag. Verbal handover loses information.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "power_microhydro",
        category = KnowledgeCategory.POWER,
        title = "Micro-hydro output and intake basics",
        summary = "The power equation, and why head beats flow for a small turbine.",
        keywords = listOf("hydro", "micro hydro", "turbine", "head", "flow", "power", "water wheel"),
        body = """
            THE EQUATION
            • Power (W) = head (m) x flow (litres/second) x 9.81 x efficiency.
            • Small pelton or turgo systems achieve 50-70%; undershot water wheels 20-30%.
            • 10 m head and 5 L/s at 60% gives 10 x 5 x 9.81 x 0.6 = 294 W, day and night.

            MEASURING THE SITE
            • Head: fill a hose with water, hold one end at the intake and the other at the turbine site; the height difference between the two water surfaces is the head. A pressure gauge reads head directly: 1 bar is about 10.2 m.
            • Flow: time how long a bucket of known volume takes to fill, or use a V-notch weir for larger streams. Measure in the dry season, not after rain.
            • Penstock: bigger is better. Friction loss should stay under 10% of head; a 50 mm pipe carries about 5-8 L/s over 100 m without losing too much.

            PRACTICALITIES
            • Debris screen and a settling basin before the intake. Grit destroys runners.
            • Keep the turbine out of flood level, and provide a bypass so the stream is not dammed in a spate.
            • Induction motors can be used as generators with capacitors, but matching load to available power needs a controller or a dump load (water heater) to prevent overspeed.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "power_wind_small",
        category = KnowledgeCategory.POWER,
        title = "Small wind: the Betz limit and why height wins",
        summary = "Realistic output from a small turbine, tower height and furling.",
        keywords = listOf("wind", "turbine", "betz", "tower", "power", "blade", "off grid"),
        body = """
            THE PHYSICS CEILING
            • Power in the wind = 0.5 x air density x swept area x wind speed cubed.
            • Betz limit: no turbine extracts more than 59.3% of that. Real small turbines reach 25-35%.
            • A 1 m radius rotor in 5 m/s wind: 0.5 x 1.225 x 3.14 x 125 = 240 W available, so maybe 70-90 W at the shaft.

            SPEED CUBED MEANS SITE QUALITY IS EVERYTHING
            • Doubling wind speed gives eight times the power. Moving from 4 m/s to 6 m/s more than triples output.
            • Ground roughness matters: at 10 m the wind is roughly half the gradient wind; at 30 m it is most of it.
            • A tower of at least 10 m, and 10 m above anything within 100 m, is the usual minimum for useful output.

            PRACTICAL DESIGN
            • Three blades, tip speed ratio 6-7, direct drive permanent-magnet alternator avoids gearbox losses.
            • Furling (tilting the rotor out of the wind) or an electronic dump load must protect against overspeed in a storm.
            • Rectify three-phase AC to DC, then charge the battery through a controller that can divert surplus to a heater. A turbine with no dump load will burn out its stator on a windy, low-load day.

            EXPECTATIONS
            • A well-sited 400 W turbine produces roughly 40-80 kWh a month in a 5 m/s site - useful for lighting and radio, not for heating.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "radio_packet_aprs",
        category = KnowledgeCategory.RADIO,
        title = "Packet radio and APRS without infrastructure",
        summary = "1200 baud AFSK, digipeaters and sending position or short messages over VHF.",
        keywords = listOf("packet", "aprs", "ax.25", "digipeater", "vhf", "data", "radio"),
        body = """
            HOW IT WORKS
            • AX.25 packets at 1200 baud AFSK on 144.800 MHz (Europe) or 144.390 MHz (North America) for APRS.
            • Each packet has a source, destination, digipeater path, and up to 256 bytes of payload.
            • A digipeater repeats packets, extending range hop by hop; WIDE1-1,WIDE2-1 is the usual path.

            WHY IT SURVIVES
            • Text messages get through where voice does not, because a packet is error-checked and can be repeated.
            • A station with a 5 W hand-held and a quarter-wave antenna can reach a digipeater 10-20 km away on a hilltop.

            SENDING WITHOUT A NETWORK
            • Two radios and a TNC (terminal node controller) or a sound-card interface plus Direwolf on a laptop form a link.
            • A local "message" to a callsign arrives even if no gateway is reachable: the other station acknowledges it.
            • Position beacons every 1-3 minutes are enough for tracking, and keep the channel free.

            SETUP NOTES
            • Audio levels matter more than power: too hot and the packet distorts, too low and it is never decoded. Adjust for a clean 1200 Hz/2200 Hz tone pair.
            • Never run a digipeater without permission on a shared frequency; incorrect paths create loops that jam the channel.
            • Keep a written log of callsigns and times. In a real emergency that log is the network.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "radio_dipole_build",
        category = KnowledgeCategory.RADIO,
        title = "Cutting and tuning a wire dipole",
        summary = "Length formulas, feed line, and how to trim for minimum SWR.",
        keywords = listOf("dipole", "antenna", "swr", "coax", "balun", "wire", "tune"),
        body = """
            LENGTH
            • Half-wave dipole, total length in metres = 143 / frequency in MHz.
            • 7.1 MHz gives 20.1 m total, so 10.05 m per leg. Cut 2% long and trim.
            • Inverted-V: same wire, apex at 8-10 m, ends 2 m above ground, about 90-120 degrees between legs.

            FEED AND MATCHING
            • 50 ohm coax to the centre. At the feed point a dipole is about 73 ohm, giving SWR near 1.5:1 without a balun.
            • A 1:1 current balun at the feed point stops the coax shield radiating and keeps the pattern predictable.
            • Height above ground changes the feed impedance; a low dipole may read 30-40 ohm.

            TUNING
            • Measure SWR at the band edges and at resonance. Minimum SWR is resonance.
            • Too low in frequency: shorten both legs equally, 1-2 cm at a time.
            • Too high: lengthen. Fold the ends back rather than soldering new wire on; folding is reversible.
            • Insulators at both ends, and rope that will not absorb water. Wet rope changes the length and the tuning.

            PRACTICAL
            • Higher is better for distance; 10 m up gives a low take-off angle.
            • Keep the wire away from metal roofs and gutters by at least a quarter wave where possible.
            • A dipole cut for 40 m also loads on 15 m in most cases; check SWR before transmitting.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "code_c_pointers",
        category = KnowledgeCategory.CODING,
        title = "C: compiling, pointers and memory mistakes",
        summary = "gcc flags, pointer arithmetic and the bugs that crash without a message.",
        keywords = listOf("c", "gcc", "pointer", "memory", "malloc", "compile", "debug"),
        body = """
            BUILDING
            • gcc -std=c11 -Wall -Wextra -O2 -o prog prog.c
            • Add -g for debug symbols and -fsanitize=address,undefined while testing; the sanitisers catch most memory bugs at runtime.
            • valgrind --leak-check=full ./prog when a leak is suspected.

            POINTERS IN ONE PAGE
            • A pointer holds an address. *p reads the value there, &x takes the address of x.
            • Arrays decay to a pointer to the first element: a[i] is *(a + i).
            • Pointer arithmetic is scaled by the pointed-to type, so p + 1 moves sizeof(T) bytes.
            • NULL is not a valid address; dereferencing it is undefined behaviour, not a guaranteed crash.

            MEMORY
            • malloc returns void*; check for NULL. calloc zeroes. realloc may move the block, so keep the returned pointer.
            • free(NULL) is safe. Freeing twice, or using after free, is undefined and often silent.
            • Every malloc needs exactly one free on every exit path. A single cleanup label (goto out) is legitimate C.

            STRINGS AND BUFFERS
            • strncpy does not guarantee a terminator; snprintf(dst, sizeof dst, "%s", src) is the safer copy.
            • Buffer overruns are the classic remote-exploitable bug: always pass the destination size.
            • sizeof(arr) in a function parameter is sizeof(pointer), a classic silent bug. Pass the length explicitly.
        """.trimIndent(),
        command = "gcc --version | head -1",
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "code_algorithms_table",
        category = KnowledgeCategory.CODING,
        title = "Algorithm complexity you can act on",
        summary = "Which structure for which job, with the costs that matter at small scale.",
        keywords = listOf("algorithm", "complexity", "sort", "hash", "tree", "big o", "data structure"),
        body = """
            CHOOSING A STRUCTURE
            • Need lookup by key: hash map, O(1) average, O(n) worst case. In Python dict, in C a hand-written open-addressing table.
            • Need order: balanced tree (Red-black/B-tree) O(log n) for insert, delete and lookup. A sorted array is fine under a few thousand items.
            • Need priority: binary heap, O(log n) push and pop, O(1) peek.
            • Need range queries: prefix sums for static data, segment tree for updates.

            SORTING
            • Comparison sorts bottom out at O(n log n). Quicksort average n log n, worst n squared on adversarial input; merge sort is stable and predictable.
            • Counting sort is O(n + k) when values are small integers - the right answer for ages, bytes or small ranges.
            • For under about 50 items, insertion sort usually beats quicksort in practice because of constant factors.

            GRAPHS
            • BFS for fewest hops in an unweighted graph, O(V + E). DFS for connectivity and cycle detection.
            • Dijkstra with a heap, O((V + E) log V), requires non-negative weights. Bellman-Ford handles negative edges, O(VE).
            • Topological sort (Kahn) for dependencies; a cycle means the task order is impossible.

            THE PRACTICAL RULE
            • n = 1,000: almost anything works. n = 1,000,000: n squared is a minute, n log n is a second. Measure before optimising.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "hardware_kernel_probe",
        category = KnowledgeCategory.HARDWARE,
        title = "Reading a Linux system from /proc and dmesg",
        summary = "The files that answer what the kernel is actually doing.",
        keywords = listOf("linux", "kernel", "proc", "dmesg", "module", "hardware", "diagnose"),
        body = """
            WHAT THE KERNEL SAYS
            • dmesg | tail -50: driver errors, USB events, out-of-memory kills, filesystem errors. Usually the first place to look.
            • dmesg -T gives human timestamps; -w follows live.
            • /proc/cmdline: the boot parameters actually used, not the ones in a config file.

            HARDWARE
            • /proc/cpuinfo: model, flags, core count. lscpu summarises it.
            • /proc/meminfo: MemAvailable is the number that matters, not MemFree.
            • /proc/interrupts: which device is hammering the CPU.
            • /sys/class/thermal/thermal_zone*/temp: millidegrees C.
            • lsblk -f: block devices, filesystems and UUIDs. blkid for UUIDs alone.

            PROCESSES
            • /proc/<pid>/cmdline: the exact argv, NUL-separated. tr '\\0' ' ' < /proc/1/cmdline reads it.
            • /proc/<pid>/status: VmRSS, Threads, State. /proc/<pid>/fd lists open files.
            • cat /proc/loadavg: 1, 5 and 15 minute averages plus running/total threads.

            WHEN SOMETHING DIED
            • OOM kill: grep -i 'killed process' in dmesg, and check /proc/vmstat for oom_kill.
            • A process killed by signal 9 usually means the OOM killer or an external supervisor.
            • Filesystem errors: dmesg | grep -i ext4, then fsck only from a state where the filesystem is not mounted.
        """.trimIndent(),
        command = "dmesg | tail -20",
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "hardware_network_debug",
        category = KnowledgeCategory.HARDWARE,
        title = "Network diagnosis with ip, ss and dig",
        summary = "From 'no route to host' to a listening socket, step by step.",
        keywords = listOf("network", "ip", "ss", "dig", "dns", "route", "tcp", "debug"),
        body = """
            LAYER BY LAYER
            • Link: ip link shows the interface up and the MAC. A down interface explains everything above it.
            • Address: ip -4 addr. No address means no DHCP lease; check the DHCP client logs.
            • Route: ip route. A missing default route means nothing beyond the local subnet.
            • DNS: cat /etc/resolv.conf, then dig example.com @1.1.1.1 to bypass local resolvers.

            IS ANYTHING LISTENING
            • ss -tulpn: TCP and UDP listeners with the owning process. A service that is "running" but not listening is a config error, not a network one.
            • curl -v http://127.0.0.1:8080/health separates "myself" from "the network".
            • ss -tan state established shows live connections; -s gives a summary.

            REACHABILITY
            • ping tests ICMP, which many networks block; a failed ping is not proof of a dead host.
            • nc -vz host port tests a TCP port directly. timeouts mean filtered, refused means nothing listening.
            • traceroute (or mtr) shows where the path stops. A path that ends at your own gateway is a local problem.

            THE COMMON CAUSES
            • Wrong subnet mask or gateway, captive portal blocking DNS, firewall rule ordering, or a service bound to 127.0.0.1 when it should bind 0.0.0.0.
        """.trimIndent(),
        command = "ip -4 addr; ip route",
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "hardware_solder_basics",
        category = KnowledgeCategory.HARDWARE,
        title = "Soldering and rework that holds",
        summary = "Temperatures, flux, joints and removing components without lifting pads.",
        keywords = listOf("solder", "iron", "flux", "smd", "rework", "electronics", "repair"),
        body = """
            TEMPERATURE
            • Leaded solder (Sn63/Pb37): iron at 320-350 C, melts at 183 C.
            • Lead-free (SAC305): iron at 350-380 C, melts at 217-220 C. Higher heat for shorter time.
            • Never leave the iron above 400 C on a pad; FR4 delaminates and pads lift.

            THE JOINT
            • Heat the pad and the lead together for 1-2 seconds, then feed solder into the joint, not onto the iron.
            • A good joint is shiny, concave and wets both surfaces. A ball sitting on top is a cold joint.
            • Flux is not optional on oxidised or reused boards. Rosin flux pen, clean with isopropyl afterwards.

            THROUGH-HOLE REMOVAL
            • Add fresh solder first: it lowers the melting point of the old joint.
            • Use solder wick with flux, or a heated vacuum desoldering tool. A manual pump alone usually leaves enough to tear a pad.
            • Wiggle each lead while heating; if it does not move, it is not fully melted.

            SMD AND REWORK
            • Hot air at 300-350 C with 20-30% airflow, preheat the board to 100-150 C to reduce thermal shock.
            • Tweezers, flux, and patience. Do not pull a component before the solder is liquid.
            • For a shorted capacitor, removing it and re-testing is faster than guessing.

            SAFETY
            • Fume extraction or an open window. Rosin smoke is a respiratory irritant.
            • Lead-free solder still contains no lead, but old boards do; wash hands, do not eat at the bench.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "surv_water_navigation",
        category = KnowledgeCategory.SURVIVAL,
        title = "Finding water in dry country",
        summary = "Where water hides, and how to read terrain and animals for it.",
        keywords = listOf("water", "desert", "dry", "navigation", "survival", "terrain", "spring"),
        body = """
            READ THE LAND
            • Water collects where the land converges: the lowest point of a valley, the outside of a bend, the base of a cliff.
            • Dry waterfalls and plunge pools often hold water in rock basins for weeks after rain.
            • Look for a line of green vegetation denser than its surroundings; willows, reeds and sycamores track subsurface water.

            ANIMALS AND INSECTS
            • Bees and ants travel toward water in the morning and back in the evening. Their line is usually short.
            • Birds flying straight and low at dusk are heading to water; grain eaters stay within a few kilometres of it.
            • Animal trails that converge from several directions usually meet at water.

            DIGGING
            • In a dry riverbed, dig at the outside of the last bend, or where the bed narrows. Water sits under the gravel, not in the sand at the surface.
            • Dig a sump 50-80 cm deep and wait; it fills from the sides. Bail the first muddy litres.
            • A solar still buys little, but a covered pit of wet cloth can condense a litre a day.

            USING IT
            • Assume all surface water is contaminated. Filter the silt through cloth, then boil, chlorinate or use SODIS.
            • In heat, drink small amounts often. Rationing water while walking is how people die of heat stroke, not of thirst.
            • Urine is not drinkable. Neither is seawater, without distillation.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    ),
    KnowledgeDoc(
        id = "farm_soil_structure",
        category = KnowledgeCategory.FARMING,
        title = "Soil structure, tilth and no-dig beds",
        summary = "Aggregates, organic matter and why compaction ruins yields.",
        keywords = listOf("soil", "structure", "compaction", "tilth", "no dig", "mulch", "organic"),
        body = """
            WHAT GOOD STRUCTURE LOOKS LIKE
            • Crumbs the size of a pea, dark colour, earthworm casts, water that soaks in within a minute.
            • A slake test: drop a dry clod into water. Good soil holds together; poor soil disintegrates into mud.
            • Organic matter target 4-6%. Each 1% holds roughly 150,000 L of water per hectare.

            COMPACTION
            • Caused by walking on wet beds, machinery, and ploughing at the same depth (a hardpan).
            • Test with a wire flag or a penetrometer: resistance at 15-20 cm is a pan. Break it with a broadfork, deep-rooted crops (daikon, alfalfa) or one deep ripping, not annual digging.

            NO-DIG BEDS
            • 10-20 cm compost on top of cardboard over weeds, never inverted soil.
            • Mulch 5-8 cm to hold moisture and stop capping; leave it off in early spring so the soil warms.
            • Walk only on paths. A bed 1.2 m wide means you never step on the growing area.

            WATER AND AIR
            • Roots need oxygen: waterlogged soil suffocates them within days. Raised beds and drainage matter more than fertilizer.
            • Water slowly and deeply, then let the surface dry. Shallow daily watering grows shallow roots.
        """.trimIndent(),
        tier = KnowledgeTier.LARGE
    )
)
