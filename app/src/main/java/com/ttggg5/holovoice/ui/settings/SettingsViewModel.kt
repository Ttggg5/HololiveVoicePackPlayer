package com.ttggg5.holovoice.ui.settings

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.backup.Backup
import com.ttggg5.holovoice.data.backup.BackupManager
import com.ttggg5.holovoice.data.backup.BackupSummary
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: PackRepository, private val backup: BackupManager) : ViewModel() {
    /** A backup file that was opened and waits for the user to confirm the restore. */
    private val _pendingRestore = MutableStateFlow<Backup?>(null)
    val pendingRestore: StateFlow<Backup?> = _pendingRestore.asStateFlow()

    /** What the last restore brought back, shown in a dialog. */
    private val _restoreResult = MutableStateFlow<BackupSummary?>(null)
    val restoreResult: StateFlow<BackupSummary?> = _restoreResult.asStateFlow()

    private val _folders = MutableStateFlow<List<PackRepository.SyncedFolder>>(emptyList())
    val folders: StateFlow<List<PackRepository.SyncedFolder>> = _folders.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Done / total while talent pictures are downloading (also the automatic first download). */
    val pictureProgress: StateFlow<Pair<Int, Int>?> = repository.talentPictureProgress

    /** Topics, clips and talents in the library, for the summary at the top. */
    data class LibraryStats(val topics: Int = 0, val clips: Int = 0, val talents: Int = 0)

    val stats: StateFlow<LibraryStats> = combine(repository.packs, repository.talentSummaries) { packs, talents ->
        LibraryStats(packs.size, packs.sumOf { it.clipCount }, talents.count { it.slug != null })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryStats())

    init {
        reloadFolders()
    }

    private fun reloadFolders() {
        viewModelScope.launch { _folders.value = repository.syncedFolders() }
    }

    private fun run(onDone: (String) -> Unit, block: suspend () -> String) {
        viewModelScope.launch {
            _busy.value = true
            val message = try {
                block()
            } catch (e: Exception) {
                e.message ?: "Something went wrong"
            } finally {
                _busy.value = false
            }
            onDone(message)
        }
    }

    fun backUp(uri: Uri, onDone: (String) -> Unit) = run(onDone) {
        "Backed up " + backup.export(uri).describe()
    }

    /** Reads [uri]; a valid backup then waits in [pendingRestore] for confirmation. */
    fun openBackup(uri: Uri, onError: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                _pendingRestore.value = backup.read(uri)
            } catch (e: Exception) {
                onError(e.message ?: "Couldn't read the backup")
            } finally {
                _busy.value = false
            }
        }
    }

    fun cancelRestore() {
        _pendingRestore.value = null
    }

    fun confirmRestore(onError: (String) -> Unit) {
        val pending = _pendingRestore.value ?: return
        _pendingRestore.value = null
        run({ if (it.isNotEmpty()) onError(it) }) {
            _restoreResult.value = backup.restore(pending)
            reloadFolders()
            ""
        }
    }

    fun dismissRestoreResult() {
        _restoreResult.value = null
    }

    fun refreshTalents(onDone: (String) -> Unit) = run(onDone) {
        val count = repository.talents(forceRefresh = true).size
        val pictures = runCatching { repository.downloadTalentPictures() }.getOrNull()
        "Talent list updated ($count talents)" + (pictures?.takeIf { it.downloaded > 0 }?.let { ", ${it.downloaded} new pictures" } ?: "")
    }

    /** Downloads every talent's picture again and stores it in the app. */
    fun redownloadPictures(onDone: (String) -> Unit) = run(onDone) {
        val r = repository.downloadTalentPictures(force = true)
        "Downloaded ${r.downloaded} talent pictures" + if (r.failed > 0) " (${r.failed} failed, check your connection)" else ""
    }

    fun matchTalents(onDone: (String) -> Unit) = run(onDone) {
        val (matched, total) = repository.rematchAllTalents()
        "Matched a talent for $matched of $total clips"
    }

    fun syncNow(onDone: (String) -> Unit) = run(onDone) {
        repository.syncAll()
        reloadFolders()
        "Folders synced"
    }

    fun removeFolder(folder: PackRepository.SyncedFolder, onDone: (String) -> Unit) = run(onDone) {
        repository.removeSyncedFolder(folder)
        reloadFolders()
        "Removed \"${folder.name}\""
    }

    fun clearHistory(onDone: (String) -> Unit) = run(onDone) {
        repository.clearPlayHistory()
        "Play history cleared"
    }
}
