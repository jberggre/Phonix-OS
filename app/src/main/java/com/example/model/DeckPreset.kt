package com.example.model

import java.util.Locale

/**
 * Three complete window arrangements for a desktop of a given size.
 *
 * Positions are fractions of the viewport and every window goes through [WindowGeometry.normalize],
 * so the same preset lays out a docked 1920x1080 monitor and a 411x869 phone screen without a second
 * table of numbers. Ids are derived from the preset and the window type, so re-applying a preset
 * replaces its windows instead of piling duplicates on the desktop.
 */
enum class DeckPreset(val title: String, val subtitle: String) {
    WORKSTATION("Workstation", "Terminal on the left, browser on the right"),
    BUILD("Build", "Editor over the terminal, files down the side"),
    MEDIA("Media", "Browser wide, phone screen beside it");

    private data class Slot(
        val type: WindowType,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    private fun slots(): List<Slot> = when (this) {
        WORKSTATION -> listOf(
            Slot(WindowType.TERMINAL, 0.02f, 0.06f, 0.47f, 0.87f),
            Slot(WindowType.BROWSER, 0.51f, 0.06f, 0.47f, 0.87f)
        )
        BUILD -> listOf(
            Slot(WindowType.VS_CODE, 0.02f, 0.06f, 0.56f, 0.58f),
            Slot(WindowType.TERMINAL, 0.02f, 0.66f, 0.56f, 0.28f),
            Slot(WindowType.FILE_EXPLORER, 0.60f, 0.06f, 0.38f, 0.88f)
        )
        MEDIA -> listOf(
            Slot(WindowType.BROWSER, 0.02f, 0.06f, 0.60f, 0.88f),
            Slot(WindowType.PHONE_MIRROR, 0.64f, 0.06f, 0.34f, 0.88f)
        )
    }

    fun windows(bounds: WindowBounds): List<DesktopWindow> = slots().mapIndexed { index, slot ->
        WindowGeometry.normalize(
            DesktopWindow(
                id = "deck_${name.lowercase(Locale.ROOT)}_${slot.type.name.lowercase(Locale.ROOT)}",
                type = slot.type,
                posX = bounds.widthDp * slot.x,
                posY = bounds.heightDp * slot.y,
                width = bounds.widthDp * slot.width,
                height = bounds.heightDp * slot.height,
                zIndex = (index + 1).toFloat()
            ),
            bounds
        )
    }
}
