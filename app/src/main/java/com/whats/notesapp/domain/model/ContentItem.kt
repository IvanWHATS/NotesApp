package com.whats.notesapp.domain.model

import java.util.UUID

sealed interface ContentItem {

    val id: String

    data class Text(
        val text: String,
        override val id: String = UUID.randomUUID().toString()
    ) : ContentItem

    data class Image(
        val url: String,
        override val id: String = UUID.randomUUID().toString()
    ) : ContentItem
}