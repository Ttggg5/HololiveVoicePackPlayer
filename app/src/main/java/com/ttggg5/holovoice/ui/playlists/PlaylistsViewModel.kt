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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(private val repository: PackRepository) : ViewModel() {
    val playlists: StateFlow<List<PlaylistWithCount>?> =
        repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(repository.createPlaylist(name)) }
    }

    /** Loads a playlist's clips once, e.g. to play it straight from the list. */
    fun withClips(playlistId: Long, block: (List<ClipWithPack>) -> Unit) {
        viewModelScope.launch { block(repository.playlistItems(playlistId).first().map { it.item }) }
    }
}

class PlaylistViewModel(private val playlistId: Long, private val repository: PackRepository) : ViewModel() {
    val playlist: StateFlow<Playlist?> =
        repository.playlist(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val items: StateFlow<List<PlaylistItem>?> =
        repository.playlistItems(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun rename(name: String) {
        viewModelScope.launch { repository.renamePlaylist(playlistId, name) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            onDeleted()
        }
    }

    fun remove(item: PlaylistItem) {
        viewModelScope.launch { repository.removeFromPlaylist(item.entryId) }
    }

    fun move(from: Int, to: Int) {
        viewModelScope.launch { repository.movePlaylistEntry(playlistId, from, to) }
    }

    fun toggleFavorite(item: ClipWithPack) {
        viewModelScope.launch { repository.setFavorite(item.clip.id, !item.clip.isFavorite) }
    }
}
