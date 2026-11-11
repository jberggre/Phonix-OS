package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DesktopWindow
import com.example.model.WindowBounds
import com.example.model.WindowGeometry
import com.example.model.WindowType
import com.example.util.DesktopPrefs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WindowLayoutPersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        DesktopPrefs.clear(context)
    }

    @After
    fun tearDown() {
        DesktopPrefs.clear(context)
    }

    @Test
    fun `window layout round trips for the same size class`() {
        val bounds = WindowBounds(1920f, 1080f)
        val key = WindowGeometry.layoutKey(bounds)
        val windows = listOf(
            DesktopWindow(
                id = "win_terminal_a",
                type = WindowType.TERMINAL,
                posX = 120f,
                posY = 80f,
                width = 520f,
                height = 410f,
                zIndex = 7f
            ),
            DesktopWindow(
                id = "win_browser_b",
                type = WindowType.BROWSER,
                title = "Docs",
                posX = 640f,
                posY = 160f,
                width = 700f,
                height = 500f,
                isMinimized = true,
                zIndex = 9f
            )
        )

        DesktopPrefs.saveWindows(context, key, windows)

        assertEquals(windows, DesktopPrefs.loadWindows(context, key))
    }

    @Test
    fun `an empty desktop is remembered instead of looking unsaved`() {
        val phoneKey = WindowGeometry.layoutKey(WindowBounds(411.4f, 869.2f))

        assertNull(DesktopPrefs.loadWindows(context, phoneKey))

        DesktopPrefs.saveWindows(context, phoneKey, emptyList())

        val restored = DesktopPrefs.loadWindows(context, phoneKey)
        assertNotNull(restored)
        assertTrue(restored!!.isEmpty())
        assertNull(DesktopPrefs.loadWindows(context, WindowGeometry.layoutKey(WindowBounds(1920f, 1080f))))
    }

    @Test
    fun `layout keys bucket fractional dp but keep phone and monitor apart`() {
        assertEquals(
            WindowGeometry.layoutKey(WindowBounds(1920f, 1080f)),
            WindowGeometry.layoutKey(WindowBounds(1919.6f, 1079.7f))
        )
        assertNotEquals(
            WindowGeometry.layoutKey(WindowBounds(411.4f, 869.2f)),
            WindowGeometry.layoutKey(WindowBounds(1920f, 1080f))
        )
    }

    @Test
    fun `linux command desktop shortcuts round trip through DesktopPrefs`() {
        val cliShortcut = com.example.model.DesktopShortcut.createFromLinuxCommand(
            title = "htop",
            command = "htop",
            isGuiApp = false,
            gridCol = 0,
            gridRow = 1
        )
        val guiShortcut = com.example.model.DesktopShortcut.createFromLinuxCommand(
            title = "GIMP",
            command = "gimp",
            isGuiApp = true,
            gridCol = 1,
            gridRow = 1
        )
        val initial = com.example.model.DesktopCustomization(
            shortcuts = listOf(cliShortcut, guiShortcut)
        )
        DesktopPrefs.save(context, initial)

        val restored = DesktopPrefs.load(context)
        assertNotNull(restored)
        assertEquals(2, restored!!.shortcuts.size)
        assertEquals("htop", restored.shortcuts[0].linuxCommand)
        assertEquals(false, restored.shortcuts[0].isLinuxGuiApp)
        assertEquals("gimp", restored.shortcuts[1].linuxCommand)
        assertEquals(true, restored.shortcuts[1].isLinuxGuiApp)
    }
}
