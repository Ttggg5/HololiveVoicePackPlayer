package com.ttggg5.holovoice.ui

import com.ttggg5.holovoice.ui.components.formatDuration
import com.ttggg5.holovoice.ui.playlists.formatTotal
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun durationIsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:05", formatDuration(4_600)) // rounds to the nearest second
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("62:05", formatDuration(3_725_000)) // long clips keep counting minutes
    }

    @Test
    fun totalUsesTheLargestUnits() {
        assertEquals("", formatTotal(0))
        assertEquals("42 s", formatTotal(42_000))
        assertEquals("3 min", formatTotal(3 * 60_000 + 20_000))
        assertEquals("1 h 0 min", formatTotal(3_600_000))
        assertEquals("2 h 15 min", formatTotal((2 * 60 + 15) * 60_000L))
    }
}
