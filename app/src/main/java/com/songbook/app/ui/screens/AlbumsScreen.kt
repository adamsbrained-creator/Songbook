package com.songbook.app.ui.screens

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
import com.songbook.app.data.UiStyle
import com.songbook.app.ui.components.ArtTile
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.GlassTitle
import com.songbook.app.ui.components.MomentsHeader
import com.songbook.app.ui.components.SectionTitle
import com.songbook.app.ui.components.TextInputDialog
import com.songbook.app.ui.components.TopChips
import com.songbook.app.ui.components.WorkflowEntry
import com.songbook.app.ui.components.albumCountLabel
import com.songbook.app.ui.components.groupKind
import com.songbook.app.ui.components.initials
import com.songbook.app.ui.components.screenBg
import com.songbook.app.ui.components.songCountLabel
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

@Composable
fun AlbumsScreen(
    albums: List<AlbumEntity>,
    songs: List<SongEntity>,
    groups: List<GroupEntity>,
    columns: Int,
    workflowCount: Int,
    ideaCount: Int,
    onOpenAlbum: (Long) -> Unit,
    onOpenGroup: (Long) -> Unit,
    onCreateAlbum: (String) -> Unit,
    onCreateGroup: (name: String, albumIds: List<Long>) -> Unit,
    onOpenWorkflow: () -> Unit,
    onNavigate: (String) -> Unit,
    onSettings: () -> Unit
) {
    val c = Sb.colors
    val style = Sb.style
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
    val side = if (style == UiStyle.NEW) 16.dp else 24.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().screenBg().statusBarsPadding(),
        contentPadding = PaddingValues(start = side, end = side, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ---- header, one per look ----
        item(span = { GridItemSpan(maxLineSpan) }) {
            val subtitle = if (selecting) {
                if (selected.isEmpty()) "Pick two or more to group" else "${selected.size} selected"
            } else "Your music, organized your way."
            val actions: @Composable () -> Unit = {
                if (selecting) {
                    ChipPill("Cancel", active = false, onClick = { selecting = false; selected = emptySet() })
                    ChipPill("Group", active = selected.size >= 2, onClick = { if (selected.size >= 2) naming = true })
                } else {
                    if (loose.size >= 2) ChipPill("Select", active = false, onClick = { selecting = true })
                    if (style != UiStyle.NEW) ChipPill("+ New", active = false, onClick = { creating = true })
                }
            }
            when (style) {
                UiStyle.NEW -> Column(Modifier.fillMaxWidth()) {
                    MomentsHeader(
                        title = if (selecting) "Select albums" else "Albums",
                        onSettings = onSettings,
                        onPlus = { creating = true },
                        trailing = { if (loose.size >= 2 || selecting) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { actions() } }
                    )
                    TopChips("albums", onNavigate, Modifier.padding(bottom = 4.dp))
                }
                UiStyle.GLASS -> Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    GlassTitle(if (selecting) "Select albums" else "Albums", subtitle, Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
                }
                else -> Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (selecting) "Select albums" else "Albums", style = sbStyle(26, FontWeight.Bold, c.tx))
                        Text(subtitle, style = sbStyle(14, color = c.mu), modifier = Modifier.padding(top = 2.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
                }
            }
        }

        if (groups.isNotEmpty() && !selecting) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Groups", Modifier.padding(top = 4.dp)) }
            items(groups, key = { "g${it.id}" }) { g ->
                val n = memberCount[g.id] ?: 0
                val kind = g.typeName.ifBlank { groupKind(n) }
                ArtTile(
                    gradient = g.gradient, image = g.image, title = g.name,
                    subtitle = "$kind · ${albumCountLabel(n)}",
                    initialsText = initials(g.name), badge = g.badge, badgeColor = g.badgeColor,
                    columns = columns, label = kind, rating = g.rating,
                    onClick = { onOpenGroup(g.id) }
                )
            }
            if (loose.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Albums", Modifier.padding(top = 4.dp)) }
            }
        }

        if (albums.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { EmptyHint("No albums yet. Tap “+ New” to create one.") }
        }
        items(loose, key = { "a${it.id}" }) { a ->
            val n = counts[a.id] ?: 0
            ArtTile(
                gradient = a.gradient, image = a.image, title = a.name,
                subtitle = if (a.typeName.isBlank()) songCountLabel(n) else "${a.typeName} · ${songCountLabel(n)}",
                initialsText = initials(a.name), badge = a.badge, badgeColor = a.badgeColor,
                columns = columns,
                label = a.typeName.ifBlank { null },
                rating = a.rating,
                selected = if (selecting) a.id in selected else null,
                onClick = {
                    if (selecting) selected = if (a.id in selected) selected - a.id else selected + a.id
                    else onOpenAlbum(a.id)
                }
            )
        }

        if (style == UiStyle.NEW && !selecting) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                WorkflowEntry(workflowCount, ideaCount, onOpenWorkflow, Modifier.padding(top = 6.dp), horizontal = 0.dp)
            }
        }
    }
}
