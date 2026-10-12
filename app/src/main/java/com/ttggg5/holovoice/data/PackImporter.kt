package com.ttggg5.holovoice.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Scans user-picked folders and ZIP archives for audio and turns every directory
 * that directly contains audio files into one [Pack].
 */
class PackImporter(
    private val context: Context,
    private val db: AppDatabase,
) {
    /** One directory that holds audio files, before it is written to the database. */
    private data class Candidate(
        val name: String,
        val parentName: String?,
        val audio: List<Pair<String, String>>, // file name -> uri
        val images: List<Pair<String, String>>, // file name -> uri
        val sourceUri: String,
        val source: PackSource,
    )

    val packsDir: File get() = File(context.filesDir, "packs")

    /** Imports a folder picked with ACTION_OPEN_DOCUMENT_TREE. Returns the number of packs created. */
    suspend fun importFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Cannot open folder")
        val candidates = mutableListOf<Candidate>()
        walkTree(root, parentName = null, treeUri = treeUri.toString(), out = candidates)
        if (candidates.isEmpty()) {
            releaseTreeIfUnused(treeUri.toString())
        }
        save(candidates)
    }

    private fun walkTree(dir: DocumentFile, parentName: String?, treeUri: String, out: MutableList<Candidate>) {
        val audio = mutableListOf<Pair<String, String>>()
        val images = mutableListOf<Pair<String, String>>()
        val subDirs = mutableListOf<DocumentFile>()
        for (file in dir.listFiles()) {
            val name = file.name ?: continue
            if (FileNames.isJunk(name)) continue
            when {
                file.isDirectory -> subDirs += file
                FileNames.isAudio(name) -> audio += name to file.uri.toString()
                FileNames.isImage(name) -> images += name to file.uri.toString()
            }
        }
        val dirName = dir.name ?: "Voice pack"
        if (audio.isNotEmpty()) {
            out += Candidate(dirName, parentName, audio, images, treeUri, PackSource.FOLDER)
        }
        for (sub in subDirs) walkTree(sub, dirName, treeUri, out)
    }

    /** Imports a ZIP picked with ACTION_OPEN_DOCUMENT by extracting it into app storage. */
    suspend fun importZip(zipUri: Uri): Int = withContext(Dispatchers.IO) {
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
            val candidates = extracted
                .groupBy { it.parentFile!! }
                .mapNotNull { (dir, files) ->
                    val audio = files.filter { FileNames.isAudio(it.name) }
                    if (audio.isEmpty()) return@mapNotNull null
                    val images = files.filter { FileNames.isImage(it.name) }
                        .ifEmpty { extracted.filter { it.parentFile == dir.parentFile && FileNames.isImage(it.name) } }
                    val isRoot = dir == dest
                    Candidate(
                        name = if (isRoot) zipName else dir.name,
                        parentName = if (isRoot || dir.parentFile == dest) zipName else dir.parentFile?.name,
                        audio = audio.map { it.name to Uri.fromFile(it).toString() },
                        images = images.map { it.name to Uri.fromFile(it).toString() },
                        sourceUri = dir.path,
                        source = PackSource.ZIP,
                    )
                }
            if (candidates.isEmpty()) dest.deleteRecursively()
            save(candidates)
        } catch (e: Exception) {
            dest.deleteRecursively()
            throw e
        }
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

    private suspend fun save(candidates: List<Candidate>): Int {
        for (c in candidates) {
            val packId = db.packDao().insert(
                Pack(
                    name = c.name,
                    talent = FileNames.guessTalent(c.name, c.parentName),
                    coverUri = FileNames.pickCover(c.images.map { it.first })
                        ?.let { cover -> c.images.first { it.first == cover }.second },
                    sourceUri = c.sourceUri,
                    source = c.source,
                ),
            )
            val clips = c.audio.map { (name, uri) ->
                val (track, title) = FileNames.parseClip(name)
                Clip(packId = packId, title = title, fileUri = uri, durationMs = durationOf(uri), trackNo = track)
            }
            db.clipDao().insertAll(clips)
        }
        return candidates.size
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

    /** Removes the extracted files of a ZIP pack and prunes directories left without audio. */
    fun deleteExtracted(pack: Pack, clips: List<Clip>) {
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
