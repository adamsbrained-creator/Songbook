package com.songbook.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject

object Backup {
    fun toJson(albums: List<AlbumEntity>, songs: List<SongEntity>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("albums", JSONArray().also { arr ->
            albums.forEach {
                arr.put(JSONObject().put("id", it.id).put("name", it.name).put("gradient", it.gradient)
                    .put("year", it.year).put("createdAt", it.createdAt))
            }
        })
        root.put("songs", JSONArray().also { arr ->
            songs.forEach {
                arr.put(
                    JSONObject().put("id", it.id).put("title", it.title)
                        .put("albumId", it.albumId ?: JSONObject.NULL)
                        .put("genre", it.genre).put("tags", it.tags).put("gradient", it.gradient)
                        .put("body", it.body).put("capo", it.capo).put("favorite", it.favorite)
                        .put("updatedAt", it.updatedAt)
                )
            }
        })
        return root.toString(2)
    }

    fun fromJson(json: String): Pair<List<AlbumEntity>, List<SongEntity>> {
        val root = JSONObject(json)
        val albums = root.optJSONArray("albums") ?: JSONArray()
        val songs = root.optJSONArray("songs") ?: JSONArray()
        val a = (0 until albums.length()).map { i ->
            val o = albums.getJSONObject(i)
            AlbumEntity(
                id = o.optLong("id"), name = o.optString("name"), gradient = o.optInt("gradient"),
                year = o.optInt("year", java.time.Year.now().value), createdAt = o.optLong("createdAt")
            )
        }
        val s = (0 until songs.length()).map { i ->
            val o = songs.getJSONObject(i)
            SongEntity(
                id = o.optLong("id"), title = o.optString("title"),
                albumId = if (o.isNull("albumId")) null else o.optLong("albumId"),
                genre = o.optString("genre"), tags = o.optString("tags"), gradient = o.optInt("gradient"),
                body = o.optString("body"), capo = o.optInt("capo"), favorite = o.optBoolean("favorite"),
                updatedAt = o.optLong("updatedAt")
            )
        }
        return a to s
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
