package dev.supersudoku.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateTest {
    @Test
    fun solverCountsUniqueness() {
        // classic starter with unique solution
        val puzzle = intArrayOf(
            5, 3, 0, 0, 7, 0, 0, 0, 0,
            6, 0, 0, 1, 9, 5, 0, 0, 0,
            0, 9, 8, 0, 0, 0, 0, 6, 0,
            8, 0, 0, 0, 6, 0, 0, 0, 3,
            4, 0, 0, 8, 0, 3, 0, 0, 1,
            7, 0, 0, 0, 2, 0, 0, 0, 6,
            0, 6, 0, 0, 0, 0, 2, 8, 0,
            0, 0, 0, 4, 1, 9, 0, 0, 5,
            0, 0, 0, 0, 8, 0, 0, 7, 9,
        )
        assertEquals(1, ClassicSolver.countSolutions(puzzle.copyOf(), 2))
        // empty cell ambiguity check: blank grid has many solutions
        assertEquals(2, ClassicSolver.countSolutions(IntArray(81), 2))
    }

    @Test
    fun generateEasyUnique() {
        val puzzle = ClassicGenerator.generate(Difficulty.EASY, Random(42))
        val givens = puzzle.count { it != 0 }
        assertTrue(givens >= Difficulty.EASY.givens, "givens=$givens")
        assertEquals(1, ClassicSolver.countSolutions(puzzle.copyOf(), 2))
        // solution validates clean as classic
        val sol = puzzle.copyOf()
        assertTrue(ClassicSolver.solveInto(sol, Random(7)))
        val g = GridDef("p", "P", Variant.CLASSIC, 0, 0, emptyMap())
        val get: (Pos) -> Int = { sol[it.y * 9 + it.x] }
        assertTrue(validateGrid(g, get).isEmpty())
        assertTrue(isGridComplete(g, get))
    }

    @Test
    fun generateMediumUnique() {
        val puzzle = ClassicGenerator.generate(Difficulty.MEDIUM, Random(1234))
        assertTrue(puzzle.count { it != 0 } >= Difficulty.MEDIUM.givens)
        assertEquals(1, ClassicSolver.countSolutions(puzzle.copyOf(), 2))
    }

    @Test
    fun timeBoxStillUnique() {
        val start = System.currentTimeMillis()
        val puzzle = ClassicGenerator.generate(Difficulty.HARD, Random(99), timeBudgetMs = 500)
        val elapsed = System.currentTimeMillis() - start
        assertTrue(elapsed < 10_000, "elapsed=$elapsed")
        assertEquals(1, ClassicSolver.countSolutions(puzzle.copyOf(), 2))
    }
}
