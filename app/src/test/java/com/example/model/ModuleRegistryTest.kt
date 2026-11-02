package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The registry is the single answer to "can this module open now?", so these tests pin both the
 * refusals and the coverage: a new [WindowType] without a declaration is a compile-time gap nobody
 * would notice, which is exactly how the dead start menu survived.
 */
class ModuleRegistryTest {

    private val barePhone = ModuleFacts(guestInstalled = false, isDeviceOwner = false)
    private val fullDeck = ModuleFacts(guestInstalled = true, isDeviceOwner = true)

    @Test
    fun everyWindowTypeHasExactlyOneModule() {
        WindowType.entries.forEach { type ->
            val matches = ModuleRegistry.modules.filter { it.windowType == type }
            assertEquals("WindowType $type should have one module, has ${matches.size}", 1, matches.size)
        }
    }

    @Test
    fun moduleIdsAreUniqueAndStable() {
        val ids = ModuleRegistry.modules.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(WindowType.entries.size, ModuleRegistry.modules.size)
    }

    @Test
    fun guestModulesAreBlockedWithAReasonWithoutAGuest() {
        val terminal = PhonixModule.TERMINAL
        assertFalse(terminal.isAvailable(barePhone))
        assertEquals("Needs an installed Linux guest.", terminal.blockedReason(barePhone))
        assertTrue(terminal.isAvailable(fullDeck))
        assertNull(terminal.blockedReason(fullDeck))
    }

    @Test
    fun debloaterNeedsDeviceOwnerRatherThanAGuest() {
        val debloat = PhonixModule.DEBLOAT
        assertFalse(debloat.isAvailable(ModuleFacts(guestInstalled = true, isDeviceOwner = false)))
        assertEquals("Needs Phonix to be the device owner.", debloat.blockedReason(barePhone))
        assertTrue(debloat.isAvailable(fullDeck))
    }

    @Test
    fun plainWindowsAlwaysOpen() {
        listOf(PhonixModule.SETTINGS, PhonixModule.PRIVACY, PhonixModule.ABOUT, PhonixModule.BROWSER)
            .forEach { assertTrue("$it should open on a bare phone", it.isAvailable(barePhone)) }
        assertEquals(0, ModuleRegistry.blocked(ModuleFacts(guestInstalled = true, isDeviceOwner = true)).size)
    }

    @Test
    fun blockedReportsEveryUnavailableModuleWithItsReason() {
        val blocked = ModuleRegistry.blocked(barePhone)
        assertEquals(ModuleRegistry.modules.size - ModuleRegistry.available(barePhone).size, blocked.size)
        assertTrue(blocked.any { it.first == PhonixModule.VS_CODE })
        assertTrue(blocked.all { it.second.isNotBlank() })
    }

    @Test
    fun lookupByWindowTypeFindsTheModule() {
        assertEquals(PhonixModule.AI, ModuleRegistry.byWindowType(WindowType.AI_ASSISTANT))
        assertEquals(PhonixModule.FILE_EXPLORER, ModuleRegistry.byWindowType(WindowType.FILE_EXPLORER))
        assertNotNull(ModuleRegistry.byWindowType(WindowType.BENCHMARK))
    }

    @Test
    fun paletteSearchIsTermwiseAndIgnoresCase() {
        assertTrue(ModuleRegistry.matches("LINUX", PhonixModule.LINUX_DESKTOP))
        assertTrue(ModuleRegistry.matches("linux desk", PhonixModule.LINUX_DESKTOP))
        assertFalse(ModuleRegistry.matches("linux browser", PhonixModule.LINUX_DESKTOP))
        assertTrue(ModuleRegistry.matches("term", PhonixModule.TERMINAL))
        // An empty query is the untouched start menu: everything the phone can do.
        assertEquals(ModuleRegistry.available(fullDeck).size, ModuleRegistry.search("  ", fullDeck).size)
    }

    @Test
    fun paletteHidesBlockedModulesRatherThanOfferingThem() {
        val withGuest = ModuleRegistry.search("terminal", fullDeck).map { it.id }
        val withoutGuest = ModuleRegistry.search("terminal", barePhone).map { it.id }
        assertTrue(withGuest.contains("terminal"))
        assertFalse(withoutGuest.contains("terminal"))
        assertTrue(ModuleRegistry.search("settings", barePhone).map { it.id }.contains("settings"))
    }
}
