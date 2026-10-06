package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.data.Stage
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.SongGradients
import com.songbook.app.ui.theme.sbStyle
import kotlin.math.roundToInt

// ---- generic dialogs -----------------------------------------------------------------------

@Composable
fun SbDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String = "Cancel",
    content: @Composable () -> Unit
) {
    val c = Sb.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.sheet,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, style = sbStyle(18, FontWeight.Bold, c.tx)) },
        text = { content() },
        confirmButton = {
            if (confirmText != null && onConfirm != null) {
                TextButton(onClick = onConfirm) { Text(confirmText, style = sbStyle(14, FontWeight.SemiBold, c.ac)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText, style = sbStyle(14, FontWeight.Medium, c.mu)) }
        }
    )
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmText: String = "Save",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    SbDialog(title, onDismiss, confirmText, { if (text.isNotBlank()) onConfirm(text.trim()) }) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(label) },
            singleLine = true,
            textStyle = sbStyle(15, color = Sb.colors.tx)
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SbDialog(title, onDismiss, confirmText, onConfirm) {
        Text(message, style = sbStyle(14, color = Sb.colors.mu))
    }
}

// ---- artwork -------------------------------------------------------------------------------

@Composable
fun GradientPicker(selected: Int, onSelect: (Int) -> Unit) {
    val c = Sb.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SongGradients.indices.forEach { i ->
            val on = i == selected
            Box(
                Modifier
                    .size(52.dp)
                    .then(if (on) Modifier.border(2.dp, c.ac, RoundedCornerShape(18.dp)) else Modifier)
                    .padding(4.dp)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) { SquareArtwork(i, 44.dp, 14.dp) }
        }
    }
}

/** Pick a gradient or upload your own picture. [onPick] gets the final gradient and image file (or null). */
@Composable
fun ArtworkDialog(gradient: Int, image: String?, onPick: (Int, String?) -> Unit, onDismiss: () -> Unit) {
    var g by remember { mutableIntStateOf(gradient) }
    var img by remember { mutableStateOf(image) }
    SbDialog("Artwork", onDismiss, "Done", { onPick(g, img) }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ArtworkEditor(g, img) { ng, ni -> g = ng; img = ni }
        }
    }
}

class CoverEdit(
    val name: String,
    /** null when the dialog was opened without a year field (groups). */
    val year: Int?,
    val gradient: Int,
    val image: String?,
    val badge: String,
    val badgeColor: Int,
    val rating: Float,
    val typeName: String
)

/** Edit dialog shared by albums and groups: name, (year), type, rating, badge, artwork / uploaded image. */
@Composable
fun EditCoverDialog(
    title: String,
    name0: String,
    year0: Int?,
    gradient0: Int,
    image0: String?,
    badge0: String,
    badgeColor0: Int,
    rating0: Float,
    typeName0: String,
    typePresets: List<String>,
    typeDefault: String,
    onDismiss: () -> Unit,
    onSave: (CoverEdit) -> Unit
) {
    var name by remember { mutableStateOf(name0) }
    var year by remember { mutableStateOf(year0?.toString() ?: "") }
    var gradient by remember { mutableIntStateOf(gradient0) }
    var image by remember { mutableStateOf(image0) }
    var badge by remember { mutableStateOf(badge0) }
    var badgeColor by remember { mutableIntStateOf(badgeColor0) }
    var rating by remember { mutableFloatStateOf(rating0) }
    var typeName by remember { mutableStateOf(typeName0) }
    var badgeOpen by remember { mutableStateOf(false) }
    var typeOpen by remember { mutableStateOf(false) }
    var ratingOpen by remember { mutableStateOf(false) }

    if (badgeOpen) {
        BadgeDialog(badge, badgeColor, { t, col -> badge = t; badgeColor = col; badgeOpen = false }, { badgeOpen = false })
    }
    if (typeOpen) {
        TypeDialog("Type", typePresets, typeName, typeDefault, { typeName = it; typeOpen = false }, { typeOpen = false })
    }
    if (ratingOpen) {
        RatingDialog("Rating", rating, { rating = it; ratingOpen = false }, { ratingOpen = false })
    }
    SbDialog(title, onDismiss, "Save", {
        if (name.isNotBlank()) {
            val y = if (year0 == null) null else (year.toIntOrNull() ?: year0)
            onSave(CoverEdit(name.trim(), y, gradient, image, badge, badgeColor, rating, typeName))
        }
    }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
            if (year0 != null) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter(Char::isDigit).take(4) },
                    label = { Text("Year") },
                    singleLine = true
                )
            }
            TypeRow(typeName, typeDefault) { typeOpen = true }
            RatingRow(rating) { ratingOpen = true }
            BadgeRow(badge, badgeColor, onClick = { badgeOpen = true })
            ArtworkEditor(gradient, image) { g, img -> gradient = g; image = img }
        }
    }
}

