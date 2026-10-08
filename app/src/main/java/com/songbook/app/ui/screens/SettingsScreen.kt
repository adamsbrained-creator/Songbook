package com.songbook.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AppSettings
import com.songbook.app.data.AlbumSort
import com.songbook.app.data.BackupFile
import com.songbook.app.data.BackupInfo
import com.songbook.app.data.ColumnChoices
import com.songbook.app.data.SongSort
import com.songbook.app.data.SongsLayout
import com.songbook.app.data.TrackSort
import com.songbook.app.data.readBytes
import com.songbook.app.data.writeBytes
import com.songbook.app.data.ThemeMode
import com.songbook.app.data.UiStyle
import com.songbook.app.data.displayName
import com.songbook.app.data.readText
import com.songbook.app.data.writeText
import com.songbook.app.ui.AppViewModel
import com.songbook.app.ui.components.AccentDialog
import com.songbook.app.ui.components.AppIconArt
import com.songbook.app.ui.components.Blocks
import com.songbook.app.ui.components.BlocksDialog
import com.songbook.app.ui.components.ChoiceDialog
import com.songbook.app.ui.components.GlassTitle
import com.songbook.app.ui.components.MutedValue
import com.songbook.app.ui.components.ReadingDialog
import com.songbook.app.ui.components.SbDialog
import com.songbook.app.ui.components.SbSwitch
import com.songbook.app.ui.components.ScrollSpeedDialog
import com.songbook.app.ui.components.Segmented
import com.songbook.app.ui.components.SettingsGroup
import com.songbook.app.ui.components.SettingsRow
import com.songbook.app.ui.components.chordOffsetLabel
import com.songbook.app.ui.components.greetingLine
import com.songbook.app.ui.components.screenBg
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.accentColor
import com.songbook.app.ui.theme.sbStyle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val APP_VERSION = "2.5"

private val GreetingNames = listOf("Hello", "Hi", "Hey", "Welcome back", "By time of day", "Custom")
private val TopAlbumChoices = listOf(0, 2, 4, 6, 8)

private fun backupVersionName(v: Int) = when (v) { 1 -> "1.0"; 2 -> "1.5"; 3 -> "2.0"; else -> "2.5" }

