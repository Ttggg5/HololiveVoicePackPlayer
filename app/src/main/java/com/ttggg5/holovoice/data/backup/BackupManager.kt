package com.ttggg5.holovoice.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ttggg5.holovoice.alarm.AlarmRepository
import com.ttggg5.holovoice.data.Alarm
import com.ttggg5.holovoice.data.AlarmSource
import com.ttggg5.holovoice.data.AppDatabase
import com.ttggg5.holovoice.data.ClipTalent
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PlayEvent
import com.ttggg5.holovoice.data.Playlist
import com.ttggg5.holovoice.data.PlaylistEntry
import com.ttggg5.holovoice.data.UiPrefs
import com.ttggg5.holovoice.data.orderedTalents
import com.ttggg5.holovoice.data.talents.CustomTalents
import com.ttggg5.holovoice.data.talents.Talent
import com.ttggg5.holovoice.data.talents.TalentDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Saves the user's own data (settings, favorites, play history, playlists, alarms, custom talents) to a
 * file and brings it back. The library itself isn't saved: its folders are imported again instead.
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val uiPrefs: UiPrefs,
    private val directory: TalentDirectory,
    private val alarms: AlarmRepository,
    private val appVersion: String,
) {
    /** Writes a backup to [uri] (from the system's "create document" picker). */
    suspend fun export(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val backup = build()
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Couldn't write the file")
        out.use { it.write(BackupJson.write(backup).toByteArray()) }
        BackupSummary.of(backup)
    }

    /** Reads the backup at [uri] without changing anything, so the user can confirm first. */
    suspend fun read(uri: Uri): Backup = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("Couldn't open the file")
        BackupJson.read(input.use { it.readBytes().decodeToString() })
    }

    /**
     * Restores [backup]: playlists and alarms are replaced, favorites and play counts are merged (never lowered),
     * history is added without duplicates, custom talents are added or replaced and settings are replaced.
     */
    suspend fun restore(backup: Backup): BackupSummary = withContext(Dispatchers.IO) {
        val restoredTalents = directory.restoreCustom(
            backup.customTalents.map { Talent(it.slug, CustomTalents.clean(it.nameEn), CustomTalents.cleanOrNull(it.nameJa), "") },
            backup.customTalents.mapNotNull { t -> t.picture?.let { t.slug to it } }.toMap(),
        )
        val customPictures = restoredTalents.associate { it.slug to it.imageUrl.ifEmpty { null } }

        val library = db.clipDao().getAllClips()
        val matcher = ClipMatcher(library.map { LibraryClip(it.clip.id, it.ref()) })
        val ids: List<Long?> = backup.clips.map { matcher.find(it.ref) }
        fun idOf(index: Int?): Long? = index?.let { ids.getOrNull(it) }

        var favorites = 0
        var events = 0
        var missingInPlaylists = 0
        db.withTransaction {
            db.clipTalentDao().setNames(restoredTalents.associate { it.slug to it.nameEn })
            db.clipTalentDao().setPictures(customPictures)

            backup.clips.forEachIndexed { i, c ->
                val id = ids[i] ?: return@forEachIndexed
                if (c.favorite) {
                    db.clipDao().setFavorite(id, true)
                    favorites++
                }
                if (c.playCount > 0 || c.lastPlayedAt > 0) db.clipDao().mergePlayStats(id, c.playCount, c.lastPlayedAt)
                c.manualTalents?.let { talents ->
                    val rows = talents.map { t -> ClipTalent(id, t.slug, t.name, pictureOf(t, customPictures), 0) }
                    db.clipTalentDao().replace(id, rows, manual = true)
                }
            }

            val seen = db.playEventDao().getAll().mapTo(HashSet()) { it.clipId to it.playedAt }
            val newEvents = backup.events.mapNotNull { e ->
                val id = idOf(e.clip) ?: return@mapNotNull null
                if (seen.add(id to e.playedAt)) PlayEvent(clipId = id, playedAt = e.playedAt, listenedMs = e.listenedMs) else null
            }
            db.playEventDao().insertAll(newEvents)
            events = newEvents.size

            db.playlistDao().deleteAll()
            backup.playlists.forEach { p ->
                val playlistId = db.playlistDao().insert(Playlist(name = p.name, createdAt = p.createdAt))
                val clipIds = p.clips.mapNotNull { idOf(it) }
                missingInPlaylists += p.clips.size - clipIds.size
                db.playlistDao().insertEntries(clipIds.mapIndexed { i, clipId -> PlaylistEntry(playlistId = playlistId, clipId = clipId, position = i) })
            }
        }

        val topics = TopicMatcher(db.packDao().getAll().map { it.id to TopicRef(it.sourceUri, it.name) })
        db.alarmDao().getAll().forEach { alarms.delete(it) }
        backup.alarms.forEach { a ->
            alarms.save(
                Alarm(
                    hour = a.hour.coerceIn(0, 23),
                    minute = a.minute.coerceIn(0, 59),
                    days = a.days,
                    enabled = a.enabled,
                    label = a.label,
                    source = runCatching { AlarmSource.valueOf(a.source) }.getOrDefault(AlarmSource.FAVORITES),
                    clipId = idOf(a.clip),
                    talentSlug = a.talentSlug,
                    packId = a.topic?.let(topics::find),
                    snoozeMinutes = a.snoozeMinutes,
                ),
            )
        }

        if (backup.prefs.isNotEmpty()) withContext(Dispatchers.Main) { uiPrefs.importAll(backup.prefs) }

        BackupSummary.of(backup).copy(
            favorites = favorites,
            plays = events,
            missingClips = ids.count { it == null },
            missingInPlaylists = missingInPlaylists,
        )
    }

    private suspend fun build(): Backup {
        val library = db.clipDao().getAllClips()
        val byId = library.associateBy { it.clip.id }
        val records = mutableListOf<ClipRecord>()
        val indexOf = HashMap<Long, Int>()
        fun index(id: Long): Int? = indexOf[id] ?: byId[id]?.let { c ->
            records += ClipRecord(c.ref())
            (records.size - 1).also { indexOf[id] = it }
        }

        // Clips with something of the user's on them come first, with that data.
        library.filter { it.clip.isFavorite || it.clip.playCount > 0 || it.clip.lastPlayedAt > 0 || it.clip.talentManual }.forEach { c ->
            indexOf[c.clip.id] = records.size
            records += ClipRecord(
                ref = c.ref(),
                favorite = c.clip.isFavorite,
                playCount = c.clip.playCount,
                lastPlayedAt = c.clip.lastPlayedAt,
                manualTalents = if (c.clip.talentManual) c.orderedTalents.map { TalentRef(it.slug, it.name, it.imageUrl) } else null,
            )
        }

        val events = db.playEventDao().getAll().mapNotNull { e -> index(e.clipId)?.let { EventRecord(it, e.playedAt, e.listenedMs) } }
        val entries = db.playlistDao().getAllEntries().groupBy { it.playlistId }
        val playlists = db.playlistDao().getAll().map { p ->
            PlaylistRecord(p.name, p.createdAt, entries[p.id].orEmpty().sortedBy { it.position }.mapNotNull { index(it.clipId) })
        }
        val packs = db.packDao().getAll().associateBy { it.id }
        val alarmRecords = db.alarmDao().getAll().map { a ->
            AlarmRecord(
                hour = a.hour,
                minute = a.minute,
                days = a.days,
                enabled = a.enabled,
                label = a.label,
                source = a.source.name,
                clip = a.clipId?.let { index(it) },
                talentSlug = a.talentSlug,
                topic = a.packId?.let(packs::get)?.let { TopicRef(it.sourceUri, it.name) },
                snoozeMinutes = a.snoozeMinutes,
            )
        }
        val talents = directory.custom().map { t -> CustomTalentRecord(t.slug, t.nameEn, t.nameJa, directory.customPicture(t)) }

        return Backup(
            createdAt = System.currentTimeMillis(),
            appVersion = appVersion,
            prefs = withContext(Dispatchers.Main) { uiPrefs.exportAll() },
            clips = records,
            events = events,
            playlists = playlists,
            alarms = alarmRecords,
            customTalents = talents,
        )
    }

    /** A restored talent's picture: a custom one's new file, an official one's stored picture, else its web address. */
    private fun pictureOf(t: TalentRef, custom: Map<String, String?>): String? =
        if (t.slug in custom) custom[t.slug] else directory.pictures.localUri(t.slug) ?: t.imageUrl?.takeIf { it.startsWith("http") }

    private fun ClipWithPack.ref() = ClipRef(clip.fileUri, packName, clip.category, clip.title, clip.trackNo)
}
