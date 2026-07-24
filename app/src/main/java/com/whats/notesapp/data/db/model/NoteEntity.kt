package com.whats.notesapp.data.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whats.notesapp.domain.model.NoteBackgroundColor

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val updatedAt: Long,
    val backgroundColor: NoteBackgroundColor,
    val isPinned: Boolean
)