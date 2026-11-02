package com.example.engine

import com.example.model.DefaultServerCatalog
import com.example.model.ModuleManifestFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the catalog spec builder before manifests lean on it.
 *
 * A manifest service is meant to be an addition, so the two built-in services have to describe
 * themselves exactly as they did before this round: same launch line, same stop line, same probe.
 * If this test fails, a manifest did not add a path - it changed the one people already use.
 */
class ServiceSpecTest {

    private val nginx = DefaultServerCatalog.getDefaultServices().first { it.id == "nginx_web" }
    private val openssh = DefaultServerCatalog.getDefaultServices().first { it.id == "openssh" }

    @Test
    fun catalogSpecsAreByteForByteUnchanged() {
        val spec = ServiceSupervisor.specFor(nginx)
        assertEquals(
            "cd /root 2>/dev/null || cd /; " +
                "nohup sh -c 'nginx' > /tmp/phonix-nginx_web.log 2>&1 & " +
                "sleep 2; pidof nginx >/dev/null 2>&1 && echo ph-launched || echo ph-missing",
            spec.launchCommand
        )
        assertEquals(
            "for p in \$(pidof nginx); do kill \$p 2>/dev/null; done; sleep 1; true",
            spec.stopCommand
        )
        assertEquals("pidof nginx >/dev/null 2>&1 && echo ph-alive || echo ph-dead", spec.processCommand)
        assertEquals("http://127.0.0.1:8080/health", spec.probe.describe)
        assertEquals("nginx_web", spec.id)

        // The catalog hints still win, and the manifest-only field defaults to null.
        assertEquals("nginx", nginx.startHint)
        assertEquals("ssh-keygen -A && /usr/sbin/sshd", openssh.startHint)
        assertEquals("process", ServiceSupervisor.specFor(openssh).probe.describe)
    }

    @Test
    fun aManifestSpecUsesTheDeclaredCommandsAndProbe() {
        val manifest = ModuleManifestFormat.parse(
            "probe.manifest",
            """
            #!phonix-module 1
            module = servers
            service = manifest-probe
            name = Manifest Probe
            port = 8099
            process = httpd
            launch = httpd -f -p 8099 -h /tmp/phroot
            """.trimIndent()
        )
        val service = manifest.service!!
        val item = service.toServerServiceItem(emptyList())
        val spec = ServiceSupervisor.specFor(item, service)

        assertEquals("manifest-probe", spec.id)
        assertEquals("Manifest Probe", spec.name)
        assertTrue(spec.launchCommand.contains("httpd -f -p 8099 -h /tmp/phroot"))
        assertEquals(service.stopCommand, spec.stopCommand)
        assertEquals("http://127.0.0.1:8099/health", spec.probe.describe)
        assertEquals("pidof httpd >/dev/null 2>&1 && echo ph-alive || echo ph-dead", spec.processCommand)
        // The Hub's start hint is the declared command, not an empty package name.
        assertEquals("httpd -f -p 8099 -h /tmp/phroot", item.startHint)
    }
}
