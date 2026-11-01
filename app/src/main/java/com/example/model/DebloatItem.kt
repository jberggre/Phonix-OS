package com.example.model

data class DebloatPackage(
    val packageName: String,
    val displayName: String,
    val category: String,
    val isEnabled: Boolean = true,
    val description: String,
    val estimatedStorageMb: Int = 45,
    val estimatedRamMb: Int = 30
)

object DebloatCatalog {
    /**
     * Categories the screenless Mini PC preset ticks.
     *
     * These must be category names that exist below. The preset used to compare against
     * "Telephony & Carrier", "Biometrics & Sensors" and "OEM Bloat", none of which the
     * catalogue has, so pressing the button ticked nothing and copied an empty command list.
     */
    val applianceCategories: Set<String> = setOf(
        "OEM Performance Throttler",
        "OEM Voice Assistant",
        "Analytics & Tracking",
        "Carrier Telemetry",
        "OEM Feed & News",
        "Payment & Store Sync"
    )

    fun getDefaultPackages(): List<DebloatPackage> = listOf(
        DebloatPackage(
            packageName = "com.android.chrome",
            displayName = "Google Chrome & Webview Companion",
            category = "Web Browser Bloat",
            isEnabled = false,
            description = "Consumes ~180MB storage and persistent RAM for background sync. Replace with lightweight Chromium or Phonix Browser.",
            estimatedStorageMb = 180,
            estimatedRamMb = 90
        ),
        DebloatPackage(
            packageName = "com.samsung.android.game.gos",
            displayName = "Samsung Game Optimizing Service (GOS)",
            category = "OEM Performance Throttler",
            isEnabled = true,
            description = "Known for aggressively downclocking older Galaxy S8/Note 8/S9/S10 SoCs. Disabling this removes artificial clock throttling.",
            estimatedStorageMb = 48,
            estimatedRamMb = 35
        ),
        DebloatPackage(
            packageName = "com.samsung.android.bixby.agent",
            displayName = "Bixby Voice & Wakeup Suite",
            category = "OEM Voice Assistant",
            isEnabled = true,
            description = "Background voice recognition daemons that constantly monitor microphone and consume background RAM.",
            estimatedStorageMb = 75,
            estimatedRamMb = 65
        ),
        DebloatPackage(
            packageName = "com.facebook.services",
            displayName = "Facebook Background Analytics & Telemetry",
            category = "Analytics & Tracking",
            isEnabled = true,
            description = "Preloaded analytics, AppInstaller, and sync services that wake the CPU every few minutes.",
            estimatedStorageMb = 52,
            estimatedRamMb = 40
        ),
        DebloatPackage(
            packageName = "com.samsung.android.aremoji",
            displayName = "Samsung AR Emoji & Stickers",
            category = "Unneeded Bloatware",
            isEnabled = true,
            description = "Unnecessary 3D avatar daemons consuming memory on phones dedicated to server or mini PC use.",
            estimatedStorageMb = 64,
            estimatedRamMb = 25
        ),
        DebloatPackage(
            packageName = "com.sec.android.app.billing",
            displayName = "Samsung IAP & Payment Telemetry",
            category = "Payment & Store Sync",
            isEnabled = true,
            description = "Background billing listeners that wake during network state changes.",
            estimatedStorageMb = 32,
            estimatedRamMb = 20
        ),
        DebloatPackage(
            packageName = "com.samsung.android.spage",
            displayName = "Samsung Daily / Free Content Hub",
            category = "OEM Feed & News",
            isEnabled = true,
            description = "Constantly polls web feeds in the background, consuming Wi-Fi bandwidth and battery.",
            estimatedStorageMb = 85,
            estimatedRamMb = 45
        ),
        DebloatPackage(
            packageName = "com.carrier.analytics.diag",
            displayName = "Carrier Diagnostics & Push Updaters",
            category = "Carrier Telemetry",
            isEnabled = true,
            description = "Carrier reporting tools (Verizon/AT&T/T-Mobile/Vodafone) that poll radio towers.",
            estimatedStorageMb = 28,
            estimatedRamMb = 18
        ),
        DebloatPackage(
            packageName = "com.microsoft.skydrive",
            displayName = "Microsoft OneDrive Preload",
            category = "Cloud Sync Bloat",
            isEnabled = true,
            description = "Background cloud sync daemons that poll for media changes.",
            estimatedStorageMb = 95,
            estimatedRamMb = 45
        )
    )
}

