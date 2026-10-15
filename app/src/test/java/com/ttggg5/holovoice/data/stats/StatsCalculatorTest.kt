package com.ttggg5.holovoice.data.stats

import com.ttggg5.holovoice.data.Clip
import com.ttggg5.holovoice.data.ClipTalent
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PlayEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class StatsCalculatorTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 16)

    private fun clip(id: Long, pack: Long = 1, talent: String? = null, playCount: Int = 0) = ClipWithPack(
        clip = Clip(id = id, packId = pack, title = "Clip $id", fileUri = "file:///$id", durationMs = null, trackNo = null, playCount = playCount),
        packName = "Topic $pack",
        talent = "",
        coverUri = null,
        talents = listOfNotNull(talent?.let { ClipTalent(id, it, it.uppercase(), null, 0) }),
    )

    private fun at(day: LocalDate, hour: Int = 12) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()

    private fun event(clipId: Long, day: LocalDate, ms: Long = 1_000, hour: Int = 12) =
        PlayEvent(clipId = clipId, playedAt = at(day, hour), listenedMs = ms)

    @Test
    fun weekTotalsAndTops() {
        val clips = listOf(clip(1, talent = "pekora"), clip(2, talent = "miko"), clip(3, pack = 2, talent = "pekora"))
        val events = listOf(
            event(1, today, 5_000, hour = 7),
            event(1, today.minusDays(1), 4_000, hour = 7),
            event(3, today.minusDays(2), 3_000, hour = 22),
            event(2, today.minusDays(10), 9_000), // outside the week
        )
        val s = StatsCalculator.compute(events, clips, StatsPeriod.WEEK, today, zone)
        assertEquals(12_000, s.listenedMs)
        assertEquals(3, s.plays)
        assertEquals(2, s.clips)
        assertEquals(1, s.talents)
        assertEquals(listOf("pekora"), s.topTalents.map { it.slug })
        assertEquals(3, s.topTalents.first().plays)
        assertEquals(listOf(1L, 3L), s.topClips.map { it.clip.clip.id })
        assertEquals(listOf(1L, 2L), s.topTopics.map { it.packId })
        assertEquals(7, s.buckets.size)
        assertEquals(1, s.buckets.last().plays)
        assertEquals(2, s.byHour[7])
        assertEquals(1, s.byHour[22])
    }

    @Test
    fun allTimeIncludesPlaysFromBeforeTheHistory() {
        val clips = listOf(clip(1, playCount = 10), clip(2, playCount = 0))
        val events = listOf(event(2, today))
        val s = StatsCalculator.compute(events, clips, StatsPeriod.ALL, today, zone)
        assertEquals(11, s.plays)
        assertEquals(listOf(1L, 2L), s.topClips.map { it.clip.clip.id })
        assertEquals(at(today), s.historySince)
    }

    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        val days = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(5))
        assertEquals(3, StatsCalculator.streak(days, today))
        assertEquals(2, StatsCalculator.streak(setOf(today.minusDays(1), today.minusDays(2)), today))
        assertEquals(0, StatsCalculator.streak(setOf(today.minusDays(3)), today))
    }

    @Test
    fun yearIsBucketedByMonth() {
        val s = StatsCalculator.compute(listOf(event(1, LocalDate.of(2026, 3, 5))), listOf(clip(1)), StatsPeriod.YEAR, today, zone)
        assertEquals(10, s.buckets.size)
        assertEquals("Mar", s.buckets[2].label)
        assertEquals(1, s.buckets[2].plays)
    }
}
