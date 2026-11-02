package com.example.model

import java.util.Locale

/**
 * A stable name for a display, so a deck arrangement can be remembered per screen.
 *
 * The display id is useless for this: Samsung renumbers its DeX workspace between sessions (the
 * workspace has been display 2 and display 9 on the same phone), so a profile keyed on it would
 * apply to the wrong screen or to nothing. Name and size survive a replug, and the density is
 * excluded because changing the monitor's mode must not lose the arrangement.
 */
data class DisplayIdentity(val key: String, val label: String) {

    companion object {
        const val PHONE = "builtin"

        /**
         * Builds an identity from what a display reports. A missing name falls back to the size,
         * which is still better than a profile that never matches again.
         */
        fun of(name: String?, widthPx: Int, heightPx: Int): DisplayIdentity {
            val cleanName = name?.trim().orEmpty()
            val safeWidth = widthPx.coerceAtLeast(1)
            val safeHeight = heightPx.coerceAtLeast(1)
            // Orientation flips the numbers; the screen is the same screen either way.
            val shortSide = minOf(safeWidth, safeHeight)
            val longSide = maxOf(safeWidth, safeHeight)
            val slug = cleanName.lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "display" }
            return DisplayIdentity(
                key = "$slug:$shortSide" + "x" + "$longSide",
                label = "${cleanName.ifBlank { "Display" }} ($shortSide" + "x" + "$longSide)"
            )
        }
    }
}

/**
 * Which deck arrangement belongs to which screen, and the rules for applying it.
 *
 * Deliberately a plain map with no Android types: the decision "should this display take over the
 * desktop" is the part worth testing, and it is the part that gets it wrong in practice - applying
 * a monitor's arrangement on the phone screen is the failure this guards against.
 */
class DeckProfiles(initial: Map<String, DeckPreset> = emptyMap()) {

    private val byDisplay = LinkedHashMap(initial)

    fun presetFor(identity: DisplayIdentity): DeckPreset? = byDisplay[identity.key]

    /** Remembers [preset] for [identity]; returns true when this changed the stored choice. */
    fun remember(identity: DisplayIdentity, preset: DeckPreset): Boolean =
        byDisplay.put(identity.key, preset) != preset

    fun forget(identity: DisplayIdentity): Boolean = byDisplay.remove(identity.key) != null

    fun asMap(): Map<String, DeckPreset> = LinkedHashMap(byDisplay)

    fun isEmpty(): Boolean = byDisplay.isEmpty()

    companion object {
        /**
         * Only an external screen may take the desktop over on appearance.
         *
         * The built-in panel appears every time the app is launched, so applying a preset there
         * would overwrite the arrangement the user just made on the phone - which is the one
         * arrangement they can always see.
         */
        fun mayTakeOverOnAppear(identity: DisplayIdentity): Boolean =
            identity.key != DisplayIdentity.PHONE && !identity.key.startsWith("built-in")
    }
}
