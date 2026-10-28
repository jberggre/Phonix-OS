package com.example.model

enum class CoolingMode(
    val title: String,
    val subtitle: String,
    val maxClockGhz: Float,
    val thermalLimitC: Int,
    val isThrottleBypassed: Boolean,
    val description: String
) {
    STOCK_PASSIVE(
        title = "Stock Passive Glass Body",
        subtitle = "Factory Phone Enclosure (Throttled)",
        maxClockGhz = 1.6f,
        thermalLimitC = 40,
        isThrottleBypassed = false,
        description = "Standard thermal throttling active. Clocks will drop to 1.1 GHz after 3-5 minutes under heavy load to prevent battery overheating."
    ),
    ACTIVE_MODDED_COOLING(
        title = "Active Motherboard Cooling (Heatsink + Fan)",
        subtitle = "Custom Hardware Mod (Sustained 2.45 GHz)",
        maxClockGhz = 2.45f,
        thermalLimitC = 65,
        isThrottleBypassed = true,
        description = "For phones with aftermarket copper heatsinks and 5V fans installed directly on SoC shield. Keeps CPU/GPU at 100% max clocks indefinitely (+60% sustained performance)!"
    ),
    SERVER_ECO_247(
        title = "24/7 Silent Server Eco",
        subtitle = "Ultra-Low Power Mode (~1.8W)",
        maxClockGhz = 1.2f,
        thermalLimitC = 38,
        isThrottleBypassed = false,
        description = "Downclocks CPU cores for ultra-low power consumption and cold operation. Ideal for 24/7 web hosting, Pi-hole DNS, and Samba file shares."
    )
}

data class MotherboardCoolingState(
    val activeMode: CoolingMode = CoolingMode.ACTIVE_MODDED_COOLING,
    val socTempC: Float = 34.8f,
    val batteryTempC: Float = 31.2f,
    val currentCpuClockGhz: Float = 2.45f,
    val sustainedPerformanceBoostPercent: Int = 58,
    val fanEstimatedRpm: Int = 3200,
    val batteryBypassRecommended: Boolean = true
)
