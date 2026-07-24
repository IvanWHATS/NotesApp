package com.whats.notesapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.whats.notesapp.data.db.dao.NotesDao
import com.whats.notesapp.data.db.model.ContentItemEntity
import com.whats.notesapp.data.db.model.NoteEntity

@Database(
    entities = [NoteEntity::class, ContentItemEntity::class],
    version = 6,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}