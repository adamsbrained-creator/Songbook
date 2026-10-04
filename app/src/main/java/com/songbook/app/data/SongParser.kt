package com.songbook.app.data

/** A chord placed above the lyric text, [offset] is a character index into the plain lyric text. */
data class ChordMark(val offset: Int, val name: String)

sealed interface SongLine {
    /** [label] is kept exactly as typed; the reader applies the user's upper/lower-case setting. */
    data class Section(val label: String) : SongLine
    data class Lyric(val text: String, val chords: List<ChordMark>) : SongLine
    data class Tab(val text: String) : SongLine
    data object Blank : SongLine
}

object SongParser {
    private val chordRx = Regex("""\[([^\]\n]*)]""")

    fun isTabLine(line: String): Boolean {
        val t = line.trimStart()
        return t.contains('|') && t.count { it == '-' } >= 3 && !t.contains('[')
    }

    fun hasChords(body: String): Boolean = chordRx.containsMatchIn(body)

    fun hasTabs(body: String): Boolean = body.lineSequence().any { isTabLine(it) }

    fun parse(body: String): List<SongLine> = body.lines().map { raw ->
        val trimmed = raw.trim()
        when {
            trimmed.isEmpty() -> SongLine.Blank
            trimmed.startsWith("#") -> SongLine.Section(trimmed.trimStart('#').trim())
            isTabLine(raw) -> SongLine.Tab(raw.trimEnd())
            else -> parseLyric(raw)
        }
    }

    private fun parseLyric(raw: String): SongLine.Lyric {
        val plain = StringBuilder()
        val chords = ArrayList<ChordMark>()
        var last = 0
        for (m in chordRx.findAll(raw)) {
            plain.append(raw, last, m.range.first)
            val name = m.groupValues[1].trim()
            if (name.isNotEmpty()) chords.add(ChordMark(plain.length, name))
            last = m.range.last + 1
        }
        plain.append(raw, last, raw.length)
        return SongLine.Lyric(plain.toString(), chords)
    }
}

fun SongEntity.hasChords() = SongParser.hasChords(body)
fun SongEntity.hasTabs() = SongParser.hasTabs(body)

val SongEntity.tagList: List<String>
    get() = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

/** "Lyrics · Guitar", "Lyrics · Tab", ... as shown under the song title in the mockup. */
fun SongEntity.kindLabel(): String {
    val parts = mutableListOf("Lyrics")
    if (hasChords()) parts += "Guitar"
    if (hasTabs()) parts += "Tab"
    return parts.joinToString(" · ")
}
