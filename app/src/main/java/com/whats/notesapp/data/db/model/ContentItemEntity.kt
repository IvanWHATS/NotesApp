package com.whats.notesapp.data.db.model

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "notes_content",
    primaryKeys = ["noteId", "order"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class ContentItemEntity(
    val noteId: Int,
    val order: Int,
    val contentType: ContentType,
    val content: String
)

enum class ContentType {
    TEXT, IMAGE
}
