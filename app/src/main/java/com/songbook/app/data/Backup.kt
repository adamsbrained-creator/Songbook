package com.songbook.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A backup file that was opened: the library as JSON plus the pictures that came as separate files (.zip). */
class BackupFile(val json: String, val extraImages: Map<String, ByteArray>)

object Backup {
    class Parsed(
        /** Backup format: 1 (Songbook 1.0), 2 (1.5), 3 (2.0) or 4 (2.5). */
        val version: Int,
        val albums: List<AlbumEntity>,
        val songs: List<SongEntity>,
        val groups: List<GroupEntity>,
        /** Saved app settings (backups from 2.0 on), null when the file has none. */
        val settings: JSONObject?,
        /** Cover image bytes keyed by the file name used inside the backup. */
        val images: Map<String, ByteArray>
    )

    /**
     * The whole library as JSON. [embedImages] puts the pictures inside the JSON as text (the way 1.5 and 2.0 did);
     * without it only the file names are saved, which is what the .zip backup uses (it stores the pictures as files).
     */
    fun toJson(
        albums: List<AlbumEntity>,
        songs: List<SongEntity>,
        groups: List<GroupEntity>,
        settings: AppSettings?,
        embedImages: Boolean,
        imageBytes: (String) -> ByteArray?
    ): String {
        val images = JSONObject()
        fun withImage(o: JSONObject, name: String?): JSONObject {
            if (name == null) return o.put("image", JSONObject.NULL)
            o.put("image", name)
            if (embedImages && !images.has(name)) {
                imageBytes(name)?.let { images.put(name, Base64.encodeToString(it, Base64.NO_WRAP)) }
            }
            return o
        }

        val root = JSONObject()
        root.put("version", 4)
        if (settings != null) root.put("settings", settingsJson(settings))
        root.put("groups", JSONArray().also { arr ->
            groups.forEach {
                arr.put(
                    withImage(
                        JSONObject().put("id", it.id).put("name", it.name).put("gradient", it.gradient)
                            .put("badge", it.badge).put("badgeColor", it.badgeColor).put("createdAt", it.createdAt)
                            .put("rating", it.rating.toDouble()).put("typeName", it.typeName).put("position", it.position),
                        it.image
                    )
                )
            }
        })
        root.put("albums", JSONArray().also { arr ->
            albums.forEach {
                arr.put(
                    withImage(
                        JSONObject().put("id", it.id).put("name", it.name).put("gradient", it.gradient)
                            .put("year", it.year).put("createdAt", it.createdAt)
                            .put("badge", it.badge).put("badgeColor", it.badgeColor)
                            .put("groupId", it.groupId ?: JSONObject.NULL)
                            .put("rating", it.rating.toDouble()).put("typeName", it.typeName)
                            .put("sides", it.sides).put("position", it.position),
                        it.image
                    )
                )
            }
        })
        root.put("songs", JSONArray().also { arr ->
            songs.forEach {
                arr.put(
                    withImage(
                        JSONObject().put("id", it.id).put("title", it.title)
                            .put("albumId", it.albumId ?: JSONObject.NULL)
                            .put("genre", it.genre).put("tags", it.tags).put("gradient", it.gradient)
                            .put("body", it.body).put("capo", it.capo).put("favorite", it.favorite)
                            .put("updatedAt", it.updatedAt)
                            .put("badge", it.badge).put("badgeColor", it.badgeColor)
                            .put("stage", it.stage).put("note", it.note)
                            .put("rating", it.rating.toDouble()).put("typeName", it.typeName)
                            .put("idea", it.idea).put("capoAdjust", it.capoAdjust)
                            .put("coverManual", it.coverManual).put("side", it.side)
                            .put("trackNo", it.trackNo).put("libPos", it.libPos),
                        it.image
                    )
                )
            }
        })
        if (embedImages) root.put("images", images)
        return root.toString(2)
    }

