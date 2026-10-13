package com.ttggg5.holovoice.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Picks a theme colour from a talent's picture and builds a colour scheme around it. */
object TalentColor {
    private val seeds = ConcurrentHashMap<String, Int>()

    /** The most vivid colour of the picture at [url], or null if it can't be loaded. */
    suspend fun seed(context: Context, url: String): Color? {
        seeds[url]?.let { return Color(it) }
        val request = ImageRequest.Builder(context).data(url).size(96).allowHardware(false).build()
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
        val argb = withContext(Dispatchers.Default) {
            val palette = Palette.from(result.drawable.toBitmap()).generate()
            (palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.darkVibrantSwatch ?: palette.dominantSwatch)?.rgb
        } ?: return null
        seeds[url] = argb
        return Color(argb)
    }

    /** A light or dark scheme whose primary and secondary colours come from [seed]. */
    fun scheme(seed: Color, dark: Boolean): ColorScheme {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(seed.toArgb(), hsl)
        val hue = hsl[0]
        // Greyish pictures still give a recognisable colour.
        val sat = hsl[1].coerceIn(0.45f, 0.85f)
        fun tone(l: Float, h: Float = hue, s: Float = sat) = Color(ColorUtils.HSLToColor(floatArrayOf(h, s, l)))
        val secondHue = (hue + 40f) % 360f
        return if (dark) {
            darkColorScheme(
                primary = tone(0.75f),
                onPrimary = tone(0.15f),
                primaryContainer = tone(0.30f),
                onPrimaryContainer = tone(0.90f),
                secondary = tone(0.75f, secondHue),
                onSecondary = tone(0.15f, secondHue),
            )
        } else {
            lightColorScheme(
                primary = tone(0.42f),
                onPrimary = Color.White,
                primaryContainer = tone(0.90f),
                onPrimaryContainer = tone(0.15f),
                secondary = tone(0.50f, secondHue),
                onSecondary = Color.White,
            )
        }
    }
}
