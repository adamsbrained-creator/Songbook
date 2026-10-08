package com.songbook.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/**
 * Sides of an album (Side A, Side B, Disc 2, anything you name). With sides the album page lists the songs
 * under each side. Removing a side moves its songs to the first side ([onRemove] does that right away).
 */
@Composable
fun SidesDialog(
    sides: List<String>,
    onSave: (List<String>) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    val names = remember { mutableStateListOf<String>().also { it.addAll(sides) } }
    SbDialog("Album sides", onDismiss, "Save", { onSave(names.toList()) }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (names.isEmpty()) {
                Text("No sides yet. Songs are listed in one row.", style = sbStyle(13, color = c.mu))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipPill("Side A + B", active = true, onClick = { names.clear(); names.addAll(listOf("Side A", "Side B")) })
                    ChipPill("Disc 1 + 2", active = false, inactiveBg = c.track, onClick = { names.clear(); names.addAll(listOf("Disc 1", "Disc 2")) })
                }
            }
            names.forEachIndexed { i, name ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 20) names[i] = it },
                        singleLine = true,
                        textStyle = sbStyle(15, color = c.tx),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Remove", style = sbStyle(13, FontWeight.SemiBold, c.ac),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                            if (i < sides.size) onRemove(i)
                            names.removeAt(i)
                        }.padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
            }
            val next = ('A' + names.size.coerceAtMost(25))
            ChipPill(
                "+ Add side", active = false, inactiveBg = c.track,
                modifier = Modifier.padding(top = 4.dp),
                onClick = { names.add("Side $next") }
            )
        }
    }
}
