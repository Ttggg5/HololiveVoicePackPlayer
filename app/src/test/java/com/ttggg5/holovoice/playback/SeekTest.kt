package com.ttggg5.holovoice.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SeekTest {
    @Test
    fun staysInsideTheClip() {
        assertEquals(15_000L, clampSeek(5_000, 10_000, 60_000))
        assertEquals(0L, clampSeek(3_000, -10_000, 60_000))
        assertEquals(60_000L, clampSeek(55_000, 10_000, 60_000))
        assertEquals(20_000L, clampSeek(10_000, 10_000, Long.MAX_VALUE)) // unknown duration
    }
}
