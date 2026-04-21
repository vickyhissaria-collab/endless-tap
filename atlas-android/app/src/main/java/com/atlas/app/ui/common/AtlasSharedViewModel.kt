package com.atlas.app.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.atlas.app.AtlasApp
import com.atlas.app.data.Note
import com.atlas.app.data.NoteRepository
import com.atlas.app.data.ReadingFilter
import com.atlas.app.data.Speaker
import com.atlas.app.data.Turn
import com.atlas.app.data.Vault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class AtlasSharedViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: NoteRepository = (app as AtlasApp).notes

    private val _filter = MutableStateFlow(ReadingFilter.All)
    val filter: StateFlow<ReadingFilter> = _filter.asStateFlow()

    val notes: StateFlow<List<Note>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredNotes: StateFlow<List<Note>> = combine(notes, _filter) { list, f ->
        when (f) {
            ReadingFilter.All -> list
            ReadingFilter.Personal -> list.filter { it.vault == Vault.Personal }
            ReadingFilter.Business -> list.filter { it.vault == Vault.Business }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _conversation = MutableStateFlow<List<Turn>>(emptyList())
    val conversation: StateFlow<List<Turn>> = _conversation.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val idSeq = AtomicLong(System.currentTimeMillis())

    fun setFilter(f: ReadingFilter) { _filter.value = f }

    fun ask(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        val userTurn = Turn(idSeq.incrementAndGet(), Speaker.User, clean)
        _conversation.value = _conversation.value + userTurn
        _isThinking.value = true

        viewModelScope.launch {
            val notesSnap = notes.value
            kotlinx.coroutines.delay(700)
            val reply = synthesizeReply(clean, notesSnap)
            _conversation.value = _conversation.value + reply
            _isThinking.value = false
        }
    }

    private fun synthesizeReply(q: String, corpus: List<Note>): Turn {
        val words = q.lowercase().split(Regex("\\W+")).filter { it.length > 3 }
        val scored = corpus.map { note ->
            val hay = (note.title + " " + note.body + " " + note.tags.joinToString(" ")).lowercase()
            val score = words.count { hay.contains(it) }
            note to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(3)

        val body = if (scored.isEmpty()) {
            "I don't see anything in your vault that speaks directly to that yet. Try capturing a few notes " +
                "around it and ask again — I read best when you've fed me context."
        } else {
            val bullets = scored.joinToString("\n\n") { (n, _) ->
                "*${n.title}* — ${n.preview.take(140)}${if (n.preview.length > 140) "…" else ""}"
            }
            "Here's what I found across your vault:\n\n$bullets"
        }
        return Turn(
            id = idSeq.incrementAndGet(),
            speaker = Speaker.Atlas,
            text = body,
            citations = scored.map { it.first.title }
        )
    }

    fun saveNote(
        vault: Vault,
        raw: String
    ) {
        val text = raw.trim()
        if (text.isEmpty()) return
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: "Untitled"
        val title = firstLine.removeSuffix(".").removeSuffix(":")
        val note = Note(
            id = 0,
            vault = vault,
            title = title,
            body = text,
            tags = inferTags(text),
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch { repo.save(note) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repo.delete(note) }
    }

    private fun inferTags(text: String): List<String> {
        val explicit = Regex("(?<![\\w#])#(\\w{2,20})").findAll(text)
            .map { it.groupValues[1].lowercase() }
            .toList()
        return explicit.distinct().take(4)
    }
}
