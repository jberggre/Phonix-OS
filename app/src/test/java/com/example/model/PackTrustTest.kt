package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The digest table is the root of trust, so both halves of it are pinned here: what a usable release
 * looks like, and every way a receipt must be refused.
 *
 * These cases are chosen from the bug classes this campaign has already hit: a threshold that
 * disagrees with its own expectation (a byte count that is not the file's), off-by-one bounds (a
 * body one byte over the pin), and defaults that accept bad input (a blank digest, a remote http URL,
 * a path that walks out of the guest).
 */
class PackTrustTest {

    private fun release(
        id: String = "test-pack",
        sha256: String = "a".repeat(64),
        bytes: Long = 16L,
        url: String = "https://example.org/pack.bin",
        guestPath: String = "root/packs/test.txt"
    ) = PackRelease(id = id, title = "Test", url = url, sha256 = sha256, bytes = bytes, guestPath = guestPath)

    private fun refused(receipt: PackReceipt, release: PackRelease?): String {
        val verdict = PackTrust.judge(release, receipt)
        assertTrue("expected a refusal, got $verdict", verdict is PackVerdict.Refused)
        return (verdict as PackVerdict.Refused).reason
    }

    @Test
    fun aCompleteReleaseValidates() {
        assertNull(PackTrust.validate(release()))
        assertNull(PackTrust.validate(PackTrust.shipped.single()))
    }

    @Test
    fun aMissingDigestIsAReasonNotADefault() {
        val message = PackTrust.validate(release(sha256 = ""))
        assertTrue(message!!, message.contains("pins no SHA-256"))
    }

    @Test
    fun aDigestThatIsNotAFullSha256IsRefused() {
        assertTrue(PackTrust.validate(release(sha256 = "abc123"))!!.contains("not 64 hex"))
        assertTrue(PackTrust.validate(release(sha256 = "z".repeat(64)))!!.contains("not 64 hex"))
    }

    @Test
    fun aRemotePlainHttpUrlIsRefused() {
        // Loopback http is allowed so the fetcher can be tested against a local server; anything
        // else must be TLS, because the digest protects the bytes, not the connection.
        assertTrue(PackTrust.validate(release(url = "http://example.org/pack.bin"))!!.contains("HTTPS"))
        assertNull(PackTrust.validate(release(url = "http://127.0.0.1:8080/pack.bin")))
    }

    @Test
    fun aPathThatEscapesTheGuestIsRefused() {
        assertTrue(PackTrust.validate(release(guestPath = "../outside.txt"))!!.contains("unsafe guest path"))
        assertTrue(PackTrust.validate(release(guestPath = "/etc/passwd"))!!.contains("unsafe guest path"))
        assertTrue(PackTrust.validate(release(guestPath = "root//packs/x"))!!.contains("unsafe guest path"))
    }

    @Test
    fun aByteCountOfZeroIsNotAPin() {
        assertTrue(PackTrust.validate(release(bytes = 0L))!!.contains("pins no byte count"))
        assertTrue(PackTrust.validate(release(bytes = -1L))!!.contains("pins no byte count"))
    }

    @Test
    fun anUnknownPackIsRefusedBeforeItsReceiptIsRead() {
        val reason = refused(PackReceipt(status = 200, bytes = 16L, sha256 = "a".repeat(64)), null)
        assertTrue(reason, reason.contains("No pack by that id is pinned"))
    }

    @Test
    fun aNonTwoHundredAnswerInstallsNothing() {
        val reason = refused(PackReceipt(status = 404, bytes = 0L, sha256 = ""), release())
        assertTrue(reason, reason.contains("HTTP 404"))
        assertTrue(refused(PackReceipt(302, 0L, ""), release()).contains("HTTP 302"))
    }

    @Test
    fun aShortBodyIsReportedAsTruncated() {
        val reason = refused(PackReceipt(200, 15L, "a".repeat(64)), release(bytes = 16L))
        assertTrue(reason, reason.contains("truncated"))
    }

    @Test
    fun aBodyOneByteOverThePinIsRefused() {
        val reason = refused(PackReceipt(200, 17L, "a".repeat(64)), release(bytes = 16L))
        assertTrue(reason, reason.contains("larger than declared"))
    }

    @Test
    fun aDigestMismatchNamesBothValuesAndInstallsNothing() {
        val reason = refused(PackReceipt(200, 16L, "b".repeat(64)), release(sha256 = "a".repeat(64)))
        assertTrue(reason, reason.contains("SHA-256 mismatch"))
        assertTrue(reason, reason.contains("nothing was installed"))
        // The whole value is not echoed; enough to compare, not enough to paste as a pin by mistake.
        assertFalse(reason.contains("b".repeat(64)))
    }

    @Test
    fun matchingBytesVerify() {
        val receipt = PackReceipt(200, 16L, "A".repeat(64))
        assertEquals(PackVerdict.Verified, PackTrust.judge(release(sha256 = "a".repeat(64)), receipt))
    }

    @Test
    fun benchControlsAreOnlyOfferedWhenAsked() {
        assertNull(PackTrust.find(PackTrust.BENCH_MISMATCH_ID))
        assertEquals(PackTrust.BENCH_MISMATCH_ID, PackTrust.find(PackTrust.BENCH_MISMATCH_ID, true)?.id)
        assertEquals(PackTrust.shipped.size + PackTrust.benchControls.size, PackTrust.catalogue(true).size)
        assertEquals(PackTrust.shipped.size, PackTrust.catalogue().size)
    }

    @Test
    fun theBenchMismatchIsRefusedByItsOwnDigest() {
        val control = PackTrust.find(PackTrust.BENCH_MISMATCH_ID, true)!!
        val verdict = PackTrust.judge(control, PackReceipt(200, control.bytes, "c".repeat(64)))
        assertTrue(verdict is PackVerdict.Refused)
        assertTrue((verdict as PackVerdict.Refused).reason.contains("SHA-256 mismatch"))
    }
}
