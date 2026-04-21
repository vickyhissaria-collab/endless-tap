package com.atlas.app.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atlas.app.data.Note
import com.atlas.app.data.Vault
import com.atlas.app.ui.common.AtlasSharedViewModel
import com.atlas.app.ui.theme.GoldDeep
import com.atlas.app.ui.theme.Ink1
import com.atlas.app.ui.theme.Ink2
import com.atlas.app.ui.theme.Ink3
import com.atlas.app.ui.theme.Ink4
import com.atlas.app.ui.theme.Mono
import com.atlas.app.ui.theme.Paper
import com.atlas.app.ui.theme.PurpleDeep
import com.atlas.app.ui.theme.Sans
import com.atlas.app.ui.theme.Serif
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    shared: AtlasSharedViewModel,
    noteId: Long,
    onClose: () -> Unit
) {
    val notes by shared.notes.collectAsState()
    val note = remember(noteId, notes) { notes.firstOrNull { it.id == noteId } }
    val scroll = rememberScrollState()

    var visible by remember { mutableStateOf(true) }
    BackHandler { visible = false }
    LaunchedEffect(visible) {
        if (!visible) {
            kotlinx.coroutines.delay(240)
            onClose()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(320)) { it / 4 } + fadeIn(tween(240)),
        exit = slideOutVertically(tween(240)) { it / 4 } + fadeOut(tween(200))
    ) {
        Box(Modifier.fillMaxSize().background(Paper)) {
            if (note == null) {
                Text(
                    "Note not found.",
                    modifier = Modifier.align(Alignment.Center),
                    fontFamily = Serif,
                    color = Ink3
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 28.dp)
                        .padding(top = 20.dp, bottom = 80.dp)
                ) {
                    DetailTopBar(
                        onBack = { visible = false },
                        onDelete = {
                            shared.deleteNote(note)
                            visible = false
                        }
                    )
                    Spacer(Modifier.height(40.dp))
                    VaultStamp(vault = note.vault)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        note.title,
                        fontFamily = Serif,
                        fontSize = 38.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.7).sp,
                        color = Ink1
                    )
                    Spacer(Modifier.height(14.dp))
                    val formatted = remember(note.createdAt) {
                        SimpleDateFormat("EEEE · MMMM d · h:mm a", Locale.getDefault())
                            .format(Date(note.createdAt))
                            .uppercase(Locale.getDefault())
                    }
                    Text(
                        formatted,
                        fontFamily = Mono,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = Ink3
                    )
                    Spacer(Modifier.height(28.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x141A140C)))
                    Spacer(Modifier.height(28.dp))

                    note.body.split("\n\n").forEach { para ->
                        if (para.isNotBlank()) {
                            Text(
                                para.trim(),
                                fontFamily = Serif,
                                fontSize = 18.sp,
                                lineHeight = 30.sp,
                                color = Ink2
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    if (note.tags.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            note.tags.forEach { t ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x0F1A140C))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        t,
                                        fontFamily = Sans,
                                        fontSize = 11.sp,
                                        color = Ink3
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CircleIconButton(
            icon = Icons.Rounded.ArrowBack,
            description = "Back",
            onClick = onBack
        )
        CircleIconButton(
            icon = Icons.Rounded.Delete,
            description = "Delete",
            onClick = onDelete,
            tint = Color(0xFFA0463A)
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Ink2
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x0F1A140C))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun VaultStamp(vault: Vault) {
    val (bg, fg, label) = when (vault) {
        Vault.Personal -> Triple(Color(0x1A6A4CC8), PurpleDeep, "PERSONAL")
        Vault.Business -> Triple(Color(0x1FA07D2F), GoldDeep, "BUSINESS")
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}
