package com.example

import com.example.service.ScreenMirror
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Phone Screen surface must have the phone panel's shape.
 *
 * The shipped bug used the metrics of the display the app was on. With the desktop on a
 * 1920x1080 monitor that produced a 540x303 (16:9) surface, and the window showed the desktop.
 */
class ScreenMirrorGeometryTest {

    @Test
    fun portraitPanel_keepsItsOwnAspectRatio() {
        val (width, height) = ScreenMirror.captureSize(1080, 2280)
        assertEquals(540, width)
        assertEquals(1140, height)
    }

    @Test
    fun desktopMetrics_cannotBeMistakenForThePanel() {
        val phone = ScreenMirror.captureSize(1080, 2280)
        val monitor = ScreenMirror.captureSize(1920, 1080)
        assertEquals(540 to 303, monitor)
        assertEquals("16:9 must not be the phone's shape", false, phone == monitor)
    }

    @Test
    fun landscapePanel_isWiderThanTall() {
        val (width, height) = ScreenMirror.captureSize(2280, 1080)
        assertEquals(540, width)
        assertEquals(255, height)
    }

    @Test
    fun degenerateMetrics_doNotProduceAZeroSizedSurface() {
        val (width, height) = ScreenMirror.captureSize(0, 0)
        assertEquals(540, width)
        assertTrue("a surface with no height cannot be created", height > 0)
    }
}
