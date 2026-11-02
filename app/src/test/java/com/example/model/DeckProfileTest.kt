package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A profile that applies to the wrong screen is worse than no profile, so the identity rules and the
 * "may this display take over" gate are both pinned here.
 */
class DeckProfileTest {

    private val monitor = DisplayIdentity.of("HDMI Screen", 1920, 1080)
    private val dexWorkspace = DisplayIdentity.of("Desktop", 1920, 1080)
    private val phone = DisplayIdentity.of("Built-in Screen", 1440, 3040)
    private val otherMonitor = DisplayIdentity.of("ES-G34C5", 2560, 1440)

    @Test
    fun identityIgnoresOrientationAndDensity() {
        assertEquals(
            DisplayIdentity.of("HDMI Screen", 1920, 1080),
            DisplayIdentity.of("HDMI Screen", 1080, 1920)
        )
        // Same name, same size, different mode is still the same screen.
        assertEquals(monitor.key, DisplayIdentity.of("hdmi screen", 1920, 1080).key)
    }

    @Test
    fun differentScreensGetDifferentKeys() {
        assertNotEquals(monitor.key, dexWorkspace.key)
        assertNotEquals(monitor.key, phone.key)
        assertNotEquals(monitor.key, otherMonitor.key)
        assertNotEquals(phone.key, dexWorkspace.key)
    }

    @Test
    fun aNamelessDisplayStillGetsAUsableKey() {
        val unnamed = DisplayIdentity.of(null, 1920, 1080)
        assertTrue(unnamed.key.startsWith("display:"))
        assertEquals("Display (1080x1920)", unnamed.label)
    }

    @Test
    fun profilesAreRememberedPerScreenAndCanBeForgotten() {
        val profiles = DeckProfiles()
        assertTrue(profiles.isEmpty())
        assertNull(profiles.presetFor(monitor))

        assertTrue(profiles.remember(monitor, DeckPreset.BUILD))
        assertTrue(profiles.remember(phone, DeckPreset.MEDIA))
        assertEquals(DeckPreset.BUILD, profiles.presetFor(monitor))
        assertEquals(DeckPreset.MEDIA, profiles.presetFor(phone))
        // Remembering the same choice again is not a change, so it must not rewrite storage.
        assertFalse(profiles.remember(monitor, DeckPreset.BUILD))
        assertTrue(profiles.remember(monitor, DeckPreset.WORKSTATION))
        assertEquals(DeckPreset.WORKSTATION, profiles.presetFor(monitor))

        assertTrue(profiles.forget(monitor))
        assertFalse(profiles.forget(monitor))
        assertNull(profiles.presetFor(monitor))
        assertEquals(1, profiles.asMap().size)
    }

    @Test
    fun onlyTheBuiltInPanelIsBarredFromTakingOverOnAppear() {
        assertFalse(DeckProfiles.mayTakeOverOnAppear(phone))
        assertTrue(DeckProfiles.mayTakeOverOnAppear(monitor))
        assertTrue(DeckProfiles.mayTakeOverOnAppear(dexWorkspace))
    }
}