@Composable
fun SettingsScreen(settings: AppSettings, vm: AppViewModel, onReplayIntro: () -> Unit) {
    val c = Sb.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var readingDialog by remember { mutableStateOf(false) }
    var speedDialog by remember { mutableStateOf(false) }
    var nameDialog by remember { mutableStateOf(false) }
    var greetingDialog by remember { mutableStateOf(false) }
    var accentDialog by remember { mutableStateOf(false) }
    var restore by remember { mutableStateOf<Pair<BackupInfo, BackupFile>?>(null) }
    var backupDialog by remember { mutableStateOf(false) }
    var titleDialog by remember { mutableStateOf(false) }
    // 1 songs layout, 2 album columns, 3 group columns, 4 tracks layout, 5 favorites layout, 6 song sort, 7 album sort, 8 track sort
    var choice by remember { mutableIntStateOf(0) }
    // 1 main page blocks, 2 albums page blocks
    var blocks by remember { mutableIntStateOf(0) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            val ok = context.writeBytes(uri, vm.backupZip())
            toast(if (ok) "Backup with pictures saved" else "Could not save backup")
        }
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = context.writeText(uri, vm.backupJson())
            toast(if (ok) "Library backup saved" else "Could not save backup")
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val file = context.readBytes(uri)?.let { vm.openBackup(it) }
            val info = file?.let { vm.previewBackup(it) }
            if (file == null || info == null) toast("That file isn't a Songbook backup") else restore = info to file
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            val ok = context.writeText(uri, vm.exportText())
            toast(if (ok) "Songs exported" else "Could not export songs")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = context.readText(uri)
            if (text == null) toast("Could not read that file") else {
                val n = runCatching { vm.importData(text, context.displayName(uri)) }.getOrElse { -1 }
                toast(if (n < 0) "That file isn't a valid backup" else if (n == 1) "Imported 1 song" else "Imported $n songs")
            }
        }
    }

    if (readingDialog) {
        ReadingDialog(
            settings.lyricsSize, settings.chordSize, settings.lineSpacing, settings.chordOffset, settings.sectionSize,
            onCommit = { l, ch, s, off, sec -> vm.setReading(l, ch, s, off, sec) },
            onDismiss = { readingDialog = false }
        )
    }
    if (speedDialog) ScrollSpeedDialog(settings.scrollSpeed, { v -> vm.edit { setScrollSpeed(v) } }, { speedDialog = false })
    if (nameDialog) NameDialog(settings.userName, { nameDialog = false }) { n -> vm.edit { setUserName(n) }; nameDialog = false }
    if (greetingDialog) {
        GreetingDialog(settings, { greetingDialog = false }) { style, custom ->
            vm.edit { setGreeting(style, custom) }
            greetingDialog = false
        }
    }
    if (accentDialog) {
        AccentDialog(
            settings,
            onPreset = { i -> vm.setAccent(i) },
            onCustom = { argb -> vm.edit { setCustomAccent(argb) } },
            onDismiss = { accentDialog = false }
        )
    }
    restore?.let { (info, file) ->
        RestoreDialog(info, { restore = null }) { replace, withSettings ->
            restore = null
            scope.launch {
                val r = runCatching { vm.restoreBackup(file, replace, withSettings) }.getOrNull()
                toast(
                    if (r == null) "Could not restore that backup"
                    else "Restored ${r.songs} songs, ${r.albums} albums, ${r.groups} groups" + if (r.skipped > 0) " (${r.skipped} already there)" else ""
                )
            }
        }
    }

    if (backupDialog) {
        BackupDialog(
            onDismiss = { backupDialog = false },
            onZip = { backupDialog = false; zipLauncher.launch("songbook-backup.zip") },
            onJson = { backupDialog = false; jsonLauncher.launch("songbook-library.json") }
        )
    }
    if (titleDialog) {
        TitleDialog(settings.groupsTitle, { titleDialog = false }) { t -> vm.edit { setGroupsTitle(t) }; titleDialog = false }
    }
    val trackIdx = { v: Int -> when (v) { 2 -> 1; 3 -> 2; else -> 0 } }
    val gridNames = listOf("List", "Grid, 2 columns", "Grid, 3 columns")
    val gridValue = { i: Int -> if (i == 0) 0 else i + 1 }
    when (choice) {
        1 -> ChoiceDialog("Songs on the main page", SongsLayout.names, settings.songsLayout, { v -> vm.edit { setSongsLayout(v) }; choice = 0 }, { choice = 0 })
        2 -> ChoiceDialog("Albums per row", ColumnChoices, settings.albumColumns - 1, { v -> vm.edit { setAlbumColumns(v + 1) }; choice = 0 }, { choice = 0 })
        3 -> ChoiceDialog("Groups per row", ColumnChoices, settings.groupColumns - 1, { v -> vm.edit { setGroupColumns(v + 1) }; choice = 0 }, { choice = 0 })
        4 -> ChoiceDialog("Songs inside an album", gridNames, trackIdx(settings.trackLayout), { v -> vm.edit { setTrackLayout(gridValue(v)) }; choice = 0 }, { choice = 0 })
        5 -> ChoiceDialog("Favorites page", gridNames, trackIdx(settings.favLayout), { v -> vm.edit { setFavLayout(gridValue(v)) }; choice = 0 }, { choice = 0 })
        6 -> ChoiceDialog("Sort songs", SongSort.names, settings.songSort, { v -> vm.edit { setSongSort(v) }; choice = 0 }, { choice = 0 })
        7 -> ChoiceDialog("Sort albums and groups", AlbumSort.names, settings.albumSort, { v -> vm.edit { setAlbumSort(v) }; choice = 0 }, { choice = 0 })
        8 -> ChoiceDialog("Sort songs inside an album", TrackSort.names, settings.trackSort, { v -> vm.edit { setTrackSort(v) }; choice = 0 }, { choice = 0 })
    }
    if (blocks == 1) {
        BlocksDialog(
            "Main page", Blocks.resolve(settings.homeLayout, Blocks.home(settings.uiStyle)),
            onSave = { vm.edit { setHomeLayout(Blocks.encode(it)) }; blocks = 0 },
            onReset = { vm.edit { setHomeLayout("") }; blocks = 0 },
            onDismiss = { blocks = 0 }
        )
    }
    if (blocks == 2) {
        BlocksDialog(
            "Albums page", Blocks.resolve(settings.albumsLayout, Blocks.albums(settings.uiStyle)),
            onSave = { vm.edit { setAlbumsLayout(Blocks.encode(it)) }; blocks = 0 },
            onReset = { vm.edit { setAlbumsLayout("") }; blocks = 0 },
            onDismiss = { blocks = 0 }
        )
    }

    Column(Modifier.fillMaxSize().screenBg().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 130.dp)) {
        when (Sb.style) {
            UiStyle.NEW -> Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", style = sbStyle(34, FontWeight.Bold, c.tx))
                Box(Modifier.size(40.dp).clip(CircleShape).background(c.ac), contentAlignment = Alignment.Center) {
                    Text(settings.userName.trim().take(1).uppercase().ifEmpty { "♪" }, style = sbStyle(17, FontWeight.Bold, Color.White))
                }
            }
            UiStyle.GLASS -> GlassTitle("Settings", null, Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp))
            else -> Text("Settings", style = sbStyle(26, FontWeight.Bold, c.tx), modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp))
        }

        SettingsGroup("Look") {
            SettingsRow("Interface", first = true) {
                Segmented(UiStyle.names, settings.uiStyle) { v -> vm.edit { setUiStyle(v) } }
            }
            if (settings.uiStyle == UiStyle.NEW) {
                SettingsRow("Song Card") {
                    Segmented(listOf("Artwork", "Lyrics", "Chords"), settings.cardFace) { v -> vm.edit { setCardFace(v) } }
                }
            }
        }
        Text(
            when (settings.uiStyle) {
                UiStyle.NEW -> "New: story circles, swipeable song cards, a player bar and a reader with a player panel."
                UiStyle.GLASS -> "Glass: translucent panels over a soft glow, floating tab bar and a floating reader bar."
                else -> "Old: the classic cards with a bottom navigation bar."
            },
            style = sbStyle(12, color = c.mu),
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
        )

        SettingsGroup("Profile") {
            SettingsRow("Name", first = true, onClick = { nameDialog = true }) { MutedValue(settings.userName.ifBlank { "Not set" }) }
            SettingsRow("Greeting", onClick = { greetingDialog = true }) { MutedValue(greetingLine(settings)) }
        }

        SettingsGroup("Appearance") {
            SettingsRow("Theme", first = true) {
                Segmented(listOf("Light", "Dark", "System"), settings.theme.ordinal) { vm.setTheme(ThemeMode.entries[it]) }
            }
            SettingsRow("Accent Color", onClick = { accentDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(accentColor(c.isDark, settings.accent, settings.accentCustom)))
                    MutedValue(if (settings.accent == 3) "#%06X".format(settings.accentCustom and 0xFFFFFF) else listOf("Red", "Blue", "Orange")[settings.accent.coerceIn(0, 2)])
                }
            }
            if (settings.uiStyle == UiStyle.OLD) {
                SettingsRow("Navigation Bar") {
                    Segmented(listOf("Raised", "Simple"), if (settings.navRaised) 0 else 1) { vm.setNavRaised(it == 0) }
                }
            }
        }

        SettingsGroup("Layout") {
            SettingsRow("Albums on Main Page", first = true) {
                Segmented(listOf("Hide", "2", "4", "6", "8"), TopAlbumChoices.indexOf(settings.topAlbums).coerceAtLeast(0)) { i ->
                    vm.edit { setTopAlbums(TopAlbumChoices[i]) }
                }
            }
            SettingsRow("Songs, main page", onClick = { choice = 1 }) { MutedValue(SongsLayout.names[settings.songsLayout]) }
            SettingsRow("Albums per row", onClick = { choice = 2 }) { MutedValue(ColumnChoices[settings.albumColumns - 1]) }
            SettingsRow("Groups per row", onClick = { choice = 3 }) { MutedValue(ColumnChoices[settings.groupColumns - 1]) }
            SettingsRow("Songs inside an album", onClick = { choice = 4 }) { MutedValue(gridNames[trackIdx(settings.trackLayout)]) }
            SettingsRow("Favorites", onClick = { choice = 5 }) { MutedValue(gridNames[trackIdx(settings.favLayout)]) }
        }

        SettingsGroup("Sorting") {
            SettingsRow("Songs", first = true, onClick = { choice = 6 }) { MutedValue(SongSort.names[settings.songSort].substringBefore(" (")) }
            SettingsRow("Albums and groups", onClick = { choice = 7 }) { MutedValue(AlbumSort.names[settings.albumSort].substringBefore(" (")) }
            SettingsRow("Songs inside an album", onClick = { choice = 8 }) { MutedValue(TrackSort.names[settings.trackSort]) }
        }

        SettingsGroup("Pages") {
            SettingsRow("Main page blocks", first = true, onClick = { blocks = 1 }) { MutedValue("Order and hide") }
            SettingsRow("Albums page blocks", onClick = { blocks = 2 }) { MutedValue("Order and hide") }
            SettingsRow("Groups heading", onClick = { titleDialog = true }) { MutedValue(settings.groupsTitle.ifBlank { "Automatic" }) }
        }

        SettingsGroup("Reading") {
            SettingsRow("Lyrics Font Size", first = true, onClick = { readingDialog = true }) { MutedValue("${settings.lyricsSize} pt") }
            SettingsRow("Chord Size", onClick = { readingDialog = true }) { MutedValue("${settings.chordSize} pt") }
            SettingsRow("Chord Distance", onClick = { readingDialog = true }) { MutedValue(chordOffsetLabel(settings.chordOffset)) }
            SettingsRow("Line Spacing", onClick = { readingDialog = true }) { MutedValue("${"%.1f".format(settings.lineSpacing)}×") }
            SettingsRow("Section Title Size", onClick = { readingDialog = true }) { MutedValue("${settings.sectionSize} pt") }
            SettingsRow("Section Names") {
                Segmented(listOf("VERSE", "verse", "As typed"), settings.sectionStyle) { v -> vm.edit { setSectionStyle(v) } }
            }
        }

        SettingsGroup("Song Display") {
            SettingsRow("Show Chords", first = true) { SbSwitch(settings.showChords) { vm.setShowChords(it) } }
            SettingsRow("Show Tabs") { SbSwitch(settings.showTabs) { vm.setShowTabs(it) } }
            SettingsRow("Auto Scroll") { SbSwitch(settings.autoScroll) { vm.setAutoScroll(it) } }
            SettingsRow("Auto Scroll Speed", onClick = { speedDialog = true }) { MutedValue("${settings.scrollSpeed} / 10") }
            SettingsRow("Edit Button") { SbSwitch(settings.showEditButton) { v -> vm.edit { setShowEditButton(v) } } }
            SettingsRow("Song Page Theme") {
                Segmented(listOf("App", "Dark", "Light"), settings.songTheme) { v -> vm.edit { setSongTheme(v) } }
            }
        }

        SettingsGroup("Data") {
            SettingsRow("Backup", first = true, onClick = { backupDialog = true }) { MutedValue(backupLabel(settings.lastBackup)) }
            SettingsRow("Restore Backup", onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { MutedValue("Any version") }
            SettingsRow("Export Songs", onClick = { exportLauncher.launch("songbook-songs.txt") })
            SettingsRow("Import Songs", onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) })
        }

        SettingsGroup("About") {
            SettingsRow("Show Introduction Again", first = true, onClick = onReplayIntro)
        }

        Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AppIconArt(44.dp)
            Text("Songbook · Version $APP_VERSION", style = sbStyle(13, color = c.mu), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** Asks how to restore a backup file: add to the library or replace it, with or without the saved settings. */
@Composable
private fun RestoreDialog(info: BackupInfo, onDismiss: () -> Unit, onRestore: (replace: Boolean, withSettings: Boolean) -> Unit) {
    val c = Sb.colors
    var mode by remember { mutableIntStateOf(0) }
    var withSettings by remember { mutableStateOf(info.hasSettings) }
    SbDialog("Restore backup", onDismiss, "Restore", { onRestore(mode == 1, withSettings && info.hasSettings) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Songbook ${backupVersionName(info.version)} backup", style = sbStyle(15, FontWeight.SemiBold, c.tx))
            Text(
                "${info.songs} songs · ${info.albums} albums" + (if (info.groups > 0) " · ${info.groups} groups" else "") +
                    (if (info.ideas > 0) " · ${info.ideas} ideas" else "") +
                    (if (info.images > 0) " · ${info.images} pictures" else ""),
                style = sbStyle(13, color = c.mu)
            )
            Segmented(listOf("Add to library", "Replace library"), mode) { mode = it }
            Text(
                if (mode == 0) "Songs, albums and groups that are already here are skipped, nothing is overwritten."
                else "Everything in your library now is deleted first and replaced by the backup.",
                style = sbStyle(12, color = if (mode == 0) c.mu else c.ac)
            )
            if (info.hasSettings) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Also restore settings", style = sbStyle(14, FontWeight.Medium, c.tx))
                    SbSwitch(withSettings) { withSettings = it }
                }
            } else {
                Text("This backup has no settings (backups before 2.0 don't).", style = sbStyle(12, color = c.mu))
            }
        }
    }
}

