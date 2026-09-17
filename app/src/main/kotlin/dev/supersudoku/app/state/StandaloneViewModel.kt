package dev.supersudoku.app.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.supersudoku.core.GridDef
import dev.supersudoku.core.PlayBoard
import dev.supersudoku.core.Pos
import dev.supersudoku.core.UndoStack
import dev.supersudoku.core.validateGrid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Extra surface shared by single-board (9x9) view-models. */
interface SingleBoardVm : BoardPlayground {
    val boardVersion: Int
    val ready: Boolean
    val grid: GridDef
    fun canUndo(): Boolean
    fun canRedo(): Boolean
    override fun undo(): Boolean
    override fun redo(): Boolean
    fun flaggedCells(mode: MistakeMode): Set<Pos>
    fun remainingCounts(): IntArray
}

/** Standalone variant game: independent 9x9 with its own givens and save. */
class StandaloneViewModel(app: Application) : AndroidViewModel(app), SingleBoardVm {
    override val board = PlayBoard(9, 9)
    private val undo = UndoStack(board)
    override val histories: UndoStack get() = undo

    override var boardVersion by mutableStateOf(0)
        private set
    /** Elapsed play seconds for the loaded game (ticks while its screen is open). */
    override var playSeconds by mutableStateOf(0L)
        private set

    /** One tick from the screen clock. No save here; seconds persist via touch()/flush(). */
    override fun tickSecond() {
        playSeconds++
    }

    /** Persist immediately (screen going away); cancels the debounced save. */
    override fun flush() {
        saveJob?.cancel()
        val game = gameId
        if (game != null) {
            val values = board.value.copyOf()
            val notes = board.notes.copyOf()
            val corner = board.corner.copyOf()
            val seconds = playSeconds
            viewModelScope.launch {
                VariantGames.save(getApplication(), game, values, notes, corner, seconds)
            }
            return
        }
        if (gridId == null || saveName.isEmpty()) return
        val values = board.value.copyOf()
        val notes = board.notes.copyOf()
        val corner = board.corner.copyOf()
        val seconds = playSeconds
        val name = saveName
        viewModelScope.launch {
            SaveStore.saveBoard(getApplication(), name, values, notes, corner, seconds)
        }
    }
    override var selection by mutableStateOf<Pos?>(null)
    override var notesMode by mutableStateOf(false)
    override var cornerMode by mutableStateOf(false)
    override var armedDigit by mutableStateOf<Int?>(null)

    /** Peer cells for auto-clearing pencil marks: row + column + 3x3 box. */
    override fun peerCells(x: Int, y: Int): Sequence<Pos> = sequence {
        for (i in 0 until 9) if (i != x) yield(Pos(i, y))
        for (j in 0 until 9) if (j != y) yield(Pos(x, j))
        val bx = x / 3 * 3
        val by = y / 3 * 3
        for (dy in 0 until 3) for (dx in 0 until 3) {
            val p = Pos(bx + dx, by + dy)
            if (p.x != x || p.y != y) yield(p)
        }
    }

    override var ready by mutableStateOf(false)
        private set
    override lateinit var grid: GridDef
        private set

    var title by mutableStateOf("")
        private set

    /** Local 9x9 solution (all zeros when unknown). */
    private var solution = IntArray(81)

    private var gridId: String? = null
    private var gameId: String? = null
    private var saveName: String = ""
    private var saveJob: Job? = null

    fun ensureGrid(id: String) {
        if (gridId != null || gameId != null) return
        gridId = id
        viewModelScope.launch {
            val loaded = StandaloneStore.load(getApplication(), id)
            if (loaded != null) {
                grid = loaded.grid
                title = loaded.grid.name
                solution = loaded.solution
                saveName = loaded.saveName
                loaded.givens.forEachIndexed { i, v ->
                    if (v != 0) {
                        board.value[i] = v
                        board.given[i] = true
                    }
                }
                SaveStore.loadBoard(getApplication(), saveName, 9, 9)?.let { data ->
                    data.values.copyInto(board.value)
                    data.notes.copyInto(board.notes)
                    data.corner.copyInto(board.corner)
                    playSeconds = data.seconds
                }
                // Re-assert givens (repairs stale saves).
                loaded.givens.forEachIndexed { i, v ->
                    if (v != 0) {
                        board.value[i] = v
                        board.given[i] = true
                    }
                }
            }
            ready = true
            boardVersion++
        }
    }

    override fun touch() {
        boardVersion++
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            val game = gameId
            if (game != null) {
                VariantGames.save(
                    getApplication(), game, board.value, board.notes, board.corner, playSeconds
                )
            } else if (gridId != null && saveName.isNotEmpty()) {
                SaveStore.saveBoard(
                    getApplication(), saveName, board.value, board.notes, board.corner, playSeconds
                )
            }
        }
    }

    /** Load a generated variant game (own givens + progress). */
    fun ensureGame(id: String) {
        if (gridId != null || gameId != null) return
        gameId = id
        viewModelScope.launch {
            val entry = VariantGames.load(getApplication(), id)
            val template = entry?.let { StandaloneStore.load(getApplication(), it.gridId) }
            if (entry != null && template != null) {
                grid = template.grid
                title = template.grid.name
                solution = template.solution
                entry.givens.forEachIndexed { i, v ->
                    if (v != 0) {
                        board.value[i] = v
                        board.given[i] = true
                    }
                }
                entry.values.forEachIndexed { i, v ->
                    if (!board.given[i]) board.value[i] = v
                }
                entry.notes.copyInto(board.notes)
                entry.corner.copyInto(board.corner)
                playSeconds = entry.seconds
            }
            ready = true
            boardVersion++
        }
    }

    override fun canUndo() = undo.canUndo()
    override fun canRedo() = undo.canRedo()
    override fun undo() = undo.undo().also { if (it) touch() }
    override fun redo() = undo.redo().also { if (it) touch() }

    /** Remaining counts per digit. Givens don't count as "filled". */
    override fun remainingCounts(): IntArray {
        val givenCount = IntArray(10)
        val userCount = IntArray(10)
        for (i in 0 until 81) {
            val v = board.value[i]
            if (v in 1..9) {
                if (board.given[i]) givenCount[v]++ else userCount[v]++
            }
        }
        return IntArray(10) { d -> if (d == 0) 0 else 9 - givenCount[d] - userCount[d] }
    }

    /** Cells whose entry differs from the known solution. */
    fun wrongCells(): Set<Pos> {
        val out = HashSet<Pos>()
        for (i in 0 until 81) {
            val v = board.value[i]
            if (v != 0 && !board.given[i] && solution[i] != 0 && solution[i] != v) {
                out.add(Pos(i % 9, i / 9))
            }
        }
        return out
    }

    override fun flaggedCells(mode: MistakeMode): Set<Pos> = when (mode) {
        MistakeMode.OFF -> emptySet()
        MistakeMode.CONFLICTS -> validateGrid(grid) { board.get(it) }.flatMap { it.cells }.toSet()
        MistakeMode.WRONG -> wrongCells()
    }
}
