package com.whats.notesapp.presentation.screens.editing

sealed interface EditNoteEvent {
    data object NavigateBack : EditNoteEvent
}