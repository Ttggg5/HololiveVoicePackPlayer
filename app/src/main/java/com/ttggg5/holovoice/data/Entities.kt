package com.ttggg5.holovoice.data

import com.ttggg5.holovoice.data.talents.TalentTags
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class PackSource { FOLDER, ZIP }

@Entity(tableName = "packs")
data class Pack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val talent: String,
    /** Image URI (content:// or file://) used as the pack cover, if any. */
    val coverUri: String?,
    /** Tree URI for folder imports, or the extraction directory path for ZIP imports. */
    val sourceUri: String,
    val source: PackSource,
    val importedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "clips",
    foreignKeys = [
        ForeignKey(
            entity = Pack::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("packId")],
)
data class Clip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: Long,
    val title: String,
    val fileUri: String,
    val durationMs: Long?,
    val trackNo: Int?,
    val isFavorite: Boolean = false,
    /** Talent matched from the file name (or picked by the user), with their profile picture. */
    val talentSlug: String? = null,
    val talentName: String? = null,
    val talentImageUrl: String? = null,
    /** True when the user chose the talent; automatic matching then leaves it alone. */
    @ColumnInfo(defaultValue = "0") val talentManual: Boolean = false,
    /** Sub-folder path inside the topic ("Morning", "Extra / Short"); null for files in the topic's root. */
    val category: String? = null,
    @ColumnInfo(defaultValue = "0") val addedAt: Long = 0,
    @ColumnInfo(defaultValue = "0") val lastPlayedAt: Long = 0,
    @ColumnInfo(defaultValue = "0") val playCount: Int = 0,
)

/** One of possibly several talents heard in a clip; [position] keeps their order. */
@Entity(
    tableName = "clip_talents",
    primaryKeys = ["clipId", "slug"],
    foreignKeys = [
        ForeignKey(entity = Clip::class, parentColumns = ["id"], childColumns = ["clipId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("slug")],
)
data class ClipTalent(
    val clipId: Long,
    val slug: String,
    val name: String,
    val imageUrl: String?,
    val position: Int,
)

/** A clip together with the name/talent/cover of the pack it belongs to, and all its talents. */
data class ClipWithPack(
    @Embedded val clip: Clip,
    @ColumnInfo(name = "packName") val packName: String,
    @ColumnInfo(name = "talent") val talent: String,
    @ColumnInfo(name = "coverUri") val coverUri: String?,
    @Relation(parentColumn = "id", entityColumn = "clipId")
    val talents: List<ClipTalent> = emptyList(),
)

/** Picture to show for a clip: a collage for several talents, the talent's picture, else the pack cover. */
val ClipWithPack.artwork: String?
    get() = talentImages.let { images ->
        // Several talents: a split collage of all their pictures.
        if (images.size > 1) Collage.uri(images) else images.firstOrNull() ?: clip.talentImageUrl ?: coverUri
    }

/** Thumbnail of a clip in lists: its topic's thumbnail, else [artwork]. */
val ClipWithPack.thumbnail: String?
    get() = coverUri ?: artwork

/** The clip's talents in order (falls back to the single primary talent from older data). */
val ClipWithPack.orderedTalents: List<ClipTalent>
    get() = talents.sortedBy { it.position }.ifEmpty {
        listOfNotNull(clip.talentSlug?.let { ClipTalent(clip.id, it, clip.talentName ?: it, clip.talentImageUrl, 0) })
    }

/** Who is speaking: "A & B" for the matched talents, else the pack's talent. */
val ClipWithPack.speaker: String
    get() = orderedTalents.takeIf { it.isNotEmpty() }?.joinToString(" & ") { TalentTags.clean(it.name) } ?: talent

/** Profile pictures of all talents in the clip, in order. */
val ClipWithPack.talentImages: List<String> get() = orderedTalents.mapNotNull { it.imageUrl }

data class PackWithCount(
    @Embedded val pack: Pack,
    @ColumnInfo(name = "clipCount") val clipCount: Int,
    @ColumnInfo(name = "categoryCount") val categoryCount: Int,
    /** Clips played at least once. */
    @ColumnInfo(name = "listenedCount") val listenedCount: Int = 0,
) {
    /** Every clip of the topic has been listened to. */
    val allListened: Boolean get() = clipCount > 0 && listenedCount >= clipCount
}

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playlist_entries",
    foreignKeys = [
        ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Clip::class, parentColumns = ["id"], childColumns = ["clipId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("playlistId"), Index("clipId")],
)
data class PlaylistEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val clipId: Long,
    val position: Int,
)

data class PlaylistWithCount(
    @Embedded val playlist: Playlist,
    @ColumnInfo(name = "entryCount") val entryCount: Int,
    @ColumnInfo(name = "coverUri") val coverUri: String?,
    /** Sum of the clips' lengths (clips without a known length count as 0). */
    @ColumnInfo(name = "totalMs") val totalMs: Long = 0,
)

/** A topic thumbnail used in a playlist, in entry order (for the playlist's mosaic). */
data class PlaylistCover(
    @ColumnInfo(name = "playlistId") val playlistId: Long,
    @ColumnInfo(name = "coverUri") val coverUri: String,
)

/** One row of a playlist: the clip plus the entry id that identifies this occurrence. */
data class PlaylistItem(
    @Embedded val item: ClipWithPack,
    @ColumnInfo(name = "entryId") val entryId: Long,
)

/** One talent in the library (or, with a null [slug], all clips without a talent). */
data class TalentSummary(
    @ColumnInfo(name = "slug") val slug: String?,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "imageUrl") val imageUrl: String?,
    @ColumnInfo(name = "clipCount") val clipCount: Int,
    @ColumnInfo(name = "plays") val plays: Int,
    @ColumnInfo(name = "lastPlayed") val lastPlayed: Long,
    @ColumnInfo(name = "topicCount") val topicCount: Int,
    /** Clips played at least once. */
    @ColumnInfo(name = "listenedCount") val listenedCount: Int = 0,
) {
    /** Every clip of this talent has been listened to. */
    val allListened: Boolean get() = clipCount > 0 && listenedCount >= clipCount
}
