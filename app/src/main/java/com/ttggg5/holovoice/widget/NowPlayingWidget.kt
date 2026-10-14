package com.ttggg5.holovoice.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import com.ttggg5.holovoice.MainActivity
import com.ttggg5.holovoice.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Home-screen widget: the playing clip (talent picture, title, who is speaking) over the topic's
 * thumbnail, with previous / play-pause / next. The buttons are media-button presses, so they work
 * even when the app is closed (the last queue is resumed). Tapping the widget opens Now Playing.
 */
class NowPlayingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                manager.updateAppWidget(ids, NowPlayingWidget.build(context, NowPlayingWidget.load(context)))
            } finally {
                pending.finish()
            }
        }
    }
}

@OptIn(UnstableApi::class)
object NowPlayingWidget {
    /** What the widget shows; kept in preferences so it can be redrawn while the app is closed. */
    data class Info(
        val title: String,
        val subtitle: String,
        val isPlaying: Boolean,
        val picture: String?,
        val cover: String?,
    )

    private const val PREFS = "widget"

    fun save(context: Context, info: Info?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (info == null) {
                clear()
            } else {
                putString("title", info.title)
                putString("subtitle", info.subtitle)
                putBoolean("playing", info.isPlaying)
                putString("picture", info.picture)
                putString("cover", info.cover)
            }
        }.apply()
    }

    fun load(context: Context): Info? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val title = prefs.getString("title", null) ?: return null
        return Info(
            title = title,
            subtitle = prefs.getString("subtitle", "").orEmpty(),
            isPlaying = prefs.getBoolean("playing", false),
            picture = prefs.getString("picture", null),
            cover = prefs.getString("cover", null),
        )
    }

    /** Saves [info] and redraws every placed widget (does nothing when none is placed). */
    suspend fun push(context: Context, info: Info?) {
        val app = context.applicationContext
        save(app, info)
        val manager = AppWidgetManager.getInstance(app)
        val ids = manager.getAppWidgetIds(ComponentName(app, NowPlayingWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val views = withContext(Dispatchers.IO) { build(app, info) }
        manager.updateAppWidget(ids, views)
    }

    fun build(context: Context, info: Info?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_now_playing)
        if (info == null) {
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_nothing_playing))
            views.setTextViewText(R.id.widget_subtitle, context.getString(R.string.widget_tap_to_start))
            views.setImageViewResource(R.id.widget_picture, R.drawable.ic_widget_placeholder)
            views.setViewVisibility(R.id.widget_backdrop, View.GONE)
        } else {
            views.setTextViewText(R.id.widget_title, info.title)
            views.setTextViewText(R.id.widget_subtitle, info.subtitle)
            val picture = info.picture?.let { decode(context, it, 160) }?.let(::circle)
            if (picture != null) {
                views.setImageViewBitmap(R.id.widget_picture, picture)
            } else {
                views.setImageViewResource(R.id.widget_picture, R.drawable.ic_widget_placeholder)
            }
            // A tiny thumbnail stretched over the widget reads as a soft blur. Before Android 12 the
            // widget can't clip it to the round corners, so it is left out there.
            val backdrop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.cover?.let { decode(context, it, 48) } else null
            if (backdrop != null) {
                views.setImageViewBitmap(R.id.widget_backdrop, backdrop)
                views.setViewVisibility(R.id.widget_backdrop, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_backdrop, View.GONE)
            }
        }
        val playing = info?.isPlaying == true
        views.setImageViewResource(R.id.widget_play, if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        views.setContentDescription(R.id.widget_play, if (playing) "Pause" else "Play")
        views.setOnClickPendingIntent(R.id.widget_play, mediaButton(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.widget_prev, mediaButton(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        views.setOnClickPendingIntent(R.id.widget_next, mediaButton(context, KeyEvent.KEYCODE_MEDIA_NEXT))
        views.setOnClickPendingIntent(
            R.id.widget_info,
            PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_PLAYER, true),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        return views
    }

    private fun mediaButton(context: Context, keyCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        keyCode,
        Intent(Intent.ACTION_MEDIA_BUTTON)
            .setComponent(ComponentName(context, MediaButtonReceiver::class.java))
            .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** A picture from the app's storage or a picked folder, scaled down to about [size] px. */
    fun decode(context: Context, uriString: String, size: Int): Bitmap? = runCatching {
        val uri = Uri.parse(uriString)
        fun open() = when (uri.scheme) {
            "file" -> File(uri.path!!).inputStream()
            "content" -> context.contentResolver.openInputStream(uri)
            else -> null // pictures not stored yet (still on the web) are skipped
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) sample *= 2
        val decoded = open()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: return null
        val scale = size.toFloat() / minOf(decoded.width, decoded.height)
        if (scale >= 1f) decoded else Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
    }.getOrNull()

    /** [source] center-cropped into a circle. */
    fun circle(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, ((size - source.width) / 2f), ((size - source.height) / 2f), paint)
        return out
    }
}
