package com.whats.notesapp.presentation.screens.editing

sealed interface EditNoteEvent {
    data object NavigateBack : EditNoteEvent

    data class RequestFocus(val target: FocusTarget?) : EditNoteEvent
}