package com.ttggg5.holovoice.data.talents

import org.junit.Assert.assertEquals
import org.junit.Test

class TalentPageParserTest {
    private val jaPage = """
        <nav><a href="https://hololive.hololivepro.com/talents/?gp=hololive">hololive</a></nav>
        <ul class="talent_list">
          <li><a href="https://hololive.hololivepro.com/talents/usada-pekora/">
            <figure><img src="https://hololive.hololivepro.com/wp-content/uploads/pekora.png" alt="兎田ぺこら"></figure>
            <h3>兎田ぺこら<span>Usada Pekora</span></h3></a></li>
          <li><a href="/talents/houshou-marine/">
            <img class="lazy" src="data:image/gif;base64,AAAA" data-src="//cdn.example.com/marine.png">
            <h3>宝鐘マリン</h3></a></li>
          <li><a href="https://hololive.hololivepro.com/talents/no-image/"><h3>画像なし</h3></a></li>
        </ul>
    """.trimIndent()

    private val enPage = """
        <li><a href="https://hololive.hololivepro.com/en/talents/houshou-marine/">
          <img srcset="https://example.com/marine-en.png 1x, https://example.com/marine-en@2x.png 2x">
          <h3>Houshou Marine</h3></a></li>
    """.trimIndent()

    @Test
    fun parsesNamesAndImages() {
        val talents = TalentPageParser.parse(jaPage)
        assertEquals(listOf("usada-pekora", "houshou-marine"), talents.map { it.slug })

        val pekora = talents[0]
        assertEquals("Usada Pekora", pekora.nameEn)
        assertEquals("兎田ぺこら", pekora.nameJa)
        assertEquals("https://hololive.hololivepro.com/wp-content/uploads/pekora.png", pekora.imageUrl)

        val marine = talents[1]
        assertEquals("Houshou Marine", marine.nameEn) // from the slug
        assertEquals("宝鐘マリン", marine.nameJa)
        assertEquals("https://cdn.example.com/marine.png", marine.imageUrl) // lazy-load placeholder skipped
    }

    @Test
    fun parsesSrcsetAndMergesPages() {
        val en = TalentPageParser.parse(enPage).single()
        assertEquals("https://example.com/marine-en.png", en.imageUrl)
        assertEquals("Houshou Marine", en.nameEn)

        val ja = TalentPageParser.parse(jaPage).first { it.slug == "houshou-marine" }
        val merged = TalentPageParser.merge(ja, en)
        assertEquals("宝鐘マリン", merged.nameJa)
        assertEquals("Houshou Marine", merged.nameEn)
        assertEquals(ja.imageUrl, merged.imageUrl)
    }
}
