package com.example.model

data class PartitionConfig(
    val totalDeviceStorageGb: Float = 32.0f,
    val androidSystemUsedGb: Float = 11.4f,
    val allocatedLinuxGb: Float = 12.0f,
    val allocatedSwapGb: Float = 2.0f,
    val filesystemType: String = "ext4",
    val containerImagePath: String = "/data/data/com.example/rootfs.img",
    val swapFilePath: String = "/data/data/com.example/swap.img",
    val sharedStorageMount: String = "/sdcard/PhonixShare",
    val ioOptimization: String = "noatime,nodiratime,barrier=0", // Reduces flash wear on older eMMC chips
    val isPartitionMounted: Boolean = false,
    val isExt4Formatted: Boolean = false,
    val zRamEnabled: Boolean = true,
    val readOnlyRootfsOption: Boolean = false // OverlayFS for crash-proof 24/7 servers
) {
    val freeStorageGb: Float
        get() = (totalDeviceStorageGb - androidSystemUsedGb - allocatedLinuxGb - allocatedSwapGb).coerceAtLeast(0.5f)

    val linuxUsedStorageGb: Float
        get() = (allocatedLinuxGb * 0.42f) // approx 42% of allocated container used initially
}

enum class VirtualizationBackend(val displayName: String, val description: String) {
    PROOT_ACCEL("PRoot + DRI3", "User-space chroot emulator with VirGL / Turnip hardware acceleration. No root required."),
    CHROOT_DIRECT("Chroot Native", "Fastest raw I/O execution with direct kernel namespaces. Requires root if available."),
    KVM_CONTAINER("MicroVM / KVM", "Full hypervisor isolation with simulated ACPI & PCI devices for older 64-bit devices.")
}
