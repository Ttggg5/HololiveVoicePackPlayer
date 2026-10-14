package com.ttggg5.holovoice.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.ui.components.Artwork

/**
 * A playlist's picture: a mosaic of up to four of its topics' thumbnails (one fills it, two sit side
 * by side, three or four make a grid). Without covers it shows [icon] on a gradient.
 */
@Composable
fun PlaylistArt(covers: List<String>, modifier: Modifier = Modifier, icon: ImageVector = Icons.AutoMirrored.Filled.QueueMusic, iconSize: Dp = 40.dp) {
    val gap = 2.dp
    Box(modifier) {
        when (covers.size) {
            0 -> Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer)),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(iconSize))
            }
            1 -> Artwork(covers[0], Modifier.fillMaxSize(), iconSize = iconSize)
            2 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                covers.forEach { Artwork(it, Modifier.weight(1f).fillMaxSize(), iconSize = iconSize / 2) }
            }
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Artwork(covers[0], Modifier.weight(1f).fillMaxSize(), iconSize = iconSize / 2)
                    Artwork(covers[1], Modifier.weight(1f).fillMaxSize(), iconSize = iconSize / 2)
                }
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Artwork(covers[2], Modifier.weight(1f).fillMaxSize(), iconSize = iconSize / 2)
                    if (covers.size > 3) Artwork(covers[3], Modifier.weight(1f).fillMaxSize(), iconSize = iconSize / 2)
                }
            }
        }
    }
}

/** "34 min", "1 h 5 min" or "45 s". */
fun formatTotal(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return when {
        h > 0 -> "$h h $m min"
        m > 0 -> "$m min"
        totalSec > 0 -> "$totalSec s"
        else -> ""
    }
}
