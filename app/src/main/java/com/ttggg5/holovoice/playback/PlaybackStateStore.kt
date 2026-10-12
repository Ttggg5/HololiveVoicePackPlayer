package com.ttggg5.holovoice.playback

import android.content.Context
import androidx.media3.common.Player

/** What is needed to rebuild the queue after the app or service was killed. */
data class SavedPlayback(
    val clipIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
    val speed: Float,
)

class PlaybackStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE)

    fun save(player: Player) {
        val ids = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        prefs.edit()
            .putString(KEY_QUEUE, ids.joinToString(","))
            .putInt(KEY_INDEX, player.currentMediaItemIndex)
            .putLong(KEY_POSITION, player.currentPosition.coerceAtLeast(0))
            .putBoolean(KEY_SHUFFLE, player.shuffleModeEnabled)
            .putInt(KEY_REPEAT, player.repeatMode)
            .putFloat(KEY_SPEED, player.playbackParameters.speed)
            .apply()
    }

    fun load(): SavedPlayback = SavedPlayback(
        clipIds = prefs.getString(KEY_QUEUE, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() },
        index = prefs.getInt(KEY_INDEX, 0),
        positionMs = prefs.getLong(KEY_POSITION, 0),
        shuffle = prefs.getBoolean(KEY_SHUFFLE, false),
        repeatMode = prefs.getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF),
        speed = prefs.getFloat(KEY_SPEED, 1f),
    )

    private companion object {
        const val KEY_QUEUE = "queue"
        const val KEY_INDEX = "index"
        const val KEY_POSITION = "position"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT = "repeat"
        const val KEY_SPEED = "speed"
    }
}
