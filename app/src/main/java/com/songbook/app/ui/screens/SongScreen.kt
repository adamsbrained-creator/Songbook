package com.songbook.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.songbook.app.applySystemBars
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.AppSettings
import com.songbook.app.data.SectionStyle
import com.songbook.app.data.SongEntity
import com.songbook.app.data.SongLine
import com.songbook.app.data.SongParser
import com.songbook.app.data.SongTheme
import com.songbook.app.data.Stage
import com.songbook.app.data.Transposer
import com.songbook.app.data.UiStyle
import com.songbook.app.data.serialize
import com.songbook.app.data.replaceLine
import com.songbook.app.data.withChordsAt
import com.songbook.app.data.wordAt
import com.songbook.app.ui.components.BadgePill
import com.songbook.app.ui.components.CapoDialog
import com.songbook.app.ui.components.ChordDialog
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.GlassBackdrop
import com.songbook.app.ui.components.GlassRoundButton
import com.songbook.app.ui.components.HeartButton
import com.songbook.app.ui.components.RatingDialog
import com.songbook.app.ui.components.RatingTag
import com.songbook.app.ui.components.ReadingDialog
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.SbSwitch
import com.songbook.app.ui.components.ScrollSpeedDialog
import com.songbook.app.ui.components.SquareArtwork
import com.songbook.app.ui.components.StarRow
import com.songbook.app.ui.components.StrokeIcon
import com.songbook.app.ui.components.formatLongDate
import com.songbook.app.ui.components.glassSurface
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.components.songType
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.SbColors
import com.songbook.app.ui.theme.SongbookTheme
import com.songbook.app.ui.theme.sbStyle
import kotlin.math.roundToInt

/** Top half of an ellipse, used for the "Edit" button at the bottom of the classic reader. */
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

class ReaderActions(
    val onClose: () -> Unit,
    val onMenu: () -> Unit,
    val onEdit: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onCapo: (capo: Int, adjust: Boolean) -> Unit,
    val onShowTabs: (Boolean) -> Unit,
    val onReading: (Int, Int, Float, Int, Int) -> Unit,
    val onLyricsView: (Boolean) -> Unit,
    val onAutoScroll: (Boolean) -> Unit,
    val onScrollSpeed: (Int) -> Unit,
    val onSongTheme: (Int) -> Unit,
    val onRate: (Float) -> Unit,
    /** The whole song text after a chord was typed on a word. */
    val onBody: (String) -> Unit
)

/**
 * The song page. Its theme follows the app by default, or is forced dark / light with the
 * "Song page" setting. Three layouts: classic, New (player panel) and Glass (floating bar).
 */
@Composable
fun SongScreen(song: SongEntity?, album: AlbumEntity?, settings: AppSettings, appDark: Boolean, actions: ReaderActions) {
    // The song was deleted from the menu: go back.
    LaunchedEffect(song == null) { if (song == null) actions.onClose() }
    if (song == null) return

    val songDark = when (settings.songTheme) {
        SongTheme.DARK -> true
        SongTheme.LIGHT -> false
        else -> appDark
    }
    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(songDark) {
        activity?.applySystemBars(songDark)
        onDispose { activity?.applySystemBars(appDark) }
    }

    SongbookTheme(dark = songDark, accent = settings.accent, accentCustom = settings.accentCustom, style = settings.uiStyle) {
        Box(Modifier.fillMaxSize()) {
            if (settings.uiStyle == UiStyle.GLASS) GlassBackdrop() else Box(Modifier.fillMaxSize().background(Sb.colors.bg))
            SongContent(song, album, settings, actions)
        }
    }
}

