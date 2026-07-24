@file:OptIn(ExperimentalMaterial3Api::class)

package com.whats.notesapp.presentation.screens.editing

 import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
 import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.whats.notesapp.presentation.components.CompactSearchBar
import com.whats.notesapp.presentation.components.DropdownMenuButton
import com.whats.notesapp.presentation.components.ImageGroup
import com.whats.notesapp.presentation.components.SearchableTextContent
import com.whats.notesapp.presentation.mappers.toUiContent
import com.whats.notesapp.presentation.model.ContentItemUiModel
import com.whats.notesapp.presentation.ui.CustomIcons
import com.whats.notesapp.presentation.ui.theme.NotesAppTheme
import com.whats.notesapp.presentation.utils.DateFormatter
import com.whats.notesapp.presentation.utils.ObserveAsEvents
import com.whats.notesapp.presentation.utils.toColor

private const val HEADER_ITEMS_COUNT = 2


@Composable
fun EditNoteScreen(
    modifier: Modifier = Modifier,
    noteId: Int?,
    viewModel: EditNoteViewModel = hiltViewModel(
        creationCallback = { factory: EditNoteViewModel.Factory ->
            factory.create(noteId)
        }
    ),
    onFinished: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when(event) {
            EditNoteEvent.NavigateBack -> onFinished()
        }
    }

    EditNoteScreenContent(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun EditNoteScreenContent(
    modifier: Modifier = Modifier,
    state: EditNoteScreenState,
    onAction: (EditNoteScreenAction) -> Unit
) {
    when (state) {
        is EditNoteScreenState.Editing -> {

            BackHandler(enabled = state.searchState is SearchState.Active) {
                if (state.searchState is SearchState.Active) {
                    onAction(EditNoteScreenAction.CloseSearch)
                } else {
                    onAction(EditNoteScreenAction.NavigateBack)
                }
            }

            var showBottomSheet by remember { mutableStateOf(false) }

            Scaffold(
                modifier = modifier,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    when(state.searchState) {
                        is SearchState.Active ->
                            SearchTopAppBar(
                                searchState = state.searchState,
                                onAction = onAction
                            )
                        SearchState.Inactive ->
                            EditingTopAppBar(
                                state = state,
                                onOpenBackgroundPicker = { showBottomSheet = true },
                                onAction = onAction
                            )
                    }
                },
                bottomBar = {
                    when(state.searchState) {
                        is SearchState.Active ->
                            SearchBottomAppBar(
                                searchState = state.searchState,
                                onAction = onAction
                            )
                        SearchState.Inactive ->
                            EditingBottomAppBar(
                                state = state,
                                onAction = onAction
                            )

                    }

                }
            ) { innerPadding ->

                val listState = rememberLazyListState()

                val currentMatch = (state.searchState as? SearchState.Active)?.currentMatch

                LaunchedEffect(currentMatch, state.uiContent) {
                    if (currentMatch == null) return@LaunchedEffect

                    val uiContentIndex = state.uiContent.indexOfFirst { item ->
                        item is ContentItemUiModel.Text &&
                        item.index == currentMatch.contentItemIndex
                    }

                    if (uiContentIndex == -1) return@LaunchedEffect

                    listState.animateScrollToItem(
                        index = uiContentIndex + HEADER_ITEMS_COUNT,
                        scrollOffset = -100
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(state.note.backgroundColor.toColor())
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        bottom = 16.dp
                    )
                )
                {
                    item(key = "title"){
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth(),
                            value = state.note.title,
                            onValueChange = { onAction(EditNoteScreenAction.InputTitle(it)) },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                            textStyle = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            placeholder = {
                                Text(
                                    text = "Title",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            }
                        )
                    }

                    item(key = "meta"){
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = DateFormatter.formateDataToString(state.note.updatedAt),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.note.isPinned) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(R.drawable.ic_keep),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    contentDescription = "Is Note Pinned Icon"
                                )
                            }
                        }
                    }

                    items(
                        items = state.uiContent,
                        key = { uiModel -> uiModel.stableKey }
                    ) { contentItem ->
                        when (contentItem) {
                            is ContentItemUiModel.ImageGroup -> {
                                ImageGroup(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    imageUrls = contentItem.urls,
                                    onDeleteImageClick = { imageIndex ->
                                        onAction(EditNoteScreenAction.DeleteImage(contentItem.indexes[imageIndex]))
                                    }
                                )

                            }

                            is ContentItemUiModel.Text -> {
                                SearchableTextContent(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    text = contentItem.text,
                                    searchMatches = contentItem.searchMatches, // Будет пустым, если нет матчей
                                    activeMatchRange = contentItem.activeMatchRange,
                                    onTextChanged = { newText ->
                                        onAction(EditNoteScreenAction.InputContent(newText, contentItem.index))
                                    }
                                )
                            }
                        }
                    }
                }
                val sheetState = rememberModalBottomSheetState()

                if (showBottomSheet) {
                    ModalBottomSheet(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = Color.Transparent,
                        onDismissRequest = {
                            showBottomSheet = false
                        },
                        sheetState = sheetState
                    ) {
                        BottomSheetBackgroundColorPickerContent(
                            state,
                            onAction
                        )
                    }
                }

            }
        }

        EditNoteScreenState.Loading -> {

        }
    }
}

