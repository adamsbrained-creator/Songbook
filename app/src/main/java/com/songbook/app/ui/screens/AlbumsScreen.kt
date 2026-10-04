package com.songbook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.GroupEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.ui.components.ArtTile
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.SectionTitle
import com.songbook.app.ui.components.TextInputDialog
import com.songbook.app.ui.components.albumCountLabel
import com.songbook.app.ui.components.groupKind
import com.songbook.app.ui.components.initials
import com.songbook.app.ui.components.songCountLabel
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

@Composable
fun AlbumsScreen(
    albums: List<AlbumEntity>,
    songs: List<SongEntity>,
    groups: List<GroupEntity>,
    columns: Int,
    onOpenAlbum: (Long) -> Unit,
    onOpenGroup: (Long) -> Unit,
    onCreateAlbum: (String) -> Unit,
    onCreateGroup: (name: String, albumIds: List<Long>) -> Unit
) {
    val c = Sb.colors
    var creating by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var naming by remember { mutableStateOf(false) }

    if (creating) {
        TextInputDialog("New album", "Album name", confirmText = "Create",
            onConfirm = { onCreateAlbum(it); creating = false }, onDismiss = { creating = false })
    }
    if (naming) {
        TextInputDialog("Name this group", "Group name (e.g. The Flower Trilogy)", confirmText = "Create",
            onConfirm = { name ->
                onCreateGroup(name, selected.toList())
                naming = false
                selecting = false
                selected = emptySet()
            },
            onDismiss = { naming = false })
    }

    val counts = remember(songs) { songs.groupingBy { it.albumId }.eachCount() }
    val groupIds = remember(groups) { groups.map { it.id }.toSet() }
    // Albums that live inside a group are shown on the group's page instead.
    val loose = remember(albums, groupIds) { albums.filter { it.groupId == null || it.groupId !in groupIds } }
    val memberCount = remember(albums) { albums.filter { it.groupId != null }.groupingBy { it.groupId }.eachCount() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().background(c.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (selecting) "Select albums" else "Albums", style = sbStyle(26, FontWeight.Bold, c.tx))
                    Text(
                        if (selecting) {
                            if (selected.isEmpty()) "Pick two or more to group" else "${selected.size} selected"
                        } else "Your music, organized your way.",
                        style = sbStyle(14, color = c.mu),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selecting) {
                        ChipPill("Cancel", active = false, onClick = { selecting = false; selected = emptySet() })
                        ChipPill(
                            "Group",
                            active = selected.size >= 2,
                            onClick = { if (selected.size >= 2) naming = true },
                            inactiveBg = c.cd
                        )
                    } else {
                        if (loose.size >= 2) ChipPill("Select", active = false, onClick = { selecting = true })
                        ChipPill("+ New", active = false, onClick = { creating = true })
                    }
                }
            }
        }

        if (groups.isNotEmpty() && !selecting) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Groups", Modifier.padding(top = 4.dp))
            }
            items(groups, key = { "g${it.id}" }) { g ->
                val n = memberCount[g.id] ?: 0
                ArtTile(
                    gradient = g.gradient,
                    image = g.image,
                    title = g.name,
                    subtitle = "${groupKind(n)} · ${albumCountLabel(n)}",
                    initialsText = initials(g.name),
                    badge = g.badge,
                    badgeColor = g.badgeColor,
                    columns = columns,
                    label = groupKind(n),
                    onClick = { onOpenGroup(g.id) }
                )
            }
            if (loose.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle("Albums", Modifier.padding(top = 4.dp))
                }
            }
        }

        if (albums.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { EmptyHint("No albums yet. Tap “+ New” to create one.") }
        }
        items(loose, key = { "a${it.id}" }) { a ->
            ArtTile(
                gradient = a.gradient,
                image = a.image,
                title = a.name,
                subtitle = songCountLabel(counts[a.id] ?: 0),
                initialsText = initials(a.name),
                badge = a.badge,
                badgeColor = a.badgeColor,
                columns = columns,
                selected = if (selecting) a.id in selected else null,
                onClick = {
                    if (selecting) selected = if (a.id in selected) selected - a.id else selected + a.id
                    else onOpenAlbum(a.id)
                }
            )
        }
    }
}
