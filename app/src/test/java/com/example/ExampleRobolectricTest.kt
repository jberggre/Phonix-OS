package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CoolingMode
import com.example.model.DesktopCustomization
import com.example.model.WindowType
import com.example.model.DeviceChangeChoices
import com.example.model.SetupPersona
import com.example.model.DeviceUseMode
import com.example.util.DesktopPrefs
import com.example.model.DesktopShortcut
import com.example.model.DesktopWallpaper
import com.example.model.KnowledgeCategory
import com.example.ui.components.nextMinuteDelayMillis
import com.example.util.DeviceSetupPreferences
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
    fun `device setup choices default off until explicitly saved`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("phonix_os_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().putBoolean("oobe_completed", true).commit()
        try {
            val defaults = DeviceSetupPreferences.choices(context)
            assertEquals(DeviceUseMode.DAILY_DRIVER, defaults.useMode)
            assertFalse(DeviceSetupPreferences.hasSavedChoices(context))
            assertFalse(defaults.applySystemWallpaper)
            assertFalse(defaults.startLinuxOnLaunch)
            assertFalse(defaults.keepLinuxInBackground)
            assertFalse(defaults.startLinuxAtBoot)
            assertFalse(defaults.watchExternalDisplays)
            assertFalse(defaults.immersiveExternalDisplay)
            assertFalse(defaults.allowSystemPrivateDns)
            assertFalse(defaults.allowDeviceOwnerNetworkManagement)
            assertFalse(defaults.allowDebloatCommands)
            assertFalse(defaults.allowFactoryReset)
            assertFalse(DeviceSetupPreferences.shouldStartLinuxOnLaunch(context))
            assertFalse(DeviceSetupPreferences.shouldKeepLinuxInBackground(context))
            assertFalse(DeviceSetupPreferences.shouldStartLinuxAtBoot(context))
            assertFalse(DeviceSetupPreferences.shouldWatchExternalDisplays(context))

            val optedIn = DeviceChangeChoices(
                useMode = DeviceUseMode.CYBERDECK,
                startLinuxOnLaunch = true,
                keepLinuxInBackground = true,
                startLinuxAtBoot = true
            )
            DeviceSetupPreferences.save(context, optedIn)
            assertTrue(DeviceSetupPreferences.hasSavedChoices(context))
            assertEquals(optedIn, DeviceSetupPreferences.choices(context))
            assertTrue(DeviceSetupPreferences.shouldStartLinuxOnLaunch(context))
            assertTrue(DeviceSetupPreferences.shouldKeepLinuxInBackground(context))
            assertTrue(DeviceSetupPreferences.shouldStartLinuxAtBoot(context))

            DeviceSetupPreferences.save(context, optedIn.copy(keepLinuxInBackground = false))
            assertFalse(DeviceSetupPreferences.shouldStartLinuxAtBoot(context))
        } finally {
            prefs.edit().clear().commit()
        }
    }

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
        val scFiles = initialShortcuts.first { it.id == "sc_files" }
        val scBrowser = initialShortcuts.first { it.id == "sc_browser" }

        // File Explorer owns (0, 0); the browser starts two columns to its right.
        assertEquals(0, scFiles.gridCol)
        assertEquals(0, scFiles.gridRow)
        assertEquals(0, scBrowser.gridRow)

        // Drop the browser directly onto File Explorer's grid slot (col 0, row 0).
        viewModel.dropShortcut("sc_browser", 16f, 16f)

        val updatedShortcuts = viewModel.uiState.value.customization.shortcuts
        val updatedBrowser = updatedShortcuts.first { it.id == "sc_browser" }
        val updatedFiles = updatedShortcuts.first { it.id == "sc_files" }

        // Both shortcuts must NOT have the same position! They must have swapped or displaced
        assertFalse(
            "Browser and File Explorer must not overlap at the same grid coordinates",
            updatedBrowser.gridCol == updatedFiles.gridCol && updatedBrowser.gridRow == updatedFiles.gridRow
        )
        assertFalse(
            "Browser and File Explorer must not overlap at the same pixel coordinates",
            updatedBrowser.posX == updatedFiles.posX && updatedBrowser.posY == updatedFiles.posY
        )

        assertEquals(0, updatedBrowser.gridCol)
        assertEquals(0, updatedBrowser.gridRow)

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

        // Nothing has started the guest in this test, so the state must say so rather than
        // defaulting to a running container the device does not have.
        assertEquals(ContainerStatus.STOPPED, viewModel.uiState.value.containerStatus)

        // Execute shutdown command
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
    fun `desktop keeps a conventional icon set and the removed duplicates stay gone`() {
        val ids = DesktopCustomization().shortcuts.map { it.id }

        // A Windows/Linux-like desktop: files, terminal, browser, settings - not an app drawer.
        assertTrue(ids.containsAll(listOf("sc_files", "sc_terminal", "sc_browser", "sc_settings")))

        // Photos, the distro store and the hardware monitor are the OS card / Vitals widgets now.
        assertFalse("Photos shortcut should be gone", ids.contains("sc_photos"))
        assertFalse("Linux System shortcut should be gone", ids.contains("sc_distro"))
        assertFalse("System Monitor shortcut should be gone", ids.contains("sc_monitor"))
    }

    @Test
    fun `deleting a desktop shortcut leaves its taskbar pin alone`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)
        val pin = viewModel.uiState.value.customization.taskbarPins.first { it.id == "sc_files" }

        // Both a desktop icon and a taskbar pin exist for File Explorer. Removing the icon used to
        // remove the app from the bar as well - measured on device when Local AI vanished.
        assertTrue(viewModel.uiState.value.customization.shortcuts.any { it.id == pin.id })
        viewModel.removeShortcut(pin.id)

        assertTrue(viewModel.uiState.value.customization.shortcuts.none { it.id == pin.id })
        assertTrue(
            "the taskbar pin must survive deleting the desktop icon",
            viewModel.uiState.value.customization.taskbarPins.any { it.id == pin.id }
        )
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
    fun `welcome persona sets a daily driver up with nothing invasive`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        viewModel.selectSetupPersona(SetupPersona.DAILY_PHONE)
        val state = viewModel.uiState.value

        assertEquals(SetupPersona.DAILY_PHONE, state.setupPersona)
        assertEquals(DeviceUseMode.DAILY_DRIVER, state.deviceChanges.useMode)
        assertFalse(state.deviceChanges.applySystemWallpaper)
        assertFalse(state.deviceChanges.watchExternalDisplays)
        assertFalse(state.deviceChanges.keepLinuxInBackground)
        assertFalse(state.deviceChanges.startLinuxAtBoot)

        // Daily-safe trim only: analytics and carrier telemetry, never the whole catalogue.
        val ticked = state.debloatPackages.filter { it.isEnabled }
        assertTrue(ticked.isNotEmpty())
        assertTrue(ticked.all { it.category == "Analytics & Tracking" || it.category == "Carrier Telemetry" })
    }

    @Test
    fun `cyberdeck persona opts into appliance behaviour and the full trim list`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        viewModel.selectSetupPersona(SetupPersona.CYBERDECK_BUILD)
        val state = viewModel.uiState.value

        assertEquals(DeviceUseMode.CYBERDECK, state.deviceChanges.useMode)
        assertTrue(state.deviceChanges.watchExternalDisplays)
        assertTrue(state.deviceChanges.keepLinuxInBackground)
        assertTrue(state.debloatPackages.all { it.isEnabled })
        // The persona never touches the two dangerous switches, whatever mode is chosen.
        assertFalse(state.deviceChanges.allowFactoryReset)
        assertFalse(state.deviceChanges.allowSystemPrivateDns)
    }

    @Test
    fun `trim targets skip packages this device does not have`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)
        val missing = viewModel.uiState.value.debloatPackages.first().packageName
        val present = viewModel.uiState.value.debloatPackages.last().packageName

        viewModel.updateDebloatPresence(mapOf(missing to false, present to true))

        val targets = viewModel.debloatTargets().map { it.packageName }
        assertFalse(targets.contains(missing))
        assertTrue(targets.contains(present))
    }

    @Test
    fun `renaming an entry changes its label everywhere and never its artwork`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)
        val before = viewModel.uiState.value.customization.shortcuts.first()

        viewModel.startRename(before.id)
        viewModel.updateRenameDraft("My Files")
        viewModel.commitRename()

        val state = viewModel.uiState.value
        val renamed = state.customization.shortcuts.first { it.id == before.id }
        assertEquals("My Files", renamed.title)
        // The artwork is chosen from windowType, so the type must survive a rename.
        assertEquals(before.windowType, renamed.windowType)
        assertTrue(state.customization.taskbarPins.filter { it.id == before.id }.all { it.title == "My Files" })
        assertEquals(null, state.renameTargetId)
    }

    @Test
    fun `debloat advice is labelled and needs no model`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)
        viewModel.updateDebloatPresence(mapOf("com.samsung.android.game.gos" to true))

        viewModel.requestDebloatAdvice()
        val state = viewModel.uiState.value

        // Without a running model this must still answer, and must say where the text came from.
        assertNotNull(state.debloatAdvice)
        assertFalse(state.debloatAdviceFromModel)
        assertTrue(state.debloatAdvice!!.contains("ticked"))
        assertTrue(
            "advice must be explicit about whether Phonix can remove anything",
            state.debloatAdvice!!.contains("device owner") || state.debloatAdvice!!.contains("not device owner")
        )
    }

    @Test
    fun `closing the assistant clears its session but minimising keeps it`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PhonixViewModel(app)

        viewModel.openWindow(WindowType.AI_ASSISTANT)
        viewModel.setAiKnowledgeScope(KnowledgeCategory.CODING)
        viewModel.toggleKnowledgeLibrary()
        val window = viewModel.uiState.value.windows.first { it.type == WindowType.AI_ASSISTANT }

        // Minimising is not closing: the shelf choice and the library stay exactly as they were.
        viewModel.minimizeWindow(window.id)
        assertEquals(KnowledgeCategory.CODING, viewModel.uiState.value.aiKnowledgeScope)
        assertTrue(viewModel.uiState.value.showKnowledgeLibrary)

        // Closing is a reset, so the next question starts with a clean context.
        viewModel.closeWindow(window.id)
        val afterClose = viewModel.uiState.value
        assertEquals(null, afterClose.aiKnowledgeScope)
        assertFalse(afterClose.showKnowledgeLibrary)
        assertFalse(afterClose.isAiGenerating)
        assertEquals(
            "only the welcome message should remain",
            1,
            afterClose.aiChatMessages.size
        )
    }

    @Test
    fun `desktop layout round-trips through storage with renames and pins intact`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DesktopPrefs.clear(context)
        try {
            val saved = DesktopCustomization(
                wallpaper = DesktopWallpaper.CUSTOM_PHOTO,
                customWallpaperPath = "/data/user/0/com.example/files/desktop_custom_wallpaper.jpg",
                showDesktopGrid = true,
                showIconFrames = false,
                lockIconsToGrid = true,
                shortcuts = listOf(
                    DesktopShortcut(
                        id = "sc_terminal",
                        title = "My Shell",
                        windowType = WindowType.TERMINAL,
                        posX = 210f,
                        posY = 96f,
                        hasCustomPosition = true
                    )
                )
            )
            DesktopPrefs.save(context, saved)
            val loaded = DesktopPrefs.load(context)

            assertNotNull("a saved desktop must load", loaded)
            assertEquals(DesktopWallpaper.CUSTOM_PHOTO, loaded!!.wallpaper)
            assertEquals(saved.customWallpaperPath, loaded.customWallpaperPath)
            assertTrue(loaded.showDesktopGrid)
            assertFalse(loaded.showIconFrames)
            assertTrue(loaded.lockIconsToGrid)
            assertEquals(1, loaded.shortcuts.size)
            assertEquals("My Shell", loaded.shortcuts.first().title)
            assertEquals(210f, loaded.shortcuts.first().posX)
            // The taskbar has its own list and must survive on its own.
            assertEquals(saved.taskbarPins.map { it.id }, loaded.taskbarPins.map { it.id })
        } finally {
            DesktopPrefs.clear(context)
        }
    }

    @Test
    fun `taskbar clock wakes on the minute boundary, not every second`() {
        assertEquals(60_000L, nextMinuteDelayMillis(0L))
        assertEquals(30_000L, nextMinuteDelayMillis(30_000L))
        assertEquals(1L, nextMinuteDelayMillis(59_999L))
        // Never zero: a zero delay would spin the loop at the boundary.
        assertEquals(60_000L, nextMinuteDelayMillis(60_000L))
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
