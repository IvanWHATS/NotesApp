package com.whats.notesapp.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.whats.notesapp.data.db.model.ContentItemEntity
import com.whats.notesapp.data.db.model.NoteEntity
import com.whats.notesapp.data.db.model.NoteWIthContent
import com.whats.notesapp.data.mappers.toEntityList
import com.whats.notesapp.domain.model.ContentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    @Transaction
    @Query("""
        SELECT * 
        FROM notes 
        ORDER BY updatedAt DESC
    """)
    fun observeAllNotes(): Flow<List<NoteWIthContent>>

    @Transaction
    @Query("""
        SELECT * 
        FROM notes
        WHERE id == :noteId
    """)
    suspend fun getNoteById(noteId: Int): NoteWIthContent

    @Transaction
    @Query("""
        SELECT DISTINCT notes.* 
        FROM notes JOIN notes_content 
            ON id == noteId
        WHERE title LIKE '%'||:query||'%' 
            OR content LIKE '%'||:query||'%' 
        ORDER BY updatedAt DESC
    """)
    fun observeNotesSearch(query: String): Flow<List<NoteWIthContent>>

    @Transaction
    @Query("""
        DELETE 
        FROM notes 
        WHERE id == :noteId
    """)
    suspend fun deleteNote(noteId: Int)

    @Query("""
        UPDATE notes
        SET isPinned = NOT isPinned
        WHERE id == :noteId
    """)
    suspend fun updatePinnedStatus(noteId: Int)

    @Upsert
    suspend fun upsertNote(note: NoteEntity): Long

    @Upsert
    suspend fun upsertNoteContent(content: List<ContentItemEntity>)

    @Query("""
        DELETE 
        FROM notes_content
        WHERE noteId == :noteId
    """)
    suspend fun deleteNoteContent(noteId: Int)

    @Transaction
    suspend fun upsertNoteWithContent(
        noteEntity: NoteEntity,
        content: List<ContentItem>
    ): Int {
        val upsertResult = upsertNote(noteEntity)

        val noteId = if (noteEntity.id == 0) upsertResult.toInt() else noteEntity.id

        val contentItems = content.toEntityList(noteId)

        upsertNoteContent(contentItems)

        return noteId
    }

}