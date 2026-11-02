package com.example.model

/**
 * How hot is too hot to start something expensive on a phone.
 *
 * These are policy thresholds, not measurements: nothing in this project has run a soak yet, so the
 * numbers are a defensible starting point for a phone SoC rather than a calibrated limit. What the
 * policy buys is that the limit exists in one place, is testable, and can be changed after a real
 * soak without hunting through the UI.
 *
 * The reading is [SystemTelemetry]'s own: the hottest readable thermal zone, falling back to the
 * battery sensor when no zone answers. An unknown reading is not treated as cool - it means "no
 * opinion", and the caller proceeds, which is the honest behaviour for a sensor that is absent on
 * some units.
 */
data class ThermalReading(
    val hottestC: Float?,
    val isCharging: Boolean = false,
    /** True once a real sensor has answered; without it, a 0.0 reading means nothing. */
    val hasReading: Boolean = false
)

enum class ThermalVerdict {
    /** No sensor answered, so nothing is claimed either way. */
    UNKNOWN,
    OK,
    /** Run it, but say so: the phone is working hard and will throttle. */
    WARM,
    /** Refuse to start new sustained work. */
    HOT
}

object ThermalPolicy {

    /** A phone SoC under a 1.5B model sits in the 50s; sustained load this high is throttling. */
    const val WARM_C = 55f

    /** Above this, start nothing new: the battery ages and the phone becomes unpleasant to hold. */
    const val HOT_C = 70f

    /** Charging adds heat at the same load, so the warm band starts earlier. */
    const val WARM_CHARGING_C = 48f

    fun verdict(reading: ThermalReading): ThermalVerdict {
        if (!reading.hasReading || reading.hottestC == null) return ThermalVerdict.UNKNOWN
        val temperature = reading.hottestC
        return when {
            temperature >= HOT_C -> ThermalVerdict.HOT
            temperature >= WARM_C -> ThermalVerdict.WARM
            reading.isCharging && temperature >= WARM_CHARGING_C -> ThermalVerdict.WARM
            else -> ThermalVerdict.OK
        }
    }

    /** Whether to start a new sustained job (inference, a distro install, a big pack import). */
    fun mayStartSustainedWork(reading: ThermalReading): Boolean = verdict(reading) != ThermalVerdict.HOT

    /** The sentence shown when a start is refused, or null when it is allowed. */
    fun refusal(reading: ThermalReading, activity: String): String? = when (verdict(reading)) {
        ThermalVerdict.HOT -> "$activity will not start: the phone is at " +
            "${reading.hottestC?.toInt()} C, above the ${HOT_C.toInt()} C limit. " +
            "Let it cool, then try again."
        else -> null
    }

    /**
     * A one-line note for a job that was allowed to start, so a warm phone is stated rather than
     * discovered as unexplained throttling.
     */
    fun caution(reading: ThermalReading): String? = when (verdict(reading)) {
        // The parentheses matter: without them the "expect it to throttle" tail binds to the
        // else branch and disappears exactly when the phone is charging, which is the hotter case.
        ThermalVerdict.WARM -> "The phone is warm (${reading.hottestC?.toInt()} C)" +
            (if (reading.isCharging) " while charging" else "") +
            "; expect it to throttle."
        else -> null
    }
}
