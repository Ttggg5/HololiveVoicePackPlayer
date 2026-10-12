package com.ttggg5.holovoice.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.PackWithCount
import com.ttggg5.holovoice.data.TalentSort
import com.ttggg5.holovoice.data.TalentSummary
import com.ttggg5.holovoice.data.speaker
import com.ttggg5.holovoice.data.talents.TalentMatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Search text plus filter chips. [talentSlug] "" means "clips without a talent". */
data class SearchFilters(
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val talentSlug: String? = null,
    val packId: Long? = null,
) {
    val isEmpty: Boolean get() = query.isBlank() && !favoritesOnly && talentSlug == null && packId == null
}

class SearchViewModel(private val repository: PackRepository) : ViewModel() {
    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    val talents: StateFlow<List<TalentSummary>> = repository.talentSummaries
        .map { TalentSort.NAME.sorted(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topics: StateFlow<List<PackWithCount>> =
        repository.packs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(FlowPreview::class)
    val results: StateFlow<List<ClipWithPack>> = combine(repository.allClips, _filters.debounce(150)) { clips, f ->
        if (f.isEmpty) emptyList() else clips.filter { it.passes(f) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun update(transform: (SearchFilters) -> SearchFilters) {
        _filters.value = transform(_filters.value)
    }

    fun toggleFavorite(item: ClipWithPack) {
        viewModelScope.launch { repository.setFavorite(item.clip.id, !item.clip.isFavorite) }
    }

    private fun ClipWithPack.passes(f: SearchFilters): Boolean {
        if (f.favoritesOnly && !clip.isFavorite) return false
        if (f.packId != null && clip.packId != f.packId) return false
        if (f.talentSlug != null && (clip.talentSlug ?: "") != f.talentSlug) return false
        if (f.query.isBlank()) return true
        val q = f.query.trim().lowercase()
        val nq = TalentMatcher.normalize(f.query)
        return listOfNotNull(clip.title, speaker, packName, clip.category).any { field ->
            field.lowercase().contains(q) || (nq.isNotEmpty() && TalentMatcher.normalize(field).contains(nq))
        }
    }
}
