package com.example

import com.example.model.UpdateFeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The updater's honesty rules, tested without a device: a feed that cannot be verified must not
 * produce an update, and build numbers decide order, not version-name text.
 */
class UpdateFeedTest {

    private val digest = "0123456789abcdef".repeat(4)

    private fun document(
        versionCode: String = "7",
        versionName: String = "1.6",
        apkUrl: String = "https://example.org/phonix-1.6.apk",
        sha: String = digest
    ) = """
        {
          "versionCode": $versionCode,
          "versionName": "$versionName",
          "apkUrl": "$apkUrl",
          "sha256": "$sha",
          "notes": "Fixes",
          "published": "2026-11-02"
        }
    """.trimIndent()

    @Test
    fun `a complete feed parses`() {
        val manifest = UpdateFeed.parse(document())
        assertNotNull(manifest)
        assertEquals(7, manifest!!.versionCode)
        assertEquals("1.6", manifest.versionName)
        assertEquals("https://example.org/phonix-1.6.apk", manifest.apkUrl)
        assertEquals(digest, manifest.sha256)
        assertEquals("Fixes", manifest.notes)
    }

    @Test
    fun `a versionCode sent as a string still parses`() {
        assertEquals(7, UpdateFeed.parse(document(versionCode = "\"7\""))?.versionCode)
    }

    @Test
    fun `an unverified upgrade is rejected`() {
        assertNull(UpdateFeed.parse(document(sha = "not-a-digest")))
        assertNull(UpdateFeed.parse(document(sha = "")))
    }

    @Test
    fun `a plain-http or missing apk url is rejected`() {
        assertNull(UpdateFeed.parse(document(apkUrl = "http://example.org/phonix.apk")))
        assertNull(UpdateFeed.parse(document(apkUrl = "")))
    }

    @Test
    fun `a feed with no usable build number is rejected`() {
        assertNull(UpdateFeed.parse(document(versionCode = "0")))
        assertNull(UpdateFeed.parse(document(versionCode = "null")))
        assertNull(UpdateFeed.parse("not json at all"))
    }

    @Test
    fun `build numbers decide order, so 1_10 beats 1_9`() {
        val newer = UpdateFeed.parse(document(versionCode = "10", versionName = "1.10"))!!
        val olderByName = UpdateFeed.parse(document(versionCode = "9", versionName = "1.9"))!!
        assertTrue(UpdateFeed.isNewer(newer, currentVersionCode = 9))
        assertFalse(UpdateFeed.isNewer(olderByName, currentVersionCode = 10))
        assertFalse("same build is not an update", UpdateFeed.isNewer(olderByName, currentVersionCode = 9))
    }

    @Test
    fun `only a full sha-256 is a digest`() {
        assertTrue(UpdateFeed.isDigest(digest))
        assertFalse(UpdateFeed.isDigest(digest.dropLast(1)))
        assertFalse(UpdateFeed.isDigest("z" + digest.drop(1)))
    }
}
