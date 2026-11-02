package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The VS Code CLI bootstrap, as pure text.
 *
 * Two builds exist because two loaders exist, the shell takes one line at a time, and the only
 * acceptable proof of an install is the version the binary prints.
 */
class VsCodeCliTest {

    @Test
    fun onlyTheMuslBuildIsPublishedForArm64() {
        assertTrue(GuestLibc.MUSL.supportsCli)
        assertTrue(GuestLibc.MUSL.downloadUrl, GuestLibc.MUSL.downloadUrl.endsWith("os=cli-alpine-arm64"))
        assertTrue(GuestLibc.MUSL.downloadUrl.startsWith("https://"))
        // Measured 2026-11-02: the glibc arm64 URL answers HTTP 404.
        assertFalse(GuestLibc.GNU.supportsCli)
    }

    @Test
    fun theRefusalNamesTheMeasuredReason() {
        assertTrue(VsCodeCli.NO_ARMV64_GLIBC_CLI, VsCodeCli.NO_ARMV64_GLIBC_CLI.contains("404"))
        assertTrue(VsCodeCli.NO_ARMV64_GLIBC_CLI, VsCodeCli.NO_ARMV64_GLIBC_CLI.contains("Alpine"))
    }

    @Test
    fun theBuildComesFromTheLoaderNotTheDistroName() {
        assertEquals(GuestLibc.MUSL, VsCodeCli.libc(hasMuslLoader = true))
        assertEquals(GuestLibc.GNU, VsCodeCli.libc(hasMuslLoader = false))
        assertEquals(GuestLibc.MUSL, VsCodeCli.libcFromProbe("PHONIX_LIBC=MUSL"))
        assertEquals(GuestLibc.GNU, VsCodeCli.libcFromProbe("PHONIX_LIBC=GNU"))
    }

    @Test
    fun everyScriptIsOneLineBecauseTheShellTakesOneCommand() {
        val scripts = listOf(
            VsCodeCli.detectLibcScript(),
            VsCodeCli.installScript(GuestLibc.GNU),
            VsCodeCli.installScript(GuestLibc.MUSL),
            VsCodeCli.serveCommand()
        )
        for (script in scripts) {
            assertTrue("newline in: " + script, !script.contains("\n"))
        }
    }

    @Test
    fun theInstallScriptAsksForTheBuildItWasGivenAndEndsByPrintingTheVersion() {
        val script = VsCodeCli.installScript(GuestLibc.MUSL)
        assertTrue(script, script.contains("os=cli-alpine-arm64"))
        assertTrue(script, script.contains("tar -xzf cli.tar.gz"))
        assertTrue(script, script.endsWith("./code --version"))
    }

    @Test
    fun theFetchWorksOnTheBundledAlpineImageWhichHasNoCurl() {
        // Measured from app/src/main/assets/alpine-rootfs.tgz: usr/bin/wget, sbin/apk, bin/tar,
        // bin/busybox, lib/ld-musl-aarch64.so.1 - and no curl anywhere in the archive.
        val fetch = VsCodeCli.fetchCommand(GuestLibc.MUSL)
        assertTrue(fetch, fetch.contains("command -v curl"))
        assertTrue("must fall back to busybox wget", fetch.contains("wget -q -O cli.tar.gz"))
        assertTrue(fetch, fetch.contains("os=cli-alpine-arm64"))
    }

    @Test
    fun theFetchAsksTheOneUrlMicrosoftServes() {
        val fetch = VsCodeCli.fetchCommand(GuestLibc.MUSL)
        assertEquals(2, Regex("os=cli-alpine-arm64").findAll(fetch).count())
    }

    @Test
    fun theServerBindsLoopbackOnTheFixedPortWithoutAToken() {
        val command = VsCodeCli.serveCommand()
        assertTrue(command, command.contains("serve-web"))
        assertTrue(command, command.contains("--host 127.0.0.1"))
        assertTrue(command, command.contains("--port 8000"))
        assertTrue(command, command.contains("--without-connection-token"))
        assertTrue("must detach", command.trimEnd().endsWith("&"))
    }

    @Test
    fun theVersionIsReadFromTheShapeTheCliActuallyPrints() {
        assertEquals("1.109.5", VsCodeCli.parseVersion("1.109.5" + System.lineSeparator() + "0e1a5c" + System.lineSeparator() + "arm64"))
        // Measured on the Alpine guest, 2026-11-02.
        val real = "code 1.144.0 (commit 2c33b50b16c472efe58357d1d2a8a5414bc23fbf)"
        assertEquals("1.144.0", VsCodeCli.parseVersion(real))
    }

    @Test
    fun theLastWordsNameWhatFailedAndStayShort() {
        assertEquals(" The guest printed nothing.", VsCodeCli.lastWords(""))
        val two = "a" + System.lineSeparator() + "b" + System.lineSeparator()
        val tail = VsCodeCli.lastWords(two)
        assertTrue(tail, tail.contains("Guest said: a | b"))
        val long = VsCodeCli.lastWords("x".repeat(400))
        assertTrue(long, long.length < 260)
        assertTrue(long, long.endsWith("..."))
    }

    @Test
    fun outputWithoutAVersionLineIsNotAnInstall() {
        assertNull(VsCodeCli.parseVersion("bash: ./code: No such file or directory"))
        assertNull(VsCodeCli.parseVersion("curl: (22) The requested URL returned error: 404"))
        assertNull("a URL is not a version", VsCodeCli.parseVersion("https://192.168.1.5/pack"))
        assertNull(VsCodeCli.parseVersion(""))
    }
}
