package com.ttggg5.holovoice.data.backup

import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/**
 * Everything a backup file holds. Clips are listed once in [clips] and referred to by their index there,
 * because database ids change when the library is imported again.
 */
data class Backup(
    val createdAt: Long,
    val appVersion: String,
    /** The `ui` preferences, values typed as Boolean / Int / Long / Float / String / Set<String>. */
    val prefs: Map<String, Any>,
    val clips: List<ClipRecord>,
    val events: List<EventRecord>,
    val playlists: List<PlaylistRecord>,
    val alarms: List<AlarmRecord>,
    val customTalents: List<CustomTalentRecord>,
) {
    companion object {
        const val FORMAT = "holovoice-backup"
        const val VERSION = 1
    }
}

/** How a clip is found again: its file, else its topic + sub-folder + title + track number. */
data class ClipRef(val uri: String, val topic: String, val category: String?, val title: String, val trackNo: Int?)

data class TalentRef(val slug: String, val name: String, val imageUrl: String?)

data class ClipRecord(
    val ref: ClipRef,
    val favorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0,
    /** Talents the user picked by hand (null when they were matched automatically). */
    val manualTalents: List<TalentRef>? = null,
)

data class EventRecord(val clip: Int, val playedAt: Long, val listenedMs: Long)

data class PlaylistRecord(val name: String, val createdAt: Long, val clips: List<Int>)

data class TopicRef(val sourceUri: String, val name: String)

data class AlarmRecord(
    val hour: Int,
    val minute: Int,
    val days: Int,
    val enabled: Boolean,
    val label: String,
    val source: String,
    val clip: Int?,
    val talentSlug: String?,
    val topic: TopicRef?,
    val snoozeMinutes: Int,
)

data class CustomTalentRecord(val slug: String, val nameEn: String, val nameJa: String?, val picture: ByteArray?) {
    override fun equals(other: Any?): Boolean =
        other is CustomTalentRecord && slug == other.slug && nameEn == other.nameEn && nameJa == other.nameJa &&
            picture.contentEquals(other.picture)

    override fun hashCode(): Int = (slug.hashCode() * 31 + nameEn.hashCode()) * 31 + (picture?.contentHashCode() ?: 0)
}

/** Reading and writing [Backup] as JSON. */
object BackupJson {
    fun write(b: Backup): String = JSONObject()
        .put("format", Backup.FORMAT)
        .put("version", Backup.VERSION)
        .put("createdAt", b.createdAt)
        .put("appVersion", b.appVersion)
        .put("prefs", JSONObject().also { o -> b.prefs.toSortedMap().forEach { (k, v) -> o.put(k, typed(v)) } })
        .put("clips", array(b.clips) { c ->
            JSONObject()
                .put("uri", c.ref.uri)
                .put("topic", c.ref.topic)
                .putOpt("category", c.ref.category)
                .put("title", c.ref.title)
                .putOpt("trackNo", c.ref.trackNo)
                .apply {
                    if (c.favorite) put("favorite", true)
                    if (c.playCount > 0) put("playCount", c.playCount)
                    if (c.lastPlayedAt > 0) put("lastPlayedAt", c.lastPlayedAt)
                    c.manualTalents?.let { list ->
                        put("talents", array(list) { JSONObject().put("slug", it.slug).put("name", it.name).putOpt("img", it.imageUrl) })
                    }
                }
        })
        .put("events", array(b.events) { JSONArray().put(it.clip).put(it.playedAt).put(it.listenedMs) })
        .put("playlists", array(b.playlists) { p ->
            JSONObject().put("name", p.name).put("createdAt", p.createdAt).put("clips", JSONArray(p.clips))
        })
        .put("alarms", array(b.alarms) { a ->
            JSONObject()
                .put("hour", a.hour).put("minute", a.minute).put("days", a.days).put("enabled", a.enabled)
                .put("label", a.label).put("source", a.source)
                .putOpt("clip", a.clip).putOpt("talent", a.talentSlug)
                .putOpt("topic", a.topic?.let { JSONObject().put("sourceUri", it.sourceUri).put("name", it.name) })
                .put("snoozeMinutes", a.snoozeMinutes)
        })
        .put("customTalents", array(b.customTalents) { t ->
            JSONObject().put("slug", t.slug).put("en", t.nameEn).putOpt("ja", t.nameJa)
                .putOpt("picture", t.picture?.let { Base64.getEncoder().encodeToString(it) })
        })
        .toString()

