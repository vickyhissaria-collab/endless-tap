package com.atlas.app.data

enum class Vault { Personal, Business }

data class Note(
    val id: Long,
    val vault: Vault,
    val title: String,
    val body: String,
    val tags: List<String>,
    val createdAt: Long
) {
    val preview: String
        get() = body.lineSequence().firstOrNull { it.isNotBlank() }
            ?.take(180)
            ?: ""
}

enum class Speaker { User, Atlas }

data class Turn(
    val id: Long,
    val speaker: Speaker,
    val text: String,
    val citations: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReadingFilter { All, Personal, Business }
