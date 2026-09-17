package dev.supersudoku.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Regression: every standalone variant game is uniquely solvable. */
class StandaloneTest {
    private fun asset(name: String): String {
        val f = File("../app/src/main/assets/$name")
        assertTrue(f.exists(), "missing ${f.absolutePath}")
        return f.readText()
    }

    @Test
    fun allStandaloneGridsUnique() {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solution = PuzzleLoader.loadSolution(asset("solution.json"))
        val standalone = StandaloneLoader.load(asset("standalone.json"))
        assertEquals(puzzle.grids.map { it.id }.toSet(), standalone.keys)
        for (g in puzzle.grids) {
            val givens = standalone.getValue(g.id)
            assertTrue(givens.isNotEmpty(), "${g.id}: no givens")
            // Givens agree with the ring solution inside this grid's bounds.
            for ((p, v) in givens) {
                assertTrue(
                    p.x in g.x until g.x + 9 && p.y in g.y until g.y + 9,
                    "${g.id}: given out of bounds at $p",
                )
                assertEquals(solution[p], v, "${g.id}: given disagrees with solution at $p")
            }
            val solo = g.copy(givens = emptyMap())
            assertEquals(
                1, SuperGenerator.countSolutions(listOf(solo), givens, 2),
                "${g.id}: expected exactly one standalone solution",
            )
        }
    }
}
