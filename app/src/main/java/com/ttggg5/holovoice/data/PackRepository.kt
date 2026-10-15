package com.ttggg5.holovoice.data

import android.net.Uri
import com.ttggg5.holovoice.data.talents.Talent
import com.ttggg5.holovoice.data.talents.TalentLinker
import com.ttggg5.holovoice.data.talents.TalentPictures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PackRepository(
    private val db: AppDatabase,
    private val importer: PackImporter,
    private val talentLinker: TalentLinker,
    private val libraryRoots: LibraryRoots,
) {
    val packs: Flow<List<PackWithCount>> = db.packDao().observePacks()
    val favorites: Flow<List<ClipWithPack>> = db.clipDao().observeFavorites()

    fun pack(id: Long): Flow<Pack?> = db.packDao().observePack(id)

    fun clips(packId: Long): Flow<List<ClipWithPack>> = db.clipDao().observeClips(packId)

    fun search(query: String): Flow<List<ClipWithPack>> = db.clipDao().search(query)

    fun clip(id: Long): Flow<ClipWithPack?> = db.clipDao().observeClip(id)

    /** Looks up clips by id, preserving the order (and repeats) of [ids] and skipping missing ones. */
    suspend fun clipsByIds(ids: List<Long>): List<ClipWithPack> {
        val byId = ids.distinct().chunked(900).flatMap { db.clipDao().getByIds(it) }.associateBy { it.clip.id }
        return ids.mapNotNull(byId::get)
    }

    val playlists: Flow<List<PlaylistWithCount>> = db.playlistDao().observeAll()

    /** Up to four different topic thumbnails per playlist (by playlist id), for the mosaics. */
    val playlistCovers: Flow<Map<Long, List<String>>> = db.playlistDao().observeCovers().map { rows ->
        rows.groupBy({ it.playlistId }, { it.coverUri }).mapValues { (_, covers) -> covers.distinct().take(4) }
    }

    fun playlist(id: Long): Flow<Playlist?> = db.playlistDao().observe(id)

    fun playlistItems(id: Long): Flow<List<PlaylistItem>> = db.playlistDao().observeEntries(id)

    suspend fun createPlaylist(name: String): Long = db.playlistDao().insert(Playlist(name = name.trim()))

    suspend fun renamePlaylist(id: Long, name: String) = db.playlistDao().rename(id, name.trim())

    suspend fun deletePlaylist(id: Long) = db.playlistDao().delete(id)

    suspend fun addToPlaylist(playlistId: Long, clipIds: List<Long>) = db.playlistDao().addClips(playlistId, clipIds)

    suspend fun removeFromPlaylist(entryId: Long) = db.playlistDao().removeEntry(entryId)

    suspend fun movePlaylistEntry(playlistId: Long, from: Int, to: Int) = db.playlistDao().move(playlistId, from, to)

    /** The official talent list (cached; refreshed weekly). [forceRefresh] downloads it now and may throw. */
    suspend fun talents(forceRefresh: Boolean = false): List<Talent> =
        if (forceRefresh) talentLinker.directory.refresh() else talentLinker.directory.load()

    /** Done / total while talent pictures are downloading, else null. */
    val talentPictureProgress: StateFlow<Pair<Int, Int>?> get() = talentLinker.directory.pictures.progress

    /**
     * Stores every talent's picture in the app (only missing or changed ones unless [force]) and
     * points the clips at the stored files, so pictures show offline.
     */
    suspend fun downloadTalentPictures(force: Boolean = false): TalentPictures.Result {
        val directory = talentLinker.directory
        val talents = directory.loadRemote()
        if (talents.isEmpty()) error("Couldn't download the talent list. Check your connection.")
        val result = directory.pictures.ensureAll(talents, force)
        val stored = talents.mapNotNull { t -> directory.pictures.localUri(t.slug)?.let { t.slug to it } }.toMap()
        db.clipTalentDao().setPictures(stored)
        // Names saved before tags like "[Alum]" were split off get the clean name.
        db.clipTalentDao().setNames(talents.associate { it.slug to it.nameEn })
        return result
    }

    /** Talents the user added themselves. */
    suspend fun customTalents(): List<Talent> = talentLinker.directory.custom()

    fun isCustomTalent(slug: String?): Boolean = talentLinker.directory.isCustom(slug)

    /** Adds a talent that isn't on the hololive site, then matches files named after them. */
    suspend fun addTalent(nameEn: String, nameJa: String?, picture: Uri?): Talent {
        val talent = talentLinker.directory.addCustom(nameEn, nameJa, picture)
        runCatching { rematchAllTalents() }
        return talent
    }

    /** Edits a custom talent and updates the name and picture already shown on their clips. */
    suspend fun editTalent(slug: String, nameEn: String, nameJa: String?, picture: Uri?, removePicture: Boolean): Talent? {
        val talent = talentLinker.directory.updateCustom(slug, nameEn, nameJa, picture, removePicture) ?: return null
        db.clipTalentDao().setNames(mapOf(slug to talent.nameEn))
        db.clipTalentDao().setPictures(mapOf(slug to talent.imageUrl.ifEmpty { null }))
        runCatching { rematchAllTalents() }
        return talent
    }

    /** Removes a custom talent and takes them off every clip. */
    suspend fun deleteTalent(slug: String) {
        talentLinker.directory.removeCustom(slug)
        db.clipTalentDao().removeTalent(slug)
    }

    /** Re-runs automatic matching on every clip (manual picks are kept). Returns matched and total counts. */
    suspend fun rematchAllTalents(): Pair<Int, Int> {
        val talents = talentLinker.directory.load()
        if (talents.isEmpty()) error("Couldn't download the talent list. Check your connection.")
        val clips = db.clipDao().getAll()
        return talentLinker.matchClips(clips, talents) to clips.size
    }

    suspend fun setClipTalents(clipIds: List<Long>, talents: List<Talent>) = talentLinker.setTalents(clipIds, talents)

    /** Sets [talents] on every clip of the pack; the first one also becomes the pack's talent. */
    suspend fun setPackTalents(pack: Pack, talents: List<Talent>) {
        talentLinker.setTalents(db.clipDao().getClips(pack.id).map { it.id }, talents)
        talents.firstOrNull()?.let { db.packDao().update(pack.copy(talent = it.nameEn)) }
    }

    val allClips: Flow<List<ClipWithPack>> = db.clipDao().observeAllClips()

    suspend fun getAllClips(): List<ClipWithPack> = db.clipDao().getAllClips()

    val talentSummaries: Flow<List<TalentSummary>> = db.clipDao().observeTalents().map { list -> list.filter { it.clipCount > 0 } }

    /** Clips of one talent; a null [slug] gives the clips that have no talent yet. */
    fun clipsByTalent(slug: String?): Flow<List<ClipWithPack>> =
        if (slug == null) db.clipDao().observeClipsWithoutTalent() else db.clipDao().observeClipsByTalent(slug)

    fun recentlyPlayed(limit: Int = 20): Flow<List<ClipWithPack>> = db.clipDao().observeRecentlyPlayed(limit)

    suspend fun clearPlayHistory() = db.clipDao().clearPlayHistory()

    /** A folder the app keeps in sync, for the Settings page. */
    data class SyncedFolder(val uri: String, val name: String, val isLibrary: Boolean, val topics: Int)

    /** Library folders ("Import many topics") and stand-alone imported folders. */
    suspend fun syncedFolders(): List<SyncedFolder> = withContext(Dispatchers.IO) {
        val libraries = libraryRoots.roots.map { root ->
            SyncedFolder(root, folderName(root), isLibrary = true, topics = db.packDao().getPacksWithSourceLike(childPrefix(root) + "%").size)
        }
        val singles = db.packDao().folderSources().filter { libraryRoots.rootOf(it) == null }.map { source ->
            val name = db.packDao().getPacksBySource(source).firstOrNull()?.name ?: folderName(source)
            SyncedFolder(source, name, isLibrary = false, topics = 1)
        }
        (libraries + singles).sortedBy { it.name.lowercase() }
    }

    /** Removes a synced folder: a library folder with all its topics, or a single-folder topic. */
    suspend fun removeSyncedFolder(folder: SyncedFolder) = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            if (folder.isLibrary) {
                db.packDao().getPacksWithSourceLike(childPrefix(folder.uri) + "%").forEach { db.packDao().delete(it.id) }
                libraryRoots.remove(folder.uri)
            } else {
                db.packDao().getPacksBySource(folder.uri).forEach { db.packDao().delete(it.id) }
            }
            importer.releaseTreeIfUnused(folder.uri)
        }
    }

    private fun folderName(uri: String): String =
        Uri.decode(Uri.parse(uri).lastPathSegment.orEmpty()).substringAfterLast(':').substringAfterLast('/').ifEmpty { uri }

    suspend fun markPlayed(clipId: Long) = db.clipDao().markPlayed(clipId, System.currentTimeMillis())

    suspend fun insertPlaylistEntryAt(playlistId: Long, clipId: Long, index: Int) =
        db.playlistDao().insertAt(playlistId, clipId, index)

    fun categories(packId: Long): Flow<List<String>> = db.clipDao().observeCategories(packId)

    private val syncMutex = Mutex()
    private val _syncing = MutableStateFlow(false)

    /** True while imported folders are being rescanned. */
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _syncMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** One message per sync that changed something (or found an unreadable folder, or was asked for by the user). */
    val syncMessages: SharedFlow<String> = _syncMessages.asSharedFlow()

    private var lastSyncAt = 0L

    suspend fun importFolder(uri: Uri): PackImporter.SyncResult = syncMutex.withLock {
        libraryRoots.include(uri.toString())
        importer.importFolder(uri)
    }

    /** "Import many topics": each sub-folder of [uri] becomes a topic, kept in sync from now on. */
    suspend fun importLibrary(uri: Uri): PackImporter.LibraryResult = syncMutex.withLock {
        libraryRoots.include(uri.toString())
        // If this folder was imported before as a single topic, its sub-folders now become topics instead.
        db.packDao().getPacksBySource(uri.toString()).forEach { db.packDao().delete(it.id) }
        val result = importer.importLibrary(uri, libraryRoots.excluded)
        libraryRoots.add(uri.toString())
        result
    }

    suspend fun importZip(uri: Uri): PackImporter.SyncResult = syncMutex.withLock { importer.importZip(uri) }

    /**
     * Rescans every imported folder so new files appear and deleted ones disappear without
     * importing again. Skipped when the last sync was less than [minIntervalMs] ago. A [manual]
     * sync (refresh button / pull to refresh) also reports when nothing changed.
     */
    suspend fun syncAll(minIntervalMs: Long = 0, manual: Boolean = false) {
        if (System.currentTimeMillis() - lastSyncAt < minIntervalMs) return
        syncMutex.withLock {
            _syncing.value = true
            try {
                var added = 0
                var removed = 0
                var topicsAdded = 0
                var topicsRemoved = 0
                val unreadable = mutableListOf<String>()
                val roots = libraryRoots.roots
                for (root in roots) {
                    val result = runCatching { importer.syncLibrary(root, libraryRoots.excluded) }.getOrNull()
                    if (result == null) {
                        unreadable += Uri.parse(root).lastPathSegment?.substringAfterLast(':') ?: "a library folder"
                    } else {
                        added += result.clipsAdded
                        removed += result.clipsRemoved
                        topicsAdded += result.topicsAdded
                        topicsRemoved += result.topicsRemoved
                    }
                }
                // Stand-alone folders; topics inside a library folder were synced above.
                for (source in db.packDao().folderSources().filter { libraryRoots.rootOf(it) == null }) {
                    val result = runCatching { importer.syncFolder(source) }.getOrNull()
                    if (result == null) {
                        db.packDao().getPacksBySource(source).firstOrNull()?.let { unreadable += it.name }
                    } else {
                        added += result.added
                        removed += result.removed
                    }
                }
                lastSyncAt = System.currentTimeMillis()
                val parts = buildList {
                    if (topicsAdded > 0) add("$topicsAdded new topics")
                    if (topicsRemoved > 0) add("$topicsRemoved topics removed")
                    if (added > 0) add("$added new clips")
                    if (removed > 0) add("$removed clips removed")
                }
                if (parts.isNotEmpty()) {
                    _syncMessages.emit("Library updated: ${parts.joinToString(", ")}")
                } else if (manual && unreadable.isEmpty()) {
                    _syncMessages.emit("Everything is up to date")
                }
                if (unreadable.isNotEmpty()) {
                    _syncMessages.emit("Can't read ${unreadable.joinToString()}. Import the folder again to restore access.")
                }
            } finally {
                _syncing.value = false
            }
        }
    }

    suspend fun setFavorite(clipId: Long, favorite: Boolean) = db.clipDao().setFavorite(clipId, favorite)

    suspend fun rename(pack: Pack, name: String, talent: String) =
        db.packDao().update(pack.copy(name = name.trim(), talent = talent.trim().ifEmpty { FileNames.UNKNOWN_TALENT }))

    suspend fun delete(packId: Long) = withContext(Dispatchers.IO) {
        val pack = db.packDao().getPack(packId) ?: return@withContext
        val clips = db.clipDao().getClips(packId)
        db.packDao().delete(packId)
        when (pack.source) {
            PackSource.ZIP -> importer.deleteExtracted(pack, clips)
            PackSource.FOLDER ->
                // A topic from a library folder: remember not to bring it back on the next sync.
                if (libraryRoots.rootOf(pack.sourceUri) != null) {
                    libraryRoots.exclude(pack.sourceUri)
                } else {
                    importer.releaseTreeIfUnused(pack.sourceUri)
                }
        }
    }
}
