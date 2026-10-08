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
    private val spaces = Regex("""\s+""")

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
            // "[G Am Dm]" holds three chords on the same spot: they are laid out next to each other
            m.groupValues[1].trim().split(spaces).filter { it.isNotEmpty() }
                .forEach { chords.add(ChordMark(plain.length, it)) }
            last = m.range.last + 1
        }
        plain.append(raw, last, raw.length)
        return SongLine.Lyric(plain.toString(), chords)
    }
}

/** Writes a parsed line back as text: every chord becomes "[Chord]" in front of its letter. */
fun SongParser.serialize(line: SongLine.Lyric): String {
    val sb = StringBuilder()
    var pos = 0
    for (m in line.chords.sortedBy { it.offset }) {
        val o = m.offset.coerceIn(pos, line.text.length)
        sb.append(line.text, pos, o)
        pos = o
        sb.append('[').append(m.name).append(']')
    }
    sb.append(line.text, pos, line.text.length)
    return sb.toString()
}

/** The word around [index] (or the next word when it points at a space): its start and end in [text]. */
fun SongParser.wordAt(text: String, index: Int): IntRange? {
    if (text.isBlank()) return null
    var i = index.coerceIn(0, text.length - 1)
    if (text[i].isWhitespace()) {
        var j = i
        while (j < text.length && text[j].isWhitespace()) j++
        if (j < text.length) i = j else {
            j = i
            while (j > 0 && text[j].isWhitespace()) j--
            i = j
        }
    }
    var a = i
    while (a > 0 && !text[a - 1].isWhitespace()) a--
    var b = i
    while (b < text.length && !text[b].isWhitespace()) b++
    return a until b
}

/** Replaces every chord inside [word] by [names], placed on the first letter of the word. */
fun SongParser.withChordsAt(line: SongLine.Lyric, word: IntRange, names: List<String>): SongLine.Lyric {
    val kept = line.chords.filter { it.offset < word.first || it.offset > word.last }
    val added = names.map { ChordMark(word.first, it) }
    return line.copy(chords = (kept + added).sortedBy { it.offset })
}

/** [body] with line number [index] replaced by [newLine]. */
fun SongParser.replaceLine(body: String, index: Int, newLine: String): String {
    val lines = body.lines().toMutableList()
    if (index !in lines.indices) return body
    lines[index] = newLine
    return lines.joinToString("\n")
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
