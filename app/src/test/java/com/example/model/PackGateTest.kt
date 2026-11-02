package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A manifest may name a pack, and the module it describes stays blocked until that pack's digest has
 * verified. These tests pin both halves of that sentence: the declaration parses, and the gate does
 * not become a second way to bypass a measured refusal.
 */
class PackGateTest {

    private fun manifest(pack: String = "phonix-license") = """
        #!phonix-module 1
        module = servers
        pack = $pack
    """.trimIndent()

    @Test
    fun aManifestCanNameAPinnedPack() {
        val parsed = ModuleManifestFormat.parse("p.manifest", manifest())
        assertEquals("phonix-license", parsed.pack)
        val set = ModuleManifests.resolve(listOf("p.manifest" to manifest()))
        assertEquals(0, set.issues.size)
        assertEquals(listOf("phonix-license"), set.packs())
        assertEquals(listOf("phonix-license"), set.packsFor(PhonixModule.SERVER_MANAGER.id))
    }

    @Test
    fun aPackIdMustBeWellFormed() {
        val message = try {
            ModuleManifestFormat.parse("p.manifest", manifest("Phonix License!"))
            ""
        } catch (error: ModuleManifestException) {
            error.message.orEmpty()
        }
        assertTrue(message, message.contains("is not a pack id"))
    }

    @Test
    fun aPackWithNoPinnedDigestIsReported() {
        val set = ModuleManifests.resolve(listOf("p.manifest" to manifest("not-a-pinned-pack")))
        assertEquals(0, set.manifests.size)
        assertEquals(1, set.issues.size)
        assertTrue(set.issues.single().message.contains("no pinned digest"))
    }

    @Test
    fun aModuleStaysBlockedUntilItsPackVerifies() {
        val set = ModuleManifests.resolve(listOf("p.manifest" to manifest()))
        val facts = ModuleFacts(guestInstalled = true, isDeviceOwner = true)

        val blocked = ModuleRegistry.resolved(facts, set).first { it.module == PhonixModule.SERVER_MANAGER }
        assertFalse(blocked.isAvailable)
        assertTrue(blocked.blockedReason!!.contains("phonix-license"))
        assertTrue(blocked.blockedReason!!.contains("not downloaded and verified"))

        val verified = ModuleRegistry.resolved(
            facts.copy(verifiedPacks = setOf("phonix-license")),
            set
        ).first { it.module == PhonixModule.SERVER_MANAGER }
        assertTrue(verified.isAvailable)
        assertNull(verified.blockedReason)
    }

    @Test
    fun aVerifiedPackCannotUnblockAModuleWithoutItsCapability() {
        // The built-in refusal keeps precedence: a pack is a prerequisite, never a substitute.
        val set = ModuleManifests.resolve(listOf("p.manifest" to manifest()))
        val resolved = ModuleRegistry.resolved(
            ModuleFacts(guestInstalled = false, isDeviceOwner = false, verifiedPacks = setOf("phonix-license")),
            set
        ).first { it.module == PhonixModule.SERVER_MANAGER }
        assertEquals("Needs an installed Linux guest.", resolved.blockedReason)
    }

    @Test
    fun theLoaderHonoursTheIdsTheBuildCanVerify() {
        val dir = java.nio.file.Files.createTempDirectory("phonix-pack-gate").toFile()
        try {
            java.io.File(dir, "bench.manifest").writeText(
                """
                #!phonix-module 1
                module = notepad
                pack = bench-digest-control
                """.trimIndent()
            )
            // Shipped ids only: the control cannot be named, so the file is reported.
            val shippedOnly = ModuleManifestStore(dir).load()
            assertEquals(0, shippedOnly.manifests.size)
            assertEquals(1, shippedOnly.issues.size)
            // A debug build passes the wider catalogue and the same file is accepted.
            val withControls = ModuleManifestStore(dir).load(
                knownPacks = PackTrust.catalogue(includeBenchControls = true).map { it.id }.toSet()
            )
            assertEquals(1, withControls.manifests.size)
            assertEquals(0, withControls.issues.size)
            assertEquals(listOf("bench-digest-control"), withControls.packs())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aPackOnAnotherModulesManifestDoesNotBlockThisOne() {
        val terminal = ModuleManifests.resolve(
            listOf("t.manifest" to "#!phonix-module 1\nmodule = terminal\npack = phonix-license\n")
        )
        val servers = ModuleRegistry.resolved(
            ModuleFacts(guestInstalled = true, isDeviceOwner = true),
            terminal
        ).first { it.module == PhonixModule.SERVER_MANAGER }
        assertTrue(servers.isAvailable)
    }
}
