package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The pack format is the only way imported material enters the offline library, so the refusals
 * matter as much as the successes: a pack that half-parses must never reach the shelves.
 */
class LibraryPackTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val validPack = """
        #!phonix-pack 1
        @pack River field notes
        @shelf WATER
        @title Rainwater harvesting
        Gutter, first-flush diverter, then a covered tank.
        @shelf FIRST_AID
        @title Cleaning a wound
        Irrigate with clean water under pressure for five minutes.
    """.trimIndent()

    @Test
    fun parsesEachDocumentOntoItsShelf() {
        val docs = LibraryPackFormat.parse("river", validPack)
        assertEquals(2, docs.size)
        assertEquals(KnowledgeCategory.WATER, docs[0].category)
        assertEquals("Rainwater harvesting", docs[0].title)
        assertTrue(docs[0].body.startsWith("Gutter, first-flush"))
        assertEquals(KnowledgeCategory.FIRST_AID, docs[1].category)
        assertEquals("River field notes", docs[0].packTitle)
        assertEquals(listOf("pack:river:0", "pack:river:1"), docs.map { it.id })
    }

    @Test
    fun packDocumentsAppearInEveryTier() {
        // Imports are the owner's own material; a size setting must not hide them.
        val doc = LibraryPackFormat.parse("river", validPack).first().toKnowledgeDoc()
        assertEquals(KnowledgeTier.LIGHTWEIGHT, doc.tier)
        assertTrue(doc.keywords.contains("water"))
        assertTrue(doc.summary.contains("River field notes"))
    }

    @Test
    fun refusesFileWithoutHeader() {
        try {
            LibraryPackFormat.parse("bad", "just some notes\nno header")
            fail("expected a refusal")
        } catch (e: LibraryPackException) {
            assertTrue(e.message!!.contains("#!phonix-pack"))
        }
    }

    @Test
    fun refusesUnknownShelf() {
        val pack = "#!phonix-pack 1\n@shelf GARDENING\n@title Weeds\nbody\n"
        try {
            LibraryPackFormat.parse("typo", pack)
            fail("expected a refusal")
        } catch (e: LibraryPackException) {
            // The message must name a real shelf so the typo is fixable from the error alone.
            assertTrue(e.message!!.contains("WATER"))
        }
    }

    @Test
    fun refusesDocumentWithoutTitleOrBody() {
        for (pack in listOf(
            "#!phonix-pack 1\n@shelf WATER\nbody without a title\n",
            "#!phonix-pack 1\n@shelf WATER\n@title Empty\n"
        )) {
            try {
                LibraryPackFormat.parse("bad", pack)
                fail("expected a refusal for: $pack")
            } catch (_: LibraryPackException) {
                // expected
            }
        }
    }

    @Test
    fun installsListsAndDeletesWithRealBytes() {
        val store = LibraryPackStore(temp.newFolder("packs"))
        assertEquals(0L, store.bytes())
        assertTrue(store.packs().isEmpty())

        val installed = store.install("River field notes.txt", validPack)
        assertEquals("river-field-notes", installed.id)
        assertEquals(2, installed.docCount)
        assertEquals(validPack.toByteArray(Charsets.UTF_8).size.toLong(), installed.bytes)
        assertEquals(installed.bytes, store.bytes())
        assertEquals(2, store.docs().size)

        assertTrue(store.delete(installed.id))
        assertFalse(store.delete(installed.id))
        assertEquals(0L, store.bytes())
        assertTrue(store.packs().isEmpty())
        assertTrue(store.docs().isEmpty())
    }

    @Test
    fun badPackDoesNotEvictTheInstalledOne() {
        val store = LibraryPackStore(temp.newFolder("packs"))
        store.install("notes.txt", validPack)
        try {
            store.install("notes.txt", "#!phonix-pack 1\n@shelf NOPE\n@title x\nbody\n")
            fail("expected a refusal")
        } catch (_: LibraryPackException) {
            // expected
        }
        assertEquals(1, store.packs().size)
        assertEquals(2, store.docs().size)
    }

    @Test
    fun refusesOversizedPack() {
        val huge = "#!phonix-pack 1\n@shelf WATER\n@title x\n" + "a".repeat(LibraryPackFormat.MAX_CHARS)
        try {
            LibraryPackFormat.parse("huge", huge)
            fail("expected a refusal")
        } catch (e: LibraryPackException) {
            assertTrue(e.message!!.contains("limit"))
        }
    }
}
