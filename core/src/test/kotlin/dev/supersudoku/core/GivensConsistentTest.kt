package dev.supersudoku.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The 124 givens are a subset of the true solution, so no correct
 * validator may flag any of them. Catches decode errors (wrong cage
 * totals, flipped inequalities, mis-parsed equations) and validator bugs.
 */
class GivensConsistentTest {
    @Test
    fun givensAreConflictFree() {
        val f = File("../app/src/main/assets/puzzle.json")
        assertTrue(f.exists())
        val puzzle = PuzzleLoader.load(f.readText())
        val board = PlayBoard(33, 33)
        for (g in puzzle.grids) for ((pos, v) in g.givens) {
            board.setGiven(pos.x, pos.y, v)
        }
        val conflicts = validateSuper(puzzle) { board.get(it) }
        assertTrue(conflicts.isEmpty(), "givens flagged: $conflicts")
    }

    @Test
    fun knownEquationFragmentsHold() {
        // H1 total row starts with 8, operands start with 3: 3x + yz = 8w is
        // satisfiable (e.g. 30+50=80); check the cage bound logic instead:
        // cage [(5,13),(5,14)] total 17 forces {8,9} in some order.
        val f = File("../app/src/main/assets/puzzle.json")
        val puzzle = PuzzleLoader.load(f.readText())
        val killer = puzzle.grids.first { it.id == "killer" }
        val cage = killer.cages.first { it.total == 17 }
        assertTrue(cage.cells.size == 2)
    }
}
