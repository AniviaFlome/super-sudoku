package dev.supersudoku.app.state

import dev.supersudoku.core.PlayBoard
import dev.supersudoku.core.Pos
import dev.supersudoku.core.UndoStack
import dev.supersudoku.core.enterDigit
import dev.supersudoku.core.toggleCorner
import dev.supersudoku.core.toggleNote

/**
 * Shared cell-input semantics for every playable board (super ring, practice,
 * standalone variants). Faithful port of OpenSudoku's IMSelectOnTap /
 * IMInsertOnTap, adapted to our notes control:
 *
 * - Cell-first (SELECT): board taps only select; the keypad enters into the
 *   selection. Nothing happens without a selection.
 * - Digit-first (INSERT): the keypad arms a digit (1-9); tapping cells
 *   fills them. Tapping the armed digit again disarms. With bidirectional
 *   selection on, tapping a valued cell arms its digit instead of
 *   overwriting it. Clearing happens via the same-digit toggle.
 * - Popup (POPUP): board taps open the editor; keypad still enters.
 * - Pencil marks come in two flavors: center notes and corner (superscript)
 *   notes. The notes control cycles value -> center -> corner.
 */
interface BoardPlayground {
    val board: PlayBoard
    val histories: UndoStack
    var selection: Pos?
    /** Center pencil marks mode. */
    var notesMode: Boolean
    /** Corner (superscript) pencil marks mode. */
    var cornerMode: Boolean

    /**
     * Armed keypad digit for digit-first mode (1-9), or null when disarmed.
     */
    var armedDigit: Int?

    /** Scope for move-right-after-entry and arrow keys (e.g. focused grid). */
    fun rightScope(p: Pos): Boolean = board.inBounds(p.x, p.y)

    /** Cells whose pencil marks are cleared when a digit is entered nearby. */
    fun peerCells(x: Int, y: Int): Sequence<Pos> = sequence {
        for (i in 0 until board.cols) if (i != x) yield(Pos(i, y))
        for (j in 0 until board.rows) if (j != y) yield(Pos(x, j))
    }

    /** Persist/version bump after a board mutation (VM wires autosave). */
    fun touch()

    fun undo(): Boolean
    fun redo(): Boolean

    /** Elapsed play seconds for the loaded game. */
    val playSeconds: Long

    /** One screen-clock tick (no save; seconds persist via touch()/flush()). */
    fun tickSecond()

    /** Persist immediately when the screen goes away. */
    fun flush()

    fun select(p: Pos?) {
        selection = p
    }

    /** Keyboard/arrows: step the selection, landing on the next in-scope cell. */
    fun moveSelection(dx: Int, dy: Int) {
        val start = selection ?: Pos(-dx, -dy)
        var p = start
        repeat(maxOf(board.cols, board.rows) + 1) {
            p = Pos(p.x + dx, p.y + dy)
            if (!board.inBounds(p.x, p.y)) return
            if (rightScope(p)) {
                selection = p
                return
            }
        }
    }

    fun enter(d: Int, clearPeers: Boolean = true) {
        val s = selection ?: return
        if (!board.inBounds(s.x, s.y) || board.isGiven(s.x, s.y)) return
        // Pressing the shown digit again removes it (OpenSudoku toggle).
        // Selection never moves: entering only writes the cell.
        val effective = if (!notesMode && !cornerMode && d != 0 && board.get(s.x, s.y) == d) 0 else d
        histories.checkpoint()
        if (cornerMode && effective != 0) board.toggleCorner(s.x, s.y, effective)
        else if (notesMode && effective != 0) board.toggleNote(s.x, s.y, effective)
        else board.enterDigit(s.x, s.y, effective)
        if (clearPeers && effective != 0) clearPeerNotes(s.x, s.y, effective)
        touch()
    }

    /** Clear digit [d] from the pencil marks (center + corner) around a cell. */
    fun clearPeerNotes(x: Int, y: Int, d: Int) {
        for (p in peerCells(x, y)) {
            if (!board.inBounds(p.x, p.y) || board.isGiven(p.x, p.y)) continue
            board.notes[board.idx(p.x, p.y)] =
                board.notes[board.idx(p.x, p.y)] and (1 shl d).inv()
            board.corner[board.idx(p.x, p.y)] =
                board.corner[board.idx(p.x, p.y)] and (1 shl d).inv()
        }
    }

