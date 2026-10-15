package com.ttggg5.holovoice.data.talents

import android.content.Context
import android.net.Uri
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
class TalentDirectory(private val context: Context, val pictures: TalentPictures) {
    private val cacheFile = File(context.filesDir, "talents.json")
    private val mutex = Mutex()
    private var memory: List<Talent>? = null

    /** Talents the user added themselves (not on the hololive site), with their own pictures. */
    private val customFile = File(context.filesDir, "custom_talents.json")
    private val customDir = File(context.filesDir, "custom_talents")
    private val customMutex = Mutex()
    private var customMemory: List<Talent>? = null

    /** Cached talents, refreshed from the site when the cache is empty or older than a week, plus the user's own. */
    suspend fun load(): List<Talent> = withLocalPictures(loadRemote()) + custom()

    /** Downloads the list now. Throws if the site can't be reached or nothing could be parsed. */
    suspend fun refresh(): List<Talent> = withLocalPictures(refreshRemote()) + custom()

    /** The talents the user added. */
    suspend fun custom(): List<Talent> = customMutex.withLock { customMemory ?: readCustom().also { customMemory = it } }

    fun isCustom(slug: String?): Boolean = CustomTalents.isCustom(slug)

    /** Adds a talent; [picture] (e.g. from the photo picker) is copied into app storage. */
    suspend fun addCustom(nameEn: String, nameJa: String?, picture: Uri?): Talent = customMutex.withLock {
        val list = customMemory ?: readCustom()
        val slug = CustomTalents.slug(System.currentTimeMillis(), list.map { it.slug }.toSet())
        val talent = Talent(slug, CustomTalents.clean(nameEn), CustomTalents.cleanOrNull(nameJa), picture?.let { copyPicture(slug, it) }.orEmpty())
        writeCustom(list + talent)
        talent
    }

    /** Renames a custom talent; a new [picture] replaces the old one, [removePicture] clears it. */
    suspend fun updateCustom(slug: String, nameEn: String, nameJa: String?, picture: Uri?, removePicture: Boolean = false): Talent? =
        customMutex.withLock {
            val list = customMemory ?: readCustom()
            val old = list.firstOrNull { it.slug == slug } ?: return@withLock null
            val image = when {
                picture != null -> copyPicture(slug, picture).also { deletePicture(old.imageUrl) }
                removePicture -> "".also { deletePicture(old.imageUrl) }
                else -> old.imageUrl
            }
            val updated = old.copy(nameEn = CustomTalents.clean(nameEn), nameJa = CustomTalents.cleanOrNull(nameJa), imageUrl = image)
            writeCustom(list.map { if (it.slug == slug) updated else it })
            updated
        }

    suspend fun removeCustom(slug: String) = customMutex.withLock {
        val list = customMemory ?: readCustom()
        list.firstOrNull { it.slug == slug }?.let { deletePicture(it.imageUrl) }
        writeCustom(list.filterNot { it.slug == slug })
    }

    /** Copies the picked picture under a new name each time, so image caches show the new one. */
    private suspend fun copyPicture(slug: String, source: Uri): String = withContext(Dispatchers.IO) {
        customDir.mkdirs()
        val target = File(customDir, "$slug-${System.currentTimeMillis()}.img")
        context.contentResolver.openInputStream(source)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            ?: error("Couldn't read the picture")
        "file://${target.absolutePath}"
    }

    private suspend fun deletePicture(uri: String) = withContext(Dispatchers.IO) {
        if (uri.startsWith("file://")) File(uri.removePrefix("file://")).takeIf { it.parentFile == customDir }?.delete()
    }

    private suspend fun writeCustom(list: List<Talent>) = withContext(Dispatchers.IO) {
        val json = JSONArray()
        list.forEach { t ->
            json.put(JSONObject().put("slug", t.slug).put("en", t.nameEn).put("ja", t.nameJa ?: JSONObject.NULL).put("img", t.imageUrl))
        }
        customFile.writeText(json.toString())
        customMemory = list
    }

    private suspend fun readCustom(): List<Talent> = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONArray(customFile.readText())
            (0 until json.length()).map { i ->
                val o = json.getJSONObject(i)
                Talent(o.getString("slug"), o.getString("en"), if (o.isNull("ja")) null else o.getString("ja"), o.optString("img"))
            }
        }.getOrDefault(emptyList())
    }

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
                    .put("img", t.imageUrl)
                    .put("tags", JSONArray(t.tags)),
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
                // Caches written before tags were split off are cleaned here.
                val (en, enTags) = TalentTags.split(o.getString("en"))
                val (ja, jaTags) = if (o.isNull("ja")) null to emptyList() else TalentTags.split(o.getString("ja"))
                val stored = o.optJSONArray("tags")?.let { a -> (0 until a.length()).map(a::getString) }.orEmpty()
                Talent(
                    slug = o.getString("slug"),
                    nameEn = en,
                    nameJa = ja,
                    imageUrl = o.getString("img"),
                    tags = (stored + enTags + jaTags).distinct(),
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
