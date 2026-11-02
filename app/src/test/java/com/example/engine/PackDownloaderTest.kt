package com.example.engine

import com.example.model.PackRelease
import com.example.model.PackTrust
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.security.MessageDigest

/**
 * The fetcher against a real socket.
 *
 * The decision table is [com.example.model.PackTrustTest]'s job; these tests are about the parts a
 * pure function cannot show: that a truncated transfer or a non-200 answer leaves no file in the
 * guest, that a redirect is followed, and that a digest mismatch removes the downloaded bytes instead
 * of leaving them somewhere a guest command could run them.
 */
class PackDownloaderTest {

    private lateinit var server: HttpServer
    private lateinit var privateRoot: File
    private lateinit var guestRoot: File
    private val body = "phonix pack bytes\n".repeat(20).toByteArray()
    private val digest: String = sha256(body)

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @Before
    fun start() {
        privateRoot = Files.createTempDirectory("phonix-packs").toFile()
        guestRoot = Files.createTempDirectory("phonix-guest").toFile().also { it.mkdirs() }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/good") { ex -> send(ex, 200, body, body.size.toLong()) }
        server.createContext("/missing") { ex -> send(ex, 404, ByteArray(0), -1L) }
        server.createContext("/redirect") { ex ->
            ex.responseHeaders.add("Location", "/good")
            send(ex, 302, ByteArray(0), -1L)
        }
        server.createContext("/oversize") { ex ->
            val doubled = body + body
            send(ex, 200, doubled, doubled.size.toLong())
        }
        server.createContext("/truncated") { ex ->
            // Announces the full length, then closes early: the body never arrives.
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.write(body.copyOf(10))
            ex.responseBody.close()
        }
        server.start()
    }

    @After
    fun stop() {
        server.stop(0)
        privateRoot.deleteRecursively()
        guestRoot.deleteRecursively()
    }

    private fun send(ex: HttpExchange, code: Int, bytes: ByteArray, length: Long) {
        ex.sendResponseHeaders(code, if (length >= 0) length else -1)
        if (bytes.isNotEmpty()) ex.responseBody.write(bytes)
        ex.close()
    }

    private fun release(path: String = "/good", pinned: String = digest, bytes: Long = body.size.toLong()) =
        PackRelease(
            id = "test-pack",
            title = "Test pack",
            url = "http://127.0.0.1:${server.address.port}$path",
            sha256 = pinned,
            bytes = bytes,
            guestPath = "root/packs/test.txt"
        )

    private fun downloader() = PackDownloader(privateRoot = privateRoot, guestRoot = { guestRoot })

    private fun guestFile() = File(guestRoot, "root/packs/test.txt")

    private fun tempFiles(): List<String> =
        (File(privateRoot, "tmp").listFiles() ?: emptyArray()).map { it.name }

    @Test
    fun aMatchingPackInstallsIntoTheGuestTreeAndIsRecordedVerified() {
        val result: Result<File> = runBlocking { downloader().install(release()) }
        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
        val file = guestFile()
        assertTrue(file.isFile)
        assertEquals(body.size.toLong(), file.length())
        assertTrue(file.readBytes().contentEquals(body))
        assertTrue("no partial may remain", tempFiles().isEmpty())
        assertFalse(File(guestRoot, "root/packs/test.txt.part").exists())
        assertEquals(setOf("test-pack"), downloader().verified(listOf(release())))
    }

    @Test
    fun aNonTwoHundredAnswerLeavesNothingBehind() {
        val result: Result<File> = runBlocking { downloader().install(release(path = "/missing")) }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("HTTP 404"))
        assertFalse(guestFile().exists())
        assertTrue(tempFiles().isEmpty())
        assertTrue(downloader().verified(listOf(release())).isEmpty())
    }

    @Test
    fun aTruncatedTransferInstallsNothingAndKeepsNoFile() {
        val result: Result<File> = runBlocking { downloader().install(release(path = "/truncated")) }
        assertTrue(result.isFailure)
        assertFalse("a half-downloaded file must not be left in the guest", guestFile().exists())
        assertFalse(File(guestRoot, "root/packs/test.txt.part").exists())
        assertTrue(tempFiles().isEmpty())
    }

    @Test
    fun aDigestMismatchRemovesTheDownloadedBytes() {
        val wrong = PackRelease(
            id = "test-pack",
            title = "Test pack",
            url = "http://127.0.0.1:${server.address.port}/good",
            sha256 = "0".repeat(64),
            bytes = body.size.toLong(),
            guestPath = "root/packs/test.txt"
        )
        val result: Result<File> = runBlocking { downloader().install(wrong) }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("SHA-256 mismatch"))
        assertFalse(guestFile().exists())
        assertTrue(tempFiles().isEmpty())
        assertTrue(downloader().verified(listOf(wrong)).isEmpty())
    }

    @Test
    fun aBodyLargerThanThePinIsRefused() {
        val result: Result<File> = runBlocking { downloader().install(release(path = "/oversize")) }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("larger than declared"))
        assertFalse(guestFile().exists())
        assertTrue(tempFiles().isEmpty())
    }

    @Test
    fun aRedirectIsFollowedAndThenVerified() {
        val result: Result<File> = runBlocking { downloader().install(release(path = "/redirect")) }
        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
        assertTrue(guestFile().isFile)
        assertTrue(guestFile().readBytes().contentEquals(body))
    }

    @Test
    fun aFailedReplaceLeavesNoPartFileInTheGuestAndKeepsWhatWasThere() {
        // A directory where the pack belongs makes both the rename and the copy fail after the
        // bytes are already staged, which is the one path where a guest `*.part` used to survive.
        val occupied = guestFile().also { it.mkdirs() }
        File(occupied, "held.txt").writeText("not ours to delete")

        val result = runBlocking { downloader().install(release()) }

        assertTrue(result.isFailure)
        assertFalse("a staging file must not survive a failed install", File(guestFile().path + ".part").exists())
        assertTrue("an unrelated directory must be left alone", File(occupied, "held.txt").isFile)
        assertTrue(tempFiles().isEmpty())
    }

    @Test
    fun theShippedCatalogueDoesNotReportADebugOnlyControlAsInstalled() {
        val control = PackTrust.benchControls.single()
        File(privateRoot, "verified").also { it.mkdirs() }
        File(privateRoot, "verified/" + control.id + ".sha256").writeText(control.sha256)
        val planted = File(guestRoot, control.guestPath)
        planted.parentFile!!.mkdirs()
        planted.writeBytes(ByteArray(control.bytes.toInt()))

        assertEquals(emptySet<String>(), downloader().verified())
        assertEquals(setOf(control.id), downloader().verified(listOf(control)))
    }

    @Test
    fun anUnknownPackIsRefusedWithoutTouchingTheNetwork() {
        val result: Result<File> = runBlocking { downloader().install(null) }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("No pack by that id is pinned"))
        assertFalse(guestFile().exists())
    }
}