// ---- album picker --------------------------------------------------------------------------

@Composable
fun AlbumPickerDialog(
    albums: List<AlbumEntity>,
    selectedId: Long?,
    onPick: (Long?) -> Unit,
    onCreateAndPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        TextInputDialog(
            title = "New album",
            label = "Album name",
            confirmText = "Create",
            onConfirm = { onCreateAndPick(it) },
            onDismiss = { creating = false }
        )
        return
    }
    SbDialog("Choose album", onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            @Composable
            fun Option(label: String, on: Boolean, color: Color = c.tx, onClick: () -> Unit) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = sbStyle(15, if (on) FontWeight.SemiBold else FontWeight.Medium, if (on) c.ac else color))
                }
            }
            Option("No album", selectedId == null) { onPick(null) }
            albums.forEach { Option(it.name, it.id == selectedId) { onPick(it.id) } }
            Option("+ New album", false, c.ac) { creating = true }
        }
    }
}

// ---- reading dialog (Aa button and Settings) -----------------------------------------------

fun chordOffsetLabel(offset: Int): String = when {
    offset == 0 -> "Default"
    offset < 0 -> "Closer ${-offset}"
    else -> "Farther $offset"
}

/**
 * Font size, chord size, line spacing and chord distance. When [songTheme]/[onSongTheme] are given
 * (song page only) a "Song page" theme switch is shown as well.
 */
@Composable
fun ReadingDialog(
    lyricsSize: Int,
    chordSize: Int,
    lineSpacing: Float,
    chordOffset: Int,
    onCommit: (lyrics: Int, chord: Int, spacing: Float, offset: Int) -> Unit,
    onDismiss: () -> Unit,
    songTheme: Int? = null,
    onSongTheme: ((Int) -> Unit)? = null
) {
    val c = Sb.colors
    var l by remember { mutableIntStateOf(lyricsSize) }
    var ch by remember { mutableIntStateOf(chordSize) }
    var s by remember { mutableFloatStateOf(lineSpacing) }
    var off by remember { mutableIntStateOf(chordOffset) }
    var theme by remember { mutableIntStateOf(songTheme ?: 0) }
    val colors = SliderDefaults.colors(thumbColor = c.ac, activeTrackColor = c.ac, inactiveTrackColor = c.ln)

    SbDialog("Reading", onDismiss, "Done", { onDismiss() }, dismissText = "Close") {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Lyrics font size · $l pt", style = sbStyle(13, FontWeight.Medium, c.tx))
            Slider(
                value = l.toFloat(), onValueChange = { l = it.roundToInt() },
                onValueChangeFinished = { onCommit(l, ch, s, off) },
                valueRange = 12f..28f, steps = 15, colors = colors
            )
            Text("Chord size · $ch pt", style = sbStyle(13, FontWeight.Medium, c.tx))
            Slider(
                value = ch.toFloat(), onValueChange = { ch = it.roundToInt() },
                onValueChangeFinished = { onCommit(l, ch, s, off) },
                valueRange = 10f..22f, steps = 11, colors = colors
            )
            Text("Chord distance · ${chordOffsetLabel(off)}", style = sbStyle(13, FontWeight.Medium, c.tx))
            Slider(
                value = off.toFloat(), onValueChange = { off = it.roundToInt() },
                onValueChangeFinished = { onCommit(l, ch, s, off) },
                valueRange = -6f..16f, steps = 21, colors = colors
            )
            Text("Line spacing · ${"%.1f".format(s)}×", style = sbStyle(13, FontWeight.Medium, c.tx))
            Slider(
                value = s, onValueChange = { s = (it * 10).roundToInt() / 10f },
                onValueChangeFinished = { onCommit(l, ch, s, off) },
                valueRange = 1.0f..2.0f, steps = 9, colors = colors
            )
            if (songTheme != null && onSongTheme != null) {
                Text("Song page theme", style = sbStyle(13, FontWeight.Medium, c.tx), modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                Segmented(listOf("Match app", "Dark", "Light"), theme) { theme = it; onSongTheme(it) }
            }
        }
    }
}

