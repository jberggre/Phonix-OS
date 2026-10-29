package com.example.model

/**
 * Build guides, not controls.
 *
 * An unrooted Android app cannot set CPU clocks, lift a thermal limit or read a fan, so these
 * entries describe the physical build the unit is going into. The screen that shows them says
 * exactly that, and no number here is presented as a measurement we did not make.
 */
enum class CoolingMode(
    val title: String,
    val subtitle: String,
    val description: String,
) {
    STOCK_PASSIVE(
        title = "Stock phone body",
        subtitle = "Factory glass, battery in place",
        description = "Samsung's thermal policy decides the clocks. Expect sustained performance to drop " +
            "once the phone warms up, and nothing in this app can raise that limit."
    ),
    ACTIVE_MODDED_COOLING(
        title = "Heatsink and fan on the board",
        subtitle = "Hardware mod inside the case",
        description = "A copper shim, thermal paste and a small fan move heat off the SoC shield, so the " +
            "same policy throttles later. We have not measured a boost figure on this build; treat any " +
            "percentage you have seen quoted as unverified."
    ),
    SERVER_ECO_247(
        title = "Always-on server build",
        subtitle = "Low power, long life",
        description = "For a unit that stays on: keep the guest mostly idle, remove heavy Android apps and " +
            "use a slow charger. Android still owns the clocks - Phonix shows what the sensors report " +
            "and stays out of the way."
    )
}

data class MotherboardCoolingState(
    val activeMode: CoolingMode = CoolingMode.STOCK_PASSIVE
)
