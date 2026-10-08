package com.songbook.app.data

/** How the song list is ordered. */
object SongSort {
    const val RECENT = 0
    const val TITLE_AZ = 1
    const val TITLE_ZA = 2
    const val RATING = 3
    const val OLDEST = 4
    const val ALBUM = 5
    const val FAVORITES = 6
    const val MANUAL = 7
    val names = listOf("Recently edited", "Title A–Z", "Title Z–A", "Highest rated", "Oldest first", "By album", "Favorites first", "My order (drag to arrange)")
}

object AlbumSort {
    const val CREATED = 0
    const val NAME = 1
    const val YEAR_NEW = 2
    const val YEAR_OLD = 3
    const val RATING = 4
    const val MANUAL = 5
    val names = listOf("Date added", "Name A–Z", "Newest year", "Oldest year", "Highest rated", "My order (drag to arrange)")
}

/** Order of the songs inside one album. */
object TrackSort {
    const val ALBUM = 0
    const val TITLE = 1
    const val RATING = 2
    val names = listOf("Album order", "Title A–Z", "Highest rated")
}

private fun unarranged(v: Int) = if (v == 0) Int.MAX_VALUE else v

/** Album order of one list of songs: side, then the arranged place, then the order they were added. */
fun List<SongEntity>.inAlbumOrder(): List<SongEntity> =
    sortedWith(compareBy<SongEntity>({ it.side }, { unarranged(it.trackNo) }, { it.id }))

fun List<SongEntity>.sortedSongs(mode: Int, albumNames: Map<Long, String>): List<SongEntity> = when (mode) {
    SongSort.TITLE_AZ -> sortedBy { it.title.lowercase() }
    SongSort.TITLE_ZA -> sortedByDescending { it.title.lowercase() }
    SongSort.RATING -> sortedWith(compareByDescending<SongEntity> { it.rating }.thenBy { it.title.lowercase() })
    SongSort.OLDEST -> sortedBy { it.id }
    SongSort.ALBUM -> sortedWith(
        compareBy<SongEntity>({ albumNames[it.albumId]?.lowercase() ?: "\uffff" }, { it.side }, { unarranged(it.trackNo) }, { it.id })
    )
    SongSort.FAVORITES -> sortedWith(compareByDescending<SongEntity> { it.favorite }.thenByDescending { it.updatedAt })
    SongSort.MANUAL -> sortedWith(compareBy<SongEntity>({ unarranged(it.libPos) }, { it.id }))
    else -> sortedByDescending { it.updatedAt }
}

/** Songs of one side (or of the whole album when it has no sides) in the chosen track order. */
fun List<SongEntity>.sortedTracks(mode: Int): List<SongEntity> = when (mode) {
    TrackSort.TITLE -> sortedBy { it.title.lowercase() }
    TrackSort.RATING -> sortedWith(compareByDescending<SongEntity> { it.rating }.thenBy { it.title.lowercase() })
    else -> inAlbumOrder()
}

fun List<AlbumEntity>.sortedAlbums(mode: Int): List<AlbumEntity> = when (mode) {
    AlbumSort.NAME -> sortedBy { it.name.lowercase() }
    AlbumSort.YEAR_NEW -> sortedWith(compareByDescending<AlbumEntity> { it.year }.thenBy { it.createdAt })
    AlbumSort.YEAR_OLD -> sortedWith(compareBy<AlbumEntity>({ it.year }, { it.createdAt }))
    AlbumSort.RATING -> sortedWith(compareByDescending<AlbumEntity> { it.rating }.thenBy { it.name.lowercase() })
    AlbumSort.MANUAL -> sortedWith(compareBy<AlbumEntity>({ unarranged(it.position) }, { it.createdAt }))
    else -> sortedBy { it.createdAt }
}

fun List<GroupEntity>.sortedGroups(mode: Int): List<GroupEntity> = when (mode) {
    AlbumSort.NAME -> sortedBy { it.name.lowercase() }
    AlbumSort.RATING -> sortedWith(compareByDescending<GroupEntity> { it.rating }.thenBy { it.name.lowercase() })
    AlbumSort.MANUAL -> sortedWith(compareBy<GroupEntity>({ unarranged(it.position) }, { it.createdAt }))
    else -> sortedBy { it.createdAt }
}

/** "Side A" -> "A", so tracks read A1, A2, B1... Other side names fall back to plain numbers. */
fun sideLetter(name: String): String? =
    Regex("(?i)^side\\s*([A-Za-z0-9])$").find(name.trim())?.groupValues?.get(1)?.uppercase()
