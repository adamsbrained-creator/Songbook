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
        containerColor = c.cd,
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

// ---- artwork picker ------------------------------------------------------------------------

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

@Composable
fun ArtworkDialog(selected: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    SbDialog("Change artwork", onDismiss) {
        GradientPicker(selected) { onPick(it) }
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

// ---- reading size dialog (Aa button and Settings) ------------------------------------------

@Composable
fun ReadingDialog(
    lyricsSize: Int,
    chordSize: Int,
    lineSpacing: Float,
    onCommit: (lyrics: Int, chord: Int, spacing: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    var l by remember { mutableIntStateOf(lyricsSize) }
    var ch by remember { mutableIntStateOf(chordSize) }
    var s by remember { mutableFloatStateOf(lineSpacing) }
    val colors = SliderDefaults.colors(thumbColor = c.ac, activeTrackColor = c.ac, inactiveTrackColor = c.ln)

    SbDialog("Reading", onDismiss, "Done", { onDismiss() }, dismissText = "Close") {
        Column {
            Text("Lyrics font size · $l pt", style = sbStyle(13, FontWeight.Medium, c.tx))
            Slider(
                value = l.toFloat(), onValueChange = { l = it.roundToInt() },
                onValueChangeFinished = { onCommit(l, ch, s) },
                valueRange = 12f..28f, steps = 15, colors = colors
            )
            Text("Chord size · $ch pt", style = sbStyle(13, FontWeight.Medium, c.tx))
            Slider(
                value = ch.toFloat(), onValueChange = { ch = it.roundToInt() },
                onValueChangeFinished = { onCommit(l, ch, s) },
                valueRange = 10f..22f, steps = 11, colors = colors
            )
            Text("Line spacing · ${"%.1f".format(s)}×", style = sbStyle(13, FontWeight.Medium, c.tx))
            Slider(
                value = s, onValueChange = { s = (it * 10).roundToInt() / 10f },
                onValueChangeFinished = { onCommit(l, ch, s) },
                valueRange = 1.0f..2.0f, steps = 9, colors = colors
            )
        }
    }
}

@Composable
fun CapoDialog(capo: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = Sb.colors
    var v by remember { mutableIntStateOf(capo) }
    SbDialog("Capo", onDismiss, "Done", { onPick(v) }) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { if (v > 0) v-- }) { Text("−", style = sbStyle(28, FontWeight.Bold, c.ac)) }
            Text(if (v == 0) "No capo" else "Fret $v", style = sbStyle(18, FontWeight.SemiBold, c.tx))
            TextButton(onClick = { if (v < 11) v++ }) { Text("+", style = sbStyle(28, FontWeight.Bold, c.ac)) }
        }
    }
}

// ---- song menu sheet -----------------------------------------------------------------------

private enum class MenuSub { None, Move, Artwork, Delete }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuSheet(
    song: SongEntity,
    albumName: String?,
    albums: List<AlbumEntity>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onMove: (Long?) -> Unit,
    onCreateAlbumAndMove: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onArtwork: (Int) -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val c = Sb.colors
    var sub by remember { mutableStateOf(MenuSub.None) }

    when (sub) {
        MenuSub.Move -> AlbumPickerDialog(
            albums = albums,
            selectedId = song.albumId,
            onPick = { onMove(it); onDismiss() },
            onCreateAndPick = { onCreateAlbumAndMove(it); onDismiss() },
            onDismiss = { sub = MenuSub.None }
        )
        MenuSub.Artwork -> ArtworkDialog(song.gradient, { onArtwork(it); onDismiss() }, { sub = MenuSub.None })
        MenuSub.Delete -> ConfirmDialog(
            title = "Delete song?",
            message = "“${song.title}” will be removed permanently.",
            confirmText = "Delete",
            onConfirm = { onDelete(); onDismiss() },
            onDismiss = { sub = MenuSub.None }
        )
        MenuSub.None -> Unit
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.cd,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 14.dp)
                    .size(40.dp, 5.dp)
                    .background(c.ln, RoundedCornerShape(3.dp))
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp).navigationBarsPadding()) {
            Row(
                Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SquareArtwork(song.gradient, 56.dp, 18.dp)
                Column {
                    Text(song.title, style = sbStyle(17, FontWeight.Bold, c.tx), maxLines = 1)
                    Text(albumName ?: "No album", style = sbStyle(13, color = c.mu))
                }
            }
            MenuRow("Edit") { onEdit(); onDismiss() }
            MenuRow("Duplicate") { onDuplicate(); onDismiss() }
            MenuRow("Move to Album") { sub = MenuSub.Move }
            MenuRow(if (song.favorite) "Remove from Favorites" else "Add to Favorites") { onToggleFavorite(); onDismiss() }
            MenuRow("Change Artwork") { sub = MenuSub.Artwork }
            MenuRow("Export") { onExport(); onDismiss() }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.ln))
            Row(
                Modifier.fillMaxWidth().height(50.dp).clickable { sub = MenuSub.Delete },
                verticalAlignment = Alignment.CenterVertically
            ) { Text("Delete Song", style = sbStyle(15, FontWeight.SemiBold, c.ac)) }
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    val c = Sb.colors
    Row(
        Modifier.fillMaxWidth().height(50.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = sbStyle(15, FontWeight.Medium, c.tx))
        StrokeIcon(SbIcons.ChevronRight, c.mu, size = 20.dp)
    }
}
