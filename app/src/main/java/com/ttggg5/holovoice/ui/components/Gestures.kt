package com.ttggg5.holovoice.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.UiPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Two-finger pinch changes the grid tile size (remembered in [UiPrefs]). While the fingers move, the
 * grid is only scaled around them (smooth, no re-layout); on release the new size is applied once and
 * the grid settles into its new columns. Only reacts while two fingers are down, so one-finger
 * scrolling, taps and swipes behave as usual.
 */
@Composable
fun Modifier.pinchToResize(prefs: UiPrefs): Modifier {
    val cellSize by prefs.gridCellDp.collectAsStateWithLifecycle()
    val latest by rememberUpdatedState(cellSize)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableFloatStateOf(1f) }
    var pivot by remember { mutableStateOf(TransformOrigin.Center) }
    var settle by remember { mutableStateOf<Job?>(null) }
    return this
        // The zoomed preview never draws over the bars around the grid.
        .clipToBounds()
        .pointerInput(prefs) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var start = -1
                var scale = 1f
                do {
                    // Initial pass: we see the event before the grid scrolls, and take it only for pinches.
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.count { it.pressed } >= 2) {
                        if (start < 0) {
                            settle?.cancel()
                            start = latest
                            scale = preview
                        }
                        val before = GridDensity.of((start * scale).roundToInt())
                        scale = (scale * event.calculateZoom())
                            .coerceIn(UiPrefs.MIN_GRID_DP.toFloat() / start, UiPrefs.MAX_GRID_DP.toFloat() / start)
                        preview = scale
                        val centre = event.calculateCentroid()
                        if (centre != Offset.Unspecified && size.width > 0 && size.height > 0) {
                            pivot = TransformOrigin(centre.x / size.width, centre.y / size.height)
                        }
                        // A tick when the layout will switch (mosaic / tiles / wide cards).
                        if (GridDensity.of((start * scale).roundToInt()) != before) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
                if (start > 0) {
                    // Apply the size once; the grid re-lays out at about the same tile size, so the
                    // leftover scale is close to 1 and settles away.
                    prefs.setGridCellDp((start * scale).roundToInt())
                    val committed = prefs.gridCellDp.value.coerceAtLeast(1)
                    val from = scale * start / committed
                    preview = from
                    settle = scope.launch {
                        animate(from, 1f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { v, _ ->
                            preview = v
                        }
                    }
                }
            }
        }
        .graphicsLayer {
            scaleX = preview
            scaleY = preview
            transformOrigin = pivot
        }
}
