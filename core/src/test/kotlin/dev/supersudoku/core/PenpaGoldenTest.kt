package dev.supersudoku.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Golden test: importing TheSudokuer's Penpa+ transcription URL must
 * reproduce the bundled puzzle.json (modulo the image-verified (8,7)=3
 * correction and display names/labels).
 */
class PenpaGoldenTest {
    private fun readAsset(name: String): String {
        val f = File("../app/src/main/assets/$name")
        assertTrue(f.exists(), "missing ${f.absolutePath}")
        return f.readText()
    }

    private fun readUrl(): String {
        val f = File("../docs/penpa-url.txt")
        assertTrue(f.exists(), "missing ${f.absolutePath}")
        return f.readText().trim()
    }

    @Test
    fun superUrlRoundTrips() {
        val bundled = PuzzleLoader.load(readAsset("puzzle.json"))
        val imported = try {
            PenpaImport.import(readUrl()).puzzle
        } catch (e: PenpaImportError) {
            fail("import failed: ${e.message}")
        }
        assertEquals(bundled.grids.size, imported.grids.size, "grid count")
        val byId = bundled.grids.associateBy { it.id }
        // bundled has the image-verified (8,7)=3 correction; the raw URL does not
        val corrected = Pos(8, 7) to 3
        // imported ids are positional (g0..); match by rect instead
        for (imp in imported.grids) {
            val ref = byId.values.firstOrNull { it.x == imp.x && it.y == imp.y }
                ?: fail("imported grid at (${imp.x},${imp.y}) matches no bundled grid")
            assertEquals(ref.variant, imp.variant, "variant @(${imp.x},${imp.y})")
            assertEquals(
                ref.givens - corrected.first,
                imp.givens,
                "givens @(${imp.x},${imp.y})",
            )
            assertEquals(
                ref.inequalities.map { it.first to it.second }.toSet(),
                imp.inequalities.map { it.first to it.second }.toSet(),
                "inequalities @(${imp.x},${imp.y})",
            )
            assertEquals(
                ref.dots.map { setOf(it.first, it.second) }.toSet(),
                imp.dots.map { setOf(it.first, it.second) }.toSet(),
                "dots @(${imp.x},${imp.y})",
            )
            assertEquals(
                ref.cages.map { it.cells to it.total }.toSet(),
                imp.cages.map { it.cells to it.total }.toSet(),
                "cages @(${imp.x},${imp.y})",
            )
            assertEquals(
                ref.equations.map { e -> e.operands to e.total }.toSet(),
                imp.equations.map { e -> e.operands to e.total }.toSet(),
                "equations @(${imp.x},${imp.y})",
            )
            assertEquals(ref.diagonals, imp.diagonals, "diagonals @(${imp.x},${imp.y})")
            assertEquals(
                ref.regions.map { it }.toSet(),
                imp.regions.map { it }.toSet(),
                "regions @(${imp.x},${imp.y})",
            )
            assertEquals(
                ref.groups.map { it }.toSet(),
                imp.groups.map { it }.toSet(),
                "groups @(${imp.x},${imp.y})",
            )
        }
        // bundled labels are the transcription source plus the compass-caption
        // adjustments from scripts/emit_puzzle.py (LABEL_MOVES); keep in sync.
        val moves = mapOf(
            Triple("X", 5, 24) to Triple("X", 4, 25),
            Triple("Kropki", 31, 10) to Triple("Kropki", 30, 11),
        )
        assertEquals(
            bundled.labels.map { Triple(it.text, it.x, it.y) }.toSet(),
            imported.labels.map { Triple(it.text, it.x, it.y) }
                .map { moves[it] ?: it }.toSet(),
        )
    }

    @Test
    fun badUrlsRejected() {
        try {
            PenpaImport.import("https://example.com/not-a-puzzle")
            fail("should reject")
        } catch (e: PenpaImportError) {
            // expected
        }
        try {
            PenpaImport.import("https://swaroopg92.github.io/penpa-edit/#m=solve&p=!!!!")
            fail("should reject")
        } catch (e: PenpaImportError) {
            // expected
        }
    }
}
