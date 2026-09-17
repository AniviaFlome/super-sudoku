package dev.supersudoku.app.state

import android.content.Context
import dev.supersudoku.core.MapDifficulty
import dev.supersudoku.core.Pos
import dev.supersudoku.core.SuperGenerator
import dev.supersudoku.core.isGridComplete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random

/**
 * Generated games per standalone variant (mirrors PracticeStore entries and
 * SuperStore maps): each game is a fresh 9x9 given-set dug from the ring
 * solution, with its own progress. The minted Originals stay untouched.
 */
object VariantGames {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class GameFile(
        val gridId: String,
        val givens: List<Int>,
        val values: List<Int>,
        val notes: List<Int>,
        val corner: List<Int> = emptyList(),
        val difficulty: String = "MEDIUM",
        val createdAt: Long,
        /** Elapsed play seconds; defaults zero so older entries still load. */
        val seconds: Long = 0L,
    )

    data class Entry(
        val id: String,
        val gridId: String,
        val name: String,
        val difficultyLabel: String,
        val userFilled: Int,
        val fillable: Int,
        val solved: Boolean,
        val createdAt: Long,
    )

    data class Loaded(
        val id: String,
        val gridId: String,
        val givens: IntArray,
        val values: IntArray,
        val notes: IntArray,
        val corner: IntArray,
        val difficulty: MapDifficulty,
        val seconds: Long,
    )

    /** Mint targets per variant (same calibration as standalone.json). */
    private fun baseTarget(gridId: String): Int = when (gridId) {
        "classic" -> 32
        "futoshiki" -> 10
        "addition" -> 30
        "killer" -> 36
        "kropki" -> 14
        "sudoku_x" -> 32
        "irregular" -> 32
        "disjoint" -> 32
        else -> 30
    }

    fun targetFor(gridId: String, difficulty: MapDifficulty): Int {
        val base = baseTarget(gridId)
        // Template-kept variants (futo/kropki) generate around their own count.
        if (gridId == "futoshiki" || gridId == "kropki") return base
        return when (difficulty) {
            MapDifficulty.EASY -> base + 8
            MapDifficulty.MEDIUM -> base
            MapDifficulty.HARD -> base - 8
        }.coerceIn(24, 60)
    }

    private fun dir(context: Context) = File(context.filesDir, "variantgames").also { it.mkdirs() }
    private fun file(context: Context, id: String) = File(dir(context), "$id.json")

    private fun cornerOf(g: GameFile): IntArray =
        if (g.corner.size == 81) g.corner.toIntArray() else IntArray(81)

    private fun toEntry(id: String, g: GameFile, grid: dev.supersudoku.core.GridDef, number: Int): Entry {
        val givens = g.givens.count { it != 0 }
        val userFilled = g.values.indices.count { i -> g.values[i] != 0 && g.givens[i] == 0 }
        val solved = isGridComplete(grid) { p -> g.values[p.y * 9 + p.x] }
        val diffLabel = runCatching { MapDifficulty.valueOf(g.difficulty).label }.getOrDefault("Medium")
        return Entry(id, g.gridId, "$diffLabel #$number", diffLabel, userFilled, 81 - givens, solved, g.createdAt)
    }

    suspend fun list(context: Context, gridId: String): List<Entry> = withContext(Dispatchers.IO) {
        // Local grid needed only for the solved check; load once per call.
        val loaded = StandaloneStore.load(context, gridId)
        val grid = loaded?.grid ?: return@withContext emptyList()
        dir(context).listFiles { f -> f.name.endsWith(".json") }
            ?.mapNotNull { f ->
                runCatching {
                    val g = json.decodeFromString<GameFile>(f.readText())
                    if (g.gridId != gridId) return@runCatching null
                    if (g.givens.size != 81 || g.values.size != 81 || g.notes.size != 81) {
                        return@runCatching null
                    }
                    f.nameWithoutExtension to g
                }.getOrNull()
            }
            ?.sortedBy { it.second.createdAt }
            ?.mapIndexed { index, (id, g) -> toEntry(id, g, grid, index + 1) }
            ?: emptyList()
    }

    suspend fun count(context: Context, gridId: String): Int = withContext(Dispatchers.IO) {
        dir(context).listFiles { f -> f.name.endsWith(".json") }
            ?.count { f ->
                runCatching { json.decodeFromString<GameFile>(f.readText()).gridId == gridId }
                    .getOrDefault(false)
            } ?: 0
    }

