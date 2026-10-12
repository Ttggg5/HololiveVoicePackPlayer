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
)

/** A clip together with the name/talent/cover of the pack it belongs to. */
data class ClipWithPack(
    @Embedded val clip: Clip,
    @ColumnInfo(name = "packName") val packName: String,
    @ColumnInfo(name = "talent") val talent: String,
    @ColumnInfo(name = "coverUri") val coverUri: String?,
)

data class PackWithCount(
    @Embedded val pack: Pack,
    @ColumnInfo(name = "clipCount") val clipCount: Int,
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
