package com.ttggg5.holovoice.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.ttggg5.holovoice.data.talents.TalentLinker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Turns a user-picked folder or ZIP into one topic ([Pack]). Audio in sub-folders becomes
 * categories of that topic. Folders are synced rather than re-imported: running the scan
 * again adds new files, removes deleted ones and keeps existing clips (favorites, playlists,
 * talent picks) untouched.
 */
class PackImporter(
    private val context: Context,
    private val db: AppDatabase,
    private val talentLinker: TalentLinker,
) {
    /** A file found by a scan, with the sub-folders (relative to the topic root) it sits in. */
    private data class ScannedFile(val name: String, val uri: String, val dirs: List<String>)

    /** What a sync changed. */
    data class SyncResult(val topicName: String, val added: Int, val removed: Int, val created: Boolean) {
        val changed: Boolean get() = added > 0 || removed > 0 || created
    }

    /** What syncing a library folder (one topic per sub-folder) changed. */
    data class LibraryResult(
        val rootName: String,
        val topicsAdded: Int,
        val topicsRemoved: Int,
        val clipsAdded: Int,
        val clipsRemoved: Int,
        val topics: Int,
        val looseFiles: Boolean,
    ) {
        val changed: Boolean get() = topicsAdded > 0 || topicsRemoved > 0 || clipsAdded > 0 || clipsRemoved > 0
    }

    val packsDir: File get() = File(context.filesDir, "packs")

    /** Imports a folder whose sub-folders are each a topic ("Import many topics"). */
    suspend fun importLibrary(treeUri: Uri, excluded: Set<String>): LibraryResult = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        syncLibrary(treeUri.toString(), excluded) ?: error("Cannot read folder")
    }

    /**
     * Rescans a library folder: every sub-folder becomes (or stays) a topic, topics whose
     * sub-folder is gone are removed. Returns null, changing nothing, if the folder can't be read.
     */
    suspend fun syncLibrary(rootUri: String, excluded: Set<String>): LibraryResult? = withContext(Dispatchers.IO) {
        val root = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(rootUri)) }.getOrNull()
        if (root == null || !root.isDirectory || !root.canRead()) return@withContext null
        val children = runCatching { root.listFiles().toList() }.getOrNull() ?: return@withContext null
        val subDirs = children.filter { it.isDirectory && !FileNames.isJunk(it.name.orEmpty()) }
        val looseFiles = children.any { !it.isDirectory && FileNames.isAudio(it.name.orEmpty()) }

        var topicsAdded = 0
        var clipsAdded = 0
        var clipsRemoved = 0
        val present = HashSet<String>()
        for (dir in subDirs) {
            val uri = dir.uri.toString()
            present += uri
            if (uri in excluded) continue
            val result = syncFolder(uri) ?: continue
            if (result.created) topicsAdded++
            clipsAdded += result.added
            clipsRemoved += result.removed
        }
        // Topics whose sub-folder was deleted or renamed.
        var topicsRemoved = 0
        for (pack in db.packDao().getPacksWithSourceLike(childPrefix(rootUri) + "%")) {
            if (pack.sourceUri !in present) {
                db.packDao().delete(pack.id)
                topicsRemoved++
            }
        }
        val topics = db.packDao().getPacksWithSourceLike(childPrefix(rootUri) + "%").size
        LibraryResult(root.name ?: "Library", topicsAdded, topicsRemoved, clipsAdded, clipsRemoved, topics, looseFiles)
    }

    /**
     * Imports a folder picked with ACTION_OPEN_DOCUMENT_TREE. Picking a folder that is already
     * in the library updates that topic instead of creating a duplicate.
     */
    suspend fun importFolder(treeUri: Uri): SyncResult = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val result = syncFolder(treeUri.toString()) ?: error("Cannot read folder")
        if (!result.created && db.packDao().countWithSource(treeUri.toString()) == 0) {
            releaseTreeIfUnused(treeUri.toString())
        }
        result
    }

    /**
     * Rescans an imported folder. Returns null, changing nothing, when the folder can't be read
     * (permission revoked, SD card removed), so a temporary problem never empties the library.
     */
    suspend fun syncFolder(treeUri: String): SyncResult? = withContext(Dispatchers.IO) {
        val root = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) }.getOrNull()
        if (root == null || !root.isDirectory || !root.canRead()) return@withContext null
        val audio = mutableListOf<ScannedFile>()
        val images = mutableListOf<ScannedFile>()
        val walked = runCatching { walkTree(root, emptyList(), audio, images) }.isSuccess
        if (!walked) return@withContext null
        applyScan(treeUri, PackSource.FOLDER, root.name ?: "Voice pack", audio, images)
    }

    private fun walkTree(dir: DocumentFile, dirs: List<String>, audio: MutableList<ScannedFile>, images: MutableList<ScannedFile>) {
        for (file in dir.listFiles()) {
            val name = file.name ?: continue
            if (FileNames.isJunk(name)) continue
            when {
                file.isDirectory -> walkTree(file, dirs + name, audio, images)
                FileNames.isAudio(name) -> audio += ScannedFile(name, file.uri.toString(), dirs)
                FileNames.isImage(name) -> images += ScannedFile(name, file.uri.toString(), dirs)
            }
        }
    }

    /** Imports a ZIP picked with ACTION_OPEN_DOCUMENT by extracting it into app storage. */
    suspend fun importZip(zipUri: Uri): SyncResult = withContext(Dispatchers.IO) {
        val zipName = displayName(zipUri)?.let(FileNames::baseName) ?: "Voice pack"
        val dest = File(packsDir, UUID.randomUUID().toString())
        try {
            // Japanese archives are frequently Shift_JIS encoded, which ZipInputStream rejects as UTF-8.
            val extracted = try {
                extract(zipUri, dest, Charsets.UTF_8)
            } catch (e: IllegalArgumentException) {
                dest.deleteRecursively()
                extract(zipUri, dest, Charset.forName("MS932"))
            }
            val rootPath = dest.canonicalFile
            val relativeDirs = extracted.map { file ->
                file.parentFile!!.canonicalFile.relativeTo(rootPath).path.split(File.separatorChar).filter { it.isNotEmpty() }
            }
            val dirs = FileNames.stripCommonRoot(relativeDirs)
            val scanned = extracted.zip(dirs) { file, d -> ScannedFile(file.name, Uri.fromFile(file).toString(), d) }
            val audio = scanned.filter { FileNames.isAudio(it.name) }
            if (audio.isEmpty()) {
                dest.deleteRecursively()
                return@withContext SyncResult(zipName, 0, 0, created = false)
            }
            applyScan(dest.path, PackSource.ZIP, zipName, audio, scanned.filter { FileNames.isImage(it.name) })
        } catch (e: Exception) {
            dest.deleteRecursively()
            throw e
        }
    }

    /** Writes a scan result into the single topic that belongs to [sourceUri]. */
    private suspend fun applyScan(
        sourceUri: String,
        source: PackSource,
        rootName: String,
        audio: List<ScannedFile>,
        images: List<ScannedFile>,
    ): SyncResult {
        val packDao = db.packDao()
        val clipDao = db.clipDao()
        val existingPacks = packDao.getPacksBySource(sourceUri)
        val existingClips = clipDao.getClipsBySource(sourceUri)
        if (audio.isEmpty() && existingPacks.isEmpty()) return SyncResult(rootName, 0, 0, created = false)

        val cover = pickCover(images)
        val primary = existingPacks.firstOrNull()
        val packId: Long
        val topicName: String
        if (primary == null) {
            topicName = rootName
            packId = packDao.insert(
                Pack(
                    name = rootName,
                    talent = FileNames.guessTalent(rootName, null),
                    coverUri = cover,
                    sourceUri = sourceUri,
                    source = source,
                ),
            )
        } else {
            // Libraries from before topics had one pack per sub-folder; the merged topic takes the folder's name.
            topicName = if (existingPacks.size > 1) rootName else primary.name
            packId = primary.id
            val updated = primary.copy(name = topicName, coverUri = cover ?: primary.coverUri)
            if (updated != primary) packDao.update(updated)
        }

        val byUri = existingClips.associateBy { it.fileUri }
        val kept = HashSet<Long>()
        val newClips = mutableListOf<Clip>()
        for (file in audio) {
            val (track, title) = FileNames.parseClip(file.name)
            val category = FileNames.categoryOf(file.dirs)
            val old = byUri[file.uri]
            if (old != null) {
                kept += old.id
                if (old.packId != packId || old.category != category || old.trackNo != track) {
                    clipDao.updatePlacement(old.id, packId, category, track)
                }
            } else {
                newClips += Clip(
                    packId = packId,
                    title = title,
                    fileUri = file.uri,
                    durationMs = durationOf(file.uri),
                    trackNo = track,
                    category = category,
                    addedAt = System.currentTimeMillis(),
                )
            }
        }
        val removed = existingClips.map { it.id }.filterNot { it in kept }
        removed.chunked(900).forEach { clipDao.deleteClips(it) }
        existingPacks.drop(1).forEach { packDao.delete(it.id) }

        if (newClips.isNotEmpty()) {
            val ids = clipDao.insertAll(newClips)
            // Offline is fine: clips just stay without a talent until "Match talents" is run.
            val talents = runCatching { talentLinker.directory.load() }.getOrDefault(emptyList())
            talentLinker.matchClips(newClips.zip(ids) { clip, id -> clip.copy(id = id) }, talents)
        }
        if (audio.isEmpty()) {
            packDao.delete(packId)
        } else {
            adoptMostCommonTalent(packId)
        }
        return SyncResult(topicName, added = newClips.size, removed = removed.size, created = primary == null)
    }

    /** Prefers a cover-like image in the topic's root, then in any category. */
    private fun pickCover(images: List<ScannedFile>): String? {
        val pool = images.filter { it.dirs.isEmpty() }.ifEmpty { images }
        val name = FileNames.pickCover(pool.map { it.name }) ?: return null
        return pool.first { it.name == name }.uri
    }

    private fun extract(zipUri: Uri, dest: File, charset: Charset): List<File> {
        val files = mutableListOf<File>()
        val input = context.contentResolver.openInputStream(zipUri) ?: error("Cannot open ZIP")
        ZipInputStream(input.buffered(), charset).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\', '/')
                if (entry.isDirectory || FileNames.isJunk(name)) continue
                if (!FileNames.isAudio(name) && !FileNames.isImage(name)) continue
                val target = FileNames.safeResolve(dest, name) ?: continue
                target.parentFile?.mkdirs()
                target.outputStream().use { zip.copyTo(it) }
                files += target
            }
        }
        return files
    }

    /** Gives a pack whose talent couldn't be guessed from its name the talent most of its clips matched. */
    private suspend fun adoptMostCommonTalent(packId: Long) {
        val pack = db.packDao().getPack(packId) ?: return
        if (pack.talent != FileNames.UNKNOWN_TALENT) return
        val name = db.clipDao().getClips(packId).mapNotNull { it.talentName }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: return
        db.packDao().update(pack.copy(talent = name))
    }

    private fun durationOf(uri: String): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(uri))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    /** Drops the persisted read permission on a picked folder once no pack refers to it any more. */
    suspend fun releaseTreeIfUnused(treeUri: String) {
        if (db.packDao().countWithSource(treeUri) > 0) return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(treeUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    /** Removes the extracted files of a ZIP topic (older libraries: only this pack's files, pruning empty folders). */
    suspend fun deleteExtracted(pack: Pack, clips: List<Clip>) {
        val topicDir = File(pack.sourceUri)
        val othersInside = db.packDao().countWithSourceLike(topicDir.path + File.separator + "%")
        if (topicDir.parentFile?.canonicalPath == packsDir.canonicalPath && othersInside == 0) {
            topicDir.deleteRecursively()
            return
        }
        val root = packsDir.path + File.separator
        clips.mapNotNull { Uri.parse(it.fileUri).path?.let(::File) }
            .filter { it.path.startsWith(root) }
            .forEach { it.delete() }
        var dir: File? = File(pack.sourceUri)
        while (dir != null && dir.path.startsWith(root)) {
            val children = dir.listFiles().orEmpty()
            // Keep directories that still feed another pack (sub-folders or remaining audio).
            if (children.any { it.isDirectory || FileNames.isAudio(it.name) }) break
            dir.deleteRecursively()
            dir = dir.parentFile
        }
    }
}
