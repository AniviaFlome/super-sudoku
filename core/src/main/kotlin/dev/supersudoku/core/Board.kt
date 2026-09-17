package dev.supersudoku.core

/** Playable board state: values, givens and pencil marks. UI-agnostic. */
class PlayBoard(val cols: Int, val rows: Int) {
    val value = IntArray(cols * rows) // 0 = empty, else 1..9
    val given = BooleanArray(cols * rows)
    /** Center pencil marks: bit d set => mark d. */
    val notes = IntArray(cols * rows)
    /** Corner (superscript) pencil marks: bit d set => mark d. */
    val corner = IntArray(cols * rows)

    fun idx(x: Int, y: Int) = y * cols + x
    fun inBounds(x: Int, y: Int) = x in 0 until cols && y in 0 until rows

    fun get(x: Int, y: Int): Int = value[idx(x, y)]
    fun get(p: Pos): Int = if (inBounds(p.x, p.y)) value[idx(p.x, p.y)] else 0

    fun setGiven(x: Int, y: Int, v: Int) {
        value[idx(x, y)] = v
        given[idx(x, y)] = true
    }

    fun isGiven(x: Int, y: Int) = given[idx(x, y)]

    fun noteMask(x: Int, y: Int) = notes[idx(x, y)]
    fun hasNote(x: Int, y: Int, d: Int) = notes[idx(x, y)] and (1 shl d) != 0
    fun cornerMask(x: Int, y: Int) = corner[idx(x, y)]
    fun hasCorner(x: Int, y: Int, d: Int) = corner[idx(x, y)] and (1 shl d) != 0

    fun snapshot(): Snapshot = Snapshot(value.copyOf(), notes.copyOf(), corner.copyOf())
    fun restore(s: Snapshot) {
        s.value.copyInto(value)
        s.notes.copyInto(notes)
        // Tolerate snapshots minted before corner marks existed.
        if (s.corner.size == corner.size) s.corner.copyInto(corner)
    }

    data class Snapshot(val value: IntArray, val notes: IntArray, val corner: IntArray = IntArray(0))
}

/** Unlimited undo/redo over board snapshots (cheap: 33x33x2 ints ~ 9KB each). */
class UndoStack(private val board: PlayBoard, private val cap: Int = 500) {
    private val undo = ArrayDeque<PlayBoard.Snapshot>()
    private val redo = ArrayDeque<PlayBoard.Snapshot>()

    /** Record current state before mutating; callers mutate the board afterwards. */
    fun checkpoint() {
        undo.addLast(board.snapshot())
        if (undo.size > cap) undo.removeFirst()
        redo.clear()
    }

    fun canUndo() = undo.isNotEmpty()
    fun canRedo() = redo.isNotEmpty()

    fun undo(): Boolean {
        if (undo.isEmpty()) return false
        redo.addLast(board.snapshot())
        board.restore(undo.removeLast())
        return true
    }

    fun redo(): Boolean {
        if (redo.isEmpty()) return false
        undo.addLast(board.snapshot())
        board.restore(redo.removeLast())
        return true
    }

    fun clear() {
        undo.clear()
        redo.clear()
    }
}

/** Apply a digit (or erase with 0) to a non-given cell. Clears own pencil marks. */
fun PlayBoard.enterDigit(x: Int, y: Int, d: Int): Boolean {
    if (!inBounds(x, y) || isGiven(x, y)) return false
    value[idx(x, y)] = d
    if (d != 0) {
        // entering a digit clears its own pencil marks (center + corner)
        notes[idx(x, y)] = notes[idx(x, y)] and (1 shl d).inv()
        corner[idx(x, y)] = corner[idx(x, y)] and (1 shl d).inv()
        if (value[idx(x, y)] == 0) {
            notes[idx(x, y)] = 0
            corner[idx(x, y)] = 0
        }
    } else {
        notes[idx(x, y)] = 0
        corner[idx(x, y)] = 0
    }
    return true
}

/** Toggle a center pencil mark on an empty non-given cell. */
fun PlayBoard.toggleNote(x: Int, y: Int, d: Int): Boolean {
    if (!inBounds(x, y) || isGiven(x, y) || value[idx(x, y)] != 0) return false
    notes[idx(x, y)] = notes[idx(x, y)] xor (1 shl d)
    return true
}

/** Toggle a corner (superscript) pencil mark on an empty non-given cell. */
fun PlayBoard.toggleCorner(x: Int, y: Int, d: Int): Boolean {
    if (!inBounds(x, y) || isGiven(x, y) || value[idx(x, y)] != 0) return false
    corner[idx(x, y)] = corner[idx(x, y)] xor (1 shl d)
    return true
}
