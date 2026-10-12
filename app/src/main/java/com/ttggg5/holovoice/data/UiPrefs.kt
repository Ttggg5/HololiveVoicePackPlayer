package com.ttggg5.holovoice.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Remembered UI choices (sort orders, grid/list, onboarding). */
class UiPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("ui", Context.MODE_PRIVATE)

    private inline fun <reified E : Enum<E>> read(key: String, default: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, default.name)!!) }.getOrDefault(default)

    private val _clipSort = MutableStateFlow(read("clipSort", ClipSort.TRACK))
    val clipSort: StateFlow<ClipSort> = _clipSort.asStateFlow()

    private val _topicSort = MutableStateFlow(read("topicSort", TopicSort.TALENT))
    val topicSort: StateFlow<TopicSort> = _topicSort.asStateFlow()

    private val _playlistSort = MutableStateFlow(read("playlistSort", PlaylistSort.NAME))
    val playlistSort: StateFlow<PlaylistSort> = _playlistSort.asStateFlow()

    private val _talentSort = MutableStateFlow(read("talentSort", TalentSort.NAME))
    val talentSort: StateFlow<TalentSort> = _talentSort.asStateFlow()

    private val _clipView = MutableStateFlow(read("clipView", ClipView.GRID))
    val clipView: StateFlow<ClipView> = _clipView.asStateFlow()

    private val _onboardingDone = MutableStateFlow(prefs.getBoolean("onboardingDone", false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    fun setClipSort(v: ClipSort) = save("clipSort", v).also { _clipSort.value = v }
    fun setTopicSort(v: TopicSort) = save("topicSort", v).also { _topicSort.value = v }
    fun setPlaylistSort(v: PlaylistSort) = save("playlistSort", v).also { _playlistSort.value = v }
    fun setTalentSort(v: TalentSort) = save("talentSort", v).also { _talentSort.value = v }
    fun setClipView(v: ClipView) = save("clipView", v).also { _clipView.value = v }

    fun setOnboardingDone(done: Boolean) {
        prefs.edit().putBoolean("onboardingDone", done).apply()
        _onboardingDone.value = done
    }

    private fun save(key: String, value: Enum<*>) {
        prefs.edit().putString(key, value.name).apply()
    }
}
