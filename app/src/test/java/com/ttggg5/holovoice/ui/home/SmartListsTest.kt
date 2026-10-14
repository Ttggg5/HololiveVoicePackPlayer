package com.ttggg5.holovoice.ui.home

import com.ttggg5.holovoice.data.Clip
import com.ttggg5.holovoice.data.ClipTalent
import com.ttggg5.holovoice.data.ClipWithPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SmartListsTest {
    private fun clip(id: Long, talent: String? = null, plays: Int = 0, added: Long = 0, lastPlayed: Long = 0) = ClipWithPack(
        clip = Clip(
            id = id,
            packId = 1,
            title = "Clip $id",
            fileUri = "file:///$id",
            durationMs = null,
            trackNo = null,
            addedAt = added,
            lastPlayedAt = lastPlayed,
            playCount = plays,
        ),
        packName = "Topic",
        talent = "Pack talent",
        coverUri = null,
        talents = listOfNotNull(talent?.let { ClipTalent(id, it, it.uppercase(), "$it.png", 0) }),
    )

    private val day = LocalDate.of(2026, 10, 14)

    @Test
    fun talentOfTheDayStaysForTheDayAndChangesTheNext() {
        val clips = listOf(clip(1, "aqua"), clip(2, "miko"), clip(3, "pekora"), clip(4, "miko"))
        val today = buildSmartLists(clips, day).talentOfTheDay!!
        assertEquals(today.slug, buildSmartLists(clips.reversed(), day).talentOfTheDay!!.slug)
        assertNotEquals(today.slug, buildSmartLists(clips, day.plusDays(1)).talentOfTheDay!!.slug)
        // All of that talent's clips, and only theirs.
        assertEquals(clips.filter { c -> c.talents.any { it.slug == today.slug } }.map { it.clip.id }, today.clips.map { it.clip.id })
    }

    @Test
    fun noTalentOfTheDayWithoutTalents() {
        assertNull(buildSmartLists(listOf(clip(1), clip(2)), day).talentOfTheDay)
        assertNull(buildSmartLists(emptyList(), day).talentOfTheDay)
    }

    @Test
    fun notPlayedIsNewestFirst() {
        val clips = listOf(clip(1, added = 10), clip(2, added = 30, plays = 2), clip(3, added = 20))
        assertEquals(listOf(3L, 1L), buildSmartLists(clips, day).notPlayed.map { it.clip.id })
    }

    @Test
    fun mostPlayedByPlaysThenLastPlayed() {
        val clips = listOf(
            clip(1, plays = 2, lastPlayed = 100),
            clip(2, plays = 5, lastPlayed = 50),
            clip(3, plays = 2, lastPlayed = 300),
            clip(4),
        )
        assertEquals(listOf(2L, 3L, 1L), buildSmartLists(clips, day).mostPlayed.map { it.clip.id })
    }
}