/** Backup format: .zip with every picture as a file, or the library alone as .json. */
@Composable
private fun BackupDialog(onDismiss: () -> Unit, onZip: () -> Unit, onJson: () -> Unit) {
    val c = Sb.colors
    SbDialog("Backup", onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            @Composable
            fun Option(title: String, text: String, onClick: () -> Unit) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 10.dp)) {
                    Text(title, style = sbStyle(15, FontWeight.SemiBold, c.ac))
                    Text(text, style = sbStyle(12, color = c.mu))
                }
            }
            Option("With pictures (.zip)", "Songs, albums, groups, settings and every cover picture. Use this one before updating or changing phone.", onZip)
            Option("Library only (.json)", "Everything except the pictures. Small and fast.", onJson)
        }
    }
}

@Composable
private fun TitleDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    SbDialog("Groups heading", onDismiss, "Save", { onSave(text.trim()) }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text, onValueChange = { if (it.length <= 24) text = it }, label = { Text("Heading") },
                singleLine = true, textStyle = sbStyle(15, color = Sb.colors.tx)
            )
            Text(
                "Leave empty and the Albums page names each section after the type you chose: Trilogies, Box sets...",
                style = sbStyle(12, color = Sb.colors.mu)
            )
        }
    }
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    SbDialog("Your name", onDismiss, "Save", { onSave(text.trim()) }) {
        OutlinedTextField(
            value = text, onValueChange = { if (it.length <= 24) text = it }, label = { Text("Name") },
            singleLine = true, textStyle = sbStyle(15, color = Sb.colors.tx)
        )
    }
}

