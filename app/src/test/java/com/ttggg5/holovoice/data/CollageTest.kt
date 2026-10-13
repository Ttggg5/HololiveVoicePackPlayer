package com.ttggg5.holovoice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollageTest {
    @Test
    fun roundTripKeepsAwkwardUrls() {
        val images = listOf(
            "https://example.com/a b.png",
            "https://example.com/兎田ぺこら.png?x=1&y=2",
            "content://com.android.providers/document/primary%3AMusic%2Fc.jpg",
        )
        assertEquals(images, Collage.parse(Collage.uri(images)))
    }

    @Test
    fun normalUrlsAreNotCollages() {
        assertNull(Collage.parse("https://example.com/a.png"))
        assertNull(Collage.parse(null))
    }

    @Test
    fun onlyMultiTalentClipsGetACollage() {
        fun clip(vararg images: String) = ClipWithPack(
            clip = Clip(id = 1, packId = 1, title = "t", fileUri = "f", durationMs = null, trackNo = null),
            packName = "p",
            talent = "x",
            coverUri = "cover.png",
            talents = images.mapIndexed { i, url -> ClipTalent(1, "s$i", "n$i", url, i) },
        )
        assertEquals("a.png", clip("a.png").artwork)
        assertEquals(listOf("a.png", "b.png"), Collage.parse(clip("a.png", "b.png").artwork))
        assertEquals("cover.png", clip().artwork)
    }
}
