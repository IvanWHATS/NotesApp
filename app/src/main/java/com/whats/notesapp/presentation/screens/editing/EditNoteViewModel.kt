package com.whats.notesapp.presentation.screens.editing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.domain.model.Note
import com.whats.notesapp.domain.model.NoteBackgroundColor
import com.whats.notesapp.domain.useCases.AddNoteUseCase
import com.whats.notesapp.domain.useCases.DeleteNoteUseCase
import com.whats.notesapp.domain.useCases.EditNoteUseCase
import com.whats.notesapp.domain.useCases.GetNoteUseCase
import com.whats.notesapp.domain.useCases.SwitchPinnedStatusUseCase
import com.whats.notesapp.presentation.mappers.toUiContent
import com.whats.notesapp.presentation.model.ContentItemUiModel
import com.whats.notesapp.presentation.model.TextMatch
import com.whats.notesapp.presentation.utils.SearchTextMatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EditNoteViewModel.Factory::class)
class EditNoteViewModel @AssistedInject constructor(
    @Assisted("noteId") private val noteId: Int?,
    private val addNoteUseCase: AddNoteUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val editNoteUseCase: EditNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val switchPinnedStatusUseCase: SwitchPinnedStatusUseCase
) : ViewModel() {

    private var lastSavedNote: Note? = null

    private val _state = MutableStateFlow<EditNoteScreenState>(
        EditNoteScreenState.Loading
    )
    val state = _state.asStateFlow()


    private val _events = Channel<EditNoteEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val noteHistory = History<Note>()


    init {
        loadNote()
        observeUiContent()
        observeSelectedMatch()
    }

    private fun loadNote() {
        viewModelScope.launch {
            _state.update {
                val initialNote = noteId?.let {
                    getNoteUseCase(noteId).let {
                        if (it.content.isEmpty() || it.content.last() !is ContentItem.Text)
                            it.copy(content = it.content + ContentItem.Text(""))
                        else it
                    }
                } ?: Note(
                    title = "",
                    content = listOf(ContentItem.Text("")),
                    updatedAt = System.currentTimeMillis(),
                    backgroundColor = NoteBackgroundColor.Default,
                    isPinned = false
                )

                lastSavedNote = initialNote

                val initialUiContent = initialNote.content.toUiContent()

                EditNoteScreenState.Editing(
                    note = initialNote,
                    uiContent = initialUiContent
                )
            }
        }
    }


    @OptIn(FlowPreview::class)
    private fun observeUiContent() {
        viewModelScope.launch {
            val contentFlow =
                _state
                    .mapNotNull { (it as? EditNoteScreenState.Editing)?.note?.content }
                    .distinctUntilChanged()

            val queryFlow =
                _state
                    .map {
                        (it as? EditNoteScreenState.Editing)
                            ?.searchState
                    }
                    .filterIsInstance<SearchState.Active>()
                    .map { it.query }
                    .debounce { if (it.isBlank()) 0 else 300 }
                    .distinctUntilChanged()

            combine(contentFlow, queryFlow) { content, query ->
                content to query
            }.collectLatest { (content, query) ->
                val currentState =
                    _state.value as? EditNoteScreenState.Editing
                        ?: return@collectLatest

                val syncedUiContent =
                    currentState.uiContent.syncText(content)

                if (currentState.searchState !is SearchState.Active) {
                    _state.update { state ->
                        if (state is EditNoteScreenState.Editing) {
                            state.copy(uiContent = syncedUiContent)
                        } else state
                    }
                    return@collectLatest
                }

                val matches = SearchTextMatcher.getTextMatches(content, query)

                val activeIndex =
                    if (matches.isNotEmpty()) 0 else null

                val searchedUiContent = syncedUiContent.applySearchHighlights(
                    searchMatches = matches,
                    activeMatchIndex = activeIndex
                )

                _state.update { state ->
                    if (state is EditNoteScreenState.Editing) {
                        state.copy(
                            uiContent = searchedUiContent,
                            searchState = SearchState.Active(
                                query = query,
                                matches = matches,
                                activeMatchIndex = activeIndex
                            )
                        )
                    } else state
                }
            }
        }
    }

    private fun observeSelectedMatch() {
        viewModelScope.launch {
            _state
                .mapNotNull { state ->
                    (state as? EditNoteScreenState.Editing)
                        ?.searchState
                        ?.let { it as? SearchState.Active }
                        ?.activeMatchIndex
                }
                .distinctUntilChanged()
                .collectLatest { activeIndex ->

                    val currentState =
                        _state.value as? EditNoteScreenState.Editing ?: return@collectLatest

                    val searchState =
                        currentState.searchState as? SearchState.Active
                            ?: return@collectLatest

                    val updatedUiContent =
                        currentState.uiContent.applySearchHighlights(
                            searchMatches = searchState.matches,
                            activeMatchIndex = activeIndex
                        )

                    _state.update {
                        if (it is EditNoteScreenState.Editing) {
                            it.copy(uiContent = updatedUiContent)
                        } else it
                    }
                }
        }
    }

    private fun List<ContentItemUiModel>.syncText(rawContent: List<ContentItem>): List<ContentItemUiModel> {
        return map { uiModel ->
            if (uiModel is ContentItemUiModel.Text) {
                val newText =
                    (rawContent.getOrNull(uiModel.index) as? ContentItem.Text)?.text ?: uiModel.text
                if (uiModel.text != newText) {

                    uiModel.copy(
                        text = newText
                    )
                } else uiModel
            } else uiModel
        }
    }

    private fun List<ContentItemUiModel>.applySearchHighlights(
        searchMatches: List<TextMatch>,
        activeMatchIndex: Int?
    ): List<ContentItemUiModel> {
        if (searchMatches.isEmpty() && activeMatchIndex == null) {
            val hasAnyHighlight =
                any { it is ContentItemUiModel.Text && it.searchMatches.isNotEmpty() }
            if (!hasAnyHighlight) return this
        }

        val matchesByIndex = searchMatches.groupBy { it.contentItemIndex }
        val activeMatch = activeMatchIndex?.let { searchMatches.getOrNull(it) }

        return map { uiModel ->
            when (uiModel) {
                is ContentItemUiModel.ImageGroup -> uiModel
                is ContentItemUiModel.Text -> {
                    val textMatches =
                        matchesByIndex[uiModel.index]?.map { it.charRange } ?: emptyList()
                    val activeRange =
                        activeMatch?.takeIf { it.contentItemIndex == uiModel.index }?.charRange

                    if (uiModel.searchMatches == textMatches && uiModel.activeMatchRange == activeRange) {
                        uiModel
                    } else {
                        uiModel.copy(searchMatches = textMatches, activeMatchRange = activeRange)
                    }
                }
            }
        }
    }


    fun onAction(action: EditNoteScreenAction) {
        when (action) {
            is EditNoteScreenAction.InputTitle -> updateTitle(action.title)

            is EditNoteScreenAction.InputContent -> updateContent(action.content, action.index)

            is EditNoteScreenAction.AddImage -> addImage(action.uri)

            is EditNoteScreenAction.DeleteImage -> deleteImage(action.index)

            is EditNoteScreenAction.InputSearchQuery -> applySearch(action.query)

            is EditNoteScreenAction.ChangeBackground -> changeBackground(action.backgroundColor)

            EditNoteScreenAction.SaveChanges -> viewModelScope.launch {
                saveIfNeeded()
            }

            EditNoteScreenAction.DeleteNote -> deleteNote()

            EditNoteScreenAction.NavigateBack -> navigateBack()

            EditNoteScreenAction.SwitchPinStatus -> switchPinStatus()

            EditNoteScreenAction.OpenSearch -> openSearch()

            EditNoteScreenAction.CloseSearch -> closeSearch()

            EditNoteScreenAction.SelectNextMatch -> selectNextMatch()

            EditNoteScreenAction.SelectPreviousMatch -> selectPreviousMatch()
        }
    }

    private fun updateTitle(title: String) {
        _state.update { prevState ->
            if (prevState is EditNoteScreenState.Editing) {
                val newNote = prevState.note.copy(title = title)
                prevState.copy(note = newNote)
            } else {
                prevState
            }
        }
    }

    private fun updateContent(content: String, index: Int) {
        _state.update { prevState ->
            if (prevState is EditNoteScreenState.Editing) {
                val newContent = prevState.note.content.mapIndexed { contentIndex, contentItem ->
                    if (contentIndex == index && contentItem is ContentItem.Text) {
                        contentItem.copy(text = content)
                    } else {
                        contentItem
                    }
                }
                val newNote = prevState.note.copy(content = newContent)
                prevState.copy(note = newNote)
            } else {
                prevState
            }
        }
    }

    private fun addImage(uri: Uri) {
        _state.update { prevState ->
            if (prevState is EditNoteScreenState.Editing) {
                prevState.note.content.toMutableList().apply {
                    if (lastOrNull() is ContentItem.Text &&
                        (last() as ContentItem.Text).text.isBlank()
                    ) {
                        removeAt(lastIndex)
                    }
                    add(ContentItem.Image(uri.toString()))
                    add(ContentItem.Text(""))
                }.let {
                    val newNote = prevState.note.copy(content = it)
                    prevState.copy(
                        note = newNote,
                        uiContent = it.toUiContent()
                    )
                }
            } else {
                prevState
            }
        }
    }

    private fun deleteImage(index: Int) {
        _state.update { prevState ->
            if (prevState is EditNoteScreenState.Editing) {
                prevState.note.content.toMutableList().apply {
                    if (index in indices &&
                        get(index) is ContentItem.Image
                    ) {
                        removeAt(index)
                    }

                    if (lastOrNull() !is ContentItem.Text) {
                        add(ContentItem.Text(""))
                    }
                }.let {
                    val newNote = prevState.note.copy(content = it)
                    prevState.copy(
                        note = newNote,
                        uiContent = it.toUiContent()
                    )
                }
            } else {
                prevState
            }
        }
    }

    private fun applySearch(query: String) {
        _state.update { state ->
            if (state is EditNoteScreenState.Editing &&
                state.searchState is SearchState.Active
            ) {
                state.copy(
                    searchState = state.searchState.copy(
                        query = query
                    )
                )
            } else state
        }
    }

    private fun changeBackground(backgroundColor: NoteBackgroundColor) {
        _state.update { state ->
            if (state is EditNoteScreenState.Editing) {

                val newNote = state.note.copy(
                    backgroundColor = backgroundColor
                )
                state.copy(note = newNote)
            } else state
        }
    }

    private suspend fun saveIfNeeded() {
        val state = _state.value as? EditNoteScreenState.Editing ?: return
        if (state.note == lastSavedNote) return
        if (!state.isSaveEnabled) return //TODO("Error popup")
        state.run {
            val content = note.content.filter {
                it !is ContentItem.Text || it.text.isNotBlank()
            }
            if (isNewNote) {
                val newNoteId = addNoteUseCase(
                    title = note.title,
                    content = content,
                    backgroundColor = note.backgroundColor
                )

                val savedNote = note.copy(id = newNoteId)

                _state.update {
                    (it as EditNoteScreenState.Editing).copy(
                        note = savedNote
                    )
                }

                lastSavedNote = savedNote

            } else {
                editNoteUseCase(
                    noteId = note.id,
                    title = note.title,
                    content = content,
                    backgroundColor = note.backgroundColor,
                    isPinned = note.isPinned
                )
                lastSavedNote = note
            }

        }
    }

    private fun deleteNote() {
        viewModelScope.launch {
            (_state.value as? EditNoteScreenState.Editing)?.apply {
                deleteNoteUseCase(note.id)
                _events.send(EditNoteEvent.NavigateBack)
            }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            saveIfNeeded()
            _events.send(EditNoteEvent.NavigateBack)
        }
    }

    private fun switchPinStatus() {
        viewModelScope.launch {
            _state.update { prevState ->
                if (prevState is EditNoteScreenState.Editing) {
                    switchPinnedStatusUseCase(prevState.note.id)
                    val newNote = prevState.note.copy(isPinned = !prevState.note.isPinned)
                    prevState.copy(note = newNote)
                } else {
                    prevState
                }
            }
        }
    }

    private fun openSearch() {
        _state.update { state ->
            if (state is EditNoteScreenState.Editing &&
                state.searchState is SearchState.Inactive
            ) {
                state.copy(
                    searchState = SearchState.Active()
                )
            } else state
        }
    }

    private fun closeSearch() {
        _state.update { state ->
            if (state is EditNoteScreenState.Editing &&
                state.searchState is SearchState.Active
            ) {
                state.copy(
                    searchState = SearchState.Inactive,
                    uiContent = state.uiContent.applySearchHighlights(
                        searchMatches = emptyList(),
                        activeMatchIndex = null
                    )
                )
            } else state
        }
    }

    private fun selectNextMatch() {
        _state.update { state ->
            if (state is EditNoteScreenState.Editing &&
                state.searchState is SearchState.Active &&
                state.searchState.hasMatches
            ) {
                val searchState = state.searchState

                state.copy(
                    searchState = searchState.copy(activeMatchIndex = searchState.nextMatchIndex())
                )
            } else state
        }
    }

    private fun selectPreviousMatch() {
        _state.update { state ->
            if (state is EditNoteScreenState.Editing &&
                state.searchState is SearchState.Active &&
                state.searchState.hasMatches
            ) {
                val searchState = state.searchState

                state.copy(
                    searchState = searchState.copy(activeMatchIndex = searchState.previousMatchIndex())
                )
            } else state
        }
    }

    private fun SearchState.Active.nextMatchIndex(): Int =
        when (val current = activeMatchIndex) {
            null -> 0
            else -> (current + 1) % matches.size
        }

    private fun SearchState.Active.previousMatchIndex(): Int =
        when (val current = activeMatchIndex) {
            null -> 0
            0 -> matches.lastIndex
            else -> current - 1
        }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("noteId") noteId: Int?
        ): EditNoteViewModel
    }
}