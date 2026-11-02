package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A preset is applied to whatever display is in front of the phone, so the only hard requirement is
 * that no window lands outside the viewport on either size class.
 */
class DeckPresetTest {

    private val monitor = WindowBounds(1920f, 1080f)
    private val phone = WindowBounds(411.4f, 869.2f)

    @Test
    fun everyPresetStaysInsideBothDisplays() {
        for (bounds in listOf(monitor, phone)) {
            DeckPreset.entries.forEach { preset ->
                preset.windows(bounds).forEach { window ->
                    assertTrue(
                        "${preset.name} ${window.type} starts off-screen at ${window.posX},${window.posY}",
                        window.posX >= 0f && window.posY >= 0f
                    )
                    assertTrue(
                        "${preset.name} ${window.type} overflows ${bounds.widthDp} dp",
                        window.posX + window.width <= bounds.widthDp + 0.5f
                    )
                    assertTrue(
                        "${preset.name} ${window.type} overflows ${bounds.heightDp} dp",
                        window.posY + window.height <= bounds.heightDp + 0.5f
                    )
                }
            }
        }
    }

    @Test
    fun idsAreStableSoReapplyingReplacesRatherThanDuplicates() {
        val first = DeckPreset.BUILD.windows(monitor)
        val second = DeckPreset.BUILD.windows(monitor)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(first.size, first.map { it.id }.toSet().size)
        assertTrue(first.all { it.id.startsWith("deck_build_") })
    }

    @Test
    fun theThreePresetsAreActuallyDifferent() {
        assertEquals(3, DeckPreset.entries.size)
        val shapes = DeckPreset.entries.map { preset ->
            preset.windows(monitor).joinToString("|") { "${it.type.name}@${it.posX},${it.posY},${it.width}" }
        }
        assertEquals(3, shapes.toSet().size)
        assertNotEquals(
            DeckPreset.WORKSTATION.windows(monitor).first().width,
            DeckPreset.MEDIA.windows(monitor).first().width
        )
    }

    @Test
    fun eachPresetCarriesTheWindowsItsNamePromises() {
        assertTrue(DeckPreset.WORKSTATION.windows(monitor).any { it.type == WindowType.TERMINAL })
        assertTrue(DeckPreset.BUILD.windows(monitor).any { it.type == WindowType.VS_CODE })
        assertTrue(DeckPreset.MEDIA.windows(monitor).any { it.type == WindowType.PHONE_MIRROR })
        assertEquals(2, DeckPreset.WORKSTATION.windows(monitor).size)
        assertEquals(3, DeckPreset.BUILD.windows(monitor).size)
    }

    @Test
    fun phoneLayoutHasNoOverlappingIdenticalWindows() {
        val windows = DeckPreset.BUILD.windows(phone)
        assertEquals(windows.size, windows.map { it.type }.toSet().size)
        // Half of a phone screen is still a usable window, not a sliver.
        windows.forEach { assertTrue("${it.type} is ${it.width} dp wide", it.width >= 120f) }
    }
}
