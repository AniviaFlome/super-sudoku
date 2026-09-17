package dev.supersudoku.core

import java.io.File

/**
 * Build-time entry point: solves carykh's Super Sudoku and writes
 * app/src/main/assets/solution.json. Run via `:core:solveSuper`.
 *
 * Usage: gradle :core:solveSuper [--args="negativeKropki"]
 * Exit code 0 + "UNIQUE" only when exactly one solution exists.
 */
fun main(args: Array<String>) {
    val negativeKropki = args.contains("negativeKropki")
    val onlyGrid = args.firstOrNull { it.startsWith("grid=") }?.substringAfter("=")
    val puzzleFile = File("../app/src/main/assets/puzzle.json")
    require(puzzleFile.exists()) { "missing ${puzzleFile.absolutePath}" }
    val puzzle = PuzzleLoader.load(puzzleFile.readText())
    val grids = if (onlyGrid != null) puzzle.grids.filter { it.id == onlyGrid } else puzzle.grids
    require(grids.isNotEmpty()) { "no such grid: $onlyGrid" }

    val solver = SuperSolver(grids, negativeKropki = negativeKropki)
    val dom = solver.initialDomains()
    if (!args.contains("nogivens")) {
        for (g in grids) for ((pos, v) in g.givens) {
            solver.setGiven(dom, pos, v)
        }
    }
    val t0 = System.currentTimeMillis()
    solver.search(dom, limit = 2)
    val ms = System.currentTimeMillis() - t0
    println("grids=${grids.map { it.id }} negativeKropki=$negativeKropki solutions=${solver.solutions.size} timeMs=$ms")
    if (args.contains("dump")) {
        // print each solution as 33 rows of 33 chars (. for empty/non-grid)
        val allCells = grids.flatMap { it.cells() }.toSet()
        for ((si, s) in solver.solutions.withIndex()) {
            println("--- solution $si ---")
            val m = solver.solutionMap(s)
            for (y in 0 until 33) {
                println((0 until 33).joinToString("") { x ->
                    val p = Pos(x, y)
                    if (p !in allCells) "#"
                    else (m[p] ?: 0).let { if (it == 0) "." else it.toString() }
                })
            }
        }
    }
    if (solver.solutions.size != 1) {
        println(if (solver.solutions.isEmpty()) "NO_SOLUTION" else "NOT_UNIQUE")
        kotlin.system.exitProcess(1)
    }
    val sol = solver.solutionMap(solver.solutions[0])
    if (onlyGrid != null) {
        println("single-grid SAT, skipping solution.json")
        return
    }
    // sanity: solution extends givens and passes every validator
    val board = PlayBoard(33, 33)
    for ((pos, v) in sol) board.setGiven(pos.x, pos.y, v)
    val conflicts = validateSuper(puzzle) { board.get(it) }
    require(conflicts.isEmpty()) { "solution violates rules: $conflicts" }

    val out = buildString {
        append("{\"cells\":[")
        sol.entries.sortedWith(compareBy({ it.key.y }, { it.key.x })).forEachIndexed { i, (p, v) ->
            if (i > 0) append(",")
            append("[${p.x},${p.y},$v]")
        }
        append("]}")
    }
    File("../app/src/main/assets/solution.json").writeText(out)
    println("UNIQUE wrote solution.json (${sol.size} cells)")
}
