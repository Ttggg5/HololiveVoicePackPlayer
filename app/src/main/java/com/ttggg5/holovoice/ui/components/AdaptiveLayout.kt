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
