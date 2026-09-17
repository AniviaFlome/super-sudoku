package dev.supersudoku.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Board position. For the super puzzle: 33x33 canvas coords. For practice: 9x9 coords. */
data class Pos(val x: Int, val y: Int)

enum class Variant {
    CLASSIC, FUTOSHIKI, ADDITION, KILLER, KROPKI, SUDOKU_X, IRREGULAR, DISJOINT;

    companion object {
        fun of(s: String): Variant = valueOf(s.uppercase())
    }
}

/** Killer cage: digits may repeat (carykh's twist); only the sum must match. */
data class Cage(val cells: Set<Pos>, val total: Int)

/**
 * Addition equation: value(operand) = concat of its cell digits in listed order
 * (single-cell operand = that digit). Holds iff sum(values) == value(total).
 */
data class Equation(val operands: List<List<Pos>>, val total: List<Pos>)

data class GridDef(
    val id: String,
    val name: String,
    val variant: Variant,
    /** Top-left corner on the 33x33 canvas. */
    val x: Int,
    val y: Int,
    val givens: Map<Pos, Int>,
    val inequalities: List<Pair<Pos, Pos>> = emptyList(), // first < second
    val dots: List<Pair<Pos, Pos>> = emptyList(), // consecutive digits
    val cages: List<Cage> = emptyList(),
    val equations: List<Equation> = emptyList(),
    val diagonals: Boolean = false,
    val regions: List<Set<Pos>> = emptyList(), // irregular boxes
    val groups: List<Set<Pos>> = emptyList(), // disjoint positional groups
) {
    /** All 81 cells of this 9x9 (including shared overlap cells). */
    fun cells(): Set<Pos> =
        (0 until 9).flatMap { dy -> (0 until 9).map { dx -> Pos(x + dx, y + dy) } }.toSet()

    fun rows(): List<List<Pos>> = (0 until 9).map { dy -> (0 until 9).map { dx -> Pos(x + dx, y + dy) } }
    fun cols(): List<List<Pos>> = (0 until 9).map { dx -> (0 until 9).map { dy -> Pos(x + dx, y + dy) } }

    /** Standard 3x3 boxes (unused for IRREGULAR, which uses [regions]). */
    fun boxes(): List<List<Pos>> = (0 until 3).flatMap { by ->
        (0 until 3).map { bx ->
            (0 until 3).flatMap { dy -> (0 until 3).map { dx -> Pos(x + bx * 3 + dx, y + by * 3 + dy) } }
        }
    }

    fun maindiag(): List<Pos> = if (diagonals) {
        (0 until 9).map { i -> Pos(x + i, y + i) }
    } else emptyList()

    fun updiag(): List<Pos> = if (diagonals) {
        (0 until 9).map { i -> Pos(x + 8 - i, y + i) }
    } else emptyList()
}

data class BoardLabel(val text: String, val x: Int, val y: Int)

data class SuperPuzzle(val grids: List<GridDef>, val labels: List<BoardLabel>) {
    fun gridsAt(p: Pos): List<GridDef> = grids.filter { g ->
        p.x in g.x until g.x + 9 && p.y in g.y until g.y + 9
    }

    /** Tight board extents covering all grids (for rendering/sizing). */
    fun boardSize(): Pair<Int, Int> {
        if (grids.isEmpty()) return 9 to 9
        val w = grids.maxOf { it.x + 9 }
        val h = grids.maxOf { it.y + 9 }
        return w to h
    }

    /** Shift every grid/label/solution so min x,y become 0 (for imports). */
    fun normalized(): SuperPuzzle {
        val minX = grids.minOf { it.x }
        val minY = grids.minOf { it.y }
        if (minX == 0 && minY == 0) return this
        fun shift(p: Pos) = Pos(p.x - minX, p.y - minY)
        return copy(
            grids = grids.map { g ->
                g.copy(
                    x = g.x - minX, y = g.y - minY,
                    givens = g.givens.mapKeys { (p, _) -> shift(p) },
                    inequalities = g.inequalities.map { (a, b) -> shift(a) to shift(b) },
                    dots = g.dots.map { (a, b) -> shift(a) to shift(b) },
                    cages = g.cages.map { c -> Cage(c.cells.map(::shift).toSet(), c.total) },
                    equations = g.equations.map { e ->
                        Equation(
                            e.operands.map { op -> op.map(::shift) },
                            e.total.map(::shift),
                        )
                    },
                    regions = g.regions.map { r -> r.map(::shift).toSet() },
                    groups = g.groups.map { r -> r.map(::shift).toSet() },
                )
            },
            labels = labels.map { it.copy(x = it.x - minX, y = it.y - minY) },
        )
    }
}

// ---------- JSON DTOs (mirror puzzle.json emitted by scripts/emit_puzzle.py) ----------

