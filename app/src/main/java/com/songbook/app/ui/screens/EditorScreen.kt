package com.songbook.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.data.Stage
import com.songbook.app.data.tagList
import com.songbook.app.ui.components.AlbumPickerDialog
import com.songbook.app.ui.components.BadgeDialog
import com.songbook.app.ui.components.BadgeRow
import com.songbook.app.ui.components.StageDialog
import com.songbook.app.ui.components.ConfirmDialog
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.StrokeIcon
import com.songbook.app.ui.components.TextInputDialog
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.SbColors
import com.songbook.app.ui.theme.sbStyle

private val ChordRx = Regex("\\[[^\\]\\n]*]")

/** Colors [chords] and "# Section" lines while keeping the text itself untouched. */
private class SongSyntax(private val c: SbColors) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val b = AnnotatedString.Builder(s)
        var idx = 0
        for (line in s.split("\n")) {
            if (line.trimStart().startsWith("#")) {
                b.addStyle(SpanStyle(color = c.ac, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em), idx, idx + line.length)
            }
            idx += line.length + 1
        }
        for (m in ChordRx.findAll(s)) {
            b.addStyle(SpanStyle(color = c.ac, fontFamily = FontFamily.Monospace), m.range.first, m.range.last + 1)
        }
        return TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private fun TextFieldValue.insert(snippet: String, caretBack: Int = 0): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val newText = text.replaceRange(start, end, snippet)
    val pos = start + snippet.length - caretBack
    return copy(text = newText, selection = TextRange(pos))
}

private fun TextFieldValue.insertSection(name: String): TextFieldValue {
    val start = selection.min
    val needsBreak = start > 0 && text[start - 1] != '\n'
    return insert((if (needsBreak) "\n" else "") + "# $name\n")
}

@Composable
fun EditorScreen(
    song: SongEntity?,
    isNew: Boolean,
    albums: List<AlbumEntity>,
    onClose: () -> Unit,
    onDiscardNew: () -> Unit,
    onSave: (SongEntity) -> Unit,
    onCreateAlbum: (String, (Long) -> Unit) -> Unit
) {
    if (song == null) {
        Box(Modifier.fillMaxSize().background(Sb.colors.bg))
        return
    }
    EditorContent(song, isNew, albums, onClose, onDiscardNew, onSave, onCreateAlbum)
}

