package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * A frosted "liquid glass" look: a translucent, slightly accent-tinted pane that is lighter at the top,
 * a soft diagonal sheen, and a bright rim along the top edge fading out towards the bottom.
 */
@Composable
fun Modifier.liquidGlass(shape: Shape): Modifier {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val base = lerp(colors.surfaceContainerHigh, colors.primary, if (dark) 0.10f else 0.06f)
    val pane = Brush.verticalGradient(
        listOf(
            lerp(base, Color.White, if (dark) 0.06f else 0.35f).copy(alpha = if (dark) 0.80f else 0.82f),
            base.copy(alpha = if (dark) 0.90f else 0.92f),
        ),
    )
    val sheen = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (dark) 0.10f else 0.30f),
        0.35f to Color.White.copy(alpha = 0f),
        0.75f to Color.White.copy(alpha = 0f),
        1f to Color.White.copy(alpha = if (dark) 0.05f else 0.12f),
    )
    val rim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.28f else 0.85f),
            Color.White.copy(alpha = if (dark) 0.06f else 0.25f),
            (if (dark) Color.White else Color.Black).copy(alpha = 0.06f),
        ),
    )
    // An opaque base first: whatever is behind (the window, the card's own shadow) must not show through.
    return this
        .background(colors.background, shape)
        .background(pane, shape)
        .background(sheen, shape)
        .border(1.dp, rim, shape)
}
