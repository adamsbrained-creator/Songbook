package com.songbook.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.CircleArtwork
import com.songbook.app.ui.components.ReorderList
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.StrokeIcon
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.components.screenBg
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/** One row of the Arrange page. [side] is only used for the songs of an album that has sides. */
class ArrangeItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val gradient: Int,
    val image: String?,
    val side: Int = 0
)

/**
 * Drag the rows with the handle on the right into the order you want, then tap Done.
 * With [sides] (an album with Side A / Side B) pick a side first: each side is arranged on its own.
 */
@Composable
fun ArrangeScreen(
    title: String,
    items: List<ArrangeItem>,
    sides: List<String>,
    onDone: (List<Long>) -> Unit,
    onCancel: () -> Unit
) {
    val c = Sb.colors
    val order = remember { mutableStateListOf<ArrangeItem>().also { it.addAll(items) } }
    var side by remember { mutableIntStateOf(0) }
    val shown = if (sides.isEmpty()) order.toList() else order.filter { it.side.coerceIn(0, sides.lastIndex) == side }

    Column(Modifier.fillMaxSize().screenBg().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Cancel", style = sbStyle(14, FontWeight.Medium, c.mu),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onCancel).padding(horizontal = 12.dp, vertical = 10.dp)
            )
            Text(title, style = sbStyle(16, FontWeight.SemiBold, c.tx), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Text(
                "Done", style = sbStyle(14, FontWeight.Bold, c.ac),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onDone(order.map { it.id }) }.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
        Text(
            "Hold the ≡ handle on the right and drag a row up or down.",
            style = sbStyle(12, color = c.mu),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        if (sides.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sides.forEachIndexed { i, name -> ChipPill(name, active = side == i, onClick = { side = i }) }
            }
        }
        if (shown.isEmpty()) {
            Text("Nothing here to arrange.", style = sbStyle(14, color = c.mu), modifier = Modifier.padding(24.dp))
        }
        ReorderList(
            items = shown,
            key = { it.id },
            onMove = { from, to ->
                if (from in shown.indices && to in shown.indices) {
                    val a = shown[from]
                    val b = shown[to]
                    val ia = order.indexOf(a)
                    val ib = order.indexOf(b)
                    if (ia >= 0 && ib >= 0) {
                        order.removeAt(ia)
                        order.add(ib, a)
                    }
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) { item, dragging, handle ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .sbCard(18.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircleArtwork(item.gradient, 44.dp, image = item.image)
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = sbStyle(15, FontWeight.SemiBold, if (dragging) c.ac else c.tx), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (item.subtitle.isNotBlank()) Text(item.subtitle, style = sbStyle(12, color = c.mu), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(handle.padding(horizontal = 14.dp, vertical = 14.dp)) { StrokeIcon(SbIcons.Drag, c.mu, size = 24.dp) }
            }
        }
    }
}
