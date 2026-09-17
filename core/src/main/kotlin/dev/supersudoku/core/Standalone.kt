package dev.supersudoku.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Standalone 9x9 givens per variant grid (emitted by :core:mintStandalone). */
object StandaloneLoader {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Doc(val grids: List<Entry>)

    @Serializable
    private data class Entry(val id: String, val givens: List<List<Int>> = emptyList())

    /** Grid id -> givens (global canvas coords, matching puzzle.json/solution.json). */
    fun load(text: String): Map<String, Map<Pos, Int>> {
        val dto = json.decodeFromString<Doc>(text)
        return dto.grids.associate { e ->
            e.id to e.givens.associate { Pos(it[0], it[1]) to it[2] }
        }
    }
}
