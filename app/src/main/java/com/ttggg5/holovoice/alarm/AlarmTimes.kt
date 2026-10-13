package com.ttggg5.holovoice.alarm

import java.time.DayOfWeek
import java.time.LocalDateTime

/** Pure time maths for alarms (unit-tested). */
object AlarmTimes {
    /** Bit for [day] in an alarm's weekday mask (Monday = bit 0). */
    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    /**
     * The next time an alarm at [hour]:[minute] rings after [now]. [days] = 0 rings once
     * (today if still ahead, else tomorrow); otherwise only on the days in the mask.
     */
    fun next(hour: Int, minute: Int, days: Int, now: LocalDateTime): LocalDateTime {
        var candidate = now.toLocalDate().atTime(hour, minute)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        if (days == 0) return candidate
        repeat(7) {
            if (days and bit(candidate.dayOfWeek) != 0) return candidate
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    /** "Mon, Wed, Fri", "Every day", "Weekdays", "Weekends" or "Once". */
    fun daysLabel(days: Int): String {
        val weekdays = (0..4).sumOf { 1 shl it }
        val weekend = (5..6).sumOf { 1 shl it }
        return when (days) {
            0 -> "Once"
            weekdays or weekend -> "Every day"
            weekdays -> "Weekdays"
            weekend -> "Weekends"
            else -> DayOfWeek.entries.filter { days and bit(it) != 0 }
                .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
        }
    }

    /** "in 7 h 12 min" for a duration in milliseconds. */
    fun untilLabel(ms: Long): String {
        val minutes = ((ms + 59_999) / 60_000).coerceAtLeast(1)
        val d = minutes / (24 * 60)
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return buildList {
            if (d > 0) add("$d d")
            if (h > 0) add("$h h")
            if (m > 0 && d == 0L) add("$m min")
        }.joinToString(" ", prefix = "in ")
    }
}
