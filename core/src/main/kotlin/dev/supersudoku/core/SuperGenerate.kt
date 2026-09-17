package dev.supersudoku.core

import kotlin.random.Random

/** Map generation difficulty: how many givens to dig away (from ~125). */
enum class MapDifficulty(val maxRemove: Int, val timeBudgetMs: Long, val label: String) {
    EASY(12, 15_000, "Easy"),
    MEDIUM(28, 30_000, "Medium"),
    HARD(48, 60_000, "Hard"),
}

/**
 * Super map generator: mints fresh playable "maps" out of one constraint
 * template (e.g. carykh's ring). Cage shapes/totals, inequalities, dots,
 * regions, groups and equations are fixed constraints; only the given set
 * changes, so every generated map shares the template's known solution.
 *
 * Strategy mirrors [ClassicGenerator]: dig holes while the puzzle still has
 * exactly one solution. Digging starts from the template's givens (keeps the
 * puzzle's character and difficulty) and removes up to [maxRemove] givens
 * within [timeBudgetMs]. The result is always valid (unique solution);
 * it may simply remove fewer givens when the budget elapses.
 */
object SuperGenerator {
    /** Count solutions (up to [limit]) for [givens] on these grids. */
    fun countSolutions(
        grids: List<GridDef>,
        givens: Map<Pos, Int>,
        limit: Int = 2,
        negativeKropki: Boolean = false,
    ): Int {
        val solver = SuperSolver(grids, negativeKropki)
        val dom = solver.initialDomains()
        for ((p, v) in givens) solver.setGiven(dom, p, v)
        solver.search(dom, limit)
        return solver.solutions.size
    }

    data class GeneratedMap(
        /** New givens (subset of [solution], same solution as the template). */
        val givens: Map<Pos, Int>,
        val removedCount: Int,
        val timedOut: Boolean,
    )    fun generate(
        template: SuperPuzzle,
        solution: Map<Pos, Int>,
        random: Random = Random.Default,
        /** Max givens to remove (cap for novelty vs difficulty drift). */
        maxRemove: Int = 32,
        timeBudgetMs: Long = 30_000,
        negativeKropki: Boolean = false,
        /**
         * Per-grid safety floor: never leave a grid with fewer than this
         * fraction of its template givens (keeps every variant playable).
         */
        minPerGridFraction: Double = 0.4,
    ): GeneratedMap {
        // Merge template givens (overlap cells must agree; validated elsewhere).
        val base = HashMap<Pos, Int>()
        for (g in template.grids) for ((p, v) in g.givens) base[p] = v
        require(base.isNotEmpty()) { "template has no givens" }
        // Sanity: givens must match the known solution.
        for ((p, v) in base) {
            require(solution[p] == v) { "template given at $p disagrees with solution" }
        }
        val floors = template.grids.associate { g ->
            g.id to (g.givens.size * minPerGridFraction).toInt().coerceAtLeast(6)
        }
        fun gridCounts(givens: Map<Pos, Int>): Map<String, Int> {
            val counts = HashMap<String, Int>()
            for (g in template.grids) {
                var n = 0
                for ((p, _) in g.givens) if (p in givens) n++
                counts[g.id] = n
            }
            return counts
        }
        val givens = HashMap(base)
        val order = givens.keys.shuffled(random)
        val deadline = System.currentTimeMillis() + timeBudgetMs
        var removed = 0
        var timedOut = false
        for (p in order) {
            if (removed >= maxRemove) break
            if (System.currentTimeMillis() > deadline) {
                timedOut = true
                break
            }
            // Per-grid floor check (only grids that had this cell as a given).
            var blocked = false
            for (g in template.grids) {
                if (p !in g.givens) continue
                val remaining = g.givens.keys.count { it in givens } - 1
                if (remaining < (floors[g.id] ?: 0)) {
                    blocked = true
                    break
                }
            }
            if (blocked) continue
            val v = givens.remove(p) ?: continue
            if (countSolutions(template.grids, givens, 2, negativeKropki) != 1) {
                givens[p] = v // restore: removal breaks uniqueness
            } else {
                removed++
            }
        }
        return GeneratedMap(givens.toMap(), removed, timedOut)
    }

    /**
     * Dig down from a full solution to a standalone puzzle: start with every
     * cell given, remove in random order while uniqueness holds, stopping at
     * [targetGivens] (or the time budget). Used for standalone variant games
     * whose template givens alone are not uniquely solvable.
     */
    fun digDown(
        grids: List<GridDef>,
        solution: Map<Pos, Int>,
        targetGivens: Int,
        random: Random = Random.Default,
        timeBudgetMs: Long = 120_000,
        negativeKropki: Boolean = false,
    ): GeneratedMap {
        val givens = HashMap(solution)
        val order = givens.keys.shuffled(random)
        val deadline = System.currentTimeMillis() + timeBudgetMs
        var removed = 0
        var timedOut = false
        for (p in order) {
            if (givens.size <= targetGivens) break
            if (System.currentTimeMillis() > deadline) {
                timedOut = true
                break
            }
            val v = givens.remove(p) ?: continue
            if (countSolutions(grids, givens, 2, negativeKropki) != 1) {
                givens[p] = v // restore: removal breaks uniqueness
            } else {
                removed++
            }
        }
        return GeneratedMap(givens.toMap(), removed, timedOut)
    }
}
