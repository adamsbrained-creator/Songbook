package com.songbook.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AppSettings
import com.songbook.app.data.ThemeMode
import com.songbook.app.data.displayName
import com.songbook.app.data.readText
import com.songbook.app.data.writeText
import com.songbook.app.ui.AppViewModel
import com.songbook.app.ui.components.MutedValue
import com.songbook.app.ui.components.ReadingDialog
import com.songbook.app.ui.components.SbSwitch
import com.songbook.app.ui.components.Segmented
import com.songbook.app.ui.components.SettingsGroup
import com.songbook.app.ui.components.SettingsRow
import com.songbook.app.ui.theme.AccentLight
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(settings: AppSettings, vm: AppViewModel) {
    val c = Sb.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var readingDialog by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = context.writeText(uri, vm.backupJson())
            toast(if (ok) "Backup saved" else "Could not save backup")
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
        ReadingDialog(settings.lyricsSize, settings.chordSize, settings.lineSpacing,
            onCommit = { l, ch, s -> vm.setReading(l, ch, s) },
            onDismiss = { readingDialog = false })
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 130.dp)) {
        Text("Settings", style = sbStyle(26, androidx.compose.ui.text.font.FontWeight.Bold, c.tx), modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp))

        SettingsGroup("Appearance") {
            SettingsRow("Theme", first = true) {
                Segmented(listOf("Light", "Dark", "System"), settings.theme.ordinal) { vm.setTheme(ThemeMode.entries[it]) }
            }
            SettingsRow("Accent Color") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AccentLight.forEachIndexed { i, color ->
                        val on = settings.accent == i
                        Box(
                            Modifier.size(38.dp).then(if (on) Modifier.border(2.dp, color, CircleShape) else Modifier).padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(color).clickable { vm.setAccent(i) })
                        }
                    }
                }
            }
            SettingsRow("Navigation Bar") {
                Segmented(listOf("Raised", "Simple"), if (settings.navRaised) 0 else 1) { vm.setNavRaised(it == 0) }
            }
        }

        SettingsGroup("Reading") {
            SettingsRow("Lyrics Font Size", first = true, onClick = { readingDialog = true }) { MutedValue("${settings.lyricsSize} pt") }
            SettingsRow("Chord Size", onClick = { readingDialog = true }) { MutedValue("${settings.chordSize} pt") }
            SettingsRow("Line Spacing", onClick = { readingDialog = true }) { MutedValue("${"%.1f".format(settings.lineSpacing)}×") }
        }

        SettingsGroup("Song Display") {
            SettingsRow("Show Chords", first = true) { SbSwitch(settings.showChords) { vm.setShowChords(it) } }
            SettingsRow("Show Tabs") { SbSwitch(settings.showTabs) { vm.setShowTabs(it) } }
            SettingsRow("Auto Scroll") { SbSwitch(settings.autoScroll) { vm.setAutoScroll(it) } }
        }

        SettingsGroup("Data") {
            SettingsRow("Backup", first = true, onClick = { backupLauncher.launch("songbook-backup.json") }) {
                MutedValue(backupLabel(settings.lastBackup))
            }
            SettingsRow("Export Songs", onClick = { exportLauncher.launch("songbook-songs.txt") })
            SettingsRow("Import Songs", onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) })
        }

        Text(
            "Songbook · Version 1.0",
            style = sbStyle(13, color = c.mu),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}

private fun backupLabel(millis: Long): String {
    if (millis <= 0L) return "Never"
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return if (date == LocalDate.now()) "Today" else date.format(DateTimeFormatter.ofPattern("MMM d"))
}
