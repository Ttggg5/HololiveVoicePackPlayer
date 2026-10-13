package com.ttggg5.holovoice.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.alarm.AlarmRepository
import com.ttggg5.holovoice.data.Alarm
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.PackWithCount
import com.ttggg5.holovoice.data.TalentSummary
import com.ttggg5.holovoice.playback.PlayerController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmsViewModel(
    private val alarms: AlarmRepository,
    repository: PackRepository,
    private val player: PlayerController,
) : ViewModel() {
    private fun <T> state(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val list: StateFlow<List<Alarm>?> = state(alarms.alarms.map { it }, null)
    val clips: StateFlow<List<ClipWithPack>> = state(repository.allClips, emptyList())
    val talents: StateFlow<List<TalentSummary>> = state(repository.talentSummaries.map { l -> l.filter { it.slug != null } }, emptyList())
    val topics: StateFlow<List<PackWithCount>> = state(repository.packs, emptyList())
    val pendingClip: StateFlow<Long?> = alarms.pendingClip

    fun clearPendingClip() {
        alarms.pendingClip.value = null
    }

    /** When the next alarm rings (epoch ms), or null when none is on. */
    fun next(list: List<Alarm>): Pair<Alarm, Long>? = alarms.nextOf(list)

    fun save(alarm: Alarm) {
        viewModelScope.launch { alarms.save(alarm) }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch { alarms.setEnabled(alarm, enabled) }
    }

    fun delete(alarm: Alarm, onDeleted: (undo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            alarms.delete(alarm)
            onDeleted { viewModelScope.launch { alarms.save(alarm) } }
        }
    }

    /** Plays what the alarm would ring with, in the normal player. */
    fun preview(alarm: Alarm) {
        viewModelScope.launch { alarms.pickClip(alarm)?.let { player.playQueue(listOf(it)) } }
    }
}
