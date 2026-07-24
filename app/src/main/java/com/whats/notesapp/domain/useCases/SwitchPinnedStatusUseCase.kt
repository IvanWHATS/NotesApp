package com.whats.notesapp.domain.useCases

import com.whats.notesapp.domain.repository.NotesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwitchPinnedStatusUseCase @Inject constructor(
    private val repository: NotesRepository
) {
    suspend operator fun invoke(noteId: Int) {
        repository.switchPinnedStatus(noteId)
    }
}