    /** Throws [IllegalArgumentException] when [text] isn't a backup from this app. */
    fun read(text: String): Backup {
        val root = runCatching { JSONObject(text) }.getOrElse { throw IllegalArgumentException("This file isn't a Holo Voice backup.") }
        require(root.optString("format") == Backup.FORMAT) { "This file isn't a Holo Voice backup." }
        require(root.optInt("version") <= Backup.VERSION) { "This backup is from a newer version of the app. Update the app first." }
        val prefs = root.optJSONObject("prefs")?.let { o -> o.keys().asSequence().mapNotNull { k -> untyped(o.optJSONObject(k))?.let { k to it } }.toMap() }
        return Backup(
            createdAt = root.optLong("createdAt"),
            appVersion = root.optString("appVersion"),
            prefs = prefs.orEmpty(),
            clips = list(root.optJSONArray("clips")) { o: JSONObject ->
                ClipRecord(
                    ref = ClipRef(o.getString("uri"), o.getString("topic"), o.str("category"), o.getString("title"), o.int("trackNo")),
                    favorite = o.optBoolean("favorite"),
                    playCount = o.optInt("playCount"),
                    lastPlayedAt = o.optLong("lastPlayedAt"),
                    manualTalents = o.optJSONArray("talents")?.let { arr ->
                        list(arr) { t: JSONObject -> TalentRef(t.getString("slug"), t.getString("name"), t.str("img")) }
                    },
                )
            },
            events = list(root.optJSONArray("events")) { a: JSONArray -> EventRecord(a.getInt(0), a.getLong(1), a.getLong(2)) },
            playlists = list(root.optJSONArray("playlists")) { o: JSONObject ->
                PlaylistRecord(o.getString("name"), o.optLong("createdAt"), o.optJSONArray("clips")?.let { c -> (0 until c.length()).map(c::getInt) }.orEmpty())
            },
            alarms = list(root.optJSONArray("alarms")) { o: JSONObject ->
                AlarmRecord(
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    days = o.optInt("days"),
                    enabled = o.optBoolean("enabled", true),
                    label = o.optString("label"),
                    source = o.optString("source", "FAVORITES"),
                    clip = o.int("clip"),
                    talentSlug = o.str("talent"),
                    topic = o.optJSONObject("topic")?.let { TopicRef(it.getString("sourceUri"), it.getString("name")) },
                    snoozeMinutes = o.optInt("snoozeMinutes", 10),
                )
            },
            customTalents = list(root.optJSONArray("customTalents")) { o: JSONObject ->
                CustomTalentRecord(
                    o.getString("slug"),
                    o.getString("en"),
                    o.str("ja"),
                    o.str("picture")?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() },
                )
            },
        )
    }

    /** A pref value with its type, so an Int doesn't come back as a Long or a Float as a Double. */
    private fun typed(v: Any): JSONObject = when (v) {
        is Boolean -> JSONObject().put("t", "b").put("v", v)
        is Int -> JSONObject().put("t", "i").put("v", v)
        is Long -> JSONObject().put("t", "l").put("v", v)
        is Float -> JSONObject().put("t", "f").put("v", v.toString())
        is Set<*> -> JSONObject().put("t", "set").put("v", JSONArray(v.map { it.toString() }))
        else -> JSONObject().put("t", "s").put("v", v.toString())
    }

    private fun untyped(o: JSONObject?): Any? = o?.let {
        runCatching {
            when (it.getString("t")) {
                "b" -> it.getBoolean("v")
                "i" -> it.getInt("v")
                "l" -> it.getLong("v")
                "f" -> it.getString("v").toFloat()
                "set" -> it.getJSONArray("v").let { a -> (0 until a.length()).map(a::getString).toSet() }
                "s" -> it.getString("v")
                else -> null
            }
        }.getOrNull()
    }

    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key)
    private fun JSONObject.int(key: String): Int? = if (isNull(key)) null else optInt(key)

    private fun <T> array(items: List<T>, f: (T) -> Any): JSONArray = JSONArray().also { a -> items.forEach { a.put(f(it)) } }

    private inline fun <reified J, T> list(arr: JSONArray?, f: (J) -> T): List<T> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i -> (arr.opt(i) as? J)?.let { runCatching { f(it) }.getOrNull() } }
    }
}

