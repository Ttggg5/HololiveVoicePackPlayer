package com.ttggg5.holovoice.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PackRepository(
    private val db: AppDatabase,
    private val importer: PackImporter,
) {
    val packs: Flow<List<PackWithCount>> = db.packDao().observePacks()
    val favorites: Flow<List<ClipWithPack>> = db.clipDao().observeFavorites()

    fun pack(id: Long): Flow<Pack?> = db.packDao().observePack(id)

    fun clips(packId: Long): Flow<List<ClipWithPack>> = db.clipDao().observeClips(packId)

    fun search(query: String): Flow<List<ClipWithPack>> = db.clipDao().search(query)

    suspend fun importFolder(uri: Uri): Int = importer.importFolder(uri)

    suspend fun importZip(uri: Uri): Int = importer.importZip(uri)

    suspend fun setFavorite(clipId: Long, favorite: Boolean) = db.clipDao().setFavorite(clipId, favorite)

    suspend fun rename(pack: Pack, name: String, talent: String) =
        db.packDao().update(pack.copy(name = name.trim(), talent = talent.trim().ifEmpty { "Unknown" }))

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
