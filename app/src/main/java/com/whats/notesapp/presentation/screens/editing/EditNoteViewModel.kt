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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
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
        Loading
    )
    val state = _state.asStateFlow()


    private val _events = Channel<EditNoteEvent>(Channel.CONFLATED)
    val events = _events.receiveAsFlow()

    private val editFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1
    )

    private val history = History<Note>()

    //private var pendingHistoryNote: Note? = null

    init {
        loadNote()
        observeEditableChanges()
        observeSearchableContent()
        observeSelectedMatch()
    }

    private fun loadNote() {
        viewModelScope.launch {
            val initialNote = noteId?.let {
                getNoteUseCase(noteId).let {
                    if (it.content.isEmpty() || it.content.last() !is Text)
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

            history.push(initialNote)

            val initialUiContent = initialNote.content.toUiContent()

            _state.update {
                EditNoteScreenState.Editing(
                    note = initialNote,
                    uiContent = initialUiContent
                )
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchableContent() {
        viewModelScope.launch {
            val contentFlow =
                _state
                    .mapNotNull { (it as? Editing)?.note?.content }
                    .distinctUntilChanged()

            val queryFlow =
                _state
                    .map {
                        (it as? Editing)
                            ?.searchState
                    }
                    .filterIsInstance<SearchState.Active>()
                    .map { it.query }
                    .debounce { if (it.isBlank()) 0 else 100 }
                    .distinctUntilChanged()

            combine(contentFlow, queryFlow) { content, query ->
                content to query
            }.collectLatest { (content, query) ->
                val currentState =
                    _state.value as? Editing
                        ?: return@collectLatest

                if (currentState.searchState !is Active) {
                    return@collectLatest
                }

                val matches = SearchTextMatcher.getTextMatches(content, query)

                val activeIndex =
                    if (matches.isNotEmpty()) 0 else null

                val searchedUiContent = currentState.uiContent.applySearchHighlights(
                    searchMatches = matches,
                    activeMatchIndex = activeIndex
                )

                _state.update { state ->
                    if (state is Editing) {
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
                    (state as? Editing)
                        ?.searchState
                        ?.let { it as? Active }
                        ?.activeMatchIndex
                }
                .distinctUntilChanged()
                .collectLatest { activeIndex ->

                    val currentState =
                        _state.value as? Editing ?: return@collectLatest

                    val searchState =
                        currentState.searchState as? Active
                            ?: return@collectLatest

                    val updatedUiContent =
                        currentState.uiContent.applySearchHighlights(
                            searchMatches = searchState.matches,
                            activeMatchIndex = activeIndex
                        )

                    _state.update {
                        if (it is Editing) {
                            it.copy(uiContent = updatedUiContent)
                        } else it
                    }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeEditableChanges() {
        viewModelScope.launch {
            editFlow
                .debounce(500)
                .collectLatest {
                    commitPendingEdit()
                }
        }
    }

    private fun commitPendingEdit() {
        val currentState = _state.value as? Editing ?: return

        // Защита от мусора в стеке: пушим только если текст изменился
        if (history.current != currentState.note) {
            history.push(currentState.note)
            _state.update { state ->
                if (state is Editing) {
                    state.copy(
                        canUndo = history.canUndo,
                        canRedo = history.canRedo
                    )
                } else state
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
                is ImageGroup -> uiModel
                is Text -> {
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

    private enum class NoteUpdateType {
        TextOnly,
        StructureChanged,
        Title,
        MetadataOnly
    }

    private fun updateNote(
        updateType: NoteUpdateType,
        transform: (Note) -> Note
    ) {
        _state.update { state ->
            if (state is Editing) {
                val newNote = transform(state.note)
                state.copy(
                    note = newNote,
                    uiContent = when (updateType) {
                        TextOnly -> state.uiContent.syncText(newNote.content)
                        StructureChanged -> newNote.content.toUiContent()
                        Title -> state.uiContent
                        MetadataOnly -> state.uiContent
                    }
                )
            } else state
        }
        when (updateType) {
            TextOnly,
            StructureChanged,
            Title -> editFlow.tryEmit(Unit)
            MetadataOnly -> {}
        }
    }

    private fun determineFocusTarget(
        oldNote: Note,
        newNote: Note
    ): FocusTarget? {
        val oldContent = oldNote.content.associateBy { it.id }
        val newContent = newNote.content.associateBy { it.id }

        // 1. Добавлен блок? → фокус на него
        val added = newNote.content.firstOrNull { it.id !in oldContent }
        if (added != null) {
            return when (added) {
                is ContentItem.Text -> FocusTarget.TextBlock(
                    id = added.id,
                    cursorPosition = added.text.length
                )
                is ContentItem.Image -> FocusTarget.ImageBlock(added.id)
            }
        }

        // 2. Удалён блок? → фокус на соседний (предыдущий или следующий)
        val removed = oldNote.content.firstOrNull { it.id !in newContent }
        if (removed != null) {
            val removedIndex = oldNote.content.indexOfFirst { it.id == removed.id }
            // Сначала смотрим на тот же индекс, если нет — на последний
            val target = newNote.content.getOrNull(removedIndex)
                ?: newNote.content.getOrNull(removedIndex - 1)
                ?: newNote.content.lastOrNull()

            return when (target) {
                is ContentItem.Text -> FocusTarget.TextBlock(target.id, target.text.length)
                is ContentItem.Image -> FocusTarget.ImageBlock(target.id)
                null -> null
            }
        }

        // 3. Изменился существующий блок? → фокус на него
        val changed = newNote.content.firstOrNull { newItem ->
            oldContent[newItem.id] != newItem
        }
        if (changed != null) {
            return when (changed) {
                is ContentItem.Text -> {
                    val oldText = (oldContent[changed.id] as? ContentItem.Text)?.text ?: ""
                    val newText = changed.text
                    // Если текст дописывался в конец — ставим курсор в конец
                    val cursor = if (newText.startsWith(oldText)) newText.length else newText.length
                    FocusTarget.TextBlock(changed.id, cursor)
                }
                is ContentItem.Image -> FocusTarget.ImageBlock(changed.id)
            }
        }

        // 4. Изменился title?
        if (oldNote.title != newNote.title) {
            return FocusTarget.Title(newNote.title.length)
        }

        return null
    }

    private fun restoreNote(note: Note) {
        _state.update { state ->
            if (state is Editing) {
                state.copy(
                    note = note,
                    uiContent = note.content.toUiContent(),
                    canUndo = history.canUndo,
                    canRedo = history.canRedo
                )
            } else {
                state
            }
        }
    }

    fun onAction(action: EditNoteScreenAction) {
        when (action) {
            is InputTitle -> updateTitle(action.title)

            is InputContent -> updateTextContent(action.content, action.index)

            is AddImage -> addImage(action.uri)

            is DeleteImage -> deleteImage(action.index)

            is InputSearchQuery -> applySearch(action.query)

            is ChangeBackground -> changeBackground(action.backgroundColor)

            SaveChanges -> viewModelScope.launch {
                saveIfNeeded()
            }

            DeleteNote -> deleteNote()

            NavigateBack -> navigateBack()

            SwitchPinStatus -> switchPinStatus()

            OpenSearch -> openSearch()

            CloseSearch -> closeSearch()

            SelectNextMatch -> selectNextMatch()

            SelectPreviousMatch -> selectPreviousMatch()

            UndoChange -> undo()

            RedoChange -> redo()
        }
    }

    private fun updateTitle(title: String) {
        updateNote(
            Title
        ) { note -> note.copy(title = title) }
    }

    private fun updateTextContent(content: String, index: Int) {
        updateNote(
            TextOnly
        ) { note ->
            val newContent = note.content.mapIndexed { contentIndex, contentItem ->
                if (contentIndex == index && contentItem is Text) {
                    contentItem.copy(text = content)
                } else {
                    contentItem
                }
            }
            val newNote = note.copy(content = newContent)
            newNote
        }
    }

    private fun addImage(uri: Uri) {
        updateNote(
            StructureChanged
        ) { note ->
            note.content.toMutableList().apply {
                if (lastOrNull() is Text &&
                    (last() as Text).text.isBlank()
                ) {
                    removeAt(lastIndex)
                }
                add(ContentItem.Image(uri.toString()))
                add(ContentItem.Text(""))
            }.let {
                val newNote = note.copy(content = it)
                newNote
            }
        }
    }

    private fun deleteImage(index: Int) {
        updateNote(
            StructureChanged
        ) { note ->
            note.content.toMutableList().apply {
                if (index in indices &&
                    get(index) is Image
                ) {
                    removeAt(index)
                }

                if (lastOrNull() !is Text) {
                    add(ContentItem.Text(""))
                }
            }.let {
                val newNote = note.copy(content = it)
                newNote
            }
        }
    }

    private fun applySearch(query: String) {
        _state.update { state ->
            if (state is Editing &&
                state.searchState is Active
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
        updateNote(
            MetadataOnly
        ) { note -> note.copy(backgroundColor = backgroundColor) }
    }

    private suspend fun saveIfNeeded() {
        val state = _state.value as? Editing ?: return
        if (state.note == lastSavedNote) return
        if (!state.isSaveEnabled) return //TODO("Error popup")
        state.run {
            val content = note.content.filter {
                it !is Text || it.text.isNotBlank()
            }
            if (isNewNote) {
                val newNoteId = addNoteUseCase(
                    title = note.title,
                    content = content,
                    backgroundColor = note.backgroundColor
                )

                val savedNote = note.copy(id = newNoteId)

                _state.update {
                    (it as Editing).copy(
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
            (_state.value as? Editing)?.apply {
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
            val state = _state.value as? Editing
                ?: return@launch

            switchPinnedStatusUseCase(state.note.id)

            _state.update { state ->
                if (state is Editing) {
                    state.copy(
                        note = state.note.copy(
                            isPinned = !state.note.isPinned
                        )
                    )
                } else {
                    state
                }
            }
        }
    }

    private fun openSearch() {
        _state.update { state ->
            if (state is Editing &&
                state.searchState is Inactive
            ) {
                state.copy(
                    searchState = SearchState.Active()
                )
            } else state
        }
    }

    private fun closeSearch() {
        _state.update { state ->
            if (state is Editing &&
                state.searchState is Active
            ) {
                state.copy(
                    searchState = Inactive,
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
            if (state is Editing &&
                state.searchState is Active &&
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
            if (state is Editing &&
                state.searchState is Active &&
                state.searchState.hasMatches
            ) {
                val searchState = state.searchState

                state.copy(
                    searchState = searchState.copy(activeMatchIndex = searchState.previousMatchIndex())
                )
            } else state
        }
    }

    private fun undo() {
        commitPendingEdit()

        val currentNote = (_state.value as? Editing)?.note ?: return
        val restoredNote = history.undo() ?: return

        val focusTarget = determineFocusTarget(currentNote, restoredNote)
        restoreNote(restoredNote)
        _events.trySend(EditNoteEvent.RequestFocus(focusTarget))
    }

    private fun redo() {
        commitPendingEdit()

        val currentNote = (_state.value as? Editing)?.note ?: return
        val restoredNote = history.redo() ?: return

        val focusTarget = determineFocusTarget(currentNote, restoredNote)
        restoreNote(restoredNote)
        _events.trySend(EditNoteEvent.RequestFocus(focusTarget))
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