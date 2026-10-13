package com.ttggg5.holovoice.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.PathParser
import com.ttggg5.holovoice.R
import com.ttggg5.holovoice.data.Accent

/**
 * The picture shown behind the media notification / lock-screen player: the topic thumbnail (or an
 * accent-coloured gradient with a faint hololive mark), and the talent picture in a big circle on the right.
 * Key content sits in the vertical middle so Android 13+'s wide crop still shows it.
 */
object NotificationArtwork {
    private const val SIZE = 1024
    private const val HEADPHONES =
        "M12,1c-4.97,0 -9,4.03 -9,9v7c0,1.66 1.34,3 3,3h3v-8H5v-2c0,-3.87 3.13,-7 7,-7s7,3.13 7,7v2h-4v8h3c1.66,0 3,-1.34 3,-3v-7c0,-4.97 -4.03,-9 -9,-9z"

    private fun Accent.gradient(): Pair<Int, Int> = when (this) {
        Accent.HOLO_BLUE -> 0xFF38D0F5.toInt() to 0xFF1A5FD0.toInt()
        Accent.SAKURA_PINK -> 0xFFFF8FB8.toInt() to 0xFFC2185B.toInt()
        Accent.LAVENDER -> 0xFFB39DFF.toInt() to 0xFF5E35B1.toInt()
        Accent.MINT -> 0xFF6FE3C6.toInt() to 0xFF00796B.toInt()
        Accent.SUNSET -> 0xFFFFB27A.toInt() to 0xFFD84315.toInt()
    }

    @Volatile private var mark: Bitmap? = null

    private fun mark(context: Context): Bitmap? = mark ?: runCatching {
        BitmapFactory.decodeResource(context.resources, R.drawable.hololive_mark)
    }.getOrNull()?.also { mark = it }

    /**
     * Composes the card. With a topic [cover] it fills the background (dimmed so the white controls
     * stay readable); without one an accent gradient with a faint hololive mark is used. The talent
     * [picture] (or collage) sits in a ringed circle on the right; it is left out when only the cover exists.
     */
    fun composeCard(context: Context, cover: Bitmap?, picture: Bitmap?, accent: Accent): Bitmap {
        val size = SIZE.toFloat()
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val (start, end) = accent.gradient()

        if (cover != null) {
            canvas.drawCropped(cover, RectF(0f, 0f, size, size))
            // Darker on the left (where the title sits) and at the bottom (controls).
            canvas.drawRect(0f, 0f, size, size, Paint().apply {
                shader = LinearGradient(0f, 0f, size, 0f, Color.argb(70, 0, 0, 0), Color.argb(10, 0, 0, 0), Shader.TileMode.CLAMP)
            })
            canvas.drawRect(0f, size * 0.55f, size, size, Paint().apply {
                shader = LinearGradient(0f, size * 0.55f, 0f, size, Color.TRANSPARENT, Color.argb(60, 0, 0, 0), Shader.TileMode.CLAMP)
            })
            if (picture == null) return out
        } else {
            // Background gradient in the accent colour, with two soft highlights.
            canvas.drawRect(0f, 0f, size, size, Paint().apply { shader = LinearGradient(0f, 0f, size, size, start, end, Shader.TileMode.CLAMP) })
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(28, 255, 255, 255) }
            canvas.drawCircle(size * 0.85f, size * 0.12f, size * 0.42f, glow)
            canvas.drawCircle(size * 0.08f, size * 0.95f, size * 0.32f, glow)

            // Faint hololive mark on the left.
            mark(context)?.let { m ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    alpha = 42 // ~16%
                }
                canvas.drawBitmap(m, null, RectF(40f, 252f, 560f, 772f), paint)
            }
        }

        // The clip's picture in a big circle on the right, with a white ring and soft shadow.
        val cx = 760f
        val cy = size / 2
        val r = 135f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(24f, 0f, 8f, Color.argb(80, 0, 0, 0))
        }
        canvas.drawCircle(cx, cy, r + 8f, ring)
        val circle = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(circle)
        if (picture != null) {
            canvas.drawCropped(picture, RectF(cx - r, cy - r, cx + r, cy + r))
        } else {
            canvas.drawColor(end)
            val glyph = PathParser.createPathFromPathData(HEADPHONES)
            val scale = r * 1.1f / 24f
            glyph.transform(Matrix().apply { postScale(scale, scale); postTranslate(cx - 12f * scale, cy - 11f * scale) })
            canvas.drawPath(glyph, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        }
        canvas.restore()
        return out
    }

    private fun Canvas.drawCropped(bitmap: Bitmap, dst: RectF) {
        val scale = maxOf(dst.width() / bitmap.width, dst.height() / bitmap.height)
        val w = (dst.width() / scale).toInt()
        val h = (dst.height() / scale).toInt()
        val left = (bitmap.width - w) / 2
        val top = (bitmap.height - h) / 2
        drawBitmap(bitmap, Rect(left, top, left + w, top + h), dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }
}
