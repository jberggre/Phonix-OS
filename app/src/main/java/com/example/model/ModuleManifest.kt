package com.example.model

import java.io.File
import java.util.Locale

/**
 * A module declared as data instead of compiled into [ModuleRegistry].
 *
 * The format is deliberately small, like `#!phonix-pack`, because a manifest is a declaration and
 * not a program: it names a module, the guest packages that module needs, and - optionally - one
 * service the supervisor may keep alive.
 *
 *     #!phonix-module 1
 *     module = servers
 *     window = server_manager        # optional; must agree with the module's own window
 *     packages = busybox, nginx      # optional
 *     service = manifest-probe       # optional block
 *     name = Manifest Probe
 *     port = 8099                    # omit for a process-only service
 *     process = httpd
 *     probe = http                  # optional; http|process, must agree with port
 *     launch = httpd -f -p 8099 -h /tmp/phroot
 *     stop = for p in $(pidof httpd); do kill $p; done
 *
 * **Why a manifest cannot unblock a module.** Whether the phone has a guest, or device ownership,
 * is measured from the phone ([ModuleFacts]); a file on disk must never be able to claim it. So a
 * manifest annotates a module with the packages it needs, and can never change the refusal a module
 * already gives. That keeps the capability semantics of the built-in registry exactly as they are.
 *
 * **Why a bad manifest is an error rather than a silent fallback.** A file that cannot be parsed,
 * names a module nobody has, or declares a probe with nothing to ask would otherwise leave a module
 * quietly behaving as if the author's intent had been applied. Instead every rejection is reported
 * ([ManifestIssue]) and the module keeps its built-in declaration, which is the documented
 * behaviour and not "success".
 */
class ModuleManifestException(message: String) : Exception(message)

/** One service a manifest asks the supervisor to keep alive, declared as data. */
data class ManifestService(
    val id: String,
    val name: String,
    val processName: String,
    /** Null means the service has no HTTP surface, so the process is the only signal there is. */
    val port: Int?,
    val launchCommand: String,
    val stopCommand: String
) {
    /** The probe the supervisor will use: a port means HTTP, no port means the process. */
    val probe: ServiceProbe
        get() = port?.let { ServiceProbe.Http(it) } ?: ServiceProbe.Process("pidof $processName")

    /**
     * The Server Hub entry for this service, so a declared service is startable and tappable in the
     * same place as a catalog one. A manifest service has no Alpine package of its own, which is
     * why [packages] is empty for most of them.
     */
    fun toServerServiceItem(packages: List<String>): ServerServiceItem = ServerServiceItem(
        id = id,
        name = name,
        category = "Manifest",
        port = port ?: 0,
        protocol = if (port != null) "HTTP" else "PROCESS",
        apkPackage = packages.firstOrNull() ?: "",
        processName = processName,
        description = "Declared by a module manifest. " +
            (port?.let { "Answers on port $it." } ?: "Has no port; liveness is the process."),
        internalPath = "",
        // No package means nothing to install, so the card must not offer to install "".
        isInstalled = packages.isEmpty(),
        declaredLaunch = launchCommand
    )
}

data class ModuleManifest(
    val source: String,
    val module: PhonixModule,
    val packages: List<String> = emptyList(),
    val service: ManifestService? = null,
    /**
     * A pack id from [PackTrust]. The module this manifest describes stays blocked until that pack's
     * pinned digest has verified, so a declaration can require content without vouching for it.
     */
    val pack: String? = null
)

/** A rejection with the file it came from, so the user can fix the file rather than guess. */
data class ManifestIssue(val source: String, val message: String) {
    override fun toString(): String = "$source: $message"
}

/**
 * The result of reading every manifest on disk.
 *
 * [manifests] are the accepted declarations; [issues] are everything rejected. Both are kept: a
 * single bad file must not hide the good ones, and it must not be forgotten either.
 */
