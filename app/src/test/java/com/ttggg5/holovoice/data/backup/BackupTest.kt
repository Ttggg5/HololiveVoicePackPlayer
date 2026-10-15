package com.ttggg5.holovoice.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTest {
    private val morning = ClipRef("content://tree/a/morning.mp3", "Pekora ASMR", "Morning", "Good morning", 1)
    private val night = ClipRef("content://tree/a/night.mp3", "Pekora ASMR", null, "Good night", null)

    private val sample = Backup(
        createdAt = 1_700_000_000_000,
        appVersion = "0.1.0",
        prefs = mapOf(
            "haptics" to false,
            "skipBackSec" to 15,
            "defaultSpeed" to 1.25f,
            "lastSync" to 1_700_000_000_123L,
            "themeMode" to "DARK",
            "excluded" to setOf("a", "b"),
        ),
        clips = listOf(
            ClipRecord(morning, favorite = true, playCount = 3, lastPlayedAt = 99, manualTalents = listOf(TalentRef("usada-pekora", "Usada Pekora", null))),
            ClipRecord(night, manualTalents = emptyList()),
        ),
        events = listOf(EventRecord(0, 10, 2_000), EventRecord(1, 20, 0)),
        playlists = listOf(PlaylistRecord("Sleep", 5, listOf(1, 0, 1))),
        alarms = listOf(AlarmRecord(7, 30, 0b0011111, true, "Work", "TOPIC", null, null, TopicRef("content://tree/a", "Pekora ASMR"), 5)),
        customTalents = listOf(
            CustomTalentRecord("custom-1", "Someone", "誰か", byteArrayOf(1, 2, 3)),
            CustomTalentRecord("custom-2", "No Picture", null, null),
        ),
    )

    @Test
    fun jsonRoundTripKeepsEverythingAndItsTypes() {
        val back = BackupJson.read(BackupJson.write(sample))
        assertEquals(sample, back)
        // Typed prefs: an Int stays an Int, a Float a Float.
        assertEquals(15, back.prefs["skipBackSec"])
        assertEquals(1.25f, back.prefs["defaultSpeed"])
        assertEquals(1_700_000_000_123L, back.prefs["lastSync"])
    }

    @Test
    fun otherFilesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { BackupJson.read("not json") }
        assertThrows(IllegalArgumentException::class.java) { BackupJson.read("""{"format":"something-else"}""") }
        assertThrows(IllegalArgumentException::class.java) { BackupJson.read("""{"format":"holovoice-backup","version":99}""") }
    }

    @Test
    fun clipsAreFoundByFileThenByName() {
        val matcher = ClipMatcher(
            listOf(
                LibraryClip(1, morning),
                // The same clip imported from another place: a different file address.
                LibraryClip(2, night.copy(uri = "content://other/night.mp3", title = " good NIGHT ")),
            ),
        )
        assertEquals(1L, matcher.find(morning))
        assertEquals(1L, matcher.find(morning.copy(topic = "Renamed")))
        assertEquals(2L, matcher.find(night))
        assertNull(matcher.find(night.copy(uri = "content://x", trackNo = 4)))
        assertNull(matcher.find(morning.copy(uri = "content://x", category = "Evening")))
    }

    @Test
    fun topicsAreFoundBySourceThenByName() {
        val matcher = TopicMatcher(
            listOf(
                10L to TopicRef("content://library", "Pekora ASMR"),
                11L to TopicRef("content://library", "Miko ASMR"),
                12L to TopicRef("content://single", "Suisei songs"),
            ),
        )
        assertEquals(11L, matcher.find(TopicRef("content://library", "Miko ASMR")))
        assertEquals(12L, matcher.find(TopicRef("content://single", "Renamed")))
        assertEquals(10L, matcher.find(TopicRef("content://moved", "pekora asmr")))
        assertNull(matcher.find(TopicRef("content://gone", "Gone")))
    }

    @Test
    fun summaryReadsNaturally() {
        assertEquals(
            "1 favorite, 1 playlist, 1 alarm, 2 custom talents, 2 plays of history and settings",
            BackupSummary.of(sample).describe(),
        )
        assertEquals(
            "3 playlists (1 clip not found)",
            BackupSummary(0, 0, 3, 0, 0, settings = false, missingInPlaylists = 1).describe(),
        )
        assertEquals("nothing", BackupSummary(0, 0, 0, 0, 0, settings = false).describe())
    }
}
