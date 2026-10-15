package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.ttggg5.holovoice.data.Collage

/** Cover image (or a talent collage) with a tinted placeholder icon shown underneath (and when there is no cover). */
@Composable
fun Artwork(
    model: Any?,
    modifier: Modifier = Modifier,
    iconSize: Dp,
    placeholder: ImageVector = Icons.Filled.MusicNote,
) {
    Box(modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(placeholder, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(iconSize))
        // An empty string means "no picture" (e.g. a talent added without one).
        val model = model.takeUnless { it == "" }
        val collage = Collage.parse(model)
        if (collage != null) {
            TalentCollage(collage, Modifier.fillMaxSize())
        } else if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
