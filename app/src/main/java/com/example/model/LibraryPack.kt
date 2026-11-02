package com.example.model

import java.io.File
import java.util.Locale

/**
 * An imported offline-library pack.
 *
 * The project hosts nothing, so a pack is a UTF-8 text file the owner writes, types or receives
 * from someone else. The format is deliberately small enough to author on the phone itself:
 *
 *     #!phonix-pack 1
 *     @pack River field notes
 *     @shelf WATER
 *     @title Rainwater harvesting
 *     Line one of the body.
 *     @shelf FIRST_AID
 *     @title Cleaning a wound
 *     ...
 *
 * `@shelf` starts a new document and names the [KnowledgeCategory] it belongs to; `@title` names
 * the document that follows. An unknown shelf is an error rather than a silently empty shelf,
 * because a typo that quietly discards an imported document is worse than a refusal.
 */
class LibraryPackException(message: String) : Exception(message)

/** One document inside an installed pack, identified by its pack and its position in the file. */
data class PackDoc(
    val id: String,
    val packId: String,
    val packTitle: String,
    val category: KnowledgeCategory,
    val title: String,
    val body: String
) {
    /**
     * Pack documents are reported as Lightweight so they appear in every tier: imports are the
     * owner's own material and should never be hidden by a size setting.
     */
    fun toKnowledgeDoc(): KnowledgeDoc = KnowledgeDoc(
        id = id,
        category = category,
        title = title,
        summary = "Imported pack: $packTitle ($title)",
        body = body,
        keywords = (title.lowercase(Locale.ROOT).split(Regex("[^a-z0-9]+")) + category.name.lowercase(Locale.ROOT))
            .filter { it.length > 2 }
            .distinct(),
        tier = KnowledgeTier.LIGHTWEIGHT
    )
}

object LibraryPackFormat {

    const val HEADER = "#!phonix-pack"

    /** Guards against importing a multi-hundred-megabyte file as a "document". */
    const val MAX_CHARS = 8 * 1024 * 1024

    fun parse(packId: String, text: String): List<PackDoc> {
        if (text.length > MAX_CHARS) {
            throw LibraryPackException(
                "That pack is ${text.length} characters; the limit is $MAX_CHARS."
            )
        }
        val lines = text.removePrefix("\uFEFF").split("\n")
        var index = 0
        while (index < lines.size && lines[index].isBlank()) index++
        if (index >= lines.size || !lines[index].trim().startsWith(HEADER)) {
            throw LibraryPackException("A pack must start with \"$HEADER 1\".")
        }
        index++

        val docs = mutableListOf<PackDoc>()
        var packTitle = displayName(packId)
        var shelf: KnowledgeCategory? = null
        var title: String? = null
        val body = StringBuilder()

        fun flush() {
            val currentShelf = shelf ?: return
            val currentTitle = title
            if (currentTitle.isNullOrBlank()) {
                throw LibraryPackException("A document on ${currentShelf.title} has no @title.")
            }
            val text = body.toString().trim()
            if (text.isEmpty()) {
                throw LibraryPackException("\"$currentTitle\" has no body.")
            }
            docs += PackDoc(
                id = "pack:$packId:${docs.size}",
                packId = packId,
                packTitle = packTitle,
                category = currentShelf,
                title = currentTitle,
                body = text
            )
            body.setLength(0)
            title = null
        }

        for (raw in lines.drop(index)) {
            val line = raw.trimEnd('\r')
            val trimmed = line.trim()
            when {
                trimmed.startsWith("@shelf", ignoreCase = true) -> {
                    flush()
                    val name = trimmed.substringAfter("@shelf").trim()
                    shelf = KnowledgeCategory.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?: throw LibraryPackException(
                            "\"$name\" is not a shelf. Use one of: " +
                                KnowledgeCategory.entries.joinToString(", ") { it.name } + "."
                        )
                }
                trimmed.startsWith("@title", ignoreCase = true) -> {
                    // A second @title closes the previous document only if it already has one;
                    // otherwise this is the first title after a new @shelf, which has no body yet.
                    if (title != null) flush()
                    val parsedTitle = trimmed.substringAfter("@title").trim()
                    if (parsedTitle.isBlank()) throw LibraryPackException("An @title line is empty.")
                    title = parsedTitle
                }
                trimmed.startsWith("@pack", ignoreCase = true) -> {
                    val name = trimmed.substringAfter("@pack").trim()
                    if (name.isNotEmpty()) packTitle = name
                }
                else -> if (shelf != null) body.append(line).append('\n')
            }
        }
        flush()

        if (docs.isEmpty()) throw LibraryPackException("The pack contains no documents.")
        return docs
    }

    /** Human name for a pack whose header carries no `@pack` line. */
    fun displayName(packId: String): String =
        packId.replace('-', ' ').replace('_', ' ').trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            .ifBlank { "Imported pack" }
}

/** A pack as it exists on disk: what the library screen lists and can delete. */
data class InstalledPack(
    val id: String,
    val title: String,
    val docCount: Int,
    val bytes: Long
)

/**
 * Packs live in one directory of the app's private storage, one file each, so deleting a pack is a
 * single file delete and the space is really returned. Nothing here downloads: a pack arrives as
 * bytes the caller already has, which keeps the install path testable with no network.
 */
class LibraryPackStore(private val root: File) {

    fun packs(): List<InstalledPack> = packFiles().mapNotNull { file -> read(file)?.let { (pack, docs) ->
        InstalledPack(pack.first, pack.second, docs.size, file.length())
    } }.sortedBy { it.title }

    /** Every document of every installed pack, in pack-name order. */
    fun docs(): List<PackDoc> = packFiles().flatMap { file -> read(file)?.let { it.second } ?: emptyList() }

    /** Total bytes the packs occupy on disk. */
    fun bytes(): Long = packFiles().sumOf { it.length() }

    /**
     * Writes [text] as a pack named after [fileName]. An existing pack with the same identifier is
     * replaced, and the new file only appears once it has parsed, so a bad pack cannot evict a good
     * one.
     */
    fun install(fileName: String, text: String): InstalledPack {
        val id = idFrom(fileName)
        val docs = LibraryPackFormat.parse(id, text)
        root.mkdirs()
        if (!root.isDirectory) throw LibraryPackException("Could not create the pack directory.")
        val target = File(root, id + SUFFIX)
        val part = File(root, id + SUFFIX + ".part")
        part.delete()
        part.writeText(text)
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
        return InstalledPack(id, docs.first().packTitle, docs.size, target.length())
    }

    /** Removes a pack and returns whether a file was actually deleted. */
    fun delete(id: String): Boolean {
        val file = File(root, id + SUFFIX)
        return file.isFile && file.delete()
    }

    private fun packFiles(): List<File> =
        (root.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(SUFFIX) }
            .sortedBy { it.name }

    private fun read(file: File): Pair<Pair<String, String>, List<PackDoc>>? = runCatching {
        val id = file.name.removeSuffix(SUFFIX)
        val docs = LibraryPackFormat.parse(id, file.readText())
        (id to docs.first().packTitle) to docs
    }.getOrNull()

    companion object {
        const val SUFFIX = ".pack"

        /** A file-system-safe identifier derived from the imported file's name. */
        fun idFrom(fileName: String): String {
            val base = fileName.substringBeforeLast('.').lowercase(Locale.ROOT)
            val cleaned = base.replace(Regex("[^a-z0-9_-]+"), "-").trim('-', '_')
            return cleaned.ifBlank { "pack" }
        }
    }
}
