package com.ttggg5.holovoice.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ttggg5.holovoice.data.ClipWithPack

/** Builds a playable item whose media id is the clip id, so queues can be saved and restored by id. */
fun ClipWithPack.toMediaItem(): MediaItem {
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
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()
}

/** Custom session command and extras shared by [PlaybackService] and [PlayerController]. */
object SleepTimer {
    const val COMMAND = "com.ttggg5.holovoice.SLEEP_TIMER"
    const val ARG_MINUTES = "minutes"
    const val ARG_END_OF_TRACK = "endOfTrack"
    const val EXTRA_ENDS_AT = "sleepEndsAt"
    const val EXTRA_END_OF_TRACK = "sleepEndOfTrack"
}
