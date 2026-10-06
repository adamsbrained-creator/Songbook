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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AppSettings
import com.songbook.app.ui.theme.AccentLight
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle
import kotlin.math.roundToInt

/** A labelled row for edit dialogs: label on the left, whatever [value] draws on the right, then a chevron. */
@Composable
fun PickRow(label: String, onClick: () -> Unit, value: @Composable () -> Unit) {
    val c = Sb.colors
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = sbStyle(15, FontWeight.Medium, c.tx))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            value()
            StrokeIcon(SbIcons.ChevronRight, c.mu, size = 20.dp)
        }
    }
}

// ---- types: Single, EP, Demo, Trilogy... or anything you write ---------------------------

val SongTypes = listOf("Original song", "Demo", "Cover", "Live", "Instrumental", "Remix", "Acoustic")
val AlbumTypes = listOf("Album", "Single", "EP", "LP", "Mixtape", "Compilation", "Live album")
val GroupTypes = listOf("Trilogy", "Duology", "Series", "Collection", "Box set", "Discography")

/** "Type  Single >" row. [value] blank shows [defaultLabel] in grey. */
@Composable
fun TypeRow(value: String, defaultLabel: String, onClick: () -> Unit) {
    PickRow("Type", onClick) {
        Text(value.ifBlank { defaultLabel }, style = sbStyle(14, color = Sb.colors.mu))
    }
}

/**
 * Pick one of the ready-made [presets] or write your own title. Choosing "Automatic" (empty text)
 * goes back to [defaultLabel].
 */
@Composable
fun TypeDialog(
    title: String,
    presets: List<String>,
    current: String,
    defaultLabel: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    var text by remember { mutableStateOf(current) }
    SbDialog(title, onDismiss, "Save", { onSave(text.trim()) }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            @Composable
            fun Option(label: String, on: Boolean, onClick: () -> Unit) {
                Row(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = sbStyle(15, if (on) FontWeight.SemiBold else FontWeight.Medium, if (on) c.ac else c.tx))
                }
            }
            Option("Automatic ($defaultLabel)", text.isBlank()) { text = "" }
            presets.forEach { p -> Option(p, text.equals(p, ignoreCase = true)) { text = p } }
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 24) text = it },
                label = { Text("Or write your own") },
                singleLine = true,
                textStyle = sbStyle(15, color = c.tx),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }
}

// ---- accent color: presets plus RGB sliders --------------------------------------------------

private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

/**
 * Accent color: the three presets or your own color from red, green and blue sliders.
 * Changes apply as soon as a slider is released, so you can watch the whole app change.
 */
@Composable
fun AccentDialog(
    settings: AppSettings,
    onPreset: (Int) -> Unit,
    onCustom: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    val start = settings.accentCustom
    var r by remember { mutableIntStateOf((start shr 16) and 0xFF) }
    var g by remember { mutableIntStateOf((start shr 8) and 0xFF) }
    var b by remember { mutableIntStateOf(start and 0xFF) }
    var custom by remember { mutableStateOf(settings.accent == 3) }
    val apply = { custom = true; onCustom(argb(r, g, b)) }

    SbDialog("Accent color", onDismiss, "Done", { onDismiss() }, dismissText = "Close") {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentLight.forEachIndexed { i, color ->
                    val on = !custom && settings.accent == i
                    Box(
                        Modifier.size(44.dp).then(if (on) Modifier.border(2.dp, color, CircleShape) else Modifier).padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(color).clickable { custom = false; onPreset(i) })
                    }
                }
                val customColor = Color(argb(r, g, b))
                Box(
                    Modifier.size(44.dp).then(if (custom) Modifier.border(2.dp, customColor, CircleShape) else Modifier).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(customColor).clickable { apply() })
                }
            }
            Text(
                "Your color  #%02X%02X%02X".format(r, g, b),
                style = sbStyle(13, FontWeight.Medium, c.mu),
                modifier = Modifier.padding(top = 8.dp)
            )
            ChannelSlider("Red", r, Color(0xFFE5484D), { r = it }, apply)
            ChannelSlider("Green", g, Color(0xFF30A46C), { g = it }, apply)
            ChannelSlider("Blue", b, Color(0xFF3E63DD), { b = it }, apply)
        }
    }
}

@Composable
private fun ChannelSlider(label: String, value: Int, tint: Color, onChange: (Int) -> Unit, onRelease: () -> Unit) {
    val c = Sb.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = sbStyle(13, FontWeight.Medium, c.tx), modifier = Modifier.width(52.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            onValueChangeFinished = onRelease,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint, inactiveTrackColor = c.ln),
            modifier = Modifier.weight(1f)
        )
        Text("$value", style = sbStyle(12, color = c.mu), modifier = Modifier.width(32.dp))
    }
}