@Composable
private fun GreetingDialog(settings: AppSettings, onDismiss: () -> Unit, onSave: (Int, String) -> Unit) {
    val c = Sb.colors
    var style by remember { mutableIntStateOf(settings.greeting.coerceIn(0, GreetingNames.lastIndex)) }
    var custom by remember { mutableStateOf(settings.greetingCustom) }
    SbDialog("Greeting", onDismiss, "Save", { onSave(style, custom.trim()) }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                greetingLine(settings.copy(greeting = style, greetingCustom = custom)),
                style = sbStyle(20, FontWeight.Bold, c.ac), modifier = Modifier.padding(bottom = 10.dp)
            )
            GreetingNames.forEachIndexed { i, label ->
                Row(
                    Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).clickable { style = i },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = sbStyle(15, if (i == style) FontWeight.SemiBold else FontWeight.Medium, if (i == style) c.ac else c.tx))
                }
            }
            if (style == 5) {
                OutlinedTextField(
                    value = custom, onValueChange = { if (it.length <= 24) custom = it }, label = { Text("Your greeting") },
                    singleLine = true, textStyle = sbStyle(15, color = c.tx), modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}


private fun backupLabel(millis: Long): String {
    if (millis <= 0L) return "Never"
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return if (date == LocalDate.now()) "Today" else date.format(DateTimeFormatter.ofPattern("MMM d"))
}
