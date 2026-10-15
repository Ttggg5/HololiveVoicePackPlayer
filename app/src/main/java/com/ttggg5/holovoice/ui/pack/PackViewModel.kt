package com.ttggg5.holovoice.ui.pack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.Pack
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.talents.Talent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PackViewModel(packId: Long, private val repository: PackRepository) : ViewModel() {
    val pack: StateFlow<Pack?> =
        repository.pack(packId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    /** Sub-folder categories of this topic, in display order. */
    val categories: StateFlow<List<String>> =
        repository.categories(packId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clips: StateFlow<List<ClipWithPack>> =
        repository.clips(packId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rename(pack: Pack, name: String, talent: String) {
        viewModelScope.launch { repository.rename(pack, name, talent) }
    }

    /** Sets [talents] on every clip of the topic; [onDone] gets a message to show. */
    fun setTalents(pack: Pack, talents: List<Talent>, onDone: (String) -> Unit) {
        viewModelScope.launch {
            repository.setPackTalents(pack, talents)
            onDone(
                if (talents.isEmpty()) "Cleared talents for ${pack.name}"
                else "Set ${talents.joinToString(" & ") { it.nameEn }} for ${pack.name}",
            )
        }
    }

    /** Re-runs automatic talent matching (manual picks are kept). */
    fun matchTalents(onDone: (String) -> Unit) {
        viewModelScope.launch {
            onDone(
                try {
                    val (matched, total) = repository.rematchAllTalents()
                    "Matched a talent for $matched of $total clips"
                } catch (e: Exception) {
                    e.message ?: "Talent matching failed"
                },
            )
        }
    }

    fun delete(pack: Pack, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(pack.id)
            onDone()
        }
    }

    fun toggleFavorite(item: ClipWithPack) {
        viewModelScope.launch { repository.setFavorite(item.clip.id, !item.clip.isFavorite) }
    }
}