/** A library clip as [ClipMatcher] sees it. */
data class LibraryClip(val id: Long, val ref: ClipRef)

/** Finds the clips of a backup in the current library. */
class ClipMatcher(library: List<LibraryClip>) {
    private val byUri = library.associate { it.ref.uri to it.id }
    private val byName = library.groupBy { nameKey(it.ref) }.mapValues { (_, clips) -> clips.first().id }

    /** The clip's id in this library, or null when it isn't there. */
    fun find(ref: ClipRef): Long? = byUri[ref.uri] ?: byName[nameKey(ref)]

    private fun nameKey(r: ClipRef): String =
        listOf(r.topic.trim().lowercase(), r.category?.trim()?.lowercase().orEmpty(), r.title.trim().lowercase(), r.trackNo?.toString().orEmpty())
            .joinToString("\u0000")
}

/** Finds the topics an alarm played in the current library. */
class TopicMatcher(topics: List<Pair<Long, TopicRef>>) {
    private val bySource = topics.groupBy({ it.second.sourceUri }, { it.first })
    private val byName = topics.groupBy({ it.second.name.trim().lowercase() }, { it.first })

    fun find(ref: TopicRef): Long? {
        val sameSource = bySource[ref.sourceUri].orEmpty()
        // A library folder holds several topics with the same source, so the name decides between them.
        return sameSource.singleOrNull()
            ?: byName[ref.name.trim().lowercase()]?.let { named -> named.firstOrNull { it in sameSource } ?: named.first() }
            ?: sameSource.firstOrNull()
    }
}

/** What a backup held, or what a restore brought back, for the message shown afterwards. */
data class BackupSummary(
    val favorites: Int,
    val plays: Int,
    val playlists: Int,
    val alarms: Int,
    val talents: Int,
    val settings: Boolean,
    /** Clips in the backup that aren't in the library (restore only). */
    val missingClips: Int = 0,
    val missingInPlaylists: Int = 0,
) {
    /** "42 favorites, 5 playlists, 2 alarms, 1 custom talent, 310 plays and settings". */
    fun describe(): String {
        val parts = buildList {
            if (favorites > 0) add(count(favorites, "favorite"))
            if (playlists > 0) add(count(playlists, "playlist") + if (missingInPlaylists > 0) " (${count(missingInPlaylists, "clip")} not found)" else "")
            if (alarms > 0) add(count(alarms, "alarm"))
            if (talents > 0) add(count(talents, "custom talent"))
            if (plays > 0) add(count(plays, "play") + " of history")
            if (settings) add("settings")
        }
        return when (parts.size) {
            0 -> "nothing"
            1 -> parts[0]
            else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last()
        }
    }

    companion object {
        fun of(b: Backup) = BackupSummary(
            favorites = b.clips.count { it.favorite },
            plays = b.events.size,
            playlists = b.playlists.size,
            alarms = b.alarms.size,
            talents = b.customTalents.size,
            settings = b.prefs.isNotEmpty(),
        )

        private fun count(n: Int, noun: String) = "$n " + if (n == 1) noun else noun + "s"
    }
}
