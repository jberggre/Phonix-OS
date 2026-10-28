package com.example.model

enum class GameCategory {
    X86_WINDOWS, RETRO_CONSOLE, NATIVE_LINUX, CLOUD_STREAM
}

data class GameTitleItem(
    val id: String,
    val name: String,
    val category: GameCategory,
    val engineRunner: String, // Box64 + Wine 9.0, RetroArch Snes9x, Native VirGL, Moonlight
    val targetFps: Int = 60,
    val ramRequirementMb: Int = 1024,
    val compatibilityRating: String, // Perfect (60 FPS), Playable (30-45 FPS), Streamed (<5ms)
    val description: String,
    val isInstalled: Boolean = true
)

object DefaultGamingCatalog {
    val items = listOf(
        // Box64 + Wine x86 Windows Classics
        GameTitleItem(
            id = "fallout_nv",
            name = "Fallout: New Vegas",
            category = GameCategory.X86_WINDOWS,
            engineRunner = "Box64 0.2.8 + Wine 9.0 + DXVK",
            targetFps = 45,
            ramRequirementMb = 2048,
            compatibilityRating = "Playable (35-50 FPS with Active Cooling)",
            description = "Classic post-apocalyptic RPG running via x86 to ARM64 binary translation and VirGL Turnip graphics."
        ),
        GameTitleItem(
            id = "halflife_2",
            name = "Half-Life 2",
            category = GameCategory.X86_WINDOWS,
            engineRunner = "Box64 + Wine + OpenGL",
            targetFps = 60,
            ramRequirementMb = 1536,
            compatibilityRating = "Perfect (55-60 FPS)",
            description = "Valve's legendary Source engine FPS. Runs smoothly on Snapdragon 835 / Exynos 8895 with active heatsink."
        ),
        GameTitleItem(
            id = "aoe_2",
            name = "Age of Empires II: HD",
            category = GameCategory.X86_WINDOWS,
            engineRunner = "Box64 + DirectDraw",
            targetFps = 60,
            ramRequirementMb = 1024,
            compatibilityRating = "Perfect (60 FPS)",
            description = "Historical RTS masterpiece. Fully playable with USB mouse and keyboard connected to your phone."
        ),
        GameTitleItem(
            id = "heroes_3",
            name = "Heroes of Might and Magic III",
            category = GameCategory.X86_WINDOWS,
            engineRunner = "VCMI Native / Wine",
            targetFps = 60,
            ramRequirementMb = 512,
            compatibilityRating = "Flawless (60 FPS)",
            description = "Turn-based strategy classic. Perfect for relaxed gaming on an external HDMI monitor."
        ),

        // Native Linux ARM Games
        GameTitleItem(
            id = "supertuxkart",
            name = "SuperTuxKart 3D",
            category = GameCategory.NATIVE_LINUX,
            engineRunner = "Native ARM64 + VirGL 3D GLES",
            targetFps = 60,
            ramRequirementMb = 768,
            compatibilityRating = "Flawless (60 FPS VSync)",
            description = "Full 3D open-source kart racing game with multiplayer, tracks, and native ARM64 shader compiling."
        ),
        GameTitleItem(
            id = "openttd",
            name = "OpenTTD (Transport Tycoon)",
            category = GameCategory.NATIVE_LINUX,
            engineRunner = "Native ARM64 Binary",
            targetFps = 60,
            ramRequirementMb = 256,
            compatibilityRating = "Flawless (60 FPS)",
            description = "Deep logistics simulation. Runs on literally any phone hardware with sub-100MB RAM usage."
        ),
        GameTitleItem(
            id = "gzdoom",
            name = "GZDoom / Freedoom Engine",
            category = GameCategory.NATIVE_LINUX,
            engineRunner = "Native GZDoom + VirGL",
            targetFps = 60,
            ramRequirementMb = 512,
            compatibilityRating = "Flawless (60 FPS)",
            description = "Hardware-accelerated classic Doom engine with dynamic lighting, high-res textures, and mods."
        ),

        // RetroArch Multi-Console
        GameTitleItem(
            id = "retroarch_suite",
            name = "RetroArch Multi-Console Hub",
            category = GameCategory.RETRO_CONSOLE,
            engineRunner = "RetroArch (PS1 / N64 / SNES / GBA)",
            targetFps = 60,
            ramRequirementMb = 512,
            compatibilityRating = "Flawless (Full Speed)",
            description = "Full retro console gaming center with USB gamepad mapping, save states, and shader filters."
        ),

        // Low-Latency Game Streaming
        GameTitleItem(
            id = "moonlight_stream",
            name = "Moonlight / Steam Link Station",
            category = GameCategory.CLOUD_STREAM,
            engineRunner = "Hardware H.264/HEVC Decoder",
            targetFps = 60,
            ramRequirementMb = 512,
            compatibilityRating = "Ultra-Low Latency (<4ms decode)",
            description = "Stream AAA modern games from your main gaming PC to your TV at 1080p 60fps with zero fan noise."
        )
    )
}
