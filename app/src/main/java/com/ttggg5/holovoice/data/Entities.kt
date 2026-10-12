package com.ttggg5.holovoice.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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

/** A clip together with the name/talent/cover of the pack it belongs to. */
data class ClipWithPack(
    @Embedded val clip: Clip,
    @ColumnInfo(name = "packName") val packName: String,
    @ColumnInfo(name = "talent") val talent: String,
    @ColumnInfo(name = "coverUri") val coverUri: String?,
)

/** Picture to show for a clip: the talent's profile picture, else the pack cover. */
val ClipWithPack.artwork: String? get() = clip.talentImageUrl ?: coverUri

/** Who is speaking: the matched talent, else the pack's talent. */
val ClipWithPack.speaker: String get() = clip.talentName ?: talent

data class PackWithCount(
    @Embedded val pack: Pack,
    @ColumnInfo(name = "clipCount") val clipCount: Int,
    @ColumnInfo(name = "categoryCount") val categoryCount: Int,
)

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
)
