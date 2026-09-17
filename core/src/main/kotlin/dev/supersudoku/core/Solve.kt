package dev.supersudoku.core

import kotlin.math.abs

/**
 * Constraint solver for the Super Sudoku ring (and any GridDef set).
 *
 * Variables are board cells (global [Pos]), domains are 1..9 bitmasks.
 * Propagation: unit naked/hidden singles, inequality bounds, kropki
 * adjacency, killer sum bounds, equation column feasibility. Search: MRV.
 *
 * Prune helpers return -1 (contradiction), 0 (no change) or 1 (changed).
 *
 * @param negativeKropki when true, cells NOT joined by a dot may not hold
 * consecutive digits (standard "all dots given" Kropki). The intended rule
 * for carykh's puzzle is determined empirically: the setting that yields a
 * unique solution wins (see SolveMain report).
 */
class SuperSolver(
    private val grids: List<GridDef>,
    private val negativeKropki: Boolean = false,
    cells: Set<Pos> = grids.flatMap { it.cells() }.toSet(),
) {
    private val index: Map<Pos, Int> = cells.withIndex().associate { (i, p) -> p to i }
    private val posOf: Array<Pos> = Array(cells.size) { Pos(-1, -1) }.also { arr ->
        for ((p, i) in index) arr[i] = p
    }
    private val n = cells.size
    private val ALL = (1..9).fold(0) { m, d -> m or (1 shl d) }

    private val units: List<IntArray> = buildList {
        for (g in grids) for (u in g.units()) {
            val ids = u.mapNotNull { index[it] }.toIntArray()
            if (ids.size == u.size) add(ids)
        }
    }
    private val ineqs: List<Triple<Int, Int, Boolean>> = buildList {
        for (g in grids) for ((lo, hi) in g.inequalities) {
            val a = index[lo]
            val b = index[hi]
            if (a != null && b != null) add(Triple(a, b, true))
        }
    }
    private val dots: List<Pair<Int, Int>> = buildList {
        for (g in grids) for ((a, b) in g.dots) {
            val x = index[a]
            val y = index[b]
            if (x != null && y != null) add(x to y)
        }
    }
    private val dotAdj: Array<MutableList<Int>> =
        Array<MutableList<Int>>(n) { _: Int -> mutableListOf<Int>() }.also { da ->
            for ((a, b) in dots) {
                da[a].add(b)
                da[b].add(a)
            }
        }
    private val kropkiCells: Set<Int> = grids
        .filter { it.dots.isNotEmpty() }
        .flatMap { it.cells() }.mapNotNull { index[it] }.toSet()

    private data class CageC(val vars: IntArray, val total: Int)
    private val cages: List<CageC> = buildList {
        for (g in grids) for (c in g.cages) {
            val vars = c.cells.mapNotNull { index[it] }.toIntArray()
            if (vars.size == c.cells.size) add(CageC(vars, c.total))
        }
    }

    private data class EqC(
        val operands: List<IntArray>, // most significant first
        val total: IntArray, // most significant first
    )
    private val equations: List<EqC> = buildList {
        for (g in grids) for (e in g.equations) {
            val ops = e.operands.map { op -> op.mapNotNull { index[it] }.toIntArray() }
            val tot = e.total.mapNotNull { index[it] }.toIntArray()
            if (ops.withIndex().all { (i, op) -> op.size == e.operands[i].size } &&
                tot.size == e.total.size
            ) {
                add(EqC(ops, tot))
            }
        }
    }

    val solutions = mutableListOf<IntArray>()

    fun initialDomains(): IntArray = IntArray(n) { ALL }

    fun setGiven(dom: IntArray, p: Pos, v: Int) {
        val i = index[p] ?: return
        dom[i] = 1 shl v
    }

    fun minOf(dom: IntArray, i: Int): Int {
        for (d in 1..9) if (dom[i] and (1 shl d) != 0) return d
        return 10
    }

    fun maxOf(dom: IntArray, i: Int): Int {
        for (d in 9 downTo 1) if (dom[i] and (1 shl d) != 0) return d
        return 0
    }

    fun count(dom: IntArray, i: Int): Int {
        var c = 0
        for (d in 1..9) if (dom[i] and (1 shl d) != 0) c++
        return c
    }

    /** Propagate to fixpoint; false on contradiction. */
    fun propagate(dom: IntArray): Boolean {
        while (true) {
            var any = false
            for (u in units) {
                when (pruneUnit(dom, u)) {
                    -1 -> return false
                    1 -> any = true
                    else -> Unit
                }
            }
            for ((a, b) in ineqs) {
                // a < b
                val maxB = maxOf(dom, b)
                var nm = 0
                for (d in 1 until maxB) if (dom[a] and (1 shl d) != 0) nm = nm or (1 shl d)
                if (nm == 0) return false
                if (nm != dom[a]) {
                    dom[a] = nm
                    any = true
                }
                val minA = minOf(dom, a)
                nm = 0
                for (d in minA + 1..9) if (dom[b] and (1 shl d) != 0) nm = nm or (1 shl d)
                if (nm == 0) return false
                if (nm != dom[b]) {
                    dom[b] = nm
                    any = true
                }
            }
            for ((a, b) in dots) {
                when (pruneConsec(dom, a, b)) {
                    -1 -> return false
                    1 -> any = true
                    else -> Unit
                }
                when (pruneConsec(dom, b, a)) {
                    -1 -> return false
                    1 -> any = true
                    else -> Unit
                }
            }
            if (negativeKropki) {
                when (pruneNegativeKropki(dom)) {
                    -1 -> return false
                    1 -> any = true
                    else -> Unit
                }
            }
            for (cage in cages) {
                when (pruneCage(dom, cage)) {
                    -1 -> return false
                    1 -> any = true
                    else -> Unit
                }
            }
            for (eq in equations) {
                if (!equationFeasible(dom, eq)) return false
            }
            for (i in 0 until n) if (dom[i] == 0) return false
            if (!any) return true
        }
    }

    private fun pruneUnit(dom: IntArray, u: IntArray): Int {
        var changed = false
        for (v in u) {
            val m = dom[v]
            if (m != 0 && m and (m - 1) == 0) {
                for (w in u) {
                    if (w != v && dom[w] and m != 0) {
                        dom[w] = dom[w] and m.inv()
                        if (dom[w] == 0) return -1
                        changed = true
                    }
                }
            }
        }
        for (d in 1..9) {
            val bit = 1 shl d
            var last = -1
            var cnt = 0
            for (v in u) {
                if (dom[v] and bit != 0) {
                    last = v
                    cnt++
                }
            }
            if (cnt == 0) return -1
            if (cnt == 1 && dom[last] != bit) {
                dom[last] = bit
                changed = true
            }
        }
        return if (changed) 1 else 0
    }

    /** a must have a consecutive partner in b's domain. */
    private fun pruneConsec(dom: IntArray, a: Int, b: Int): Int {
        var nm = 0
        for (d in 1..9) {
            if (dom[a] and (1 shl d) == 0) continue
            if ((d > 1 && dom[b] and (1 shl (d - 1)) != 0) ||
                (d < 9 && dom[b] and (1 shl (d + 1)) != 0)
            ) nm = nm or (1 shl d)
        }
        if (nm == 0) return -1
        if (nm == dom[a]) return 0
        dom[a] = nm
        return 1
    }

    /**
     * Negative kropki (sound but weak): if neighbour j is a forced singleton w,
     * i cannot be w±1.
     */
    private fun pruneNegativeKropki(dom: IntArray): Int {
        var changed = false
        for (i in 0 until n) {
            if (i !in kropkiCells) continue
            val p = posOf[i]
            val neighbours = listOf(
                Pos(p.x + 1, p.y), Pos(p.x - 1, p.y), Pos(p.x, p.y + 1), Pos(p.x, p.y - 1),
            )
            for (q in neighbours) {
                val j = index[q] ?: continue
                if (j !in kropkiCells || j in dotAdj[i]) continue
                if (count(dom, j) != 1) continue
                val w = minOf(dom, j)
                var nm = dom[i]
                if (w > 1) nm = nm and (1 shl (w - 1)).inv()
                if (w < 9) nm = nm and (1 shl (w + 1)).inv()
                if (nm == 0) return -1
                if (nm != dom[i]) {
                    dom[i] = nm
                    changed = true
                }
            }
        }
        return if (changed) 1 else 0
    }

    /** Killer bounds with repeats allowed + per-cell feasibility. */
    private fun pruneCage(dom: IntArray, cage: CageC): Int {
        var changed = false
        var fixed = 0
        var open = 0
        for (v in cage.vars) {
            if (count(dom, v) == 1) fixed += minOf(dom, v) else open++
        }
        if (fixed + open > cage.total) return -1
        if (fixed + open * 9 < cage.total) return -1
        for (v in cage.vars) {
            if (count(dom, v) == 1) continue
            var nm = 0
            for (d in 1..9) {
                if (dom[v] and (1 shl d) == 0) continue
                val rest = open - 1
                if (fixed + d + rest <= cage.total && fixed + d + rest * 9 >= cage.total) {
                    nm = nm or (1 shl d)
                }
            }
            if (nm == 0) return -1
            if (nm != dom[v]) {
                dom[v] = nm
                changed = true
            }
        }
        return if (changed) 1 else 0
    }

    /**
     * Column-wise addition feasibility with carries (right-aligned).
     * Sound but weak: only rejects impossible column/carry combos.
     */
    private fun equationFeasible(dom: IntArray, eq: EqC): Boolean {
        val width = maxOf(eq.total.size, eq.operands.maxOf { it.size })
        fun aligned(v: IntArray): List<Int> {
            val pad = width - v.size
            return List(pad) { -1 } + v.toList()
        }
        val ops = eq.operands.map { aligned(it) }
        val tot = aligned(eq.total)
        var carryMin = 0
        var carryMax = 0
        for (c in width - 1 downTo 0) {
            val tVar = tot[c]
            var sMin = carryMin
            var sMax = carryMax
            for (op in ops) {
                val v = op[c]
                if (v == -1) continue
                sMin += minOf(dom, v)
                sMax += maxOf(dom, v)
            }
            if (tVar == -1) {
                // left of the total: column sum must be a pure carry (0..29)
                if (sMin > 29) return false
                carryMin = 0
                carryMax = 2
                continue
            }
            val tMin = minOf(dom, tVar)
            val tMax = maxOf(dom, tVar)
            var ok = false
            var cOutMin = 3
            var cOutMax = -1
            for (co in 0..2) {
                if (tMax + 10 * co >= sMin && tMin + 10 * co <= sMax) {
                    ok = true
                    if (co < cOutMin) cOutMin = co
                    if (co > cOutMax) cOutMax = co
                }
            }
            if (!ok) return false
            carryMin = cOutMin
            carryMax = cOutMax
        }
        // most significant carry must vanish (no extra total digit)
        return carryMin <= 0
    }

    /** Depth-first search collecting up to [limit] solutions. */
    fun search(dom: IntArray, limit: Int = 2): Boolean {
        if (solutions.size >= limit) return true
        if (!propagate(dom)) return false
        var pick = -1
        var pickSize = 10
        for (i in 0 until n) {
            val c = count(dom, i)
            if (c == 0) return false
            if (c > 1 && c < pickSize) {
                pickSize = c
                pick = i
            }
        }
        if (pick == -1) {
            solutions.add(dom.copyOf())
            return solutions.size >= limit
        }
        for (d in 1..9) {
            if (dom[pick] and (1 shl d) != 0) {
                val next = dom.copyOf()
                next[pick] = 1 shl d
                if (search(next, limit)) return true
            }
        }
        return solutions.size >= limit
    }

    fun solutionMap(sol: IntArray): Map<Pos, Int> {
        val out = HashMap<Pos, Int>()
        for (i in 0 until n) out[posOf[i]] = minOf(sol, i)
        return out
    }
}
