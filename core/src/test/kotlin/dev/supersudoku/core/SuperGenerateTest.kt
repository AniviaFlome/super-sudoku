package dev.supersudoku.core

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SuperGenerateTest {
    private fun asset(name: String): String {
        val f = File("../app/src/main/assets/$name")
        assertTrue(f.exists(), "missing ${f.absolutePath}")
        return f.readText()
    }

    @Test
    fun templateGivensAreUniquelySolvable() {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val base = HashMap<Pos, Int>()
        for (g in puzzle.grids) for ((p, v) in g.givens) base[p] = v
        assertEquals(1, SuperGenerator.countSolutions(puzzle.grids, base, 2))
    }

    @Test
    fun diggingKeepsUniqueSolutionAndSubsetOfIt() {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solution = PuzzleLoader.loadSolution(asset("solution.json"))
        val out = SuperGenerator.generate(
            template = puzzle,
            solution = solution,
            random = Random(7),
            maxRemove = 4,
            timeBudgetMs = 120_000,
        )
        // Result is a strict subset of the template givens and of the solution.
        val base = HashMap<Pos, Int>()
        for (g in puzzle.grids) for ((p, v) in g.givens) base[p] = v
        assertTrue(out.givens.size <= base.size)
        for ((p, v) in out.givens) {
            assertEquals(base[p], v, "generated given differs at $p")
            assertEquals(solution[p], v, "generated given disagrees with solution at $p")
        }
        // Still exactly one solution.
        assertEquals(1, SuperGenerator.countSolutions(puzzle.grids, out.givens, 2))
    }
}
