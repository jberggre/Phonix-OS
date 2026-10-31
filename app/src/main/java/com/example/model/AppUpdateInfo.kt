package com.example.model

enum class UpdateCheckState {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    ERROR
}

data class AppUpdateInfo(
    val currentVersionName: String = "1.5",
    val currentVersionCode: Int = 6,
    val latestVersionName: String = "1.5",
    val latestVersionCode: Int = 6,
    val releaseUrl: String = "https://github.com/jberggre/Phonix-OS/releases",
    val apkDownloadUrl: String = "",
    val releaseNotes: String = "",
    val publishedDate: String = "",
    val sha256: String = "",
    val isUpdateAvailable: Boolean = false
) {
    val currentVersion: String get() = currentVersionName
    val latestVersion: String get() = latestVersionName
    val downloadUrl: String get() = if (apkDownloadUrl.isNotEmpty()) apkDownloadUrl else releaseUrl
}
