package dev.supersudoku.app.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.supersudoku.core.Conflict
import dev.supersudoku.core.GridDef
import dev.supersudoku.core.PlayBoard
import dev.supersudoku.core.Pos
import dev.supersudoku.core.PuzzleLoader
import dev.supersudoku.core.SuperPuzzle
import dev.supersudoku.core.UndoStack
import dev.supersudoku.core.enterDigit
import dev.supersudoku.core.isGridComplete
import dev.supersudoku.core.toggleNote
import dev.supersudoku.core.units
import dev.supersudoku.core.validateSuper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which puzzle this view-model instance plays. */
sealed interface PuzzleSource {
    data object Bundled : PuzzleSource
    data class Custom(val id: String) : PuzzleSource
    /** A generated map (fresh givens out of the bundled template). */
    data class SuperMap(val id: String) : PuzzleSource
}

/** Board + camera + selection state for a (possibly multi-grid) puzzle. */
class SuperViewModel(app: Application) : AndroidViewModel(app), BoardPlayground {
    private val _puzzle = MutableStateFlow<SuperPuzzle?>(null)
    val puzzle: StateFlow<SuperPuzzle?> = _puzzle.asStateFlow()

    /** The proven solution when known (bundled solution.json or imported). */
    private var solution: Map<Pos, Int>? = null

    private var saveName = "super_save.json"

    override lateinit var board: PlayBoard
    override lateinit var histories: UndoStack

    /** Board extents covering all grids (33×33 bundled, tight box for customs). */
    var boardCols by mutableStateOf(33)
        private set
    var boardRows by mutableStateOf(33)
        private set

    /** Bumped on every board mutation so composables recompose. */
    var boardVersion by mutableStateOf(0)
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
        if (!::board.isInitialized) return
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

    /** Keypad digit armed for digit-first mode. `0` = erase armed, null = none. */
    override var armedDigit by mutableStateOf<Int?>(null)

    /** Scope for move-right-after-entry: focused grid, else any gridded cell. */
    override fun rightScope(p: Pos): Boolean {
        if (!board.inBounds(p.x, p.y)) return false
        val f = focusGrid()
        return if (f != null) {
            p.x in f.x until f.x + 9 && p.y in f.y until f.y + 9
        } else {
            _puzzle.value?.gridsAt(p)?.isNotEmpty() == true
        }
    }

    /** Peer cells for auto-clearing pencil marks: shared row/col/box units. */
    override fun peerCells(x: Int, y: Int): Sequence<Pos> = sequence {
        val seen = HashSet<Pos>()
        val p = Pos(x, y)
        for (g in _puzzle.value?.gridsAt(p).orEmpty()) {
            for (u in g.units()) {
                if (p !in u) continue
                for (c in u) {
                    if (c != p && seen.add(c)) yield(c)
                }
            }
        }
        if (seen.isEmpty()) {
            // Outside any grid (shouldn't happen for real taps): row/col bands.
            for (q in super<BoardPlayground>.peerCells(x, y)) yield(q)
        }
    }

    /** Null = overview canvas; otherwise the focused 9x9 grid id. */
    var focusGridId by mutableStateOf<String?>(null)

    // overview camera (board-space px)
    var cellBase by mutableFloatStateOf(32f)
    var scale by mutableFloatStateOf(1f)
    var origin by mutableStateOf(Offset.Zero)
    /** Last known overview viewport size in px (for wheel-pan clamps). */
    var viewW by mutableFloatStateOf(1f)
    var viewH by mutableFloatStateOf(1f)
    /** True once the camera has been fitted; small viewport jitters (dialogs,
     * insets) must never reset the user's zoom after that. */
    var cameraFitted by mutableStateOf(false)

    private var saveJob: Job? = null
    private var loadedKey: String? = null

    /** Load once (idempotent per source key); UI shows a spinner until done. */
    fun ensureLoaded(source: PuzzleSource) {
        val key = sourceKey(source)
        if (loadedKey != null) return
        loadedKey = key
        viewModelScope.launch {
            loadSource(source)
        }
    }

    /**
     * Run [block] once [source] is loaded (loads first when needed).
     * Used by actions like Restart that must not touch an unloaded board.
     */
    fun whenReady(source: PuzzleSource, block: () -> Unit) {
        val key = sourceKey(source)
        if (loadedKey == key && ::board.isInitialized) {
            block()
            return
        }
        loadedKey = key
        viewModelScope.launch {
            loadSource(source)
            if (::board.isInitialized) block()
        }
    }

    private fun sourceKey(source: PuzzleSource) = when (source) {
        is PuzzleSource.Bundled -> "bundled"
        is PuzzleSource.Custom -> "custom:${source.id}"
        is PuzzleSource.SuperMap -> "map:${source.id}"
    }

    private suspend fun loadSource(source: PuzzleSource) {
        when (source) {
            is PuzzleSource.Bundled -> loadBundled()
            is PuzzleSource.Custom -> loadCustom(source.id)
            is PuzzleSource.SuperMap -> loadMap(source.id)
        }
    }

    private suspend fun loadBundled() {
        val assets = getApplication<Application>().assets
        val text = assets.open("puzzle.json").bufferedReader().use { it.readText() }
        val p = PuzzleLoader.load(text)
        solution = runCatching {
            PuzzleLoader.loadSolution(
                assets.open("solution.json").bufferedReader().use { it.readText() }
            )
        }.getOrNull()
        saveName = "super_save.json"
        finishLoad(p)
    }

