package com.whats.notesapp.presentation.mappers

import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.presentation.model.ContentItemUiModel
import java.util.UUID


fun List<ContentItem>.toUiContent(): List<ContentItemUiModel> {
    if (isEmpty()) return emptyList()

    val result = mutableListOf<ContentItemUiModel>()

    val currentImageGroupUrls = mutableListOf<String>()
    val currentImageGroupIndexes = mutableListOf<Int>()

    fun flushImageGroup() {
        if (currentImageGroupIndexes.isNotEmpty()) {
            result.add(
                ContentItemUiModel.ImageGroup(
                    urls = currentImageGroupUrls.toList(),
                    indexes = currentImageGroupIndexes.toList(),
                    stableKey = UUID.randomUUID().toString()
                )
            )

            currentImageGroupUrls.clear()
            currentImageGroupIndexes.clear()
        }
    }

    forEachIndexed { index, contentItem ->
        when (contentItem) {
            is ContentItem.Image -> {
                currentImageGroupUrls.add(contentItem.url)
                currentImageGroupIndexes.add(index)
            }

            is ContentItem.Text -> {
                flushImageGroup()
                result.add(
                    ContentItemUiModel.Text(
                        text = contentItem.text,
                        index = index,
                        stableKey = UUID.randomUUID().toString(),
                    )
                )
            }
        }
    }
    flushImageGroup()

    return result
}

fun List<ContentItemUiModel>.toDomainList(): List<ContentItem> = flatMap { itemUiModel ->
    when(itemUiModel) {
        is ContentItemUiModel.ImageGroup -> itemUiModel.urls.map { ContentItem.Image(it) }
        is ContentItemUiModel.Text -> listOf(ContentItem.Text(itemUiModel.text))
    }
}



