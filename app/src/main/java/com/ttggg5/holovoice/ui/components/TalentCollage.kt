package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** Left part of a slanted split: from 60% across at the top to 40% at the bottom. */
private val LeftSlant = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width * 0.6f, 0f)
    lineTo(size.width * 0.4f, size.height)
    lineTo(0f, size.height)
    close()
}

private val RightSlant = GenericShape { size, _ ->
    moveTo(size.width * 0.6f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(size.width * 0.4f, size.height)
    close()
}

/**
 * Split picture for a clip with several talents: 2 → diagonal split, 3 → one big + two small,
 * 4+ → 2×2 grid with "+N" on the last cell. Fills its parent; the caller clips the outer shape.
 */
@Composable
fun TalentCollage(images: List<String>, modifier: Modifier = Modifier) {
    val gap = 2.dp
    val divider = MaterialTheme.colorScheme.surface
    Box(modifier.fillMaxSize().background(divider)) {
        when {
            images.size <= 1 -> Picture(images.firstOrNull(), Modifier.fillMaxSize())
            images.size == 2 -> {
                // Each picture is centred in its own (wider than half) area, then cut by the slant.
                Box(Modifier.fillMaxSize().clip(LeftSlant)) {
                    Picture(images[0], Modifier.fillMaxHeight().fillMaxWidth(0.6f))
                }
                Box(Modifier.fillMaxSize().clip(RightSlant)) {
                    Picture(images[1], Modifier.fillMaxHeight().fillMaxWidth(0.6f).align(Alignment.CenterEnd))
                }
                Canvas(Modifier.fillMaxSize()) {
                    drawLine(
                        color = divider,
                        start = Offset(size.width * 0.6f, 0f),
                        end = Offset(size.width * 0.4f, size.height),
                        strokeWidth = gap.toPx() * 1.5f,
                    )
                }
            }
            images.size == 3 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Picture(images[0], Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Picture(images[1], Modifier.weight(1f).fillMaxWidth())
                    Picture(images[2], Modifier.weight(1f).fillMaxWidth())
                }
            }
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                val extra = images.size - 4
                for (row in 0..1) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                Picture(images[index], Modifier.fillMaxSize())
                                if (index == 3 && extra > 0) {
                                    BoxWithConstraints(
                                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "+$extra",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (maxHeight.value * 0.4f).coerceIn(8f, 40f).sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Picture(url: String?, modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}
