package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * The manifest format is a declaration language, so every refusal here is a rule the format claims
 * to enforce. The cases are chosen from the bug classes this campaign has already hit: a threshold
 * that disagrees with its own expectation (a declared window the module does not own), off-by-one
 * bounds (port 65535 in, 65536 out; 32 packages in, 33 out), a probe with nothing to evaluate, and
 * defaults that would quietly accept bad input (an unknown key, a repeated key, a shell fragment
 * where a package name belongs).
 */
class ModuleManifestTest {

    private fun parse(text: String, source: String = "probe.manifest"): ModuleManifest =
        ModuleManifestFormat.parse(source, text)

    private fun rejected(text: String): String = try {
        ModuleManifestFormat.parse("bad.manifest", text)
        throw AssertionError("expected the manifest to be rejected")
    } catch (error: ModuleManifestException) {
        error.message ?: ""
    }

    private val full = """
        #!phonix-module 1
        module = servers
        window = server_manager
        packages = busybox, nginx   # trailing comment
        service = manifest-probe
        name = Manifest Probe
        port = 8099
        process = httpd
        probe = http
        launch = httpd -f -p 8099 -h /tmp/phroot
        stop = for p in ${'$'}(pidof httpd); do kill ${'$'}p; done
    """.trimIndent()

    @Test
    fun parsesAModuleAndItsServiceDeclaredAsData() {
        val manifest = parse(full)
        assertEquals(PhonixModule.SERVER_MANAGER, manifest.module)
        assertEquals(listOf("busybox", "nginx"), manifest.packages)
        val service = manifest.service!!
        assertEquals("manifest-probe", service.id)
        assertEquals("Manifest Probe", service.name)
        assertEquals(8099, service.port ?: 0)
        assertEquals("httpd", service.processName)
        assertEquals("http://127.0.0.1:8099/health", service.probe.describe)
        assertEquals("httpd -f -p 8099 -h /tmp/phroot", service.launchCommand)
        assertTrue(service.stopCommand.startsWith("for p in " + "\$(pidof httpd)"))
    }

    @Test
    fun aManifestMustNameAKnownModule() {
        val message = rejected("#!phonix-module 1\nmodule = photoshop\n")
        assertTrue(message, message.contains("not a module id"))
        assertTrue(message, message.contains("servers"))
    }

    @Test
    fun aDeclaredWindowMustAgreeWithTheModuleItNames() {
        // The bug class: a declaration that disagrees with the thing it claims to describe.
        val message = rejected("#!phonix-module 1\nmodule = servers\nwindow = browser\n")
        assertTrue(message, message.contains("is server_manager"))
    }

    @Test
    fun headerAndVersionAreRequired() {
        assertTrue(rejected("module = servers\n").contains("must start with"))
        assertTrue(rejected("#!phonix-module 2\nmodule = servers\n").contains("version"))
    }

    @Test
    fun unknownAndRepeatedKeysAreRefusedRatherThanIgnored() {
        assertTrue(rejected("#!phonix-module 1\nmodule = servers\ncolour = blue\n").contains("unknown key"))
        assertTrue(rejected("#!phonix-module 1\nmodule = servers\nmodule = browser\n").contains("repeats"))
        assertTrue(rejected("#!phonix-module 1\nmodule =\n").contains("no value"))
    }

    @Test
    fun portsAreOneTo65535Inclusive() {
        fun withPort(port: String) =
            "#!phonix-module 1\nmodule = servers\nservice = p\nprocess = httpd\nport = $port\nlaunch = httpd\n"

        assertEquals(1, parse(withPort("1")).service!!.port ?: 0)
        assertEquals(65535, parse(withPort("65535")).service!!.port ?: 0)
        // 0 is not a listening port, and 65536 does not fit on the wire: both are refused, not clamped.
        assertTrue(rejected(withPort("0")).contains("outside 1-65535"))
        assertTrue(rejected(withPort("65536")).contains("outside 1-65535"))
        assertTrue(rejected(withPort("-1")).contains("outside 1-65535"))
        assertTrue(rejected(withPort("eighty")).contains("not a number"))
    }

    @Test
    fun anHttpProbeWithNothingToAskIsAnError() {
        val message = rejected(
            "#!phonix-module 1\nmodule = servers\nservice = p\nprocess = httpd\nprobe = http\nlaunch = httpd\n"
        )
        assertTrue(message, message.contains("no port"))
    }

    @Test
    fun aProcessProbeCannotIgnoreADeclaredPort() {
        val message = rejected(
            "#!phonix-module 1\nmodule = servers\nservice = p\nprocess = httpd\nport = 8099\nprobe = process\nlaunch = httpd\n"
        )
        assertTrue(message, message.contains("never be asked"))
        assertTrue(rejected(
            "#!phonix-module 1\nmodule = servers\nservice = p\nprocess = httpd\nprobe = tcp\nlaunch = httpd\n"
        ).contains("not one of"))
    }

    @Test
    fun aServiceMustBeStartableAndProbeable() {
        assertTrue(rejected("#!phonix-module 1\nmodule = servers\nservice = p\nprocess = httpd\n").contains("no \"launch\""))
        assertTrue(rejected("#!phonix-module 1\nmodule = servers\nservice = p\nlaunch = httpd\n").contains("no \"process\""))
        assertTrue(rejected("#!phonix-module 1\nmodule = servers\nport = 8099\nlaunch = httpd\n").contains("no \"service\" id"))
    }

    @Test
    fun aManifestCannotShadowABuiltInService() {
        val message = rejected(
            "#!phonix-module 1\nmodule = servers\nservice = nginx_web\nprocess = nginx\nlaunch = nginx\n"
        )
        assertTrue(message, message.contains("built-in service id"))
    }

