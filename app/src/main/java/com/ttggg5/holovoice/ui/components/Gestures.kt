package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.UiPrefs
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Two-finger pinch changes the grid tile size (remembered in [UiPrefs]). The grid re-lays out as you
 * pinch, in steps (tiles always fill the width, so only the number of columns changes), and the tiles
 * glide to their new places. Small finger wobbles don't flip the layout back and forth. Only reacts
 * while two fingers are down, so one-finger scrolling, taps and swipes behave as usual.
 */
@Composable
fun Modifier.pinchToResize(prefs: UiPrefs): Modifier {
    val cellSize by prefs.gridCellDp.collectAsStateWithLifecycle()
    val latest by rememberUpdatedState(cellSize)
    val haptics = LocalHapticFeedback.current
    return this
        .clipToBounds()
        .pointerInput(prefs) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var start = -1
                var scale = 1f
                var written = 0
                do {
                    // Initial pass: we see the event before the grid scrolls, and take it only for pinches.
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.count { it.pressed } >= 2) {
                        if (start < 0) {
                            start = latest
                            written = latest
                        }
                        scale = (scale * event.calculateZoom())
                            .coerceIn(UiPrefs.MIN_GRID_DP.toFloat() / start, UiPrefs.MAX_GRID_DP.toFloat() / start)
                        val target = (start * scale).roundToInt()
                        val densityChanged = GridDensity.of(target) != GridDensity.of(written)
                        // Hysteresis: only re-lay out after a clear change (or when the layout style switches).
                        if (densityChanged || abs(target - written) >= written * STEP) {
                            prefs.setGridCellDp(target)
                            written = target
                            // A tick when the layout switches (mosaic / tiles / wide cards).
                            if (densityChanged) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
                if (start > 0) prefs.setGridCellDp((start * scale).roundToInt())
            }
        }
}

/** Smallest relative size change that re-lays out the grid during a pinch. */
private const val STEP = 0.06f
