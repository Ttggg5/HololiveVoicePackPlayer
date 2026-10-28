package com.ttggg5.holovoice.ui.components

import com.ttggg5.holovoice.data.UiPrefs
import org.junit.Assert.assertEquals
import org.junit.Test

class GridDensityTest {
    @Test
    fun smallTilesBecomeAMosaicAndBigOnesWideCards() {
        assertEquals(GridDensity.MOSAIC, GridDensity.of(UiPrefs.MIN_GRID_DP))
        assertEquals(GridDensity.MOSAIC, GridDensity.of(99))
        assertEquals(GridDensity.NORMAL, GridDensity.of(100))
        assertEquals(GridDensity.NORMAL, GridDensity.of(UiPrefs.DEFAULT_GRID_DP))
        assertEquals(GridDensity.NORMAL, GridDensity.of(260))
        assertEquals(GridDensity.WIDE, GridDensity.of(261))
        assertEquals(GridDensity.WIDE, GridDensity.of(UiPrefs.MAX_GRID_DP))
    }
}
