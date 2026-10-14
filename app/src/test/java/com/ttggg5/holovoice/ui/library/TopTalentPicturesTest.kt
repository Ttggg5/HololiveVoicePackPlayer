package com.ttggg5.holovoice.ui.library

import com.ttggg5.holovoice.data.Clip
import com.ttggg5.holovoice.data.ClipTalent
import com.ttggg5.holovoice.data.ClipWithPack
import org.junit.Assert.assertEquals
import org.junit.Test

class TopTalentPicturesTest {
    private var nextId = 1L

    /** A clip in topic [pack] with the given talent slugs; "x?" means talent x without a picture. */
    private fun clip(pack: Long, vararg slugs: String): ClipWithPack {
        val id = nextId++
        return ClipWithPack(
            clip = Clip(id = id, packId = pack, title = "Clip $id", fileUri = "file:///$id", durationMs = null, trackNo = null),
            packName = "Topic $pack",
            talent = "",
            coverUri = null,
            talents = slugs.mapIndexed { i, s ->
                val slug = s.removeSuffix("?")
                ClipTalent(id, slug, slug, if (s.endsWith("?")) null else "$slug.png", i)
            },
        )
    }

    @Test
    fun mostCommonTalentsFirstPerTopic() {
        val clips = listOf(clip(1, "miko"), clip(1, "pekora"), clip(1, "pekora", "miko"), clip(1, "pekora"), clip(2, "aqua"))
        assertEquals(
            mapOf(1L to listOf("pekora.png", "miko.png"), 2L to listOf("aqua.png")),
            topTalentPictures(clips),
        )
    }

    @Test
    fun atMostThreeAndOnlyWithPictures() {
        val clips = listOf(clip(1, "a", "b", "c", "d"), clip(1, "a"), clip(1, "e?"), clip(1, "e?"), clip(1, "e?"))
        assertEquals(listOf("a.png", "b.png", "c.png"), topTalentPictures(clips)[1])
        assertEquals(emptyList<String>(), topTalentPictures(listOf(clip(3, "z?")))[3])
    }
}
