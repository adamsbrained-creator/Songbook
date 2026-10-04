package com.songbook.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import com.songbook.app.applySystemBars
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.AppSettings
import com.songbook.app.data.SongEntity
import com.songbook.app.data.SongLine
import com.songbook.app.data.SongParser
import com.songbook.app.ui.components.CapoDialog
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.HeartButton
import com.songbook.app.ui.components.ReadingDialog
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.SquareArtwork
import com.songbook.app.ui.components.formatLongDate
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.SbColors
import com.songbook.app.ui.theme.SongbookTheme
import com.songbook.app.ui.theme.sbStyle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Top half of an ellipse, used for the "Edit" button at the bottom of the reader. */
private object DomeShape : Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(0f, size.height)
            arcTo(Rect(0f, 0f, size.width, size.height * 2f), 180f, 180f, false)
            close()
        }
        return Outline.Generic(path)
    }
}

/** The song page is dark in the mockup, whatever the app theme is. */
@Composable
fun SongScreen(
    song: SongEntity?,
    album: AlbumEntity?,
    settings: AppSettings,
    appDark: Boolean,
    onClose: () -> Unit,
    onMenu: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCapo: (Int) -> Unit,
    onShowChords: (Boolean) -> Unit,
    onShowTabs: (Boolean) -> Unit,
    onReading: (Int, Int, Float) -> Unit
) {
    // The song was deleted from the menu: go back.
    LaunchedEffect(song == null) { if (song == null) onClose() }
    if (song == null) return

    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(Unit) {
        activity?.applySystemBars(true)
        onDispose { activity?.applySystemBars(appDark) }
    }

    SongbookTheme(dark = true, accent = settings.accent) {
        SongContent(song, album, settings, onClose, onMenu, onEdit, onToggleFavorite, onCapo, onShowChords, onShowTabs, onReading)
    }
}

@Composable
private fun SongContent(
    song: SongEntity,
    album: AlbumEntity?,
    settings: AppSettings,
    onClose: () -> Unit,
    onMenu: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCapo: (Int) -> Unit,
    onShowChords: (Boolean) -> Unit,
    onShowTabs: (Boolean) -> Unit,
    onReading: (Int, Int, Float) -> Unit
) {
    val c = Sb.colors
    val lines = remember(song.body) { SongParser.parse(song.body) }
    val hasTabs = remember(song.body) { SongParser.hasTabs(song.body) }
    var readingDialog by remember { mutableStateOf(false) }
    var capoDialog by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (readingDialog) ReadingDialog(settings.lyricsSize, settings.chordSize, settings.lineSpacing, onReading, { readingDialog = false })
    if (capoDialog) CapoDialog(song.capo, { onCapo(it); capoDialog = false }, { capoDialog = false })

    LaunchedEffect(settings.autoScroll) {
        if (settings.autoScroll) {
            while (true) {
                delay(40)
                if (!scroll.isScrollInProgress) scroll.scrollBy(1f)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SbIconButton(SbIcons.ChevronDown, "Close", onClose, size = 24.dp)
                Text("Now Reading", style = sbStyle(16, FontWeight.SemiBold, c.ac))
                SbIconButton(SbIcons.More, "More", onMenu)
            }
            Row(
                Modifier.padding(horizontal = 24.dp).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SquareArtwork(song.gradient, 60.dp, 18.dp)
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = sbStyle(22, FontWeight.Bold, c.tx), maxLines = 2)
                    Text("Original song · ${album?.name ?: "No album"}", style = sbStyle(12, color = c.mu), modifier = Modifier.padding(top = 2.dp))
                    Text("Edited ${formatLongDate(song.updatedAt)}", style = sbStyle(12, color = c.mu))
                }
                HeartButton(song.favorite, onToggleFavorite)
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipPill("Aa", active = false, onClick = { readingDialog = true })
                ChipPill("Chords", active = settings.showChords, onClick = { onShowChords(!settings.showChords) })
                ChipPill(if (song.capo == 0) "No capo" else "Capo ${song.capo}", active = false, onClick = { capoDialog = true })
                ChipPill("Tab", active = settings.showTabs && hasTabs, onClick = { onShowTabs(!settings.showTabs) })
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 70.dp + navInset)
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.cd)
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Column(Modifier.horizontalScroll(rememberScrollState())) {
                        SongBody(lines, settings, c)
                    }
                    Box(Modifier.height(24.dp))
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 75.dp)
                .fillMaxWidth()
                .height(64.dp + navInset)
                .clip(DomeShape)
                .background(c.ac)
                .clickable(onClick = onEdit)
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text("Edit", style = sbStyle(16, FontWeight.SemiBold, Color.White))
        }
    }
}

@Composable
private fun SongBody(lines: List<SongLine>, settings: AppSettings, c: SbColors) {
    var first = true
    lines.forEach { line ->
        when (line) {
            is SongLine.Section -> {
                Text(
                    line.label,
                    style = sbStyle(11, FontWeight.SemiBold, c.ac).copy(letterSpacing = 0.14.em),
                    modifier = Modifier.padding(top = if (first) 14.dp else 18.dp, bottom = 8.dp)
                )
            }
            is SongLine.Lyric -> ChordLyricLine(line, settings, c)
            is SongLine.Tab -> if (settings.showTabs) {
                Text(line.text, style = sbStyle(13, FontWeight.Medium, c.tx, mono = true), softWrap = false, modifier = Modifier.padding(bottom = 2.dp))
            }
            SongLine.Blank -> Box(Modifier.height(10.dp))
        }
        first = false
    }
}

/** Lyric line with chords positioned above the exact character they belong to. */
@Composable
private fun ChordLyricLine(line: SongLine.Lyric, settings: AppSettings, c: SbColors) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val lyricStyle = sbStyle(settings.lyricsSize, FontWeight.Medium, c.tx).copy(lineHeight = (settings.lyricsSize * settings.lineSpacing).sp)
    val chordStyle = sbStyle(settings.chordSize, FontWeight.Medium, c.ac, mono = true)
    val showChords = settings.showChords && line.chords.isNotEmpty()

    if (!showChords && line.text.isBlank()) return

    Column(Modifier.padding(bottom = 8.dp)) {
        if (showChords) {
            val placed = remember(line, settings.lyricsSize, settings.chordSize) {
                val layout = measurer.measure(AnnotatedString(line.text), lyricStyle, softWrap = false, maxLines = 1)
                val gap = with(density) { 8.dp.toPx() }
                var minX = 0f
                line.chords.map { m ->
                    val x = layout.getHorizontalPosition(m.offset.coerceIn(0, line.text.length), true)
                    val w = measurer.measure(AnnotatedString(m.name), chordStyle).size.width
                    val px = maxOf(x, minX)
                    minX = px + w + gap
                    m.name to px
                }
            }
            Box(Modifier.height((settings.chordSize * 1.6f).dp)) {
                placed.forEach { (name, px) ->
                    Text(name, style = chordStyle, softWrap = false, maxLines = 1, modifier = Modifier.offset { IntOffset(px.roundToInt(), 0) })
                }
            }
        }
        if (line.text.isNotBlank()) {
            Text(line.text, style = lyricStyle, softWrap = false, maxLines = 1)
        } else {
            Box(Modifier.height(4.dp))
        }
    }
}
