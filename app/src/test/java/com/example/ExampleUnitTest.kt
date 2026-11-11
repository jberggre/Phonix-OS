package com.example

import com.example.model.CoolingMode
import com.example.model.DebloatCatalog
import com.example.model.DefaultServerCatalog
import com.example.model.GameCatalog
import com.example.model.WindowType
import com.example.ui.components.AppIconGlyphs
import com.example.model.KnowledgeCategory
import com.example.model.KnowledgeLibrary
import com.example.model.LinuxAppCatalog
import com.example.model.StorageState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `library retrieval stays on the selected shelf`() {
        val water = KnowledgeLibrary.retrieve("how do I make water safe to drink", KnowledgeCategory.WATER)
        assertTrue("expected a water document", water.isNotEmpty())
        assertTrue(
            "a scoped search must not return another shelf",
            water.all { it.category == KnowledgeCategory.WATER }
        )

        val unscoped = KnowledgeLibrary.retrieve("how do I make water safe to drink", null)
        assertTrue(unscoped.isNotEmpty())
        assertEquals(KnowledgeCategory.WATER, unscoped.first().category)
    }

    @Test
    fun `library answers a coding question with the coding shelf`() {
        val hits = KnowledgeLibrary.retrieve("write a snake game in python for the terminal", KnowledgeCategory.CODING)
        assertTrue(hits.any { it.category == KnowledgeCategory.CODING })
        assertTrue("the snake recipe should rank", hits.any { it.id == "code_snake" })

        // A question pair of words with no overlap must not invent context.
        assertTrue(KnowledgeLibrary.contextFor("qwertyuiop zxcvbnm", null) == null)
    }

    @Test
    fun `every window type resolves to its own glyph`() {
        val glyphs = WindowType.entries.associateWith { AppIconGlyphs.forWindow(it) }
        // App Store and Settings used to fall through to the generic laptop glyph.
        assertNotEquals(glyphs[WindowType.BROWSER], glyphs[WindowType.APP_STORE])
        assertNotEquals(glyphs[WindowType.FILE_EXPLORER], glyphs[WindowType.SETTINGS])
        assertEquals(
            "each surface needs a distinct glyph",
            WindowType.entries.size,
            glyphs.values.toSet().size
        )
    }

    @Test
    fun `pinned phone apps get a glyph from their name, not the browser globe`() {
        val chat = AppIconGlyphs.forApp("WhatsApp Messenger", "com.whatsapp")
        val camera = AppIconGlyphs.forApp("Camera", "com.android.camera")
        val browser = AppIconGlyphs.forWindow(WindowType.BROWSER)

        assertNotEquals(browser, chat)
        assertNotEquals(browser, camera)
        assertNotEquals(chat, camera)
    }

    @Test
    fun testDebloatCatalogPackages() {
        val packages = DebloatCatalog.getDefaultPackages()
        assertTrue("Expected non-empty debloat list", packages.isNotEmpty())

        val hasGos = packages.any { it.packageName == "com.samsung.android.game.gos" }
        assertTrue("Samsung GOS package must be present in debloat list", hasGos)

        val hasBixby = packages.any { it.packageName == "com.samsung.android.bixby.agent" }
        assertTrue("Bixby package must be present in debloat list", hasBixby)
    }

    @Test
    fun testDefaultServerCatalogUnprivilegedPorts() {
        val servers = DefaultServerCatalog.getDefaultServices()
        assertTrue("Expected server services in catalog", servers.isNotEmpty())

        // Android unprivileged port constraint: all exposed ports must be >= 1024
        for (server in servers) {
            assertTrue(
                "Port ${server.port} on service ${server.name} must be >= 1024",
                server.port >= 1024
            )
        }
    }

    @Test
    fun testCoolingModeDefinitions() {
        val stock = CoolingMode.STOCK_PASSIVE
        assertNotNull(stock.title)
        assertNotNull(stock.description)

        val active = CoolingMode.ACTIVE_MODDED_COOLING
        assertEquals("Heatsink and fan on the board", active.title)
    }

    @Test
    fun testGameCatalogEntriesAndDirectUrls() {
        val games = GameCatalog.games
        assertTrue("Game catalog should have titles", games.isNotEmpty())

        val emulators = GameCatalog.emulators
        assertTrue("Emulator catalog should have titles", emulators.isNotEmpty())

        // Open-source titles should have fallback URLs (F-Droid or GitHub release)
        val mindustry = GameCatalog.byPackage("io.anuke.mindustry")
        assertNotNull(mindustry)
        assertNotNull(mindustry?.directUrl)

        val retroArch = GameCatalog.byPackage("com.retroarch")
        assertNotNull(retroArch)
        assertNotNull(retroArch?.directUrl)
    }

    @Test
    fun testLinuxAppCatalogAlpinePackages() {
        val apps = LinuxAppCatalog.apps
        assertTrue("Linux apps catalog should have entries", apps.isNotEmpty())
        assertTrue("Expected mpv in apps", apps.any { it.packageName == "mpv" })
        assertTrue("Expected htop in apps", apps.any { it.packageName == "htop" })
    }

    @Test
    fun testStorageStateDefaults() {
        val state = StorageState(
            totalBytes = 64_000_000_000L,
            freeBytes = 32_000_000_000L,
            guestBytes = 559_000_000L,
            guestPath = "/data/data/com.example/files/linux/alpine"
        )
        assertEquals(64_000_000_000L, state.totalBytes)
        assertEquals(559_000_000L, state.guestBytes)
    }

    @Test
    fun `linux desktop shortcuts and distro catalog entries are valid`() {
        val cli = com.example.model.DesktopShortcut.createFromLinuxCommand(
            title = "htop",
            command = "htop",
            isGuiApp = false,
            gridCol = 0,
            gridRow = 1
        )
        assertEquals("htop", cli.linuxCommand)
        assertFalse(cli.isLinuxGuiApp)
        assertEquals(WindowType.TERMINAL, cli.windowType)

        val gui = com.example.model.DesktopShortcut.createFromLinuxCommand(
            title = "GIMP",
            command = "gimp",
            isGuiApp = true,
            gridCol = 1,
            gridRow = 1
        )
        assertEquals("gimp", gui.linuxCommand)
        assertTrue(gui.isLinuxGuiApp)
        assertEquals(WindowType.LINUX_DESKTOP, gui.windowType)

        val distros = com.example.model.DistroCatalog.distros
        assertEquals(5, distros.size)
        assertTrue(distros.first().isBundled)
        assertTrue(distros.filter { !it.isBundled }.all { it.downloadUrl.startsWith("https://") })

        val des = com.example.model.LinuxDesktopEnvironment.entries
        assertEquals(4, des.size)
        assertEquals(
            com.example.model.LinuxDesktopEnvironment.XFCE4,
            com.example.model.LinuxDesktopEnvironment.fromId("xfce4")
        )
    }

    @Test
    fun `help tab and workstation documentation are searchable in offline library and notes`() {
        val tabs = com.example.viewmodel.AppNavTab.entries
        assertTrue(tabs.contains(com.example.viewmodel.AppNavTab.HELP))
        assertEquals("Help & Docs", com.example.viewmodel.AppNavTab.HELP.title)

        val workstationHits = KnowledgeLibrary.retrieve("how do I use phonix-pin and switch ubuntu xfce distro", KnowledgeCategory.HARDWARE)
        assertTrue("expected workstation doc in HARDWARE shelf", workstationHits.any { it.id == "hw_phonix_workstation" })

        val distroNote = com.example.model.DefaultAiKnowledge.matchResponse("how to install packages in debian or ubuntu with apt", "Stock", 40f)
        assertTrue(distroNote.first.contains("Debian 12"))
        assertTrue(distroNote.first.contains("apt-get"))

        val pinNote = com.example.model.DefaultAiKnowledge.matchResponse("how to pin shortcut with phonix-pin or switch xfce4", "Stock", 40f)
        assertTrue(pinNote.first.contains("phonix-pin"))
        assertTrue(pinNote.first.contains("XFCE4"))
    }

    @Test
    fun `bundled alpine-rootfs tgz extracts all 83 files and passes symlink-aware shell check`() {
        val assetCandidates = listOf(
            java.io.File("src/main/assets/alpine-rootfs.tgz"),
            java.io.File("app/src/main/assets/alpine-rootfs.tgz")
        )
        val assetFile = assetCandidates.firstOrNull { it.isFile }
        assertNotNull("Expected bundled alpine-rootfs.tgz in assets", assetFile)

        val tempDir = java.nio.file.Files.createTempDirectory("phonix-alpine-test").toFile()
        try {
            val extractedCount = assetFile!!.inputStream().use { input ->
                com.example.engine.TarGzExtractor(input).extractTo(tempDir)
            }
            assertEquals("Expected 83 regular files extracted from alpine-rootfs.tgz", 83, extractedCount)
            assertTrue(
                "bin/busybox must exist after extraction",
                com.example.engine.LinuxContainer.guestEntryExists(tempDir, "bin/busybox")
            )
            assertTrue(
                "bin/sh (symlink to /bin/busybox or fallback binary) must be detected by guestEntryExists",
                com.example.engine.LinuxContainer.guestEntryExists(tempDir, "bin/sh")
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `docker pihole adblock and samba smb are configured in server catalog and offline docs`() {
        val servers = DefaultServerCatalog.getDefaultServices()
        val smb = servers.find { it.id == "samba_smb" }
        assertNotNull("Expected Samba SMB server in DefaultServerCatalog", smb)
        assertEquals(4445, smb?.port)
        assertEquals("phonix-smb start", smb?.startHint)

        val adblock = servers.find { it.id == "dnsmasq_adblock" }
        assertNotNull("Expected Adblock DNS Sinkhole (Pi-hole Engine) in DefaultServerCatalog", adblock)
        assertEquals(5353, adblock?.port)
        assertEquals("phonix-adblock start", adblock?.startHint)

        // Verify Chrome appears only once in DebloatCatalog
        val chromeEntries = DebloatCatalog.getDefaultPackages().filter { it.packageName == "com.android.chrome" }
        assertEquals("com.android.chrome must appear exactly once in DebloatCatalog", 1, chromeEntries.size)

        // Verify Docker/Pi-hole/SMB knowledge doc and AI note response
        val dockerHits = KnowledgeLibrary.retrieve("can we run docker pihole and smb filesharing like termux", KnowledgeCategory.HARDWARE)
        assertTrue("Expected hw_docker_pihole_smb doc in HARDWARE shelf", dockerHits.any { it.id == "hw_docker_pihole_smb" })

        val dockerNote = com.example.model.DefaultAiKnowledge.matchResponse("how to run docker pihole and smb on phonix", "Stock", 40f)
        assertTrue(dockerNote.first.contains("phonix-smb"))
        assertTrue(dockerNote.first.contains("phonix-adblock"))
        assertTrue(dockerNote.first.contains("phonix-docker"))
    }

    @Test
    fun `switching x11 desktop environment preserves window manager taskbar and fallback`() {
        for (de in com.example.model.LinuxDesktopEnvironment.entries) {
            val script = com.example.engine.LinuxDesktop.buildStartScript(de, forceSwitch = true)
            // 1. Package install block must appear BEFORE killing the existing window manager
            val installIdx = script.indexOf("keeping current desktop active during install")
            val killIdx = script.indexOf("for proc in icewm icewm-session")
            assertTrue("Install check must precede WM replacement for ${de.id}", installIdx in 0 until killIdx)

            // 2. D-Bus & XDG_RUNTIME_DIR must be configured for PRoot
            assertTrue(script.contains("XDG_RUNTIME_DIR=/tmp/runtime-root"))
            assertTrue(script.contains("dbus-uuidgen --ensure"))

            // 3. Fallback to IceWM must always be present if target WM fails to start
            assertTrue(script.contains("restoring IceWM fallback window manager & taskbar"))
        }

        val xfceScript = com.example.engine.LinuxDesktop.buildStartScript(
            com.example.model.LinuxDesktopEnvironment.XFCE4,
            forceSwitch = true
        )
        assertTrue(xfceScript.contains("xfwm4 --replace"))
        assertTrue(xfceScript.contains("xfce4-panel --disable-wm-check"))

        val openboxScript = com.example.engine.LinuxDesktop.buildStartScript(
            com.example.model.LinuxDesktopEnvironment.OPENBOX,
            forceSwitch = true
        )
        assertTrue(openboxScript.contains("openbox --replace"))
        assertTrue(openboxScript.contains("tint2 -c /root/.config/tint2/tint2rc"))

        val lxqtScript = com.example.engine.LinuxDesktop.buildStartScript(
            com.example.model.LinuxDesktopEnvironment.LXQT,
            forceSwitch = true
        )
        assertTrue(lxqtScript.contains("openbox --replace"))
        assertTrue(lxqtScript.contains("lxqt-panel"))
        assertTrue(lxqtScript.contains("tint2 -c /root/.config/tint2/tint2rc"))
    }
}



