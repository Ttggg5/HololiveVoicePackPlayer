package com.ttggg5.holovoice.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun unfoldedFoldablesDockThePlayer() {
        assertTrue(isDockedLayout(700, 780)) // Z Fold inner screen, portrait
        assertTrue(isDockedLayout(880, 640)) // Z Fold inner screen, landscape
        assertTrue(isDockedLayout(673, 841)) // Pixel Fold-like, portrait
    }

    @Test
    fun tabletsAndPhonesKeepTheirLayouts() {
        assertFalse(isDockedLayout(1280, 800)) // tablet landscape
        assertFalse(isDockedLayout(800, 1280)) // tablet portrait
        assertFalse(isDockedLayout(411, 891)) // phone
        assertFalse(isDockedLayout(891, 411)) // phone landscape
        assertFalse(isDockedLayout(540, 600)) // small near-square window (split screen)
        assertFalse(isDockedLayout(700, 0))
    }
}
