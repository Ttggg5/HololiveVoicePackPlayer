package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Window width (dp) from which the side rail and the side-by-side player are used: Material's
 * "expanded" size, e.g. tablets in landscape. Narrower windows (portrait tablets, split screen)
 * keep the roomier phone layout.
 */
const val WIDE_LAYOUT_MIN_DP = 840

/** True when the window is at least [WIDE_LAYOUT_MIN_DP] wide. */
val LocalIsWideLayout = staticCompositionLocalOf { false }

/** Used instead of the real haptics when vibration feedback is turned off in Settings. */
object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

/**
 * How a zoomable grid presents its items, from the pinch size: a picture-only mosaic at the small
 * end, the normal tiles, or wide one-per-row cards with all the details at the big end.
 */
enum class GridDensity {
    MOSAIC,
    NORMAL,
    WIDE;

    companion object {
        /** Tiles smaller than this (dp) become the mosaic. */
        const val MOSAIC_BELOW = 100

        /** Tiles larger than this (dp) become wide cards. */
        const val WIDE_ABOVE = 260

        fun of(cellDp: Int): GridDensity = when {
            cellDp < MOSAIC_BELOW -> MOSAIC
            cellDp > WIDE_ABOVE -> WIDE
            else -> NORMAL
        }
    }
}

/** Columns for a zoomable grid: mosaic and normal follow the pinch size, wide cards are ~340dp each. */
fun gridColumns(cellDp: Int, density: GridDensity, scale: Float = 1f): GridCells =
    if (density == GridDensity.WIDE) GridCells.Adaptive(340.dp) else GridCells.Adaptive((cellDp * scale).dp)

/** Gap between grid items: tight for the mosaic. */
fun gridSpacing(density: GridDensity): Dp = if (density == GridDensity.MOSAIC) 4.dp else 12.dp
