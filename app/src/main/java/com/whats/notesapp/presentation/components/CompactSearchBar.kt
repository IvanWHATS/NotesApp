package com.whats.notesapp.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.whats.notesapp.presentation.ui.theme.NotesAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSearchBar(
    modifier: Modifier = Modifier,
    query: String,
    hint: String = "Search...",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onQueryChange: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // 1. Создаем объект для управления фокусом
    val focusRequester = remember { FocusRequester() }

    // 2. Запрашиваем фокус при создании (эффект сработает один раз)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = query,
        onValueChange = {
            onQueryChange(it)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .focusRequester(focusRequester), // 3. Привязываем FocusRequester к полю
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        )
    ) { innerTextField ->
        // Используем DecorationBox для кастомизации без лишних отступов Material 3
        TextFieldDefaults.DecorationBox(
            value = query,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            placeholder = {
                Text(text = hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp), // Меньший радиус для компактного вида
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent, // Убираем нижнюю линию
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            // Обнуляем стандартные конские отступы вокруг текста
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        )
    }
}

@Preview
@Composable
private fun CompactSearchBarPreview() {
    NotesAppTheme{
        CompactSearchBar(
            query = "",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp), // Уменьшенный размер иконки
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            onQueryChange = {}
        )
    }
}
