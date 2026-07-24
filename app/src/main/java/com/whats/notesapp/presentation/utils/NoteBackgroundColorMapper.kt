package com.whats.notesapp.presentation.utils

import com.whats.notesapp.domain.model.NoteBackgroundColor
import com.whats.notesapp.domain.model.NoteBackgroundColor.*
import com.whats.notesapp.presentation.ui.theme.NoteColors

fun NoteBackgroundColor.toColor() = when(this) {
    BEIGE -> NoteColors.Beige
    ORANGE -> NoteColors.Orange
    GREEN -> NoteColors.Green
    LIGHT_BLUE -> NoteColors.LightBlue
    GREY_BLUE -> NoteColors.GreyBlue
    PURPLE -> NoteColors.Purple
}
