package com.ttggg5.holovoice.ui.talents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.TalentSummary
import com.ttggg5.holovoice.data.talents.Talent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Route argument used for the "no talent yet" group. */
const val UNASSIGNED_SLUG = "_none"

class TalentsViewModel(private val repository: PackRepository) : ViewModel() {
    val talents: StateFlow<List<TalentSummary>?> =
        repository.talentSummaries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The whole official roster (Japanese and English names, stored pictures); empty until it loads. */
    private val _roster = MutableStateFlow<List<Talent>>(emptyList())
    val roster: StateFlow<List<Talent>> = _roster.asStateFlow()

    init {
        loadRoster()
    }

    /** Loads the roster again, e.g. after the user added a talent. */
    fun loadRoster() {
        viewModelScope.launch { _roster.value = runCatching { repository.talents() }.getOrDefault(emptyList()) }
    }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun matchTalents(onDone: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val message = try {
                val (matched, total) = repository.rematchAllTalents()
                "Matched a talent for $matched of $total clips"
            } catch (e: Exception) {
                e.message ?: "Talent matching failed"
            } finally {
                _busy.value = false
            }
            onDone(message)
            loadRoster()
        }
    }
}

class TalentViewModel(routeSlug: String, private val repository: PackRepository) : ViewModel() {
    val slug: String? = routeSlug.takeIf { it != UNASSIGNED_SLUG }

    val clips: StateFlow<List<ClipWithPack>?> =
        repository.clipsByTalent(slug).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val summary: StateFlow<TalentSummary?> = repository.talentSummaries
        .map { list -> list.firstOrNull { it.slug == slug } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Full entry from the official list (for the Japanese name), if available. */
    private val _talent = MutableStateFlow<Talent?>(null)
    val talent: StateFlow<Talent?> = _talent.asStateFlow()

    /** True for a talent the user added (they can edit or delete it). */
    val isCustom: Boolean = repository.isCustomTalent(slug)

    init {
        reload()
    }

    fun reload() {
        if (slug != null) {
            viewModelScope.launch {
                _talent.value = runCatching { repository.talents() }.getOrNull()?.firstOrNull { it.slug == slug }
            }
        }
    }

    /** Deletes this custom talent and takes them off every clip; [onDone] runs afterwards. */
    fun delete(onDone: () -> Unit) {
        val s = slug ?: return
        viewModelScope.launch {
            repository.deleteTalent(s)
            onDone()
        }
    }

    fun toggleFavorite(item: ClipWithPack) {
        viewModelScope.launch { repository.setFavorite(item.clip.id, !item.clip.isFavorite) }
    }
}
