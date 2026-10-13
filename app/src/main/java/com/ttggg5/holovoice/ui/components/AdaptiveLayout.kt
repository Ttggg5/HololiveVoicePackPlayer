package com.ttggg5.holovoice.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/** True on tablets and unfolded foldables (window at least 600dp wide). */
val LocalIsWideLayout = staticCompositionLocalOf { false }

/** Used instead of the real haptics when vibration feedback is turned off in Settings. */
object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}
