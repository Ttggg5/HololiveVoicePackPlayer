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
import com.ttggg5.holovoice.data.orderedTalents
import com.ttggg5.holovoice.data.talentImages

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
                        putString(ItemExtras.TALENT_IMAGE, talentImages.firstOrNull() ?: clip.talentImageUrl)
                        putStringArray(ItemExtras.TALENT_IMAGES, talentImages.toTypedArray())
                        putString(ItemExtras.TALENT_SLUGS, orderedTalents.joinToString("\n") { it.slug })
                        putString(ItemExtras.COVER, coverUri)
                        putString(ItemExtras.CATEGORY, clip.category)
                        clip.trackNo?.let { putInt(ItemExtras.TRACK, it) }
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
    const val TALENT_IMAGES = "talentImages"
    const val TALENT_SLUGS = "talentSlugs"
    const val COVER = "cover"
    const val CATEGORY = "category"

    /** The clip's sequence number from its file name ("03_…" → 3), when it has one. */
    const val TRACK = "track"
}

/** A–B loop: the service jumps back to the start whenever playback reaches the end. -1 clears it. */
object AbLoop {
    const val COMMAND = "com.ttggg5.holovoice.AB_LOOP"
    const val ARG_START = "start"
    const val ARG_END = "end"
    const val EXTRA_START = "loopStart"
    const val EXTRA_END = "loopEnd"
}

/** Custom session command and extras shared by [PlaybackService] and [PlayerController]. */
object SleepTimer {
    const val COMMAND = "com.ttggg5.holovoice.SLEEP_TIMER"
    const val ARG_MINUTES = "minutes"
    const val ARG_END_OF_TRACK = "endOfTrack"
    const val EXTRA_ENDS_AT = "sleepEndsAt"
    const val EXTRA_END_OF_TRACK = "sleepEndOfTrack"
}
