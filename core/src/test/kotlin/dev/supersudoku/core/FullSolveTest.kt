package dev.supersudoku.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end regression: the bundled puzzle decodes and solves to exactly
 * one solution (~50ms). Guards the transcription, every rule implementation
 * and the solver itself.
 */
class FullSolveTest {
    private fun asset(name: String): String {
        val f = File("../app/src/main/assets/$name")
        assertTrue(f.exists(), "missing ${f.absolutePath}")
        return f.readText()
    }

    @Test
    fun superPuzzleHasUniqueSolution() {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solver = SuperSolver(puzzle.grids)
        val dom = solver.initialDomains()
        for (g in puzzle.grids) for ((pos, v) in g.givens) {
            solver.setGiven(dom, pos, v)
        }
        solver.search(dom, 2)
        assertEquals(1, solver.solutions.size, "expected exactly one solution")
    }

    @Test
    fun committedSolutionMatchesSolver() {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val expected = PuzzleLoader.loadSolution(asset("solution.json"))
        assertEquals(576, expected.size)
        val solver = SuperSolver(puzzle.grids)
        val dom = solver.initialDomains()
        for (g in puzzle.grids) for ((pos, v) in g.givens) {
            solver.setGiven(dom, pos, v)
        }
        solver.search(dom, 1)
        assertEquals(1, solver.solutions.size)
        assertEquals(expected, solver.solutionMap(solver.solutions[0]))
    }
}
