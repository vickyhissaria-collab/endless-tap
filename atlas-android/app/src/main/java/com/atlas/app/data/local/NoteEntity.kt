package com.atlas.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.atlas.app.data.Note
import com.atlas.app.data.Vault

class Converters {
    @TypeConverter fun fromTags(value: List<String>): String = value.joinToString("|")
    @TypeConverter fun toTags(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split("|")

    @TypeConverter fun fromVault(v: Vault): String = v.name
    @TypeConverter fun toVault(v: String): Vault = Vault.valueOf(v)
}

@Entity(tableName = "notes")
@TypeConverters(Converters::class)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vault: Vault,
    val title: String,
    val body: String,
    val tags: List<String>,
    val createdAt: Long
) {
    fun toModel() = Note(id, vault, title, body, tags, createdAt)
}

fun Note.toEntity() = NoteEntity(id, vault, title, body, tags, createdAt)
