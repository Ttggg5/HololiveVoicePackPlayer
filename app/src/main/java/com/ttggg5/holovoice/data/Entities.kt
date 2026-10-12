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
