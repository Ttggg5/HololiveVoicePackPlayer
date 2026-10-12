package com.ttggg5.holovoice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PackDao {
    @Query(
        """
        SELECT packs.*, (SELECT COUNT(*) FROM clips WHERE clips.packId = packs.id) AS clipCount
        FROM packs ORDER BY talent COLLATE NOCASE, name COLLATE NOCASE
        """,
    )
    fun observePacks(): Flow<List<PackWithCount>>

    @Query("SELECT * FROM packs WHERE id = :id")
    fun observePack(id: Long): Flow<Pack?>

    @Query("SELECT * FROM packs WHERE id = :id")
    suspend fun getPack(id: Long): Pack?

    @Query("SELECT COUNT(*) FROM packs WHERE sourceUri = :sourceUri")
    suspend fun countWithSource(sourceUri: String): Int

    @Insert
    suspend fun insert(pack: Pack): Long

    @Update
    suspend fun update(pack: Pack)

    @Query("DELETE FROM packs WHERE id = :id")
    suspend fun delete(id: Long)
}

private const val CLIP_WITH_PACK =
    "SELECT clips.*, packs.name AS packName, packs.talent AS talent, packs.coverUri AS coverUri " +
        "FROM clips INNER JOIN packs ON packs.id = clips.packId"

@Dao
interface ClipDao {
    @Query("$CLIP_WITH_PACK WHERE clips.packId = :packId ORDER BY clips.trackNo IS NULL, clips.trackNo, clips.title COLLATE NOCASE")
    fun observeClips(packId: Long): Flow<List<ClipWithPack>>

    @Query("$CLIP_WITH_PACK WHERE clips.isFavorite = 1 ORDER BY packs.talent COLLATE NOCASE, packs.name COLLATE NOCASE, clips.title COLLATE NOCASE")
    fun observeFavorites(): Flow<List<ClipWithPack>>

    @Query(
        "$CLIP_WITH_PACK WHERE clips.title LIKE '%' || :query || '%' " +
            "OR packs.name LIKE '%' || :query || '%' OR packs.talent LIKE '%' || :query || '%' " +
            "ORDER BY packs.talent COLLATE NOCASE, packs.name COLLATE NOCASE, clips.title COLLATE NOCASE LIMIT 500",
    )
    fun search(query: String): Flow<List<ClipWithPack>>

    @Query("SELECT * FROM clips WHERE packId = :packId")
    suspend fun getClips(packId: Long): List<Clip>

    @Insert
    suspend fun insertAll(clips: List<Clip>)

    @Query("UPDATE clips SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)
}
