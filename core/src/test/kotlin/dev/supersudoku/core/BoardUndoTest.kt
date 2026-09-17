package dev.supersudoku.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardUndoTest {
    @Test
    fun undoRedoRoundTrip() {
        val b = PlayBoard(9, 9)
        b.setGiven(0, 0, 5)
        val undo = UndoStack(b)
        assertFalse(undo.canUndo())

        undo.checkpoint()
        b.enterDigit(1, 0, 3)
        assertEquals(3, b.get(1, 0))
        assertTrue(undo.canUndo())

        undo.checkpoint()
        b.toggleNote(2, 0, 7)
        assertTrue(b.hasNote(2, 0, 7))

        assertTrue(undo.undo())
        assertEquals(3, b.get(1, 0))
        assertFalse(b.hasNote(2, 0, 7))

        assertTrue(undo.undo())
        assertEquals(0, b.get(1, 0))
        assertFalse(undo.canUndo())

        assertTrue(undo.redo())
        assertEquals(3, b.get(1, 0))
        assertTrue(undo.redo())
        assertTrue(b.hasNote(2, 0, 7))
        assertFalse(undo.canRedo())
    }

    @Test
    fun enterDigitClearsOwnNoteAndEraseClearsAll() {
        val b = PlayBoard(9, 9)
        b.toggleNote(4, 4, 2)
        b.toggleNote(4, 4, 5)
        b.enterDigit(4, 4, 2)
        assertFalse(b.hasNote(4, 4, 2))
        assertTrue(b.hasNote(4, 4, 5))
        b.enterDigit(4, 4, 0)
        assertEquals(0, b.noteMask(4, 4))
    }

    @Test
    fun givensAreImmutable() {
        val b = PlayBoard(9, 9)
        b.setGiven(0, 0, 5)
        assertFalse(b.enterDigit(0, 0, 1))
        assertFalse(b.toggleNote(0, 0, 1))
        assertEquals(5, b.get(0, 0))
    }
}
