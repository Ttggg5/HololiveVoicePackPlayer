package com.ttggg5.holovoice.data.stats

import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PlayEvent
import com.ttggg5.holovoice.data.orderedTalents
import com.ttggg5.holovoice.data.talents.TalentTags
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The time span the stats page shows. */
enum class StatsPeriod(val label: String) {
    WEEK("7 days"),
    MONTH("30 days"),
    YEAR("This year"),
    ALL("All time"),
}

/** One bar of the "plays over time" chart: a day, or a month for the longer periods. */
data class StatsBucket(val label: String, val start: LocalDate, val plays: Int, val listenedMs: Long)

data class TalentStat(val slug: String, val name: String, val imageUrl: String?, val plays: Int, val listenedMs: Long)

data class ClipStat(val clip: ClipWithPack, val plays: Int, val listenedMs: Long)

data class TopicStat(val packId: Long, val name: String, val coverUri: String?, val plays: Int, val listenedMs: Long)

data class ListeningStats(
    val listenedMs: Long,
    val plays: Int,
    val clips: Int,
    val talents: Int,
    /** Days in a row (up to today, or up to yesterday if nothing yet today) with at least one play. */
    val streakDays: Int,
    val buckets: List<StatsBucket>,
    /** Plays per hour of the day (0–23). */
    val byHour: List<Int>,
    val topTalents: List<TalentStat>,
    val topClips: List<ClipStat>,
    val topTopics: List<TopicStat>,
    /** When the detailed history starts (the first recorded play), or null without any. */
    val historySince: Long?,
)

/** Turns the play history into the numbers on the stats page. Pure, so it can be unit-tested. */
object StatsCalculator {
    private const val TOP = 10

    fun compute(
        events: List<PlayEvent>,
        clips: List<ClipWithPack>,
        period: StatsPeriod,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ListeningStats {
        val byId = clips.associateBy { it.clip.id }
        val start: LocalDate? = when (period) {
            StatsPeriod.WEEK -> today.minusDays(6)
            StatsPeriod.MONTH -> today.minusDays(29)
            StatsPeriod.YEAR -> today.withDayOfYear(1)
            StatsPeriod.ALL -> null
        }
        fun dayOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        val inPeriod = events.filter { e -> start == null || !dayOf(e.playedAt).isBefore(start) }
            .filter { byId.containsKey(it.clipId) }

        // Play counts: the history, except "All time" also includes plays from before it existed.
        val historyPlays = inPeriod.groupingBy { it.clipId }.eachCount()
        val listened = inPeriod.groupBy { it.clipId }.mapValues { (_, list) -> list.sumOf { it.listenedMs } }
        val plays: Map<Long, Int> = if (period == StatsPeriod.ALL) {
            clips.filter { it.clip.playCount > 0 || historyPlays.containsKey(it.clip.id) }
                .associate { it.clip.id to maxOf(it.clip.playCount, historyPlays[it.clip.id] ?: 0) }
        } else {
            historyPlays
        }

        val topClips = plays.entries
            .map { (id, n) -> ClipStat(byId.getValue(id), n, listened[id] ?: 0) }
            .sortedWith(compareByDescending<ClipStat> { it.plays }.thenByDescending { it.listenedMs })

        val talentAcc = LinkedHashMap<String, TalentStat>()
        val topicAcc = LinkedHashMap<Long, TopicStat>()
        for (c in topClips) {
            for (t in c.clip.orderedTalents) {
                val old = talentAcc[t.slug]
                talentAcc[t.slug] = TalentStat(
                    t.slug,
                    TalentTags.clean(t.name),
                    old?.imageUrl ?: t.imageUrl,
                    (old?.plays ?: 0) + c.plays,
                    (old?.listenedMs ?: 0) + c.listenedMs,
                )
            }
            val p = c.clip
            val old = topicAcc[p.clip.packId]
            topicAcc[p.clip.packId] = TopicStat(p.clip.packId, p.packName, p.coverUri, (old?.plays ?: 0) + c.plays, (old?.listenedMs ?: 0) + c.listenedMs)
        }

        val byHour = MutableList(24) { 0 }
        inPeriod.forEach { byHour[Instant.ofEpochMilli(it.playedAt).atZone(zone).hour]++ }

        return ListeningStats(
            listenedMs = inPeriod.sumOf { it.listenedMs },
            plays = plays.values.sum(),
            clips = plays.size,
            talents = talentAcc.size,
            streakDays = streak(events.map { dayOf(it.playedAt) }.toSet(), today),
            buckets = buckets(inPeriod, period, today, start, ::dayOf),
            byHour = byHour,
            topTalents = talentAcc.values.sortedWith(compareByDescending<TalentStat> { it.plays }.thenByDescending { it.listenedMs }).take(TOP),
            topClips = topClips.take(TOP),
            topTopics = topicAcc.values.sortedWith(compareByDescending<TopicStat> { it.plays }.thenByDescending { it.listenedMs }).take(TOP),
            historySince = events.minOfOrNull { it.playedAt },
        )
    }

    /** Consecutive days with plays, ending today (or yesterday, so a streak isn't lost before today's first play). */
    fun streak(days: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in days) today else today.minusDays(1)
        var n = 0
        while (day in days) {
            n++
            day = day.minusDays(1)
        }
        return n
    }

    private fun buckets(
        events: List<PlayEvent>,
        period: StatsPeriod,
        today: LocalDate,
        start: LocalDate?,
        dayOf: (Long) -> LocalDate,
    ): List<StatsBucket> = when (period) {
        StatsPeriod.WEEK, StatsPeriod.MONTH -> {
            val byDay = events.groupBy { dayOf(it.playedAt) }
            generateSequence(start!!) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.map { d ->
                val list = byDay[d].orEmpty()
                StatsBucket(if (period == StatsPeriod.WEEK) d.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() } else d.dayOfMonth.toString(), d, list.size, list.sumOf { it.listenedMs })
            }.toList()
        }
        StatsPeriod.YEAR, StatsPeriod.ALL -> {
            val byMonth = events.groupBy { YearMonth.from(dayOf(it.playedAt)) }
            val first = when {
                start != null -> YearMonth.from(start)
                byMonth.isEmpty() -> YearMonth.from(today)
                // All time: from the first month with history, at most the last two years.
                else -> maxOf(byMonth.keys.min(), YearMonth.from(today).minusMonths(23))
            }
            generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(YearMonth.from(today)) }.map { m ->
                val list = byMonth[m].orEmpty()
                StatsBucket(m.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }, m.atDay(1), list.size, list.sumOf { it.listenedMs })
            }.toList()
        }
    }
}
