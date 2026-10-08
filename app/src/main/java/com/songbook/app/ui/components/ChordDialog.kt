package com.songbook.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/**
 * Small window that opens when you tap a word of the lyrics: write the chord (or several, separated by
 * spaces) that is played on that word. The chord is drawn exactly above the first letter of the word.
 * [note] explains what is saved when a capo is on. Saving an empty box removes the chords of the word.
 */
@Composable
fun ChordDialog(
    word: String,
    current: String,
    suggestions: List<String>,
    note: String?,
    onSave: (String) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    var text by remember { mutableStateOf(current) }
    SbDialog("Chord on “$word”", onDismiss, "Save", { onSave(text) }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 40) text = it },
                label = { Text("Chord, or several like Am Dm") },
                singleLine = true,
                textStyle = sbStyle(18, FontWeight.Bold, c.ac, mono = true),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )
            if (note != null) Text(note, style = sbStyle(12, color = c.mu))
            if (suggestions.isNotEmpty()) {
                Text("In this song", style = sbStyle(12, color = c.mu))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { s ->
                        ChipPill(
                            s, active = false, inactiveBg = c.track, modifier = Modifier.height(36.dp),
                            onClick = { text = if (text.isBlank()) s else "${text.trim()} $s" }
                        )
                    }
                }
            }
            if (onRemove != null) {
                ChipPill("Remove chord", active = false, onClick = onRemove, inactiveBg = c.track, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
