package com.example.model

/**
 * Games that a customer can actually get onto the unit today.
 *
 * Two rules for this list:
 *  - Every package id below was checked against the Play Store listing (HTTP 200) before it was
 *    written down. No invented titles, no invented frame rates.
 *  - Phonix bundles no games and no ROMs. It links to the store listing, the store installs it,
 *    Android runs it. A title is only marked "installed" when the package manager says so.
 *
 * Free/open-source titles come first because they match the product's no-ads, no-subscription
 * pitch and because their licences are visible.
 */
enum class GameKind { GAME, EMULATOR }

data class GameEntry(
    val packageId: String,
    val title: String,
    val genre: String,
    val blurb: String,
    val licence: String,
    val price: String,
    val kind: GameKind = GameKind.GAME,
    val fdroidUrl: String? = null,
    val directUrl: String? = null
)

/**
 * Browser games need no install and no licence question: they are ordinary web pages, which is
 * also the honest answer for a machine whose GPU budget is a recycled phone.
 */
data class BrowserGame(val title: String, val url: String, val blurb: String)

object GameCatalog {

    val games: List<GameEntry> = listOf(
        GameEntry(
            packageId = "io.anuke.mindustry",
            title = "Mindustry",
            genre = "Automation / tower defence",
            blurb = "Conveyor-fed factories, turrets and waves. Mouse-driven, so it suits a desk " +
                "better than a phone. Runs natively on Android — no Linux layer involved.",
            licence = "GPL-3.0",
            price = "Free, open source",
            fdroidUrl = "https://f-droid.org/packages/io.anuke.mindustry/",
            directUrl = "https://github.com/Anuken/Mindustry/releases"
        ),
        GameEntry(
            packageId = "com.shatteredpixel.shatteredpixeldungeon",
            title = "Shattered Pixel Dungeon",
            genre = "Roguelike",
            blurb = "Turn-based, one click at a time, and short sessions. The least demanding " +
                "game on this list on the battery and the GPU.",
            licence = "GPL-3.0",
            price = "Free, open source",
            fdroidUrl = "https://f-droid.org/packages/com.shatteredpixel.shatteredpixeldungeon/",
            directUrl = "https://github.com/00-Evan/shattered-pixel-dungeon/releases"
        ),
        GameEntry(
            packageId = "com.unciv.app",
            title = "Unciv",
            genre = "4X strategy",
            blurb = "Open-source Civilization-style empire building. Long games, low hardware " +
                "demand, and it reads well on a 1080p monitor.",
            licence = "MPL-2.0",
            price = "Free, open source",
            fdroidUrl = "https://f-droid.org/packages/com.unciv.app/",
            directUrl = "https://github.com/yairm210/Unciv/releases"
        ),
        GameEntry(
            packageId = "org.openttd.sdl",
            title = "OpenTTD",
            genre = "Transport simulation",
            blurb = "The open-source Transport Tycoon Deluxe. Built for a mouse and a big screen, " +
                "which is exactly what this machine has.",
            licence = "GPL-2.0",
            price = "Free, open source",
            directUrl = "https://github.com/OpenTTD/OpenTTD/releases"
        ),
        GameEntry(
            packageId = "org.supertuxkart.stk",
            title = "SuperTuxKart",
            genre = "Kart racer",
            blurb = "Open-source kart racing. Comfortable with a gamepad; keyboard works too. " +
                "This is the heaviest 3D title here.",
            licence = "GPL-3.0",
            price = "Free, open source",
            directUrl = "https://supertuxkart.net/Downloads"
        ),
        GameEntry(
            packageId = "com.yodo1.crossyroad",
            title = "Crossy Road",
            genre = "Arcade",
            blurb = "Endless hopper with one-button controls. Free with advertisements and " +
                "in-app purchases.",
            licence = "Proprietary",
            price = "Free with ads"
        ),
        GameEntry(
            packageId = "com.innersloth.spacemafia",
            title = "Among Us",
            genre = "Social deduction",
            blurb = "Free on mobile with ads. Multiplayer needs a network connection, so it is " +
                "one of the few things here that uses the Wi-Fi.",
            licence = "Proprietary",
            price = "Free with ads"
        ),
        GameEntry(
            packageId = "com.tgc.sky.android",
            title = "Sky: Children of the Light",
            genre = "Adventure",
            blurb = "Free exploration game with a large download. Designed around touch and a " +
                "controller; a mouse is workable but not its native input.",
            licence = "Proprietary",
            price = "Free"
        ),
        GameEntry(
            packageId = "com.supercell.brawlstars",
            title = "Brawl Stars",
            genre = "Arena brawler",
            blurb = "Free with in-app purchases. A touch game first: on DeX a mouse stands in for " +
                "a finger, and no gamepad is supported.",
            licence = "Proprietary",
            price = "Free with in-app purchases"
        )
    )

    val emulators: List<GameEntry> = listOf(
        GameEntry(
            packageId = "com.retroarch",
            title = "RetroArch",
            genre = "Multi-system emulator",
            blurb = "Frontend for many classic consoles. Ships no games: use your own cartridges' " +
                "dumps or freely licensed homebrew. Save states and shaders work as they normally do.",
            licence = "GPL-3.0",
            price = "Free, open source",
            kind = GameKind.EMULATOR,
            fdroidUrl = "https://f-droid.org/packages/com.retroarch/",
            directUrl = "https://buildbot.libretro.com/stable/"
        ),
        GameEntry(
            packageId = "org.ppsspp.ppsspp",
            title = "PPSSPP",
            genre = "PlayStation Portable emulator",
            blurb = "Runs PSP titles from disc images you own. The free build is the open-source " +
                "one; there is also a paid build in the same store.",
            licence = "GPL-2.0-or-later",
            price = "Free, open source",
            kind = GameKind.EMULATOR,
            directUrl = "https://www.ppsspp.org/download/"
        )
    )

    val browserGames: List<BrowserGame> = listOf(
        BrowserGame(
            title = "2048",
            url = "https://play2048.co/",
            blurb = "Sliding-tile puzzle. Instant, no account, works with arrow keys."
        ),
        BrowserGame(
            title = "A Dark Room",
            url = "https://adarkroom.doublespeakgames.com/",
            blurb = "Text-driven survival game that grows into a strategy game. Free, and light " +
                "enough for the phone's browser."
        ),
        BrowserGame(
            title = "itch.io — free browser games",
            url = "https://itch.io/games/free",
            blurb = "Marketplace of independent games. The browser-playable ones run without an " +
                "install; each page states its own price and licence."
        )
    )

    fun byPackage(packageId: String): GameEntry? =
        (games + emulators).firstOrNull { it.packageId == packageId }
}
