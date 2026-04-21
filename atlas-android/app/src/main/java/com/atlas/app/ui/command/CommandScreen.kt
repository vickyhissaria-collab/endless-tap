package com.atlas.app.ui.command

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atlas.app.data.Speaker
import com.atlas.app.data.Turn
import com.atlas.app.ui.common.AtlasSharedViewModel
import com.atlas.app.ui.theme.Mono
import com.atlas.app.ui.theme.Sans
import com.atlas.app.ui.theme.Serif
import com.atlas.app.ui.theme.atlasPalette
import com.atlas.app.voice.rememberSpeechRecognizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val suggestions = listOf(
    "What did I think about rest last week?",
    "Summarize this month's business notes",
    "Pull threads on 'attention' across my journal",
    "What patterns show up in my Friday entries?"
)

@Composable
fun CommandScreen(
    shared: AtlasSharedViewModel,
    onOpenCapture: () -> Unit
) {
    val p = atlasPalette()
    val turns by shared.conversation.collectAsState()
    val thinking by shared.isThinking.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(turns.size, thinking) {
        val last = turns.size - if (thinking) 0 else 1
        if (last >= 0) listState.animateScrollToItem(last.coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize().background(p.canvas)) {
        if (turns.isEmpty()) {
            GreetingState(onSuggestion = { shared.ask(it) })
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp,
                    top = 96.dp, bottom = 160.dp
                ),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                items(turns, key = { it.id }) { TurnBubble(it) }
                if (thinking) item { ThinkingRow() }
            }
        }

        PromptBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onSend = { shared.ask(it) },
            onCapture = onOpenCapture
        )
    }
}

@Composable
private fun GreetingState(onSuggestion: (String) -> Unit) {
    val p = atlasPalette()
    val scroll = rememberScrollState()
    val now = remember { System.currentTimeMillis() }
    val time = remember {
        SimpleDateFormat("EEEE · h:mm a", Locale.getDefault()).format(Date(now))
            .uppercase(Locale.getDefault())
    }
    val hour = remember {
        SimpleDateFormat("H", Locale.getDefault()).format(Date(now)).toIntOrNull() ?: 12
    }
    val greeting = when (hour) {
        in 5..11 -> "Good morning."
        in 12..17 -> "Good afternoon."
        in 18..22 -> "Good evening."
        else -> "Still awake."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
            .padding(top = 104.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            time,
            fontFamily = Mono,
            fontSize = 11.sp,
            letterSpacing = 1.8.sp,
            color = p.text3
        )
        Spacer(Modifier.height(20.dp))
        Text(
            buildGreetingHeadline(greeting, p.accent, p.text1),
            style = TextStyle(
                fontFamily = Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                letterSpacing = (-1.1).sp
            )
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Your vault holds notes across two threads. Ask anything, capture anything, or just sit.",
            fontFamily = Serif,
            fontWeight = FontWeight.Light,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = p.text2
        )

        Spacer(Modifier.height(36.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            suggestions.forEach { s ->
                SuggestionChip(s, onClick = { onSuggestion(s) })
            }
        }
    }
}

private fun buildGreetingHeadline(
    greeting: String,
    accent: Color,
    body: Color
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = body)) {
        append("$greeting  ")
    }
    withStyle(
        SpanStyle(
            color = accent,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Light
        )
    ) {
        append("What's on your mind?")
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    val p = atlasPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.surface)
            .border(1.dp, p.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            fontFamily = Serif,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = p.text2
        )
        Icon(
            Icons.Rounded.ArrowForward,
            contentDescription = null,
            tint = p.text3,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TurnBubble(turn: Turn) {
    val p = atlasPalette()
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    when (turn.speaker) {
        Speaker.User -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "YOU · ${timeFmt.format(Date(turn.createdAt))}",
                fontFamily = Mono,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = p.text3,
                modifier = Modifier.padding(end = 4.dp, bottom = 6.dp)
            )
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(p.surfaceStrong)
                    .border(1.dp, p.borderStrong, RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text(
                    turn.text,
                    fontFamily = Serif,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    color = p.text1
                )
            }
        }
        Speaker.Atlas -> Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "ATLAS · ${timeFmt.format(Date(turn.createdAt))}",
                fontFamily = Mono,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = p.accent,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            AtlasRichText(turn.text)
            if (turn.citations.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    turn.citations.forEach { title ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(p.surface)
                                .border(1.dp, p.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                title.take(28),
                                fontFamily = Mono,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp,
                                color = p.text3
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtlasRichText(raw: String) {
    val p = atlasPalette()
    val str = remember(raw, p) {
        buildAnnotatedString {
            var i = 0
            while (i < raw.length) {
                val c = raw[i]
                if (c == '*') {
                    val end = raw.indexOf('*', i + 1)
                    if (end > i) {
                        pushStyle(
                            SpanStyle(color = p.accent, fontStyle = FontStyle.Italic)
                        )
                        append(raw.substring(i + 1, end))
                        pop()
                        i = end + 1
                        continue
                    }
                }
                append(c)
                i++
            }
        }
    }
    Text(
        str,
        fontFamily = Serif,
        fontWeight = FontWeight.Light,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = p.text1
    )
}

@Composable
private fun ThinkingRow() {
    val p = atlasPalette()
    val transition = rememberInfiniteTransition(label = "think")
    val pulse by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(p.accent)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Atlas is reading your notes…",
            fontFamily = Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
            color = p.text3
        )
    }
}

@Composable
private fun PromptBar(
    modifier: Modifier,
    onSend: (String) -> Unit,
    onCapture: () -> Unit
) {
    val p = atlasPalette()
    var text by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val recognizer = rememberSpeechRecognizer(
        onPartial = { partial -> text = partial },
        onFinal = { final -> text = final }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to p.canvas.copy(alpha = 0.85f),
                    1f to p.canvas
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        val frameBorder = if (recognizer.isRecording) p.accent else p.borderStrong
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(p.surface)
                .border(1.dp, frameBorder, RoundedCornerShape(22.dp))
                .padding(4.dp)
        ) {
            if (recognizer.isRecording) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(p.accent))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "LISTENING",
                        fontFamily = Mono,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = p.accent
                    )
                }
            }

            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .heightIn(min = 44.dp),
                textStyle = TextStyle(
                    fontFamily = Sans,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = p.text1
                ),
                cursorBrush = SolidColor(p.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            onSend(text); text = ""; focus.clearFocus()
                        }
                    }
                ),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                "Ask Atlas something…",
                                fontFamily = Sans,
                                fontSize = 16.sp,
                                color = p.text3
                            )
                        }
                        inner()
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleToolButton(
                    icon = if (recognizer.isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    active = recognizer.isRecording,
                    onClick = {
                        if (recognizer.isRecording) recognizer.stop() else recognizer.start()
                    }
                )
                Spacer(Modifier.width(4.dp))
                CircleToolButton(
                    icon = Icons.Rounded.LibraryBooks,
                    active = false,
                    onClick = onCapture
                )
                Spacer(Modifier.weight(1f))
                SendButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        onSend(text); text = ""; focus.clearFocus()
                    }
                )
            }
        }
    }
}

@Composable
private fun CircleToolButton(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val p = atlasPalette()
    val bg = if (active) p.accent else Color.Transparent
    val fg = if (active) p.canvas else p.text3
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val p = atlasPalette()
    val alpha = if (enabled) 1f else 0.3f
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(p.text1.copy(alpha = alpha))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.ArrowUpward,
            contentDescription = "Send",
            tint = p.canvas,
            modifier = Modifier.size(18.dp)
        )
    }
}
