package com.whats.notesapp.presentation.screens.editing

import com.whats.notesapp.presentation.model.TextMatch

sealed interface SearchState {

    data object Inactive : SearchState

    data class Active(
        val query: String = "",
        val matches: List<TextMatch> = emptyList(),
        val activeMatchIndex: Int? = null // Индекс текущего выбранного совпадения в списке matches
    ) : SearchState {
        val hasMatches: Boolean get() = matches.isNotEmpty()
        val canNavigate: Boolean get() = matches.size > 1
        val currentMatch: TextMatch? get() = activeMatchIndex?.let { matches.getOrNull(it) }
    }
}