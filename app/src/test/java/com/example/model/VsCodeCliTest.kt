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

    fun onlyTheMuslBuildIsPublishedForArm64() {
        assertTrue(GuestLibc.MUSL.supportsCli)
        assertTrue(GuestLibc.MUSL.downloadUrl, GuestLibc.MUSL.downloadUrl.endsWith("os=cli-alpine-arm64"))
        assertTrue(GuestLibc.MUSL.downloadUrl.startsWith("https://"))
        // Measured 2026-11-02: the glibc arm64 URL answers HTTP 404.
        assertFalse(GuestLibc.GNU.supportsCli)
    }

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
    fun theVersionIsTheSemverLineTheCliPrints() {
        assertEquals("1.109.5", VsCodeCli.parseVersion("1.109.5\n0e1a5c2f0e5e0c9d0b1a\narm64\n"))
        assertEquals("1.96.2", VsCodeCli.parseVersion("1.96.2\n"))
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
        assertNull(VsCodeCli.parseVersion(""))
    }
}
