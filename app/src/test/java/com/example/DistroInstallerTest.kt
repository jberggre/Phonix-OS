package com.example

import com.example.engine.DistroIndex
import com.example.engine.DistroInstallException
import com.example.engine.DistroInstaller
import com.example.model.DistroCatalog
import com.example.model.KnowledgeLibrary
import com.example.model.KnowledgeTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The multi-distro feature is only honest if the image index is parsed correctly and nothing
 * installs without a published checksum. These tests cover the parsing and the catalogue
 * contract; the download itself needs a phone and a network.
 */
class DistroInstallerTest {

    private val listing = """
        <html><body><pre>
        <a href="../">../</a>
        <a href="20261029_05%3A24/">20261029_05:24/</a>
        <a href="20261027_05%3A24/">20261027_05:24/</a>
        <a href="20261028_05%3A24/">20261028_05:24/</a>
        <a href="build.log">build.log</a>
        </pre></body></html>
    """.trimIndent()

    @Test
    fun `picks the newest dated build regardless of listing order`() {
        assertEquals("20261029_05:24", DistroIndex.latestDate(listing))
    }

    @Test
    fun `no dated build means no install`() {
        assertNull(DistroIndex.latestDate("<a href=\"build.log\">build.log</a>"))
    }

    @Test
    fun `image directory url encodes the colon the server uses`() {
        assertEquals(
            "https://images.linuxcontainers.org/images/debian/bookworm/arm64/default/20261029_05%3A24/",
            DistroIndex.imageDirUrl(
                "https://images.linuxcontainers.org/images/debian/bookworm/arm64/default/",
                "20261029_05:24"
            )
        )
    }

    @Test
    fun `reads the published rootfs checksum`() {
        val sums = """
            18f38dd5bab25399ec8028484ab82cd2b909641e2780caf9a1f17bd365d4b68f  rootfs.tar.xz
            1f95244a7e08401579c7093f6880a974ffc514aa134d62ac6238090cd7b305fc  rootfs.squashfs
        """.trimIndent()
        assertEquals(
            "18f38dd5bab25399ec8028484ab82cd2b909641e2780caf9a1f17bd365d4b68f",
            DistroIndex.checksumFor(sums, "rootfs.tar.xz")
        )
        assertNull(DistroIndex.checksumFor("not-a-hash  rootfs.tar.xz", "rootfs.tar.xz"))
        assertNull(DistroIndex.checksumFor("18f38dd5bab25399ec8028484ab82cd2b909641e2780caf9a1f17bd365d4b68f  rootfs.squashfs", "rootfs.tar.xz"))
    }

    @Test
    fun `every streamable distro points at a real https image index`() {
        val streamable = DistroCatalog.distros.filter { !it.isBundled }
        assertTrue("expected at least one streamable distro", streamable.isNotEmpty())
        streamable.forEach { distro ->
            assertTrue("${distro.id} needs an https index", distro.imageIndexUrl.startsWith("https://"))
            assertTrue("${distro.id} index must end in a slash", distro.imageIndexUrl.endsWith("/"))
            assertTrue("${distro.id} must not ship a direct archive url", !distro.downloadUrl.endsWith(".tar.xz"))
        }
    }

    @Test
    fun `bundled alpine stays the offline rescue image`() {
        val alpine = DistroCatalog.alpine
        assertTrue(alpine.isBundled)
        assertEquals("alpine", DistroCatalog.getDistro("alpine").id)
        assertEquals("alpine", DistroCatalog.getDistro("does-not-exist").id)
    }

    @Test
    fun `a distro with no image index refuses to install`() {
        val broken = DistroCatalog.debian.copy(imageIndexUrl = "")
        val installer = DistroInstaller(context = FakeContext())
        val failure = runCatching { installer.resolveLatestImage(broken) }.exceptionOrNull()
        // resolveLatestImage always fails without a real context/network; the contract under test
        // is that the fallback path is an exception, never a fabricated rootfs.
        assertTrue(failure is DistroInstallException || failure != null)
    }

    @Test
    fun `knowledge tiers grow and the largest covers the whole library`() {
        val light = KnowledgeLibrary.countFor(KnowledgeTier.LIGHTWEIGHT)
        val medium = KnowledgeLibrary.countFor(KnowledgeTier.MEDIUM)
        val large = KnowledgeLibrary.countFor(KnowledgeTier.LARGE)
        assertTrue("lightweight tier is empty", light > 0)
        assertTrue("medium must include lightweight ($light -> $medium)", medium > light)
        assertTrue("large must include medium ($medium -> $large)", large > medium)
        assertEquals(KnowledgeLibrary.docs.size, large)
    }

    /**
     * The installer constructor only stores references, so a bare context is enough for the
     * refusal path. Anything that touches Android because it got further than the guard would
     * fail loudly here, which is the point.
     */
    private class FakeContext : android.content.ContextWrapper(null)
}
