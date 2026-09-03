package com.whats.notesapp.presentation.screens.editing

class History<T>(
    private val maxSize: Int = 50
) {

    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    val canUndo: Boolean get() = undoStack.size > 1
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    val current: T? get() = undoStack.lastOrNull()

    val size: Int get() = undoStack.size + redoStack.size

    fun push(state: T) {
        undoStack.addLast(state)
        redoStack.clear()
        trimToMaxSize()
    }

    fun undo(): T? {
        if (!canUndo) return null
        redoStack.addLast(undoStack.removeLast())
        return undoStack.last()
    }

    fun redo(): T? {
        if (!canRedo) return null
        val state = redoStack.removeLast()
        undoStack.addLast(state)
        return state
    }

    fun clear() {
        val current = undoStack.lastOrNull()
        undoStack.clear()
        redoStack.clear()
        if (current != null) undoStack.addLast(current)
    }

    private fun trimToMaxSize() {
        while (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
    }
}