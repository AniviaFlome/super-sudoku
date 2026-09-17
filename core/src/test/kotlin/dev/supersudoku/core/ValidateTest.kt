package dev.supersudoku.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun classicGrid(): GridDef = GridDef(
    id = "t", name = "T", variant = Variant.CLASSIC, x = 0, y = 0,
    givens = emptyMap(),
)

private fun boardOf(vararg cells: Triple<Int, Int, Int>): (Pos) -> Int {
    val m = cells.associate { Pos(it.first, it.second) to it.third }
    return { m[it] ?: 0 }
}

class ValidateTest {
    @Test
    fun rowDuplicateFlagged() {
        val g = classicGrid()
        val get = boardOf(Triple(0, 0, 5), Triple(3, 0, 5))
        val c = validateGrid(g, get)
        assertEquals(1, c.size)
        assertEquals(setOf(Pos(0, 0), Pos(3, 0)), c[0].cells)
    }

    @Test
    fun cleanBoardHasNoConflicts() {
        val g = classicGrid()
        // first two rows of a valid solution fragment
        val get = boardOf(
            Triple(0, 0, 5), Triple(1, 0, 3), Triple(2, 0, 4),
            Triple(0, 1, 6), Triple(1, 1, 7), Triple(2, 1, 2),
        )
        assertTrue(validateGrid(g, get).isEmpty())
    }

    @Test
    fun inequalityViolation() {
        val g = classicGrid().copy(
            inequalities = listOf(Pos(0, 0) to Pos(1, 0)),
        )
        assertTrue(validateGrid(g, boardOf(Triple(0, 0, 2), Triple(1, 0, 5))).isEmpty())
        val bad = validateGrid(g, boardOf(Triple(0, 0, 5), Triple(1, 0, 2)))
        assertEquals(1, bad.size)
        assertEquals(ConflictKind.INEQUALITY, bad[0].kind)
    }

    @Test
    fun kropkiDot() {
        val g = classicGrid().copy(dots = listOf(Pos(0, 0) to Pos(1, 0)))
        assertTrue(validateGrid(g, boardOf(Triple(0, 0, 4), Triple(1, 0, 5))).isEmpty())
        val bad = validateGrid(g, boardOf(Triple(0, 0, 4), Triple(1, 0, 6)))
        assertEquals(1, bad.size)
        assertEquals(ConflictKind.DOT, bad[0].kind)
    }

    @Test
    fun killerSumAndRepeatsAllowed() {
        val cage = Cage(setOf(Pos(0, 0), Pos(1, 0), Pos(2, 0), Pos(3, 0)), 9)
        val g = classicGrid().copy(cages = listOf(cage))
        // repeats allowed: no UNIT conflict inside cage is checked by CAGE rule;
        // 1+2+2+4=9 is fine even with the repeated 2 (row dup still flagged as UNIT)
        val ok = validateGrid(g, boardOf(
            Triple(0, 0, 1), Triple(1, 0, 2), Triple(2, 0, 2), Triple(3, 0, 4),
        ))
        assertTrue(ok.none { it.kind == ConflictKind.CAGE }, "repeats must not trip CAGE")
        // wrong sum flagged
        val bad = validateGrid(g, boardOf(
            Triple(0, 0, 1), Triple(1, 0, 2), Triple(2, 0, 3), Triple(3, 0, 4),
        ))
        assertTrue(bad.any { it.kind == ConflictKind.CAGE })
        // overshoot flagged early: 8+8=16 > 9 with 2 cells filled... use total 9, cells 8,8
        val over = validateGrid(g, boardOf(Triple(0, 0, 8), Triple(1, 0, 8)))
        assertTrue(over.any { it.kind == ConflictKind.CAGE })
    }

    @Test
    fun additionEquation() {
        // 13 + 78 = 91 style: two 2-digit operands, 2-digit total
        val eq = Equation(
            listOf(listOf(Pos(0, 0), Pos(1, 0)), listOf(Pos(0, 1), Pos(1, 1))),
            listOf(Pos(0, 2), Pos(1, 2)),
        )
        val g = classicGrid().copy(equations = listOf(eq))
        val ok = boardOf(
            Triple(0, 0, 1), Triple(1, 0, 3),
            Triple(0, 1, 7), Triple(1, 1, 8),
            Triple(0, 2, 9), Triple(1, 2, 1),
        )
        assertEquals(EquationState.OK, equationState(eq, ok))
        assertTrue(validateGrid(g, ok).none { it.kind == ConflictKind.EQUATION })
        val bad = boardOf(
            Triple(0, 0, 1), Triple(1, 0, 3),
            Triple(0, 1, 7), Triple(1, 1, 8),
            Triple(0, 2, 9), Triple(1, 2, 2),
        )
        assertEquals(EquationState.WRONG, equationState(eq, bad))
        assertTrue(validateGrid(g, bad).any { it.kind == ConflictKind.EQUATION })
        val partial = boardOf(Triple(0, 0, 1))
        assertEquals(EquationState.INCOMPLETE, equationState(eq, partial))
    }

    @Test
    fun diagonalDuplicate() {
        val g = classicGrid().copy(diagonals = true)
        // same digit twice on main diagonal, otherwise clean
        val get = boardOf(Triple(0, 0, 7), Triple(4, 4, 7))
        val c = validateGrid(g, get).filter { it.kind == ConflictKind.UNIT }
        assertEquals(1, c.size)
        assertEquals(setOf(Pos(0, 0), Pos(4, 4)), c[0].cells)
    }

    @Test
    fun irregularRegions() {
        val r0 = (0 until 3).flatMap { dy -> (0 until 3).map { Pos(it, dy) } }.toSet()
        val rest = (0 until 9).flatMap { dy -> (0 until 9).map { Pos(it, dy) } }.toSet() - r0
        // split rest into 8 fake single... simpler: two regions
        val g = classicGrid().copy(
            variant = Variant.IRREGULAR,
            regions = listOf(r0) + rest.chunked(9).map { it.toSet() },
        )
        // duplicate inside r0, same row? (0,0)=1,(1,0)=1 share row too; use col spread:
        val get = boardOf(Triple(0, 0, 1), Triple(1, 1, 1))
        val c = validateGrid(g, get).filter { it.kind == ConflictKind.UNIT }
        // (0,0),(1,1): different rows/cols/boxes, same irregular region -> 1 conflict
        assertEquals(1, c.size)
    }

    @Test
    fun disjointGroups() {
        // real disjoint shape: same box-relative offset across all 9 boxes
        val groups = (0 until 3).flatMap { oy ->
            (0 until 3).map { ox ->
                (0 until 3).flatMap { by ->
                    (0 until 3).map { bx -> Pos(bx * 3 + ox, by * 3 + oy) }
                }.toSet()
            }
        }
        val g = classicGrid().copy(variant = Variant.DISJOINT, groups = groups)
        // (0,0) and (3,3): different row/col/box, same offset (0,0) -> 1 conflict
        val get = boardOf(Triple(0, 0, 5), Triple(3, 3, 5))
        val c = validateGrid(g, get).filter { it.kind == ConflictKind.UNIT }
        assertEquals(1, c.size)
    }

    @Test
    fun gridCompletion() {
        // 2x2... use a full valid 9x9 classic solution
        val sol = intArrayOf(
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
        val g = classicGrid()
        val get: (Pos) -> Int = { sol[it.y * 9 + it.x] }
        assertTrue(isGridComplete(g, get))
        assertFalse(isGridComplete(g, boardOf(Triple(0, 0, 5))))
    }
}
