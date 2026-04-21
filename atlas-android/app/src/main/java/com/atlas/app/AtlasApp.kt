package com.atlas.app

import android.app.Application
import com.atlas.app.data.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AtlasApp : Application() {
    lateinit var notes: NoteRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notes = NoteRepository(this)
        appScope.launch { notes.seedIfEmpty() }
    }
}
