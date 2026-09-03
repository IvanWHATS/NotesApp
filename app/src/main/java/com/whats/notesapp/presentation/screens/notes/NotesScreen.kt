@file:OptIn(ExperimentalMaterial3Api::class)

package com.whats.notesapp.presentation.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whats.notesapp.R
import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.domain.model.Note
import com.whats.notesapp.domain.model.NoteBackgroundColor
import com.whats.notesapp.presentation.components.NoteCard
import com.whats.notesapp.presentation.components.NoteCardWithImage
import com.whats.notesapp.presentation.components.SearchBar
import com.whats.notesapp.presentation.ui.theme.NotesAppTheme
import com.whats.notesapp.presentation.utils.toColor


@Composable
fun NotesScreen(
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel<NotesViewModel>(),
    onNoteClick: (noteId: Int) -> Unit,
    onAddNoteClick: () -> Unit
) {

    val state by viewModel.state.collectAsStateWithLifecycle()


    NotesScreenContent(
        modifier = modifier,
        state = state,
        onNoteAction = viewModel::onAction,
        onNoteClick = onNoteClick,
        onAddNoteClick = onAddNoteClick
    )
}


@Composable
fun NotesScreenContent(
    modifier: Modifier = Modifier,
    state: NotesScreenState,
    onNoteAction: (NotesScreenAction) -> Unit,
    onNoteClick: (noteId: Int) -> Unit,
    onAddNoteClick: () -> Unit
) {

    val isPinnedEmpty = state.pinnedNotes.isEmpty()
    val isOtherEmpty = state.otherNotes.isEmpty()
    val isSearchActive = state.query.isNotEmpty()
    val isFirstNote = !state.isLoading && isPinnedEmpty && isOtherEmpty && !isSearchActive

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }

    fun clearSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun enableSelectionMode(id: Int) {
        selectionMode = true
        selectedIds = setOf(id)
    }

    BackHandler(enabled = selectionMode) {
        clearSelection()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Subtitle(text = "Selected: ${selectedIds.size}") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,

                        ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                clearSelection()
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel Selection",
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                onNoteAction(NotesScreenAction.SwitchPinnedStatus(selectedIds))
                                clearSelection()
                            }
                        ) {
                            val isOtherSelected = state.otherNotes.any { it.id in selectedIds }
                            Icon(
                                painter = painterResource(if (isOtherSelected) R.drawable.ic_keep else R.drawable.ic_keep_off),
                                contentDescription = "Pin Selected Notes")
                        }
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                onNoteAction(NotesScreenAction.DeleteNotes(selectedIds))
                                clearSelection()
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Selected Notes")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Title(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxWidth(),
                            text = "All Notes"
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),

                    )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNoteClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_note),
                    contentDescription = "Button add new note",
                )
            }
        }
    ) { innerPadding ->
        if (isFirstNote) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(

                    onClick = onAddNoteClick
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_note),
                        contentDescription = "Button add new note",
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = "Add First Note",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding
            ) {

                item {
                    Spacer(Modifier.height(16.dp))
                    SearchBar(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        query = state.query,
                        onQueryChange = {
                            onNoteAction(NotesScreenAction.InputSearchQuery(it))
                        }
                    )
                    Spacer(Modifier.height(24.dp))
                }

                if (!isPinnedEmpty) {
                    item {
                        Subtitle(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            text = "Pinned"
                        )
                        Spacer(Modifier.height(16.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            items(
                                items = state.pinnedNotes,
                                key = { note -> note.id }
                            ) { note ->
                                NoteCard(
                                    modifier = Modifier.widthIn(max = 160.dp),
                                    note = note,
                                    backgroundColor = note.backgroundColor.toColor(),
                                    isSelected = note.id in selectedIds,
                                    selectionMode = selectionMode,
                                    onClick = {
                                        if (selectionMode) {
                                            selectedIds = (if (note.id in selectedIds)
                                                selectedIds - note.id
                                            else
                                                selectedIds + note.id)
                                        } else onNoteClick(note.id)
                                    },
                                    onLongClick = {
                                        enableSelectionMode(note.id)
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                if (!isOtherEmpty) {
                    if (!isPinnedEmpty) {
                        item {
                            Subtitle(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                text = "Others"
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    itemsIndexed(
                        items = state.otherNotes,
                        key = { _, note -> note.id }
                    ) { _, note ->
                        val imageUrl = note.content.filterIsInstance<ContentItem.Image>()
                            .firstOrNull()?.url
                        if (imageUrl == null)
                            NoteCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                note = note,
                                backgroundColor = note.backgroundColor.toColor(),
                                isSelected = note.id in selectedIds,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) {
                                        selectedIds = (if (note.id in selectedIds)
                                            selectedIds - note.id
                                        else
                                            selectedIds + note.id)
                                    } else onNoteClick(note.id)
                                },
                                onLongClick = {
                                    enableSelectionMode(note.id)
                                },
                            )
                        else
                            NoteCardWithImage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                note = note,
                                imageUrl = imageUrl,
                                backgroundColor = note.backgroundColor.toColor(),
                                isSelected = note.id in selectedIds,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) {
                                        selectedIds = (if (note.id in selectedIds)
                                            selectedIds - note.id
                                        else
                                            selectedIds + note.id)
                                    } else onNoteClick(note.id)
                                },
                                onLongClick = {
                                    enableSelectionMode(note.id)
                                },
                            )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NotesScreenPreview() {
    NotesAppTheme {
        NotesScreenContent(
            modifier = Modifier,
            state = NotesScreenState(
                query = "",
                isLoading = false,
                pinnedNotes = listOf(
                    Note(
                        id = 1,
                        title = "My First Note ",
                        content = listOf(ContentItem.Text(LoremIpsum(20).values.first())),
                        updatedAt = System.currentTimeMillis(),
                        backgroundColor = NoteBackgroundColor.Default,
                        isPinned = false
                    ),
                    Note(
                        id = 2,
                        title = "My Second Note ",
                        content = listOf(ContentItem.Text(LoremIpsum(20).values.first())),
                        updatedAt = System.currentTimeMillis(),
                        backgroundColor = NoteBackgroundColor.LIGHT_BLUE,
                        isPinned = false
                    )
                ),
                otherNotes = listOf(
                    Note(
                        id = 1,
                        title = "My Third Note ",
                        content = listOf(ContentItem.Text(LoremIpsum(20).values.first())),
                        updatedAt = System.currentTimeMillis(),
                        backgroundColor = NoteBackgroundColor.GREEN,
                        isPinned = false
                    ),
                    Note(
                        id = 2,
                        title = "My Fourth Note ",
                        content = listOf(ContentItem.Text(LoremIpsum(20).values.first())),
                        updatedAt = System.currentTimeMillis(),
                        backgroundColor = NoteBackgroundColor.ORANGE,
                        isPinned = false
                    )
                )
            ),
            onNoteAction = {

            },
            onNoteClick = {

            },
            onAddNoteClick = {

            }
        )
    }
}


@Composable
private fun Title(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier,
        text = text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun Subtitle(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier,
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

