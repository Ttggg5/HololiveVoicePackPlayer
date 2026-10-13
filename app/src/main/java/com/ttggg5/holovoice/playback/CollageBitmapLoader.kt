package com.ttggg5.holovoice.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.ttggg5.holovoice.data.Accent
import com.ttggg5.holovoice.data.Collage

/**
 * Loads notification / lock-screen artwork: the topic thumbnail as the background with the clip's
 * talent picture (a split collage for clips with several talents) on top, composed by
 * [NotificationArtwork]. Topics without a thumbnail get the accent-coloured gradient instead.
 * Results are cached by cover, picture and accent, so an accent change applies straight away.
 */
@OptIn(UnstableApi::class)
class CollageBitmapLoader(
    private val context: Context,
    private val delegate: BitmapLoader,
    private val accent: () -> Accent,
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean = delegate.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = delegate.decodeBitmap(data)

    private val cache = object : LinkedHashMap<String, ListenableFuture<Bitmap>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ListenableFuture<Bitmap>>) = size > CACHE_SIZE
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = card(cover = null, picture = uri.toString())

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        metadata.artworkData?.let { return decodeBitmap(it) }
        val cover = metadata.extras?.getString(ItemExtras.COVER)
        val picture = metadata.artworkUri?.toString()?.takeIf { it != cover }
        if (cover == null && picture == null) return null
        return card(cover, picture)
    }

    private fun card(cover: String?, picture: String?): ListenableFuture<Bitmap> {
        val accent = accent()
        val key = "$cover|$picture|$accent"
        synchronized(cache) {
            cache[key]?.let { if (!it.isCancelled) return it }
        }
        // A missing image is left out (a card with a headphones glyph at worst) instead of no artwork.
        val coverLoad = cover?.let { orNull(delegate.loadBitmap(Uri.parse(it))) } ?: Futures.immediateFuture<Bitmap?>(null)
        val pictureLoad = picture?.let { orNull(loadPicture(Uri.parse(it))) } ?: Futures.immediateFuture<Bitmap?>(null)
        val result = Futures.whenAllComplete(coverLoad, pictureLoad).call(
            { NotificationArtwork.composeCard(context, Futures.getDone(coverLoad), Futures.getDone(pictureLoad), accent) },
            MoreExecutors.directExecutor(),
        )
        synchronized(cache) { cache[key] = result }
        return result
    }

    private fun orNull(future: ListenableFuture<Bitmap>): ListenableFuture<Bitmap?> =
        Futures.catching(future, Throwable::class.java, { null }, MoreExecutors.directExecutor())

    private fun loadPicture(uri: Uri): ListenableFuture<Bitmap> {
        val images = Collage.parse(uri.toString()) ?: return delegate.loadBitmap(uri)
        val loads = images.map { delegate.loadBitmap(Uri.parse(it)) }
        return Futures.transform(
            Futures.successfulAsList(loads),
            { bitmaps ->
                val loaded = bitmaps.orEmpty().filterNotNull()
                if (loaded.isEmpty()) throw IllegalStateException("No talent pictures could be loaded")
                draw(loaded, extra = images.size - 4)
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun draw(pictures: List<Bitmap>, extra: Int): Bitmap {
        val size = SIZE.toFloat()
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        val gap = 4f
        when (pictures.size) {
            1 -> canvas.drawCropped(pictures[0], RectF(0f, 0f, size, size))
            2 -> {
                val left = Path().apply {
                    moveTo(0f, 0f); lineTo(size * 0.6f, 0f); lineTo(size * 0.4f, size); lineTo(0f, size); close()
                }
                val right = Path().apply {
                    moveTo(size * 0.6f, 0f); lineTo(size, 0f); lineTo(size, size); lineTo(size * 0.4f, size); close()
                }
                canvas.save(); canvas.clipPath(left); canvas.drawCropped(pictures[0], RectF(0f, 0f, size * 0.6f, size)); canvas.restore()
                canvas.save(); canvas.clipPath(right); canvas.drawCropped(pictures[1], RectF(size * 0.4f, 0f, size, size)); canvas.restore()
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = gap * 1.5f }
                canvas.drawLine(size * 0.6f, 0f, size * 0.4f, size, line)
            }
            3 -> {
                val half = size / 2
                canvas.drawCropped(pictures[0], RectF(0f, 0f, half - gap / 2, size))
                canvas.drawCropped(pictures[1], RectF(half + gap / 2, 0f, size, half - gap / 2))
                canvas.drawCropped(pictures[2], RectF(half + gap / 2, half + gap / 2, size, size))
            }
            else -> {
                val half = size / 2
                for (i in 0 until 4) {
                    val x = if (i % 2 == 0) 0f else half + gap / 2
                    val y = if (i < 2) 0f else half + gap / 2
                    val cell = RectF(x, y, x + half - gap / 2, y + half - gap / 2)
                    canvas.drawCropped(pictures[i], cell)
                    if (i == 3 && extra > 0) {
                        canvas.drawRect(cell, Paint().apply { color = Color.argb(140, 0, 0, 0) })
                        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.WHITE
                            textSize = half * 0.4f
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        canvas.drawText("+$extra", cell.centerX(), cell.centerY() - (text.ascent() + text.descent()) / 2, text)
                    }
                }
            }
        }
        return out
    }

    /** Draws [bitmap] center-cropped to fill [dst]. */
    private fun Canvas.drawCropped(bitmap: Bitmap, dst: RectF) {
        val scale = maxOf(dst.width() / bitmap.width, dst.height() / bitmap.height)
        val w = (dst.width() / scale).toInt()
        val h = (dst.height() / scale).toInt()
        val left = (bitmap.width - w) / 2
        val top = (bitmap.height - h) / 2
        drawBitmap(bitmap, Rect(left, top, left + w, top + h), dst, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private companion object {
        const val SIZE = 512
        const val CACHE_SIZE = 8
    }
}
