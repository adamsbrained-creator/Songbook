package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/** Entry to the Workflow page: how many songs and ideas are still being written. */
@Composable
fun WorkflowEntry(count: Int, ideas: Int, onClick: () -> Unit, modifier: Modifier = Modifier, horizontal: Dp = 24.dp) {
    val c = Sb.colors
    val text = when {
        count == 0 -> "Everything is finished"
        ideas > 0 && ideas == count -> if (count == 1) "1 idea waiting" else "$count ideas waiting"
        ideas > 0 -> "$count in the making, $ideas of them ideas"
        count == 1 -> "1 song still in the making"
        else -> "$count songs still in the making"
    }
    Row(
        modifier
            .padding(horizontal = horizontal)
            .fillMaxWidth()
            .height(60.dp)
            .sbCard(20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(c.ac.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Text("$count", style = sbStyle(14, FontWeight.Bold, c.ac))
        }
        Column(Modifier.weight(1f)) {
            Text("Workflow", style = sbStyle(14, FontWeight.SemiBold, c.tx))
            Text(text, style = sbStyle(12, color = c.mu))
        }
        StrokeIcon(SbIcons.ChevronRight, c.mu, size = 20.dp)
    }
}