    /**
     * Generate + persist a fresh game for a variant (slow: call off the main
     * thread). Returns null when no valid game comes out.
     *
     * Futoshiki and Kropki keep template givens (already unique): Easy adds
     * extra solution cells (uniqueness is preserved by construction, no
     * solver needed), Medium reuses the template set, Hard digs a few away.
     * All other variants dig down from the full solution to the target.
     */
    suspend fun create(
        context: Context,
        gridId: String,
        difficulty: MapDifficulty,
    ): Loaded? = withContext(Dispatchers.Default) {
        val template = StandaloneStore.load(context, gridId) ?: return@withContext null
        val solutionMap = HashMap<Pos, Int>()
        for (i in 0 until 81) {
            val v = template.solution[i]
            if (v == 0) return@withContext null
            solutionMap[Pos(i % 9, i / 9)] = v
        }
        val bare = template.grid.copy(givens = emptyMap())
        // Template givens in local 9x9 coords.
        val templateLocal = HashMap<Pos, Int>()
        for (i in 0 until 81) {
            val v = template.givens[i]
            if (v != 0) templateLocal[Pos(i % 9, i / 9)] = v
        }
        val givens: Map<Pos, Int> = if (gridId == "futoshiki" || gridId == "kropki") {
            when (difficulty) {
                MapDifficulty.EASY -> {
                    // Superset of unique givens stays unique: sprinkle extras.
                    val extra = solutionMap.keys
                        .filter { it !in templateLocal }
                        .shuffled(Random(System.currentTimeMillis()))
                        .take(8)
                    HashMap(templateLocal).also { m ->
                        for (p in extra) m[p] = solutionMap.getValue(p)
                    }
                }
                MapDifficulty.MEDIUM -> HashMap(templateLocal)
                MapDifficulty.HARD -> {
                    val order = templateLocal.keys.shuffled(Random(System.currentTimeMillis()))
                    val dug = HashMap(templateLocal)
                    val deadline = System.currentTimeMillis() + difficulty.timeBudgetMs
                    for (p in order) {
                        if (dug.size <= templateLocal.size - 4) break
                        if (System.currentTimeMillis() > deadline) break
                        val v = dug.remove(p) ?: continue
                        if (SuperGenerator.countSolutions(listOf(bare), dug, 2) != 1) {
                            dug[p] = v
                        }
                    }
                    dug
                }
            }
        } else {
            val target = targetFor(gridId, difficulty)
            val dug = SuperGenerator.digDown(
                grids = listOf(bare),
                solution = solutionMap,
                targetGivens = target,
                random = Random(System.currentTimeMillis()),
                timeBudgetMs = difficulty.timeBudgetMs,
            )
            dug.givens
        }
        if (SuperGenerator.countSolutions(listOf(bare), givens, 2) != 1) {
            return@withContext null
        }
        val givens81 = IntArray(81) { i -> givens[Pos(i % 9, i / 9)] ?: 0 }
        val id = "v${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            file(context, id).writeText(
                json.encodeToString(
                    GameFile(
                        gridId = gridId,
                        givens = givens81.toList(),
                        values = givens81.toList(),
                        notes = List(81) { 0 },
                        corner = List(81) { 0 },
                        difficulty = difficulty.name,
                        createdAt = now,
                    )
                )
            )
        }
        Loaded(id, gridId, givens81, givens81.copyOf(), IntArray(81), IntArray(81), difficulty, 0L)
    }

    suspend fun load(context: Context, id: String): Loaded? = withContext(Dispatchers.IO) {
        val f = file(context, id)
        if (!f.exists()) return@withContext null
        runCatching {
            val g = json.decodeFromString<GameFile>(f.readText())
            if (g.givens.size != 81 || g.values.size != 81 || g.notes.size != 81) {
                return@runCatching null
            }
            Loaded(
                id, g.gridId, g.givens.toIntArray(), g.values.toIntArray(),
                g.notes.toIntArray(), cornerOf(g),
                runCatching { MapDifficulty.valueOf(g.difficulty) }.getOrDefault(MapDifficulty.MEDIUM),
                g.seconds,
            )
        }.getOrNull()
    }

    suspend fun save(
        context: Context,
        id: String,
        values: IntArray,
        notes: IntArray,
        corner: IntArray,
        seconds: Long = 0L,
    ) = withContext(Dispatchers.IO) {
        val f = file(context, id)
        if (!f.exists()) return@withContext
        runCatching {
            val cur = json.decodeFromString<GameFile>(f.readText())
            f.writeText(
                json.encodeToString(
                    cur.copy(
                        values = values.toList(), notes = notes.toList(),
                        corner = corner.toList(), seconds = seconds,
                    )
                )
            )
        }
    }

    suspend fun delete(context: Context, id: String) = withContext(Dispatchers.IO) {
        file(context, id).delete()
    }
}
