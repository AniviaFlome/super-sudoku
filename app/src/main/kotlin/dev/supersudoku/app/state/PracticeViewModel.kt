package dev.supersudoku.app.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.supersudoku.core.ClassicSolver
import dev.supersudoku.core.Difficulty
import dev.supersudoku.core.GridDef
import dev.supersudoku.core.PlayBoard
import dev.supersudoku.core.Pos
import dev.supersudoku.core.UndoStack
import dev.supersudoku.core.Variant
import dev.supersudoku.core.enterDigit
import dev.supersudoku.core.validateGrid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Classic 9x9 practice game, bound to one library entry. */
class PracticeViewModel(app: Application) : AndroidViewModel(app), BoardPlayground, SingleBoardVm {
    override val board = PlayBoard(9, 9)
    private val undo = UndoStack(board)
    override val histories: UndoStack get() = undo

    override     var boardVersion by mutableStateOf(0)
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
        val id = entryId ?: return
        val values = board.value.copyOf()
        val notes = board.notes.copyOf()
        val corner = board.corner.copyOf()
        val seconds = playSeconds
        viewModelScope.launch {
            PracticeStore.save(getApplication(), id, values, notes, corner, seconds)
        }
    }
    override var selection by mutableStateOf<Pos?>(null)
    override var notesMode by mutableStateOf(false)
    override var cornerMode by mutableStateOf(false)
    var difficulty by mutableStateOf(Difficulty.MEDIUM)
    override var ready by mutableStateOf(false)
        private set

    private var entryId: String? = null

    /** Synthetic classic grid def for validators/rendering. */
    override val grid = GridDef("practice", "Practice", Variant.CLASSIC, 0, 0, emptyMap())

    /** Solved once on demand (the puzzle is guaranteed uniquely solvable). */
    private var cachedSolution: IntArray? = null

    fun solution(): IntArray? {
        cachedSolution?.let { return it }
        val givens = IntArray(81) { if (board.given[it]) board.value[it] else 0 }
        val sol = givens.copyOf()
        return if (ClassicSolver.solveInto(sol, kotlin.random.Random(0))) {
            cachedSolution = sol
            sol
        } else {
            null
        }
    }

    /** Cells whose entry differs from the solution (mistake mode WRONG). */
    fun wrongCells(): Set<Pos> {
        val sol = solution() ?: return emptySet()
        val out = HashSet<Pos>()
        for (i in 0 until 81) {
            val v = board.value[i]
            if (v != 0 && !board.given[i] && sol[i] != v) {
                out.add(Pos(i % 9, i / 9))
            }
        }
        return out
    }

    private var saveJob: Job? = null

    /** Load a library entry once (idempotent per id). */
    fun ensureEntry(id: String) {
        if (entryId != null) return
        entryId = id
        viewModelScope.launch {
            val loaded = PracticeStore.load(getApplication(), id)
            if (loaded != null) {
                difficulty = loaded.difficulty
                loaded.puzzle.forEachIndexed { i, v ->
                    if (v != 0) {
                        board.value[i] = v
                        board.given[i] = true
                    }
                }
                loaded.values.forEachIndexed { i, v ->
                    if (!board.given[i]) board.value[i] = v
                }
                loaded.notes.copyInto(board.notes)
                loaded.corner.copyInto(board.corner)
                playSeconds = loaded.seconds
            }
            ready = true
            boardVersion++
        }
    }

    override fun touch() {
        boardVersion++
        val id = entryId ?: return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            PracticeStore.save(getApplication(), id, board.value, board.notes, board.corner, playSeconds)
        }
    }

    /** Keypad digit armed for digit-first mode. `0` = erase armed, null = none. */
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

    override fun undo() = undo.undo().also { if (it) touch() }
    override fun redo() = undo.redo().also { if (it) touch() }
    override fun canUndo() = undo.canUndo()
    override fun canRedo() = undo.canRedo()

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

    /** User-only progress: Pair(userFilled, fillable). Givens excluded. */
    fun userProgress(): Pair<Int, Int> {
        var fillable = 0
        var filled = 0
        for (i in 0 until 81) {
            if (board.given[i]) continue
            fillable++
            if (board.value[i] != 0) filled++
        }
        return filled to fillable
    }

    /** Cells to flag red under the current mistake mode. */
    override fun flaggedCells(mode: MistakeMode): Set<Pos> = when (mode) {
        MistakeMode.OFF -> emptySet()
        MistakeMode.CONFLICTS -> conflicts().flatMap { it.cells }.toSet()
        MistakeMode.WRONG -> wrongCells()
    }

    fun conflicts() = validateGrid(grid) { board.get(it) }
}
