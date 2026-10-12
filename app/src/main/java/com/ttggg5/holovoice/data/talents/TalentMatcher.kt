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

    fun toHiragana(s: String): String = buildString(s.length) {
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

    private val WORD_SEPARATORS = Regex("""[\s_\-.,・、&×+()\[\]【】「」『』/]+""")
    private val KANJI_RUN = Regex("""^[\u4E00-\u9FFF\u3400-\u4DBF々]+$""")

    /** How one talent can be recognised inside a file name. */
    private class NameForms(val anywhere: List<String>, val wholeWord: List<String>)

    private val formsCache = java.util.concurrent.ConcurrentHashMap<Talent, NameForms>()

    private fun forms(t: Talent): NameForms = formsCache.getOrPut(t) {
        val anywhere = mutableListOf<String>()
        val wholeWord = mutableListOf<String>()
        val enTokens = t.nameEn.split(' ', '-', '_').map(::normalize).filter { it.isNotEmpty() }
        // Full names (English, reversed English, slug, Japanese) count anywhere in the file name.
        anywhere += normalize(t.nameEn)
        if (enTokens.size > 1) anywhere += enTokens.reversed().joinToString("")
        anywhere += normalize(t.slug)
        val ja = t.nameJa?.let(::normalize).orEmpty()
        if (ja.length >= 2) anywhere += ja
        // Japanese family/given names: kanji runs of 2+ and kana runs of 3+ anywhere, shorter kana only as a word.
        t.nameJa?.let { raw ->
            SCRIPT_RUN.findAll(Normalizer.normalize(raw, Normalizer.Form.NFKC)).map { normalize(it.value) }.forEach { run ->
                when {
                    KANJI_RUN.matches(run) -> if (run.length >= 2) anywhere += run
                    run.length >= 3 -> anywhere += run
                    run.length == 2 -> wholeWord += run
                }
            }
        }
        // Latin given/family names only as whole words, so "Ao" doesn't hit "chaos".
        wholeWord += enTokens.filter { it.length >= 2 }
        // Given names written in kana ("そら" for ときのそら): romaji converted to kana, but only if
        // the Japanese name really starts or ends with it. Whole words only.
        if (ja.isNotEmpty()) {
            enTokens.mapNotNull(::romajiToHiragana).filter { it.length >= 2 && it != ja && (ja.startsWith(it) || ja.endsWith(it)) }
                .forEach { wholeWord += it }
        }
        NameForms(anywhere.filter { it.length >= 2 }.distinct(), wholeWord.distinct())
    }

    /**
     * Every talent whose name appears in [fileName] (full name anywhere, given/family names as
     * words), in the order they appear. Falls back to a strict fuzzy match for typos.
     */
    fun matchAll(fileName: String, talents: List<Talent>): List<Talent> {
        val full = normalize(fileName)
        if (full.isEmpty()) return emptyList()
        val prepared = toHiragana(Normalizer.normalize(fileName, Normalizer.Form.NFKC).lowercase())
        val words = prepared.split(WORD_SEPARATORS).map { it.replace(NON_LETTER, "") }.filter { it.isNotEmpty() }.toSet()
        val hits = talents.mapNotNull { t ->
            val f = forms(t)
            val anywhere = f.anywhere.mapNotNull { form -> full.indexOf(form).takeIf { it >= 0 } }
            val asWord = f.wholeWord.filter { it in words }.map { full.indexOf(it).coerceAtLeast(0) }
            (anywhere + asWord).minOrNull()?.let { position -> t to position }
        }
        if (hits.isNotEmpty()) return hits.sortedBy { it.second }.map { it.first }.distinctBy { it.slug }
        return listOfNotNull(match(fileName, talents, threshold = STRICT_THRESHOLD))
    }

    private const val STRICT_THRESHOLD = 0.85

    private val ROMAJI = mapOf(
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        "sa" to "さ", "shi" to "し", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
        "za" to "ざ", "ji" to "じ", "zi" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        "ta" to "た", "chi" to "ち", "ti" to "ち", "tsu" to "つ", "tu" to "つ", "te" to "て", "to" to "と",
        "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        "ha" to "は", "hi" to "ひ", "fu" to "ふ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        "wa" to "わ", "wo" to "を",
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ", "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
        "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ", "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
        "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ", "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
        "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ", "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
        "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ", "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
        "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
    )

    /** Hepburn romaji → hiragana ("sora" → "そら"); null if it isn't plain romaji. */
    fun romajiToHiragana(romaji: String): String? {
        val s = romaji.lowercase()
        if (s.isEmpty() || s.any { it !in 'a'..'z' }) return null
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val next = s.getOrNull(i + 1)
            when {
                c == 'n' && (next == null || (next !in "aiueoy")) -> {
                    out.append('ん')
                    i += if (next == '\'') 2 else 1
                }
                next != null && c == next && c !in "aiueon" -> {
                    out.append('っ')
                    i++
                }
                else -> {
                    val len = (3 downTo 1).firstOrNull { l -> i + l <= s.length && ROMAJI.containsKey(s.substring(i, i + l)) }
                        ?: return null
                    out.append(ROMAJI.getValue(s.substring(i, i + len)))
                    i += len
                }
            }
        }
        return out.toString()
    }

    fun match(fileName: String, talents: List<Talent>, threshold: Double = THRESHOLD): Talent? {
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
        return if (bestScore >= threshold) best else null
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
