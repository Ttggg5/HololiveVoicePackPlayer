package com.ttggg5.holovoice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SortingTest {
    private fun clip(
        id: Long,
        title: String,
        pack: String = "Topic",
        talent: String? = null,
        track: Int? = null,
        category: String? = null,
        duration: Long? = null,
        added: Long = 0,
        plays: Int = 0,
    ) = ClipWithPack(
        clip = Clip(
            id = id,
            packId = 1,
            title = title,
            fileUri = "file:///$id",
            durationMs = duration,
            trackNo = track,
            talentSlug = talent?.lowercase(),
            talentName = talent,
            category = category,
            addedAt = added,
            playCount = plays,
        ),
        packName = pack,
        talent = "Pack talent",
        coverUri = null,
    )

    @Test
    fun naturalOrder() {
        val names = listOf("Clip 10", "clip 2", "Clip 1", "Clip 02b")
        assertEquals(listOf("Clip 1", "clip 2", "Clip 02b", "Clip 10"), names.sortedWith(NaturalOrder))
        assertTrue(NaturalOrder.compare("a", "A") == 0)
    }

    @Test
    fun clipSorts() {
        val a = clip(1, "Night", talent = "Pekora", track = 2, duration = 3000, added = 10, plays = 1)
        val b = clip(2, "Morning", talent = "Marine", track = 1, duration = 1000, added = 30, plays = 5)
        val c = clip(3, "Extra", talent = null, category = "Bonus", duration = null, added = 20)
        val all = listOf(a, b, c)
        assertEquals(listOf(b, a, c), ClipSort.TRACK.sorted(all)) // root (null category) first, then by track
        assertEquals(listOf(c, b, a), ClipSort.NAME.sorted(all))
        assertEquals(listOf(b, c, a), ClipSort.TALENT.sorted(all)) // "Marine" < "Pack talent" < "Pekora"
        assertEquals(listOf(b, a, c), ClipSort.DURATION.sorted(all)) // unknown duration last
        assertEquals(listOf(b, c, a), ClipSort.RECENTLY_ADDED.sorted(all))
        assertEquals(listOf(b, a, c), ClipSort.MOST_PLAYED.sorted(all))
    }

    @Test
    fun talentSortKeepsUnassignedLast() {
        val none = TalentSummary(null, null, null, 9, 9, 0, 1)
        val gura = TalentSummary("gawr-gura", "Gawr Gura", null, 1, 0, 0, 1)
        val ao = TalentSummary("hiodoshi-ao", "Hiodoshi Ao", null, 3, 7, 0, 1)
        assertEquals(listOf(gura, ao, none), TalentSort.NAME.sorted(listOf(none, ao, gura)))
        assertEquals(listOf(ao, gura, none), TalentSort.CLIPS.sorted(listOf(gura, none, ao)))
        assertEquals(listOf(ao, gura, none), TalentSort.PLAYS.sorted(listOf(none, gura, ao)))
    }

    @Test
    fun speakerListsAllTalentsInOrder() {
        val base = clip(9, "Duet")
        val duet = base.copy(
            talents = listOf(
                ClipTalent(9, "houshou-marine", "Houshou Marine", "m.png", 1),
                ClipTalent(9, "usada-pekora", "Usada Pekora", "p.png", 0),
            ),
        )
        assertEquals("Usada Pekora & Houshou Marine", duet.speaker)
        assertEquals(listOf("p.png", "m.png"), duet.talentImages)
        assertEquals("Pack talent", base.speaker)
    }
}
