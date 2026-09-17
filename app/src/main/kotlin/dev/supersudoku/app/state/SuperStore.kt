package dev.supersudoku.app.state

import android.content.Context
import dev.supersudoku.core.GridDef
import dev.supersudoku.core.MapDifficulty
import dev.supersudoku.core.Pos
import dev.supersudoku.core.PuzzleLoader
import dev.supersudoku.core.SuperPuzzle
import dev.supersudoku.core.isGridComplete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Super puzzle library (OpenSudoku-style level browser backing store).
 * Lists the bundled Carykh puzzle plus every imported custom super puzzle,
 * each with user-only progress (givens excluded).
 */
object SuperStore {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class BoardSave(val values: List<Int>, val notes: List<Int>)

    data class SuperEntry(
        /** "bundled" or "custom:<id>". */
        val key: String,
        val title: String,
        val subtitle: String,
        val gridCount: Int,
        val userFilled: Int,
        val fillable: Int,
        val doneGrids: Int,
        val solved: Boolean,
        val hasSave: Boolean,
    )

    data class GridStat(
        val grid: GridDef,
        val userFilled: Int,
        val fillable: Int,
        val solved: Boolean,
    )

    data class PuzzleDetail(
        val puzzle: SuperPuzzle,
        val saveName: String,
        val grids: List<GridStat>,
        val userFilled: Int,
        val fillable: Int,
    )

    private fun saveFile(context: Context, saveName: String) = File(context.filesDir, saveName)

    private fun readSaveValues(context: Context, saveName: String): IntArray? {
        val f = saveFile(context, saveName)
        if (!f.exists()) return null
        return runCatching {
            val s = json.decodeFromString<BoardSave>(f.readText())
            s.values.toIntArray()
        }.getOrNull()
    }

    /** Compute user-only stats for a puzzle + optional saved values. */
    private fun statsFor(puzzle: SuperPuzzle, values: IntArray?): PuzzleDetail {
        val (w, h) = puzzle.boardSize()
        // Union of grid cells (overlaps counted once).
        val cells = puzzle.grids.flatMap { it.cells() }.toSet()
        // Given positions per board index.
        val givenIdx = HashSet<Int>()
        for (g in puzzle.grids) for ((pos, _) in g.givens) {
            if (pos.x in 0 until w && pos.y in 0 until h) givenIdx.add(pos.y * w + pos.x)
        }
        fun valueAt(x: Int, y: Int): Int {
            if (x !in 0 until w || y !in 0 until h) return 0
            return values?.getOrNull(y * w + x) ?: 0
        }
        var fillable = 0
        var filled = 0
        for (c in cells) {
            if (c.x !in 0 until w || c.y !in 0 until h) continue
            if (c.y * w + c.x in givenIdx) continue
            fillable++
            if (valueAt(c.x, c.y) != 0) filled++
        }
        val gridStats = puzzle.grids.map { g ->
            var gFillable = 0
            var gFilled = 0
            for (dy in 0 until 9) for (dx in 0 until 9) {
                val x = g.x + dx
                val y = g.y + dy
                if (x !in 0 until w || y !in 0 until h) continue
                if (y * w + x in givenIdx) continue
                gFillable++
                if (valueAt(x, y) != 0) gFilled++
            }
            val solved = if (values != null) {
                isGridComplete(g) { p -> valueAt(p.x, p.y) }
            } else false
            GridStat(g, gFilled, gFillable, solved)
        }
        return PuzzleDetail(puzzle, "", gridStats, filled, fillable)
    }

    suspend fun list(context: Context): List<SuperEntry> = withContext(Dispatchers.IO) {
        val out = ArrayList<SuperEntry>()
        // Bundled
        runCatching {
            val text = context.assets.open("puzzle.json").bufferedReader().use { it.readText() }
            val puzzle = PuzzleLoader.load(text)
            val values = readSaveValues(context, "super_save.json")
            val detail = statsFor(puzzle, values)
            val done = detail.grids.count { it.solved }
            out.add(
                SuperEntry(
                    key = "bundled",
                    title = "Carykh's Super Sudoku",
                    subtitle = "normal · >sudoku< · addition · killer · consecutive · X · strange boxes · offset",
                    gridCount = puzzle.grids.size,
                    userFilled = detail.userFilled,
                    fillable = detail.fillable,
                    doneGrids = done,
                    solved = done == puzzle.grids.size && puzzle.grids.isNotEmpty(),
                    hasSave = values != null,
                )
            )
        }
        // Customs
        val customs = CustomStore.list(context)
        for (e in customs) {
            runCatching {
                val loaded = CustomStore.load(context, e.id) ?: return@runCatching
                val (puzzle, _) = loaded
                val values = readSaveValues(context, "custom_${e.id}.json")
                val detail = statsFor(puzzle, values)
                val done = detail.grids.count { it.solved }
                out.add(
                    SuperEntry(
                        key = "custom:${e.id}",
                        title = e.name,
                        subtitle = "${puzzle.grids.size} grid${if (puzzle.grids.size == 1) "" else "s"}" +
                            if (e.hasSolution) " · solution known" else "",
                        gridCount = puzzle.grids.size,
                        userFilled = detail.userFilled,
                        fillable = detail.fillable,
                        doneGrids = done,
                        solved = done == puzzle.grids.size && puzzle.grids.isNotEmpty(),
                        hasSave = values != null,
                    )
                )
            }
        }
        out
    }

