package com.example.model

enum class WindowType(val defaultTitle: String, val iconName: String) {
    BROWSER("Phonix Web Browser (Chromium / Firefox)", "browser"),
    DISTRO_STORE("Linux Distro Downloader & Hub", "distro"),
    PERSONALIZE("Desktop Personalization & Themes", "brush"),
    TERMINAL("Phonix Terminal (bash)", "terminal"),
    SERVER_MANAGER("Server Control Center", "server"),
    RESOURCE_MONITOR("System Resource Monitor (htop)", "monitor"),
    FILE_EXPLORER("Phonix File Manager", "folder"),
    APP_STORE("Software & Server Stacks", "store"),
    SETTINGS("Control Panel", "settings"),
    NOTEPAD("Notepad / Editor", "note"),
    BENCHMARK("VirGL 3D Benchmark", "speed"),
    ABOUT("About Phonix OS", "info")
}

data class DesktopWindow(
    val id: String,
    val type: WindowType,
    val title: String = type.defaultTitle,
    val posX: Float = 24f,
    val posY: Float = 24f,
    val width: Float = 360f,
    val height: Float = 340f,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false,
    val zIndex: Float = 1f
)
