package com.atlas.app.ui.reading

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atlas.app.data.Note
import com.atlas.app.data.ReadingFilter
import com.atlas.app.data.Vault
import com.atlas.app.ui.common.AtlasSharedViewModel
import com.atlas.app.ui.theme.GoldDeep
import com.atlas.app.ui.theme.Mono
import com.atlas.app.ui.theme.PurpleDeep
import com.atlas.app.ui.theme.Sans
import com.atlas.app.ui.theme.Serif
import com.atlas.app.ui.theme.atlasPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReadingScreen(
    shared: AtlasSharedViewModel,
    onOpenNote: (Long) -> Unit,
    onOpenCapture: () -> Unit
) {
    val p = atlasPalette()
    val notes by shared.filteredNotes.collectAsState()
    val filter by shared.filter.collectAsState()
    val allCount by shared.notes.collectAsState()

    Box(Modifier.fillMaxSize().background(p.canvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp, end = 24.dp,
                top = 96.dp, bottom = 120.dp
            )
        ) {
            item {
                ReadingHeader(total = allCount.size)
                Spacer(Modifier.height(24.dp))
                FilterStrip(
                    filter = filter,
                    onChange = { shared.setFilter(it) }
                )
                Spacer(Modifier.height(20.dp))
            }

            items(notes, key = { it.id }) { note ->
                NoteCard(note = note, onClick = { onOpenNote(note.id) })
            }

            if (notes.isEmpty()) {
                item {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "Nothing here yet. Tap + to capture.",
                        fontFamily = Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        color = p.text3
                    )
                }
            }
        }

        FloatingCaptureButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 32.dp),
            onClick = onOpenCapture
        )
    }
}

@Composable
private fun ReadingHeader(total: Int) {
    val p = atlasPalette()
    val date = remember {
        SimpleDateFormat("EEEE · MMMM d", Locale.getDefault()).format(Date())
            .uppercase(Locale.getDefault())
    }
    Column {
        Text(
            "THE VAULT · $date",
            fontFamily = Mono,
            fontSize = 11.sp,
            letterSpacing = 1.9.sp,
            color = p.text3,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(14.dp))
        Text(
            buildReadingTitle(p.text1, p.accent),
            fontFamily = Serif
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "$total notes · sorted by recency",
            fontFamily = Mono,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
            color = p.text3
        )
    }
}

private fun buildReadingTitle(body: Color, accent: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = body, fontSize = 44.sp, fontWeight = FontWeight.Normal)) {
            append("Everything ")
        }
        withStyle(
            SpanStyle(
                color = accent,
                fontSize = 44.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light
            )
        ) {
            append("you've thought.")
        }
    }

@Composable
private fun FilterStrip(
    filter: ReadingFilter,
    onChange: (ReadingFilter) -> Unit
) {
    val p = atlasPalette()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(p.text1.copy(alpha = 0.04f))
            .padding(6.dp)
    ) {
        FilterPill("All", filter == ReadingFilter.All) { onChange(ReadingFilter.All) }
        FilterPill("Personal", filter == ReadingFilter.Personal) { onChange(ReadingFilter.Personal) }
        FilterPill("Business", filter == ReadingFilter.Business) { onChange(ReadingFilter.Business) }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = atlasPalette()
    val bg = if (selected) p.canvas else Color.Transparent
    val fg = if (selected) p.text1 else p.text3
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontFamily = Sans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            color = fg
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    val p = atlasPalette()
    val date = remember(note.createdAt) {
        SimpleDateFormat("MMM d · yyyy", Locale.getDefault())
            .format(Date(note.createdAt))
            .uppercase(Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VaultBadge(vault = note.vault)
            Spacer(Modifier.width(10.dp))
            Text(
                date,
                fontFamily = Mono,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp,
                color = p.text4
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            note.title,
            fontFamily = Serif,
            fontSize = 23.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp,
            color = p.text1
        )
        Spacer(Modifier.height(8.dp))
        Text(
            note.preview,
            fontFamily = Serif,
            fontSize = 16.sp,
            lineHeight = 25.sp,
            color = p.text2,
            maxLines = 3
        )
        if (note.tags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                note.tags.forEach { tag ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.text1.copy(alpha = 0.04f))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            tag,
                            fontFamily = Sans,
                            fontSize = 11.sp,
                            color = p.text3
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(p.border)
        )
    }
}

@Composable
private fun VaultBadge(vault: Vault) {
    val p = atlasPalette()
    val (bg, fg, label) = when (vault) {
        Vault.Personal -> Triple(
            Color(0x1A6A4CC8),
            if (p.isDark) Color(0xFF9D7CE8) else PurpleDeep,
            "PERSONAL"
        )
        Vault.Business -> Triple(
            Color(0x1FA07D2F),
            if (p.isDark) Color(0xFFD4A857) else GoldDeep,
            "BUSINESS"
        )
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            fontFamily = Mono,
            fontSize = 9.sp,
            letterSpacing = 1.0.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

@Composable
private fun FloatingCaptureButton(
    modifier: Modifier,
    onClick: () -> Unit
) {
    val p = atlasPalette()
    val bg = if (p.isDark) p.accent else p.text1
    val fg = if (p.isDark) p.canvas else p.canvas
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = "Capture",
            tint = fg,
            modifier = Modifier.size(24.dp)
        )
    }
}
