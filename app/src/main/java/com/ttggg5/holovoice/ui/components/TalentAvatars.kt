package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Overlapping round talent pictures (up to [max], then "+N"). */
@Composable
fun TalentAvatars(images: List<String>, modifier: Modifier = Modifier, size: Dp = 28.dp, max: Int = 3) {
    if (images.isEmpty()) return
    val shown = images.take(max)
    val extra = images.size - shown.size
    val step = size * 0.62f
    val count = shown.size + if (extra > 0) 1 else 0
    Box(modifier.size(width = step * (count - 1) + size, height = size)) {
        shown.forEachIndexed { i, url ->
            Artwork(
                url,
                Modifier
                    .offset(x = step * i)
                    .size(size)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                iconSize = size / 2,
            )
        }
        if (extra > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.offset(x = step * shown.size).size(size).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+$extra", color = MaterialTheme.colorScheme.onPrimary, fontSize = (size.value * 0.38f).sp)
                }
            }
        }
    }
}
