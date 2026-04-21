package com.atlas.app.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

@Stable
class SpeechController(
    private val isRecordingState: MutableState<Boolean>,
    private val start: () -> Unit,
    private val stop: () -> Unit
) {
    val isRecording: Boolean get() = isRecordingState.value
    fun start() = start.invoke()
    fun stop() = stop.invoke()
}

@Composable
fun rememberSpeechRecognizer(
    onPartial: (String) -> Unit,
    onFinal: (String) -> Unit
): SpeechController {
    val context = LocalContext.current
    val isRecording = remember { mutableStateOf(false) }
    val buffer = remember { StringBuilder() }

    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    val recognizer = remember {
        if (available) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    val startIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    val beginRecording: () -> Unit = remember {
        {
            val r = recognizer ?: return@remember
            buffer.clear()
            isRecording.value = true
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { isRecording.value = false }
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {
                    val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = list?.firstOrNull().orEmpty()
                    if (partial.isNotBlank()) {
                        val merged = (buffer.toString().trim() + " " + partial).trim()
                        onPartial(merged)
                    }
                }
                override fun onResults(results: Bundle?) {
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val final = list?.firstOrNull().orEmpty()
                    if (final.isNotBlank()) {
                        if (buffer.isNotEmpty()) buffer.append(' ')
                        buffer.append(final)
                    }
                    onFinal(buffer.toString().trim())
                    isRecording.value = false
                }
            })
            r.startListening(startIntent)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) beginRecording()
    }

    val start: () -> Unit = remember {
        {
            if (!available || recognizer == null) {
                // Silent fail if no recognizer on device
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) beginRecording()
                else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val stop: () -> Unit = remember {
        {
            recognizer?.stopListening()
            isRecording.value = false
        }
    }

    DisposableEffect(recognizer) {
        onDispose { recognizer?.destroy() }
    }

    return remember { SpeechController(isRecording, start, stop) }
}
