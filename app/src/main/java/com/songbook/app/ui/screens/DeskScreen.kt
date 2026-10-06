package com.songbook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songbook.app.data.IdeaData
import com.songbook.app.data.IdeaField
import com.songbook.app.data.IdeaLine
import com.songbook.app.data.IdeaTask
import com.songbook.app.data.SongEntity
import com.songbook.app.data.Stage
import com.songbook.app.data.UiStyle
import com.songbook.app.data.ideaData
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.GlassRoundButton
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.StrokeIcon
import com.songbook.app.ui.components.TextInputDialog
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.components.screenBg
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle
import kotlinx.coroutines.delay

/** Everything on the desk, so changes can be compared and saved together. */
private data class Desk(
    val title: String,
    val spark: String,
    val lines: List<IdeaLine>,
    val tasks: List<IdeaTask>,
    val fields: List<IdeaField>,
    val note: String,
    val stage: Int
)

private fun lyricsFrom(lines: List<IdeaLine>): String {
    val sb = StringBuilder()
    var current = ""
    lines.forEach { l ->
        val text = l.text.trim()
        if (text.isEmpty()) return@forEach
        if (l.tag.isNotBlank() && l.tag != current) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append("# ").append(l.tag).append("\n")
            current = l.tag
        }
        sb.append(text).append("\n")
    }
    return sb.toString().trimEnd()
}

/**
 * The songwriting desk: a place for a song that is not finished. A spark (what it is about), loose
 * lines you can tag as hook / verse / chorus, a checklist, details with names you choose, and the next step.
 * Saves by itself.
 */
