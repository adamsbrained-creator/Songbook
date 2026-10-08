package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songbook.app.data.UiStyle
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/** A block of a page: its id and whether it is shown. The list order is the order on the page. */
typealias BlockList = List<Pair<String, Boolean>>

/** The blocks of the main page and the Albums page with their default order. */
object Blocks {
    fun home(style: Int): BlockList = when (style) {
        UiStyle.NEW -> listOf("stories" to true, "filters" to true, "workflow" to true, "albums" to true, "recent" to false, "songs" to true)
        else -> listOf("filters" to true, "search" to true, "albums" to true, "workflow" to true, "recent" to true, "songs" to true)
    }

    fun albums(style: Int): BlockList = when (style) {
        UiStyle.NEW -> listOf("groups" to true, "albums" to true, "workflow" to true)
        else -> listOf("groups" to true, "albums" to true)
    }

    val labels = mapOf(
        "stories" to "Latest songs (circles and cards)",
        "search" to "Search box",
        "filters" to "Filters (All, Lyrics, Chords, Tabs)",
        "albums" to "Albums",
        "workflow" to "Workflow",
        "recent" to "Recently edited",
        "songs" to "All songs",
        "groups" to "Groups (trilogies...)"
    )

    /** Merges what was saved with the defaults: unknown blocks are dropped, new blocks are added at the end. */
    fun resolve(raw: String, defaults: BlockList): BlockList {
        val known = defaults.map { it.first }
        val saved = raw.split(',').mapNotNull { part ->
            val bits = part.split(':')
            val id = bits.getOrNull(0)?.trim()
            if (id != null && id in known) id to (bits.getOrNull(1) != "0") else null
        }.distinctBy { it.first }
        return saved + defaults.filter { d -> saved.none { it.first == d.first } }
    }

    fun encode(list: BlockList): String = list.joinToString(",") { it.first + ":" + (if (it.second) "1" else "0") }

    /** True when [id] should be drawn in [list]. */
    fun shows(list: BlockList, id: String): Boolean = list.firstOrNull { it.first == id }?.second ?: false
}

/**
 * Drag the blocks of a page into the order you want and switch the ones you do not need off.
 * [onSave] gets the new list, [onReset] goes back to the standard layout.
 */
@Composable
fun BlocksDialog(
    title: String,
    start: BlockList,
    onSave: (BlockList) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = Sb.colors
    val list = remember { mutableStateListOf<Pair<String, Boolean>>().also { it.addAll(start) } }
    SbDialog(title, onDismiss, "Save", { onSave(list.toList()) }) {
        androidx.compose.foundation.layout.Column {
            Text("Drag the handle to move a block. The switch shows or hides it.", style = sbStyle(12, color = c.mu), modifier = Modifier.padding(bottom = 8.dp))
            Box(Modifier.fillMaxWidth().height((list.size * 56).coerceAtMost(336).dp)) {
                ReorderList(
                    items = list,
                    key = { it.first.hashCode().toLong() },
                    onMove = { from, to -> if (from in list.indices && to in list.indices) list.add(to, list.removeAt(from)) }
                ) { item, dragging, handle ->
                    Row(
                        Modifier.fillMaxWidth().height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (dragging) c.track else androidx.compose.ui.graphics.Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            Blocks.labels[item.first] ?: item.first,
                            style = sbStyle(14, FontWeight.Medium, if (item.second) c.tx else c.mu),
                            modifier = Modifier.weight(1f)
                        )
                        SbSwitch(item.second) { on ->
                            val i = list.indexOfFirst { it.first == item.first }
                            if (i >= 0) list[i] = list[i].copy(second = on)
                        }
                        Box(handle.padding(6.dp)) { StrokeIcon(SbIcons.Drag, c.mu, size = 22.dp) }
                    }
                }
            }
            ChipPill("Reset to standard", active = false, onClick = onReset, inactiveBg = c.track, modifier = Modifier.padding(top = 10.dp))
        }
    }
}
