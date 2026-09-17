package dev.supersudoku.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Independent import vector (crafted by scripts/craft_mini_penpa.py). */
class PenpaMiniTest {
    @Test
    fun miniClassicPlusFeatures() {
        val url = File("../docs/mini-penpa-url.txt").readText().trim()
        val imported = try {
            PenpaImport.import(url)
        } catch (e: PenpaImportError) {
            throw AssertionError("import failed: ${e.message}")
        }
        assertEquals(1, imported.puzzle.grids.size)
        val g = imported.puzzle.grids[0]
        assertEquals(0, g.x)
        assertEquals(0, g.y)
        assertEquals("Mini Test", g.name)
        assertEquals(mapOf(Pos(0, 0) to 5, Pos(1, 1) to 3), g.givens)
        // killer cage wins the variant vote; other features still attached
        assertEquals(Variant.KILLER, g.variant)
        assertEquals(1, g.cages.size)
        assertEquals(setOf(Pos(6, 6), Pos(7, 6)), g.cages[0].cells)
        assertEquals(11, g.cages[0].total)
        assertEquals(listOf(Pos(2, 2) to Pos(3, 2)), g.inequalities)
        assertEquals(listOf(Pos(5, 3) to Pos(5, 4)), g.dots)
        assertNull(imported.solution)
    }
}
