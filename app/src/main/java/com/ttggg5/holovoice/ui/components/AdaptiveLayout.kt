package com.ttggg5.holovoice.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
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
 * True for an unfolded foldable (e.g. Galaxy Z Fold): a window at least 600dp wide that is close to
 * square. The pages then take the left half and Now Playing is docked on the right half, so the split
 * sits on the fold. Tablets (much wider than tall in landscape, much taller in portrait) and phones
 * keep their own layouts.
 */
fun isDockedLayout(widthDp: Int, heightDp: Int): Boolean {
    if (widthDp < 600 || heightDp <= 0) return false
    val ratio = widthDp.toFloat() / heightDp
    return ratio in 0.75f..1.45f
}
