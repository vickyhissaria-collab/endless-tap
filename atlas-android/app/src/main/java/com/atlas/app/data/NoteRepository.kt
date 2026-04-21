package com.atlas.app.data

import android.content.Context
import com.atlas.app.data.local.AtlasDatabase
import com.atlas.app.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(context: Context) {
    private val dao = AtlasDatabase.get(context).noteDao()

    fun observeAll(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun get(id: Long): Note? = dao.getById(id)?.toModel()

    suspend fun save(note: Note): Long =
        if (note.id == 0L) dao.insert(note.toEntity())
        else { dao.update(note.toEntity()); note.id }

    suspend fun delete(note: Note) = dao.delete(note.toEntity())

    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        Seed.all(now, day).forEach { dao.insert(it.toEntity()) }
    }
}

private object Seed {
    fun all(now: Long, day: Long): List<Note> = listOf(
        Note(
            id = 0,
            vault = Vault.Personal,
            title = "The shape of a quiet week",
            body = """
                Seven unstructured days. No work. Note the pattern of my thoughts when they have nowhere to land.

                Morning reading goes long. I lose track. By noon I've wandered far from the plan, which is
                fine — the plan was a scaffold, not a goal.

                Atlas should pick up these drifts and show me the through-line later. The book I keep
                returning to is less about the content than the silence between chapters.
            """.trimIndent(),
            tags = listOf("journal", "rest", "reading"),
            createdAt = now - 2 * day
        ),
        Note(
            id = 0,
            vault = Vault.Business,
            title = "Q2 strategy — sharpening the edge",
            body = """
                Three priorities for the quarter:

                1. Cut surface area. Too many small bets. Pick the two we can win.
                2. Hire a second designer — bandwidth is the blocker, not taste.
                3. Customer research loop: weekly, recorded, shared on Friday.

                The shared doc moved from "nice to have" to "required" after the last launch.
            """.trimIndent(),
            tags = listOf("strategy", "q2", "hiring"),
            createdAt = now - 5 * day
        ),
        Note(
            id = 0,
            vault = Vault.Personal,
            title = "On attention as a practice",
            body = """
                Attention is the long form of love. When I stop looking at a thing carefully, I stop
                loving it — not because the feeling dies but because the object becomes a stand-in for
                something generic.

                Keep looking. Keep looking at specific things: this tree, this hour, this sentence.
            """.trimIndent(),
            tags = listOf("attention", "essay"),
            createdAt = now - 9 * day
        ),
        Note(
            id = 0,
            vault = Vault.Business,
            title = "Pricing experiment — notes",
            body = """
                Tested three price points over two weeks. The mid-tier converts at 2.3× the low tier
                but only 0.7× the high tier on revenue per visitor.

                Takeaway: people either buy entry-level or premium — the middle is a graveyard.
                Cut the middle SKU and see what happens.
            """.trimIndent(),
            tags = listOf("pricing", "experiment"),
            createdAt = now - 11 * day
        ),
        Note(
            id = 0,
            vault = Vault.Personal,
            title = "Small ritual for starting",
            body = """
                Light the candle, sit, read two paragraphs of something old. Only then open the laptop.

                The ritual is not magic. It tells my nervous system that the thinking part of the day
                has begun. Remove it and I am just reacting to whatever surfaces first.
            """.trimIndent(),
            tags = listOf("ritual", "focus"),
            createdAt = now - 14 * day
        ),
        Note(
            id = 0,
            vault = Vault.Business,
            title = "Call with Amara — key threads",
            body = """
                She wants Atlas to feel like a "quiet room," not a product.

                Interesting phrasing: "I don't want an assistant, I want a reading companion that
                remembers what I've fed it."

                Her use case is almost entirely evening — 10pm onward. Design the dark mode accordingly.
            """.trimIndent(),
            tags = listOf("research", "atlas"),
            createdAt = now - 18 * day
        )
    )
}
