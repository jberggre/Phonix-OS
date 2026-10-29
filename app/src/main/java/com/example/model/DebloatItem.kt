package com.example.model

data class DebloatPackage(
    val packageName: String,
    val displayName: String,
    val category: String,
    val isEnabled: Boolean = true,
    val description: String
)

object DebloatCatalog {
    fun getDefaultPackages(): List<DebloatPackage> = listOf(
        DebloatPackage(
            packageName = "com.samsung.android.game.gos",
            displayName = "Samsung Game Optimizing Service (GOS)",
            category = "OEM Performance Throttler",
            isEnabled = true,
            description = "Known for aggressively downclocking older Galaxy S8/Note 8/S9 SoCs. Disabling this removes artificial clock throttling."
        ),
        DebloatPackage(
            packageName = "com.samsung.android.bixby.agent",
            displayName = "Bixby Voice & Wakeup Suite",
            category = "OEM Voice Assistant",
            isEnabled = true,
            description = "Background voice recognition daemons that constantly monitor microphone and consume background RAM."
        ),
        DebloatPackage(
            packageName = "com.facebook.services",
            displayName = "Facebook Background Analytics & Telemetry",
            category = "Analytics & Tracking",
            isEnabled = true,
            description = "Preloaded analytics, AppInstaller, and sync services that wake the CPU every few minutes."
        ),
        DebloatPackage(
            packageName = "com.samsung.android.aremoji",
            displayName = "Samsung AR Emoji & Stickers",
            category = "Unneeded Bloatware",
            isEnabled = true,
            description = "Unnecessary 3D avatar daemons consuming memory on phones dedicated to server or mini PC use."
        ),
        DebloatPackage(
            packageName = "com.sec.android.app.billing",
            displayName = "Samsung IAP & Payment Telemetry",
            category = "Payment & Store Sync",
            isEnabled = true,
            description = "Background billing listeners that wake during network state changes."
        ),
        DebloatPackage(
            packageName = "com.samsung.android.spage",
            displayName = "Samsung Daily / Free Content Hub",
            category = "OEM Feed & News",
            isEnabled = true,
            description = "Constantly polls web feeds in the background, consuming Wi-Fi bandwidth and battery."
        ),
        DebloatPackage(
            packageName = "com.carrier.analytics.diag",
            displayName = "Carrier Diagnostics & Push Updaters",
            category = "Carrier Telemetry",
            isEnabled = true,
            description = "Carrier reporting tools (Verizon/AT&T/T-Mobile/Vodafone) that poll radio towers."
        )
    )
}
