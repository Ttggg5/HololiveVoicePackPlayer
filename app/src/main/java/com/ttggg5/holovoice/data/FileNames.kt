package com.ttggg5.holovoice.data

import java.io.File

/** Pure helpers for turning file names into catalog data. No Android dependencies, so they are unit tested. */
object FileNames {
    val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "ogg", "oga", "opus", "wav", "flac")
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    private val COVER_HINTS = listOf("cover", "jacket", "thumb", "folder", "front", "art")

    private val TRACK_PREFIX = Regex("""^\s*(?:track\s*)?(\d{1,3})(?:\s*[._\-)\]:]+\s*|\s+|$)(.*)$""", RegexOption.IGNORE_CASE)

    const val UNKNOWN_TALENT = "Unknown"

    fun extension(name: String): String = name.substringAfterLast('.', "").lowercase()

    fun baseName(name: String): String =
        name.substringAfterLast('/').let { if ('.' in it) it.substringBeforeLast('.') else it }

    fun isAudio(name: String): Boolean = extension(name) in AUDIO_EXTENSIONS

    fun isImage(name: String): Boolean = extension(name) in IMAGE_EXTENSIONS

    /** Files that archivers or OSes leave behind and that should never be imported. */
    fun isJunk(path: String): Boolean =
        path.split('/').any { it == "__MACOSX" || it.startsWith("._") || it == ".DS_Store" }

    /** Splits "01_おはよう.mp3" into (1, "おはよう"). Names without a leading number get a null track. */
    fun parseClip(fileName: String): Pair<Int?, String> {
        val base = baseName(fileName).trim()
        val match = TRACK_PREFIX.matchEntire(base)
        if (match != null) {
            val track = match.groupValues[1].toInt()
            val rest = cleanTitle(match.groupValues[2])
            return track to rest.ifEmpty { base }
        }
        return null to cleanTitle(base).ifEmpty { base }
    }

    private fun cleanTitle(raw: String): String = raw.replace('_', ' ').replace(Regex("""\s+"""), " ").trim()

    /**
     * Guesses the talent from a pack (folder/zip) name like "Gawr Gura - Birthday 2023"
     * or "【兎田ぺこら】ボイス"; falls back to the parent folder name, then "Unknown".
     */
    fun guessTalent(packName: String, parentName: String?): String {
        Regex("""^[【\[](.+?)[】\]]""").find(packName.trim())?.let { return it.groupValues[1].trim() }
        val dash = packName.split(" - ", " – ", " — ").map { it.trim() }
        if (dash.size > 1 && dash[0].isNotEmpty()) return dash[0]
        if (!parentName.isNullOrBlank()) return parentName.trim()
        return UNKNOWN_TALENT
    }

    /** Picks the most cover-like image from a list of file names, or null. */
    fun pickCover(imageNames: List<String>): String? =
        imageNames.firstOrNull { name -> COVER_HINTS.any { baseName(name).lowercase().contains(it) } }
            ?: imageNames.minOrNull()

    /** Resolves a ZIP entry inside [destDir], rejecting entries that would escape it (zip slip). */
    fun safeResolve(destDir: File, entryName: String): File? {
        val root = destDir.canonicalFile
        val target = File(root, entryName).canonicalFile
        return if (target.path.startsWith(root.path + File.separator)) target else null
    }
}
