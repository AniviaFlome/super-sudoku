package dev.supersudoku.core

enum class ConflictKind { UNIT, INEQUALITY, DOT, CAGE, EQUATION }

/** A live rule violation. [cells] are highlighted; [gridId] scopes it to one sub-grid. */
data class Conflict(val kind: ConflictKind, val cells: Set<Pos>, val gridId: String)

/** Duplicate filled digits inside one unit (row/col/box/region/group/diagonal). */
fun unitConflicts(units: List<List<Pos>>, get: (Pos) -> Int, gridId: String): List<Conflict> {
    val out = mutableListOf<Conflict>()
    for (u in units) {
        val seen = HashMap<Int, MutableList<Pos>>()
        for (c in u) {
            val v = get(c)
            if (v in 1..9) seen.getOrPut(v) { mutableListOf() }.add(c)
        }
        for ((_, cells) in seen) {
            if (cells.size > 1) out.add(Conflict(ConflictKind.UNIT, cells.toSet(), gridId))
        }
    }
    return out
}

/** All row/col/box(/region//group/diagonal) units of a grid. */
fun GridDef.units(): List<List<Pos>> {
    val u = mutableListOf<List<Pos>>()
    u.addAll(rows())
    u.addAll(cols())
    if (variant == Variant.IRREGULAR) u.addAll(regions.map { it.toList() }) else u.addAll(boxes())
    if (variant == Variant.DISJOINT) u.addAll(groups.map { it.toList() })
    if (diagonals) {
        u.add(maindiag())
        u.add(updiag())
    }
    return u
}

/** Validate one 9x9 (or 9x9 practice board wrapped as a CLASSIC GridDef). */
fun validateGrid(grid: GridDef, get: (Pos) -> Int): List<Conflict> {
    val out = mutableListOf<Conflict>()
    out.addAll(unitConflicts(grid.units(), get, grid.id))

    for ((lo, hi) in grid.inequalities) {
        val a = get(lo)
        val b = get(hi)
        if (a in 1..9 && b in 1..9 && a >= b) {
            out.add(Conflict(ConflictKind.INEQUALITY, setOf(lo, hi), grid.id))
        }
    }
    for ((a, b) in grid.dots) {
        val x = get(a)
        val y = get(b)
        if (x in 1..9 && y in 1..9 && kotlin.math.abs(x - y) != 1) {
            out.add(Conflict(ConflictKind.DOT, setOf(a, b), grid.id))
        }
    }
    if (grid.variant == Variant.KROPKI) {
        // "Otherwise, there is no pink bridge": non-dotted orthogonal neighbours
        // inside the grid must NOT be consecutive (carykh's original rule text).
        val dotted = HashSet<String>(grid.dots.size * 2)
        for ((a, b) in grid.dots) {
            dotted.add(edgeKey(a, b))
        }
        for (dy in 0 until 9) for (dx in 0 until 9) {
            val p = Pos(grid.x + dx, grid.y + dy)
            if (dx + 1 < 9) {
                val q = Pos(p.x + 1, p.y)
                checkNegativeDot(get, p, q, dotted, grid.id, out)
            }
            if (dy + 1 < 9) {
                val q = Pos(p.x, p.y + 1)
                checkNegativeDot(get, p, q, dotted, grid.id, out)
            }
        }
    }
    for (cage in grid.cages) {
        val vals = cage.cells.map(get)
        val filled = vals.filter { it in 1..9 }
        val empty = vals.size - filled.size
        val sum = filled.sum()
        // repeats allowed: min/max achievable totals bound the cage
        val impossible = (filled.size == vals.size && sum != cage.total) ||
            sum + empty * 1 > cage.total ||
            sum + empty * 9 < cage.total
        if (impossible) {
            val culprits = cage.cells.filter { get(it) in 1..9 }.toSet()
            out.add(Conflict(ConflictKind.CAGE, culprits.ifEmpty { cage.cells }, grid.id))
        }
    }
    for (eq in grid.equations) {
        when (equationState(eq, get)) {
            EquationState.WRONG -> out.add(
                Conflict(
                    ConflictKind.EQUATION,
                    eq.operands.flatten().toSet() + eq.total.toSet(),
                    grid.id,
                )
            )
            EquationState.OK, EquationState.INCOMPLETE -> Unit
        }
    }
    return out
}

enum class EquationState { OK, WRONG, INCOMPLETE }

/** Digit string value of ordered cells, or null when any cell is empty. */
fun equationValue(cells: List<Pos>, get: (Pos) -> Int): Int? {
    var v = 0
    for (c in cells) {
        val d = get(c)
        if (d !in 1..9) return null
        v = v * 10 + d
    }
    return v
}

fun equationState(eq: Equation, get: (Pos) -> Int): EquationState {
    val ops = eq.operands.map { equationValue(it, get) ?: return EquationState.INCOMPLETE }
    val tot = equationValue(eq.total, get) ?: return EquationState.INCOMPLETE
    return if (ops.sum() == tot) EquationState.OK else EquationState.WRONG
}

private fun edgeKey(a: Pos, b: Pos): String {
    val (x1, y1, x2, y2) = if (a.x < b.x || (a.x == b.x && a.y < b.y)) {
        listOf(a.x, a.y, b.x, b.y)
    } else {
        listOf(b.x, b.y, a.x, a.y)
    }
    return "$x1,$y1,$x2,$y2"
}

private fun checkNegativeDot(
    get: (Pos) -> Int,
    p: Pos,
    q: Pos,
    dotted: Set<String>,
    gridId: String,
    out: MutableList<Conflict>,
) {
    if (edgeKey(p, q) in dotted) return
    val a = get(p)
    val b = get(q)
    if (a in 1..9 && b in 1..9 && kotlin.math.abs(a - b) == 1) {
        out.add(Conflict(ConflictKind.DOT, setOf(p, q), gridId))
    }
}

/** Validate every sub-grid (overlap cells are checked in each grid they belong to). */
fun validateSuper(puzzle: SuperPuzzle, get: (Pos) -> Int): List<Conflict> =
    puzzle.grids.flatMap { validateGrid(it, get) }

/** A grid is complete when all 81 cells are filled and conflict-free. */
fun isGridComplete(grid: GridDef, get: (Pos) -> Int): Boolean {
    for (dy in 0 until 9) for (dx in 0 until 9) {
        if (get(Pos(grid.x + dx, grid.y + dy)) !in 1..9) return false
    }
    return validateGrid(grid, get).isEmpty()
}
