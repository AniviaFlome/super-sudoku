package dev.supersudoku.app.state

import android.content.Context
import dev.supersudoku.core.ClassicGenerator
import dev.supersudoku.core.ClassicSolver
import dev.supersudoku.core.Difficulty
import dev.supersudoku.core.GridDef
import dev.supersudoku.core.Variant
import dev.supersudoku.core.isGridComplete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random

/**
 * Practice puzzle library (OpenSudoku-style): persisted generated puzzles
 * per difficulty that you can pick up individually, plus instant Random.
 */
object PracticeStore {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class EntryFile(
        val difficulty: String,
        val puzzle: List<Int>,
        val values: List<Int>,
        val notes: List<Int>,
        val createdAt: Long,
        /** Corner marks; defaults empty so older entries still load. */
        val corner: List<Int> = emptyList(),
        /** Elapsed play seconds; defaults zero so older entries still load. */
        val seconds: Long = 0L,
    )

    data class Entry(
        val id: String,
        val difficulty: Difficulty,
        /** User-filled cells only; givens are excluded. */
        val filledTotal: Int,
        val filledGivens: Int,
        val fillable: Int,
        val solved: Boolean,
        val createdAt: Long,
    )

    data class Loaded(
        val id: String,
        val difficulty: Difficulty,
        val puzzle: IntArray,
        val values: IntArray,
        val notes: IntArray,
        val corner: IntArray,
        val seconds: Long,
    )

    private val grid = GridDef("practice", "Practice", Variant.CLASSIC, 0, 0, emptyMap())

    private fun dir(context: Context) = File(context.filesDir, "practice").also { it.mkdirs() }
    private fun file(context: Context, id: String) = File(dir(context), "$id.json")

    private fun readEntry(context: Context, f: File): Entry? {
        return runCatching {
            val s = json.decodeFromString<EntryFile>(f.readText())
            if (s.puzzle.size != 81 || s.values.size != 81 || s.notes.size != 81) return@runCatching null
            val diff = Difficulty.valueOf(s.difficulty)
            val givens = s.puzzle.count { it != 0 }
            // User-only: a cell counts as filled only if the player entered it.
            val userFilled = s.values.indices.count { i -> s.values[i] != 0 && s.puzzle[i] == 0 }
            val complete = isGridComplete(grid) { p -> s.values[p.y * 9 + p.x] }
            Entry(
                id = f.nameWithoutExtension,
                difficulty = diff,
                filledTotal = userFilled,
                filledGivens = givens,
                fillable = 81 - givens,
                solved = complete,
                createdAt = s.createdAt,
            )
        }.getOrNull()
    }

    /** One-time migration of the legacy single practice save. */
    private suspend fun migrateLegacy(context: Context) = withContext(Dispatchers.IO) {
        val legacy = SaveStore.loadPractice(context) ?: return@withContext
        if ((dir(context).listFiles()?.isNotEmpty() == true)) {
            // entries already exist; drop the legacy file to avoid duplicates
            File(context.filesDir, "practice_save.json").delete()
            return@withContext
        }
        val id = "p${System.currentTimeMillis()}"
        file(context, id).writeText(
            json.encodeToString(
                EntryFile(
                    legacy.difficulty.name,
                    legacy.puzzle.toList(), legacy.values.toList(), legacy.notes.toList(),
                    System.currentTimeMillis(),
                ),
            ),
        )
        File(context.filesDir, "practice_save.json").delete()
    }

    suspend fun list(context: Context): List<Entry> = withContext(Dispatchers.IO) {
        migrateLegacy(context)
        dir(context).listFiles { f -> f.name.endsWith(".json") }
            ?.mapNotNull { readEntry(context, it) }
            ?.sortedBy { it.createdAt }
            ?: emptyList()
    }

    private fun cornerOf(s: EntryFile): IntArray =
        if (s.corner.size == 81) s.corner.toIntArray() else IntArray(81)

    /** Generate + persist a new puzzle (slow: call off the main thread). */
    suspend fun create(context: Context, difficulty: Difficulty): Loaded =
        withContext(Dispatchers.Default) {
            val puzzle = ClassicGenerator.generate(difficulty, Random(System.currentTimeMillis()))
            val id = "p${System.currentTimeMillis()}"
            file(context, id).writeText(
                json.encodeToString(
                    EntryFile(
                        difficulty.name, puzzle.toList(),
                        puzzle.toList(), List(81) { 0 },
                        System.currentTimeMillis(),
                    ),
                ),
            )
            Loaded(id, difficulty, puzzle, puzzle.copyOf(), IntArray(81), IntArray(81), 0L)
        }

    suspend fun load(context: Context, id: String): Loaded? = withContext(Dispatchers.IO) {
        val f = file(context, id)
        if (!f.exists()) return@withContext null
        runCatching {
            val s = json.decodeFromString<EntryFile>(f.readText())
            if (s.puzzle.size != 81 || s.values.size != 81 || s.notes.size != 81) {
                return@runCatching null
            }
            Loaded(
                id, Difficulty.valueOf(s.difficulty), s.puzzle.toIntArray(),
                s.values.toIntArray(), s.notes.toIntArray(), cornerOf(s), s.seconds,
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
            val cur = json.decodeFromString<EntryFile>(f.readText())
            f.writeText(
                json.encodeToString(
                    cur.copy(
                        values = values.toList(), notes = notes.toList(),
                        corner = corner.toList(), seconds = seconds,
                    )
                ),
            )
        }
    }

    suspend fun delete(context: Context, id: String) = withContext(Dispatchers.IO) {
        file(context, id).delete()
    }

    /** True when the stored puzzle is uniquely solvable (sanity for imports). */
    fun isUniquelySolvable(puzzle: IntArray): Boolean =
        ClassicSolver.countSolutions(puzzle.copyOf(), 2) == 1
}
