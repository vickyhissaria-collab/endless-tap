package com.atlas.app.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atlas.app.data.Vault
import com.atlas.app.ui.common.AtlasSharedViewModel
import com.atlas.app.ui.theme.Ink1
import com.atlas.app.ui.theme.Paper
import com.atlas.app.ui.theme.PurpleDeep
import com.atlas.app.ui.theme.Sans
import com.atlas.app.ui.theme.Serif
import com.atlas.app.voice.rememberSpeechRecognizer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    shared: AtlasSharedViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var vault by remember { mutableStateOf(Vault.Personal) }
    val baseText = remember { mutableStateOf("") }
    val recognizer = rememberSpeechRecognizer(
        onPartial = { partial ->
            text = combineDictation(baseText.value, partial)
        },
        onFinal = { final ->
            text = combineDictation(baseText.value, final)
            baseText.value = text
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Paper,
        dragHandle = { CaptureHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .fillMaxWidth()
        ) {
            Text(
                "Capture",
                fontFamily = Serif,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.6).sp,
                fontWeight = FontWeight.Medium,
                color = Ink1
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Write loose. Atlas will title it and tag it from the first line.",
                fontFamily = Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF6B5D47)
            )
            Spacer(Modifier.height(22.dp))

            VaultToggle(vault = vault, onChange = { vault = it })
            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0x1F1A140C), RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    textStyle = TextStyle(
                        fontFamily = Serif,
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        color = Ink1
                    ),
                    cursorBrush = SolidColor(PurpleDeep),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(
                                "What are you thinking?",
                                fontFamily = Serif,
                                fontStyle = FontStyle.Italic,
                                fontSize = 17.sp,
                                color = Color(0xFFA89B80)
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SheetTool(
                    label = if (recognizer.isRecording) "Stop" else "Dictate",
                    active = recognizer.isRecording,
                    icon = if (recognizer.isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    onClick = {
                        if (recognizer.isRecording) {
                            recognizer.stop()
                        } else {
                            baseText.value = text
                            recognizer.start()
                        }
                    }
                )
                SaveButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        shared.saveNote(vault, text)
                        text = ""
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

private fun combineDictation(base: String, spoken: String): String {
    val b = base.trimEnd()
    val s = spoken.trim()
    if (s.isEmpty()) return base
    if (b.isEmpty()) return s
    val sep = if (b.endsWith(".") || b.endsWith("\n")) "\n" else " "
    return b + sep + s
}

@Composable
private fun CaptureHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .width(40.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33A89B80))
    )
}

@Composable
private fun VaultToggle(vault: Vault, onChange: (Vault) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x141A140C))
            .padding(5.dp)
    ) {
        VaultPill(
            label = "Personal",
            icon = Icons.Rounded.Person,
            selected = vault == Vault.Personal,
            onClick = { onChange(Vault.Personal) }
        )
        VaultPill(
            label = "Business",
            icon = Icons.Rounded.Work,
            selected = vault == Vault.Business,
            onClick = { onChange(Vault.Business) }
        )
    }
}

@Composable
private fun VaultPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Paper else Color.Transparent
    val fg = if (selected) Ink1 else Color(0xFF6B5D47)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontFamily = Sans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

@Composable
private fun SheetTool(
    label: String,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val bg = if (active) Color(0x1FD4A857) else Color(0x141A140C)
    val fg = if (active) Color(0xFFA07D2F) else Color(0xFF3A2F1F)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontFamily = Sans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) Ink1 else Ink1.copy(alpha = 0.3f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Save",
            fontFamily = Sans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Paper
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Rounded.ArrowForward,
            contentDescription = null,
            tint = Paper,
            modifier = Modifier.size(14.dp)
        )
    }
}
