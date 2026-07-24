package com.whats.notesapp.domain.useCases

import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.domain.model.NoteBackgroundColor
import com.whats.notesapp.domain.repository.NotesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddNoteUseCase @Inject constructor(
    private val repository: NotesRepository
) {
    suspend operator fun invoke(
        title: String,
        content: List<ContentItem>,
        backgroundColor: NoteBackgroundColor
    ): Int {
        return repository.addNote(
            title = title,
            content = content,
            backgroundColor = backgroundColor
        )
    }
}