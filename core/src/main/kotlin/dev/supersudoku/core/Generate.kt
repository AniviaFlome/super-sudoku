package dev.supersudoku.core

import kotlin.random.Random

/** Classic 9x9 solver / generator (used for practice mode). Grids are IntArray(81), 0 = empty. */
object ClassicSolver {
    fun idx(r: Int, c: Int) = r * 9 + c

    private fun peersOf(i: Int): IntArray {
        val r = i / 9
        val c = i % 9
        val out = IntArray(20)
        var n = 0
        for (k in 0 until 9) {
            if (k != c) out[n++] = idx(r, k)
            if (k != r) out[n++] = idx(k, c)
        }
        val br = r / 3 * 3
        val bc = c / 3 * 3
        for (dr in 0 until 3) for (dc in 0 until 3) {
            val j = idx(br + dr, bc + dc)
            if (j != i && (br + dr) != r && (bc + dc) != c) out[n++] = j
        }
        return out.copyOf(n)
    }

    private val peers: Array<IntArray> = Array(81) { peersOf(it) }

    fun candidates(g: IntArray, i: Int): IntArray {
        if (g[i] != 0) return intArrayOf()
        val used = BooleanArray(10)
        for (j in peers[i]) {
            val v = g[j]
            if (v != 0) used[v] = true
        }
        return (1..9).filter { !used[it] }.toIntArray()
    }

    /** Count solutions up to [limit]. */
    fun countSolutions(g: IntArray, limit: Int = 2): Int {
        var best = -1
        var bestCands: IntArray = intArrayOf()
        for (i in 0 until 81) {
            if (g[i] == 0) {
                val c = candidates(g, i)
                if (c.isEmpty()) return 0
                if (best == -1 || c.size < bestCands.size) {
                    best = i
                    bestCands = c
                    if (c.size == 1) break
                }
            }
        }
        if (best == -1) return 1
        var n = 0
        for (v in bestCands) {
            g[best] = v
            n += countSolutions(g, limit - n)
            g[best] = 0
            if (n >= limit) return n
        }
        return n
    }

    /** Fill grid in place with a complete solution; false if unsolvable from this state. */
    fun solveInto(g: IntArray, random: Random): Boolean {
        var best = -1
        var bestCands: IntArray = intArrayOf()
        for (i in 0 until 81) {
            if (g[i] == 0) {
                val c = candidates(g, i)
                if (c.isEmpty()) return false
                if (best == -1 || c.size < bestCands.size) {
                    best = i
                    bestCands = c
                    if (c.size == 1) break
                }
            }
        }
        if (best == -1) return true
        val order = bestCands.toMutableList()
        order.shuffle(random)
        for (v in order) {
            g[best] = v
            if (solveInto(g, random)) return true
            g[best] = 0
        }
        return false
    }
}

enum class Difficulty(val givens: Int, val label: String) {
    EASY(44, "Easy"),
    MEDIUM(34, "Medium"),
    HARD(28, "Hard"),
}

object ClassicGenerator {
    /**
     * Generate a classic puzzle with a unique solution.
     * Digs holes in random order, keeping removals that preserve uniqueness,
     * until [difficulty.givens] clues remain (or no more safe removals).
     * Digging stops early when [timeBudgetMs] elapses; the result is always
     * a valid uniquely-solvable puzzle, possibly with a few extra givens.
     */
    fun generate(
        difficulty: Difficulty,
        random: Random = Random.Default,
        timeBudgetMs: Long = 12_000,
    ): IntArray {
        val full = IntArray(81)
        require(ClassicSolver.solveInto(full, random)) { "solver failed on empty grid" }
        val puzzle = full.copyOf()
        val deadline = System.currentTimeMillis() + timeBudgetMs
        val order = (0 until 81).shuffled(random)
        var remaining = 81
        for (i in order) {
            if (remaining <= difficulty.givens) break
            if (System.currentTimeMillis() > deadline) break
            val backup = puzzle[i]
            puzzle[i] = 0
            if (ClassicSolver.countSolutions(puzzle.copyOf(), 2) != 1) {
                puzzle[i] = backup
            } else {
                remaining--
            }
        }
        return puzzle
    }
}
