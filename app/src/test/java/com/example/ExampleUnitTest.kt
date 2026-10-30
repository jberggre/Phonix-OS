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
}
