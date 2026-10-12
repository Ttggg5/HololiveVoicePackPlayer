package com.ttggg5.holovoice.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.ttggg5.holovoice.data.ClipWithPack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(
    val clipId: Long? = null,
    val title: String = "",
    val subtitle: String = "",
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val hasNext: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

/** App-wide entry point for playback; talks to [PlaybackService] through a [MediaController]. */
class PlayerController(context: Context) {
    private val appContext = context.applicationContext
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    /** Binds to the playback service. Call from the main thread (e.g. Activity.onStart). */
    fun connect() {
        if (future != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val f = MediaController.Builder(appContext, token).buildAsync()
        future = f
        f.addListener({
            val c = runCatching { f.get() }.getOrNull() ?: run { future = null; return@addListener }
            controller = c
            c.addListener(listener)
            publish(c)
            pending.forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(appContext))
    }

    /** Unbinds; playback keeps running in the service if it is active. */
    fun disconnect() {
        controller?.removeListener(listener)
        future?.let(MediaController::releaseFuture)
        future = null
        controller = null
        pending.clear()
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else {
            pending += block
            connect()
        }
    }

    /** Soundboard tap: play a single clip, or pause it if it is the one currently playing. */
    fun playClip(clip: ClipWithPack) = withController { c ->
        if (c.currentMediaItem?.mediaId == clip.clip.id.toString() && c.isPlaying) {
            c.pause()
            return@withController
        }
        c.shuffleModeEnabled = false
        c.setMediaItem(clip.toMediaItem())
        c.prepare()
        c.play()
    }

    /** Queues [clips] as a playlist starting at [startIndex], optionally shuffled. */
    fun playQueue(clips: List<ClipWithPack>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (clips.isEmpty()) return
        withController { c ->
            c.shuffleModeEnabled = shuffle
            val start = if (shuffle) clips.indices.random() else startIndex.coerceIn(clips.indices)
            c.setMediaItems(clips.map { it.toMediaItem() }, start, 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlayPause() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            c.play()
        }
    }

    fun next() = withController { it.seekToNextMediaItem() }

    fun stop() = withController { c ->
        c.stop()
        c.clearMediaItems()
    }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = PlayerState(
            clipId = item?.mediaId?.toLongOrNull(),
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            subtitle = item?.mediaMetadata?.artist?.toString().orEmpty(),
            artworkUri = item?.mediaMetadata?.artworkUri,
            isPlaying = player.isPlaying,
            hasNext = player.hasNextMediaItem(),
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
    }
}

private fun ClipWithPack.toMediaItem(): MediaItem {
    val uri = Uri.parse(clip.fileUri)
    return MediaItem.Builder()
        .setMediaId(clip.id.toString())
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(clip.title)
                .setArtist(talent)
                .setAlbumTitle(packName)
                .setArtworkUri(coverUri?.let(Uri::parse))
                .build(),
        )
        .build()
}
