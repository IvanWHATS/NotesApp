package com.whats.notesapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.whats.notesapp.R
import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.domain.model.Note
import com.whats.notesapp.domain.model.NoteBackgroundColor
import com.whats.notesapp.presentation.ui.theme.NotesAppTheme
import com.whats.notesapp.presentation.ui.theme.OtherNotesColors
import com.whats.notesapp.presentation.ui.theme.PinnedNotesColors
import com.whats.notesapp.presentation.utils.DateFormatter

@Composable
fun NoteCard(
    modifier: Modifier = Modifier,
    note: Note,
    isSelected: Boolean,
    selectionMode: Boolean,
    backgroundColor: Color,
    onClick: (noteId: Int) -> Unit,
    onLongClick: (Note) -> Unit
) {

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 4.dp else (-1).dp,
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = { onClick(note.id) },
                onLongClick = { onLongClick(note) },
            )

    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ){
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = note.title,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = DateFormatter.formateDataToString(note.updatedAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selectionMode)
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick(note.id) },
                        modifier = Modifier.size(28.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
            }

            note.content
                .filterIsInstance<ContentItem.Text>()
                .filter { it.text.isNotBlank() }
                .joinToString("\n") { it.text }
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = it,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
        }

    }
}


@Composable
fun NoteCardWithImage(
    modifier: Modifier = Modifier,
    note: Note,
    imageUrl: String,
    isSelected: Boolean,
    selectionMode: Boolean,
    backgroundColor: Color,
    onClick: (noteId: Int) -> Unit,
    onLongClick: (Note) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 4.dp else (-1).dp,
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(16.dp)
            )
            .background(backgroundColor)
            .combinedClickable(
                onClick = { onClick(note.id) },
                onLongClick = { onLongClick(note) },
            )

    ) {
        Column {
            Box {
                AsyncImage(
                    modifier = Modifier
                        .heightIn(max = 120.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                    model = imageUrl,
                    contentDescription = "Note Image Preview",
                    contentScale = ContentScale.FillWidth
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.4f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .align(Alignment.BottomStart),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = note.title,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = DateFormatter.formateDataToString(note.updatedAt),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    if (selectionMode)
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick(note.id) },
                            modifier = Modifier.size(28.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.onPrimary,
                                uncheckedColor = MaterialTheme.colorScheme.onPrimary,
                                checkmarkColor = MaterialTheme.colorScheme.primary
                            )
                        )
                }
            }

            note.content
                .filterIsInstance<ContentItem.Text>()
                .filter { it.text.isNotBlank() }
                .joinToString("\n") { it.text }
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        text = it,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
        }
    }
}


@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun NoteCardPreview() {


    val previewWhiteImageUrl = "android.resource://com.whats.notesapp/" + R.drawable.preview_white_image

    val previewImageUrl = "android.resource://com.whats.notesapp/" + R.drawable.preview_image

    val textContent = listOf(
        ContentItem.Text(LoremIpsum(20).values.first()),
    )

    val imageContent = listOf(
        ContentItem.Image(url = previewImageUrl)
    )
    val fullContent = listOf(
        ContentItem.Text(LoremIpsum(20).values.first()),
        ContentItem.Text(""),ContentItem.Text(""),ContentItem.Text(""),
        ContentItem.Image(url = previewWhiteImageUrl),

    )

    val emptyContent = listOf(
        ContentItem.Text(""),
        ContentItem.Text(""),
        ContentItem.Text(""),
        ContentItem.Text(""),
    )


    val previewNote = Note(
        title = "Preview Note",
        content = textContent,
        updatedAt = System.currentTimeMillis(),
        backgroundColor = NoteBackgroundColor.Default,
        isPinned = false
    )

    NotesAppTheme {
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteCard(
                    modifier = Modifier
                        .widthIn(max = 160.dp)
                        .fillMaxWidth(),
                    note = previewNote.copy(title = "Pinned Note"),
                    backgroundColor = PinnedNotesColors[1],
                    onClick = { },
                    onLongClick = {},
                    isSelected = false,
                    selectionMode = false,
                )

                NoteCard(
                    modifier = Modifier
                        .widthIn(max = 160.dp)
                        .fillMaxWidth(),
                    note = previewNote.copy(title = "Pinned Empty Note", content = emptyContent),
                    backgroundColor = PinnedNotesColors[0],
                    onClick = { },
                    onLongClick = {},
                    isSelected = false,
                    selectionMode = false,
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            NoteCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                note = previewNote,
                backgroundColor = OtherNotesColors[0],
                onClick = { },
                onLongClick = {},
                isSelected = true,
                selectionMode = true,
            )
            Spacer(modifier = Modifier.size(16.dp))
            NoteCardWithImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                note = previewNote.copy(title = "Note With Image", content = fullContent),
                imageUrl = previewImageUrl,
                backgroundColor = OtherNotesColors[1],
                onClick = { },
                onLongClick = {},
                isSelected = true,
                selectionMode = true,
            )
            Spacer(modifier = Modifier.size(16.dp))
            NoteCardWithImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                note = previewNote.copy(title = "Note With Only Image", content = imageContent),
                imageUrl = previewImageUrl,
                backgroundColor = OtherNotesColors[2],
                onClick = { },
                onLongClick = {},
                isSelected = false,
                selectionMode = false,
            )

        }

    }
}