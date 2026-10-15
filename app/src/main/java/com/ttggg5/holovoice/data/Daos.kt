package com.ttggg5.holovoice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PackDao {
    @Query(
        """
        SELECT packs.*,
            (SELECT COUNT(*) FROM clips WHERE clips.packId = packs.id) AS clipCount,
            (SELECT COUNT(DISTINCT category) FROM clips WHERE clips.packId = packs.id) AS categoryCount
        FROM packs ORDER BY talent COLLATE NOCASE, name COLLATE NOCASE
        """,
    )
    fun observePacks(): Flow<List<PackWithCount>>

    @Query("SELECT * FROM packs WHERE id = :id")
    fun observePack(id: Long): Flow<Pack?>

    @Query("SELECT * FROM packs WHERE id = :id")
    suspend fun getPack(id: Long): Pack?

    @Query("SELECT * FROM packs WHERE sourceUri = :sourceUri ORDER BY id")
    suspend fun getPacksBySource(sourceUri: String): List<Pack>

    @Query("SELECT * FROM packs WHERE sourceUri LIKE :pattern")
    suspend fun getPacksWithSourceLike(pattern: String): List<Pack>

    @Query("SELECT DISTINCT sourceUri FROM packs WHERE source = 'FOLDER'")
    suspend fun folderSources(): List<String>

    @Query("SELECT COUNT(*) FROM packs WHERE sourceUri LIKE :pattern")
    suspend fun countWithSourceLike(pattern: String): Int

    @Query("SELECT COUNT(*) FROM packs WHERE sourceUri = :sourceUri")
    suspend fun countWithSource(sourceUri: String): Int

    @Insert
    suspend fun insert(pack: Pack): Long

    @Update
    suspend fun update(pack: Pack)

    @Query("DELETE FROM packs WHERE id = :id")
    suspend fun delete(id: Long)
}

internal const val CLIP_COLUMNS =
    "clips.*, packs.name AS packName, packs.talent AS talent, packs.coverUri AS coverUri"

internal const val CLIP_WITH_PACK =
    "SELECT $CLIP_COLUMNS FROM clips INNER JOIN packs ON packs.id = clips.packId"

@Dao
interface ClipDao {
    @Transaction
    @Query(
        "$CLIP_WITH_PACK WHERE clips.packId = :packId ORDER BY clips.category IS NOT NULL, " +
            "clips.category COLLATE NOCASE, clips.trackNo IS NULL, clips.trackNo, clips.title COLLATE NOCASE",
    )
    fun observeClips(packId: Long): Flow<List<ClipWithPack>>

    @Query("SELECT DISTINCT category FROM clips WHERE packId = :packId AND category IS NOT NULL ORDER BY category COLLATE NOCASE")
    fun observeCategories(packId: Long): Flow<List<String>>

    @Transaction
    @Query("$CLIP_WITH_PACK")
    fun observeAllClips(): Flow<List<ClipWithPack>>