    @Test
    fun packageAndProcessNamesMustBeShellSafe() {
        // These strings are pasted into guest shell commands, so a semicolon is a second command.
        assertTrue(rejected("#!phonix-module 1\nmodule = servers\npackages = nginx;reboot\n").contains("not a package name"))
        assertTrue(rejected(
            "#!phonix-module 1\nmodule = servers\nservice = p\nprocess = httpd && rm -rf /\nlaunch = httpd\n"
        ).contains("is not a process name"))
    }

    @Test
    fun thePackageLimitIncludesItsOwnBound() {
        val atLimit = (1..ModuleManifestFormat.MAX_PACKAGES).joinToString(", ") { "pkg$it" }
        assertEquals(
            ModuleManifestFormat.MAX_PACKAGES,
            parse("#!phonix-module 1\nmodule = servers\npackages = $atLimit\n").packages.size
        )
        val overLimit = (1..(ModuleManifestFormat.MAX_PACKAGES + 1)).joinToString(", ") { "pkg$it" }
        assertTrue(rejected("#!phonix-module 1\nmodule = servers\npackages = $overLimit\n").contains("the limit is 32"))
    }

    @Test
    fun anOmittedStopIsDerivedFromTheValidatedProcessName() {
        val service = parse(
            "#!phonix-module 1\nmodule = servers\nservice = p\nprocess = httpd\nport = 8099\nlaunch = httpd -f\n"
        ).service!!
        assertEquals(ModuleManifestFormat.defaultStop("httpd"), service.stopCommand)
        assertTrue(service.stopCommand.contains("pidof httpd"))
    }

    @Test
    fun aServiceWithoutAPortProbesTheProcessAndNeedsNoPackage() {
        val service = parse(
            "#!phonix-module 1\nmodule = servers\nservice = p\nprocess = httpd\nlaunch = httpd -f\n"
        ).service!!
        assertNull(service.port)
        assertEquals("process", service.probe.describe)
        val item = service.toServerServiceItem(emptyList())
        assertEquals("httpd", item.processName)
        // Nothing to install, so the card must not offer to install an empty package.
        assertTrue(item.isInstalled)
        assertEquals("httpd -f", item.startHint)
    }

    @Test
    fun anOversizedManifestIsRefusedBySize() {
        val huge = "#!phonix-module 1\nmodule = servers\n# " + "x".repeat(ModuleManifestFormat.MAX_CHARS)
        assertTrue(rejected(huge).contains("the limit is"))
    }

    @Test
    fun commentsAndBlankLinesAreNotEntries() {
        val manifest = parse(
            "\n\n#!phonix-module 1\n\n# a leading comment\nmodule = servers # inline\n"
        )
        assertEquals(PhonixModule.SERVER_MANAGER, manifest.module)
        assertNull(manifest.service)
    }

    @Test
    fun resolveKeepsGoodFilesAndReportsBadOnes() {
        val set = ModuleManifests.resolve(listOf("good.manifest" to full, "bad.manifest" to "nonsense\n"))
        assertEquals(1, set.manifests.size)
        assertEquals(1, set.issues.size)
        assertEquals("bad.manifest", set.issues.single().source)
        assertTrue(set.issues.single().message.isNotBlank())
        // The good file is still applied: one bad file must not hide the others.
        assertEquals(listOf("busybox", "nginx"), set.packagesFor("servers"))
    }

    @Test
    fun resolveRefusesASecondDeclarationOfTheSameService() {
        val second = full.replace("packages = busybox, nginx   # trailing comment\n", "")
        val set = ModuleManifests.resolve(listOf("a.manifest" to full, "b.manifest" to second))
        assertEquals(1, set.manifests.size)
        assertEquals(1, set.issues.size)
        assertTrue(set.issues.single().message.contains("already declared"))
        assertEquals(listOf("manifest-probe"), set.services.map { it.id })
    }

    @Test
    fun twoManifestsForOneModuleMergeTheirPackagesWithoutRepeats() {
        val a = "#!phonix-module 1\nmodule = terminal\npackages = bash, coreutils\n"
        val b = "#!phonix-module 1\nmodule = terminal\npackages = coreutils, htop\n"
        val set = ModuleManifests.resolve(listOf("a" to a, "b" to b))
        assertEquals(listOf("bash", "coreutils", "htop"), set.packagesFor("terminal"))
        assertEquals(0, set.issues.size)
        assertEquals(mapOf("terminal" to listOf("bash", "coreutils", "htop")), set.packagesByModule())
    }

    @Test
    fun noManifestsIsExactlyTheShippedRegistry() {
        val empty = ModuleManifests.resolve(emptyList())
        assertTrue(empty.manifests.isEmpty())
        assertTrue(empty.issues.isEmpty())
        assertTrue(empty.services.isEmpty())
        assertTrue(empty.packagesByModule().isEmpty())
        assertNull(empty.service("nginx_web"))
        assertTrue(empty.packagesFor("servers").isEmpty())
    }

    @Test
    fun theStoreReadsFilesAndReportsAnOversizedOne() {
        val dir = Files.createTempDirectory("phonix-manifests").toFile()
        try {
            File(dir, "good.manifest").writeText(full)
            File(dir, "huge.manifest").writeText(
                "#!phonix-module 1\nmodule = servers\n# " + "x".repeat(ModuleManifestFormat.MAX_CHARS)
            )
            val set = ModuleManifestStore(dir).load()
            assertEquals(1, set.manifests.size)
            assertEquals(1, set.issues.size)
            assertTrue(set.issues.single().message.contains("the limit is"))
            // An absent directory is "no manifests", which is the shipped behaviour, not an error.
            val missing = ModuleManifestStore(File(dir, "absent")).load()
            assertTrue(missing.manifests.isEmpty())
            assertTrue(missing.issues.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }
}
