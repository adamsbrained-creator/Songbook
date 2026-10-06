package com.songbook.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object Backup {
    class Parsed(
        /** Backup format: 1 (Songbook 1.0), 2 (1.5) or 3 (2.0). */
        val version: Int,
        val albums: List<AlbumEntity>,
        val songs: List<SongEntity>,
        val groups: List<GroupEntity>,
        /** Saved app settings (backups from 2.0 on), null when the file has none. */
        val settings: JSONObject?,
        /** Cover image bytes keyed by the file name used inside the backup. */
        val images: Map<String, ByteArray>
    )

    fun toJson(
        albums: List<AlbumEntity>,
        songs: List<SongEntity>,
        groups: List<GroupEntity>,
        settings: AppSettings?,
        imageBytes: (String) -> ByteArray?
    ): String {
        val images = JSONObject()
        fun withImage(o: JSONObject, name: String?): JSONObject {
            if (name == null) return o.put("image", JSONObject.NULL)
            o.put("image", name)
            if (!images.has(name)) {
                imageBytes(name)?.let { images.put(name, Base64.encodeToString(it, Base64.NO_WRAP)) }
            }
            return o
        }

        val root = JSONObject()
        root.put("version", 3)
        if (settings != null) root.put("settings", settingsJson(settings))
        root.put("groups", JSONArray().also { arr ->
            groups.forEach {
                arr.put(
                    withImage(
                        JSONObject().put("id", it.id).put("name", it.name).put("gradient", it.gradient)
                            .put("badge", it.badge).put("badgeColor", it.badgeColor).put("createdAt", it.createdAt)
                            .put("rating", it.rating.toDouble()).put("typeName", it.typeName),
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
                            .put("rating", it.rating.toDouble()).put("typeName", it.typeName),
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
                            .put("idea", it.idea).put("capoAdjust", it.capoAdjust),
                        it.image
                    )
                )
            }
        })
        root.put("images", images)
        return root.toString(2)
    }

    fun settingsJson(s: AppSettings): JSONObject = JSONObject()
        .put("userName", s.userName).put("greeting", s.greeting).put("greetingCustom", s.greetingCustom)
        .put("theme", s.theme.name).put("accent", s.accent).put("accentCustom", s.accentCustom)
        .put("uiStyle", s.uiStyle).put("topAlbums", s.topAlbums).put("cardFace", s.cardFace)
        .put("navRaised", s.navRaised).put("lyricsSize", s.lyricsSize).put("chordSize", s.chordSize)
        .put("lineSpacing", s.lineSpacing.toDouble()).put("chordOffset", s.chordOffset)
        .put("sectionStyle", s.sectionStyle).put("showChords", s.showChords).put("showTabs", s.showTabs)
        .put("scrollSpeed", s.scrollSpeed).put("showEditButton", s.showEditButton)
        .put("songTheme", s.songTheme).put("gridColumns", s.gridColumns).put("songsGrid", s.songsGrid)

    /** Reads backups from every version: 1 (1.0, no images / groups), 2 (1.5) and 3 (2.0). */
    fun fromJson(json: String): Parsed {
        val root = JSONObject(json)
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
                rating = o.optDouble("rating", 0.0).toFloat(), typeName = o.optString("typeName")
            )
        }
        val a = (0 until albums.length()).map { i ->
            val o = albums.getJSONObject(i)
            AlbumEntity(
                id = o.optLong("id"), name = o.optString("name"), gradient = o.optInt("gradient"),
                year = o.optInt("year", java.time.Year.now().value), createdAt = o.optLong("createdAt"),
                image = imageOf(o), badge = o.optString("badge"), badgeColor = o.optInt("badgeColor"),
                groupId = if (o.isNull("groupId")) null else o.optLong("groupId"),
                rating = o.optDouble("rating", 0.0).toFloat(), typeName = o.optString("typeName")
            )
        }
        val s = (0 until songs.length()).map { i ->
            val o = songs.getJSONObject(i)
            SongEntity(
                id = o.optLong("id"), title = o.optString("title"),
                albumId = if (o.isNull("albumId")) null else o.optLong("albumId"),
                genre = o.optString("genre"), tags = o.optString("tags"), gradient = o.optInt("gradient"),
                body = o.optString("body"), capo = o.optInt("capo"), favorite = o.optBoolean("favorite"),
                updatedAt = o.optLong("updatedAt"),
                image = imageOf(o), badge = o.optString("badge"), badgeColor = o.optInt("badgeColor"),
                stage = o.optInt("stage", Stage.DONE).coerceIn(0, Stage.DONE), note = o.optString("note"),
                rating = o.optDouble("rating", 0.0).toFloat(), typeName = o.optString("typeName"),
                idea = o.optString("idea"),
                // older backups were written with chords as shapes, so they are shown as typed
                capoAdjust = o.optBoolean("capoAdjust", false)
            )
        }
        val images = HashMap<String, ByteArray>()
        for (key in imagesObj.keys()) {
            runCatching { Base64.decode(imagesObj.getString(key), Base64.NO_WRAP) }
                .getOrNull()?.let { images[key] = it }
        }
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

fun Context.readText(uri: Uri): String? =
    runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()

fun Context.writeText(uri: Uri, text: String): Boolean =
    runCatching { contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) } != null }
        .getOrDefault(false)

fun Context.displayName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
