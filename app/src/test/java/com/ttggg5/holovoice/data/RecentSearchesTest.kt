package com.ttggg5.holovoice.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSearchesTest {
    @Test
    fun newestFirst() {
        assertEquals(listOf("miko", "pekora"), RecentSearches.add(listOf("pekora"), "miko"))
    }

    @Test
    fun repeatsMoveToTheTopIgnoringCase() {
        assertEquals(listOf("Pekora", "miko"), RecentSearches.add(listOf("miko", "pekora"), "Pekora"))
    }

    @Test
    fun blankQueriesAreIgnoredAndTextIsTrimmed() {
        val list = listOf("miko")
        assertEquals(list, RecentSearches.add(list, "   "))
        assertEquals(listOf("suisei", "miko"), RecentSearches.add(list, "  suisei "))
    }

    @Test
    fun newlinesBecomeSpaces() {
        assertEquals(listOf("good morning"), RecentSearches.add(emptyList(), "good\nmorning"))
    }

    @Test
    fun keepsAtMostTheLimit() {
        val full = (1..UiPrefs.MAX_RECENT_SEARCHES).map { "q$it" }
        val added = RecentSearches.add(full, "new")
        assertEquals(UiPrefs.MAX_RECENT_SEARCHES, added.size)
        assertEquals("new", added.first())
        assertEquals("q${UiPrefs.MAX_RECENT_SEARCHES - 1}", added.last())
    }

    @Test
    fun remove() {
        assertEquals(listOf("a", "c"), RecentSearches.remove(listOf("a", "b", "c"), "b"))
        assertEquals(listOf("a"), RecentSearches.remove(listOf("a"), "missing"))
    }
}
