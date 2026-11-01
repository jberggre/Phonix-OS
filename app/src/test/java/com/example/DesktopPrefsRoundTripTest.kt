package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DesktopCustomization
import com.example.model.IconSizeMode
import com.example.model.TaskbarPosition
import com.example.util.DesktopPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The desktop layout that comes back after a relaunch must be the one that was saved.
 *
 * These two fields regressed silently, which is why they get their own test: `showIconFrames` was
 * written under a key name the reader no longer asked for, and `areAllWindowsHidden` was never
 * written at all. Both failed without an error - the reader simply returned the default.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DesktopPrefsRoundTripTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `icon frames and hidden windows survive a save and load`() {
        val saved = DesktopCustomization(
            iconSize = IconSizeMode.LARGE,
            taskbarPosition = TaskbarPosition.RIGHT,
            showIconFrames = true,
            areAllWindowsHidden = true,
            lockIconsToGrid = true
        )

        DesktopPrefs.save(context, saved)
        val loaded = DesktopPrefs.load(context)

        assertTrue("a saved layout must be readable", loaded != null)
        assertEquals("icon frames", true, loaded!!.showIconFrames)
        assertEquals("hidden windows", true, loaded.areAllWindowsHidden)
        assertEquals("icon size", IconSizeMode.LARGE, loaded.iconSize)
        assertEquals("taskbar edge", TaskbarPosition.RIGHT, loaded.taskbarPosition)
        assertEquals("grid snap", true, loaded.lockIconsToGrid)
    }

    @Test
    fun `a layout written with the old icon-frame key is still honoured`() {
        // Layouts on disk before the V3 rename carried the flag under showIconFramesV2.
        context.getSharedPreferences("phonix_desktop_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("desktop_state_v1", """{"showIconFramesV2":true}""")
            .commit()

        assertEquals(true, DesktopPrefs.load(context)?.showIconFrames)
    }
}
