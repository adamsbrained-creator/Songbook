package com.songbook.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gradient: Int = 0,
    val year: Int = java.time.Year.now().value,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "songs",
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("albumId")]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val albumId: Long? = null,
    val genre: String = "",
    /** Comma separated, e.g. "indie, demo". */
    val tags: String = "",
    val gradient: Int = 0,
    /**
     * Song text. Chords are written inline in square brackets ("[Em]I woke up"),
     * section headings start with "#" ("# Verse 1"), tab lines look like "e|--0--2--|".
     */
    val body: String = "",
    val capo: Int = 0,
    val favorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
