package com.example.model

/**
 * How much space is actually on the device, and where the Linux guest lives.
 *
 * Phonix deliberately does not partition the phone: the guest is a directory tree inside the
 * app's private storage. The old screen showed a 12 GB "EXT4 container image" and a 2 GB
 * "zRAM swap partition" that never existed, which made the storage figures meaningless.
 */
data class StorageState(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    /** Bytes the Alpine root filesystem currently occupies. */
    val guestBytes: Long = 0L,
    /** Absolute path of the guest rootfs, shown so the user can verify it themselves. */
    val guestPath: String = ""
)
