package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.data.Stage
import com.songbook.app.data.UiStyle
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/** Everything the song menu can do. The menu itself comes in three looks, the actions are the same. */
class SongMenuActions(
    val onEdit: () -> Unit,
    val onDesk: () -> Unit,
    val onDuplicate: () -> Unit,
    val onMove: (Long?) -> Unit,
    val onCreateAlbumAndMove: (String) -> Unit,
    val onToggleFavorite: () -> Unit,
    val onArtwork: (gradient: Int, image: String?) -> Unit,
    val onBadge: (text: String, color: Int) -> Unit,
    val onStage: (Int) -> Unit,
    val onRate: (Float) -> Unit,
    val onType: (String) -> Unit,
    val onSide: (Int) -> Unit,
    val onUseAlbumCover: () -> Unit,
    val onExport: () -> Unit,
    val onDelete: () -> Unit
)

private enum class MenuSub { None, Move, Artwork, Badge, Stage, Rate, Type, Side, Delete }

private class MenuItem(val label: String, val sub: MenuSub? = null, val run: (() -> Unit)? = null, val danger: Boolean = false)

@Composable
fun SongMenuSheet(
    song: SongEntity,
    albumName: String?,
    albums: List<AlbumEntity>,
    /** Names of the sides of the song's album (empty when it has none). */
    sides: List<String>,
    actions: SongMenuActions,
    onDismiss: () -> Unit
) {
    var sub by remember { mutableStateOf(MenuSub.None) }
    val a = actions

    // sub dialogs are shared by every look
    when (sub) {
        MenuSub.Move -> AlbumPickerDialog(albums, song.albumId, { a.onMove(it); onDismiss() }, { a.onCreateAlbumAndMove(it); onDismiss() }, { sub = MenuSub.None })
        MenuSub.Artwork -> ArtworkDialog(
            song.gradient, song.image, { g, img -> a.onArtwork(g, img); onDismiss() }, { sub = MenuSub.None },
            onReset = if (song.albumId != null) ({ a.onUseAlbumCover(); onDismiss() }) else null
        )
        MenuSub.Side -> ChoiceDialog("Side", sides, song.side.coerceIn(0, (sides.size - 1).coerceAtLeast(0)), { a.onSide(it); onDismiss() }, { sub = MenuSub.None })
        MenuSub.Badge -> BadgeDialog(song.badge, song.badgeColor, { t, col -> a.onBadge(t, col); onDismiss() }, { sub = MenuSub.None })
        MenuSub.Stage -> StageDialog(song.stage, { a.onStage(it); onDismiss() }, { sub = MenuSub.None })
        MenuSub.Rate -> RatingDialog("Rating", song.rating, { a.onRate(it); onDismiss() }, { sub = MenuSub.None })
        MenuSub.Type -> TypeDialog("Type", SongTypes, song.typeName, "Original song", { a.onType(it); onDismiss() }, { sub = MenuSub.None })
        MenuSub.Delete -> ConfirmDialog(
            title = "Delete song?",
            message = "“${song.title}” will be removed permanently.",
            confirmText = "Delete",
            onConfirm = { a.onDelete(); onDismiss() },
            onDismiss = { sub = MenuSub.None }
        )
        MenuSub.None -> Unit
    }

    val items = buildList {
        add(MenuItem("Edit", run = { a.onEdit(); onDismiss() }))
        if (song.stage < Stage.DONE) add(MenuItem("Open Desk", run = { a.onDesk(); onDismiss() }))
        add(MenuItem("Duplicate", run = { a.onDuplicate(); onDismiss() }))
        add(MenuItem("Move to Album", sub = MenuSub.Move))
        if (sides.isNotEmpty()) add(MenuItem("Side", sub = MenuSub.Side))
        add(MenuItem(if (song.favorite) "Remove from Favorites" else "Add to Favorites", run = { a.onToggleFavorite(); onDismiss() }))
        add(MenuItem("Rate", sub = MenuSub.Rate))
        add(MenuItem("Set Type", sub = MenuSub.Type))
        add(MenuItem("Change Artwork", sub = MenuSub.Artwork))
        add(MenuItem("Set Badge", sub = MenuSub.Badge))
        add(MenuItem("Workflow Stage", sub = MenuSub.Stage))
        add(MenuItem("Export", run = { a.onExport(); onDismiss() }))
        add(MenuItem("Delete Song", sub = MenuSub.Delete, danger = true))
    }
    val pick: (MenuItem) -> Unit = { item -> if (item.sub != null) sub = item.sub else item.run?.invoke() }

    if (Sb.style == UiStyle.NEW) SongMenuLifted(song, items, pick, onDismiss)
    else SongMenuBottomSheet(song, albumName, items, pick, onDismiss)
}

// ---- classic and glass: bottom sheet -----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongMenuBottomSheet(
    song: SongEntity,
    albumName: String?,
    items: List<MenuItem>,
    pick: (MenuItem) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.sheet,
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
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Row(
                Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SquareArtwork(song.gradient, 56.dp, 18.dp, image = song.image)
                Column {
                    Text(song.title, style = sbStyle(17, FontWeight.Bold, c.tx), maxLines = 1)
                    Text(albumName ?: "No album", style = sbStyle(13, color = c.mu))
                }
            }
            items.forEach { item ->
                if (item.danger) Box(Modifier.fillMaxWidth().height(1.dp).background(c.ln))
                Row(
                    Modifier.fillMaxWidth().height(50.dp).clickable { pick(item) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.label, style = sbStyle(15, if (item.danger) FontWeight.SemiBold else FontWeight.Medium, if (item.danger) c.ac else c.tx))
                    if (!item.danger) StrokeIcon(SbIcons.ChevronRight, c.mu, size = 20.dp)
                }
            }
        }
    }
}

// ---- new look: the song lifts off the page, actions in a card under it ----------------------

@Composable
private fun SongMenuLifted(
    song: SongEntity,
    items: List<MenuItem>,
    pick: (MenuItem) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(onClick = onDismiss)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                Modifier.align(Alignment.Center).verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SquareArtwork(song.gradient, 112.dp, 22.dp, elevated = true, image = song.image)
                Text(
                    song.title,
                    style = sbStyle(14, FontWeight.Bold, Color.White),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 10.dp, bottom = 14.dp).width(220.dp)
                )
                Column(
                    Modifier
                        .width(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.sheet)
                        .clickable(enabled = false) {}
                ) {
                    items.forEachIndexed { i, item ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.ln))
                        Row(
                            Modifier.fillMaxWidth().height(44.dp).clickable { pick(item) }.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.label, style = sbStyle(14, if (item.danger) FontWeight.Bold else FontWeight.Medium, if (item.danger) c.ac else c.tx))
                            if (!item.danger) StrokeIcon(SbIcons.ChevronRight, c.mu, size = 18.dp)
                        }
                    }
                }
            }
        }
    }
}
