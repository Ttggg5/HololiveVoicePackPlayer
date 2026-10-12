package com.ttggg5.holovoice.data.talents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TalentMatcherTest {
    private val talents = listOf(
        Talent("usada-pekora", "Usada Pekora", "兎田ぺこら", "p.png"),
        Talent("houshou-marine", "Houshou Marine", "宝鐘マリン", "m.png"),
        Talent("hoshimachi-suisei", "Hoshimachi Suisei", "星街すいせい", "s.png"),
        Talent("tokino-sora", "Tokino Sora", "ときのそら", "t.png"),
        Talent("shirakami-fubuki", "Shirakami Fubuki", "白上フブキ", "f.png"),
        Talent("gawr-gura", "Gawr Gura", null, "g.png"),
        Talent("hiodoshi-ao", "Hiodoshi Ao", "火威青", "a.png"),
    )

    private fun slugsIn(name: String) = TalentMatcher.matchAll(name, talents).map { it.slug }

    private fun slugFor(name: String) = TalentMatcher.match(name, talents)?.slug

    @Test
    fun fullNames() {
        assertEquals("usada-pekora", slugFor("兎田ぺこら"))
        assertEquals("usada-pekora", slugFor("Usada Pekora"))
        assertEquals("houshou-marine", slugFor("宝鐘マリン"))
        assertEquals("gawr-gura", slugFor("gawr_gura"))
    }

    @Test
    fun partialNames() {
        assertEquals("usada-pekora", slugFor("Pekora"))
        assertEquals("usada-pekora", slugFor("ぺこら"))
        assertEquals("hoshimachi-suisei", slugFor("Suisei"))
        assertEquals("houshou-marine", slugFor("マリン"))
        assertEquals("tokino-sora", slugFor("そら"))
        assertEquals("usada-pekora", slugFor("Pekora Usada"))
    }

    @Test
    fun widthKanaAndSuffixVariants() {
        assertEquals("usada-pekora", slugFor("ＵＳＡＤＡ　ＰＥＫＯＲＡ"))
        assertEquals("shirakami-fubuki", slugFor("白上ふぶき"))
        assertEquals("usada-pekora", slugFor("Usada Pekora (2)"))
        assertEquals("houshou-marine", slugFor("Houshou Marine 03"))
    }

    @Test
    fun typos() {
        assertEquals("usada-pekora", slugFor("Usada Pekoa"))
        assertEquals("hoshimachi-suisei", slugFor("Hoshimati Suisei"))
    }

    @Test
    fun unknownNames() {
        assertNull(slugFor("Good morning"))
        assertNull(slugFor("x"))
    }

    @Test
    fun search() {
        assertEquals(true, TalentMatcher.matchesSearch("pek", talents[0]))
        assertEquals(true, TalentMatcher.matchesSearch("ぺこ", talents[0]))
        assertEquals(false, TalentMatcher.matchesSearch("marine", talents[0]))
    }

    @Test
    fun matchAll_findsNamesInsideFileNames() {
        assertEquals(listOf("usada-pekora"), slugsIn("兎田ぺこら_おはよう"))
        assertEquals(listOf("usada-pekora"), slugsIn("01 UsadaPekora morning"))
        assertEquals(listOf("hoshimachi-suisei"), slugsIn("すいせい誕生日"))
    }

    @Test
    fun matchAll_findsSeveralTalentsInOrder() {
        assertEquals(listOf("usada-pekora", "houshou-marine"), slugsIn("Pekora & Marine"))
        assertEquals(listOf("usada-pekora", "houshou-marine"), slugsIn("ぺこら×マリン"))
        assertEquals(listOf("hoshimachi-suisei", "houshou-marine"), slugsIn("星街すいせい with 宝鐘マリン"))
    }

    @Test
    fun matchAll_shortNamesOnlyAsWholeWords() {
        assertEquals(listOf("hiodoshi-ao"), slugsIn("Ao_test"))
        assertEquals(emptyList<String>(), slugsIn("chaos"))
        assertEquals(listOf("tokino-sora"), slugsIn("そら"))
        assertEquals(emptyList<String>(), slugsIn("そらいろ"))
    }

    @Test
    fun matchAll_noMatchAndTypoFallback() {
        assertEquals(emptyList<String>(), slugsIn("Good morning"))
        assertEquals(listOf("usada-pekora"), slugsIn("Usada Pekoa"))
        assertEquals(listOf("hoshimachi-suisei"), slugsIn("Hoshimati Suisei"))
    }

    @Test
    fun romaji() {
        assertEquals("そら", TalentMatcher.romajiToHiragana("Sora"))
        assertEquals("ぺこら", TalentMatcher.romajiToHiragana("pekora"))
        assertEquals("ふぶき", TalentMatcher.romajiToHiragana("Fubuki"))
        assertEquals("まりん", TalentMatcher.romajiToHiragana("marin"))
        assertEquals(null, TalentMatcher.romajiToHiragana("Ina'nis!"))
    }
}