    /** Full per-grid detail for one library key (for the level rows + random). */
    suspend fun detail(context: Context, key: String): PuzzleDetail? = withContext(Dispatchers.IO) {
        if (key == "bundled") {
            runCatching {
                val text = context.assets.open("puzzle.json").bufferedReader().use { it.readText() }
                val puzzle = PuzzleLoader.load(text)
                val values = readSaveValues(context, "super_save.json")
                statsFor(puzzle, values).copy(saveName = "super_save.json")
            }.getOrNull()
        } else if (key.startsWith("map:")) {
            runCatching {
                val loaded = loadMap(context, key.removePrefix("map:")) ?: return@runCatching null
                val (puzzle, _, saveName) = loaded
                statsFor(puzzle, readSaveValues(context, saveName)).copy(saveName = saveName)
            }.getOrNull()
        } else if (key.startsWith("custom:")) {
            runCatching {
                val (puzzle, _) = CustomStore.load(context, key.removePrefix("custom:")) ?: return@runCatching null
                val id = key.removePrefix("custom:")
                val values = readSaveValues(context, "custom_$id.json")
                statsFor(puzzle, values).copy(saveName = "custom_$id.json")
            }.getOrNull()
        } else null
    }

    // ---------- Generated maps ("new maps out of it") ----------

    @Serializable
    private data class MapFile(
        val templateKey: String,
        /** Givens as [x, y, value]; always a subset of the template solution. */
        val givens: List<List<Int>>,
        val removed: Int,
        val createdAt: Long,
        /** Stable display number (Map #n), assigned at creation. */
        val number: Int = 0,
        /** Generation difficulty name ([MapDifficulty]). */
        val difficulty: String = "MEDIUM",
    )

    data class MapEntry(
        val id: String,
        val name: String,
        val templateKey: String,
        val removed: Int,
        val userFilled: Int,
        val fillable: Int,
        val doneGrids: Int,
        val gridCount: Int,
        val solved: Boolean,
        val hasSave: Boolean,
        val createdAt: Long,
        val difficultyLabel: String,
    )

    private fun mapsDir(context: Context) = File(context.filesDir, "supermaps").also { it.mkdirs() }
    private fun mapFile(context: Context, id: String) = File(mapsDir(context), "$id.json")
    private fun currentFile(context: Context) = File(context.filesDir, "super_current_map.txt")

    fun mapSaveName(id: String) = "supermap_$id.json"

    /** Cheap count of generated maps (for the home folder row). */
    fun mapsCount(context: Context): Int =
        runCatching {
            mapsDir(context).listFiles { f -> f.name.endsWith(".json") }?.size ?: 0
        }.getOrDefault(0)

    /** Display name for one map ("Map #n"), or null when missing. */
    fun mapName(context: Context, id: String): String? = runCatching {
        val m = json.decodeFromString<MapFile>(mapFile(context, id).readText())
        val n = if (m.number > 0) m.number else 1
        "Map #$n"
    }.getOrNull()

    /** Id of the current map, or null for the original bundled givens. */
    fun currentMapId(context: Context): String? = runCatching {
        val f = currentFile(context)
        if (!f.exists()) null else f.readText().trim().ifBlank { null }
    }.getOrNull()

    fun setCurrentMapId(context: Context, id: String?) {
        val f = currentFile(context)
        if (id == null) f.delete()
        else f.writeText(id)
    }

    private fun mapGivens(m: MapFile): Map<Pos, Int> =
        m.givens.associate { Pos(it[0], it[1]) to it[2] }

    private fun puzzleWithGivens(template: SuperPuzzle, givens: Map<Pos, Int>): SuperPuzzle =
        template.copy(grids = template.grids.map { g ->
            g.copy(givens = g.givens.mapNotNull { (p, _) ->
                givens[p]?.let { p to it }
            }.toMap())
        })

