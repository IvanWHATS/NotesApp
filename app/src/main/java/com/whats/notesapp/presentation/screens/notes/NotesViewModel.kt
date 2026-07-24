@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.whats.notesapp.presentation.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whats.notesapp.domain.model.Note
import com.whats.notesapp.domain.useCases.DeleteNoteUseCase
import com.whats.notesapp.domain.useCases.GetAllNotesUseCase
import com.whats.notesapp.domain.useCases.SearchNotesUseCase
import com.whats.notesapp.domain.useCases.SwitchPinnedStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val switchPinnedStatusUseCase: SwitchPinnedStatusUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {


    private val _state = MutableStateFlow(NotesScreenState())
    val state = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        searchQuery
            .onEach { query ->
                _state.update { it.copy(query = query) }
            }
            .flatMapLatest { query ->
                if (query.isBlank()) getAllNotesUseCase()
                else searchNotesUseCase(query)
            }
            .onEach { notes ->
                val (pinned, other) = notes.partition { it.isPinned }
                _state.update {
                    it.copy(
                        pinnedNotes = pinned,
                        otherNotes = other,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: NotesScreenAction) {
        viewModelScope.launch {
            when (action) {
                is NotesScreenAction.InputSearchQuery -> {
                    searchQuery.update {
                        action.query.trim()
                    }
                }

                is NotesScreenAction.SwitchPinnedStatus -> {
                    var matchedIds: List<Int>
                    if (state.value.otherNotes.any { it.id in action.noteIds }) {
                        val otherNotesIds = state.value.otherNotes.map { it.id }
                        matchedIds = action.noteIds.filter { it in otherNotesIds }

                    } else {
                        val pinnedNotesIds = state.value.pinnedNotes.map { it.id }
                        matchedIds = action.noteIds.filter { it in pinnedNotesIds }
                    }
                    matchedIds.forEach { switchPinnedStatusUseCase(it) }
                }

                is NotesScreenAction.DeleteNotes -> {
                    action.noteIds.forEach { deleteNoteUseCase(it) }
                }
            }
        }
    }
}

data class NotesScreenState(
    val query: String = "",
    val pinnedNotes: List<Note> = listOf(),
    val otherNotes: List<Note> = listOf(),
    val isLoading: Boolean = true
)

sealed interface NotesScreenAction {

    data class InputSearchQuery(val query: String) : NotesScreenAction

    data class SwitchPinnedStatus(val noteIds: Set<Int>) : NotesScreenAction

    data class DeleteNotes(val noteIds: Set<Int>) : NotesScreenAction

}