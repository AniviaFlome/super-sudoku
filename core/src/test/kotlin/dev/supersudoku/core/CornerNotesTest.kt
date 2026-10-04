package dev.supersudoku.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CornerNotesTest {
    @Test
    fun cornerMarksToggleIndependentlyOfCenter() {
        val b = PlayBoard(9, 9)
        assertTrue(b.toggleCorner(0, 0, 5))
        assertTrue(b.hasCorner(0, 0, 5))
        assertFalse(b.hasNote(0, 0, 5))
        assertTrue(b.toggleNote(0, 0, 5))
        assertTrue(b.hasNote(0, 0, 5))
        assertTrue(b.hasCorner(0, 0, 5))
        // Entering the digit hides both flavors behind the value but keeps them.
        assertTrue(b.enterDigit(0, 0, 5))
        assertTrue(b.hasNote(0, 0, 5))
        assertTrue(b.hasCorner(0, 0, 5))
        // Erasing preserves both flavors so the marks reappear.
        b.toggleCorner(1, 1, 3)
        b.toggleNote(1, 1, 4)
        assertTrue(b.enterDigit(1, 1, 0))
        assertTrue(b.hasCorner(1, 1, 3))
        assertTrue(b.hasNote(1, 1, 4))
    }

    @Test
    fun enterOtherDigitPreservesMarksForErase() {
        val b = PlayBoard(9, 9)
        b.toggleCorner(0, 0, 1)
        b.toggleCorner(0, 0, 2)
        b.toggleNote(0, 0, 3)
        // Entering a different digit hides the marks behind the value but
        // keeps them, so erasing brings them back.
        assertTrue(b.enterDigit(0, 0, 5))
        assertTrue(b.hasCorner(0, 0, 1))
        assertTrue(b.hasCorner(0, 0, 2))
        assertTrue(b.hasNote(0, 0, 3))
        assertTrue(b.enterDigit(0, 0, 0))
        assertTrue(b.hasCorner(0, 0, 1))
        assertTrue(b.hasCorner(0, 0, 2))
        assertTrue(b.hasNote(0, 0, 3))
        // Entering a marked digit also keeps that digit's marks.
        assertTrue(b.enterDigit(0, 0, 1))
        assertTrue(b.hasCorner(0, 0, 1))
        assertTrue(b.hasCorner(0, 0, 2))
        assertTrue(b.hasNote(0, 0, 3))
    }

    @Test
    fun snapshotsCarryCornerMarks() {
        val b = PlayBoard(9, 9)
        b.toggleCorner(2, 2, 7)
        b.toggleNote(2, 2, 1)
        val s = b.snapshot()
        b.toggleCorner(2, 2, 7)
        b.toggleNote(2, 2, 1)
        b.restore(s)
        assertTrue(b.hasCorner(2, 2, 7))
        assertTrue(b.hasNote(2, 2, 1))
    }
}