/**
 * Capo fret and the "show chord shapes" switch. [sample] is a few chords of the song so the
 * preview can show what changes: "Bm G D  →  Am F C".
 */
@Composable
fun CapoDialog(
    capo: Int,
    adjust: Boolean,
    sample: List<String>,
    onPick: (capo: Int, adjust: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    var v by remember { mutableIntStateOf(capo) }
    var adj by remember { mutableStateOf(adjust) }
    SbDialog("Capo", onDismiss, "Done", { onPick(v, adj) }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { if (v > 0) v-- }) { Text("−", style = sbStyle(28, FontWeight.Bold, c.ac)) }
                Text(if (v == 0) "No capo" else "Fret $v", style = sbStyle(18, FontWeight.SemiBold, c.tx))
                TextButton(onClick = { if (v < 11) v++ }) { Text("+", style = sbStyle(28, FontWeight.Bold, c.ac)) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Show chord shapes", style = sbStyle(14, FontWeight.Medium, c.tx))
                    Text("Chords and tabs follow the capo", style = sbStyle(12, color = c.mu))
                }
                SbSwitch(adj) { adj = it }
            }
            if (adj && v > 0 && sample.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.track).padding(12.dp)) {
                    Text("Written   ${sample.joinToString("  ")}", style = sbStyle(12, color = c.mu, mono = true))
                    Text(
                        "Play      ${sample.joinToString("  ") { com.songbook.app.data.Transposer.chord(it, -v) }}",
                        style = sbStyle(12, FontWeight.Bold, c.ac, mono = true)
                    )
                }
            }
        }
    }
}

/** Auto scroll speed card, 1 (slow) to 10 (fast). Changes apply live. */
@Composable
fun ScrollSpeedDialog(speed: Int, onSpeed: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = Sb.colors
    var v by remember { mutableIntStateOf(speed) }
    val colors = SliderDefaults.colors(thumbColor = c.ac, activeTrackColor = c.ac, inactiveTrackColor = c.ln)
    SbDialog("Auto scroll speed", onDismiss, "Done", { onDismiss() }, dismissText = "Close") {
        Column {
            Text("Speed · $v", style = sbStyle(13, FontWeight.Medium, c.tx))
            Slider(
                value = v.toFloat(),
                onValueChange = { v = it.roundToInt() },
                onValueChangeFinished = { onSpeed(v) },
                valueRange = 1f..10f,
                steps = 8,
                colors = colors
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Slower", style = sbStyle(12, color = c.mu))
                Text("Faster", style = sbStyle(12, color = c.mu))
            }
        }
    }
}

/** Pick a workflow stage (Idea ... Finished). */
@Composable
fun StageDialog(selected: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = Sb.colors
    SbDialog("Workflow stage", onDismiss) {
        Column {
            Stage.names.forEachIndexed { i, name ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable { onPick(i) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, style = sbStyle(15, if (i == selected) FontWeight.SemiBold else FontWeight.Medium, if (i == selected) c.ac else c.tx))
                }
            }
        }
    }
}
