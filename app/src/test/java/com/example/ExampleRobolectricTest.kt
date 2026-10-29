package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CoolingMode
import com.example.model.WindowType
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.ContainerStatus
import com.example.viewmodel.PhonixViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Phonix OS", appName)
    }

    @Test
    fun `test grid snapping prevents app overlap when dropped on occupied cell`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        // Enable grid lock for grid snapping tests
        viewModel.toggleLockIconsToGrid(true)

        val initialShortcuts = viewModel.uiState.value.customization.shortcuts
        val scStore = initialShortcuts.first { it.id == "sc_store" }
        val scBrowser = initialShortcuts.first { it.id == "sc_browser" }

        // Initially sc_store is at (0, 0) and sc_browser is at (0, 1)
        assertEquals(0, scStore.gridCol)
        assertEquals(0, scStore.gridRow)
        assertEquals(0, scBrowser.gridCol)
        assertEquals(1, scBrowser.gridRow)

        // Drop sc_browser directly onto sc_store's grid slot (x=16f, y=16f -> col 0, row 0)
        viewModel.dropShortcut("sc_browser", 16f, 16f)

        val updatedShortcuts = viewModel.uiState.value.customization.shortcuts
        val updatedBrowser = updatedShortcuts.first { it.id == "sc_browser" }
        val updatedStore = updatedShortcuts.first { it.id == "sc_store" }

        // Both shortcuts must NOT have the same position! They must have swapped or displaced
        assertFalse(
            "Browser and Store shortcuts must not overlap at the same grid coordinates",
            updatedBrowser.gridCol == updatedStore.gridCol && updatedBrowser.gridRow == updatedStore.gridRow
        )
        assertFalse(
            "Browser and Store shortcuts must not overlap at the same pixel coordinates",
            updatedBrowser.posX == updatedStore.posX && updatedBrowser.posY == updatedStore.posY
        )

        // Specifically, sc_browser took (0, 0) and sc_store was swapped to (0, 1)
        assertEquals(0, updatedBrowser.gridCol)
        assertEquals(0, updatedBrowser.gridRow)
        assertEquals(0, updatedStore.gridCol)
        assertEquals(1, updatedStore.gridRow)

        // Ensure ALL shortcuts on the desktop have completely unique grid coordinates
        val allSlots = updatedShortcuts.map { Pair(it.gridCol, it.gridRow) }
        assertEquals("All shortcuts must have unique grid slots without any duplicates", allSlots.size, allSlots.toSet().size)
    }

    @Test
    fun `test multiple shortcuts dropped on exact same grid cell never overlap`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        // Enable grid lock
        viewModel.toggleLockIconsToGrid(true)

        // Drag 4 different shortcuts one by one onto slot (0, 0)
        val testShortcutIds = listOf("sc_terminal", "sc_personalize", "sc_files", "sc_server")
        for (id in testShortcutIds) {
            viewModel.dropShortcut(id, 16f, 16f) // targeting (0, 0)
        }

        val shortcuts = viewModel.uiState.value.customization.shortcuts
        val gridSlots = shortcuts.map { Pair(it.gridCol, it.gridRow) }

        // Verify zero overlap across all 9 desktop apps
        assertEquals("No shortcuts may share the same grid slot", gridSlots.size, gridSlots.toSet().size)
        val pixelPositions = shortcuts.map { Pair(it.posX, it.posY) }
        assertEquals("No shortcuts may share the same pixel coordinates", pixelPositions.size, pixelPositions.toSet().size)
    }

    @Test
    fun `test freeform shortcut placement anywhere and opening closing homescreen widgets`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        // 1. Verify default is freeform mode (lockIconsToGrid = false)
        assertFalse("Default mode should be freeform unconstrained movement", viewModel.uiState.value.customization.lockIconsToGrid)

        // 2. Move shortcut freely anywhere on desktop (e.g. x = 230f, y = 340f)
        viewModel.dropShortcut("sc_terminal", 230f, 340f)
        val terminal = viewModel.uiState.value.customization.shortcuts.first { it.id == "sc_terminal" }
        assertEquals(230f, terminal.posX, 0.5f)
        assertEquals(340f, terminal.posY, 0.5f)

        // 3. Test closing and reopening Active Linux Distro card
        assertTrue("Distro card should be open by default", viewModel.uiState.value.customization.showQuickDistroWidget)
        viewModel.toggleDesktopWidget("distro", false)
        assertFalse("Distro card should now be closed", viewModel.uiState.value.customization.showQuickDistroWidget)
        viewModel.toggleDesktopWidget("distro", true)
        assertTrue("Distro card should now be reopened", viewModel.uiState.value.customization.showQuickDistroWidget)

        // 4. Test closing and reopening System Vitals widget
        assertTrue("System vitals should be open by default", viewModel.uiState.value.customization.showSystemMonitorWidget)
        viewModel.toggleDesktopWidget("system", false)
        assertFalse("System vitals should now be closed", viewModel.uiState.value.customization.showSystemMonitorWidget)
        viewModel.toggleDesktopWidget("system", true)
        assertTrue("System vitals should now be reopened", viewModel.uiState.value.customization.showSystemMonitorWidget)
    }

    @Test
    fun `test toggle lock to grid eliminates any overlaps from freeform placement`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        // Unlock grid
        viewModel.toggleLockIconsToGrid(false)
        assertFalse(viewModel.uiState.value.customization.lockIconsToGrid)

        // Re-lock to grid
        viewModel.toggleLockIconsToGrid(true)
        assertTrue(viewModel.uiState.value.customization.lockIconsToGrid)

        val shortcuts = viewModel.uiState.value.customization.shortcuts
        val gridSlots = shortcuts.map { Pair(it.gridCol, it.gridRow) }
        assertEquals("All shortcuts must have unique slots after locking to grid", gridSlots.size, gridSlots.toSet().size)
    }

    @Test
    fun `test terminal command clear and power execution`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        // Execute a command (records input or container status)
        viewModel.executeTerminalCommand("uname -a")
        val lines = viewModel.uiState.value.terminalLines
        assertTrue("Terminal should record input or status message", lines.isNotEmpty())

        // Execute clear command
        viewModel.executeTerminalCommand("clear")
        val linesAfterClear = viewModel.uiState.value.terminalLines
        assertTrue("Terminal should be completely cleared after clear command", linesAfterClear.isEmpty())

        // Execute shutdown command
        assertEquals(ContainerStatus.RUNNING, viewModel.uiState.value.containerStatus)
        viewModel.executeTerminalCommand("shutdown")
        assertEquals(ContainerStatus.STOPPED, viewModel.uiState.value.containerStatus)
    }

    @Test
    fun `test window lifecycle and desktop hide-restore`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        // Open Distro Store window
        viewModel.openWindow(WindowType.DISTRO_STORE)
        val distroWindow = viewModel.uiState.value.windows.find { it.type == WindowType.DISTRO_STORE }
        assertNotNull("Distro Store window should be present", distroWindow)
        assertFalse(distroWindow!!.isMinimized)

        // Toggle all windows (Show Desktop)
        viewModel.toggleAllWindowVisibility()
        assertTrue(viewModel.uiState.value.customization.areAllWindowsHidden)
        assertTrue(viewModel.uiState.value.windows.all { it.isMinimized })

        // Restore windows
        viewModel.toggleAllWindowVisibility()
        assertFalse(viewModel.uiState.value.customization.areAllWindowsHidden)
        assertTrue(viewModel.uiState.value.windows.any { !it.isMinimized })

        // Close all windows
        viewModel.closeAllWindows()
        assertTrue(viewModel.uiState.value.windows.isEmpty())
    }

    @Test
    fun `test cooling profile and direct power mode`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        viewModel.setCoolingMode(CoolingMode.ACTIVE_MODDED_COOLING)
        val state = viewModel.uiState.value
        assertEquals(CoolingMode.ACTIVE_MODDED_COOLING, state.coolingState.activeMode)
        assertTrue(state.notificationMessage?.contains("Heatsink and fan") == true)

        viewModel.toggleBatteryRemovedDirectPower(true)
        assertTrue(viewModel.uiState.value.isBatteryRemovedDirectPower)
    }

    @Test
    fun `test server service toggle and terminal state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        val nginx = viewModel.uiState.value.serverServices.first { it.id == "nginx_web" }
        assertFalse(nginx.isRunning)

        viewModel.toggleService("nginx_web")
        // Without active proot container running in JVM test, sets prompt to start container
        assertTrue(viewModel.uiState.value.notificationMessage?.contains("container") == true)
    }
}
