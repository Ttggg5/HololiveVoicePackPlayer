package com.ttggg5.holovoice.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.stats.ListeningStats
import com.ttggg5.holovoice.data.stats.StatsCalculator
import com.ttggg5.holovoice.data.stats.StatsPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class StatsViewModel(repository: PackRepository) : ViewModel() {
    private val _period = MutableStateFlow(StatsPeriod.WEEK)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    fun setPeriod(p: StatsPeriod) {
        _period.value = p
    }

    /** Null until the history and library have loaded. */
    val stats: StateFlow<ListeningStats?> = combine(repository.playEvents, repository.allClips, _period) { events, clips, period ->
        StatsCalculator.compute(events, clips, period, LocalDate.now())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
