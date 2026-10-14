package com.ttggg5.holovoice.data.talents

/**
 * Extracts talents from the HTML of https://hololive.hololivepro.com/talents (and /en/talents).
 *
 * The parser does not depend on exact class names: it looks for links to `/talents/<slug>/`
 * that contain an image, and reads the names from the link text and the image alt text.
 * Japanese text (kana/kanji) becomes [Talent.nameJa], Latin text [Talent.nameEn].
 */
object TalentPageParser {
    private val LINK = Regex(
        """<a\s[^>]*?href\s*=\s*["']([^"']*/talents/([A-Za-z0-9_%\-]+)/?)["'][^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val IMG = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val TAG = Regex("""<[^>]+>""")
    private val JAPANESE = Regex("""[぀-ヿ㐀-鿿豈-﫿]""")
    private val LATIN_LETTER = Regex("""[A-Za-z]""")

    fun parse(html: String, baseUrl: String = "https://hololive.hololivepro.com"): List<Talent> {
        val bySlug = LinkedHashMap<String, Talent>()
        for (m in LINK.findAll(html)) {
            val slug = decode(m.groupValues[2]).lowercase()
            val inner = m.groupValues[3]
            val imgTag = IMG.find(inner)?.value ?: continue
            val image = imageUrl(imgTag, baseUrl) ?: continue
            val texts = (listOfNotNull(attr(imgTag, "alt")) + textPieces(inner)).map { it.trim() }.filter { it.isNotEmpty() }
            val ja = texts.firstOrNull { JAPANESE.containsMatchIn(it) }?.let { japanesePart(it) }
            val en = pickEnglish(texts, slug)
            // "[Alum] Kiryu Coco" → name "Kiryu Coco" with the tag "Alum".
            val (cleanEn, enTags) = TalentTags.split(en)
            val (cleanJa, jaTags) = ja?.let(TalentTags::split) ?: (null to emptyList())
            val talent = Talent(
                slug = slug,
                nameEn = cleanEn.ifBlank { en },
                nameJa = cleanJa?.ifBlank { ja },
                imageUrl = image,
                tags = (enTags + jaTags).distinct(),
            )
            bySlug[slug] = bySlug[slug]?.let { merge(it, talent) } ?: talent
        }
        return bySlug.values.toList()
    }

    /** Combines the Japanese-page and English-page entries of the same talent. */
    fun merge(a: Talent, b: Talent): Talent = Talent(
        slug = a.slug,
        nameEn = if (a.nameEn == titleFromSlug(a.slug) && b.nameEn != titleFromSlug(b.slug)) b.nameEn else a.nameEn,
        nameJa = a.nameJa ?: b.nameJa,
        imageUrl = a.imageUrl,
        tags = (a.tags + b.tags).distinct(),
    )

    fun titleFromSlug(slug: String): String =
        slug.split('-', '_').filter { it.isNotEmpty() }.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    private fun pickEnglish(texts: List<String>, slug: String): String {
        val latin = texts.filter { LATIN_LETTER.containsMatchIn(it) && !JAPANESE.containsMatchIn(it) }
        val slugKey = slug.replace(Regex("[^a-z]"), "")
        // Prefer the text that spells the slug (e.g. "Usada Pekora" for usada-pekora), then any Latin text.
        return latin.firstOrNull { it.lowercase().replace(Regex("[^a-z]"), "") == slugKey }
            ?: latin.firstOrNull { it.length <= 40 }
            ?: titleFromSlug(slug)
    }

    /** "兎田ぺこら Usada Pekora" → "兎田ぺこら". */
    private fun japanesePart(text: String): String =
        text.split(Regex("""\s{2,}|\s(?=[A-Za-z])""")).firstOrNull { JAPANESE.containsMatchIn(it) }?.trim() ?: text

    private fun textPieces(inner: String): List<String> =
        decodeEntities(inner.replace(TAG, "\n")).split('\n')

    private fun imageUrl(imgTag: String, baseUrl: String): String? {
        val candidates = listOfNotNull(
            attr(imgTag, "data-src"),
            attr(imgTag, "data-lazy-src"),
            attr(imgTag, "src"),
            attr(imgTag, "data-srcset")?.firstSrcset(),
            attr(imgTag, "srcset")?.firstSrcset(),
        )
        val url = candidates.firstOrNull { it.isNotBlank() && !it.startsWith("data:") } ?: return null
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> baseUrl.trimEnd('/') + url
            else -> url
        }
    }

    private fun String.firstSrcset(): String? = split(',').firstOrNull()?.trim()?.substringBefore(' ')

    private fun attr(tag: String, name: String): String? =
        Regex("""\s${Regex.escape(name)}\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
            .find(tag)?.groupValues?.get(1)?.let(::decodeEntities)

    private fun decode(s: String): String = runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

    private fun decodeEntities(s: String): String = s
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("""&#(\d+);""")) { it.groupValues[1].toInt().toChar().toString() }
}
