package com.ttggg5.holovoice.data

import android.net.Uri
import com.ttggg5.holovoice.data.talents.Talent
import com.ttggg5.holovoice.data.talents.TalentLinker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PackRepository(
    private val db: AppDatabase,
    private val importer: PackImporter,
    private val talentLinker: TalentLinker,
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

    /** Re-runs automatic matching on every clip (manual picks are kept). Returns matched and total counts. */
    suspend fun rematchAllTalents(): Pair<Int, Int> {
        val talents = talentLinker.directory.load()
        if (talents.isEmpty()) error("Couldn't download the talent list. Check your connection.")
        val clips = db.clipDao().getAll()
        return talentLinker.matchClips(clips, talents) to clips.size
    }

    suspend fun setClipTalent(clipIds: List<Long>, talent: Talent?) = talentLinker.setTalent(clipIds, talent)

    /** Sets [talent] on every clip of the pack and, when not null, as the pack's talent. */
    suspend fun setPackTalent(pack: Pack, talent: Talent?) {
        talentLinker.setTalent(db.clipDao().getClips(pack.id).map { it.id }, talent)
        if (talent != null) db.packDao().update(pack.copy(talent = talent.nameEn))
    }

    suspend fun importFolder(uri: Uri): Int = importer.importFolder(uri)

    suspend fun importZip(uri: Uri): Int = importer.importZip(uri)

    suspend fun setFavorite(clipId: Long, favorite: Boolean) = db.clipDao().setFavorite(clipId, favorite)

    suspend fun rename(pack: Pack, name: String, talent: String) =
        db.packDao().update(pack.copy(name = name.trim(), talent = talent.trim().ifEmpty { FileNames.UNKNOWN_TALENT }))

    suspend fun delete(packId: Long) = withContext(Dispatchers.IO) {
        val pack = db.packDao().getPack(packId) ?: return@withContext
        val clips = db.clipDao().getClips(packId)
        db.packDao().delete(packId)
        when (pack.source) {
            PackSource.ZIP -> importer.deleteExtracted(pack, clips)
            PackSource.FOLDER -> importer.releaseTreeIfUnused(pack.sourceUri)
        }
    }
}
