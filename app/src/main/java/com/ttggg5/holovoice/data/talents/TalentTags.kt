package com.ttggg5.holovoice.data.talents

/**
 * Some names on the talent list carry a status tag, e.g. "[Alum] Kiryu Coco" or "【卒業】桐生ココ".
 * [split] separates the plain name from its tags so the name reads cleanly and the tag can be shown
 * on its own (as a badge).
 */
object TalentTags {
    private val LEADING = Regex("""^\s*[\[【(（〔]([^\]】)）〕]{1,20})[\]】)）〕]\s*""")
    private val TRAILING = Regex("""\s*[\[【(（〔]([^\]】)）〕]{1,20})[\]】)）〕]\s*$""")
    private val ALUM = Regex("""(?i)^(alum|alumni|alumna|graduated?|graduation|卒業|卒業生)$""")

    /** "[Alum] Kiryu Coco" → ("Kiryu Coco", ["Alum"]). Names without tags come back unchanged. */
    fun split(raw: String): Pair<String, List<String>> {
        var name = raw.trim()
        val tags = mutableListOf<String>()
        while (true) {
            val m = LEADING.find(name) ?: break
            if (m.range.last + 1 >= name.length) break // nothing would be left of the name
            tags += label(m.groupValues[1])
            name = name.substring(m.range.last + 1)
        }
        while (true) {
            val m = TRAILING.find(name) ?: break
            if (m.range.first == 0) break
            tags.add(0, label(m.groupValues[1]))
            name = name.substring(0, m.range.first)
        }
        return name.trim() to tags.distinct()
    }

    /** The name without tags. */
    fun clean(raw: String): String = split(raw).first

    private fun label(tag: String): String {
        val t = tag.trim()
        return if (ALUM.matches(t)) "Alum" else t
    }
}
