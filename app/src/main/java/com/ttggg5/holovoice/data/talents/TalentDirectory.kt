package com.ttggg5.holovoice.data.talents

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The official talent list (names + profile pictures), downloaded from the hololive site
 * and cached in app storage so matching works offline after the first download.
 * Talents come back with their stored picture ([TalentPictures]) when it has been downloaded.
 */
class TalentDirectory(context: Context, val pictures: TalentPictures) {
    private val cacheFile = File(context.filesDir, "talents.json")
    private val mutex = Mutex()
    private var memory: List<Talent>? = null

    /** Cached talents, refreshed from the site when the cache is empty or older than a week. */
    suspend fun load(): List<Talent> = withLocalPictures(loadRemote())

    /** Downloads the list now. Throws if the site can't be reached or nothing could be parsed. */
    suspend fun refresh(): List<Talent> = withLocalPictures(refreshRemote())

    /** Like [load], but with the picture URLs on the hololive site. */
    suspend fun loadRemote(): List<Talent> = mutex.withLock {
        val cached = memory ?: readCache().also { memory = it }
        val stale = System.currentTimeMillis() - cacheFile.lastModified() > MAX_AGE_MS
        if (cached.isEmpty() || stale) {
            runCatching { download() }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return@withLock store(it) }
        }
        cached
    }

    /** Like [refresh], but with the picture URLs on the hololive site. */
    suspend fun refreshRemote(): List<Talent> = mutex.withLock {
        val fresh = download()
        if (fresh.isEmpty()) error("No talents found on the hololive site")
        store(fresh)
    }

    private fun withLocalPictures(talents: List<Talent>): List<Talent> =
        talents.map { t -> pictures.localUri(t.slug)?.let { t.copy(imageUrl = it) } ?: t }

    private suspend fun download(): List<Talent> = withContext(Dispatchers.IO) {
        val ja = TalentPageParser.parse(fetch(JA_URL))
        val en = runCatching { TalentPageParser.parse(fetch(EN_URL)) }.getOrDefault(emptyList()).associateBy { it.slug }
        val merged = ja.map { t -> en[t.slug]?.let { TalentPageParser.merge(t, it) } ?: t }
        merged + en.values.filter { e -> merged.none { it.slug == e.slug } }
    }

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) HoloVoice")
            conn.setRequestProperty("Accept-Language", if (url == EN_URL) "en" else "ja")
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode} from $url")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun store(talents: List<Talent>): List<Talent> = withContext(Dispatchers.IO) {
        val json = JSONArray()
        talents.forEach { t ->
            json.put(
                JSONObject()
                    .put("slug", t.slug)
                    .put("en", t.nameEn)
                    .put("ja", t.nameJa ?: JSONObject.NULL)
                    .put("img", t.imageUrl),
            )
        }
        cacheFile.writeText(json.toString())
        memory = talents
        talents
    }

    private suspend fun readCache(): List<Talent> = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONArray(cacheFile.readText())
            (0 until json.length()).map { i ->
                val o = json.getJSONObject(i)
                Talent(
                    slug = o.getString("slug"),
                    nameEn = o.getString("en"),
                    nameJa = if (o.isNull("ja")) null else o.getString("ja"),
                    imageUrl = o.getString("img"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val JA_URL = "https://hololive.hololivepro.com/talents"
        const val EN_URL = "https://hololive.hololivepro.com/en/talents"
        const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    }
}
