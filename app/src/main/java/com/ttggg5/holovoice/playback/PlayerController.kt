package com.ttggg5.holovoice.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.ttggg5.holovoice.data.ClipWithPack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/** One entry of the play queue. [index] is the position in the player's (unshuffled) item list. */
data class QueueItem(
    val index: Int,
    val key: String,
    val clipId: Long?,
    val title: String,
    val subtitle: String,
    val artworkUri: Uri?,
)

data class PlayerState(
    val clipId: Long? = null,
    val title: String = "",
    /** Who is speaking (matched talent, else the topic's talent). */
    val subtitle: String = "",
    /** Topic (pack) name. */
    val album: String = "",
    val artworkUri: Uri? = null,
    val packId: Long? = null,
    val category: String? = null,
    val talentImageUrl: String? = null,
    /** Pictures and ids of every talent in the clip, in order. */
    val talentImages: List<String> = emptyList(),
    val talentSlugs: List<String> = emptyList(),
    val coverUri: String? = null,
    val isPlaying: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    /** Queue in play order (shuffle order when shuffle is on). */
    val queue: List<QueueItem> = emptyList(),
    val currentIndex: Int = C.INDEX_UNSET,
    /** Wall-clock time the sleep timer fires, or 0 when no timed sleep is set. */
    val sleepEndsAt: Long = 0,
    val sleepAtEndOfTrack: Boolean = false,
)

/** App-wide entry point for playback; talks to [PlaybackService] through a [MediaController]. */
@OptIn(UnstableApi::class)
class PlayerController(context: Context) {
    private val appContext = context.applicationContext
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Current playback position, polled while collected. */
    val position: Flow<Long> = flow {
        while (true) {
            emit(controller?.currentPosition ?: 0L)
            delay(250)
        }
    }

