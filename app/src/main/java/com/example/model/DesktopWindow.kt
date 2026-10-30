package com.example.model

enum class WindowType(val defaultTitle: String, val iconName: String) {
    BROWSER("Phonix Browser", "browser"),
    LINUX_DESKTOP("Linux Desktop (X11)", "linux"),
    DISTRO_STORE("Linux System", "distro"),
    PERSONALIZE("Desktop Personalization & Themes", "brush"),
    TERMINAL("Phonix Terminal", "terminal"),
    SERVER_MANAGER("Servers", "server"),
    RESOURCE_MONITOR("Resource Monitor", "monitor"),
    FILE_EXPLORER("Phonix File Manager", "folder"),
    APP_STORE("Software & Server Stacks", "store"),
    SETTINGS("Control Panel", "settings"),
    PRIVACY("Privacy Hub", "shield"),
    NOTEPAD("Notepad / Editor", "note"),
    BENCHMARK("Container Benchmark", "speed"),
    ABOUT("About Phonix OS", "info"),
    VS_CODE("VS Code Web IDE", "code"),
    PHONE_MIRROR("Phone Screen (read-only)", "phone"),
    AI_ASSISTANT("Local AI", "smart_toy")
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
