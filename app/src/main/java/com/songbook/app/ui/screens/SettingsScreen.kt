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
import com.songbook.app.data.BackupInfo
import com.songbook.app.data.ThemeMode
import com.songbook.app.data.UiStyle
import com.songbook.app.data.displayName
import com.songbook.app.data.readText
import com.songbook.app.data.writeText
import com.songbook.app.ui.AppViewModel
import com.songbook.app.ui.components.AccentDialog
import com.songbook.app.ui.components.AppIconArt
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

const val APP_VERSION = "2.0"

private val GreetingNames = listOf("Hello", "Hi", "Hey", "Welcome back", "By time of day", "Custom")
private val TopAlbumChoices = listOf(0, 2, 4, 6, 8)

private fun backupVersionName(v: Int) = when (v) { 1 -> "1.0"; 2 -> "1.5"; else -> "2.0" }

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
    var restore by remember { mutableStateOf<Pair<BackupInfo, String>?>(null) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = context.writeText(uri, vm.backupJson())
            toast(if (ok) "Backup saved" else "Could not save backup")
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = context.readText(uri)
            val info = text?.let { vm.previewBackup(it) }
            if (text == null || info == null) toast("That file isn't a Songbook backup") else restore = info to text
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
            settings.lyricsSize, settings.chordSize, settings.lineSpacing, settings.chordOffset,
            onCommit = { l, ch, s, off -> vm.setReading(l, ch, s, off) },
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
    restore?.let { (info, text) ->
        RestoreDialog(info, { restore = null }) { replace, withSettings ->
            restore = null
            scope.launch {
                val r = runCatching { vm.restoreBackup(text, replace, withSettings) }.getOrNull()
                toast(
                    if (r == null) "Could not restore that backup"
                    else "Restored ${r.songs} songs, ${r.albums} albums, ${r.groups} groups" + if (r.skipped > 0) " (${r.skipped} already there)" else ""
                )
            }
        }
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
            SettingsRow("Albums Grid") {
                Segmented(listOf("2 columns", "3 columns"), settings.gridColumns - 2) { v -> vm.edit { setGridColumns(v + 2) } }
            }
            SettingsRow("Songs Layout") {
                Segmented(listOf("List", "Grid"), if (settings.songsGrid) 1 else 0) { v -> vm.edit { setSongsGrid(v == 1) } }
            }
        }

        SettingsGroup("Reading") {
            SettingsRow("Lyrics Font Size", first = true, onClick = { readingDialog = true }) { MutedValue("${settings.lyricsSize} pt") }
            SettingsRow("Chord Size", onClick = { readingDialog = true }) { MutedValue("${settings.chordSize} pt") }
            SettingsRow("Chord Distance", onClick = { readingDialog = true }) { MutedValue(chordOffsetLabel(settings.chordOffset)) }
            SettingsRow("Line Spacing", onClick = { readingDialog = true }) { MutedValue("${"%.1f".format(settings.lineSpacing)}×") }
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
            SettingsRow("Backup", first = true, onClick = { backupLauncher.launch("songbook-backup.json") }) { MutedValue(backupLabel(settings.lastBackup)) }
            SettingsRow("Restore Backup", onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { MutedValue("From 1.0, 1.5 or 2.0") }
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
                    (if (info.ideas > 0) " · ${info.ideas} ideas" else ""),
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
