package com.ttggg5.holovoice.data.talents

/** A talent from the official talent list. [slug] (from the profile URL) is the stable id. */
data class Talent(
    val slug: String,
    val nameEn: String,
    val nameJa: String?,
    val imageUrl: String,
) {
    /** "Usada Pekora (兎田ぺこら)", or just the English name when there is no Japanese one. */
    val displayName: String get() = if (nameJa.isNullOrBlank()) nameEn else "$nameEn ($nameJa)"
}