    @Transaction
    @Query("$CLIP_WITH_PACK")
    suspend fun getAllClips(): List<ClipWithPack>

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.id IN (SELECT clipId FROM clip_talents WHERE slug = :slug)")
    fun observeClipsByTalent(slug: String): Flow<List<ClipWithPack>>

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.id NOT IN (SELECT clipId FROM clip_talents)")
    fun observeClipsWithoutTalent(): Flow<List<ClipWithPack>>

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.lastPlayedAt > 0 ORDER BY clips.lastPlayedAt DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int): Flow<List<ClipWithPack>>

    @Query(
        """
        SELECT ct.slug AS slug, MAX(ct.name) AS name, MAX(ct.imageUrl) AS imageUrl,
            COUNT(*) AS clipCount, SUM(c.playCount) AS plays, MAX(c.lastPlayedAt) AS lastPlayed,
            COUNT(DISTINCT c.packId) AS topicCount
        FROM clip_talents ct INNER JOIN clips c ON c.id = ct.clipId GROUP BY ct.slug
        UNION ALL
        SELECT NULL AS slug, NULL AS name, NULL AS imageUrl,
            COUNT(*) AS clipCount, COALESCE(SUM(playCount), 0) AS plays, COALESCE(MAX(lastPlayedAt), 0) AS lastPlayed,
            COUNT(DISTINCT packId) AS topicCount
        FROM clips WHERE id NOT IN (SELECT clipId FROM clip_talents)
        """,
    )
    fun observeTalents(): Flow<List<TalentSummary>>

    @Query("UPDATE clips SET lastPlayedAt = 0, playCount = 0")
    suspend fun clearPlayHistory()

    @Query("UPDATE clips SET lastPlayedAt = :now, playCount = playCount + 1 WHERE id = :id")
    suspend fun markPlayed(id: Long, now: Long)

    @Query("SELECT clips.* FROM clips INNER JOIN packs ON packs.id = clips.packId WHERE packs.sourceUri = :sourceUri")
    suspend fun getClipsBySource(sourceUri: String): List<Clip>

    @Query("UPDATE clips SET packId = :packId, category = :category, trackNo = :trackNo WHERE id = :id")
    suspend fun updatePlacement(id: Long, packId: Long, category: String?, trackNo: Int?)

    @Query("DELETE FROM clips WHERE id IN (:ids)")
    suspend fun deleteClips(ids: List<Long>)

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.isFavorite = 1 ORDER BY packs.talent COLLATE NOCASE, packs.name COLLATE NOCASE, clips.title COLLATE NOCASE")
    fun observeFavorites(): Flow<List<ClipWithPack>>

    @Transaction
    @Query(
        "$CLIP_WITH_PACK WHERE clips.title LIKE '%' || :query || '%' " +
            "OR packs.name LIKE '%' || :query || '%' OR packs.talent LIKE '%' || :query || '%' " +
            "OR clips.talentName LIKE '%' || :query || '%' " +
            "ORDER BY packs.talent COLLATE NOCASE, packs.name COLLATE NOCASE, clips.title COLLATE NOCASE LIMIT 500",
    )
    fun search(query: String): Flow<List<ClipWithPack>>

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.id = :id")
    fun observeClip(id: Long): Flow<ClipWithPack?>

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ClipWithPack>

    @Query("SELECT * FROM clips WHERE packId = :packId")
    suspend fun getClips(packId: Long): List<Clip>

    @Query("SELECT * FROM clips")
    suspend fun getAll(): List<Clip>

    @Query(
        "UPDATE clips SET talentSlug = :slug, talentName = :name, talentImageUrl = :imageUrl, " +
            "talentManual = :manual WHERE id IN (:ids)",
    )
    suspend fun setTalent(ids: List<Long>, slug: String?, name: String?, imageUrl: String?, manual: Boolean)

    @Insert
    suspend fun insertAll(clips: List<Clip>): List<Long>

    @Query("UPDATE clips SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)
}

@Dao
abstract class PlaylistDao {
    @Query(
        """
        SELECT playlists.*,
            (SELECT COUNT(*) FROM playlist_entries e WHERE e.playlistId = playlists.id) AS entryCount,
            (SELECT packs.coverUri FROM playlist_entries e
                INNER JOIN clips ON clips.id = e.clipId
                INNER JOIN packs ON packs.id = clips.packId
                WHERE e.playlistId = playlists.id AND packs.coverUri IS NOT NULL
                ORDER BY e.position LIMIT 1) AS coverUri,
            (SELECT COALESCE(SUM(clips.durationMs), 0) FROM playlist_entries e
                INNER JOIN clips ON clips.id = e.clipId
                WHERE e.playlistId = playlists.id) AS totalMs
        FROM playlists ORDER BY name COLLATE NOCASE
        """,
    )
    abstract fun observeAll(): Flow<List<PlaylistWithCount>>

    @Query(
        """
        SELECT e.playlistId AS playlistId, packs.coverUri AS coverUri FROM playlist_entries e
            INNER JOIN clips ON clips.id = e.clipId
            INNER JOIN packs ON packs.id = clips.packId
            WHERE packs.coverUri IS NOT NULL
            ORDER BY e.playlistId, e.position
        """,
    )
    abstract fun observeCovers(): Flow<List<PlaylistCover>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    abstract fun observe(id: Long): Flow<Playlist?>

    @Transaction
    @Query(
        "SELECT $CLIP_COLUMNS, e.id AS entryId FROM playlist_entries e " +
            "INNER JOIN clips ON clips.id = e.clipId INNER JOIN packs ON packs.id = clips.packId " +
            "WHERE e.playlistId = :playlistId ORDER BY e.position",
    )
    abstract fun observeEntries(playlistId: Long): Flow<List<PlaylistItem>>

