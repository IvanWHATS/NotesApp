package com.whats.notesapp.presentation.screens.editing

import android.net.Uri
import com.whats.notesapp.domain.model.NoteBackgroundColor

sealed interface EditNoteScreenAction {

    data class InputTitle(val title: String) : EditNoteScreenAction

    data class InputContent(val content: String, val index: Int) : EditNoteScreenAction

    data class AddImage(val uri: Uri) : EditNoteScreenAction

    data class DeleteImage(val index: Int) : EditNoteScreenAction

    data class InputSearchQuery(val query: String) : EditNoteScreenAction

    data class ChangeBackground(val backgroundColor: NoteBackgroundColor) : EditNoteScreenAction

    data object SaveChanges : EditNoteScreenAction

    data object DeleteNote : EditNoteScreenAction

    data object NavigateBack : EditNoteScreenAction

    data object SwitchPinStatus : EditNoteScreenAction

    data object OpenSearch : EditNoteScreenAction

    data object CloseSearch : EditNoteScreenAction

    data object SelectNextMatch : EditNoteScreenAction

    data object SelectPreviousMatch : EditNoteScreenAction
}