@Composable
private fun SongContent(song: SongEntity, album: AlbumEntity?, settings: AppSettings, a: ReaderActions) {
    val c = Sb.colors
    val style = Sb.style
    val lines = remember(song.body) { SongParser.parse(song.body) }
    val hasTabs = remember(song.body) { SongParser.hasTabs(song.body) }
    val shift = if (song.capoAdjust && song.capo > 0) -song.capo else 0
    val tabCapo = if (song.capoAdjust) song.capo else 0
    val songChords = remember(lines) {
        lines.filterIsInstance<SongLine.Lyric>().flatMap { l -> l.chords.map { it.name } }.distinct().take(10)
    }
    val sample = songChords.take(4)
    var readingDialog by remember { mutableStateOf(false) }
    var capoDialog by remember { mutableStateOf(false) }
    var speedDialog by remember { mutableStateOf(false) }
    var rateDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var chordTarget by remember { mutableStateOf<Pair<Int, IntRange>?>(null) }
    val scroll = rememberScrollState()
    val lyricsView = settings.lyricsView

    if (readingDialog) {
        ReadingDialog(
            settings.lyricsSize, settings.chordSize, settings.lineSpacing, settings.chordOffset, settings.sectionSize,
            onCommit = a.onReading, onDismiss = { readingDialog = false },
            songTheme = settings.songTheme, onSongTheme = a.onSongTheme
        )
    }
    if (capoDialog) CapoDialog(song.capo, song.capoAdjust, sample, { cp, adj -> a.onCapo(cp, adj); capoDialog = false }, { capoDialog = false })
    if (speedDialog) ScrollSpeedDialog(settings.scrollSpeed, a.onScrollSpeed, { speedDialog = false })
    if (rateDialog) RatingDialog("Rate song", song.rating, { a.onRate(it); rateDialog = false }, { rateDialog = false })

    // Tap a word: write the chord that is played on it. The chord shows exactly above the first letter of the word.
    chordTarget?.let { (lineIndex, range) ->
        val line = lines.getOrNull(lineIndex) as? SongLine.Lyric
        if (line == null || range.last >= line.text.length) {
            chordTarget = null
        } else {
            val word = line.text.substring(range.first, range.last + 1)
            val here = line.chords.filter { it.offset in range }.map { Transposer.chord(it.name, shift) }
            val save: (String) -> Unit = { typed ->
                // what is typed is the shape that is played; the song keeps the sounding chord
                val names = typed.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { Transposer.chord(it, -shift) }
                val newLine = SongParser.serialize(SongParser.withChordsAt(line, range, names))
                a.onBody(SongParser.replaceLine(song.body, lineIndex, newLine))
                chordTarget = null
            }
            ChordDialog(
                word = word,
                current = here.joinToString(" "),
                suggestions = songChords.map { Transposer.chord(it, shift) },
                note = if (shift != 0) "Capo ${song.capo}: type the shape you play. It is saved as the sounding chord." else null,
                onSave = save,
                onRemove = if (here.isNotEmpty()) ({ save("") }) else null,
                onDismiss = { chordTarget = null }
            )
        }
    }

    // Switching between the Chords and Lyrics pages starts from the top.
    LaunchedEffect(lyricsView) { scroll.scrollTo(0) }

    // Auto scroll: smooth, speed in dp per second, pauses while the finger is on the page, stops at the end.
    val speed by rememberUpdatedState(settings.scrollSpeed)
    val density = LocalDensity.current
    LaunchedEffect(settings.autoScroll) {
        if (!settings.autoScroll) return@LaunchedEffect
        kotlinx.coroutines.delay(1500)
        var last = withFrameNanos { it }
        var carry = 0f
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now
            if (scroll.isScrollInProgress) continue
            val dpPerSecond = 4f + (speed - 1) * 6.2f
            carry += with(density) { dpPerSecond.dp.toPx() } * dt
            val whole = carry.toInt()
            if (whole >= 1) {
                scroll.scrollBy(whole.toFloat())
                carry -= whole
            }
            if (scroll.maxValue > 0 && scroll.value >= scroll.maxValue) {
                a.onAutoScroll(false)
                break
            }
        }
    }

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val chips: @Composable () -> Unit = {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChipPill("Aa", active = false, onClick = { readingDialog = true })
            ChipPill("Chords", active = !lyricsView, onClick = { a.onLyricsView(false) })
            ChipPill("Lyrics", active = lyricsView, onClick = { a.onLyricsView(true) })
            if (!lyricsView) {
                ChipPill(
                    if (song.capo == 0) "No capo" else if (song.capoAdjust) "Capo ${song.capo} · shapes" else "Capo ${song.capo}",
                    active = false, onClick = { capoDialog = true }
                )
                ChipPill("Tab", active = settings.showTabs && hasTabs, onClick = { a.onShowTabs(!settings.showTabs) })
            }
        }
    }
    val infoRow: @Composable () -> Unit = {
        Row(
            Modifier.padding(horizontal = 24.dp).padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SquareArtwork(song.gradient, 60.dp, 18.dp, image = song.image)
            Column(Modifier.weight(1f)) {
                Text(song.title, style = sbStyle(22, FontWeight.Bold, c.tx), maxLines = 2)
                Text("${songType(song.typeName)} · ${album?.name ?: "No album"}", style = sbStyle(12, color = c.mu), modifier = Modifier.padding(top = 2.dp))
                Text("Edited ${formatLongDate(song.updatedAt)}", style = sbStyle(12, color = c.mu))
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgePill(song.badge, song.badgeColor)
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).clickable { rateDialog = true }.padding(vertical = 2.dp)) {
                        if (song.rating > 0f) StarRow(song.rating, starSize = 13.dp, gap = 2.dp, color = c.tx)
                        else Text("Rate", style = sbStyle(11, FontWeight.Medium, c.mu))
                    }
                }
            }
            HeartButton(song.favorite, a.onToggleFavorite)
        }
    }
    val onWord: (Int, IntRange) -> Unit = { lineIndex, range -> chordTarget = lineIndex to range }
    val card: @Composable ColumnScope.(Modifier) -> Unit = { m ->
        Box(m.fillMaxWidth().weight(1f).sbCard(28.dp).clip(RoundedCornerShape(28.dp))) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 4.dp)) {
                if (lyricsView) PlainLyrics(lines, settings, c)
                else Column(Modifier.horizontalScroll(rememberScrollState())) { SongBody(lines, settings, c, shift, tabCapo, onWord) }
                Box(Modifier.height(24.dp))
            }
            // bigger window: only the lyrics fill the screen
            Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp).size(34.dp).clip(CircleShape).background(c.track)
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) { StrokeIcon(if (expanded) SbIcons.Collapse else SbIcons.Expand, c.mu, size = 18.dp) }
        }
    }

    if (expanded) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            card(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp))
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
            ) {
                ToolButton("−") { a.onScrollSpeed((settings.scrollSpeed - 1).coerceAtLeast(1)) }
                ToolButton(null, if (settings.autoScroll) SbIcons.Pause else SbIcons.Play) { a.onAutoScroll(!settings.autoScroll) }
                ToolButton("+") { a.onScrollSpeed((settings.scrollSpeed + 1).coerceAtMost(10)) }
                ToolButton("Aa") { readingDialog = true }
                ToolButton(null, SbIcons.Collapse) { expanded = false }
            }
        }
    } else when (style) {
        // ------------------------------------------------------------------ NEW: player panel
        UiStyle.NEW -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SbIconButton(SbIcons.ChevronDown, "Close", a.onClose, size = 24.dp)
                Text("Now Reading", style = sbStyle(16, FontWeight.SemiBold, c.ac))
                SbIconButton(SbIcons.More, "More", a.onMenu)
            }
            chips()
            card(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
            val sections = remember(lines) { lines.filterIsInstance<SongLine.Section>().map { it.label } }
            Column(
                Modifier.padding(16.dp).navigationBarsPadding().fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(panelBrush(song.gradient))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = sbStyle(16, FontWeight.Bold, Color.White), maxLines = 1)
                        Text(
                            if (settings.autoScroll) "Auto Scroll · speed ${settings.scrollSpeed}/10" else "${songType(song.typeName)} · ${Stage.name(song.stage)}",
                            style = sbStyle(12, color = Color.White.copy(alpha = 0.8f)),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { speedDialog = true }
                        )
                    }
                    BadgePill(song.badge, song.badgeColor, solid = true)
                    if (song.rating > 0f) Box(Modifier.padding(start = 8.dp)) { RatingTag(song.rating, color = Color.White) }
                }
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sections.firstOrNull() ?: "Start", style = sbStyle(10, color = Color.White.copy(alpha = 0.8f)), maxLines = 1)
                    val frac = if (scroll.maxValue > 0) scroll.value / scroll.maxValue.toFloat() else 0f
                    Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxWidth(frac.coerceIn(0.02f, 1f)).height(4.dp).background(Color.White, CircleShape))
                    }
                    Text(sections.lastOrNull()?.takeIf { sections.size > 1 } ?: "End", style = sbStyle(10, color = Color.White.copy(alpha = 0.8f)), maxLines = 1)
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    PlayerButton("−", { a.onScrollSpeed((settings.scrollSpeed - 1).coerceAtLeast(1)) })
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)).clickable { a.onAutoScroll(!settings.autoScroll) },
                        contentAlignment = Alignment.Center
                    ) { StrokeIcon(if (settings.autoScroll) SbIcons.Pause else SbIcons.Play, Color.White, size = 28.dp, filled = true) }
                    PlayerButton("+", { a.onScrollSpeed((settings.scrollSpeed + 1).coerceAtMost(10)) })
                    if (settings.showEditButton) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable(onClick = a.onEdit), contentAlignment = Alignment.Center) {
                            StrokeIcon(SbIcons.Edit, Color.White, size = 20.dp)
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------------ GLASS: floating bar
        UiStyle.GLASS -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                GlassRoundButton(SbIcons.ChevronDown, "Close", a.onClose)
                Text("Now Reading", style = sbStyle(15, FontWeight.SemiBold, c.ac))
                GlassRoundButton(SbIcons.More, "More", a.onMenu)
            }
            infoRow()
            chips()
            card(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding().fillMaxWidth().height(60.dp)
                    .glassSurface(RoundedCornerShape(30.dp), strong = true).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically
            ) {
                BarItem(if (settings.autoScroll) SbIcons.Pause else SbIcons.Play, "Auto Scroll", settings.autoScroll) { a.onAutoScroll(!settings.autoScroll) }
                BarItem(null, "Speed", false, "${settings.scrollSpeed}/10") { speedDialog = true }
                BarItem(null, "Size", false, "${settings.lyricsSize} pt") { readingDialog = true }
                BarItem(null, "Capo", song.capo > 0, if (song.capo == 0) "–" else "${song.capo}") { capoDialog = true }
                if (settings.showEditButton) BarItem(SbIcons.Edit, "Edit", false) { a.onEdit() }
            }
        }

        // ------------------------------------------------------------------ OLD
        else -> {
            val bottomPad = if (settings.showEditButton) 70.dp + navInset else 16.dp + navInset
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        SbIconButton(SbIcons.ChevronDown, "Close", a.onClose, size = 24.dp)
                        Text("Now Reading", style = sbStyle(16, FontWeight.SemiBold, c.ac))
                        SbIconButton(SbIcons.More, "More", a.onMenu)
                    }
                    infoRow()
                    chips()
                    // Auto scroll: the switch turns it on; once on, the label opens the speed card.
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp).height(36.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val on = settings.autoScroll
                        Row(
                            Modifier.clip(RoundedCornerShape(10.dp)).then(if (on) Modifier.clickable { speedDialog = true } else Modifier).padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Auto Scroll", style = sbStyle(13, FontWeight.Medium, if (on) c.ac else c.mu))
                            if (on) {
                                Text("· speed ${settings.scrollSpeed}", style = sbStyle(12, color = c.mu))
                                StrokeIcon(SbIcons.ChevronRight, c.ac, size = 16.dp)
                            }
                        }
                        SbSwitch(on) { a.onAutoScroll(it) }
                    }
                    card(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = bottomPad))
                }
                if (settings.showEditButton) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).padding(horizontal = 75.dp).fillMaxWidth().height(64.dp + navInset)
                            .clip(DomeShape).background(c.ac).clickable(onClick = a.onEdit).padding(top = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) { Text("Edit", style = sbStyle(16, FontWeight.SemiBold, Color.White)) }
                }
            }
        }
    }
}

