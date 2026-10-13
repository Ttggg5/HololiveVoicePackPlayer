package com.ttggg5.holovoice.data.talents

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

/**
 * Talent profile pictures stored in the app (one file per talent), so they show offline and
 * everywhere (lists, notification, colours) from the same file. [index] remembers which remote
 * picture each file came from, so a changed picture on the site is downloaded again.
 */
class TalentPictures(context: Context) {
    private val dir = File(context.filesDir, "talent_pictures").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")
    private val mutex = Mutex()

    private val _progress = MutableStateFlow<Pair<Int, Int>?>(null)

    /** Done / total while pictures are downloading, else null. */
    val progress: StateFlow<Pair<Int, Int>?> = _progress.asStateFlow()

    data class Result(val downloaded: Int, val failed: Int)

    private fun file(slug: String) = File(dir, "${slug.replace(Regex("[^a-z0-9_-]"), "_")}.img")

    /** The stored picture of [slug] as a file URI, or null if it hasn't been downloaded. */
    fun localUri(slug: String): String? = file(slug).takeIf { it.isFile && it.length() > 0 }?.let { "file://${it.absolutePath}" }

    /**
     * Downloads the pictures of [talents] that are missing or changed on the site; with [force],
     * all of them. A failed download keeps the previous file.
     */
    suspend fun ensureAll(talents: List<Talent>, force: Boolean = false): Result = mutex.withLock {
        withContext(Dispatchers.IO) {
            val index = readIndex()
            val todo = talents.filter { t -> force || localUri(t.slug) == null || index[t.slug] != t.imageUrl }
            if (todo.isEmpty()) return@withContext Result(0, 0)
            val done = AtomicInteger()
            val failed = AtomicInteger()
            _progress.value = 0 to todo.size
            val limit = Semaphore(PARALLEL)
            try {
                coroutineScope {
                    todo.map { t ->
                        async {
                            limit.withPermit {
                                if (runCatching { download(t.imageUrl, file(t.slug)) }.isSuccess) {
                                    synchronized(index) { index[t.slug] = t.imageUrl }
                                } else {
                                    failed.incrementAndGet()
                                }
                                _progress.value = done.incrementAndGet() to todo.size
                            }
                        }
                    }.awaitAll()
                }
            } finally {
                writeIndex(index)
                _progress.value = null
            }
            Result(todo.size - failed.get(), failed.get())
        }
    }

    private fun download(url: String, target: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) HoloVoice")
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode} from $url")
            // Write next to the target and swap in, so a broken download never replaces a good picture.
            val tmp = File(target.parentFile, target.name + ".tmp")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (tmp.length() == 0L || !tmp.renameTo(target)) {
                tmp.delete()
                error("Couldn't save $url")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun readIndex(): MutableMap<String, String> = runCatching {
        val json = JSONObject(indexFile.readText())
        json.keys().asSequence().associateWith { json.getString(it) }.toMutableMap()
    }.getOrDefault(mutableMapOf())

    private fun writeIndex(index: Map<String, String>) {
        runCatching { indexFile.writeText(JSONObject(index).toString()) }
    }

    private companion object {
        const val PARALLEL = 4
    }
}
