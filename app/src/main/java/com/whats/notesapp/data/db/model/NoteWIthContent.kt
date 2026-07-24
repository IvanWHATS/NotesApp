package com.whats.notesapp.data.db.model

import androidx.room.Embedded
import androidx.room.Relation


data class NoteWIthContent(
    @Embedded
    val note: NoteEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val content: List<ContentItemEntity>
)