    fun erase() {
        val s = selection ?: return
        if (!board.inBounds(s.x, s.y) || board.isGiven(s.x, s.y)) return
        if (board.get(s.x, s.y) == 0 && board.noteMask(s.x, s.y) == 0 &&
            board.cornerMask(s.x, s.y) == 0
        ) {
            return
        }
        histories.checkpoint()
        board.enterDigit(s.x, s.y, 0)
        touch()
    }

    /** Long-press a digit: pencil mark in the active flavor (corner if corner mode). */
    fun pencil(d: Int) {
        val s = selection ?: return
        if (!board.inBounds(s.x, s.y) || board.isGiven(s.x, s.y)) return
        histories.checkpoint()
        if (cornerMode) board.toggleCorner(s.x, s.y, d) else board.toggleNote(s.x, s.y, d)
        touch()
    }

    /** Cycle value -> center marks -> corner marks -> value. */
    fun toggleNotesMode() {
        if (!notesMode && !cornerMode) notesMode = true
        else if (notesMode) {
            notesMode = false
            cornerMode = true
        } else {
            cornerMode = false
        }
    }

    /** Route a board-cell tap. Returns true if a popup should open. */
    fun tapCell(p: Pos?, mode: TapMode, bidirectional: Boolean = true, clearPeers: Boolean = true): Boolean {
        if (p == null || !board.inBounds(p.x, p.y)) {
            selection = null
            return false
        }
        selection = p
        when (mode) {
            // Cell-first: tapping only selects; the keypad does the entering.
            TapMode.SELECT -> Unit
            // Digit-first: fill the tapped cell with the armed digit.
            TapMode.INSERT -> {
                val cur = board.get(p)
                val d = armedDigit
                if (d == null) {
                    // Nothing armed: tapping only selects.
                    return false
                }
                if (bidirectional && cur != 0 && cur != d) {
                    // Tapping a valued cell arms its digit instead of wiping it.
                    armedDigit = cur
                } else {
                    if (board.isGiven(p.x, p.y)) return false
                    histories.checkpoint()
                    if ((notesMode || cornerMode) && d != 0) {
                        if (cornerMode) board.toggleCorner(p.x, p.y, d)
                        else board.toggleNote(p.x, p.y, d)
                    } else {
                        // Pressing the shown digit again removes it (OpenSudoku toggle).
                        val effective = if (d != 0 && cur == d) 0 else d
                        board.enterDigit(p.x, p.y, effective)
                        if (clearPeers && effective != 0) clearPeerNotes(p.x, p.y, effective)
                    }
                    touch()
                }
            }
            TapMode.POPUP -> return true
        }
        return false
    }

    /** Route a keypad digit tap. */
    fun tapDigit(d: Int, mode: TapMode, clearPeers: Boolean = true) {
        when (mode) {
            // Cell-first: enter into the selection; ignore without one.
            TapMode.SELECT -> {
                if (selection != null) enter(d, clearPeers)
            }
            // Digit-first: arm/disarm (tapping the armed digit disarms).
            TapMode.INSERT -> {
                armedDigit = if (armedDigit == d) null else d
            }
            TapMode.POPUP -> {
                // popup has its own buttons; keypad taps still enter into selection
                if (selection != null) enter(d, clearPeers)
            }
        }
    }
}

/** Cycle order for the in-pad input-mode switcher. */
fun nextTapMode(m: TapMode): TapMode = when (m) {
    TapMode.SELECT -> TapMode.INSERT
    TapMode.INSERT -> TapMode.POPUP
    TapMode.POPUP -> TapMode.SELECT
}

/** Short label for the in-pad input-mode switcher. */
fun tapModeAbbr(m: TapMode): String = when (m) {
    TapMode.SELECT -> "Cell"
    TapMode.INSERT -> "Digit"
    TapMode.POPUP -> "Popup"
}
