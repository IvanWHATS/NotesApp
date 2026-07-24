package com.whats.notesapp.domain.model

data class Note(
    val id: Int = 0,
    val title: String,
    val content: List<ContentItem>,
    val updatedAt: Long,
    val backgroundColor: NoteBackgroundColor,
    val isPinned: Boolean
)