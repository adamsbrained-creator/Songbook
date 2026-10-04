package com.songbook.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.displayName
import com.songbook.app.data.readText
import com.songbook.app.ui.components.AlbumPickerDialog
import com.songbook.app.ui.components.ArtworkDialog
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.SbDialog
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.SquareArtwork
import com.songbook.app.ui.components.StrokeIcon
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle
import kotlinx.coroutines.launch

/** Opens the sheet; [albumId] pre-selects the album (used by "Add Song" on the Album page). */
data class NewSongRequest(val albumId: Long? = null)

class NewSongInput(
    val title: String,
    val albumId: Long?,
    val genre: String,
    val tags: String,
    val gradient: Int,
    val body: String
)

private val Templates = listOf(
    "Verse + Chorus" to "# Verse 1\n\n\n# Chorus\n\n",
    "Verse, Chorus, Bridge" to "# Verse 1\n\n\n# Chorus\n\n\n# Verse 2\n\n\n# Bridge\n\n\n# Chorus\n\n",
    "Blank page" to ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSongSheet(
    request: NewSongRequest,
    albums: List<AlbumEntity>,
    onDismiss: () -> Unit,
    onCreate: (NewSongInput) -> Unit,
    onCreateAlbum: (String, (Long) -> Unit) -> Unit
) {
    val c = Sb.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tab by remember { mutableIntStateOf(0) } // 0 New Song, 1 From Template, 2 Import
    var title by remember { mutableStateOf("") }
    var albumId by remember { mutableStateOf(request.albumId) }
    var genre by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var gradient by remember { mutableIntStateOf(0) }
    var template by remember { mutableIntStateOf(0) }
    var importedBody by remember { mutableStateOf<String?>(null) }
    var importedName by remember { mutableStateOf<String?>(null) }

    var pickAlbum by remember { mutableStateOf(false) }
    var pickArtwork by remember { mutableStateOf(false) }
    var pickTemplate by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importedBody = context.readText(uri)
            importedName = context.displayName(uri)
            if (title.isBlank()) title = importedName?.substringBeforeLast('.').orEmpty()
        }
    }

    if (pickAlbum) {
        AlbumPickerDialog(
            albums, albumId,
            onPick = { albumId = it; pickAlbum = false },
            onCreateAndPick = { name -> onCreateAlbum(name) { id -> albumId = id }; pickAlbum = false },
            onDismiss = { pickAlbum = false }
        )
    }
    if (pickArtwork) ArtworkDialog(gradient, { gradient = it; pickArtwork = false }, { pickArtwork = false })
    if (pickTemplate) {
        SbDialog("Template", { pickTemplate = false }) {
            Column {
                Templates.forEachIndexed { i, (name, _) ->
                    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable { template = i; pickTemplate = false }, verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = sbStyle(15, if (i == template) FontWeight.SemiBold else FontWeight.Medium, if (i == template) c.ac else c.tx))
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.cd,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 14.dp).height(5.dp).fillMaxWidth(0.11f).background(c.ln, RoundedCornerShape(3.dp)))
        }
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)
        ) {
            Text("New Song", style = sbStyle(26, FontWeight.Bold, c.ac))
            Text("Create something new.", style = sbStyle(14, color = c.mu), modifier = Modifier.padding(top = 2.dp))

            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("New Song", "From Template", "Import").forEachIndexed { i, label ->
                    ChipPill(label, tab == i, { tab = i }, inactiveBg = c.bg)
                }
            }

            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldRow("Song title") { HintField(title, { title = it }, "Untitled") }
                if (tab == 1) {
                    FieldRow("Template", onClick = { pickTemplate = true }) { Value(Templates[template].first) }
                }
                if (tab == 2) {
                    FieldRow("File", onClick = { importLauncher.launch(arrayOf("text/plain", "*/*")) }) {
                        Value(importedName ?: "Choose")
                    }
                }
                FieldRow("Album", onClick = { pickAlbum = true }) {
                    Value(albums.firstOrNull { it.id == albumId }?.name ?: "Choose")
                }
                FieldRow("Genre") { HintField(genre, { genre = it }, "Indie folk") }
                FieldRow("Tags") { HintField(tags, { tags = it }, "Add tags") }
                FieldRow("Artwork", onClick = { pickArtwork = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SquareArtwork(gradient, 30.dp, 9.dp)
                        StrokeIcon(SbIcons.ChevronRight, c.tx, size = 20.dp)
                    }
                }
            }

            Box(
                Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(c.ac)
                    .clickable {
                        val body = when (tab) {
                            1 -> Templates[template].second
                            2 -> importedBody.orEmpty()
                            else -> ""
                        }
                        val input = NewSongInput(
                            title = title.trim().ifEmpty { "Untitled" },
                            albumId = albumId,
                            genre = genre.trim(),
                            tags = tags.trim(),
                            gradient = gradient,
                            body = body
                        )
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onCreate(input) }
                    },
                contentAlignment = Alignment.Center
            ) { Text("Create Song", style = sbStyle(16, FontWeight.SemiBold, Color.White)) }
        }
    }
}

@Composable
private fun FieldRow(label: String, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    val c = Sb.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = sbStyle(14, color = c.tx))
        Box(Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterEnd) { trailing() }
    }
}

@Composable
private fun Value(text: String) {
    Text(text, style = sbStyle(14, color = Sb.colors.mu), maxLines = 1)
}

@Composable
private fun HintField(value: String, onChange: (String) -> Unit, hint: String) {
    val c = Sb.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = sbStyle(14, color = c.tx).copy(textAlign = TextAlign.End),
        cursorBrush = SolidColor(c.ac),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterEnd) {
                if (value.isEmpty()) Text(hint, style = sbStyle(14, color = c.mu), textAlign = TextAlign.End)
                inner()
            }
        }
    )
}
