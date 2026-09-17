package dev.supersudoku.app.state

import android.content.Context
import dev.supersudoku.core.GridDef
import dev.supersudoku.core.Pos
import dev.supersudoku.core.PuzzleLoader
import dev.supersudoku.core.StandaloneLoader
import dev.supersudoku.core.isGridComplete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Standalone variant games: each of the 8 variant grids as an independent
 * 9x9 puzzle (own givens from standalone.json, own save), decoupled from
 * the ring. Grids/coordinates are shifted to a local 0..8 board.
 */
object StandaloneStore {
    data class Entry(
        val gridId: String,
        val name: String,
        val variantLabel: String,
        val userFilled: Int,
        val fillable: Int,
        val solved: Boolean,
    )

    data class Loaded(
        val grid: GridDef,
        /** Local 9x9 givens (0 = empty). */
        val givens: IntArray,
        /** Local 9x9 solution (0 when unknown). */
        val solution: IntArray,
        val saveName: String,
    )

    fun saveName(gridId: String) = "standalone_$gridId.json"

    /** Shift a grid's geometry to local 0..8 coordinates. */
    fun localGrid(g: GridDef): GridDef {
        fun shift(p: Pos) = Pos(p.x - g.x, p.y - g.y)
        return g.copy(
            x = 0, y = 0,
            givens = g.givens.mapKeys { (p, _) -> shift(p) },
            inequalities = g.inequalities.map { (a, b) -> shift(a) to shift(b) },
            dots = g.dots.map { (a, b) -> shift(a) to shift(b) },
            cages = g.cages.map { c -> dev.supersudoku.core.Cage(c.cells.map(::shift).toSet(), c.total) },
            equations = g.equations.map { e ->
                dev.supersudoku.core.Equation(
                    e.operands.map { op -> op.map(::shift) },
                    e.total.map(::shift),
                )
            },
            regions = g.regions.map { r -> r.map(::shift).toSet() },
            groups = g.groups.map { r -> r.map(::shift).toSet() },
        )
    }

    private fun slice81(global: Map<Pos, Int>, g: GridDef): IntArray =
        IntArray(81) { i -> global[Pos(g.x + i % 9, g.y + i / 9)] ?: 0 }

    suspend fun load(context: Context, gridId: String): Loaded? = withContext(Dispatchers.IO) {
        runCatching {
            val puzzle = PuzzleLoader.load(
                context.assets.open("puzzle.json").bufferedReader().use { it.readText() }
            )
            val g = puzzle.grids.firstOrNull { it.id == gridId } ?: return@runCatching null
            val standalone = StandaloneLoader.load(
                context.assets.open("standalone.json").bufferedReader().use { it.readText() }
            )
            val givensGlobal = standalone[gridId] ?: return@runCatching null
            val solutionGlobal = runCatching {
                PuzzleLoader.loadSolution(
                    context.assets.open("solution.json").bufferedReader().use { it.readText() }
                )
            }.getOrNull().orEmpty()
            Loaded(
                grid = localGrid(g.copy(givens = givensGlobal)),
                givens = slice81(givensGlobal, g),
                solution = slice81(solutionGlobal, g),
                saveName = saveName(gridId),
            )
        }.getOrNull()
    }

    suspend fun list(context: Context): List<Entry> = withContext(Dispatchers.IO) {
        val loaded = puzzleGrids(context) ?: return@withContext emptyList()
        val (puzzle, standalone, solution) = loaded
        puzzle.grids.map { g ->
            val givensGlobal = standalone[g.id].orEmpty()
            val givens = slice81(givensGlobal, g)
            val sol = slice81(solution, g)
            val saved = SaveStore.loadBoard(context, saveName(g.id), 9, 9)
            val values = saved?.values ?: givens.copyOf()
            val givenMask = BooleanArray(81) { givens[it] != 0 }
            var fillable = 0
            var filled = 0
            for (i in 0 until 81) {
                if (givenMask[i]) continue
                fillable++
                if (values[i] != 0) filled++
            }
            val local = localGrid(g.copy(givens = givensGlobal))
            val solved = isGridComplete(local) { p -> values[p.y * 9 + p.x] }
            Entry(
                gridId = g.id,
                name = g.name,
                variantLabel = g.variant.name.lowercase().replace('_', ' '),
                userFilled = filled,
                fillable = fillable,
                solved = solved,
            )
        }
    }

    private suspend fun puzzleGrids(context: Context): Triple<
        dev.supersudoku.core.SuperPuzzle, Map<String, Map<Pos, Int>>, Map<Pos, Int>,
        >? = withContext(Dispatchers.IO) {
        runCatching {
            val puzzle = PuzzleLoader.load(
                context.assets.open("puzzle.json").bufferedReader().use { it.readText() }
            )
            val standalone = StandaloneLoader.load(
                context.assets.open("standalone.json").bufferedReader().use { it.readText() }
            )
            val solution = runCatching {
                PuzzleLoader.loadSolution(
                    context.assets.open("solution.json").bufferedReader().use { it.readText() }
                )
            }.getOrNull().orEmpty()
            Triple(puzzle, standalone, solution)
        }.getOrNull()
    }
}
