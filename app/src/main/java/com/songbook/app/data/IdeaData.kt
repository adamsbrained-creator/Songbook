package com.songbook.app.data

import org.json.JSONArray
import org.json.JSONObject

/** One scrap of lyric: a line, optionally tagged ("Hook", "Verse"...). */
data class IdeaLine(val text: String, val tag: String = "")

data class IdeaTask(val text: String, val done: Boolean = false)

/** A free-form detail the user names themselves: "Key: Em", "Tempo: 92", "Reference: ...". */
data class IdeaField(val name: String, val value: String)

/**
 * Everything the songwriting desk keeps next to a song that is still being written.
 * Stored as JSON in [SongEntity.idea].
 */
data class IdeaData(
    val spark: String = "",
    val lines: List<IdeaLine> = emptyList(),
    val tasks: List<IdeaTask> = emptyList(),
    val fields: List<IdeaField> = emptyList()
) {
    val isEmpty: Boolean
        get() = spark.isBlank() && lines.isEmpty() && tasks.isEmpty() && fields.isEmpty()

    val openTasks: Int get() = tasks.count { !it.done }

    fun toJson(): String {
        if (isEmpty) return ""
        val o = JSONObject()
        o.put("spark", spark)
        o.put("lines", JSONArray().also { a -> lines.forEach { a.put(JSONObject().put("t", it.text).put("g", it.tag)) } })
        o.put("tasks", JSONArray().also { a -> tasks.forEach { a.put(JSONObject().put("t", it.text).put("d", it.done)) } })
        o.put("fields", JSONArray().also { a -> fields.forEach { a.put(JSONObject().put("n", it.name).put("v", it.value)) } })
        return o.toString()
    }

    companion object {
        val SuggestedFields = listOf("Key", "Tempo", "Mood", "Theme", "Reference", "Instrument")
        val LineTags = listOf("Hook", "Verse", "Chorus", "Bridge")

        fun parse(json: String): IdeaData {
            if (json.isBlank()) return IdeaData()
            return runCatching {
                val o = JSONObject(json)
                fun <T> list(key: String, f: (JSONObject) -> T): List<T> {
                    val a = o.optJSONArray(key) ?: return emptyList()
                    return (0 until a.length()).map { f(a.getJSONObject(it)) }
                }
                IdeaData(
                    spark = o.optString("spark"),
                    lines = list("lines") { IdeaLine(it.optString("t"), it.optString("g")) },
                    tasks = list("tasks") { IdeaTask(it.optString("t"), it.optBoolean("d")) },
                    fields = list("fields") { IdeaField(it.optString("n"), it.optString("v")) }
                )
            }.getOrDefault(IdeaData())
        }
    }
}

val SongEntity.ideaData: IdeaData get() = IdeaData.parse(idea)