/** The song's own two colors, darkened a little so white text on the player panel stays readable. */
private fun panelBrush(index: Int): Brush {
    val g = com.songbook.app.ui.theme.SongGradients
    val pair = g[((index % g.size) + g.size) % g.size]
    return Brush.linearGradient(pair.map { androidx.compose.ui.graphics.lerp(it, Color.Black, 0.28f) })
}

/** Round button of the full screen reader: a short text or an icon. */
@Composable
private fun ToolButton(text: String?, icon: com.songbook.app.ui.components.IconSpec? = null, onClick: () -> Unit) {
    val c = Sb.colors
    val glass = Sb.style == UiStyle.GLASS
    Box(
        Modifier.size(48.dp).let { if (glass) it.glassSurface(CircleShape, strong = true) else it.background(c.cd, CircleShape) }
            .clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) StrokeIcon(icon, c.tx, size = 22.dp, filled = icon == SbIcons.Play || icon == SbIcons.Pause)
        else Text(text ?: "", style = sbStyle(16, FontWeight.Bold, c.tx))
    }
}

@Composable
private fun PlayerButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, style = sbStyle(22, FontWeight.Bold, Color.White)) }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BarItem(
    icon: com.songbook.app.ui.components.IconSpec?,
    label: String,
    on: Boolean,
    value: String? = null,
    onClick: () -> Unit
) {
    val c = Sb.colors
    val tint = if (on) c.ac else c.tx
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) StrokeIcon(icon, tint, size = 22.dp, filled = on && label == "Auto Scroll")
        else Text(value ?: "", style = sbStyle(15, FontWeight.Bold, tint))
        Text(label, style = sbStyle(9, FontWeight.Medium, c.mu), maxLines = 1)
    }
}