@Composable
private fun EditorContent(
    song: SongEntity,
    isNew: Boolean,
    albums: List<AlbumEntity>,
    onClose: () -> Unit,
    onDiscardNew: () -> Unit,
    onSave: (SongEntity) -> Unit,
    onCreateAlbum: (String, (Long) -> Unit) -> Unit
) {
    val c = Sb.colors
    var title by rememberSaveable(song.id) { mutableStateOf(song.title) }
    var albumId by rememberSaveable(song.id) { mutableStateOf(song.albumId) }
    var tags by rememberSaveable(song.id) { mutableStateOf(song.tagList) }
    var body by rememberSaveable(song.id, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(song.body)) }
    var mode by rememberSaveable { mutableStateOf("Lyrics") }
    var stage by rememberSaveable(song.id) { mutableStateOf(song.stage) }
    var badge by rememberSaveable(song.id) { mutableStateOf(song.badge) }
    var badgeColor by rememberSaveable(song.id) { mutableStateOf(song.badgeColor) }
    var note by rememberSaveable(song.id) { mutableStateOf(song.note) }
    var pickStage by remember { mutableStateOf(false) }
    var pickBadge by remember { mutableStateOf(false) }

    var pickAlbum by remember { mutableStateOf(false) }
    var addTag by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val dirty = title != song.title || albumId != song.albumId || tags != song.tagList || body.text != song.body ||
        stage != song.stage || badge != song.badge || badgeColor != song.badgeColor || note != song.note
    val focus = remember { FocusRequester() }
    val syntax = remember(c) { SongSyntax(c) }

    fun leave() {
        when {
            isNew && !dirty -> onDiscardNew()
            dirty -> confirmDiscard = true
            else -> onClose()
        }
    }
    BackHandler { leave() }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            message = "Your edits to this song won't be saved.",
            confirmText = "Discard",
            onConfirm = { confirmDiscard = false; if (isNew) onDiscardNew() else onClose() },
            onDismiss = { confirmDiscard = false }
        )
    }
    if (pickAlbum) {
        AlbumPickerDialog(
            albums = albums,
            selectedId = albumId,
            onPick = { albumId = it; pickAlbum = false },
            onCreateAndPick = { name -> onCreateAlbum(name) { id -> albumId = id }; pickAlbum = false },
            onDismiss = { pickAlbum = false }
        )
    }
    if (pickStage) StageDialog(stage, { stage = it; pickStage = false }, { pickStage = false })
    if (pickBadge) BadgeDialog(badge, badgeColor, { t, col -> badge = t; badgeColor = col; pickBadge = false }, { pickBadge = false })
    if (addTag) {
        TextInputDialog("Add tag", "Tag", confirmText = "Add",
            onConfirm = { t -> if (t !in tags) tags = tags + t; addTag = false },
            onDismiss = { addTag = false })
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SbIconButton(SbIcons.Close, "Close", { leave() }, size = 24.dp)
            Text("Edit Song", style = sbStyle(16, FontWeight.SemiBold, c.ac))
            Box(
                Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(c.ac)
                    .clickable {
                        onSave(
                            song.copy(
                                title = title.trim().ifEmpty { "Untitled" }, albumId = albumId, tags = tags.joinToString(", "),
                                body = body.text, stage = stage, badge = badge, badgeColor = badgeColor, note = note.trim()
                            )
                        )
                    }
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) { Text("Done", style = sbStyle(13, FontWeight.SemiBold, Color.White)) }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // title
            Column(Modifier.fillMaxWidth().sbCard(22.dp).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text("TITLE", style = sbStyle(11, color = c.mu))
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = sbStyle(18, FontWeight.Bold, c.tx),
                    cursorBrush = SolidColor(c.ac),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
            // album
            Row(
                Modifier.fillMaxWidth().sbCard(22.dp).clip(RoundedCornerShape(22.dp)).clickable { pickAlbum = true }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ALBUM", style = sbStyle(11, color = c.mu))
                    Text(albums.firstOrNull { it.id == albumId }?.name ?: "No album", style = sbStyle(18, FontWeight.Bold, c.tx), modifier = Modifier.padding(top = 2.dp))
                }
                StrokeIcon(SbIcons.ChevronRight, c.tx, size = 22.dp)
            }
            // tags
            Column(Modifier.fillMaxWidth().sbCard(22.dp).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text("TAGS", style = sbStyle(11, color = c.mu))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    tags.forEach { t ->
                        Row(
                            Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(c.bg)
                                .clickable { tags = tags - t }.padding(start = 16.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(t, style = sbStyle(13, FontWeight.SemiBold, c.tx))
                            StrokeIcon(SbIcons.Close, c.mu, size = 14.dp)
                        }
                    }
                    ChipPill("+ Add", active = tags.isEmpty(), onClick = { addTag = true }, inactiveBg = c.bg)
                }
            }
            // status: workflow stage, badge, next step
            Column(Modifier.fillMaxWidth().sbCard(22.dp).clip(RoundedCornerShape(22.dp)).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text("STATUS", style = sbStyle(11, color = c.mu))
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable { pickStage = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Workflow stage", style = sbStyle(15, FontWeight.Medium, c.tx))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(Stage.name(stage), style = sbStyle(14, color = c.mu))
                        StrokeIcon(SbIcons.ChevronRight, c.mu, size = 20.dp)
                    }
                }
                BadgeRow(badge, badgeColor, onClick = { pickBadge = true })
                if (stage < Stage.DONE) {
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it },
                        textStyle = sbStyle(14, color = c.tx),
                        cursorBrush = SolidColor(c.ac),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.bg).padding(horizontal = 14.dp, vertical = 12.dp)) {
                                if (note.isEmpty()) Text("Next step, shown on the Workflow page", style = sbStyle(14, color = c.mu))
                                inner()
                            }
                        }
                    )
                }
            }
            // text
            Box(Modifier.fillMaxWidth().sbCard(28.dp).padding(horizontal = 20.dp, vertical = 18.dp)) {
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    textStyle = sbStyle(17, FontWeight.Medium, c.tx).copy(lineHeight = 30.sp),
                    cursorBrush = SolidColor(c.ac),
                    visualTransformation = syntax,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp).focusRequester(focus),
                    decorationBox = { inner ->
                        Box {
                            if (body.text.isEmpty()) {
                                Text(
                                    "Start writing…\n\nTip: put chords in brackets, like [Em]I woke up,\nand start a section with # Verse",
                                    style = sbStyle(15, color = c.mu)
                                )
                            }
                            inner()
                        }
                    }
                )
            }
            Box(Modifier.height(8.dp))
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ChipPill("Lyrics", mode == "Lyrics", { mode = "Lyrics"; focus.requestFocus() })
            ChipPill("Chord", mode == "Chord", { mode = "Chord"; body = body.insert("[]", caretBack = 1); focus.requestFocus() })
            ChipPill("Verse", mode == "Verse", { mode = "Verse"; body = body.insertSection("Verse"); focus.requestFocus() })
            ChipPill("Chorus", mode == "Chorus", { mode = "Chorus"; body = body.insertSection("Chorus"); focus.requestFocus() })
            ChipPill("Bridge", mode == "Bridge", { mode = "Bridge"; body = body.insertSection("Bridge"); focus.requestFocus() })
        }
    }
}
