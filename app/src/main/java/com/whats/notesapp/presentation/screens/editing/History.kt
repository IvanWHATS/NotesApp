package com.whats.notesapp.presentation.screens.editing

class History<T>(
    private val maxSize: Int = 50
) {

    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun push(state: T) {
        undoStack.addLast(state)

        if (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }

        redoStack.clear()
    }

    fun undo(current: T): T? {
        val previous = undoStack.removeLastOrNull() ?: return null

        redoStack.addLast(current)

        return previous
    }

    fun redo(current: T): T? {
        val next = redoStack.removeLastOrNull() ?: return null

        undoStack.addLast(current)

        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}