// ---- section headings ----------------------------------------------------------------------

@Composable
private fun SectionHeading(label: String, style: Int, size: Int, first: Boolean, c: SbColors) {
    val text = when (style) {
        SectionStyle.LOWER -> label.lowercase()
        SectionStyle.AS_WRITTEN -> label
        else -> label.uppercase()
    }
    val textStyle = if (style == SectionStyle.UPPER) {
        sbStyle(size, FontWeight.SemiBold, c.ac).copy(letterSpacing = 0.14.em)
    } else {
        sbStyle(size, FontWeight.SemiBold, c.ac).copy(letterSpacing = 0.02.em)
    }
    Text(text, style = textStyle, modifier = Modifier.padding(top = if (first) 14.dp else 18.dp, bottom = 8.dp))
}

// ---- Chords page ---------------------------------------------------------------------------

@Composable
private fun SongBody(
    lines: List<SongLine>,
    settings: AppSettings,
    c: SbColors,
    shift: Int,
    tabCapo: Int,
    onWord: (Int, IntRange) -> Unit
) {
    var first = true
    lines.forEachIndexed { index, line ->
        when (line) {
            is SongLine.Section -> SectionHeading(line.label, settings.sectionStyle, settings.sectionSize, first, c)
            is SongLine.Lyric -> ChordLyricLine(line, settings, c, shift) { range -> onWord(index, range) }
            is SongLine.Tab -> if (settings.showTabs) {
                Text(Transposer.tabLine(line.text, tabCapo), style = sbStyle(13, FontWeight.Medium, c.tx, mono = true), softWrap = false, modifier = Modifier.padding(bottom = 2.dp))
            }
            SongLine.Blank -> Box(Modifier.height(10.dp))
        }
        first = false
    }
}

