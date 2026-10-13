package com.ttggg5.holovoice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.PackWithCount
import com.ttggg5.holovoice.data.TalentSort
import com.ttggg5.holovoice.data.TalentSummary
import com.ttggg5.holovoice.data.TopicSort
import com.ttggg5.holovoice.data.orderedTalents
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(private val repository: PackRepository) : ViewModel() {
    private fun <T> state(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    /** Null until the library has loaded, so Home doesn't flash the "empty" state. */
    val topics: StateFlow<List<PackWithCount>?> = state(repository.packs.map { it }, null)

    val recentTopics: StateFlow<List<PackWithCount>> =
        state(repository.packs.map { TopicSort.RECENTLY_ADDED.sorted(it).take(12) }, emptyList())

    val recentlyPlayed: StateFlow<List<ClipWithPack>> = state(repository.recentlyPlayed(20), emptyList())

    /** Talents with a picture, most played first. */
    val talents: StateFlow<List<TalentSummary>> =
        state(repository.talentSummaries.map { TalentSort.PLAYS.sorted(it).filter { t -> t.slug != null }.take(20) }, emptyList())

    val favorites: StateFlow<List<ClipWithPack>> = state(repository.favorites.map { it.take(20) }, emptyList())

    /** One talent a day and their clips. */
    data class TalentOfTheDay(val slug: String, val name: String, val imageUrl: String?, val clips: List<ClipWithPack>)

    /** Lists built from play history: today's talent, clips never played, and the most played ones. */
    data class SmartLists(
        val talentOfTheDay: TalentOfTheDay? = null,
        val notPlayed: List<ClipWithPack> = emptyList(),
        val mostPlayed: List<ClipWithPack> = emptyList(),
    )

    val smartLists: StateFlow<SmartLists> = state(repository.allClips.map(::buildSmartLists), SmartLists())

    private fun buildSmartLists(clips: List<ClipWithPack>): SmartLists {
        val talents = clips.flatMap { it.orderedTalents }.distinctBy { it.slug }.sortedBy { it.slug }
        // Same talent all day, a different one tomorrow.
        val today = talents.getOrNull((LocalDate.now().toEpochDay() % talents.size.coerceAtLeast(1)).toInt())?.let { t ->
            TalentOfTheDay(t.slug, t.name, t.imageUrl, clips.filter { c -> c.orderedTalents.any { it.slug == t.slug } })
        }
        return SmartLists(
            talentOfTheDay = today,
            notPlayed = clips.filter { it.clip.playCount == 0 }.sortedByDescending { it.clip.addedAt },
            mostPlayed = clips.filter { it.clip.playCount > 0 }
                .sortedWith(compareByDescending<ClipWithPack> { it.clip.playCount }.thenByDescending { it.clip.lastPlayedAt }),
        )
    }

    fun withAllClips(block: (List<ClipWithPack>) -> Unit) {
        viewModelScope.launch { block(repository.getAllClips()) }
    }

    fun toggleFavorite(item: ClipWithPack) {
        viewModelScope.launch { repository.setFavorite(item.clip.id, !item.clip.isFavorite) }
    }
}
