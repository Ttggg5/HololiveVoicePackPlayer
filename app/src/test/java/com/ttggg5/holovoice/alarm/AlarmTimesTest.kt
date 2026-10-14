package com.ttggg5.holovoice.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class AlarmTimesTest {
    // 2026-10-13 is a Tuesday.
    private val now = LocalDateTime.of(2026, 10, 13, 8, 0)

    @Test
    fun onceLaterToday() {
        assertEquals(LocalDateTime.of(2026, 10, 13, 9, 30), AlarmTimes.next(9, 30, 0, now))
    }

    @Test
    fun onceAlreadyPassedRingsTomorrow() {
        assertEquals(LocalDateTime.of(2026, 10, 14, 7, 0), AlarmTimes.next(7, 0, 0, now))
        assertEquals(LocalDateTime.of(2026, 10, 14, 8, 0), AlarmTimes.next(8, 0, 0, now))
    }

    @Test
    fun weekdayMaskSkipsToNextMatchingDay() {
        val friday = AlarmTimes.bit(DayOfWeek.FRIDAY)
        assertEquals(LocalDateTime.of(2026, 10, 16, 7, 0), AlarmTimes.next(7, 0, friday, now))
        val tuesday = AlarmTimes.bit(DayOfWeek.TUESDAY)
        // Today's time has passed, so next week's Tuesday.
        assertEquals(LocalDateTime.of(2026, 10, 20, 7, 0), AlarmTimes.next(7, 0, tuesday, now))
        assertEquals(LocalDateTime.of(2026, 10, 13, 9, 0), AlarmTimes.next(9, 0, tuesday, now))
    }

    @Test
    fun rollsOverMonthAndYear() {
        val newYearsEve = LocalDateTime.of(2026, 12, 31, 23, 30)
        assertEquals(LocalDateTime.of(2027, 1, 1, 6, 0), AlarmTimes.next(6, 0, 0, newYearsEve))
        val monday = AlarmTimes.bit(DayOfWeek.MONDAY)
        assertEquals(LocalDateTime.of(2027, 1, 4, 6, 0), AlarmTimes.next(6, 0, monday, newYearsEve))
    }

    @Test
    fun labels() {
        assertEquals("Once", AlarmTimes.daysLabel(0))
        assertEquals("Every day", AlarmTimes.daysLabel(0b1111111))
        assertEquals("Weekdays", AlarmTimes.daysLabel(0b0011111))
        assertEquals("Weekends", AlarmTimes.daysLabel(0b1100000))
        assertEquals("Mon, Wed", AlarmTimes.daysLabel(0b0000101))
        assertEquals("in 7 h 12 min", AlarmTimes.untilLabel((7 * 60 + 12) * 60_000L))
    }

    @Test
    fun untilLabelRoundsUpAndDropsMinutesOnceDaysShow() {
        assertEquals("in 1 min", AlarmTimes.untilLabel(30_000))
        assertEquals("in 2 h", AlarmTimes.untilLabel(2 * 3_600_000L))
        assertEquals("in 1 d 3 h", AlarmTimes.untilLabel(((24 + 3) * 60 + 5) * 60_000L))
    }
}