data class ManifestSet(
    val manifests: List<ModuleManifest> = emptyList(),
    val issues: List<ManifestIssue> = emptyList()
) {
    val services: List<ManifestService> get() = manifests.mapNotNull { it.service }

    /** Packages every accepted manifest adds to [moduleId], in declaration order, without repeats. */
    fun packagesFor(moduleId: String): List<String> =
        manifests.filter { it.module.id == moduleId }.flatMap { it.packages }.distinct()

    fun packagesByModule(): Map<String, List<String>> =
        manifests.map { it.module.id }.distinct()
            .associateWith { packagesFor(it) }
            .filterValues { it.isNotEmpty() }

    fun service(id: String): ManifestService? = services.firstOrNull { it.id == id }

    /** Every pack any accepted manifest names, in file order, without repeats. */
    fun packs(): List<String> = manifests.mapNotNull { it.pack }.distinct()

    fun packsFor(moduleId: String): List<String> =
        manifests.filter { it.module.id == moduleId }.mapNotNull { it.pack }.distinct()
}

object ModuleManifestFormat {

    const val HEADER = "#!phonix-module"
    const val VERSION = 1

    /** A declaration, not a script bundle: 16 KiB is already ~200 lines of comments. */
    const val MAX_CHARS = 16 * 1024

    /** A module needing more than 32 packages is describing an image, not a window. */
    const val MAX_PACKAGES = 32

    private const val MAX_TOKEN = 64

    /** Guest package and process names become shell words, so only these characters are allowed. */
    private val TOKEN = Regex("[a-z0-9][a-z0-9._+-]{0,$MAX_TOKEN}")

    private val SERVICE_ID = Regex("[a-z0-9][a-z0-9_-]{0,$MAX_TOKEN}")

    /** Pack ids name a row of [PackTrust]; they are never a path or a URL. */
    private val PACK_ID = Regex("[a-z0-9][a-z0-9._-]{0,$MAX_TOKEN}")

    private val KEYS = setOf(
        "module", "window", "pack", "packages", "service", "name", "port", "process", "probe", "launch", "stop"
    )

    private val SERVICE_KEYS = setOf("service", "name", "port", "process", "probe", "launch", "stop")

