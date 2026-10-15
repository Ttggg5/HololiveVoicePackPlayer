package com.ttggg5.holovoice.data.talents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomTalentsTest {
    @Test
    fun slugsAreUniqueAndMarked() {
        assertEquals("custom-100", CustomTalents.slug(100, emptySet()))
        assertEquals("custom-102", CustomTalents.slug(100, setOf("custom-100", "custom-101")))
        assertTrue(CustomTalents.isCustom("custom-100"))
        assertFalse(CustomTalents.isCustom("usada-pekora"))
        assertFalse(CustomTalents.isCustom(null))
    }

    @Test
    fun namesAreTidied() {
        assertEquals("Shirakami Fubuki", CustomTalents.clean("  Shirakami   Fubuki "))
        assertNull(CustomTalents.cleanOrNull("   "))
        assertNull(CustomTalents.cleanOrNull(null))
        assertEquals("白上フブキ", CustomTalents.cleanOrNull(" 白上フブキ "))
        assertFalse(CustomTalents.isValidName("  "))
        assertTrue(CustomTalents.isValidName("A"))
    }
}