    @Insert
    abstract suspend fun insert(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_entries WHERE playlistId = :playlistId")
    abstract suspend fun maxPosition(playlistId: Long): Int

    @Insert
    abstract suspend fun insertEntries(entries: List<PlaylistEntry>)

    @Query("DELETE FROM playlist_entries WHERE id = :entryId")
    abstract suspend fun removeEntry(entryId: Long)

    @Query("SELECT id FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position")
    abstract suspend fun entryIds(playlistId: Long): List<Long>

    @Query("UPDATE playlist_entries SET position = :position WHERE id = :entryId")
    abstract suspend fun setPosition(entryId: Long, position: Int)

    @Query("UPDATE playlist_entries SET position = position + 1 WHERE playlistId = :playlistId AND position >= :position")
    abstract suspend fun shiftFrom(playlistId: Long, position: Int)

    /** Inserts [clipId] at [index] (used to undo a removal). */
    @Transaction
    open suspend fun insertAt(playlistId: Long, clipId: Long, index: Int) {
        val ids = entryIds(playlistId)
        ids.forEachIndexed { i, entryId -> setPosition(entryId, i) }
        val position = index.coerceIn(0, ids.size)
        shiftFrom(playlistId, position)
        insertEntries(listOf(PlaylistEntry(playlistId = playlistId, clipId = clipId, position = position)))
    }

    /** Appends [clipIds] to the end of the playlist. */
    @Transaction
    open suspend fun addClips(playlistId: Long, clipIds: List<Long>) {
        val start = maxPosition(playlistId) + 1
        insertEntries(clipIds.mapIndexed { i, clipId -> PlaylistEntry(playlistId = playlistId, clipId = clipId, position = start + i) })
    }

    /** Moves the entry at index [from] to index [to] and renumbers all positions. */
    @Transaction
    open suspend fun move(playlistId: Long, from: Int, to: Int) {
        entryIds(playlistId).moved(from, to).forEachIndexed { index, entryId -> setPosition(entryId, index) }
    }
}

@Dao
abstract class ClipTalentDao {
    @Query("DELETE FROM clip_talents WHERE clipId = :clipId")
    abstract suspend fun deleteFor(clipId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(talents: List<ClipTalent>)

    @Query(
        "UPDATE clips SET talentSlug = :slug, talentName = :name, talentImageUrl = :imageUrl, " +
            "talentManual = :manual WHERE id = :clipId",
    )
    abstract suspend fun setPrimary(clipId: Long, slug: String?, name: String?, imageUrl: String?, manual: Boolean)

    @Query("UPDATE clip_talents SET imageUrl = :imageUrl WHERE slug = :slug AND imageUrl IS NOT :imageUrl")
    abstract suspend fun setTalentPicture(slug: String, imageUrl: String?)

    @Query("UPDATE clips SET talentImageUrl = :imageUrl WHERE talentSlug = :slug AND talentImageUrl IS NOT :imageUrl")
    abstract suspend fun setPrimaryPicture(slug: String, imageUrl: String?)

    @Query("UPDATE clip_talents SET name = :name WHERE slug = :slug AND name IS NOT :name")
    abstract suspend fun setTalentName(slug: String, name: String)

    @Query("UPDATE clips SET talentName = :name WHERE talentSlug = :slug AND talentName IS NOT :name")
    abstract suspend fun setPrimaryName(slug: String, name: String)

    /** Gives every clip of these talents (slug to name) the current name, e.g. without an "[Alum]" tag. */
    @Transaction
    open suspend fun setNames(names: Map<String, String>) {
        names.forEach { (slug, name) ->
            setTalentName(slug, name)
            setPrimaryName(slug, name)
        }
    }

    /** Points every clip of [pictures]' talents (slug to picture) at that picture. */
    @Transaction
    open suspend fun setPictures(pictures: Map<String, String?>) {
        pictures.forEach { (slug, uri) ->
            setTalentPicture(slug, uri)
            setPrimaryPicture(slug, uri)
        }
    }

    @Query("SELECT DISTINCT clipId FROM clip_talents WHERE slug = :slug")
    abstract suspend fun clipsWith(slug: String): List<Long>

    @Query("SELECT * FROM clip_talents WHERE clipId = :clipId ORDER BY position")
    abstract suspend fun talentsOf(clipId: Long): List<ClipTalent>

    @Query("UPDATE clips SET talentSlug = NULL, talentName = NULL, talentImageUrl = NULL WHERE talentSlug = :slug")
    abstract suspend fun clearPrimary(slug: String)

    /** Takes [slug] off every clip; the clips keep their other talents. */
    @Transaction
    open suspend fun removeTalent(slug: String) {
        for (clipId in clipsWith(slug)) {
            replace(clipId, talentsOf(clipId).filterNot { it.slug == slug }, manual = false)
        }
        clearPrimary(slug)
    }

    /** Replaces all talents of a clip; the first one is also stored as the clip's primary talent. */
    @Transaction
    open suspend fun replace(clipId: Long, talents: List<ClipTalent>, manual: Boolean) {
        deleteFor(clipId)
        if (talents.isNotEmpty()) insertAll(talents.mapIndexed { i, t -> t.copy(clipId = clipId, position = i) })
        val first = talents.firstOrNull()
        setPrimary(clipId, first?.slug, first?.name, first?.imageUrl, manual)
    }
}
