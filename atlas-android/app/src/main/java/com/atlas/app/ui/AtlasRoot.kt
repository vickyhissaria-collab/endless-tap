package com.atlas.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlas.app.ui.capture.CaptureSheet
import com.atlas.app.ui.command.CommandScreen
import com.atlas.app.ui.common.AtlasSharedViewModel
import com.atlas.app.ui.common.IdentityStrip
import com.atlas.app.ui.detail.DetailScreen
import com.atlas.app.ui.reading.ReadingScreen
import com.atlas.app.ui.theme.AtlasMode
import com.atlas.app.ui.theme.AtlasTheme
import com.atlas.app.ui.theme.atlasPalette

@Composable
fun AtlasRoot() {
    var mode by rememberSaveable { mutableStateOf(AtlasMode.Command) }
    var showCapture by remember { mutableStateOf(false) }
    var openedNoteId by remember { mutableStateOf<Long?>(null) }
    val shared: AtlasSharedViewModel = viewModel()

    AtlasTheme(mode = mode) {
        val palette = atlasPalette()
        val bg by animateColorAsState(palette.canvas, tween(600), label = "bg")

        Box(Modifier.fillMaxSize().background(bg)) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(400)) togetherWith fadeOut(tween(200)))
                },
                label = "mode"
            ) { current ->
                when (current) {
                    AtlasMode.Command -> CommandScreen(
                        shared = shared,
                        onOpenCapture = { showCapture = true }
                    )
                    AtlasMode.Reading -> ReadingScreen(
                        shared = shared,
                        onOpenNote = { openedNoteId = it },
                        onOpenCapture = { showCapture = true }
                    )
                }
            }

            IdentityStrip(
                mode = mode,
                onModeChange = { mode = it }
            )

            if (showCapture) {
                CaptureSheet(
                    shared = shared,
                    onDismiss = { showCapture = false }
                )
            }

            val id = openedNoteId
            if (id != null) {
                DetailScreen(
                    shared = shared,
                    noteId = id,
                    onClose = { openedNoteId = null }
                )
            }
        }
    }
}