@Serializable
private data class PuzzleJson(
    val meta: MetaJson,
    val grids: List<GridJson>,
    val labels: List<LabelJson>,
    val unassigned: List<List<Int>> = emptyList(),
)

@Serializable
private data class MetaJson(val name: String)

@Serializable
private data class GridJson(
    val id: String,
    val name: String,
    val variant: String,
    val x: Int,
    val y: Int,
    val size: Int = 9,
    val givens: List<List<Int>> = emptyList(),
    val inequalities: List<IneqJson> = emptyList(),
    val dots: List<List<List<Int>>> = emptyList(),
    val cages: List<CageJson> = emptyList(),
    val equations: List<EqJson> = emptyList(),
    val diagonals: Boolean = false,
    val regions: List<List<List<Int>>> = emptyList(),
    val groups: List<List<List<Int>>> = emptyList(),
)

@Serializable
private data class IneqJson(val lo: List<Int>, val hi: List<Int>)

@Serializable
private data class CageJson(val cells: List<List<Int>>, val total: Int)

@Serializable
private data class EqJson(val operands: List<List<List<Int>>>, val total: List<List<Int>>)

@Serializable
private data class LabelJson(val text: String, val x: Int, val y: Int)

private fun p(a: List<Int>) = Pos(a[0], a[1])

object PuzzleLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(text: String): SuperPuzzle {
        val dto = json.decodeFromString<PuzzleJson>(text)
        require(dto.unassigned.isEmpty()) { "puzzle has unassigned givens: ${dto.unassigned}" }
        return SuperPuzzle(
            grids = dto.grids.map { g ->
                require(g.size == 9) { "grid ${g.id} size != 9" }
                GridDef(
                    id = g.id,
                    name = g.name,
                    variant = Variant.of(g.variant),
                    x = g.x,
                    y = g.y,
                    givens = g.givens.associate { p(it) to it[2] },
                    inequalities = g.inequalities.map { p(it.lo) to p(it.hi) },
                    dots = g.dots.map { p(it[0]) to p(it[1]) },
                    cages = g.cages.map { Cage(it.cells.map(::p).toSet(), it.total) },
                    equations = g.equations.map { e ->
                        Equation(e.operands.map { op -> op.map(::p) }, e.total.map(::p))
                    },
                    diagonals = g.diagonals,
                    regions = g.regions.map { r -> r.map(::p).toSet() },
                    groups = g.groups.map { r -> r.map(::p).toSet() },
                )
            },
            labels = dto.labels.map { BoardLabel(it.text, it.x, it.y) },
        )
    }

    /** solution.json (emitted by :core:solveSuper) -> cell values by position. */
    fun loadSolution(text: String): Map<Pos, Int> {
        val obj = json.decodeFromString<SolutionJson>(text)
        return obj.cells.associate { p(it) to it[2] }
    }

    /** Display name stored in a puzzle file's meta section. */
    fun readName(text: String): String = try {
        json.decodeFromString<PuzzleJson>(text).meta.name
    } catch (_: Exception) {
        "Imported puzzle"
    }
}

@Serializable
private data class SolutionJson(val cells: List<List<Int>>)

/** Serialize back to the puzzle.json schema (used for imported custom puzzles). */
fun SuperPuzzle.saveToJson(puzzleName: String = "Imported puzzle"): String {
    val dto = PuzzleJson(
        meta = MetaJson(puzzleName),
        grids = grids.map { g ->
            GridJson(
                id = g.id, name = g.name, variant = g.variant.name.lowercase(),
                x = g.x, y = g.y, size = 9,
                givens = g.givens.map { (p, v) -> listOf(p.x, p.y, v) },
                inequalities = g.inequalities.map { (lo, hi) ->
                    IneqJson(listOf(lo.x, lo.y), listOf(hi.x, hi.y))
                },
                dots = g.dots.map { (a, b) -> listOf(listOf(a.x, a.y), listOf(b.x, b.y)) },
                cages = g.cages.map { c ->
                    CageJson(c.cells.map { listOf(it.x, it.y) }, c.total)
                },
                equations = g.equations.map { e ->
                    EqJson(
                        e.operands.map { op -> op.map { listOf(it.x, it.y) } },
                        e.total.map { listOf(it.x, it.y) },
                    )
                },
                diagonals = g.diagonals,
                regions = g.regions.map { r -> r.map { listOf(it.x, it.y) } },
                groups = g.groups.map { r -> r.map { listOf(it.x, it.y) } },
            )
        },
        labels = labels.map { LabelJson(it.text, it.x, it.y) },
    )
    return Json.encodeToString(PuzzleJson.serializer(), dto)
}

/** solution.json round-trip for custom puzzles with known solutions. */
fun saveSolution(sol: Map<Pos, Int>): String {
    val dto = SolutionJson(sol.map { (p, v) -> listOf(p.x, p.y, v) })
    return Json.encodeToString(SolutionJson.serializer(), dto)
}


