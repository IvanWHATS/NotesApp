package com.whats.notesapp.domain.useCases

import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.domain.model.Note
import com.whats.notesapp.domain.model.NoteBackgroundColor
import com.whats.notesapp.domain.repository.NotesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditNoteUseCase @Inject constructor(
    private val repository: NotesRepository
) {
    suspend operator fun invoke(
        noteId: Int,
        title: String,
        content: List<ContentItem>,
        backgroundColor: NoteBackgroundColor,
        isPinned: Boolean
    ) {
        repository.editNote(
            noteId = noteId,
            title = title,
            content = content,
            backgroundColor = backgroundColor,
            isPinned = isPinned
        )
    }
}