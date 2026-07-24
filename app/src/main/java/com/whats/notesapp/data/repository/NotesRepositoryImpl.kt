package com.whats.notesapp.data.repository

import com.whats.notesapp.data.ImageFileManager
import com.whats.notesapp.data.db.dao.NotesDao
import com.whats.notesapp.data.db.model.NoteEntity
import com.whats.notesapp.data.mappers.toContentItemDomainList
import com.whats.notesapp.data.mappers.toDomain
import com.whats.notesapp.data.mappers.toDomainList
import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.domain.model.Note
import com.whats.notesapp.domain.model.NoteBackgroundColor
import com.whats.notesapp.domain.repository.NotesRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class NotesRepositoryImpl @Inject constructor(
    private val notesDao: NotesDao,
    private val imageFileManager: ImageFileManager
) : NotesRepository {

    override suspend fun addNote(
        title: String,
        content: List<ContentItem>,
        backgroundColor: NoteBackgroundColor
    ): Int {
        val processedContent = content.processForStorage()

        val noteEntity = NoteEntity(
            title = title,
            updatedAt = System.currentTimeMillis(),
            backgroundColor = backgroundColor,
            isPinned = false
        )

        return notesDao.upsertNoteWithContent(noteEntity, processedContent)
    }

    override suspend fun deleteNote(noteId: Int) {

        notesDao.getNoteById(noteId)
            .toDomain()
            .content
            .filterIsInstance<ContentItem.Image>()
            .forEach { imageFileManager.deleteImage(it.url) }

        notesDao.deleteNote(noteId)
    }

    override suspend fun editNote(
        noteId: Int,
        title: String,
        content: List<ContentItem>,
        backgroundColor: NoteBackgroundColor,
        isPinned: Boolean
    ) {
        val oldNote = notesDao.getNoteById(noteId).toDomain()
        val oldUrls = oldNote.content.filterIsInstance<ContentItem.Image>().map { it.url }
        val newUrls = content.filterIsInstance<ContentItem.Image>().map { it.url }

        (oldUrls - newUrls.toSet()).forEach {
            imageFileManager.deleteImage(it)
        }

        val processedContent = content.processForStorage()
        val noteEntity = NoteEntity(
            id = noteId,
            title = title,
            updatedAt = System.currentTimeMillis(),
            backgroundColor = backgroundColor,
            isPinned = isPinned
        )

        notesDao.upsertNoteWithContent(noteEntity, processedContent)
    }

    override fun getAllNotes(): Flow<List<Note>> {
        return notesDao.observeAllNotes().map { it.toDomainList() }
    }

    override suspend fun getNote(noteId: Int): Note {
        return notesDao.getNoteById(noteId).toDomain()
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return notesDao.observeNotesSearch(query).map { it.toDomainList() }
    }

    override suspend fun switchPinnedStatus(noteId: Int) {
        notesDao.updatePinnedStatus(noteId)
    }

    private suspend fun List<ContentItem>.processForStorage(): List<ContentItem> {
        return map { item ->
            when (item) {
                is ContentItem.Image -> {
                    if (imageFileManager.isInternal(item.url))
                        item
                    else {
                        val internalUrl = imageFileManager.copyImageToInternalStorage(item.url)
                        ContentItem.Image(url = internalUrl)
                    }
                }

                is ContentItem.Text -> item
            }
        }

    }

}