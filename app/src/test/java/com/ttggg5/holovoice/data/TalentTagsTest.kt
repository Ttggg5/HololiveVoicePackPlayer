package com.ttggg5.holovoice.data

import com.ttggg5.holovoice.data.talents.TalentTags
import org.junit.Assert.assertEquals
import org.junit.Test

class TalentTagsTest {
    @Test
    fun leadingEnglishTag() {
        assertEquals("Kiryu Coco" to listOf("Alum"), TalentTags.split("[Alum] Kiryu Coco"))
    }

    @Test
    fun leadingJapaneseTag() {
        assertEquals("桐生ココ" to listOf("Alum"), TalentTags.split("【卒業】桐生ココ"))
    }

    @Test
    fun trailingTag() {
        assertEquals("Gawr Gura" to listOf("Alum"), TalentTags.split("Gawr Gura (Alum)"))
    }

    @Test
    fun otherTagsKeepTheirText() {
        assertEquals("Someone" to listOf("Affiliate"), TalentTags.split("[Affiliate] Someone"))
    }

    @Test
    fun plainNamesUnchanged() {
        assertEquals("Usada Pekora" to emptyList<String>(), TalentTags.split("Usada Pekora"))
        assertEquals("兎田ぺこら" to emptyList<String>(), TalentTags.split("兎田ぺこら"))
    }

    @Test
    fun aNameThatIsOnlyBracketsIsKept() {
        assertEquals("[Alum]" to emptyList<String>(), TalentTags.split("[Alum]"))
    }
}
