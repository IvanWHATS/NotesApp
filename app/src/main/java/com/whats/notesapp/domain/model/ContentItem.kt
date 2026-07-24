package com.whats.notesapp.domain.model

sealed interface ContentItem {

    data class Text(val text: String) : ContentItem

    data class Image(val url: String) : ContentItem
}