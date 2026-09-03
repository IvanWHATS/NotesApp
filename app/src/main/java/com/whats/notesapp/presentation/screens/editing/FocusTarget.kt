package com.whats.notesapp.presentation.screens.editing

sealed interface FocusTarget {
    data class Title(val cursorPosition: Int = 0) : FocusTarget
    data class TextBlock(
        val id: String,
        val cursorPosition: Int? = null
    ) : FocusTarget
    data class ImageBlock(val id: String) : FocusTarget
}