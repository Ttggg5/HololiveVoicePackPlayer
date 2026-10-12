package com.ttggg5.holovoice.ui.pack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.Pack
import com.ttggg5.holovoice.data.PackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PackViewModel(packId: Long, private val repository: PackRepository) : ViewModel() {
    val pack: StateFlow<Pack?> =
        repository.pack(packId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val clips: StateFlow<List<ClipWithPack>> =
        repository.clips(packId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(item: ClipWithPack) {
        viewModelScope.launch { repository.setFavorite(item.clip.id, !item.clip.isFavorite) }
    }
}
