package com.songbook.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Workflow stages for songs that are still being written (see the Workflow page). */
object Stage {
    val names = listOf("Idea", "Lyrics", "Chords", "Arrangement", "Finished")
    /** Ideas live on the Workflow page only; they are hidden from the library, albums and favorites. */
    const val IDEA = 0
    const val DONE = 4
    fun name(stage: Int): String = names[stage.coerceIn(0, DONE)]
}

/** A group of albums, e.g. a trilogy. Albums point at it with [AlbumEntity.groupId]. */
@Entity(tableName = "album_groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gradient: Int = 0,
    /** File name inside the app's covers folder, see [ImageStore]. */
    val image: String? = null,
    val badge: String = "",
    val badgeColor: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** 0..5 in steps of 0.25, 0 = not rated. */
    val rating: Float = 0f,
    /** Custom title such as "Trilogy" or "Box set". Blank = chosen automatically from the album count. */
    val typeName: String = ""
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gradient: Int = 0,
    val year: Int = java.time.Year.now().value,
    val createdAt: Long = System.currentTimeMillis(),
    val image: String? = null,
    val badge: String = "",
    val badgeColor: Int = 0,
    val groupId: Long? = null,
    val rating: Float = 0f,
    /** "Single", "EP", "LP"... or anything custom. Blank = "Album". */
    val typeName: String = ""
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
    val updatedAt: Long = System.currentTimeMillis(),
    val image: String? = null,
    val badge: String = "",
    val badgeColor: Int = 0,
    /** 0..3 = still being written, [Stage.DONE] = finished (not shown in Workflow). */
    val stage: Int = Stage.DONE,
    /** "Next step" note shown on the Workflow page. */
    val note: String = "",
    val rating: Float = 0f,
    /** "Demo", "Cover", "Live"... or anything custom. Blank = "Original song". */
    val typeName: String = "",
    /** Songwriting desk (spark, lines, checklist, custom fields) as JSON, see [IdeaData]. */
    val idea: String = "",
    /** When true and a capo is set, chords and tabs are shown as shapes for that capo. */
    val capoAdjust: Boolean = true
)
