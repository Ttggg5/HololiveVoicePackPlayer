package com.ttggg5.holovoice.data.talents

/** Rules for talents the user adds themselves (see [TalentDirectory.addCustom]). */
object CustomTalents {
    private const val PREFIX = "custom-"

    /** A slug that can't clash with the official ones (which never start with [PREFIX]) or with [taken]. */
    fun slug(now: Long, taken: Set<String>): String {
        var n = now
        while ("$PREFIX$n" in taken) n++
        return "$PREFIX$n"
    }

    fun isCustom(slug: String?): Boolean = slug?.startsWith(PREFIX) == true

    /** Trimmed name with inner runs of spaces collapsed. */
    fun clean(name: String): String = name.trim().replace(Regex("\\s+"), " ")

    /** [clean]ed, or null when blank. */
    fun cleanOrNull(name: String?): String? = name?.let(::clean)?.takeIf { it.isNotEmpty() }

    /** A talent needs an English (or romaji) name. */
    fun isValidName(name: String): Boolean = clean(name).isNotEmpty()
}