    fun parse(source: String, text: String): ModuleManifest {
        fun fail(message: String): Nothing = throw ModuleManifestException("$source: $message")

        // Bounds first: a manifest is read into memory, so an enormous file is refused by size
        // rather than by running out of it.
        if (text.length > MAX_CHARS) {
            fail("is ${text.length} characters; the limit is $MAX_CHARS.")
        }

        val lines = text.removePrefix("\uFEFF").split("\n")
        var index = 0
        while (index < lines.size && lines[index].isBlank()) index++
        val header = lines.getOrNull(index)?.trim()
        if (header == null || !header.startsWith(HEADER)) {
            fail("must start with \"$HEADER $VERSION\".")
        }
        val version = header.removePrefix(HEADER).trim()
        if (version != VERSION.toString()) {
            // A future format read as this one would apply half of it, which is worse than refusing.
            fail("declares format version \"$version\"; this build reads $VERSION.")
        }
        index++

        val values = LinkedHashMap<String, String>()
        for (lineNumber in index until lines.size) {
            // `#` starts a comment anywhere on the line, so a value cannot contain one.
            val line = lines[lineNumber].substringBefore('#').trim()
            if (line.isEmpty()) continue
            val equals = line.indexOf('=')
            if (equals <= 0) {
                fail("line ${lineNumber + 1} is not \"key = value\": \"$line\"")
            }
            val key = line.substring(0, equals).trim().lowercase(Locale.ROOT)
            val value = line.substring(equals + 1).trim()
            if (key !in KEYS) {
                // An unknown key is almost always a typo, and a typo that is ignored does nothing.
                fail("line ${lineNumber + 1} uses unknown key \"$key\"; this build reads ${KEYS.sorted().joinToString(", ")}.")
            }
            if (values.containsKey(key)) {
                // Last-wins would silently drop the first value, which is how two authors disagree.
                fail("line ${lineNumber + 1} repeats \"$key\"; a key may appear once.")
            }
            if (value.isEmpty()) {
                fail("line ${lineNumber + 1} gives \"$key\" no value.")
            }
            values[key] = value
        }

        val moduleId = values["module"]
            ?: fail("has no \"module\" line; a manifest must name the module it describes.")
        val module = PhonixModule.entries.firstOrNull { it.id == moduleId.lowercase(Locale.ROOT) }
            ?: fail(
                "\"$moduleId\" is not a module id; known ids are " +
                    PhonixModule.entries.joinToString(", ") { it.id } + "."
            )

        values["window"]?.let { declared ->
            val expected = module.windowType.name.lowercase(Locale.ROOT)
            if (declared.lowercase(Locale.ROOT) != expected) {
                // A manifest that names a window its module does not own is a lie about the app.
                fail("declares window \"$declared\", but module \"${module.id}\" is $expected.")
            }
        }

        val packages = values["packages"]
            ?.split(',', ' ', '\t')
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()
        if (packages.size > MAX_PACKAGES) {
            fail("declares ${packages.size} packages; the limit is $MAX_PACKAGES.")
        }
        packages.firstOrNull { !TOKEN.matches(it) }?.let { bad ->
            // These strings are pasted into guest shell commands, so anything else could be a second
            // command rather than a package name.
            fail("\"$bad\" is not a package name; use lowercase letters, digits, dot, dash, plus or underscore.")
        }

        val pack = values["pack"]?.lowercase(Locale.ROOT)?.also { declared ->
            if (!PACK_ID.matches(declared)) {
                fail("\"$declared\" is not a pack id; use lowercase letters, digits, dot, dash or underscore.")
            }
        }

        val declaresService = SERVICE_KEYS.any { values.containsKey(it) }
        if (!declaresService) {
            return ModuleManifest(source = source, module = module, packages = packages, pack = pack)
        }

        val serviceId = values["service"]
            ?: fail("declares service settings but no \"service\" id.")
        if (!SERVICE_ID.matches(serviceId)) {
            fail("service id \"$serviceId\" must be lowercase letters, digits, dash or underscore.")
        }
        if (DefaultServerCatalog.getDefaultServices().any { it.id == serviceId }) {
            fail("\"$serviceId\" is a built-in service id; a manifest cannot shadow one.")
        }

        val processName = values["process"]
            ?: fail("service \"$serviceId\" has no \"process\"; nothing could be probed or stopped.")
        if (!TOKEN.matches(processName)) {
            fail("\"$processName\" is not a process name; use lowercase letters, digits, dot, dash, plus or underscore.")
        }

        val port = values["port"]?.let { raw ->
            val parsed = raw.toIntOrNull() ?: fail("port \"$raw\" is not a number.")
            // Real ports are 1..65535 inclusive: 0 is not a listening port, and 65536 does not fit
            // on the wire. Both are refused rather than clamped.
            if (parsed !in 1..65535) fail("port $parsed is outside 1-65535.")
            parsed
        }

        val probeKind = values["probe"]?.lowercase(Locale.ROOT)
        if (probeKind != null && probeKind != "http" && probeKind != "process") {
            fail("probe \"$probeKind\" is not one of http, process.")
        }
        if (probeKind == "http" && port == null) {
            // The failure the brief names: a probe that cannot be evaluated is an error, not a
            // healthy default.
            fail("declares an http probe but no port, so there is nothing to ask.")
        }
        if (probeKind == "process" && port != null) {
            fail("declares a process probe but also port $port, which would never be asked.")
        }
        if (probeKind == null && port != null) {
            // Documented default rather than a guess: a port is the stronger signal, so it is used.
            // Both branches are evaluable, which is the property that matters.
            // (Nothing is silently accepted here: the port itself was validated above.)
        }

        val launch = values["launch"]
            ?: fail("service \"$serviceId\" has no \"launch\"; a supervised service must be startable.")
        val stop = values["stop"] ?: defaultStop(processName)
        val name = values["name"] ?: serviceId

        return ModuleManifest(
            source = source,
            module = module,
            packages = packages,
            pack = pack,
            service = ManifestService(
                id = serviceId,
                name = name,
                processName = processName,
                port = port,
                launchCommand = launch,
                stopCommand = stop
            )
        )
    }

