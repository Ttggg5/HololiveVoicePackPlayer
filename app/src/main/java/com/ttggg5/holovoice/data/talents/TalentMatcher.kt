package com.ttggg5.holovoice.data.talents

import java.text.Normalizer

/** Finds the talent whose name is closest to a file name such as "兎田ぺこら" or "Usada Pekora (2)". */
object TalentMatcher {
    private const val THRESHOLD = 0.62

    private val COPY_SUFFIX = Regex("""[\s_\-]*(\(\d+\)|\[\d+]|copy|コピー|\d+)\s*$""", RegexOption.IGNORE_CASE)
    private val NON_LETTER = Regex("""[^\p{L}]""")
    private val SCRIPT_RUN = Regex("""[一-鿿㐀-䶿々]+|[぀-ゟ゠-ヿ]+|[A-Za-z]+""")

    /** NFKC, lowercase, katakana → hiragana, copy/track suffixes and non-letters removed. */
    fun normalize(raw: String): String {
        var s = Normalizer.normalize(raw, Normalizer.Form.NFKC).trim()
        while (true) {
            val stripped = s.replace(COPY_SUFFIX, "").trim()
            if (stripped == s || stripped.isEmpty()) break
            s = stripped
        }
        return toHiragana(s.lowercase()).replace(NON_LETTER, "")
    }

    private fun toHiragana(s: String): String = buildString(s.length) {
        for (c in s) append(if (c in 'ァ'..'ヶ') c - 0x60 else c)
    }

    /** All normalized names a talent can be called by, longest first. */
    fun variants(t: Talent): List<String> {
        val raw = buildList {
            add(t.nameEn)
            add(t.slug)
            val enTokens = t.nameEn.split(' ', '-', '_').filter { it.isNotBlank() }
            if (enTokens.size > 1) add(enTokens.reversed().joinToString(" "))
            addAll(enTokens)
            t.nameJa?.let { ja ->
                add(ja)
                val nfkc = Normalizer.normalize(ja, Normalizer.Form.NFKC)
                addAll(nfkc.split(' ', '・', '　'))
                addAll(SCRIPT_RUN.findAll(nfkc).map { it.value })
            }
        }
        return raw.map(::normalize).filter { it.length >= 2 }.distinct().sortedByDescending { it.length }
    }

    /** Similarity of two normalized strings in 0..1. */
    fun score(query: String, variant: String): Double {
        if (query.isEmpty() || variant.isEmpty()) return 0.0
        if (query == variant) return 1.0
        val shorter = minOf(query.length, variant.length)
        val longer = maxOf(query.length, variant.length)
        if (shorter >= 2 && (query.contains(variant) || variant.contains(query))) {
            return 0.5 + 0.5 * shorter / longer
        }
        return 1.0 - levenshtein(query, variant).toDouble() / longer
    }

    fun match(fileName: String, talents: List<Talent>): Talent? {
        val query = normalize(fileName)
        if (query.length < 2) return null
        var best: Talent? = null
        var bestScore = 0.0
        var bestLength = 0
        for (t in talents) {
            for (v in variants(t)) {
                val s = score(query, v)
                if (s > bestScore || (s == bestScore && v.length > bestLength)) {
                    best = t
                    bestScore = s
                    bestLength = v.length
                }
            }
        }
        return if (bestScore >= THRESHOLD) best else null
    }

    /** Whether [t] should be shown for the search text [query] in the talent picker. */
    fun matchesSearch(query: String, t: Talent): Boolean {
        val q = normalize(query)
        return q.isEmpty() || variants(t).any { it.contains(q) }
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val tmp = prev
            prev = cur
            cur = tmp
        }
        return prev[b.length]
    }
}
