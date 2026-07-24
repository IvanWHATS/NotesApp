package com.whats.notesapp.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay


@Composable
fun ImageGroup(
    modifier: Modifier = Modifier,
    imageUrls: List<String>,
    onDeleteImageClick: (Int) -> Unit
) {
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageUrls.forEachIndexed { index, url ->
            ImageContent(
                modifier = Modifier.weight(1f),
                imageUrl = url,
                onDeleteImage = {
                    onDeleteImageClick(index)
                }
            )
        }
    }
}

@Composable
fun ImageContent(
    modifier: Modifier = Modifier,
    imageUrl: String,
    contentDescription: String? = null,
    onDeleteImage: () -> Unit
) {
    Box(
        modifier = modifier.heightIn(max = 500.dp)
        ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit,

        )
        Icon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .clickable {
                    onDeleteImage()
                },
            imageVector = Icons.Default.Close,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun SearchableTextContent(
    modifier: Modifier = Modifier,
    text: String,
    searchMatches: List<IntRange> = emptyList(), // Пусто, если поиска нет
    activeMatchRange: IntRange? = null,
    onTextChanged: (String) -> Unit
) {

    val activeColor = MaterialTheme.colorScheme.secondary
    val inactiveColor = activeColor.copy(alpha = 0.4f)
    // 1. Оптимизация: Если матчей нет, просто используем обычный текст.
    val annotatedText = remember(
        text,
        searchMatches,
        activeMatchRange
    ) {
        if (searchMatches.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)

                searchMatches.forEach { range ->
                    val color =
                        if (range == activeMatchRange) {
                            activeColor
                        } else {
                            inactiveColor
                        }

                    addStyle(
                        style = SpanStyle(background = color),
                        start = range.first,
                        end = range.last + 1
                    )
                }
            }
        }
    }

    var isFocused by remember { mutableStateOf(false) }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(300) // ждём появления клавиатуры
            bringIntoViewRequester.bringIntoView()
        }
    }

    var selection by remember {
        mutableStateOf(TextRange(text.length))
    }

    val textFieldValue = remember(
        annotatedText,
        selection,
        text
    ) {
        TextFieldValue(
            annotatedString = annotatedText,
            selection = TextRange(
                start = selection.start.coerceAtMost(text.length),
                end = selection.end.coerceAtMost(text.length)
            )
        )
    }

    // 4. Всегда рендерим один и тот же BasicTextField
    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            selection = newValue.selection
            isFocused = true
            onTextChanged(newValue.text)
        },
        modifier = modifier.fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged {
                isFocused = it.isFocused
            },
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = "Note something there",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                }
                innerTextField()
            }
        }
    )
}