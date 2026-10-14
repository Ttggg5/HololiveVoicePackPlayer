package com.ttggg5.holovoice.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.Playlist
import com.ttggg5.holovoice.data.PlaylistItem
import com.ttggg5.holovoice.data.PlaylistWithCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(private val repository: PackRepository) : ViewModel() {
    val playlists: StateFlow<List<PlaylistWithCount>?> =
        repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Up to four topic thumbnails per playlist, for the mosaics. */
    val covers: StateFlow<Map<Long, List<String>>> =
        repository.playlistCovers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** For the pinned Favorites card. */
    val favoriteCount: StateFlow<Int> =
        repository.favorites.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun rename(id: Long, name: String) {
        viewModelScope.launch { repository.renamePlaylist(id, name) }
    }

    /** Deletes a playlist; [onDeleted] gets a function that recreates it (undo). */
    fun delete(id: Long, name: String, onDeleted: (undo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            val clipIds = repository.playlistItems(id).first().map { it.item.clip.id }
            repository.deletePlaylist(id)
            onDeleted {
                viewModelScope.launch {
                    val newId = repository.createPlaylist(name)
                    repository.addToPlaylist(newId, clipIds)
                }
            }
        }
    }

    fun create(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(repository.createPlaylist(name)) }
    }

    /** Loads a playlist's clips once, e.g. to play it straight from the list. */
    fun withClips(playlistId: Long, block: (List<ClipWithPack>) -> Unit) {
        viewModelScope.launch { block(repository.playlistItems(playlistId).first().map { it.item }) }
    }
}

class PlaylistViewModel(private val playlistId: Long, private val repository: PackRepository) : ViewModel() {
    /** Up to four topic thumbnails of this playlist, for its mosaic and banner. */
    val covers: StateFlow<List<String>> = repository.playlistCovers.map { it[playlistId].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlist: StateFlow<Playlist?> =
        repository.playlist(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val items: StateFlow<List<PlaylistItem>?> =
        repository.playlistItems(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun rename(name: String) {
        viewModelScope.launch { repository.renamePlaylist(playlistId, name) }
    }

    /** Deletes the playlist; [onDeleted] gets a function that recreates it (undo). */
    fun delete(onDeleted: (undo: suspend () -> Unit) -> Unit) {
        viewModelScope.launch {
            val name = playlist.value?.name ?: return@launch
            val clipIds = repository.playlistItems(playlistId).first().map { it.item.clip.id }
            repository.deletePlaylist(playlistId)
            onDeleted {
                val id = repository.createPlaylist(name)
                repository.addToPlaylist(id, clipIds)
            }
        }
    }

    /** Removes one row; returns an undo action. */
    fun remove(item: PlaylistItem, index: Int, onRemoved: (undo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            repository.removeFromPlaylist(item.entryId)
            onRemoved {
                viewModelScope.launch { repository.insertPlaylistEntryAt(playlistId, item.item.clip.id, index) }
            }
        }
    }

    fun move(from: Int, to: Int) {
        viewModelScope.launch { repository.movePlaylistEntry(playlistId, from, to) }
    }

    fun toggleFavorite(item: ClipWithPack) {
        viewModelScope.launch { repository.setFavorite(item.clip.id, !item.clip.isFavorite) }
    }
}