@Composable
private fun BottomSheetBackgroundColorPickerContent(
    state: EditNoteScreenState.Editing,
    onAction: (EditNoteScreenAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            text = "Background",

        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier,
            text = "Colors",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(
                items = NoteBackgroundColor.entries,
                key = { it }
            ) { backgroundColor ->
                val isSelected = backgroundColor == state.note.backgroundColor
                BackgroundColorPicker(
                    color = backgroundColor.toColor(),
                    isSelected = isSelected,
                    onSelect = {
                        onAction(EditNoteScreenAction.ChangeBackground(backgroundColor))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun BackgroundColorPicker(
    color: Color,
    isSelected: Boolean,
    onSelect: ()-> Unit,
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clickable {
                onSelect()
            }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Icon(
            modifier = Modifier.align(Alignment.Center).fillMaxSize().padding(12.dp),
            imageVector = Icons.AutoMirrored.Filled.Notes,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun BottomSheetBackgroundColorPickerPreview() {
    NotesAppTheme {
        BottomSheetBackgroundColorPickerContent(
            state = EditNoteScreenState.Editing(
                note = Note(
                    title = "",
                    content = listOf(),
                    updatedAt = System.currentTimeMillis(),
                    isPinned = false,
                    backgroundColor = NoteBackgroundColor.GREEN
                ),
                searchState = SearchState.Inactive,
                uiContent = listOf(),
            ),
            onAction = {  }
        )
    }

}

@Composable
private fun EditingTopAppBar(
    state: EditNoteScreenState.Editing,
    onOpenBackgroundPicker: () -> Unit,
    onAction: (EditNoteScreenAction) -> Unit
) {
    TopAppBar(
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .padding(start = 16.dp, end = 8.dp)
                    .clickable {
                        onAction(EditNoteScreenAction.NavigateBack)
                    },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        },
        actions = {
            DropdownMenuButton(
                modifier = Modifier.padding(horizontal = 8.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) { close ->
                DropdownMenuItem(
                    text = {
                        Text("Search")
                    },
                    onClick = {
                        onAction(EditNoteScreenAction.OpenSearch)
                        close()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("Background")
                    },
                    onClick = {
                        onOpenBackgroundPicker()
                        close()
                    }
                )
                if (!state.isNewNote) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (state.note.isPinned) "Unpin" else "Pin",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onAction(EditNoteScreenAction.SwitchPinStatus)
                            close()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                color = Color.Red
                            )
                        },
                        onClick = {
                            onAction(EditNoteScreenAction.DeleteNote)
                            close()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun SearchTopAppBar(
    searchState: SearchState.Active,
    onAction: (EditNoteScreenAction) -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        title = {
            CompactSearchBar(
                modifier = Modifier.padding(horizontal = 8.dp),
                query = searchState.query,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp), // Уменьшенный размер иконки
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onQueryChange = { query ->
                    onAction(EditNoteScreenAction.InputSearchQuery(query))
                }
            )
        },
        actions = {
            TextButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = {
                    onAction(EditNoteScreenAction.CloseSearch)
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditingBottomAppBar(
    modifier: Modifier = Modifier,
    state: EditNoteScreenState.Editing,
    onAction: (EditNoteScreenAction) -> Unit
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(), onResult = { uri ->
            uri?.let {
                onAction(EditNoteScreenAction.AddImage(it))
            }
        })

    BottomAppBar(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = state.isSaveEnabled,
                    onClick = {
                        onAction(EditNoteScreenAction.SaveChanges)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = "Save Note"
                    )
                }
                IconButton(
                    onClick = {
                        imagePicker.launch("image/*")
                    }
                ) {
                    Icon(
                        imageVector = CustomIcons.MaterialIconsAddPhotoAlternate,
                        contentDescription = "Add photo from gallery",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

            }
        }
    )
}


@Composable
private fun SearchBottomAppBar(
    modifier: Modifier = Modifier,
    searchState: SearchState.Active,
    onAction: (EditNoteScreenAction) -> Unit
) {
    BottomAppBar(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Absolute.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                enabled = searchState.canNavigate,
                onClick = {
                    onAction(EditNoteScreenAction.SelectPreviousMatch)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Save Note"
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                text = if (!searchState.hasMatches) "0/0" else "${searchState.activeMatchIndex!! + 1}/${searchState.matches.size}",
                textAlign = TextAlign.Center,
                color = if (!searchState.hasMatches) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )

            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                enabled = searchState.canNavigate,
                onClick = {
                    onAction(EditNoteScreenAction.SelectNextMatch)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Save Note"
                )
            }
        }
    }
}




@Preview
@Composable
private fun EditNoteScreenPreview(

) {
    NotesAppTheme {
        val previewContent = listOf(ContentItem.Text(LoremIpsum(20).values.first()))
        EditNoteScreenContent(
            modifier = Modifier,
            state = EditNoteScreenState.Editing(
                note = Note(
                    title = "",
                    content = previewContent,
                    updatedAt = System.currentTimeMillis(),
                    isPinned = false,
                    backgroundColor = NoteBackgroundColor.GREEN
                ),
                searchState = SearchState.Inactive,
                uiContent = previewContent.toUiContent(),
            ),
            onAction = {}
        )
    }
}

