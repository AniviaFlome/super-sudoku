package dev.supersudoku.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Loads the real decoded puzzle.json and checks its shape. */
class LoaderTest {
    private fun loadPuzzle(): SuperPuzzle {
        val f = File("../app/src/main/assets/puzzle.json")
        assertTrue(f.exists(), "puzzle.json missing at ${f.absolutePath}")
        return PuzzleLoader.load(f.readText())
    }

    @Test
    fun eightGrids() {
        val p = loadPuzzle()
        assertEquals(
            listOf("classic", "futoshiki", "addition", "killer", "kropki", "sudoku_x", "irregular", "disjoint"),
            p.grids.map { it.id },
        )
    }

    @Test
    fun elementCounts() {
        val p = loadPuzzle()
        val byId = p.grids.associateBy { it.id }
        val givens = p.grids.flatMap { g -> g.givens.keys.map { it to g.id } }
        // 125 unique given cells (124 transcribed + (8,7)=3 image-verified
        // correction); 2 shared via overlaps appear in both grids
        assertEquals(125, givens.distinctBy { it.first }.size)
        assertEquals(84, byId.getValue("futoshiki").inequalities.size)
        assertEquals(33, byId.getValue("kropki").dots.size)
        assertEquals(21, byId.getValue("killer").cages.size)
        assertEquals(5, byId.getValue("addition").equations.size)
        assertEquals(9, byId.getValue("irregular").regions.size)
        assertEquals(9, byId.getValue("disjoint").groups.size)
    }

    @Test
    fun killerTwists() {
        val p = loadPuzzle()
        val killer = p.grids.first { it.id == "killer" }
        // 4-cell cage summing to 9 needs repeated digits
        assertTrue(killer.cages.any { it.cells.size == 4 && it.total == 9 })
        // 5-cell cage summing to 11 (from the r/sudoku solve discussion)
        assertTrue(killer.cages.any { it.cells.size == 5 && it.total == 11 })
        // 2-cell cage 8+9=17
        assertTrue(killer.cages.any { it.cells.size == 2 && it.total == 17 })
        // every cage has a positive total
        assertTrue(killer.cages.all { it.total > 0 })
    }

    @Test
    fun additionEquations() {
        val p = loadPuzzle()
        val add = p.grids.first { it.id == "addition" }
        // H1: "3? + ?? = 8?" rows
        val h1 = add.equations[0]
        assertEquals(
            listOf(listOf(Pos(19, 11), Pos(20, 11)), listOf(Pos(19, 12), Pos(20, 12))),
            h1.operands,
        )
        assertEquals(listOf(Pos(19, 13), Pos(20, 13)), h1.total)
        // every equation cell lies inside the addition grid
        for (e in add.equations) {
            for (op in e.operands) for (c in op) {
                assertTrue(c.x in 18..26 && c.y in 6..14, "operand $c outside addition grid")
            }
        }
    }

    @Test
    fun irregularAndDisjointPartition() {
        val p = loadPuzzle()
        val irr = p.grids.first { it.id == "irregular" }
        val cells = irr.regions.flatten().toSet()
        assertEquals(81, cells.size)
        assertTrue(irr.regions.all { it.size == 9 })
        val dis = p.grids.first { it.id == "disjoint" }
        assertEquals(81, dis.groups.flatten().toSet().size)
        assertTrue(dis.groups.all { it.size == 9 })
        // disjoint groups = box positions
        for (grp in dis.groups) {
            val rel = grp.map { Pos((it.x - 12) % 3, (it.y - 24) % 3) }.toSet()
            assertEquals(1, rel.size)
        }
    }

    @Test
    fun overlaps() {
        val p = loadPuzzle()
        // shared givens appear in both grids (kropki+irregular overlap)
        val kropki = p.grids.first { it.id == "kropki" }
        val irr = p.grids.first { it.id == "irregular" }
        assertEquals(6, kropki.givens[Pos(24, 18)])
        assertEquals(6, irr.givens[Pos(24, 18)])
        assertEquals(8, kropki.givens[Pos(25, 19)])
    }

    @Test
    fun gridsAt() {
        val p = loadPuzzle()
        // ring overlap: classic x futoshiki share (12..14, 6..8)
        assertEquals(
            setOf("classic", "futoshiki"),
            p.gridsAt(Pos(13, 7)).map { it.id }.toSet(),
        )
        // center slot is empty
        assertTrue(p.gridsAt(Pos(16, 16)).isEmpty())
    }
}
