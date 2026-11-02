package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.example.util.DesktopPrefs

/**
 * The profile map is what makes a deck survive a replug, so it has to round-trip exactly and has to
 * disappear when the desktop is reset - a stale profile would silently rearrange the next session.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DeckProfilePersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DesktopPrefs.clear(context)
    }

    @After
    fun tearDown() {
        DesktopPrefs.clear(context)
    }

    @Test
    fun profilesRoundTripPerDisplay() {
        val saved = linkedMapOf(
            "desktop:1080x1920" to "BUILD",
            "hdmi-screen:1080x1920" to "MEDIA"
        )
        DesktopPrefs.saveDeckProfiles(context, saved)
        assertEquals(saved, DesktopPrefs.loadDeckProfiles(context))
    }

    @Test
    fun nothingSavedMeansNothingRemembered() {
        assertTrue(DesktopPrefs.loadDeckProfiles(context).isEmpty())
    }

    @Test
    fun blankPresetNamesAreDroppedRatherThanReturned() {
        DesktopPrefs.saveDeckProfiles(context, mapOf("desktop:1080x1920" to ""))
        assertTrue(DesktopPrefs.loadDeckProfiles(context).isEmpty())
    }

    @Test
    fun resettingTheDesktopForgetsTheProfiles() {
        DesktopPrefs.saveDeckProfiles(context, mapOf("desktop:1080x1920" to "BUILD"))
        DesktopPrefs.clear(context)
        assertTrue(DesktopPrefs.loadDeckProfiles(context).isEmpty())
    }
}
