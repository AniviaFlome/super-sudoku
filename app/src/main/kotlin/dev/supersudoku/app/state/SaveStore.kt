package dev.supersudoku.app.state

import android.content.Context
import dev.supersudoku.core.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Plain-JSON save files in filesDir (no permissions needed). */
object SaveStore {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SuperSave(
        val values: List<Int>,
        val notes: List<Int>,
        /** Corner marks; defaults empty so pre-dual-mark saves still load. */
        val corner: List<Int> = emptyList(),
        /** Elapsed play seconds; defaults zero so older saves still load. */
        val seconds: Long = 0L,
    )

    @Serializable
    private data class PracticeSave(
        val difficulty: String,
        val puzzle: List<Int>,
        val values: List<Int>,
        val notes: List<Int>,
    )

    private fun boardFile(context: Context, name: String) = File(context.filesDir, name)

    suspend fun saveBoard(
        context: Context,
        name: String,
        values: IntArray,
        notes: IntArray,
        corner: IntArray = IntArray(values.size),
        seconds: Long = 0L,
    ) = withContext(Dispatchers.IO) {
        boardFile(context, name).writeText(
            json.encodeToString(SuperSave(values.toList(), notes.toList(), corner.toList(), seconds))
        )
    }

    data class BoardData(val values: IntArray, val notes: IntArray, val corner: IntArray, val seconds: Long)

    suspend fun loadBoard(
        context: Context,
        name: String,
        cols: Int,
        rows: Int,
    ): BoardData? = withContext(Dispatchers.IO) {
        val f = boardFile(context, name)
        if (!f.exists()) return@withContext null
        runCatching {
            val s = json.decodeFromString<SuperSave>(f.readText())
            if (s.values.size != cols * rows || s.notes.size != cols * rows) {
                return@runCatching null
            }
            val corner = if (s.corner.size == cols * rows) s.corner.toIntArray()
            else IntArray(cols * rows)
            BoardData(s.values.toIntArray(), s.notes.toIntArray(), corner, s.seconds)
        }.getOrNull()
    }

    fun hasBoard(context: Context, name: String) = boardFile(context, name).exists()

    private fun practiceFile(context: Context) = File(context.filesDir, "practice_save.json")

    fun hasPractice(context: Context) = practiceFile(context).exists()

    /** Saved practice difficulty, if any (cheap metadata read, no board needed). */
    fun practiceDifficulty(context: Context): Difficulty? = try {
        val f = practiceFile(context)
        if (!f.exists()) null
        else Difficulty.valueOf(json.decodeFromString<PracticeSave>(f.readText()).difficulty)
    } catch (_: Exception) {
        null
    }

    suspend fun savePractice(
        context: Context,
        difficulty: Difficulty,
        puzzle: IntArray,
        values: IntArray,
        notes: IntArray,
    ) = withContext(Dispatchers.IO) {
        practiceFile(context).writeText(
            json.encodeToString(
                PracticeSave(difficulty.name, puzzle.toList(), values.toList(), notes.toList())
            )
        )
    }

    data class PracticeLoaded(
        val difficulty: Difficulty,
        val puzzle: IntArray,
        val values: IntArray,
        val notes: IntArray,
    )

    suspend fun loadPractice(context: Context): PracticeLoaded? = withContext(Dispatchers.IO) {
        val f = practiceFile(context)
        if (!f.exists()) return@withContext null
        runCatching {
            val s = json.decodeFromString<PracticeSave>(f.readText())
            if (s.puzzle.size != 81 || s.values.size != 81 || s.notes.size != 81) {
                return@runCatching null
            }
            PracticeLoaded(
                Difficulty.valueOf(s.difficulty),
                s.puzzle.toIntArray(), s.values.toIntArray(), s.notes.toIntArray(),
            )
        }.getOrNull()
    }
}
