package com.ttggg5.holovoice.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/** Material 3 motion: easings and durations shared by every transition in the app. */
object Motion {
    /** Things arriving on screen: fast start, long gentle settle. */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Things leaving the screen: slow start, quick exit. */
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Things moving within the screen. */
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val SHORT = 200
    const val MEDIUM = 350
    const val LONG = 450

    fun <T> enter(duration: Int = MEDIUM, delay: Int = 0) = tween<T>(duration, delay, EmphasizedDecelerate)
    fun <T> exit(duration: Int = SHORT) = tween<T>(duration, easing = EmphasizedAccelerate)
    fun <T> move(duration: Int = MEDIUM) = tween<T>(duration, easing = Standard)

    /** Soft springs for list items appearing, disappearing and moving. */
    fun <T> itemFade(): FiniteAnimationSpec<T> = spring(stiffness = Spring.StiffnessMediumLow)
    val itemPlacement: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset(1, 1))
}
