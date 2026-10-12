package com.ttggg5.holovoice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileNamesTest {
    @Test
    fun parseClip_stripsTrackNumberAndExtension() {
        assertEquals(1 to "おはよう", FileNames.parseClip("01_おはよう.mp3"))
        assertEquals(12 to "Good morning", FileNames.parseClip("12. Good morning.wav"))
        assertEquals(3 to "Hi there", FileNames.parseClip("Track 3 - Hi_there.ogg"))
        assertEquals(7 to "07", FileNames.parseClip("07.m4a"))
    }

    @Test
    fun parseClip_withoutNumber() {
        assertEquals(null to "Good night", FileNames.parseClip("Good_night.flac"))
        assertEquals(null to "2023 Birthday", FileNames.parseClip("2023 Birthday.mp3"))
        assertEquals(null to "1st anniversary", FileNames.parseClip("1st anniversary.mp3"))
    }

    @Test
    fun audioAndJunkDetection() {
        assertTrue(FileNames.isAudio("a/b/C.MP3"))
        assertFalse(FileNames.isAudio("readme.txt"))
        assertTrue(FileNames.isJunk("__MACOSX/pack/01.mp3"))
        assertTrue(FileNames.isJunk("pack/._01.mp3"))
        assertFalse(FileNames.isJunk("pack/01.mp3"))
    }

    @Test
    fun guessTalent() {
        assertEquals("Gawr Gura", FileNames.guessTalent("Gawr Gura - Birthday 2023", null))
        assertEquals("兎田ぺこら", FileNames.guessTalent("【兎田ぺこら】誕生日ボイス", null))
        assertEquals("Pekora", FileNames.guessTalent("Birthday", "Pekora"))
        assertEquals("Unknown", FileNames.guessTalent("Birthday", null))
    }

    @Test
    fun pickCover_prefersCoverNames() {
        assertEquals("jacket.png", FileNames.pickCover(listOf("a.jpg", "jacket.png")))
        assertEquals("a.jpg", FileNames.pickCover(listOf("b.jpg", "a.jpg")))
        assertNull(FileNames.pickCover(emptyList()))
    }

    @Test
    fun safeResolve_blocksZipSlip() {
        val dir = Files.createTempDirectory("zip").toFile()
        try {
            assertNotNull(FileNames.safeResolve(dir, "pack/01.mp3"))
            assertNull(FileNames.safeResolve(dir, "../evil.mp3"))
            assertNull(FileNames.safeResolve(dir, "pack/../../evil.mp3"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
