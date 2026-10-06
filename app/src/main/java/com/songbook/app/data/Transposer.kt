package com.songbook.app.data

/**
 * Capo support. Chords in a song are written as they sound. With a capo on fret N the player uses
 * shapes that are N semitones lower (capo 2: Bm becomes Am, G becomes F). Tab numbers drop by N too.
 */
object Transposer {
    private val sharps = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val flats = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
    private val natural = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)
    private val chordRx = Regex("""^([A-G])([#b]?)([^/]*)(?:/([A-G])([#b]?))?$""")

    private fun note(letter: Char, accidental: String, shift: Int): String {
        var i = natural.getValue(letter)
        if (accidental == "#") i += 1
        if (accidental == "b") i -= 1
        val n = (((i + shift) % 12) + 12) % 12
        // keep the spelling style of the original chord: sharps stay sharps, everything else uses flats
        return if (accidental == "#") sharps[n] else flats[n]
    }

    /** [semitones] > 0 moves up, < 0 down. Anything that is not a chord ("N.C.", "x2") is returned as it is. */
    fun chord(name: String, semitones: Int): String {
        if (semitones == 0) return name
        val m = chordRx.matchEntire(name.trim()) ?: return name
        val (root, acc, quality, bassLetter, bassAcc) = m.destructured
        val newRoot = note(root[0], acc, semitones)
        val bass = if (bassLetter.isNotEmpty()) "/" + note(bassLetter[0], bassAcc, semitones) else ""
        return newRoot + quality + bass
    }

    private val fretRx = Regex("""\d+""")

    /**
     * Lowers the fret numbers of a tab line by [capo]. Numbers below the capo cannot be lowered and
     * stay as they are. Numbers that get shorter are padded with "-" so the columns stay aligned.
     */
    fun tabLine(line: String, capo: Int): String {
        if (capo <= 0) return line
        val bar = line.indexOf('|')
        if (bar < 0) return line
        val head = line.substring(0, bar + 1)
        val body = line.substring(bar + 1)
        val out = fretRx.replace(body) { m ->
            val n = m.value.toIntOrNull() ?: return@replace m.value
            if (n < capo) m.value else n.minus(capo).toString().padStart(m.value.length, '-')
        }
        return head + out
    }
}
