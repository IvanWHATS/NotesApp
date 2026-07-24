package com.whats.notesapp.domain.repository

import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.domain.model.Note
import com.whats.notesapp.domain.model.NoteBackgroundColor
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    suspend fun addNote(
        title: String,
        content: List<ContentItem>,
        backgroundColor: NoteBackgroundColor
    ): Int

    suspend fun deleteNote(noteId: Int)

    suspend fun editNote(
        noteId: Int,
        title: String,
        content: List<ContentItem>,
        backgroundColor: NoteBackgroundColor,
        isPinned: Boolean
    )

    fun getAllNotes(): Flow<List<Note>>

    suspend fun getNote(noteId: Int): Note

    fun searchNotes(query: String): Flow<List<Note>>

    suspend fun switchPinnedStatus(noteId: Int)
}