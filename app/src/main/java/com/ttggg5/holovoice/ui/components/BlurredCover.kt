package com.ttggg5.holovoice.ui.components

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ttggg5.holovoice.ui.theme.Motion

/**
 * A topic thumbnail as a background: lightly blurred, then washed with [wash] so text on top stays
 * readable in light and dark themes. Cross-fades when [model] changes (e.g. the next clip).
 */
@Composable
fun BlurredCover(model: Any?, wash: Brush, modifier: Modifier = Modifier, blur: Dp = 3.dp) {
    Box(modifier) {
        Crossfade(targetState = model, animationSpec = tween(Motion.LONG), label = "blurredCover") { picture ->
            if (picture != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(picture).size(480).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        // Real blur on Android 12+; older versions show the picture unblurred under the wash.
                        .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(blur) else Modifier),
                )
            }
        }
        Box(Modifier.fillMaxSize().background(wash))
    }
}
