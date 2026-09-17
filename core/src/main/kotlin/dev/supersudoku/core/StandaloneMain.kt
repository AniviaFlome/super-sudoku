package dev.supersudoku.core

import java.io.File
import kotlin.random.Random

/**
 * Build-time entry point: mints standalone 9x9 givens per variant grid and
 * writes app/src/main/assets/standalone.json.
 *
 * Grids already uniquely solvable from template givens keep them; the rest
 * are dug down from the full solution to a target given count (unique
 * solution guaranteed by construction).
 *
 * Usage: gradle :core:mintStandalone
 */
fun main() {
    val puzzleFile = File("../app/src/main/assets/puzzle.json")
    val solutionFile = File("../app/src/main/assets/solution.json")
    require(puzzleFile.exists()) { "missing ${puzzleFile.absolutePath}" }
    require(solutionFile.exists()) { "missing ${solutionFile.absolutePath}" }
    val puzzle = PuzzleLoader.load(puzzleFile.readText())
    val solution = PuzzleLoader.loadSolution(solutionFile.readText())
    val merged = HashMap<Pos, Int>()
    for (g in puzzle.grids) for ((p, v) in g.givens) merged[p] = v

    // Grids already unique standalone keep template givens; else dig target.
    val digTargets = mapOf(
        "classic" to 32,
        "addition" to 30,
        "killer" to 36,
        "sudoku_x" to 32,
        "irregular" to 32,
        "disjoint" to 32,
    )
    val out = StringBuilder()
    out.append("{\"grids\":[")
    puzzle.grids.forEachIndexed { gi, g ->
        val inBounds = merged.filterKeys { (x, y) -> x in g.x until g.x + 9 && y in g.y until g.y + 9 }
        val solo = g.copy(givens = inBounds)
        val baseCount = SuperGenerator.countSolutions(listOf(solo), inBounds, 2)
        val finalGivens: Map<Pos, Int>
        if (baseCount == 1 && g.id !in digTargets) {
            finalGivens = inBounds
            println("${g.id}: template givens=${inBounds.size} already unique, kept")
        } else {
            val target = digTargets[g.id] ?: inBounds.size
            val full = solution.filterKeys { (x, y) -> x in g.x until g.x + 9 && y in g.y until g.y + 9 }
            require(full.size == 81) { "${g.id}: solution slice != 81 (${full.size})" }
            val t0 = System.currentTimeMillis()
            val dug = SuperGenerator.digDown(
                grids = listOf(g.copy(givens = emptyMap())),
                solution = full,
                targetGivens = target,
                random = Random(g.id.hashCode()),
                timeBudgetMs = 180_000,
            )
            finalGivens = dug.givens
            val check = SuperGenerator.countSolutions(listOf(g.copy(givens = emptyMap())), finalGivens, 2)
            println("${g.id}: dug to ${finalGivens.size} (target $target, removed ${dug.removedCount}, timedOut=${dug.timedOut}) uniqueness=$check ms=${System.currentTimeMillis() - t0}")
            require(check == 1) { "${g.id}: minted puzzle not unique" }
        }
        if (gi > 0) out.append(",")
        out.append("{\"id\":\"${g.id}\",\"givens\":[")
        finalGivens.entries.sortedWith(compareBy({ it.key.y }, { it.key.x })).forEachIndexed { i, (p, v) ->
            if (i > 0) out.append(",")
            out.append("[${p.x},${p.y},$v]")
        }
        out.append("]}")
    }
    out.append("]}")
    File("../app/src/main/assets/standalone.json").writeText(out.toString())
    println("wrote standalone.json")
}
