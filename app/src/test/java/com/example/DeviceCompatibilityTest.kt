package com.example

import com.example.model.DeviceCompatibilityClassifier
import com.example.model.DeviceFormFactor
import com.example.model.DisplayRoute
import com.example.model.DesktopWindow
import com.example.model.FoldPosture
import com.example.model.VersionComparator
import com.example.model.WindowBounds
import com.example.model.WindowGeometry
import com.example.model.WindowType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCompatibilityTest {
    @Test
    fun classifiesDisplayRoutesWithoutOverstatingPixelMetadata() {
        assertEquals(
            DisplayRoute.SAMSUNG_DEX_WORKSPACE,
            DeviceCompatibilityClassifier.displayRoute("Samsung", hasDexWorkspace = true, hasExternalDisplay = true)
        )
        assertEquals(
            DisplayRoute.PIXEL_EXTERNAL_DISPLAY_UNVERIFIED,
            DeviceCompatibilityClassifier.displayRoute("Google", hasDexWorkspace = false, hasExternalDisplay = true)
        )
        assertEquals(
            DisplayRoute.PHONE_ONLY,
            DeviceCompatibilityClassifier.displayRoute("Google", hasDexWorkspace = false, hasExternalDisplay = false)
        )
    }

    @Test
    fun doesNotMistakeASecondPanelForAnExternalMonitor() {
        // Measured on a Pixel 9 Pro Fold: the cover panel is display 3 and neither panel carries
        // FLAG_PRESENTATION, so only a real presentation output may count as a monitor.
        assertTrue(DeviceCompatibilityClassifier.isPresentationOutput(true, true))
        assertFalse(DeviceCompatibilityClassifier.isPresentationOutput(false, true))
        assertFalse(DeviceCompatibilityClassifier.isPresentationOutput(true, false))
    }

    @Test
    fun classifiesFoldPostureAndLargeWindows() {
        assertEquals(DeviceFormFactor.FOLDABLE_UNFOLDED, DeviceCompatibilityClassifier.formFactor(FoldPosture.FLAT, 390))
        assertEquals(DeviceFormFactor.FOLDABLE_HALF_OPENED, DeviceCompatibilityClassifier.formFactor(FoldPosture.HALF_OPENED, 390))
        assertEquals(DeviceFormFactor.LARGE_WINDOW, DeviceCompatibilityClassifier.formFactor(FoldPosture.UNKNOWN, 600))
        assertEquals(DeviceFormFactor.PHONE, DeviceCompatibilityClassifier.formFactor(FoldPosture.UNKNOWN, 599))
    }

    @Test
    fun normalizesAndResizesWindowsWithinAvailableBounds() {
        val original = DesktopWindow(
            id = "test",
            type = WindowType.TERMINAL,
            posX = 900f,
            posY = 900f,
            width = 700f,
            height = 600f
        )
        val bounds = WindowBounds(widthDp = 800f, heightDp = 600f)
        val normalized = WindowGeometry.normalize(original, bounds)
        assertEquals(700f, normalized.width, 0f)
        assertEquals(600f, normalized.height, 0f)
        assertEquals(100f, normalized.posX, 0f)
        assertEquals(0f, normalized.posY, 0f)

        val resized = WindowGeometry.resize(normalized, -1000f, -1000f, bounds)
        assertEquals(240f, resized.width, 0f)
        assertEquals(180f, resized.height, 0f)
        assertEquals(100f, resized.posX, 0f)
        assertEquals(0f, resized.posY, 0f)

        val moved = WindowGeometry.move(resized, 1000f, 1000f, bounds)
        assertEquals(560f, moved.posX, 0f)
        assertEquals(420f, moved.posY, 0f)
    }

    @Test
    fun comparesDottedReleaseVersionsNumerically() {
        assertTrue(VersionComparator.isNewer("v1.2", "1.1"))
        assertFalse(VersionComparator.isNewer("1.9", "1.10"))
        assertTrue(VersionComparator.isNewer("1.10", "1.9"))
        assertFalse(VersionComparator.isNewer("1.2", "v1.2.0"))
        assertFalse(VersionComparator.isNewer("not-a-version", "1.2"))
    }
}
