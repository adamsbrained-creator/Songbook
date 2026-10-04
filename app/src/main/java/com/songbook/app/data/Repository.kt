package com.songbook.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SongbookRepository(
    private val dao: SongbookDao,
    private val store: SettingsStore
) {
    val songs: Flow<List<SongEntity>> = dao.songs()
    val albums: Flow<List<AlbumEntity>> = dao.albums()
    val settings: Flow<AppSettings> = store.flow

    suspend fun song(id: Long) = dao.song(id)

    suspend fun createSong(song: SongEntity): Long = dao.insertSong(song.copy(updatedAt = System.currentTimeMillis()))

    suspend fun saveSong(song: SongEntity) = dao.updateSong(song.copy(updatedAt = System.currentTimeMillis()))

    /** Changes that should not bump "Recently Edited" (favorite flag etc.). */
    suspend fun saveSongQuietly(song: SongEntity) = dao.updateSong(song)

    suspend fun deleteSong(id: Long) = dao.deleteSong(id)

    suspend fun duplicate(song: SongEntity): Long =
        dao.insertSong(song.copy(id = 0, title = song.title + " copy", favorite = false, updatedAt = System.currentTimeMillis()))

    suspend fun createAlbum(name: String, gradient: Int = 0): Long =
        dao.insertAlbum(AlbumEntity(name = name.trim().ifEmpty { "Untitled album" }, gradient = gradient))

    suspend fun updateAlbum(album: AlbumEntity) = dao.updateAlbum(album)
    suspend fun deleteAlbum(id: Long) = dao.deleteAlbum(id)

    // ---- settings -------------------------------------------------------------------------
    suspend fun setTheme(v: ThemeMode) = store.setTheme(v)
    suspend fun setAccent(v: Int) = store.setAccent(v)
    suspend fun setNavRaised(v: Boolean) = store.setNavRaised(v)
    suspend fun setReading(l: Int, c: Int, s: Float) = store.setReading(l, c, s)
    suspend fun setShowChords(v: Boolean) = store.setShowChords(v)
    suspend fun setShowTabs(v: Boolean) = store.setShowTabs(v)
    suspend fun setAutoScroll(v: Boolean) = store.setAutoScroll(v)
    suspend fun setLastBackup(v: Long) = store.setLastBackup(v)
    suspend fun setUserName(v: String) = store.setUserName(v)

    // ---- backup ---------------------------------------------------------------------------
    suspend fun backupJson(): String = Backup.toJson(dao.allAlbums(), dao.allSongs())

    suspend fun exportText(): String = Backup.toText(dao.allAlbums(), dao.allSongs())

    /** Imports a JSON backup or a plain text file. Returns the number of songs added. */
    suspend fun importData(text: String, fileName: String?): Int {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) {
            val (albums, songs) = Backup.fromJson(trimmed)
            val existing = dao.allAlbums().associateBy { it.name }
            val idMap = HashMap<Long, Long>()
            for (a in albums) {
                val found = existing[a.name]
                idMap[a.id] = found?.id ?: dao.insertAlbum(a.copy(id = 0))
            }
            for (s in songs) {
                dao.insertSong(s.copy(id = 0, albumId = s.albumId?.let { idMap[it] }))
            }
            return songs.size
        }
        val title = fileName?.substringBeforeLast('.')?.ifBlank { null } ?: "Imported song"
        dao.insertSong(SongEntity(title = title, body = text))
        return 1
    }

    // ---- first run sample data ------------------------------------------------------------
    suspend fun seedIfNeeded() {
        val s = store.flow.first()
        if (s.seeded) return
        if (dao.albumCount() == 0 && dao.songCount() == 0) {
            dao.insertAlbum(AlbumEntity(name = "Get Back", gradient = 1, year = 2026))
            dao.insertAlbum(AlbumEntity(name = "Songbook Vol. I", gradient = 0, year = 2026))
            val unreleased = dao.insertAlbum(AlbumEntity(name = "Unreleased", gradient = 2, year = 2026))
            dao.insertAlbum(AlbumEntity(name = "Ideas", gradient = 3, year = 2026))
            dao.insertSong(
                SongEntity(
                    title = "I Don't Exist",
                    albumId = unreleased,
                    genre = "Indie folk",
                    tags = "indie, folk-rock, demo",
                    gradient = 0,
                    capo = 2,
                    body = SAMPLE_BODY
                )
            )
        }
        store.setSeeded(true)
    }

    companion object {
        const val SAMPLE_BODY = """# Verse 1
[Em]I woke up underneath the [D]morning
[C]But I couldn't recognize the light
[G]Everybody kept on talking
[D]I was somewhere [Em]out of sight

# Chorus
[Am]And if I don't [F]exist tonight
[C]Will the morning [G]notice me"""
    }
}
