package dev.supersudoku.core

import kotlin.test.Test
import kotlin.test.assertEquals
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
        // Entering the digit clears both flavors.
        assertTrue(b.enterDigit(0, 0, 5))
        assertFalse(b.hasNote(0, 0, 5))
        assertFalse(b.hasCorner(0, 0, 5))
        // Erasing clears both flavors.
        b.toggleCorner(1, 1, 3)
        b.toggleNote(1, 1, 4)
        assertTrue(b.enterDigit(1, 1, 0))
        assertEquals(0, b.cornerMask(1, 1))
        assertEquals(0, b.noteMask(1, 1))
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
