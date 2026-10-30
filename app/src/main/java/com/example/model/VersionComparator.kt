package com.example.model

/** Compares simple dotted release versions numerically, ignoring an optional v prefix. */
object VersionComparator {
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(value: String): List<Int>? = Regex("^v?(\\d+(?:\\.\\d+){0,2})(?:[-+].*)?$", RegexOption.IGNORE_CASE)
            .matchEntire(value.trim())
            ?.groupValues?.get(1)
            ?.split('.')
            ?.map { it.toIntOrNull() ?: return null }

        val candidateParts = parts(candidate) ?: return false
        val currentParts = parts(current) ?: return false
        for (index in 0 until maxOf(candidateParts.size, currentParts.size)) {
            val candidatePart = candidateParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (candidatePart != currentPart) return candidatePart > currentPart
        }
        return false
    }
}
