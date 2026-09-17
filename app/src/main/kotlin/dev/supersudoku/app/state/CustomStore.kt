package dev.supersudoku.app.state

import android.content.Context
import dev.supersudoku.core.Pos
import dev.supersudoku.core.PuzzleLoader
import dev.supersudoku.core.SuperPuzzle
import dev.supersudoku.core.saveSolution
import dev.supersudoku.core.saveToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Imported custom puzzles, stored as our puzzle.json schema + optional solution. */
object CustomStore {
    data class Entry(val id: String, val name: String, val gridCount: Int, val hasSolution: Boolean)

    private fun dir(context: Context) = File(context.filesDir, "custom").also { it.mkdirs() }
    private fun puzzleFile(context: Context, id: String) = File(dir(context), "$id.json")
    private fun solutionFile(context: Context, id: String) = File(dir(context), "$id.solution.json")

    fun list(context: Context): List<Entry> {
        val d = dir(context)
        if (!d.exists()) return emptyList()
        return d.listFiles { f -> f.name.endsWith(".json") && !f.name.endsWith(".solution.json") }
            ?.mapNotNull { f ->
                runCatching {
                    val text = f.readText()
                    val puzzle = PuzzleLoader.load(text)
                    val id = f.nameWithoutExtension
                    Entry(
                        id = id,
                        name = PuzzleLoader.readName(text),
                        gridCount = puzzle.grids.size,
                        hasSolution = solutionFile(context, id).exists(),
                    )
                }.getOrNull()
            }
            ?.sortedBy { it.id }
            ?: emptyList()
    }

    suspend fun save(
        context: Context,
        puzzle: SuperPuzzle,
        name: String,
        solution: Map<Pos, Int>?,
    ): String = withContext(Dispatchers.IO) {
        val base = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(24)
            .ifBlank { "puzzle" }
        var id = base
        var n = 1
        while (puzzleFile(context, id).exists()) {
            n++
            id = "$base-$n"
        }
        puzzleFile(context, id).writeText(puzzle.saveToJson(name))
        if (solution != null) {
            solutionFile(context, id).writeText(saveSolution(solution))
        }
        id
    }

    suspend fun load(context: Context, id: String): Pair<SuperPuzzle, Map<Pos, Int>?>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val puzzle = PuzzleLoader.load(puzzleFile(context, id).readText())
                val solFile = solutionFile(context, id)
                val sol = if (solFile.exists()) {
                    PuzzleLoader.loadSolution(solFile.readText())
                } else {
                    null
                }
                puzzle to sol
            }.getOrNull()
        }

    suspend fun delete(context: Context, id: String) = withContext(Dispatchers.IO) {
        puzzleFile(context, id).delete()
        solutionFile(context, id).delete()
        // saves live next to filesDir root
        File(context.filesDir, "custom_$id.json").delete()
    }
}
