package com.example

import com.example.engine.LinuxSystemEmulator
import com.example.model.CoolingMode
import com.example.model.DebloatCatalog
import com.example.model.DefaultServerCatalog
import com.example.model.HardwareAccelerationConfig
import com.example.model.PartitionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDebloatCatalogReclaimsOverOneGigabyte() {
        val packages = DebloatCatalog.getDefaultPackages()
        val totalRamReclaimed = packages.filter { it.isEnabled }.sumOf { it.estimatedRamSavedMb }
        // Should reclaim > 1,000 MB RAM for older Samsung devices
        assertTrue("Expected > 1000MB reclaimed RAM, but got $totalRamReclaimed MB", totalRamReclaimed > 1000)

        val hasGos = packages.any { it.packageName == "com.samsung.android.game.gos" }
        assertTrue("Samsung GOS package must be present in debloat list", hasGos)

        val hasBixby = packages.any { it.packageName == "com.samsung.android.bixby.agent" }
        assertTrue("Bixby package must be present in debloat list", hasBixby)
    }

    @Test
    fun testLinuxSystemEmulatorCommands() {
        val emulator = LinuxSystemEmulator()
        val partition = PartitionConfig()
        val hw = HardwareAccelerationConfig()
        val servers = DefaultServerCatalog.getDefaultServices()

        // Test neofetch output
        val neofetchOut = emulator.executeCommand(
            cmdRaw = "neofetch",
            ipAddress = "192.168.1.105",
            partitionConfig = partition,
            hwConfig = hw,
            services = servers,
            uptimeSeconds = 3600
        )
        assertTrue(neofetchOut.any { it.text.contains("Phonix OS") })

        // Test sensors output for motherboard heatsink thermals
        val sensorsOut = emulator.executeCommand(
            cmdRaw = "sensors",
            ipAddress = "192.168.1.105",
            partitionConfig = partition,
            hwConfig = hw,
            services = servers,
            uptimeSeconds = 3600
        )
        assertTrue(sensorsOut.any { it.text.contains("Sustained 2.45 GHz") || it.text.contains("Active 5V Fan") })

        // Test box64 x86 translation command
        val box64Out = emulator.executeCommand(
            cmdRaw = "box64",
            ipAddress = "192.168.1.105",
            partitionConfig = partition,
            hwConfig = hw,
            services = servers,
            uptimeSeconds = 3600
        )
        assertTrue(box64Out.any { it.text.contains("Box64") && it.text.contains("Dynarec") })
    }

    @Test
    fun testPartitionConfigCalculations() {
        val partition = PartitionConfig(
            totalDeviceStorageGb = 64f,
            androidSystemUsedGb = 12f,
            allocatedLinuxGb = 16f,
            allocatedSwapGb = 2f
        )
        assertEquals(34f, partition.freeStorageGb, 0.01f)
        assertTrue(partition.ioOptimization.contains("barrier=0"))
        assertTrue(partition.ioOptimization.contains("noatime"))
    }

    @Test
    fun testActiveCoolingModeProperties() {
        val activeCooling = CoolingMode.ACTIVE_MODDED_COOLING
        assertTrue("Active cooling mode should bypass thermal throttle", activeCooling.isThrottleBypassed)
        assertEquals(2.45f, activeCooling.maxClockGhz, 0.01f)

        val passiveMode = CoolingMode.STOCK_PASSIVE
        assertFalse("Passive mode should enforce thermal throttling", passiveMode.isThrottleBypassed)
    }
}
