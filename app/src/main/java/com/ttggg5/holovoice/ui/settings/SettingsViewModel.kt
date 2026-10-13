package com.ttggg5.holovoice.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.PackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: PackRepository) : ViewModel() {
    private val _folders = MutableStateFlow<List<PackRepository.SyncedFolder>>(emptyList())
    val folders: StateFlow<List<PackRepository.SyncedFolder>> = _folders.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Done / total while talent pictures are downloading (also the automatic first download). */
    val pictureProgress: StateFlow<Pair<Int, Int>?> = repository.talentPictureProgress

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
