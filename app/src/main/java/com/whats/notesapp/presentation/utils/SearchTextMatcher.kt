package com.whats.notesapp.presentation.utils

import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.presentation.model.TextMatch

object SearchTextMatcher {

    fun getTextMatches(
        noteContent: List<ContentItem>,
        query: String
    ): List<TextMatch> {
        if (query.isEmpty()) return emptyList()

        return noteContent.flatMapIndexed { index, item ->
            if (item is ContentItem.Text) {
                findCharRanges(item.text, query).map { range ->
                    TextMatch(
                        contentItemIndex = index, // индекс исходного списка
                        charRange = range
                    )
                }
            } else {
                emptyList()
            }
        }
    }

    private fun findCharRanges(text: String, query: String): List<IntRange> {
        if (query.isEmpty()) return emptyList()

        val ranges = mutableListOf<IntRange>()
        var index = 0

        while (true) {
            index = text.indexOf(query, index, ignoreCase = true)
            if (index == -1) break

            ranges.add(index until index + query.length)
            index += query.length
        }

        return ranges
    }
}