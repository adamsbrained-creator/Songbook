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
import com.songbook.app.ui.components.CoverHeader
import com.songbook.app.ui.components.GroupTypes
import com.songbook.app.ui.components.HeaderMenuItem
import com.songbook.app.ui.components.RatingDialog
import com.songbook.app.ui.components.TextInputDialog
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
import com.songbook.app.ui.components.screenBg
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

    var editing by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(false) }
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
            rating0 = group.rating,
            typeName0 = group.typeName,
            typePresets = GroupTypes,
            typeDefault = groupKind(members.size),
            onDismiss = { editing = false }
        ) { r ->
            onUpdateGroup(
                group.copy(
                    name = r.name, gradient = r.gradient, image = r.image, badge = r.badge,
                    badgeColor = r.badgeColor, rating = r.rating, typeName = r.typeName
                )
            )
            editing = false
        }
    }
    if (renaming) {
        TextInputDialog("Rename group", "Group name", group.name, "Rename",
            onConfirm = { onUpdateGroup(group.copy(name = it)); renaming = false },
            onDismiss = { renaming = false })
    }
    if (rating) RatingDialog("Rate group", group.rating, { onUpdateGroup(group.copy(rating = it)); rating = false }, { rating = false })
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
        modifier = Modifier.fillMaxSize().screenBg().statusBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CoverHeader(
                kindLabel = group.typeName.ifBlank { groupKind(members.size) },
                title = group.name,
                meta = "${albumCountLabel(members.size)} · ${songCountLabel(songTotal)}",
                gradient = group.gradient,
                image = group.image,
                badge = group.badge,
                badgeColor = group.badgeColor,
                rating = group.rating,
                primaryLabel = "Manage Albums",
                onPrimary = { managing = true },
                secondaryLabel = "Edit Group",
                onSecondary = { editing = true },
                onBack = onBack,
                menu = listOf(
                    HeaderMenuItem("Rename group") { renaming = true },
                    HeaderMenuItem("Edit group") { editing = true },
                    HeaderMenuItem("Manage albums") { managing = true },
                    HeaderMenuItem("Rate") { rating = true },
                    HeaderMenuItem("Ungroup", danger = true) { confirmUngroup = true }
                ),
                onRate = { rating = true },
                backLabel = "Albums"
            )
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
                rating = a.rating,
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
