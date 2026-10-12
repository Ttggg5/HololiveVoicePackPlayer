package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.UiPrefs
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Two-finger pinch changes the grid tile size (remembered in [UiPrefs]). Only reacts while two
 * fingers are down, so one-finger scrolling, taps and swipes behave as usual.
 */
@Composable
fun Modifier.pinchToResize(prefs: UiPrefs): Modifier {
    val size by prefs.gridCellDp.collectAsStateWithLifecycle()
    val latest by rememberUpdatedState(size)
    return this.pointerInput(prefs) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var start = -1
            var scale = 1f
            do {
                // Initial pass: we see the event before the grid scrolls, and take it only for pinches.
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    if (start < 0) start = latest
                    scale *= event.calculateZoom()
                    prefs.setGridCellDp((start * scale).roundToInt())
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

/**
 * Horizontal fling to move to the previous (-1) or next (+1) tab. It only sees drags that no
 * child consumed, so swipeable rows, horizontal lists and pagers keep working.
 */
@Composable
fun Modifier.swipeBetweenTabs(enabled: Boolean, onSwipe: (direction: Int) -> Unit): Modifier {
    if (!enabled) return this
    val callback by rememberUpdatedState(onSwipe)
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    return this.pointerInput(Unit) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = { if (abs(total) > threshold) callback(if (total < 0) 1 else -1) },
            onDragCancel = { total = 0f },
        ) { change, amount ->
            total += amount
            change.consume()
        }
    }
}
