package com.songbook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.data.Stage
import com.songbook.app.ui.components.BadgePill
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.CircleArtwork
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.SbDialog
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.SectionTitle
import com.songbook.app.ui.components.relativeDay
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/**
 * Songwriting workflow: every song that is not "Finished" sits in a stage
 * (Idea, Lyrics, Chords, Arrangement) with a "next step" note and a one-tap way to move it on.
 */
@Composable
fun WorkflowScreen(
    songs: List<SongEntity>,
    albums: List<AlbumEntity>,
    onBack: () -> Unit,
    onOpenSong: (Long) -> Unit,
    onUpdateSong: (SongEntity) -> Unit,
    onNewIdea: () -> Unit
) {
    val c = Sb.colors
    val albumNames = remember(albums) { albums.associate { it.id to it.name } }
    val active = remember(songs) { songs.filter { it.stage < Stage.DONE } }
    val finished = remember(songs) { songs.count { it.stage >= Stage.DONE } }
    var noteFor by remember { mutableStateOf<SongEntity?>(null) }

    noteFor?.let { s ->
        NoteDialog(s.note, { noteFor = null }) { onUpdateSong(s.copy(note = it)); noteFor = null }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(c.bg).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SbIconButton(SbIcons.Back, "Back", onBack, size = 24.dp)
                Text("Workflow", style = sbStyle(16, FontWeight.SemiBold, c.ac))
                ChipPill("+ Idea", active = false, onClick = onNewIdea)
            }
        }
        item {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp)) {
                Text("Songs in the making", style = sbStyle(26, FontWeight.Bold, c.tx))
                Text(
                    "${active.size} in progress · $finished finished",
                    style = sbStyle(14, color = c.mu),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        item {
            // one box per stage with the number of songs in it
            Row(
                Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (0 until Stage.DONE).forEach { st ->
                    Column(
                        Modifier
                            .weight(1f)
                            .sbCard(16.dp)
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${active.count { it.stage == st }}", style = sbStyle(20, FontWeight.Bold, c.ac))
                        Text(Stage.name(st), style = sbStyle(10, FontWeight.Medium, c.mu), maxLines = 1)
                    }
                }
            }
        }
        if (active.isEmpty()) {
            item {
                EmptyHint("Nothing in progress. New songs land here until you mark them finished. Tap “+ Idea” to start one.")
            }
        }
        (0 until Stage.DONE).forEach { st ->
            val inStage = active.filter { it.stage == st }
            if (inStage.isNotEmpty()) {
                item(key = "h$st") {
                    SectionTitle(Stage.name(st), Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 10.dp))
                }
                items(inStage, key = { it.id }) { s ->
                    WorkflowCard(
                        song = s,
                        albumName = albumNames[s.albumId] ?: "No album",
                        onOpen = { onOpenSong(s.id) },
                        onNote = { noteFor = s },
                        onAdvance = { onUpdateSong(s.copy(stage = (s.stage + 1).coerceAtMost(Stage.DONE))) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkflowCard(
    song: SongEntity,
    albumName: String,
    onOpen: () -> Unit,
    onNote: () -> Unit,
    onAdvance: () -> Unit
) {
    val c = Sb.colors
    val last = song.stage >= Stage.DONE - 1
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .sbCard(24.dp)
            .clip(RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircleArtwork(song.gradient, 46.dp, image = song.image)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        song.title,
                        style = sbStyle(15, FontWeight.SemiBold, c.tx),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    BadgePill(song.badge, song.badgeColor)
                }
                Text("$albumName · edited ${relativeDay(song.updatedAt)}", style = sbStyle(12, color = c.mu), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // progress: one segment per stage, the last one is "Finished"
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..Stage.DONE).forEach { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i <= song.stage) c.ac else c.ln)
                )
            }
        }

        // next-step note
        Text(
            if (song.note.isBlank()) "Add a note for the next step…" else song.note,
            style = sbStyle(13, color = if (song.note.isBlank()) c.mu else c.tx),
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.bg)
                .clickable(onClick = onNote)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )

        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipPill("Open", active = false, onClick = onOpen, inactiveBg = c.bg)
            ChipPill(
                if (last) "Mark finished" else "Move to ${Stage.name(song.stage + 1)}",
                active = true,
                onClick = onAdvance
            )
        }
    }
}

@Composable
private fun NoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    SbDialog("Next step", onDismiss, "Save", { onSave(text.trim()) }) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("What's next for this song?") },
            textStyle = sbStyle(15, color = Sb.colors.tx),
            minLines = 2,
            maxLines = 4
        )
    }
}
