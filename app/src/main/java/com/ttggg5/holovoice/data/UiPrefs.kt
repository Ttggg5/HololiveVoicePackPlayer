package com.ttggg5.holovoice.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Light/dark choice in Settings. */
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** Accent colour families offered in Settings (colours are defined in the theme). */
enum class Accent(val label: String) { HOLO_BLUE("Holo blue"), SAKURA_PINK("Sakura"), LAVENDER("Lavender"), MINT("Mint"), SUNSET("Sunset") }

/** Remembered UI choices (sort orders, grid/list, onboarding) and the Settings page options. */
class UiPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("ui", Context.MODE_PRIVATE)

    private inline fun <reified E : Enum<E>> read(key: String, default: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, default.name)!!) }.getOrDefault(default)

    private val _clipSort = MutableStateFlow(read("clipSort", ClipSort.TRACK))
    val clipSort: StateFlow<ClipSort> = _clipSort.asStateFlow()

    private val _topicSort = MutableStateFlow(read("topicSort", TopicSort.NAME))
    val topicSort: StateFlow<TopicSort> = _topicSort.asStateFlow()

    private val _playlistSort = MutableStateFlow(read("playlistSort", PlaylistSort.NAME))
    val playlistSort: StateFlow<PlaylistSort> = _playlistSort.asStateFlow()

    private val _talentSort = MutableStateFlow(read("talentSort", TalentSort.NAME))
    val talentSort: StateFlow<TalentSort> = _talentSort.asStateFlow()

    private val _clipView = MutableStateFlow(read("clipView", ClipView.GRID))
    val clipView: StateFlow<ClipView> = _clipView.asStateFlow()

    /** A persisted on/off setting. */
    inner class BoolPref(private val key: String, default: Boolean) {
        private val flow = MutableStateFlow(prefs.getBoolean(key, default))
        val state: StateFlow<Boolean> = flow.asStateFlow()
        val value: Boolean get() = flow.value
        fun set(v: Boolean) {
            flow.value = v
            prefs.edit().putBoolean(key, v).apply()
        }
    }

    /** A persisted number setting. */
    inner class IntPref(private val key: String, default: Int) {
        private val flow = MutableStateFlow(prefs.getInt(key, default))
        val state: StateFlow<Int> = flow.asStateFlow()
        val value: Int get() = flow.value
        fun set(v: Int) {
            flow.value = v
            prefs.edit().putInt(key, v).apply()
        }
    }

    inner class FloatPref(private val key: String, default: Float) {
        private val flow = MutableStateFlow(prefs.getFloat(key, default))
        val state: StateFlow<Float> = flow.asStateFlow()
        val value: Float get() = flow.value
        fun set(v: Float) {
            flow.value = v
            prefs.edit().putFloat(key, v).apply()
        }
    }

    inner class EnumPref<E : Enum<E>>(private val key: String, default: E, parse: (String) -> E) {
        private val flow = MutableStateFlow(runCatching { parse(prefs.getString(key, default.name)!!) }.getOrDefault(default))
        val state: StateFlow<E> = flow.asStateFlow()
        val value: E get() = flow.value
        fun set(v: E) {
            flow.value = v
            prefs.edit().putString(key, v.name).apply()
        }
    }

    // Appearance
    val themeMode = EnumPref("themeMode", ThemeMode.SYSTEM, ThemeMode::valueOf)
    val dynamicColor = BoolPref("dynamicColor", false)
    val accent = EnumPref("accent", Accent.HOLO_BLUE, Accent::valueOf)
    val talentColor = BoolPref("talentColor", true)

    // Playback
    val skipBackSec = IntPref("skipBackSec", 10)
    val skipForwardSec = IntPref("skipForwardSec", 10)
    val defaultSpeed = FloatPref("defaultSpeed", 1f)
    val pauseOnUnplug = BoolPref("pauseOnUnplug", true)
    val resumeOnStart = BoolPref("resumeOnStart", true)

    // Gestures
    val clipSwipes = BoolPref("clipSwipes", true)
    val doubleTapFavorite = BoolPref("doubleTapFavorite", true)
    val tabSwipes = BoolPref("tabSwipes", true)
    val playerSwipes = BoolPref("playerSwipes", true)
    val haptics = BoolPref("haptics", true)

    // Library
    val autoSyncOnStart = BoolPref("autoSyncOnStart", true)
    val autoMatchTalents = BoolPref("autoMatchTalents", true)

    private val _gridCellDp = MutableStateFlow(prefs.getInt("gridCellDp", DEFAULT_GRID_DP))

    /** Minimum width of a grid tile; pinch on a grid to change it. */
    val gridCellDp: StateFlow<Int> = _gridCellDp.asStateFlow()

    fun setGridCellDp(dp: Int) {
        val clamped = dp.coerceIn(MIN_GRID_DP, MAX_GRID_DP)
        if (clamped == _gridCellDp.value) return
        _gridCellDp.value = clamped
        prefs.edit().putInt("gridCellDp", clamped).apply()
    }

    private val _recentSearches = MutableStateFlow(
        prefs.getString("recentSearches", "").orEmpty().split('\n').filter { it.isNotBlank() },
    )

    /** Latest searches first (at most [MAX_RECENT_SEARCHES]). */
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    fun addRecentSearch(query: String) = setRecentSearches(RecentSearches.add(_recentSearches.value, query))

    fun removeRecentSearch(query: String) = setRecentSearches(RecentSearches.remove(_recentSearches.value, query))

    fun clearRecentSearches() = setRecentSearches(emptyList())

    private fun setRecentSearches(list: List<String>) {
        if (list == _recentSearches.value) return
        _recentSearches.value = list
        prefs.edit().putString("recentSearches", list.joinToString("\n")).apply()
    }

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

    companion object {
        const val DEFAULT_GRID_DP = 160
        const val MIN_GRID_DP = 110
        const val MAX_GRID_DP = 260
        const val MAX_RECENT_SEARCHES = 8
    }
}

/** The recent-searches list: latest first, no duplicates (ignoring case), at most [UiPrefs.MAX_RECENT_SEARCHES]. */
object RecentSearches {
    fun add(list: List<String>, query: String, max: Int = UiPrefs.MAX_RECENT_SEARCHES): List<String> {
        // Stored newline-separated, so a query can't contain one.
        val q = query.trim().replace('\n', ' ')
        if (q.isEmpty()) return list
        return (listOf(q) + list.filterNot { it.equals(q, ignoreCase = true) }).take(max)
    }

    fun remove(list: List<String>, query: String): List<String> = list - query
}
