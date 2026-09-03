package com.whats.notesapp.presentation.screens.editing

import androidx.compose.ui.text.TextRange
import com.whats.notesapp.domain.model.Note
import com.whats.notesapp.presentation.model.ContentItemUiModel

sealed interface EditNoteScreenState {

    data class Editing(
        val note: Note,
        val uiContent: List<ContentItemUiModel>,
        val searchState: SearchState = SearchState.Inactive,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
        val cursorPosition: TextRange? = null,
        val scrollOffset: Int = 0,
        val isPreviewMode: Boolean = true,
    ) : EditNoteScreenState {
        val isSaveEnabled: Boolean
            get() = note.title.isNotBlank()
        val isNewNote: Boolean
            get() = note.id == 0
    }

    data object Loading : EditNoteScreenState
}