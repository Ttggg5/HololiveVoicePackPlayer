package com.ttggg5.holovoice.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.PackWithCount
import com.ttggg5.holovoice.data.TalentSort
import com.ttggg5.holovoice.data.TalentSummary
import com.ttggg5.holovoice.data.UiPrefs
import com.ttggg5.holovoice.data.speaker
import com.ttggg5.holovoice.data.orderedTalents
import com.ttggg5.holovoice.data.talents.Talent
import com.ttggg5.holovoice.data.talents.TalentMatcher
import com.ttggg5.holovoice.data.talents.TalentTags
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

class SearchViewModel(private val repository: PackRepository, private val prefs: UiPrefs) : ViewModel() {
    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    val talents: StateFlow<List<TalentSummary>> = repository.talentSummaries
        .map { TalentSort.NAME.sorted(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topics: StateFlow<List<PackWithCount>> =
        repository.packs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The official roster, for Japanese names (empty until loaded). */
    private val roster = MutableStateFlow<Map<String, Talent>>(emptyMap())

    init {
        viewModelScope.launch {
            roster.value = runCatching { repository.talents() }.getOrDefault(emptyList()).associateBy { it.slug }
        }
    }

    /** A talent in the library, with both names (from the official list when known). */
    data class TalentHit(val slug: String, val nameJa: String?, val nameEn: String, val imageUrl: String?, val clipCount: Int)

    /** Talents in the library, by name. */
    val libraryTalents: StateFlow<List<TalentHit>> = combine(talents, roster) { list, official ->
        list.mapNotNull { t ->
            val slug = t.slug ?: return@mapNotNull null
            val o = official[slug]
            TalentHit(slug, o?.nameJa, o?.nameEn ?: TalentTags.clean(t.name ?: slug), o?.imageUrl ?: t.imageUrl, t.clipCount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(FlowPreview::class)
    private val query = _filters.map { it.query.trim() }.debounce(150)

    /** Talents whose English or Japanese name matches the search text. */
    val talentMatches: StateFlow<List<TalentHit>> = combine(libraryTalents, query) { list, q ->
        if (q.isEmpty()) emptyList() else list.filter { matches(q, it.nameEn, it.nameJa) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Topics whose name or talent matches the search text. */
    val topicMatches: StateFlow<List<PackWithCount>> = combine(topics, query) { list, q ->
        if (q.isEmpty()) emptyList() else list.filter { matches(q, it.pack.name, it.pack.talent) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentSearches: StateFlow<List<String>> = prefs.recentSearches

    fun saveSearch() = prefs.addRecentSearch(_filters.value.query)
    fun removeRecent(query: String) = prefs.removeRecentSearch(query)
    fun clearRecent() = prefs.clearRecentSearches()

    private fun matches(q: String, vararg fields: String?): Boolean {
        val nq = TalentMatcher.normalize(q)
        return fields.filterNotNull().any { f ->
            f.contains(q, ignoreCase = true) || (nq.isNotEmpty() && TalentMatcher.normalize(f).contains(nq))
        }
    }

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
        if (f.talentSlug != null) {
            val slugs = orderedTalents.map { it.slug }
            if (if (f.talentSlug.isEmpty()) slugs.isNotEmpty() else f.talentSlug !in slugs) return false
        }
        if (f.query.isBlank()) return true
        val q = f.query.trim().lowercase()
        val nq = TalentMatcher.normalize(f.query)
        return listOfNotNull(clip.title, speaker, packName, clip.category).any { field ->
            field.lowercase().contains(q) || (nq.isNotEmpty() && TalentMatcher.normalize(field).contains(nq))
        }
    }
}
