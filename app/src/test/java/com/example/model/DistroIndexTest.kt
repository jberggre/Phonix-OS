package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The image mirror accepts no undated rootfs path, so the catalog has to resolve a dated build
 * from `meta/1.0/index-system`. These tests pin the parsing rules that make that work offline.
 */
class DistroIndexTest {

    private val index = """
        debian;bookworm;amd64;default;20261028_04:18;/images/debian/bookworm/amd64/default/20261028_04:18/
        debian;bookworm;arm64;default;20261027_04:18;/images/debian/bookworm/arm64/default/20261027_04:18/
        debian;bookworm;arm64;default;20261028_04:18;/images/debian/bookworm/arm64/default/20261028_04:18/
        debian;bookworm;arm64;cloud;20261029_04:18;/images/debian/bookworm/arm64/cloud/20261029_04:18/
        ubuntu;noble;arm64;default;20261028_04:18;/images/ubuntu/noble/arm64/default/20261028_04:18/
        archlinux;current;arm64;default;20261028_04:18;/images/archlinux/current/arm64/default/20261028_04:18/
        kali;current;arm64;default;20261028_04:18;/images/kali/current/arm64/default/20261028_04:18/
        not;a;valid;row
    """.trimIndent()

    @Test
    fun picksTheNewestArm64DefaultBuild() {
        val ref = DistroIndex.resolve(index, "debian", "bookworm")
        assertNotNull(ref)
        assertEquals("20261028_04:18", ref?.buildDate)
        assertEquals(
            "https://images.linuxcontainers.org/images/debian/bookworm/arm64/default/20261028_04:18/rootfs.tar.xz",
            ref?.url
        )
    }

    @Test
    fun ignoresOtherArchitecturesAndVariants() {
        // 20261029 is newer but that row is the `cloud` variant, and amd64 rows must never win.
        val ref = DistroIndex.resolve(index, "debian", "bookworm")
        assertTrue(ref!!.url.contains("/arm64/default/"))
        assertTrue(!ref.url.contains("amd64"))
        assertTrue(!ref.url.contains("/cloud/"))
    }

    @Test
    fun missingReleaseResolvesToNull() {
        assertNull(DistroIndex.resolve(index, "debian", "trixie"))
        assertNull(DistroIndex.resolve(index, "alpine", "3.24"))
    }

    @Test
    fun everyStreamedCatalogDistroResolves() {
        for (distro in DistroCatalog.distros.filter { !it.isBundled }) {
            val ref = DistroIndex.resolve(index, distro.imageName, distro.imageRelease)
            assertNotNull("${distro.name} has no image name/release that the mirror can resolve", ref)
            assertTrue(ref!!.url.endsWith("/rootfs.tar.xz"))
        }
    }

    @Test
    fun bundledImageCarriesNoMirrorIdentity() {
        val bundled = DistroCatalog.distros.filter { it.isBundled }
        assertEquals(1, bundled.size)
        assertTrue(bundled.single().imageName.isEmpty())
    }

    @Test
    fun trayOfJunkRowsIsIgnored() {
        assertNull(DistroIndex.resolve("", "debian", "bookworm"))
        assertNull(DistroIndex.resolve("\n\n;;;\n", "debian", "bookworm"))
    }
    @Test
    fun acceptsThePublishedRootfsChecksumAndRejectsJunk() {
        val sums = """
            18f38dd5bab25399ec8028484ab82cd2b909641e2780caf9a1f17bd365d4b68f  rootfs.tar.xz
            1f95244a7e08401579c7093f6880a974ffc514aa134d62ac6238090cd7b305fc  rootfs.squashfs
        """.trimIndent()
        assertEquals(
            "18f38dd5bab25399ec8028484ab82cd2b909641e2780caf9a1f17bd365d4b68f",
            DistroIndex.checksumFor(sums)
        )
        assertNull(DistroIndex.checksumFor("not-a-hash  rootfs.tar.xz"))
        assertNull(
            DistroIndex.checksumFor(
                "18f38dd5bab25399ec8028484ab82cd2b909641e2780caf9a1f17bd365d4b68f  rootfs.squashfs"
            )
        )
    }
}