    private var queueCache: List<QueueItem> = emptyList()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val queueChanged = events.containsAny(
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            )
            publish(player, rebuildQueue = queueChanged)
        }
    }

    private val sessionListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) = publishSleep(extras)
    }

    /** Binds to the playback service. Call from the main thread (e.g. Activity.onStart). */
    fun connect() {
        if (future != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val f = MediaController.Builder(appContext, token).setListener(sessionListener).buildAsync()
        future = f
        f.addListener({
            val c = runCatching { f.get() }.getOrNull() ?: run {
                future = null
                return@addListener
            }
            controller = c
            c.addListener(listener)
            publish(c, rebuildQueue = true)
            publishSleep(c.sessionExtras)
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
        if (c != null) {
            block(c)
        } else {
            pending += block
            connect()
        }
    }

    /**
     * Plays [clips] as the queue starting at [index]. Tapping the clip that is already
     * current toggles play/pause instead of restarting it.
     */
    fun playFrom(clips: List<ClipWithPack>, index: Int) {
        val target = clips.getOrNull(index) ?: return
        if (_state.value.clipId == target.clip.id) {
            togglePlayPause()
            return
        }
        playQueue(clips, index)
    }

    /**
     * Queues [clips] starting at [startIndex]. [shuffle] turns shuffle on (random start) or off;
     * null keeps the current shuffle mode.
     */
    fun playQueue(clips: List<ClipWithPack>, startIndex: Int = 0, shuffle: Boolean? = null) {
        if (clips.isEmpty()) return
        withController { c ->
            if (shuffle != null) c.shuffleModeEnabled = shuffle
            val start = if (shuffle == true) clips.indices.random() else startIndex.coerceIn(clips.indices)
            c.setMediaItems(clips.map { it.toMediaItem() }, start, 0L)
            c.prepare()
            c.play()
        }
    }

    /** Inserts [clips] right after the current item. */
    fun playNext(clips: List<ClipWithPack>) {
        if (clips.isEmpty()) return
        withController { c ->
            if (c.mediaItemCount == 0) {
                playQueue(clips, shuffle = false)
            } else {
                c.addMediaItems(c.currentMediaItemIndex + 1, clips.map { it.toMediaItem() })
            }
        }
    }

    /** Appends [clips] to the end of the queue. */
    fun addToQueue(clips: List<ClipWithPack>) {
        if (clips.isEmpty()) return
        withController { c ->
            if (c.mediaItemCount == 0) playQueue(clips, shuffle = false) else c.addMediaItems(clips.map { it.toMediaItem() })
        }
    }

    fun togglePlayPause() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            when (c.playbackState) {
                Player.STATE_IDLE -> c.prepare()
                Player.STATE_ENDED -> c.seekToDefaultPosition(c.currentTimeline.getFirstWindowIndex(c.shuffleModeEnabled).coerceAtLeast(0))
                else -> Unit
            }
            c.play()
        }
    }

    fun next() = withController { it.seekToNext() }

    /** Restarts the current clip, or goes to the previous one when near the start. */
    fun previous() = withController { it.seekToPrevious() }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    /** Jumps to the queue item at player index [index]. */
    fun skipTo(index: Int) = withController { c ->
        if (index !in 0 until c.mediaItemCount) return@withController
        c.seekToDefaultPosition(index)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun moveQueueItem(from: Int, to: Int) = withController { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }

    /** Removes the item at player index [index]; [onRemoved] gets it back so the removal can be undone. */
    fun removeQueueItem(index: Int, onRemoved: (MediaItem) -> Unit = {}) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            val item = c.getMediaItemAt(index)
            c.removeMediaItem(index)
            onRemoved(item)
        }
    }

    /** Puts a removed item back at [index] (undo). */
    fun restoreQueueItem(index: Int, item: MediaItem) = withController { c ->
        c.addMediaItem(index.coerceIn(0, c.mediaItemCount), item)
    }

    fun clearQueue() = withController { c ->
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

    fun setSpeed(speed: Float) = withController { it.setPlaybackSpeed(speed) }

    /** Sets the sleep timer: [minutes] > 0 pauses after that long, [endOfTrack] pauses when the current clip ends. */
    fun setSleepTimer(minutes: Int = 0, endOfTrack: Boolean = false) = withController { c ->
        val args = Bundle().apply {
            putInt(SleepTimer.ARG_MINUTES, minutes)
            putBoolean(SleepTimer.ARG_END_OF_TRACK, endOfTrack)
        }
        c.sendCustomCommand(SessionCommand(SleepTimer.COMMAND, Bundle.EMPTY), args)
    }

    private fun publishSleep(extras: Bundle) {
        _state.value = _state.value.copy(
            sleepEndsAt = extras.getLong(SleepTimer.EXTRA_ENDS_AT, 0L),
            sleepAtEndOfTrack = extras.getBoolean(SleepTimer.EXTRA_END_OF_TRACK, false),
        )
    }

    private fun publish(player: Player, rebuildQueue: Boolean) {
        if (rebuildQueue) queueCache = buildQueue(player)
        val item = player.currentMediaItem
        val metadata = item?.mediaMetadata
        val duration = player.duration
        _state.value = _state.value.copy(
            clipId = item?.mediaId?.toLongOrNull(),
            title = metadata?.title?.toString().orEmpty(),
            subtitle = metadata?.artist?.toString().orEmpty(),
            album = metadata?.albumTitle?.toString().orEmpty(),
            artworkUri = metadata?.artworkUri,
            packId = metadata?.extras?.getLong(ItemExtras.PACK_ID, -1L)?.takeIf { it >= 0 },
            category = metadata?.extras?.getString(ItemExtras.CATEGORY),
            talentImageUrl = metadata?.extras?.getString(ItemExtras.TALENT_IMAGE),
            talentImages = metadata?.extras?.getStringArray(ItemExtras.TALENT_IMAGES)?.toList().orEmpty(),
            talentSlugs = metadata?.extras?.getString(ItemExtras.TALENT_SLUGS)?.split('\n')?.filter { it.isNotEmpty() }.orEmpty(),
            coverUri = metadata?.extras?.getString(ItemExtras.COVER),
            isPlaying = player.isPlaying,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            durationMs = if (duration == C.TIME_UNSET) 0L else duration,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            speed = player.playbackParameters.speed,
            queue = queueCache,
            currentIndex = if (item == null) C.INDEX_UNSET else player.currentMediaItemIndex,
        )
    }

    private fun buildQueue(player: Player): List<QueueItem> {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val shuffle = player.shuffleModeEnabled
        val seen = HashMap<String, Int>()
        val result = ArrayList<QueueItem>(timeline.windowCount)
        var index = timeline.getFirstWindowIndex(shuffle)
        while (index != C.INDEX_UNSET && result.size < timeline.windowCount) {
            val item = player.getMediaItemAt(index)
            val occurrence = (seen[item.mediaId] ?: 0) + 1
            seen[item.mediaId] = occurrence
            result += QueueItem(
                index = index,
                key = "${item.mediaId}#$occurrence",
                clipId = item.mediaId.toLongOrNull(),
                title = item.mediaMetadata.title?.toString().orEmpty(),
                subtitle = listOfNotNull(item.mediaMetadata.artist, item.mediaMetadata.albumTitle).joinToString(" · "),
                artworkUri = item.mediaMetadata.artworkUri,
            )
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffle)
        }
        return result
    }
}
