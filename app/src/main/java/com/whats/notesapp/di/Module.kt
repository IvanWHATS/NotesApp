package com.whats.notesapp.di

import android.content.Context
import androidx.room.Room
import com.whats.notesapp.data.db.dao.NotesDao
import com.whats.notesapp.data.db.NotesDatabase
import com.whats.notesapp.data.repository.NotesRepositoryImpl
import com.whats.notesapp.domain.repository.NotesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface Module {

    @Binds
    fun bindNotesRepository(
        notesRepositoryImpl: NotesRepositoryImpl
    ): NotesRepository


    companion object {
        @Provides
        @Singleton
        fun provideNotesDatabase(@ApplicationContext context: Context): NotesDatabase {
            return Room.databaseBuilder(
                context,
                NotesDatabase::class.java,
                "notes.db"
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
        }

        @Provides
        fun provideNotesDao(database: NotesDatabase): NotesDao {
            return database.notesDao() // Описываем для Hilt, как достать Dao
        }

    }
}