@Composable
fun DeskScreen(
    song: SongEntity?,
    onBack: () -> Unit,
    onSave: (SongEntity) -> Unit,
    onEditText: (Long) -> Unit
) {
    val c = Sb.colors
    LaunchedEffect(song == null) { if (song == null) onBack() }
    if (song == null) return

    val initial = remember(song.id) { song.ideaData }
    var title by remember(song.id) { mutableStateOf(song.title) }
    var spark by remember(song.id) { mutableStateOf(initial.spark) }
    val lines = remember(song.id) { mutableStateListOf<IdeaLine>().also { it.addAll(initial.lines) } }
    val tasks = remember(song.id) { mutableStateListOf<IdeaTask>().also { it.addAll(initial.tasks) } }
    val fields = remember(song.id) { mutableStateListOf<IdeaField>().also { it.addAll(initial.fields) } }
    var note by remember(song.id) { mutableStateOf(song.note) }
    var stage by remember(song.id) { mutableIntStateOf(song.stage) }
    var newLine by remember { mutableStateOf("") }
    var newTask by remember { mutableStateOf("") }
    var customField by remember { mutableStateOf(false) }

    val latestSong by rememberUpdatedState(song)
    val desk = Desk(title, spark, lines.toList(), tasks.toList(), fields.toList(), note, stage)
    val latestDesk by rememberUpdatedState(desk)

    fun save(d: Desk) {
        val s = latestSong
        val json = IdeaData(d.spark, d.lines, d.tasks, d.fields).toJson()
        val t = d.title.trim().ifEmpty { "Untitled" }
        if (json != s.idea || t != s.title || d.note.trim() != s.note || d.stage != s.stage) {
            onSave(s.copy(title = t, idea = json, note = d.note.trim(), stage = d.stage))
        }
    }
    LaunchedEffect(desk) { delay(600); save(latestDesk) }
    DisposableEffect(song.id) { onDispose { save(latestDesk) } }

    if (customField) {
        TextInputDialog("New detail", "Name (e.g. Capo, Tuning, BPM)", confirmText = "Add",
            onConfirm = { fields.add(IdeaField(it, "")); customField = false }, onDismiss = { customField = false })
    }

    Column(Modifier.fillMaxSize().screenBg().statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (Sb.style == UiStyle.GLASS) GlassRoundButton(SbIcons.Back, "Back", onBack, Modifier.padding(start = 8.dp))
            else SbIconButton(SbIcons.Back, "Back", onBack, size = 24.dp)
            Text("Songwriting desk", style = sbStyle(16, FontWeight.SemiBold, c.ac))
            Text(
                "Edit text",
                style = sbStyle(13, FontWeight.SemiBold, c.ac),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { save(latestDesk); onEditText(song.id) }.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeskInput(title, { title = it }, "Song title", 26, FontWeight.Bold, Modifier.padding(horizontal = 8.dp))

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stage.names.forEachIndexed { i, n -> ChipPill(n, active = stage == i, onClick = { stage = i }) }
            }

            DeskCard("Spark", "What is this song about?") {
                DeskInput(spark, { spark = it }, "A feeling, an image, a story…", 15, FontWeight.Normal, Modifier.fillMaxWidth(), minHeight = 64)
            }

            DeskCard("Lines", "${lines.size} scraps of lyrics") {
                lines.forEachIndexed { i, l ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tags = listOf("") + IdeaData.LineTags
                        Text(
                            l.tag.ifBlank { "tag" },
                            style = sbStyle(11, FontWeight.SemiBold, if (l.tag.isBlank()) c.mu else Color.White),
                            modifier = Modifier.padding(top = 2.dp).width(54.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (l.tag.isBlank()) c.track else c.ac)
                                .clickable { lines[i] = l.copy(tag = tags[(tags.indexOf(l.tag).coerceAtLeast(0) + 1) % tags.size]) }
                                .padding(vertical = 5.dp),
                            maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        DeskInput(l.text, { lines[i] = l.copy(text = it) }, "", 15, FontWeight.Medium, Modifier.weight(1f))
                        Column {
                            Text("▲", style = sbStyle(12, color = if (i > 0) c.mu else c.ln), modifier = Modifier.clickable(enabled = i > 0) { val t = lines.removeAt(i); lines.add(i - 1, t) }.padding(horizontal = 6.dp, vertical = 2.dp))
                            Text("▼", style = sbStyle(12, color = if (i < lines.lastIndex) c.mu else c.ln), modifier = Modifier.clickable(enabled = i < lines.lastIndex) { val t = lines.removeAt(i); lines.add(i + 1, t) }.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        StrokeIcon(SbIcons.Close, c.mu, Modifier.padding(top = 4.dp).clip(CircleShape).clickable { lines.removeAt(i) }, size = 18.dp)
                    }
                }
                AddRow(newLine, { newLine = it }, "Add a line…") {
                    if (newLine.isNotBlank()) { lines.add(IdeaLine(newLine.trim())); newLine = "" }
                }
                if (lines.isNotEmpty()) {
                    ChipPill(
                        "Turn lines into lyrics", active = false, inactiveBg = c.track,
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            val s = latestSong
                            val add = lyricsFrom(lines)
                            val body = if (s.body.isBlank()) add else s.body.trimEnd() + "\n\n" + add
                            val newStage = if (stage == Stage.IDEA) 1 else stage
                            stage = newStage
                            onSave(s.copy(title = title.trim().ifEmpty { "Untitled" }, idea = IdeaData(spark, lines.toList(), tasks.toList(), fields.toList()).toJson(), note = note.trim(), stage = newStage, body = body))
                            onEditText(s.id)
                        }
                    )
                }
            }

            DeskCard("Checklist", if (tasks.isEmpty()) "Small steps to finish it" else "${tasks.count { it.done }} of ${tasks.size} done") {
                tasks.forEachIndexed { i, t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(if (t.done) c.ac else c.track).clickable { tasks[i] = t.copy(done = !t.done) },
                            contentAlignment = Alignment.Center
                        ) { if (t.done) StrokeIcon(SbIcons.Check, Color.White, size = 15.dp) }
                        DeskInput(t.text, { tasks[i] = t.copy(text = it) }, "", 15, FontWeight.Medium, Modifier.weight(1f), muted = t.done)
                        StrokeIcon(SbIcons.Close, c.mu, Modifier.clip(CircleShape).clickable { tasks.removeAt(i) }, size = 18.dp)
                    }
                }
                AddRow(newTask, { newTask = it }, "Add a step…") {
                    if (newTask.isNotBlank()) { tasks.add(IdeaTask(newTask.trim())); newTask = "" }
                }
            }

            DeskCard("Details", "Key, tempo, mood… or anything you name") {
                fields.forEachIndexed { i, f ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(f.name, style = sbStyle(14, FontWeight.SemiBold, c.mu), modifier = Modifier.width(92.dp), maxLines = 1)
                        DeskInput(f.value, { fields[i] = f.copy(value = it) }, "—", 15, FontWeight.Medium, Modifier.weight(1f))
                        StrokeIcon(SbIcons.Close, c.mu, Modifier.clip(CircleShape).clickable { fields.removeAt(i) }, size = 18.dp)
                    }
                }
                val unused = IdeaData.SuggestedFields.filter { n -> fields.none { it.name.equals(n, true) } }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    unused.forEach { n -> ChipPill("+ $n", active = false, inactiveBg = c.track, onClick = { fields.add(IdeaField(n, "")) }, modifier = Modifier.height(34.dp)) }
                    ChipPill("+ Custom", active = true, onClick = { customField = true }, modifier = Modifier.height(34.dp))
                }
            }

            DeskCard("Next step", "Shown on the Workflow page") {
                DeskInput(note, { note = it }, "What's next for this song?", 15, FontWeight.Normal, Modifier.fillMaxWidth(), minHeight = 40)
            }
        }
    }
}

@Composable
private fun DeskCard(title: String, hint: String, content: @Composable () -> Unit) {
    val c = Sb.colors
    Column(Modifier.fillMaxWidth().sbCard(22.dp).clip(RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text(title, style = sbStyle(15, FontWeight.Bold, c.tx))
        Text(hint, style = sbStyle(12, color = c.mu), modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
private fun DeskInput(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    size: Int,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    minHeight: Int = 0,
    muted: Boolean = false
) {
    val c = Sb.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = sbStyle(size, weight, if (muted) c.mu else c.tx),
        cursorBrush = SolidColor(c.ac),
        modifier = modifier,
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = minHeight.dp)) {
                if (value.isEmpty() && hint.isNotEmpty()) Text(hint, style = sbStyle(size, weight, c.mu.copy(alpha = 0.7f)))
                inner()
            }
        }
    )
}

@Composable
private fun AddRow(value: String, onChange: (String) -> Unit, hint: String, onAdd: () -> Unit) {
    val c = Sb.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(14.dp)).background(c.track).padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeskInput(value, onChange, hint, 14, FontWeight.Normal, Modifier.weight(1f).padding(vertical = 12.dp))
        Text(
            "Add", style = sbStyle(13, FontWeight.Bold, if (value.isBlank()) c.mu else c.ac),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = value.isNotBlank(), onClick = onAdd).padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}
