package dev.supersudoku.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun emptyClassic(): GridDef =
    GridDef("t", "T", Variant.CLASSIC, 0, 0, emptyMap())

class SolveTest {
    private val solved = intArrayOf(
        5, 3, 4, 6, 7, 8, 9, 1, 2,
        6, 7, 2, 1, 9, 5, 3, 4, 8,
        1, 9, 8, 3, 4, 2, 5, 6, 7,
        8, 5, 9, 7, 6, 1, 4, 2, 3,
        4, 2, 6, 8, 5, 3, 7, 9, 1,
        7, 1, 3, 9, 2, 4, 8, 5, 6,
        9, 6, 1, 5, 3, 7, 2, 8, 4,
        2, 8, 7, 4, 1, 9, 6, 3, 5,
        3, 4, 5, 2, 8, 6, 1, 7, 9,
    )

    @Test
    fun emptyGridSearchFindsValidSolution() {
        // regression: branching must try every digit (bit iteration off-by-one
        // used to skip digit 1 and shift the rest, failing the whole tree)
        val g = emptyClassic()
        val solver = SuperSolver(listOf(g))
        solver.search(solver.initialDomains(), 1)
        assertEquals(1, solver.solutions.size)
        val sol = solver.solutionMap(solver.solutions[0])
        assertEquals(81, sol.size)
        val get: (Pos) -> Int = { sol.getValue(it) }
        assertTrue(validateGrid(g, get).isEmpty())
        assertTrue(isGridComplete(g, get))
    }

    @Test
    fun singleMissingCell() {
        val solver = SuperSolver(listOf(emptyClassic()))
        val dom = solver.initialDomains()
        for (i in 0 until 81) {
            if (i == 40) continue
            solver.setGiven(dom, Pos(i % 9, i / 9), solved[i])
        }
        solver.search(dom, 2)
        assertEquals(1, solver.solutions.size)
        assertEquals(1 shl solved[40], solver.solutions[0][40])
    }

    @Test
    fun inequalityContradiction() {
        val g = emptyClassic().copy(inequalities = listOf(Pos(0, 0) to Pos(1, 0)))
        val solver = SuperSolver(listOf(g))
        val dom = solver.initialDomains()
        solver.setGiven(dom, Pos(0, 0), 9)
        assertFalse(solver.propagate(dom))
    }

    @Test
    fun killerCagePrunesToCombo() {
        // 2-cell cage totalling 3 on an otherwise empty grid:
        // only {1,2} survive in both cells
        val g = emptyClassic().copy(
            cages = listOf(Cage(setOf(Pos(0, 0), Pos(1, 0)), 3)),
        )
        val solver = SuperSolver(listOf(g))
        val dom = solver.initialDomains()
        assertTrue(solver.propagate(dom))
        val expect = (1 shl 1) or (1 shl 2)
        assertEquals(expect, dom[0])
        assertEquals(expect, dom[1])
    }

    @Test
    fun equationPropagateSmoke() {
        // H1-style equation on an empty grid: propagation must stay consistent
        // (full equation solving is exercised by the build-time SolveMain run)
        val eq = Equation(
            listOf(listOf(Pos(0, 0), Pos(1, 0)), listOf(Pos(0, 1), Pos(1, 1))),
            listOf(Pos(0, 2), Pos(1, 2)),
        )
        val g = emptyClassic().copy(equations = listOf(eq))
        val solver = SuperSolver(listOf(g))
        val dom = solver.initialDomains()
        assertTrue(solver.propagate(dom))
        for (i in 0 until 81) {
            assertTrue(solver.count(dom, i) > 0)
        }
    }
}