    /**
     * Load a generated map: puzzle with the map's givens, the template's
     * known solution (shared by all maps of a template), and its save name.
     */
    suspend fun loadMap(context: Context, id: String): Triple<SuperPuzzle, Map<Pos, Int>?, String>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val m = json.decodeFromString<MapFile>(mapFile(context, id).readText())
                if (m.templateKey != "bundled") return@runCatching null
                val text = context.assets.open("puzzle.json").bufferedReader().use { it.readText() }
                val template = PuzzleLoader.load(text)
                val sol = runCatching {
                    PuzzleLoader.loadSolution(
                        context.assets.open("solution.json").bufferedReader().use { it.readText() }
                    )
                }.getOrNull()
                Triple(puzzleWithGivens(template, mapGivens(m)), sol, mapSaveName(id))
            }.getOrNull()
        }

    suspend fun listMaps(context: Context): List<MapEntry> = withContext(Dispatchers.IO) {
        mapsDir(context).listFiles { f -> f.name.endsWith(".json") }
            ?.sortedBy { it.lastModified() }
            ?.mapNotNull { f ->
                runCatching {
                    val m = json.decodeFromString<MapFile>(f.readText())
                    if (m.templateKey != "bundled") return@runCatching null
                    val id = f.nameWithoutExtension
                    val text = context.assets.open("puzzle.json").bufferedReader().use { it.readText() }
                    val template = PuzzleLoader.load(text)
                    val puzzle = puzzleWithGivens(template, mapGivens(m))
                    val values = readSaveValues(context, mapSaveName(id))
                    val detail = statsFor(puzzle, values)
                    val done = detail.grids.count { it.solved }
                    MapEntry(
                        id = id,
                        name = "Map #${if (m.number > 0) m.number else 1}",
                        templateKey = m.templateKey,
                        removed = m.removed,
                        userFilled = detail.userFilled,
                        fillable = detail.fillable,
                        doneGrids = done,
                        gridCount = puzzle.grids.size,
                        solved = done == puzzle.grids.size && puzzle.grids.isNotEmpty(),
                        hasSave = values != null,
                        createdAt = m.createdAt,
                        difficultyLabel = runCatching {
                            MapDifficulty.valueOf(m.difficulty).label
                        }.getOrDefault("Medium"),
                    )
                }.getOrNull()
            } ?: emptyList()
    }

    /**
     * Mint a fresh map out of the bundled template (slow: call off the main
     * thread). Returns null when digging removed nothing (caller should offer
     * Restart instead of a duplicate map).
     */
    suspend fun createMap(
        context: Context,
        difficulty: MapDifficulty = MapDifficulty.MEDIUM,
    ): MapEntry? = withContext(Dispatchers.Default) {
        val text = context.assets.open("puzzle.json").bufferedReader().use { it.readText() }
        val template = PuzzleLoader.load(text)
        val sol = runCatching {
            PuzzleLoader.loadSolution(
                context.assets.open("solution.json").bufferedReader().use { it.readText() }
            )
        }.getOrNull() ?: return@withContext null
        val out = dev.supersudoku.core.SuperGenerator.generate(
            template = template,
            solution = sol,
            random = kotlin.random.Random(System.currentTimeMillis()),
            maxRemove = difficulty.maxRemove,
            timeBudgetMs = difficulty.timeBudgetMs,
        )
        if (out.removedCount == 0) return@withContext null
        val id = "m${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()
        val number = withContext(Dispatchers.IO) {
            val n = (mapsDir(context).listFiles { f -> f.name.endsWith(".json") }?.size ?: 0) + 1
            mapFile(context, id).writeText(
                json.encodeToString(
                    MapFile(
                        templateKey = "bundled",
                        givens = out.givens.map { (p, v) -> listOf(p.x, p.y, v) },
                        removed = out.removedCount,
                        createdAt = now,
                        number = n,
                        difficulty = difficulty.name,
                    )
                )
            )
            setCurrentMapId(context, id)
            n
        }
        val puzzle = puzzleWithGivens(template, out.givens)
        val detail = statsFor(puzzle, null)
        MapEntry(
            id = id,
            name = "Map #$number",
            templateKey = "bundled",
            removed = out.removedCount,
            userFilled = 0,
            fillable = detail.fillable,
            doneGrids = 0,
            gridCount = puzzle.grids.size,
            solved = false,
            hasSave = false,
            createdAt = now,
            difficultyLabel = difficulty.label,
        )
    }

    suspend fun deleteMap(context: Context, id: String) = withContext(Dispatchers.IO) {
        mapFile(context, id).delete()
        File(context.filesDir, mapSaveName(id)).delete()
        if (currentMapId(context) == id) setCurrentMapId(context, null)
    }
}
