package com.songbook.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** What a backup file contains, shown before the user decides how to restore it. */
class BackupInfo(
    val version: Int,
    val songs: Int,
    val albums: Int,
    val groups: Int,
    val hasSettings: Boolean,
    val ideas: Int,
    val images: Int
)

class RestoreResult(val songs: Int, val albums: Int, val groups: Int, val skipped: Int)

class SongbookRepository(
    private val context: Context,
    private val dao: SongbookDao,
    private val store: SettingsStore
) {
    val songs: Flow<List<SongEntity>> = dao.songs()
    val albums: Flow<List<AlbumEntity>> = dao.albums()
    val groups: Flow<List<GroupEntity>> = dao.groups()
    val settings: Flow<AppSettings> = store.flow

    suspend fun song(id: Long) = dao.song(id)

    suspend fun createSong(song: SongEntity): Long = dao.insertSong(song.copy(updatedAt = System.currentTimeMillis()))

    suspend fun saveSong(song: SongEntity) = dao.updateSong(song.copy(updatedAt = System.currentTimeMillis()))

    /** Changes that should not bump "Recently Edited" (favorite flag, badge, stage etc.). */
    suspend fun saveSongQuietly(song: SongEntity) = dao.updateSong(song)

    suspend fun deleteSong(id: Long) = dao.deleteSong(id)

    suspend fun duplicate(song: SongEntity): Long =
        dao.insertSong(song.copy(id = 0, title = song.title + " copy", favorite = false, trackNo = 0, libPos = 0, updatedAt = System.currentTimeMillis()))

    suspend fun createAlbum(name: String, gradient: Int = 0): Long =
        dao.insertAlbum(AlbumEntity(name = name.trim().ifEmpty { "Untitled album" }, gradient = gradient))

    suspend fun updateAlbum(album: AlbumEntity) = dao.updateAlbum(album)

    suspend fun deleteAlbum(id: Long) {
        dao.deleteAlbum(id)
        dao.pruneGroups()
    }

    // ---- groups (trilogies, collections) --------------------------------------------------
    /** Combines [albumIds] into a new group. The group starts with the first album's artwork. */
    suspend fun createGroup(name: String, albumIds: List<Long>): Long {
        val first = dao.allAlbums().firstOrNull { it.id in albumIds }
        val id = dao.insertGroup(
            GroupEntity(
                name = name.trim().ifEmpty { "Group" },
                gradient = first?.gradient ?: 0
            )
        )
        if (albumIds.isNotEmpty()) dao.setAlbumsGroup(albumIds, id)
        dao.pruneGroups()
        return id
    }

    suspend fun updateGroup(group: GroupEntity) = dao.updateGroup(group)

    /** Albums stay in the library; only the group is removed. */
    suspend fun deleteGroup(id: Long) {
        dao.clearGroup(id)
        dao.deleteGroup(id)
    }

    suspend fun setGroupMembers(groupId: Long, albumIds: List<Long>) {
        dao.clearGroup(groupId)
        if (albumIds.isNotEmpty()) dao.setAlbumsGroup(albumIds, groupId)
        dao.pruneGroups()
    }

    // ---- settings -------------------------------------------------------------------------
    suspend fun setTheme(v: ThemeMode) = store.setTheme(v)
    suspend fun setAccent(v: Int) = store.setAccent(v)
    suspend fun setNavRaised(v: Boolean) = store.setNavRaised(v)
    suspend fun setReading(l: Int, c: Int, s: Float, offset: Int, section: Int) = store.setReading(l, c, s, offset, section)
    suspend fun setShowChords(v: Boolean) = store.setShowChords(v)
    suspend fun setShowTabs(v: Boolean) = store.setShowTabs(v)
    suspend fun setAutoScroll(v: Boolean) = store.setAutoScroll(v)
    suspend fun setLastBackup(v: Long) = store.setLastBackup(v)
    suspend fun setUserName(v: String) = store.setUserName(v)

    /** For the many small settings (greeting, grid, look...): `editSettings { setSongsGrid(true) }`. */
    suspend fun editSettings(block: suspend SettingsStore.() -> Unit) = store.block()

    // ---- backup ---------------------------------------------------------------------------
    /** Library only, no pictures (.json). */
    suspend fun backupJson(): String =
        Backup.toJson(dao.allAlbums(), dao.allSongs(), dao.allGroups(), store.flow.first(), embedImages = false) { null }

    /** Library, settings and every picture as its own file (.zip). */
    suspend fun backupZip(): ByteArray =
        Backup.toZip(dao.allAlbums(), dao.allSongs(), dao.allGroups(), store.flow.first()) { ImageStore.readBytes(context, it) }

    fun openBackup(bytes: ByteArray): BackupFile? = Backup.open(bytes)

    suspend fun exportText(): String = Backup.toText(dao.allAlbums(), dao.allSongs())

    /** Looks inside a backup (any version) without changing anything. Null when it is not a Songbook backup. */
    fun previewBackup(file: BackupFile): BackupInfo? {
        if (!file.json.trimStart().startsWith("{")) return null
        return runCatching {
            val p = Backup.fromJson(file)
            if (p.songs.isEmpty() && p.albums.isEmpty() && p.groups.isEmpty()) return null
            BackupInfo(
                version = p.version,
                songs = p.songs.size,
                albums = p.albums.size,
                groups = p.groups.size,
                hasSettings = p.settings != null,
                ideas = p.songs.count { it.stage == Stage.IDEA },
                images = p.images.size
            )
        }.getOrNull()
    }

    /**
     * Restores a backup of any version into this one.
     * [replace] = false adds what is missing (songs with the same title and text, albums and groups
     * with the same name are skipped); true wipes the library first.
     */
    suspend fun restoreBackup(file: BackupFile, replace: Boolean, withSettings: Boolean): RestoreResult {
        val parsed = Backup.fromJson(file)
        if (replace) {
            dao.deleteAllSongs()
            dao.deleteAllAlbums()
            dao.deleteAllGroups()
        }

        // Images inside the backup are copied into app storage under fresh names.
        val imageMap = HashMap<String, String?>()
        fun remap(name: String?): String? {
            if (name == null) return null
            return imageMap.getOrPut(name) { parsed.images[name]?.let { ImageStore.saveBytes(context, it) } }
        }

        var skipped = 0
        var addedGroups = 0
        var addedAlbums = 0
        var addedSongs = 0

        val existingGroups = dao.allGroups().associateBy { it.name.trim().lowercase() }
        val groupMap = HashMap<Long, Long>()
        for (g in parsed.groups) {
            val found = existingGroups[g.name.trim().lowercase()]
            if (found != null) {
                groupMap[g.id] = found.id
                skipped++
            } else {
                groupMap[g.id] = dao.insertGroup(g.copy(id = 0, image = remap(g.image)))
                addedGroups++
            }
        }

        val existingAlbums = dao.allAlbums().associateBy { it.name.trim().lowercase() }
        val albumMap = HashMap<Long, Long>()
        for (a in parsed.albums) {
            val found = existingAlbums[a.name.trim().lowercase()]
            if (found != null) {
                albumMap[a.id] = found.id
                skipped++
            } else {
                albumMap[a.id] = dao.insertAlbum(a.copy(id = 0, image = remap(a.image), groupId = a.groupId?.let { groupMap[it] }))
                addedAlbums++
            }
        }

        val existingSongs = dao.allSongs().map { it.title.trim().lowercase() + "\u0000" + it.body.trim() }.toHashSet()
        for (s in parsed.songs) {
            val key = s.title.trim().lowercase() + "\u0000" + s.body.trim()
            if (!existingSongs.add(key)) {
                skipped++
                continue
            }
            dao.insertSong(s.copy(id = 0, albumId = s.albumId?.let { albumMap[it] }, image = remap(s.image)))
            addedSongs++
        }
        dao.pruneGroups()
        if (withSettings) parsed.settings?.let { store.restore(it) }
        // restored songs that carry the very same picture as their album follow the album again
        adoptSameCovers(force = true)
        return RestoreResult(addedSongs, addedAlbums, addedGroups, skipped)
    }

    /** Imports a JSON backup (merge) or a plain text file as one song. Returns the number of songs added. */
    suspend fun importData(text: String, fileName: String?): Int {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) return restoreBackup(BackupFile(trimmed, emptyMap()), replace = false, withSettings = false).songs
        val title = fileName?.substringBeforeLast('.')?.ifBlank { null } ?: "Imported song"
        dao.insertSong(SongEntity(title = title, body = text))
        return 1
    }

    // ---- order, sides, covers ------------------------------------------------------------
    /** Saves the order you dragged the song list into (see Sorting.kt, "My order"). */
    suspend fun arrangeSongs(ids: List<Long>) = ids.forEachIndexed { i, id -> dao.setLibPos(id, i + 1) }

    /** Saves the order of the songs inside an album (or inside one of its sides). */
    suspend fun arrangeTracks(ids: List<Long>) = ids.forEachIndexed { i, id -> dao.setTrackNo(id, i + 1) }

    suspend fun arrangeAlbums(ids: List<Long>) = ids.forEachIndexed { i, id -> dao.setAlbumPos(id, i + 1) }

    suspend fun arrangeGroups(ids: List<Long>) = ids.forEachIndexed { i, id -> dao.setGroupPos(id, i + 1) }

    suspend fun setSongSide(songId: Long, side: Int) = dao.setSongSide(songId, side)

    suspend fun setAlbumSides(album: AlbumEntity, names: List<String>) =
        dao.updateAlbum(album.copy(sides = names.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")))

    /** Removes one side; its songs join the first side. */
    suspend fun removeAlbumSide(album: AlbumEntity, index: Int) {
        val names = album.sideList.toMutableList()
        if (index !in names.indices) return
        dao.moveSideToFirst(album.id, index)
        dao.shiftSidesDown(album.id, index)
        names.removeAt(index)
        dao.updateAlbum(album.copy(sides = names.joinToString("\n")))
    }

    /** All songs of the album show the album's cover again. */
    suspend fun followAlbumCover(albumId: Long) = dao.followAlbumCover(albumId)

    /**
     * One time, after the 2.5 update: a song whose own picture is exactly the album's picture is switched
     * to follow the album, so changing the album cover later changes the song too.
     */
    suspend fun adoptSameCovers(force: Boolean = false) {
        if (!force && store.flow.first().coversAdopted) return
        val albums = dao.allAlbums().associateBy { it.id }
        for (song in dao.allSongs()) {
            val own = song.image ?: continue
            val albumImage = song.albumId?.let { albums[it] }?.image ?: continue
            val x = ImageStore.readBytes(context, own)
            val y = ImageStore.readBytes(context, albumImage)
            if (song.coverManual && x != null && y != null && x.contentEquals(y)) {
                dao.updateSong(song.copy(coverManual = false, image = null))
            }
        }
        store.setCoversAdopted(true)
    }

    /** Removes cover files nobody points at (replaced or deleted artwork). */
    suspend fun cleanupImages() {
        val used = HashSet<String>()
        dao.allSongs().forEach { s -> s.image?.let { used.add(it) } }
        dao.allAlbums().forEach { a -> a.image?.let { used.add(it) } }
        dao.allGroups().forEach { g -> g.image?.let { used.add(it) } }
        ImageStore.cleanup(context, used)
    }

    // ---- first run sample data ------------------------------------------------------------
    suspend fun seedIfNeeded() {
        val s = store.flow.first()
        if (s.seeded) return
        if (dao.albumCount() == 0 && dao.songCount() == 0) {
            dao.insertAlbum(AlbumEntity(name = "Get Back", gradient = 1, year = 2026))
            dao.insertAlbum(AlbumEntity(name = "Songbook Vol. I", gradient = 0, year = 2026, typeName = "EP"))
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
                    body = SAMPLE_BODY,
                    stage = 1,
                    note = "Write the second verse",
                    rating = 4.25f,
                    typeName = "Demo"
                )
            )
            dao.insertSong(
                SongEntity(
                    title = "Midnight Highway",
                    gradient = 2,
                    stage = Stage.IDEA,
                    note = "Find the melody for the chorus",
                    idea = SAMPLE_IDEA.toJson()
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

        val SAMPLE_IDEA = IdeaData(
            spark = "A song about driving through the night with no destination.",
            lines = listOf(
                IdeaLine("The white lines count the miles I lost", "Hook"),
                IdeaLine("Radio static singing my name", "Verse")
            ),
            tasks = listOf(
                IdeaTask("Hum a melody into the voice memo app"),
                IdeaTask("Pick a key", true)
            ),
            fields = listOf(IdeaField("Key", "Am"), IdeaField("Mood", "Restless"))
        )
    }
}
