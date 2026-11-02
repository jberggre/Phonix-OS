package com.example.model

import java.util.Locale

/**
 * What the host can actually do right now.
 *
 * Every value is measured by the caller from a real query - a guest tree that exists, a device-owner
 * install, a PackageManager answer - never assumed from a preference. A module that needs something
 * absent must say so, because a tile that opens an empty window teaches the user nothing.
 */
data class ModuleFacts(
    val guestInstalled: Boolean = false,
    val isDeviceOwner: Boolean = false,
    /** Packs whose pinned digest verified and whose bytes are installed. Measured, never declared. */
    val verifiedPacks: Set<String> = emptySet()
)

/**
 * A module as the palette sees it: the built-in declaration plus what manifests add to it.
 *
 * Availability is copied from the module, never from a manifest, so a file on disk cannot change
 * what the phone can do - only what the module says it needs.
 */
data class ResolvedModule(
    val module: PhonixModule,
    val packages: List<String>,
    val blockedReason: String?
) {
    val isAvailable: Boolean get() = blockedReason == null
}

/**
 * The module registry: one declaration per [WindowType], carrying the capability it needs.
 *
 * This is the single list the command palette and the start menu read. Before it existed, each
 * surface decided on its own whether to offer a module, which is how the VS Code badge and the
 * Start-menu tiles drifted apart from what the phone could do.
 */
enum class PhonixModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val windowType: WindowType,
    val needsGuest: Boolean = false,
    val needsDeviceOwner: Boolean = false,
    /** A measured limitation worth stating in the palette rather than hiding. */
    val caveat: String? = null
) {
    BROWSER("browser", "Phonix Browser", "Web window on the desktop", WindowType.BROWSER),
    TERMINAL("terminal", "Terminal", "Shell inside the active guest", WindowType.TERMINAL, needsGuest = true),
    DISTRO_STORE("distros", "Linux Systems", "Install, switch or remove a distribution", WindowType.DISTRO_STORE),
    LINUX_DESKTOP("linux-desktop", "Linux Desktop", "The guest's X display over loopback VNC", WindowType.LINUX_DESKTOP, needsGuest = true),
    FILE_EXPLORER("files", "File Manager", "App storage and the guest tree", WindowType.FILE_EXPLORER),
    APP_STORE("stacks", "Software & Stacks", "Server stacks installed inside the guest", WindowType.APP_STORE),
    SERVER_MANAGER("servers", "Servers", "Start and stop guest services", WindowType.SERVER_MANAGER, needsGuest = true),
    RESOURCE_MONITOR("monitor", "Resource Monitor", "CPU, memory and stored bytes", WindowType.RESOURCE_MONITOR),
    VS_CODE(
        "vscode", "VS Code (web)", "Needs code serve-web inside the guest", WindowType.VS_CODE,
        needsGuest = true,
        caveat = "The badge is a real socket probe; no editor has been measured behind it."
    ),
    AI("ai", "Local AI", "Qwen2.5 1.5B running in the guest", WindowType.AI_ASSISTANT, needsGuest = true),
    PHONE_SCREEN("phone-screen", "Phone Screen", "Read-only capture of this phone's display", WindowType.PHONE_MIRROR),
    DEBLOAT(
        "debloat", "Host Debloater", "Hide preinstalled apps on the phone", WindowType.DEBLOAT_MANAGER,
        needsDeviceOwner = true
    ),
    SETTINGS("settings", "Control Panel", "Desktop, taskbar and display options", WindowType.SETTINGS),
    PRIVACY("privacy", "Privacy Hub", "What leaves the phone, and what never does", WindowType.PRIVACY),
    PERSONALIZE("personalize", "Personalization", "Wallpaper, accent and icon size", WindowType.PERSONALIZE),
    NOTEPAD("notepad", "Notepad", "Plain text, stored on this phone", WindowType.NOTEPAD),
    BENCHMARK("benchmark", "Container Benchmark", "Measured guest throughput", WindowType.BENCHMARK),
    ABOUT("about", "About Phonix OS", "Version, licence and the update check", WindowType.ABOUT);

    /** Null when the module can open now; otherwise the reason it cannot. */
    fun blockedReason(facts: ModuleFacts): String? = when {
        needsGuest && !facts.guestInstalled -> "Needs an installed Linux guest."
        needsDeviceOwner && !facts.isDeviceOwner -> "Needs Phonix to be the device owner."
        else -> null
    }

    fun isAvailable(facts: ModuleFacts): Boolean = blockedReason(facts) == null
}

object ModuleRegistry {

    val modules: List<PhonixModule> = PhonixModule.entries

    fun available(facts: ModuleFacts): List<PhonixModule> = modules.filter { it.isAvailable(facts) }

    /** Unavailable modules with their reasons, so the palette can explain instead of omit. */
    fun blocked(facts: ModuleFacts): List<Pair<PhonixModule, String>> =
        modules.mapNotNull { module -> module.blockedReason(facts)?.let { module to it } }

    /**
     * The window a module id opens, or null when no module uses that id.
     *
     * Used by the launch intent so a bench session can open a surface that the pointer cannot
     * reach; the ids are the same ones a manifest may name, and nothing here invents authority.
     */
    fun openable(id: String): WindowType? =
        modules.firstOrNull { it.id == id.trim().lowercase(java.util.Locale.ROOT) }?.windowType

    fun byWindowType(type: WindowType): PhonixModule? = modules.firstOrNull { it.windowType == type }

    /**
     * The registry with manifests applied. With no manifests this is exactly [modules] with their
     * own [PhonixModule.blockedReason], which is what every caller saw before manifests existed.
     */
    fun resolved(facts: ModuleFacts, manifests: ManifestSet = ManifestSet()): List<ResolvedModule> =
        modules.map { module ->
            val unverifiedPack = manifests.packsFor(module.id).firstOrNull { it !in facts.verifiedPacks }
            ResolvedModule(
                module = module,
                packages = manifests.packagesFor(module.id),
                // The module's own refusal wins: a verified pack is a prerequisite, not a capability.
                blockedReason = module.blockedReason(facts)
                    ?: unverifiedPack?.let { "Needs pack \"$it\", which is not downloaded and verified." }
            )
        }

    /**
     * Palette matching. Every whitespace-separated term must appear somewhere in the module's
     * title, subtitle, id or enum name, so "linux desk" and "term" both work; an empty query
     * matches everything, which is what the start menu shows before the user types.
     */
    fun matches(query: String, module: PhonixModule): Boolean {
        val terms = query.lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }
        if (terms.isEmpty()) return true
        val haystack = (module.title + " " + module.subtitle + " " + module.id + " " + module.name)
            .lowercase(Locale.ROOT)
        return terms.all { haystack.contains(it) }
    }

    fun search(query: String, facts: ModuleFacts): List<PhonixModule> =
        available(facts).filter { matches(query, it) }
}
