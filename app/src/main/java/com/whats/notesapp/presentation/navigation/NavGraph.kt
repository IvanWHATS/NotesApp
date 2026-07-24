package com.whats.notesapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.whats.notesapp.presentation.screens.editing.EditNoteScreen
import com.whats.notesapp.presentation.screens.notes.NotesScreen
import kotlinx.serialization.Serializable

@Composable
fun NavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Notes
    ) {
        composable<Screen.Notes> {
            NotesScreen(
                onNoteClick = { noteId ->
                    navController.navigate(route = Screen.EditNote(noteId))
                },
                onAddNoteClick = {
                    navController.navigate(Screen.CreateNote)
                }
            )
        }
        composable<Screen.CreateNote> {
            EditNoteScreen(
                noteId = null,
                onFinished = {
                    navController.popBackStack()
                },
            )
        }
        composable<Screen.EditNote> { backStackEntry ->
            val editNote: Screen.EditNote = backStackEntry.toRoute()
            EditNoteScreen(
                noteId = editNote.noteId,
                onFinished = {
                    navController.popBackStack()
                },
            )
        }
    }
}


sealed interface Screen {

    @Serializable
    data object Notes : Screen

    @Serializable
    data object CreateNote : Screen

    @Serializable
    data class EditNote(val noteId: Int) : Screen
}