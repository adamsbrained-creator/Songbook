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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.GroupEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.ui.components.ArtTile
import com.songbook.app.ui.components.BadgePill
import com.songbook.app.ui.components.ConfirmDialog
import com.songbook.app.ui.components.EditCoverDialog
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.SbDialog
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.SquareArtwork
import com.songbook.app.ui.components.StrokeIcon
import com.songbook.app.ui.components.albumCountLabel
import com.songbook.app.ui.components.groupKind
import com.songbook.app.ui.components.initials
import com.songbook.app.ui.components.songCountLabel
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/** A trilogy / collection: its albums in a grid, with edit, manage albums and ungroup. */
@Composable
fun GroupScreen(
    group: GroupEntity?,
    albums: List<AlbumEntity>,
    songs: List<SongEntity>,
    columns: Int,
    onBack: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onUpdateGroup: (GroupEntity) -> Unit,
    onSetMembers: (List<Long>) -> Unit,
    onUngroup: () -> Unit
) {
    val c = Sb.colors
    // group deleted (e.g. its last album was removed): leave the screen
    LaunchedEffect(group == null) { if (group == null) onBack() }
    if (group == null) return

    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var managing by remember { mutableStateOf(false) }
    var confirmUngroup by remember { mutableStateOf(false) }

    val members = remember(albums, group.id) {
        albums.filter { it.groupId == group.id }.sortedWith(compareBy({ it.year }, { it.createdAt }))
    }
    val counts = remember(songs) { songs.groupingBy { it.albumId }.eachCount() }
    val songTotal = members.sumOf { counts[it.id] ?: 0 }

    if (editing) {
        EditCoverDialog(
            title = "Edit group",
            name0 = group.name,
            year0 = null,
            gradient0 = group.gradient,
            image0 = group.image,
            badge0 = group.badge,
            badgeColor0 = group.badgeColor,
            onDismiss = { editing = false }
        ) { r ->
            onUpdateGroup(group.copy(name = r.name, gradient = r.gradient, image = r.image, badge = r.badge, badgeColor = r.badgeColor))
            editing = false
        }
    }
    if (managing) {
        MembersDialog(
            albums = albums,
            groupId = group.id,
            initial = members.map { it.id }.toSet(),
            onDismiss = { managing = false },
            onSave = { ids -> onSetMembers(ids); managing = false }
        )
    }
    if (confirmUngroup) {
        ConfirmDialog(
            "Ungroup albums?",
            "“${group.name}” will be removed. Its albums stay in your library.",
            "Ungroup",
            onConfirm = { confirmUngroup = false; onUngroup() },
            onDismiss = { confirmUngroup = false }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().background(c.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SbIconButton(SbIcons.Back, "Back", onBack, size = 24.dp)
                Text(groupKind(members.size), style = sbStyle(16, FontWeight.SemiBold, c.ac))
                Box {
                    SbIconButton(SbIcons.More, "More", { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.background(c.cd)) {
                        DropdownMenuItem(text = { Text("Edit group", style = sbStyle(14, color = c.tx)) }, onClick = { menuOpen = false; editing = true })
                        DropdownMenuItem(text = { Text("Manage albums", style = sbStyle(14, color = c.tx)) }, onClick = { menuOpen = false; managing = true })
                        DropdownMenuItem(text = { Text("Ungroup", style = sbStyle(14, color = c.ac)) }, onClick = { menuOpen = false; confirmUngroup = true })
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SquareArtwork(group.gradient, 195.dp, 40.dp, elevated = true, image = group.image)
                Text(group.name, style = sbStyle(28, FontWeight.Bold, c.tx), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp))
                Text(
                    "${albumCountLabel(members.size)} · ${songCountLabel(songTotal)}",
                    style = sbStyle(14, color = c.mu),
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (group.badge.isNotBlank()) BadgePill(group.badge, group.badgeColor, Modifier.padding(top = 8.dp))
                Row(Modifier.padding(top = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(c.cd).clickable { editing = true }.padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Edit Group", style = sbStyle(13, FontWeight.SemiBold, c.tx)) }
                    Box(
                        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(c.ac).clickable { managing = true }.padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Manage Albums", style = sbStyle(13, FontWeight.SemiBold, Color.White)) }
                }
            }
        }
        if (members.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { EmptyHint("No albums in this group yet.") }
        }
        itemsIndexed(members, key = { _, a -> "a${a.id}" }) { i, a ->
            ArtTile(
                gradient = a.gradient,
                image = a.image,
                title = a.name,
                subtitle = songCountLabel(counts[a.id] ?: 0),
                initialsText = initials(a.name),
                badge = a.badge,
                badgeColor = a.badgeColor,
                columns = columns,
                label = "Part ${i + 1}",
                onClick = { onOpenAlbum(a.id) }
            )
        }
    }
}

/** Tick the albums that belong to the group. Albums that are in another group move over when ticked. */
@Composable
private fun MembersDialog(
    albums: List<AlbumEntity>,
    groupId: Long,
    initial: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (List<Long>) -> Unit
) {
    val c = Sb.colors
    var picked by remember { mutableStateOf(initial) }
    SbDialog("Albums in this group", onDismiss, "Save", { onSave(picked.toList()) }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            albums.forEach { a ->
                val on = a.id in picked
                val elsewhere = a.groupId != null && a.groupId != groupId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { picked = if (on) picked - a.id else picked + a.id },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(a.name, style = sbStyle(15, if (on) FontWeight.SemiBold else FontWeight.Medium, if (on) c.ac else c.tx), maxLines = 1)
                        if (elsewhere) Text("In another group", style = sbStyle(11, color = c.mu))
                    }
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(if (on) c.ac else c.ln),
                        contentAlignment = Alignment.Center
                    ) { if (on) StrokeIcon(SbIcons.Check, Color.White, size = 15.dp) }
                }
            }
        }
    }
}
