package com.ttggg5.holovoice.ui.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Position of the Now Playing overlay, shared by everything that moves it: the mini player's
 * swipe up, the player's drag down, buttons and back. [offset] is 0 when fully open and 1 when
 * closed; drags move it with the finger and [release] settles it from the finger's speed.
 */
@Stable
class PlayerSheet(private val scope: CoroutineScope, initiallyOpen: Boolean) {
    // Bounded so a fast fling can't overshoot past fully open or closed.
    val offset = Animatable(if (initiallyOpen) 0f else 1f).apply { updateBounds(0f, 1f) }

    /** Height of the area the player covers, in px. */
    var height by mutableFloatStateOf(1f)

    /**
     * Where the mini player is, in the coordinates of the area the player covers; Now Playing grows
     * out of (and shrinks back into) this rectangle. Null when the mini player isn't shown.
     */
    var miniBounds by mutableStateOf<Rect?>(null)

    /** Where the player is headed: open or closed. */
    var isOpen by mutableStateOf(initiallyOpen)
        private set

    /** Shown at all (open, opening, closing or being dragged). */
    val isVisible: Boolean get() = offset.value < 1f

    fun open() {
        isOpen = true
        scope.launch { offset.animateTo(0f, SETTLE) }
    }

    fun close() {
        isOpen = false
        scope.launch { offset.animateTo(1f, SETTLE) }
    }

    /** Moves the player by [dyPx] (down is positive), following the finger in the same frame. */
    fun drag(dyPx: Float) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            offset.snapTo((offset.value + dyPx / height.coerceAtLeast(1f)).coerceIn(0f, 1f))
        }
    }

    /** Finishes a drag: open or close depending on how far and how fast it was moved. */
    fun release(velocityPx: Float) {
        val velocity = velocityPx / height.coerceAtLeast(1f) // screen heights per second
        val open = if (isOpen) {
            !(offset.value > 0.25f || velocity > FLING)
        } else {
            offset.value < 0.75f || velocity < -FLING
        }
        isOpen = open
        scope.launch {
            offset.animateTo(
                if (open) 0f else 1f,
                SETTLE,
                initialVelocity = velocity,
            )
        }
    }

    private companion object {
        const val FLING = 0.8f

        /** Opening, closing and settling after a drag: smooth, no bounce, keeps the finger's speed. */
        val SETTLE = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    }
}