/**
 * Lyric line with chords above the exact letter they belong to. The positions come from the chords as they
 * are written, so a capo never moves a chord to another word: only the names change. Tapping a word reports it.
 */
@Composable
private fun ChordLyricLine(line: SongLine.Lyric, settings: AppSettings, c: SbColors, shift: Int, onTapWord: (IntRange) -> Unit) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // extra line height goes below the text, so the lyric sits right under its chords
    val tight = LineHeightStyle(alignment = LineHeightStyle.Alignment.Top, trim = LineHeightStyle.Trim.None)
    val lyricStyle = sbStyle(settings.lyricsSize, FontWeight.Medium, c.tx)
        .copy(lineHeight = (settings.lyricsSize * settings.lineSpacing).sp, lineHeightStyle = tight)
    val chordStyle = sbStyle(settings.chordSize, FontWeight.Medium, c.ac, mono = true)
        .copy(lineHeight = (settings.chordSize * 1.2f).sp, lineHeightStyle = tight)
    val showChords = settings.showChords && line.chords.isNotEmpty()
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    if (!showChords && line.text.isBlank()) return

    Column(Modifier.padding(bottom = 8.dp)) {
        if (showChords) {
            val placed = remember(line, settings.lyricsSize, settings.chordSize, shift) {
                val layout = measurer.measure(AnnotatedString(line.text), lyricStyle, softWrap = false, maxLines = 1)
                val gap = with(density) { 8.dp.toPx() }
                var minX = 0f
                line.chords.map { m ->
                    val x = layout.getHorizontalPosition(m.offset.coerceIn(0, line.text.length), true)
                    // room is measured with the chord as written, the capo version is drawn at the same spot
                    val w = measurer.measure(AnnotatedString(m.name), chordStyle).size.width
                    val px = maxOf(x, minX)
                    minX = px + w + gap
                    Transposer.chord(m.name, shift) to px
                }
            }
            // "Chord distance" setting: positive pushes the chords away from the lyric, negative pulls them closer.
            val boxHeight = (settings.chordSize * 1.2f + 2f + settings.chordOffset).coerceAtLeast(settings.chordSize * 0.9f)
            Box(Modifier.height(boxHeight.dp)) {
                placed.forEach { (name, px) ->
                    Text(name, style = chordStyle, softWrap = false, maxLines = 1, modifier = Modifier.offset { IntOffset(px.roundToInt(), 0) })
                }
            }
        }
        if (line.text.isNotBlank()) {
            Text(
                line.text, style = lyricStyle, softWrap = false, maxLines = 1,
                onTextLayout = { layoutResult = it },
                modifier = Modifier.pointerInput(line.text) {
                    detectTapGestures { pos ->
                        val lr = layoutResult ?: return@detectTapGestures
                        SongParser.wordAt(line.text, lr.getOffsetForPosition(pos))?.let(onTapWord)
                    }
                }
            )
        } else Box(Modifier.height(4.dp))
    }
}

// ---- Lyrics page (plain text: no chords, no tabs, long lines wrap) -------------------------

@Composable
private fun PlainLyrics(lines: List<SongLine>, settings: AppSettings, c: SbColors) {
    val style = sbStyle(settings.lyricsSize, FontWeight.Medium, c.tx).copy(lineHeight = (settings.lyricsSize * settings.lineSpacing).sp)
    var first = true
    var lastWasBlank = true
    lines.forEach { line ->
        when (line) {
            is SongLine.Section -> {
                SectionHeading(line.label, settings.sectionStyle, settings.sectionSize, first, c)
                lastWasBlank = false
                first = false
            }
            is SongLine.Lyric -> {
                val text = line.text.trim()
                if (text.isNotEmpty()) {
                    Text(text, style = style, modifier = Modifier.padding(bottom = 4.dp))
                    lastWasBlank = false
                    first = false
                }
            }
            is SongLine.Tab -> Unit
            SongLine.Blank -> if (!lastWasBlank) {
                Box(Modifier.height(12.dp))
                lastWasBlank = true
            }
        }
    }
}
