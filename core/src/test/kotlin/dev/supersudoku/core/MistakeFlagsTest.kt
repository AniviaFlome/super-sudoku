package dev.supersudoku.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mistake-mode contract: CONFLICTS shows exactly [validateGrid] conflicts,
 * WRONG shows exactly [mismatchedCells]. A wrong-but-legal entry must be
 * invisible under CONFLICTS and flagged under WRONG.
 */
class MistakeFlagsTest {
    @Test
    fun mismatchedCellsSkipsGivensEmptiesAndCorrectEntries() {
        val value = IntArray(81)
        val given = IntArray(81)
        val solution = IntArray(81) { (it % 9) + 1 }
        // correct user entry
        value[0] = solution[0]
        // wrong user entry
        value[1] = solution[1] % 9 + 1
        // given holding a (hypothetically) wrong digit is never flagged
        given[2] = 1
        value[2] = solution[2] % 9 + 1
        // empty cell never flagged
        value[3] = 0
        val flags = mismatchedCells(
            9, 9,
            get = { x, y -> value[y * 9 + x] },
            isGiven = { x, y -> given[y * 9 + x] != 0 },
            solutionAt = { x, y -> solution[y * 9 + x] },
        )
        assertEquals(setOf(Pos(1, 0)), flags)
    }

    @Test
    fun unknownSolutionHonorsFlag() {
        val value = IntArray(81).also { it[0] = 7 }
        val given = IntArray(81)
        val get = { x: Int, y: Int -> value[y * 9 + x] }
        val noGiven = { _: Int, _: Int -> false }
        val unknown = { _: Int, _: Int -> null }
        assertEquals(
            setOf(Pos(0, 0)),
            mismatchedCells(9, 9, get, noGiven, unknown, flagUnknownSolution = true),
        )
        assertTrue(
            mismatchedCells(9, 9, get, noGiven, unknown, flagUnknownSolution = false).isEmpty(),
        )
    }

    @Test
    fun conflictFreeWrongEntryHiddenFromConflictsShownToWrong() {
        val grid = GridDef(
            id = "t", name = "T", variant = Variant.CLASSIC, x = 0, y = 0,
            givens = emptyMap(),
        )
        // Single wrong digit, no duplicate in any unit: no conflict.
        val get: (Pos) -> Int = { if (it == Pos(0, 0)) 5 else 0 }
        assertTrue(validateGrid(grid, get).isEmpty())
        // ...but WRONG mode flags it against the solution.
        val flags = mismatchedCells(
            9, 9,
            get = { x, y -> get(Pos(x, y)) },
            isGiven = { _, _ -> false },
            solutionAt = { x, y -> if (x == 0 && y == 0) 3 else null },
        )
        assertEquals(setOf(Pos(0, 0)), flags)
    }

    @Test
    fun duplicateDigitsFlaggedByConflicts() {
        val grid = GridDef(
            id = "t", name = "T", variant = Variant.CLASSIC, x = 0, y = 0,
            givens = emptyMap(),
        )
        val get: (Pos) -> Int = {
            when (it) {
                Pos(0, 0), Pos(1, 0) -> 5
                else -> 0
            }
        }
        val cells = validateGrid(grid, get).flatMap { it.cells }.toSet()
        assertEquals(setOf(Pos(0, 0), Pos(1, 0)), cells)
    }
}
