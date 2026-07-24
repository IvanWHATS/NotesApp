package com.whats.notesapp.presentation.model

sealed interface ContentItemUiModel {
    val stableKey: String

    data class ImageGroup(
        val urls: List<String>,
        val indexes: List<Int>,
        override val stableKey: String
    ) : ContentItemUiModel

    data class Text(
        val text: String,
        val index: Int,
        val searchMatches: List<IntRange> = emptyList(),
        val activeMatchRange: IntRange? = null,
        override val stableKey: String
    ) : ContentItemUiModel
}