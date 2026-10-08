package com.songbook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.AlbumSort
import com.songbook.app.data.AppSettings
import com.songbook.app.data.ColumnChoices
import com.songbook.app.data.GroupEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.data.UiStyle
import com.songbook.app.data.sortedAlbums
import com.songbook.app.data.sortedGroups
import com.songbook.app.ui.components.AlbumRow
import com.songbook.app.ui.components.ArtTile
import com.songbook.app.ui.components.BlockList
import com.songbook.app.ui.components.Blocks
import com.songbook.app.ui.components.BlocksDialog
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.ChoiceDialog
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.GlassRoundButton
import com.songbook.app.ui.components.GlassTitle
import com.songbook.app.ui.components.MomentsHeader
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
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

/** "Trilogy" -> "Trilogies", "Box set" -> "Box sets", "Series" -> "Series". */
fun pluralOf(word: String): String {
    val w = word.trim()
    if (w.isEmpty()) return "Groups"
    val l = w.lowercase()
    return when {
        l.endsWith("y") && l.length > 1 && l[l.length - 2] !in "aeiou" -> w.dropLast(1) + "ies"
        l.endsWith("s") -> w
        l.endsWith("x") || l.endsWith("ch") || l.endsWith("sh") -> w + "es"
        else -> w + "s"
    }
}

class AlbumsActions(
    val onOpenAlbum: (Long) -> Unit,
    val onOpenGroup: (Long) -> Unit,
    val onCreateAlbum: (String) -> Unit,
    val onCreateGroup: (name: String, albumIds: List<Long>) -> Unit,
    val onOpenWorkflow: () -> Unit,
    val onNavigate: (String) -> Unit,
    val onSettings: () -> Unit,
    val onAlbumSort: (Int) -> Unit,
    val onAlbumColumns: (Int) -> Unit,
    val onGroupColumns: (Int) -> Unit,
    /** "albums" or "groups" */
    val onArrange: (String) -> Unit,
    val onSaveLayout: (String) -> Unit
)