    private suspend fun loadCustom(id: String) {
        val (p, sol) = CustomStore.load(getApplication(), id) ?: return
        solution = sol
        saveName = "custom_$id.json"
        finishLoad(p)
    }

    private suspend fun loadMap(id: String) {
        val (p, sol, save) = SuperStore.loadMap(getApplication(), id) ?: return
        solution = sol
        saveName = save
        finishLoad(p)
    }

    private suspend fun finishLoad(p: SuperPuzzle) {
        val (w, h) = p.boardSize()
        boardCols = w
        boardRows = h
        board = PlayBoard(w, h)
        histories = UndoStack(board)
        _puzzle.value = p
        SaveStore.loadBoard(getApplication(), saveName, w, h)?.let { data ->
            data.values.copyInto(board.value)
            data.notes.copyInto(board.notes)
            data.corner.copyInto(board.corner)
            playSeconds = data.seconds
        } ?: run { playSeconds = 0L }
        // givens are fixed: re-assert (also repairs stale saves)
        for (g in p.grids) for ((pos, value) in g.givens) {
            if (board.inBounds(pos.x, pos.y)) board.setGiven(pos.x, pos.y, value)
        }
        boardVersion++
    }

    fun focusGrid(): GridDef? = _puzzle.value?.grids?.firstOrNull { it.id == focusGridId }

    fun gridsAtSelection(): List<GridDef> {
        val s = selection ?: return emptyList()
        return _puzzle.value?.gridsAt(s) ?: emptyList()
    }

    // ---------- mutations (see BoardPlayground for input routing) ----------

    override fun touch() {
        boardVersion++
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            SaveStore.saveBoard(
                getApplication(), saveName, board.value, board.notes, board.corner, playSeconds
            )
        }
    }

    override fun undo(): Boolean {
        if (!histories.undo()) return false
        touch()
        return true
    }

    override fun redo(): Boolean {
        if (!histories.redo()) return false
        touch()
        return true
    }

    fun canUndo() = if (::histories.isInitialized) histories.canUndo() else false
    fun canRedo() = if (::histories.isInitialized) histories.canRedo() else false

    fun resetBoard() {
        if (!::board.isInitialized || !::histories.isInitialized) return
        histories.checkpoint()
        for (i in board.value.indices) {
            if (!board.given[i]) {
                board.value[i] = 0
                board.notes[i] = 0
                board.corner[i] = 0
            }
        }
        playSeconds = 0L
        touch()
    }

    /** Start over: cleared board, empty undo history, fresh save. */
    fun newGame() {
        if (!::board.isInitialized || !::histories.isInitialized) return
        for (i in board.value.indices) {
            if (!board.given[i]) {
                board.value[i] = 0
                board.notes[i] = 0
                board.corner[i] = 0
            }
        }
        histories.clear()
        selection = null
        playSeconds = 0L
        touch()
    }

    // ---------- derived ----------

    fun conflicts(): List<Conflict> {
        val p = _puzzle.value ?: return emptyList()
        return validateSuper(p) { board.get(it) }
    }

    /** Cells whose entry differs from the known solution (mistake mode WRONG). */
    fun wrongCells(): Set<Pos> {
        val sol = solution ?: return emptySet()
        val out = HashSet<Pos>()
        for (y in 0 until boardRows) for (x in 0 until boardCols) {
            val v = board.get(x, y)
            if (v != 0 && !board.isGiven(x, y) && sol[Pos(x, y)] != v) {
                out.add(Pos(x, y))
            }
        }
        return out
    }

    fun hasSolution() = solution != null

    /**
     * Remaining counts per digit for keypad badges, for the focused 9x9.
     * Givens don't count as "filled": remaining = 9 - givens(d) - user(d).
     * Null in overview/composite scope where no single total applies.
     */
    fun remainingCounts(): IntArray? {
        val g = focusGrid() ?: return null
        val givenCount = IntArray(10)
        val userCount = IntArray(10)
        for (dy in 0 until 9) for (dx in 0 until 9) {
            val x = g.x + dx
            val y = g.y + dy
            if (!board.inBounds(x, y)) continue
            val v = board.get(x, y)
            if (v in 1..9) {
                if (board.isGiven(x, y)) givenCount[v]++ else userCount[v]++
            }
        }
        return IntArray(10) { d -> if (d == 0) 0 else 9 - givenCount[d] - userCount[d] }
    }

    /**
     * User-only progress over the union of grid cells (overlaps counted once).
     * Returns Triple(userFilled, fillable, doneGrids). Givens are excluded.
     */
    fun userProgress(): Triple<Int, Int, Int> {
        val p = _puzzle.value ?: return Triple(0, 0, 0)
        val cells = p.grids.flatMap { it.cells() }.toSet()
        var fillable = 0
        var filled = 0
        for (c in cells) {
            if (!board.inBounds(c.x, c.y)) continue
            if (board.isGiven(c.x, c.y)) continue
            fillable++
            if (board.get(c) != 0) filled++
        }
        val done = p.grids.count { isGridComplete(it, board::get) }
        return Triple(filled, fillable, done)
    }

    /** Cells to flag red under the current mistake mode (WRONG falls back to conflicts). */
    fun flaggedCells(mode: MistakeMode): Set<Pos> = when (mode) {
        MistakeMode.OFF -> emptySet()
        MistakeMode.CONFLICTS -> conflicts().flatMap { it.cells }.toSet()
        MistakeMode.WRONG ->
            if (hasSolution()) wrongCells()
            else conflicts().flatMap { it.cells }.toSet()
    }
}