    /**
     * The stop command used when a manifest omits one. Deriving it is safe precisely because
     * [processName] was validated against [TOKEN] above: there is nothing in it to interpret.
     */
    fun defaultStop(processName: String): String =
        "for p in \$(pidof $processName); do kill \$p 2>/dev/null; done; sleep 1; true"
}

object ModuleManifests {

    /**
     * Parses every `source -> text` entry. A file is accepted or refused as a whole: half a manifest
     * applied (the packages but not the service) would be a state nobody declared.
     */
    fun resolve(
        entries: List<Pair<String, String>>,
        /**
         * The ids this build has a pinned digest for. A manifest naming anything else is reported,
         * because a pack with no digest can never be installed and must not look merely unpinned.
         */
        knownPacks: Set<String> = PackTrust.shipped.map { it.id }.toSet()
    ): ManifestSet {
        val accepted = mutableListOf<ModuleManifest>()
        val issues = mutableListOf<ManifestIssue>()
        val claimedServices = mutableSetOf<String>()

        entries.sortedBy { it.first }.forEach { (source, text) ->
            val manifest = try {
                ModuleManifestFormat.parse(source, text)
            } catch (error: ModuleManifestException) {
                issues += ManifestIssue(source, error.message ?: "could not be read.")
                return@forEach
            }
            manifest.pack?.let { declared ->
                if (declared !in knownPacks) {
                    issues += ManifestIssue(
                        source,
                        "names pack \"$declared\", which has no pinned digest in this build; it cannot be installed."
                    )
                    return@forEach
                }
            }
            val service = manifest.service
            if (service != null && !claimedServices.add(service.id)) {
                issues += ManifestIssue(
                    source,
                    "service \"${service.id}\" is already declared by another manifest; this file is refused."
                )
                return@forEach
            }
            accepted += manifest
        }
        return ManifestSet(manifests = accepted, issues = issues)
    }
}

/**
 * Reads manifests from a private-storage directory.
 *
 * Absent directory, absent files and unreadable files all resolve to "no manifests", which is the
 * shipped behaviour: only a file that exists and is wrong is an issue.
 */
class ModuleManifestStore(private val directory: File) {

    fun load(
        /**
         * Ids this build can verify. The default is the shipped table; a debug build passes the
         * catalogue with its bench controls, so a negative control can be exercised on hardware.
         */
        knownPacks: Set<String> = PackTrust.shipped.map { it.id }.toSet()
    ): ManifestSet {
        val files = runCatching { directory.listFiles() }
            .getOrNull()
            ?.filter { it.isFile }
            ?.sortedBy { it.name }
            ?: return ManifestSet()

        val issues = mutableListOf<ManifestIssue>()
        val entries = mutableListOf<Pair<String, String>>()
        files.forEach { file ->
            if (file.length() > ModuleManifestFormat.MAX_CHARS) {
                // Checked before readText so a stray large file cannot exhaust the heap.
                issues += ManifestIssue(
                    file.name,
                    "is ${file.length()} bytes; the limit is ${ModuleManifestFormat.MAX_CHARS}."
                )
                return@forEach
            }
            val text = runCatching { file.readText() }.getOrElse { error ->
                issues += ManifestIssue(file.name, "could not be read: ${error.message ?: error.javaClass.simpleName}")
                null
            } ?: return@forEach
            entries += file.name to text
        }

        val resolved = ModuleManifests.resolve(entries, knownPacks)
        return ManifestSet(resolved.manifests, issues + resolved.issues)
    }
}
