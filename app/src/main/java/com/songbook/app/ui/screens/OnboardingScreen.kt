package com.songbook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.songbook.app.ui.components.AppIconArt
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.components.screenBg
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

private class Feature(val title: String, val text: String)

private val Features = listOf(
    Feature("Chords above the words", "Write [Em]like this and the reader places each chord over the right letter."),
    Feature("Albums and trilogies", "Sort songs into albums, then combine albums into a trilogy or collection."),
    Feature("Workflow for unfinished songs", "Keep track of what each idea needs next, from first spark to finished.")
)

/** First-run introduction: welcome, what the app does, and the name shown on the main page. */
@Composable
fun OnboardingScreen(initialName: String = "", onDone: (name: String) -> Unit) {
    val c = Sb.colors
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    val last = page == 2

    fun finish() = onDone(name.trim())

    Column(
        Modifier
            .fillMaxSize()
            .screenBg()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (!last) {
                Text(
                    "Skip",
                    style = sbStyle(14, FontWeight.Medium, c.mu),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onDone("") }.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (page) {
                0 -> {
                    AppIconArt(128.dp)
                    Text("Songbook", style = sbStyle(32, FontWeight.Bold, c.tx), modifier = Modifier.padding(top = 32.dp))
                    Text(
                        "Your songs, all in one place.\nLyrics, chords and tabs, written the way you play them.",
                        style = sbStyle(15, color = c.mu),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                1 -> {
                    Text("Made for songwriters", style = sbStyle(26, FontWeight.Bold, c.tx), textAlign = TextAlign.Center)
                    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Features.forEach { f ->
                            Column(Modifier.fillMaxWidth().sbCard(22.dp).padding(horizontal = 20.dp, vertical = 16.dp)) {
                                Text(f.title, style = sbStyle(15, FontWeight.SemiBold, c.tx))
                                Text(f.text, style = sbStyle(13, color = c.mu), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
                else -> {
                    Text("What should we call you?", style = sbStyle(26, FontWeight.Bold, c.tx), textAlign = TextAlign.Center)
                    Text(
                        "Your name appears at the top of the main page. You can change it any time in Settings.",
                        style = sbStyle(14, color = c.mu),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Column(Modifier.padding(top = 24.dp).fillMaxWidth().sbCard(22.dp).padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Text("YOUR NAME", style = sbStyle(11, color = c.mu))
                        BasicTextField(
                            value = name,
                            onValueChange = { if (it.length <= 24) name = it },
                            singleLine = true,
                            textStyle = sbStyle(20, FontWeight.Bold, c.tx),
                            cursorBrush = SolidColor(c.ac),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { finish() }),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (name.isEmpty()) Text("Your name", style = sbStyle(20, FontWeight.Bold, c.mu.copy(alpha = 0.6f)))
                                    inner()
                                }
                            }
                        )
                    }
                    Text(
                        if (name.isBlank()) "Hello" else "Hello ${name.trim()}",
                        style = sbStyle(18, FontWeight.SemiBold, c.ac),
                        modifier = Modifier.padding(top = 20.dp)
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 28.dp).padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 18.dp)) {
                (0..2).forEach { i ->
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(if (i == page) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (i == page) c.ac else c.ln)
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(c.ac)
                    .clickable { if (last) finish() else page++ },
                contentAlignment = Alignment.Center
            ) { Text(if (last) "Get started" else "Next", style = sbStyle(16, FontWeight.SemiBold, Color.White)) }
        }
    }
}