    /** Library plus every picture as its own file: songbook-backup.zip. */
    fun toZip(
        albums: List<AlbumEntity>,
        songs: List<SongEntity>,
        groups: List<GroupEntity>,
        settings: AppSettings?,
        imageBytes: (String) -> ByteArray?
    ): ByteArray {
        val names = LinkedHashSet<String>()
        albums.forEach { a -> a.image?.let { names.add(it) } }
        songs.forEach { s -> s.image?.let { names.add(it) } }
        groups.forEach { g -> g.image?.let { names.add(it) } }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("library.json"))
            zip.write(toJson(albums, songs, groups, settings, embedImages = false, imageBytes = imageBytes).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (n in names) {
                val bytes = imageBytes(n) ?: continue
                zip.putNextEntry(ZipEntry("images/$n"))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** Opens a backup chosen by the user: a .zip (library.json + images/) or a plain / old .json file. Null if neither. */
    fun open(bytes: ByteArray): BackupFile? {
        if (bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            var json: String? = null
            val images = HashMap<String, ByteArray>()
            runCatching {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                    while (true) {
                        val e = zip.nextEntry ?: break
                        if (e.isDirectory) continue
                        val data = zip.readBytes()
                        when {
                            e.name == "library.json" -> json = String(data, Charsets.UTF_8)
                            e.name.startsWith("images/") -> images[e.name.removePrefix("images/")] = data
                        }
                    }
                }
            }
            val j = json ?: return null
            return BackupFile(j, images)
        }
        val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull() ?: return null
        return BackupFile(text, emptyMap())
    }

    fun settingsJson(s: AppSettings): JSONObject = JSONObject()
        .put("userName", s.userName).put("greeting", s.greeting).put("greetingCustom", s.greetingCustom)
        .put("theme", s.theme.name).put("accent", s.accent).put("accentCustom", s.accentCustom)
        .put("uiStyle", s.uiStyle).put("topAlbums", s.topAlbums).put("cardFace", s.cardFace)
        .put("navRaised", s.navRaised).put("lyricsSize", s.lyricsSize).put("chordSize", s.chordSize)
        .put("lineSpacing", s.lineSpacing.toDouble()).put("chordOffset", s.chordOffset)
        .put("sectionStyle", s.sectionStyle).put("sectionSize", s.sectionSize)
        .put("showChords", s.showChords).put("showTabs", s.showTabs)
        .put("scrollSpeed", s.scrollSpeed).put("showEditButton", s.showEditButton)
        .put("songTheme", s.songTheme)
        .put("songsLayout", s.songsLayout).put("albumColumns", s.albumColumns).put("groupColumns", s.groupColumns)
        .put("trackLayout", s.trackLayout).put("favLayout", s.favLayout)
        .put("songSort", s.songSort).put("albumSort", s.albumSort).put("trackSort", s.trackSort)
        .put("groupsTitle", s.groupsTitle).put("homeLayout", s.homeLayout).put("albumsLayout", s.albumsLayout)

    /** Reads backups from every version: 1 (1.0), 2 (1.5), 3 (2.0) and 4 (2.5). */
    fun fromJson(file: BackupFile): Parsed {
        val root = JSONObject(file.json.trim())
        val albums = root.optJSONArray("albums") ?: JSONArray()
        val songs = root.optJSONArray("songs") ?: JSONArray()
        val groups = root.optJSONArray("groups") ?: JSONArray()
        val imagesObj = root.optJSONObject("images") ?: JSONObject()

        fun imageOf(o: JSONObject): String? = if (o.isNull("image")) null else o.optString("image").ifEmpty { null }

        val g = (0 until groups.length()).map { i ->
            val o = groups.getJSONObject(i)
            GroupEntity(
                id = o.optLong("id"), name = o.optString("name"), gradient = o.optInt("gradient"),
                image = imageOf(o), badge = o.optString("badge"), badgeColor = o.optInt("badgeColor"),
                createdAt = o.optLong("createdAt"),
                rating = o.optDouble("rating", 0.0).toFloat(), typeName = o.optString("typeName"),
                position = o.optInt("position")
            )
        }
        val a = (0 until albums.length()).map { i ->
            val o = albums.getJSONObject(i)
            AlbumEntity(
                id = o.optLong("id"), name = o.optString("name"), gradient = o.optInt("gradient"),
                year = o.optInt("year", java.time.Year.now().value), createdAt = o.optLong("createdAt"),
                image = imageOf(o), badge = o.optString("badge"), badgeColor = o.optInt("badgeColor"),
                groupId = if (o.isNull("groupId")) null else o.optLong("groupId"),
                rating = o.optDouble("rating", 0.0).toFloat(), typeName = o.optString("typeName"),
                sides = o.optString("sides"), position = o.optInt("position")
            )
        }
        val s = (0 until songs.length()).map { i ->
            val o = songs.getJSONObject(i)
            val img = imageOf(o)
            SongEntity(
                id = o.optLong("id"), title = o.optString("title"),
                albumId = if (o.isNull("albumId")) null else o.optLong("albumId"),
                genre = o.optString("genre"), tags = o.optString("tags"), gradient = o.optInt("gradient"),
                body = o.optString("body"), capo = o.optInt("capo"), favorite = o.optBoolean("favorite"),
                updatedAt = o.optLong("updatedAt"),
                image = img, badge = o.optString("badge"), badgeColor = o.optInt("badgeColor"),
                stage = o.optInt("stage", Stage.DONE).coerceIn(0, Stage.DONE), note = o.optString("note"),
                rating = o.optDouble("rating", 0.0).toFloat(), typeName = o.optString("typeName"),
                idea = o.optString("idea"),
                // older backups were written with chords as shapes, so they are shown as typed
                capoAdjust = o.optBoolean("capoAdjust", false),
                // older backups: a song with its own picture keeps it
                coverManual = o.optBoolean("coverManual", img != null),
                side = o.optInt("side"), trackNo = o.optInt("trackNo"), libPos = o.optInt("libPos")
            )
        }
        val images = HashMap<String, ByteArray>()
        for (key in imagesObj.keys()) {
            runCatching { Base64.decode(imagesObj.getString(key), Base64.NO_WRAP) }
                .getOrNull()?.let { images[key] = it }
        }
        images.putAll(file.extraImages)
        return Parsed(root.optInt("version", 1), a, s, g, root.optJSONObject("settings"), images)
    }

    fun toText(albums: List<AlbumEntity>, songs: List<SongEntity>): String {
        val names = albums.associate { it.id to it.name }
        return songs.joinToString("\n\n==========\n\n") { s ->
            buildString {
                appendLine(s.title)
                names[s.albumId]?.let { appendLine("Album: $it") }
                if (s.capo > 0) appendLine("Capo: ${s.capo}")
                appendLine()
                append(s.body)
            }
        }
    }
}

fun Context.readBytes(uri: Uri): ByteArray? =
    runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()

fun Context.readText(uri: Uri): String? =
    runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()

fun Context.writeText(uri: Uri, text: String): Boolean = writeBytes(uri, text.toByteArray(Charsets.UTF_8))

fun Context.writeBytes(uri: Uri, bytes: ByteArray): Boolean =
    runCatching { contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null }.getOrDefault(false)

fun Context.displayName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