@Composable
fun AlbumsScreen(
    albums: List<AlbumEntity>,
    songs: List<SongEntity>,
    groups: List<GroupEntity>,
    settings: AppSettings,
    workflowCount: Int,
    ideaCount: Int,
    actions: AlbumsActions
) {
    val c = Sb.colors
    val style = Sb.style
    var creating by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var naming by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableIntStateOf(0) }  // 1 sort, 2 album columns, 3 group columns, 4 blocks

    if (creating) {
        TextInputDialog("New album", "Album name", confirmText = "Create",
            onConfirm = { actions.onCreateAlbum(it); creating = false }, onDismiss = { creating = false })
    }
    if (naming) {
        TextInputDialog("Name this group", "Group name (e.g. The Flower Trilogy)", confirmText = "Create",
            onConfirm = { name ->
                actions.onCreateGroup(name, selected.toList())
                naming = false
                selecting = false
                selected = emptySet()
            },
            onDismiss = { naming = false })
    }

    val layout: BlockList = remember(settings.albumsLayout, style) { Blocks.resolve(settings.albumsLayout, Blocks.albums(style)) }
    if (dialog == 1) ChoiceDialog("Sort albums", AlbumSort.names, settings.albumSort, { actions.onAlbumSort(it); dialog = 0 }, { dialog = 0 })
    if (dialog == 2) ChoiceDialog("Albums per row", ColumnChoices, settings.albumColumns - 1, { actions.onAlbumColumns(it + 1); dialog = 0 }, { dialog = 0 })
    if (dialog == 3) ChoiceDialog("Groups per row", ColumnChoices, settings.groupColumns - 1, { actions.onGroupColumns(it + 1); dialog = 0 }, { dialog = 0 })
    if (dialog == 4) {
        BlocksDialog(
            "Albums page", layout,
            onSave = { actions.onSaveLayout(Blocks.encode(it)); dialog = 0 },
            onReset = { actions.onSaveLayout(""); dialog = 0 },
            onDismiss = { dialog = 0 }
        )
    }

    val counts = remember(songs) { songs.groupingBy { it.albumId }.eachCount() }
    val groupIds = remember(groups) { groups.map { it.id }.toSet() }
    // Albums that live inside a group are shown on the group's page instead.
    val loose = remember(albums, groupIds, settings.albumSort) {
        albums.filter { it.groupId == null || it.groupId !in groupIds }.sortedAlbums(settings.albumSort)
    }
    val memberCount = remember(albums) { albums.filter { it.groupId != null }.groupingBy { it.groupId }.eachCount() }
    val covers = remember(albums, groups) {
        groups.associate { g -> g.id to albums.filter { it.groupId == g.id }.sortedAlbums(AlbumSort.MANUAL).mapNotNull { it.image }.take(4) }
    }
    val sortedGroups = remember(groups, settings.albumSort) { groups.sortedGroups(settings.albumSort) }
    val side = if (style == UiStyle.NEW) 16.dp else 24.dp
    val albumCols = settings.albumColumns
    val groupCols = settings.groupColumns

    // the three ways to open the page menu: one dropdown, shown next to whichever button the look has
    val pageMenu: @Composable () -> Unit = {
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.background(c.sheet)) {
            val items: List<Pair<String, () -> Unit>> = listOf(
                "Sort albums" to { dialog = 1 },
                "Albums per row" to { dialog = 2 },
                "Groups per row" to { dialog = 3 },
                "Arrange albums" to { actions.onArrange("albums") },
                "Arrange groups" to { actions.onArrange("groups") },
                "Customize this page" to { dialog = 4 }
            )
            items.forEach { (label, run) ->
                DropdownMenuItem(text = { Text(label, style = sbStyle(14, color = c.tx)) }, onClick = { menu = false; run() })
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(12),
        modifier = Modifier.fillMaxSize().screenBg().statusBarsPadding(),
        contentPadding = PaddingValues(start = side, end = side, bottom = 150.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ---- header, one per look ----
        item(span = { GridItemSpan(maxLineSpan) }) {
            val subtitle = if (selecting) {
                if (selected.isEmpty()) "Pick two or more to group" else "${selected.size} selected"
            } else "Your music, organized your way."
            val actionsRow: @Composable () -> Unit = {
                if (selecting) {
                    ChipPill("Cancel", active = false, onClick = { selecting = false; selected = emptySet() })
                    ChipPill("Group", active = selected.size >= 2, onClick = { if (selected.size >= 2) naming = true })
                } else {
                    if (loose.size >= 2) ChipPill("Select", active = false, onClick = { selecting = true })
                    if (style != UiStyle.NEW) ChipPill("+ New", active = false, onClick = { creating = true })
                    Box {
                        if (style == UiStyle.GLASS) GlassRoundButton(SbIcons.More, "More", { menu = true })
                        else SbIconButton(SbIcons.More, "More", { menu = true }, tint = c.mu)
                        pageMenu()
                    }
                }
            }
            when (style) {
                UiStyle.NEW -> Column(Modifier.fillMaxWidth()) {
                    MomentsHeader(
                        title = if (selecting) "Select albums" else "Albums",
                        onSettings = actions.onSettings,
                        onPlus = { creating = true },
                        trailing = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { actionsRow() } }
                    )
                    TopChips("albums", actions.onNavigate, Modifier.padding(bottom = 4.dp))
                }
                UiStyle.GLASS -> Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    GlassTitle(if (selecting) "Select albums" else "Albums", subtitle, Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { actionsRow() }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { actionsRow() }
                }
            }
        }

        layout.forEach { (id, on) ->
            if (!on) return@forEach
            when (id) {
                "groups" -> if (sortedGroups.isNotEmpty() && !selecting) {
                    // one section per type: all Trilogies together, then the Box sets...  or one custom heading for all
                    val sections: List<Pair<String, List<GroupEntity>>> =
                        if (settings.groupsTitle.isNotBlank()) listOf(settings.groupsTitle.trim() to sortedGroups)
                        else sortedGroups.groupBy { g -> g.typeName.ifBlank { groupKind(memberCount[g.id] ?: 0) }.trim().lowercase() }
                            .values.map { list ->
                                val g = list.first()
                                pluralOf(g.typeName.ifBlank { groupKind(memberCount[g.id] ?: 0) }) to list
                            }
                    sections.forEach { (title, list) ->
                        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(title, Modifier.padding(top = 4.dp)) }
                        items(list, key = { "g${it.id}" }, span = { GridItemSpan(12 / groupCols) }) { g ->
                            val n = memberCount[g.id] ?: 0
                            val kind = g.typeName.ifBlank { groupKind(n) }
                            val collage = covers[g.id] ?: emptyList()
                            if (groupCols == 1) {
                                AlbumRow(
                                    g.gradient, g.image, collage, g.name, "$kind · ${albumCountLabel(n)}",
                                    g.badge, g.badgeColor, g.rating, { actions.onOpenGroup(g.id) }
                                )
                            } else {
                                ArtTile(
                                    gradient = g.gradient, image = g.image, title = g.name,
                                    subtitle = "$kind · ${albumCountLabel(n)}",
                                    initialsText = initials(g.name), badge = g.badge, badgeColor = g.badgeColor,
                                    columns = groupCols, label = kind, rating = g.rating, collage = collage,
                                    onClick = { actions.onOpenGroup(g.id) }
                                )
                            }
                        }
                    }
                }
                "albums" -> {
                    if (sortedGroups.isNotEmpty() && !selecting && loose.isNotEmpty() && layout.firstOrNull { it.first == "groups" }?.second == true) {
                        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Albums", Modifier.padding(top = 4.dp)) }
                    }
                    if (albums.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyHint("No albums yet. Tap “+ New” to create one.") }
                    }
                    items(loose, key = { "a${it.id}" }, span = { GridItemSpan(12 / albumCols) }) { a ->
                        val n = counts[a.id] ?: 0
                        val sub = if (a.typeName.isBlank()) songCountLabel(n) else "${a.typeName} · ${songCountLabel(n)}"
                        val pick: () -> Unit = {
                            if (selecting) selected = if (a.id in selected) selected - a.id else selected + a.id
                            else actions.onOpenAlbum(a.id)
                        }
                        if (albumCols == 1) {
                            AlbumRow(
                                a.gradient, a.image, emptyList(), a.name, sub, a.badge, a.badgeColor, a.rating, pick,
                                selected = if (selecting) a.id in selected else null
                            )
                        } else {
                            ArtTile(
                                gradient = a.gradient, image = a.image, title = a.name, subtitle = sub,
                                initialsText = initials(a.name), badge = a.badge, badgeColor = a.badgeColor,
                                columns = albumCols, label = a.typeName.ifBlank { null }, rating = a.rating,
                                selected = if (selecting) a.id in selected else null, onClick = pick
                            )
                        }
                    }
                }
                "workflow" -> if (style == UiStyle.NEW && !selecting) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        WorkflowEntry(workflowCount, ideaCount, actions.onOpenWorkflow, Modifier.padding(top = 6.dp), horizontal = 0.dp)
                    }
                }
            }
        }
    }
}
