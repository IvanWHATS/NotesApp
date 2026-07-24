package com.whats.notesapp.data.mappers

import com.whats.notesapp.data.db.model.ContentItemEntity
import com.whats.notesapp.data.db.model.ContentType
import com.whats.notesapp.data.db.model.NoteEntity
import com.whats.notesapp.data.db.model.NoteWIthContent
import com.whats.notesapp.domain.model.ContentItem
import com.whats.notesapp.domain.model.Note

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id,
        title,
        updatedAt,
        backgroundColor = backgroundColor,
        isPinned = isPinned,
    )
}

fun NoteWIthContent.toDomain(): Note {
    return Note(
        id = note.id,
        title = note.title,
        content = content.toContentItemDomainList(),
        updatedAt = note.updatedAt,
        backgroundColor = note.backgroundColor,
        isPinned = note.isPinned
    )
}

fun List<NoteWIthContent>.toDomainList(): List<Note> = map { it.toDomain() }


fun List<ContentItem>.toEntityList(noteId: Int): List<ContentItemEntity> {
    return mapIndexed { index, contentItem ->
        when(contentItem) {
            is ContentItem.Image -> {
                ContentItemEntity(
                    noteId = noteId,
                    order = index,
                    contentType = ContentType.IMAGE,
                    content = contentItem.url,
                )
            }
            is ContentItem.Text -> {
                ContentItemEntity(
                    noteId = noteId,
                    order = index,
                    contentType = ContentType.TEXT,
                    content = contentItem.text,
                )
            }
        }
    }
}

fun List<ContentItemEntity>.toContentItemDomainList(): List<ContentItem> {
    return map { contentItemEntity ->
        when(contentItemEntity.contentType) {
            ContentType.TEXT -> ContentItem.Text(contentItemEntity.content)
            ContentType.IMAGE -> ContentItem.Image(contentItemEntity.content)
        }
    }
}