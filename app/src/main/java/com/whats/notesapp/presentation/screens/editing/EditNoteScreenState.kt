package com.whats.notesapp.presentation.screens.editing

import com.whats.notesapp.domain.model.Note
import com.whats.notesapp.presentation.model.ContentItemUiModel

sealed interface EditNoteScreenState {

    data class Editing(
        val note: Note,
        val uiContent: List<ContentItemUiModel>,
        val searchState: SearchState = SearchState.Inactive,
    ) : EditNoteScreenState {
        val isSaveEnabled: Boolean
            get() = note.title.isNotBlank()
        val isNewNote: Boolean
            get() = note.id == 0
    }

    data object Loading : EditNoteScreenState
}