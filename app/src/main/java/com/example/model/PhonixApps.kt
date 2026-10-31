package com.example.model

/**
 * An app Phonix ships, independent of where the user chose to show it.
 *
 * The desktop icon list and the taskbar pin list are layouts: a user may remove an icon or unpin
 * an entry, and that must never delete the app. Start and All apps read this catalog instead, so an
 * app removed from both surfaces is still listed and launchable. Artwork is resolved from
 * [com.example.ui.components.AppIconGlyphs] by [windowType], so every surface shows one icon.
 */
data class PhonixApp(
    val id: String,
    val title: String,
    val subtitle: String,
    val windowType: WindowType?
)

object PhonixApps {

    /** Every app the OS provides. Order is the order Start lists them in. */
    val all: List<PhonixApp> = listOf(
        PhonixApp("app_store", "App Store", "Google Play / F-Droid", WindowType.APP_STORE),
        PhonixApp("app_browser", "Phonix Browser", "Native web browser", WindowType.BROWSER),
        PhonixApp("app_files", "File Explorer", "Guest filesystem", WindowType.FILE_EXPLORER),
        PhonixApp("app_terminal", "Terminal", "Linux shell, proot, no root", WindowType.TERMINAL),
        PhonixApp("app_linux_desktop", "Linux Desktop", "Real X11 apps in a window", WindowType.LINUX_DESKTOP),
        PhonixApp("app_distro", "Linux System", "Distro, packages and disk", WindowType.DISTRO_STORE),
        PhonixApp("app_server", "Server Hub", "Nginx, SSH and Samba", WindowType.SERVER_MANAGER),
        PhonixApp("app_privacy", "Privacy Hub", "Ad blocking and Pi-hole guide", WindowType.PRIVACY),
        PhonixApp("app_monitor", "Resource Monitor", "Live battery and CPU readings", WindowType.RESOURCE_MONITOR),
        PhonixApp("app_debloat", "System Debloater", "Recover RAM and disk", WindowType.DEBLOAT_MANAGER),
        PhonixApp("app_ai", "Local AI", "Offline notes and optional local model", WindowType.AI_ASSISTANT),
        PhonixApp("app_personalize", "Personalize", "Wallpapers, icons and shapes", WindowType.PERSONALIZE),
        PhonixApp("app_notepad", "Notepad Editor", "Quick notes and scripts", WindowType.NOTEPAD),
        PhonixApp("app_benchmark", "Benchmark", "Measured, not estimated", WindowType.BENCHMARK),
        PhonixApp("app_vscode", "Code Studio", "VS Code server on loopback", WindowType.VS_CODE),
        PhonixApp("app_phone", "Phone Screen", "Read-only phone mirror", WindowType.PHONE_MIRROR),
        PhonixApp("app_settings", "Settings", "Android system settings", WindowType.SETTINGS),
        PhonixApp("app_about", "About Phonix OS", "Version, updates and device info", WindowType.ABOUT),
        PhonixApp("app_device_setup", "Device & Display Setup", "Daily driver and compatibility check", null)
    )

    fun byId(id: String): PhonixApp? = all.firstOrNull { it.id == id }
}
