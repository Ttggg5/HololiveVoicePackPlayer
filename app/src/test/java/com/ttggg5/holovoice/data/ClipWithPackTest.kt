package com.ttggg5.holovoice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipWithPackTest {
    private fun clip(
        talents: List<ClipTalent> = emptyList(),
        cover: String? = null,
        legacySlug: String? = null,
        legacyName: String? = null,
        legacyImage: String? = null,
    ) = ClipWithPack(
        clip = Clip(
            id = 1,
            packId = 1,
            title = "Ohayo",
            fileUri = "file:///1",
            durationMs = null,
            trackNo = null,
            talentSlug = legacySlug,
            talentName = legacyName,
            talentImageUrl = legacyImage,
        ),
        packName = "Morning",
        talent = "Pack talent",
        coverUri = cover,
        talents = talents,
    )

    private fun talent(slug: String, name: String, image: String?, position: Int) = ClipTalent(1, slug, name, image, position)

    @Test
    fun orderedTalentsFollowPosition() {
        val c = clip(listOf(talent("b", "B", "b.png", 1), talent("a", "A", "a.png", 0)))
        assertEquals(listOf("a", "b"), c.orderedTalents.map { it.slug })
    }

    @Test
    fun orderedTalentsFallBackToTheLegacyTalent() {
        val c = clip(legacySlug = "pekora", legacyName = "Usada Pekora", legacyImage = "p.png")
        assertEquals(listOf(ClipTalent(1, "pekora", "Usada Pekora", "p.png", 0)), c.orderedTalents)
        assertEquals(emptyList<ClipTalent>(), clip().orderedTalents)
    }

    @Test
    fun speakerJoinsCleanNamesOrUsesThePackTalent() {
        val c = clip(listOf(talent("a", "Aqua", null, 0), talent("c", "[Alum] Coco", null, 1)))
        assertEquals("Aqua & Coco", c.speaker)
        assertEquals("Pack talent", clip().speaker)
    }

    @Test
    fun artworkIsACollageOnlyForSeveralTalents() {
        val two = clip(listOf(talent("a", "A", "a.png", 0), talent("b", "B", "b.png", 1)), cover = "cover.png")
        assertEquals(listOf("a.png", "b.png"), Collage.parse(two.artwork))

        val one = clip(listOf(talent("a", "A", "a.png", 0)), cover = "cover.png")
        assertEquals("a.png", one.artwork)

        assertEquals("cover.png", clip(cover = "cover.png").artwork)
        assertNull(clip().artwork)
    }

    @Test
    fun thumbnailPrefersTheTopicCover() {
        val c = clip(listOf(talent("a", "A", "a.png", 0)), cover = "cover.png")
        assertEquals("cover.png", c.thumbnail)
        assertEquals("a.png", clip(listOf(talent("a", "A", "a.png", 0))).thumbnail)
    }

    @Test
    fun talentImagesSkipMissingPictures() {
        val c = clip(listOf(talent("a", "A", null, 0), talent("b", "B", "b.png", 1)))
        assertEquals(listOf("b.png"), c.talentImages)
    }
}
