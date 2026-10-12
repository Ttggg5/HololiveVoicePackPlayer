package com.ttggg5.holovoice.playback

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.artwork
import com.ttggg5.holovoice.data.speaker

/** Builds a playable item whose media id is the clip id, so queues can be saved and restored by id. */
@OptIn(UnstableApi::class)
fun ClipWithPack.toMediaItem(): MediaItem {
    val uri = Uri.parse(clip.fileUri)
    return MediaItem.Builder()
        .setMediaId(clip.id.toString())
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(clip.title)
                .setArtist(speaker)
                .setAlbumTitle(packName)
                .setSubtitle(clip.category)
                .setArtworkUri(artwork?.let(Uri::parse))
                .setExtras(
                    Bundle().apply {
                        putLong(ItemExtras.PACK_ID, clip.packId)
                        putString(ItemExtras.TALENT_IMAGE, clip.talentImageUrl)
                        putString(ItemExtras.COVER, coverUri)
                        putString(ItemExtras.CATEGORY, clip.category)
                    },
                )
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()
}

/** Keys of [MediaMetadata.extras] that tell the UI which topic and talent an item belongs to. */
object ItemExtras {
    const val PACK_ID = "packId"
    const val TALENT_IMAGE = "talentImage"
    const val COVER = "cover"
    const val CATEGORY = "category"
}

/** Custom session command and extras shared by [PlaybackService] and [PlayerController]. */
object SleepTimer {
    const val COMMAND = "com.ttggg5.holovoice.SLEEP_TIMER"
    const val ARG_MINUTES = "minutes"
    const val ARG_END_OF_TRACK = "endOfTrack"
    const val EXTRA_ENDS_AT = "sleepEndsAt"
    const val EXTRA_END_OF_TRACK = "sleepEndOfTrack"
}
