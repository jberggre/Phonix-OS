package com.example.model

import com.squareup.moshi.JsonReader
import okio.Buffer

/**
 * One published release, as the update feed describes it.
 *
 * The feed is a single JSON document:
 *
 * ```json
 * {
 *   "versionCode": 7,
 *   "versionName": "1.6",
 *   "apkUrl": "https://example.org/phonix/phonix-1.6.apk",
 *   "sha256": "<64 hex characters>",
 *   "notes": "What changed",
 *   "published": "2026-11-02"
 * }
 * ```
 *
 * `versionCode` — never `versionName` — decides whether a build is newer. Comparing names put
 * "1.9" above "1.10" and would strand a unit on an old build while reporting it current.
 *
 * `sha256` is required. The installer refuses an archive it cannot verify, so a truncated or
 * substituted download is never handed to Android's package installer as an update.
 */
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: String = "",
    val published: String = ""
)

/**
 * Parsing and validation for [UpdateManifest].
 *
 * This is deliberately pure JVM code (Moshi's streaming reader, no Android types) so the rules
 * that decide whether an update is offered can be unit-tested without a device or a network.
 */
object UpdateFeed {

    /** Refuses anything that is not a published SHA-256 digest. */
    fun isDigest(value: String): Boolean = SHA256.matches(value)

    /** True only when the feed's build number is strictly greater than what is installed. */
    fun isNewer(manifest: UpdateManifest, currentVersionCode: Int): Boolean =
        manifest.versionCode > currentVersionCode

    /**
     * Parses a feed document, or returns null when a required field is missing or malformed.
     * A null result is the whole point: an unusable feed must not be read as "up to date".
     */
    fun parse(json: String): UpdateManifest? {
        val root = runCatching {
            JsonReader.of(Buffer().writeUtf8(json)).use { reader ->
                reader.isLenient = true
                reader.readJsonValue()
            }
        }.getOrNull() as? Map<*, *> ?: return null

        val versionCode = when (val raw = root["versionCode"]) {
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull()
            else -> null
        } ?: return null

        val versionName = (root["versionName"] as? String)?.trim().orEmpty()
        val apkUrl = (root["apkUrl"] as? String)?.trim().orEmpty()
        val sha256 = (root["sha256"] as? String)?.trim().orEmpty()

        if (versionCode <= 0) return null
        if (versionName.isEmpty()) return null
        if (!apkUrl.startsWith("https://", ignoreCase = true)) return null
        if (!isDigest(sha256)) return null

        return UpdateManifest(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            sha256 = sha256.lowercase(),
            notes = (root["notes"] as? String).orEmpty(),
            published = (root["published"] as? String).orEmpty()
        )
    }

    private val SHA256 = Regex("^[0-9a-fA-F]{64}$")
}
