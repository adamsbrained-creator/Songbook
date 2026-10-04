package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/** gray, green, amber, blue, red, purple. A badge stores the index into this list. */
val BadgeColors = listOf(
    Color(0xFF8E8E93), Color(0xFF2FA866), Color(0xFFE59A12),
    Color(0xFF3A7BD5), Color(0xFFE5173F), Color(0xFF8A63D2)
)

/** Ready-made badges: label and color index. "Write your own" is the text field in [BadgeDialog]. */
val BadgePresets = listOf("Finished" to 1, "In progress" to 2, "Unfinished" to 0)

/**
 * Small rounded label. [solid] draws white text on a filled pill (for use on top of artwork),
 * otherwise a tinted pill is used.
 */
@Composable
fun BadgePill(text: String, colorIndex: Int, modifier: Modifier = Modifier, solid: Boolean = false) {
    if (text.isBlank()) return
    val color = BadgeColors[colorIndex.coerceIn(0, BadgeColors.lastIndex)]
    Text(
        text,
        style = sbStyle(10, FontWeight.SemiBold, if (solid) Color.White else color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (solid) color else color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun BadgeDialog(
    text: String,
    colorIndex: Int,
    onSave: (text: String, colorIndex: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    var t by remember { mutableStateOf(text) }
    var col by remember { mutableIntStateOf(colorIndex.coerceIn(0, BadgeColors.lastIndex)) }

    SbDialog("Badge", onDismiss, "Save", { onSave(t.trim(), col) }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val options = BadgePresets.map { it.first to it.second } + ("No badge" to -1)
            options.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (label, color) ->
                        val selected = if (color < 0) t.isBlank() else t.equals(label, ignoreCase = true)
                        ChipPill(
                            label, selected,
                            onClick = { if (color < 0) t = "" else { t = label; col = color } },
                            inactiveBg = c.bg
                        )
                    }
                }
            }
            OutlinedTextField(
                value = t,
                onValueChange = { if (it.length <= 18) t = it },
                label = { Text("Or write your own") },
                singleLine = true,
                textStyle = sbStyle(15, color = c.tx),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                BadgeColors.forEachIndexed { i, color ->
                    Box(
                        Modifier
                            .size(30.dp)
                            .then(if (i == col) Modifier.border(2.dp, c.tx, CircleShape) else Modifier)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { col = i }
                    )
                }
            }
            Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
                if (t.isBlank()) Text("No badge", style = sbStyle(12, color = c.mu)) else BadgePill(t, col)
            }
        }
    }
}

/** A "Badge" row for dialogs and editors: shows the current badge and opens [BadgeDialog] on tap. */
@Composable
fun BadgeRow(text: String, colorIndex: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Sb.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Badge", style = sbStyle(15, FontWeight.Medium, c.tx))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (text.isBlank()) Text("None", style = sbStyle(14, color = c.mu)) else BadgePill(text, colorIndex)
            StrokeIcon(SbIcons.ChevronRight, c.mu, size = 20.dp)
        }
    }
}
