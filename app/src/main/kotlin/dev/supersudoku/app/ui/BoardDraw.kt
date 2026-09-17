package dev.supersudoku.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.supersudoku.core.ConflictKind
import dev.supersudoku.core.GridDef
import dev.supersudoku.core.PlayBoard
import dev.supersudoku.core.Pos
import dev.supersudoku.core.Variant

/** Active board colors. Mutated by [applyPalette] when the theme changes. */
object BoardColors {
    var bg = Color(0xFF101418)
        private set
    var cellLine = Color(0xFF2A3138)
        private set
    var boxLine = Color(0xFF5A6672)
        private set
    var gridOutline = Color(0xFF9AA6B2)
        private set
    var given = Color(0xFFE8EAED)
        private set
    var entry = Color(0xFF7FB4FF)
        private set
    var note = Color(0xFF8A94A0)
        private set
    var selection = Color(0xFF2E4A6B)
        private set
    var sameDigit = Color(0xFF274264)
        private set
    var conflictBg = Color(0xFF5C1F1F)
        private set
    var conflictFg = Color(0xFFFF7B72)
        private set
    var complete = Color(0xFF3FB950)
        private set
    var operandTint = Color(0xFF1F6FEB).copy(alpha = 0.16f)
        private set
    var totalTint = Color(0xFF3FB950).copy(alpha = 0.18f)
        private set
    var dim = Color(0xFF8A94A0)
        private set
    var dotWhite = Color(0xFFF0F0F0)
        private set
    /** Consecutive "pink bridge" between cells (carykh's original). */
    var bridgePink = Color(0xFFF032E6)
        private set
    var diagonal = Color(0xFFE6194B).copy(alpha = 0.75f)
        private set
    /** Light wash for the Sudoku X grid (blue in the original). */
    var xWash = Color(0xFF4363D8).copy(alpha = 0.10f)
        private set
    var cageLine = Color(0xFF8A94A0)
        private set

    /** 9 muted tints for disjoint positional groups (all 9 groups colored). */
    var disjoint = listOf(
        Color(0xFFE6194B), Color(0xFFF58231), Color(0xFFFFE119),
        Color(0xFF3CB44B), Color(0xFF42D4F4), Color(0xFF4363D8),
        Color(0xFFF032E6), Color(0xFF9A6324), Color(0xFFA9A9A9),
    ).map { it.copy(alpha = 0.22f) }
        private set

    fun applyPalette(p: BoardPalette) {
        bg = p.bg
        cellLine = p.cellLine
        boxLine = p.boxLine
        gridOutline = p.gridOutline
        given = p.given
        entry = p.entry
        note = p.note
        selection = p.selection
        sameDigit = p.sameDigit
        conflictBg = p.conflictBg
        conflictFg = p.conflictFg
        complete = p.complete
        operandTint = p.operandTint
        totalTint = p.totalTint
        dim = p.dim
        dotWhite = p.dotWhite
        bridgePink = p.bridgePink
        diagonal = p.diagonal
        xWash = p.xWash
        cageLine = p.cageLine
        disjoint = p.disjoint
    }
}

/** Cached digit/notes glyph layouts for one cell size. */
class GlyphCache(
    private val measurer: TextMeasurer,
    private val density: androidx.compose.ui.unit.Density,
    private val cellPx: Float,
) {
    private fun sp(px: Float) =
        androidx.compose.ui.unit.TextUnit(px / density.density, androidx.compose.ui.unit.TextUnitType.Sp)

    private fun style(sizePx: Float) = TextStyle(
        fontSize = sp(sizePx),
        textAlign = TextAlign.Center,
    )

    private val digits: Map<Int, androidx.compose.ui.text.TextLayoutResult> =
        (1..9).associateWith { d ->
            measurer.measure(AnnotatedString(d.toString()), style(cellPx * 0.58f))
        }
    private val cornerG: Map<Int, androidx.compose.ui.text.TextLayoutResult> =
        (1..9).associateWith { d ->
            measurer.measure(AnnotatedString(d.toString()), style(cellPx * 0.18f))
        }

    private fun measureCenter(text: String, sizePx: Float) =
        measurer.measure(AnnotatedString(text), style(sizePx))

    /**
     * Measure the center-marks row for a mask during composition (never
     * during draw). Returns null for an empty mask.
     */
    fun measureCenterMask(mask: Int): androidx.compose.ui.text.TextLayoutResult? {
        val digits = (1..9).filter { mask and (1 shl it) != 0 }
        if (digits.isEmpty()) return null
        val text = digits.joinToString("")
        var size = cellPx * 0.26f
        var layout = measureCenter(text, size)
        while (layout.size.width > cellPx * 0.97f && size > cellPx * 0.10f) {
            size *= 0.9f
            layout = measureCenter(text, size)
        }
        return layout
    }

    fun DrawScope.drawDigit(d: Int, center: Offset, cellPx: Float, isGiven: Boolean, conflict: Boolean) {
        val layout = digits.getValue(d)
        drawText(
            layout,
            color = when {
                conflict -> BoardColors.conflictFg
                isGiven -> BoardColors.given
                else -> BoardColors.entry
            },
            topLeft = Offset(
                center.x - layout.size.width / 2f,
                center.y - layout.size.height / 2f,
            ),
        )
    }

    /**
     * Center marks: one flowing row in the middle band only (OpenSudoku
     * primary marks), pre-measured in composition via [measureCenterMask].
     */
    fun DrawScope.drawNotes(
        layout: androidx.compose.ui.text.TextLayoutResult,
        topLeft: Offset,
        cellPx: Float,
        highlighted: Boolean = false,
    ) {
        val cx = topLeft.x + cellPx / 2f
        val cy = topLeft.y + cellPx * 0.5f
        drawText(
            layout,
            color = if (highlighted) BoardColors.given else BoardColors.note,
            topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f),
        )
    }

    /**
     * Corner (superscript) marks in the top/bottom bands, distributed across
     * the 4 corners by count like OpenSudoku (top band: 5 slots, bottom: 4).
     * Slot depends on the mark's index among present marks.
     */
    fun DrawScope.drawCorner(mask: Int, topLeft: Offset, cellPx: Float, highlighted: Boolean = false) {
        val digits = (1..9).filter { mask and (1 shl it) != 0 }
        if (digits.isEmpty()) return
        val slots = cornerSlots(digits.size)
        for ((i, d) in digits.withIndex()) {
            val pos = slots[i]
            val (col, row, cols) = if (pos < 5) Triple(pos, 0, 5) else Triple(pos - 5, 2, 4)
            val layout = cornerG.getValue(d)
            val cx = topLeft.x + (col + 0.5f) / cols * cellPx
            val cy = topLeft.y + (row + 0.5f) / 3f * cellPx
            drawText(
                layout,
                color = if (highlighted) BoardColors.given else BoardColors.note,
                topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f),
            )
        }
    }

    /** Port of OpenSudoku's corner distribution for secondary marks. */
    private fun cornerSlots(n: Int): List<Int> {
        val out = IntArray(n)
        var idx = 0
        var remaining = n
        // top-left, left-aligned in 5 slots
        var count = (remaining + 3) / 4
        for (k in 0 until count) out[idx++] = k
        remaining -= count
        // top-right, right-aligned in 5 slots
        count = (remaining + 2) / 3
        for (k in 0 until count) out[idx++] = 5 - count + k
        remaining -= count
        // bottom-left in 4 slots
        count = (remaining + 1) / 2
        for (k in 0 until count) out[idx++] = 5 + k
        remaining -= count
        // bottom-right, right-aligned in 4 slots
        for (k in 0 until remaining) out[idx++] = 9 - remaining + k
        return out.toList()
    }
}

// ---------- small helpers needing density ----------

private fun DrawScope.pxToSp(px: Float) =
    androidx.compose.ui.unit.TextUnit(px / density, androidx.compose.ui.unit.TextUnitType.Sp)

/**
 * Draw one cell's background + value. [toPx] maps a global [Pos] to its top-left px.
 * All lookups use global coords so overview and focus mode share this code.
 */
fun DrawScope.drawCell(
    p: Pos,
    toPx: (Pos) -> Offset,
    cellPx: Float,
    glyphs: GlyphCache,
    value: Int,
    isGiven: Boolean,
    center: androidx.compose.ui.text.TextLayoutResult?,
    cornerMask: Int = 0,
    tint: Color?,
    selected: Boolean,
    sameDigit: Boolean,
    conflict: Boolean,
) {
    val tl = toPx(p)
    if (tint != null) drawRect(tint, tl, androidx.compose.ui.geometry.Size(cellPx, cellPx))
    if (selected) drawRect(BoardColors.selection, tl, androidx.compose.ui.geometry.Size(cellPx, cellPx))
    else if (conflict) drawRect(BoardColors.conflictBg, tl, androidx.compose.ui.geometry.Size(cellPx, cellPx))
    else if (sameDigit && value != 0) drawRect(BoardColors.sameDigit, tl, androidx.compose.ui.geometry.Size(cellPx, cellPx))
    val ctr = tl + Offset(cellPx / 2f, cellPx / 2f)
    if (value in 1..9) {
        with(glyphs) { drawDigit(value, ctr, cellPx, isGiven, conflict) }
    } else {
        val lit = selected || conflict || (sameDigit && (center != null || cornerMask != 0))
        if (cornerMask != 0) {
            with(glyphs) { drawCorner(cornerMask, tl, cellPx, lit) }
        }
        if (center != null) {
            with(glyphs) { drawNotes(center, tl, cellPx, lit) }
        }
    }
}

/**
 * Precompute center-mark layouts for cells during composition (keyed on the
 * board version by callers). Measuring during the draw pass is unreliable,
 * so the draw path only consumes ready layouts.
 */
@Composable
fun rememberCenterMarks(
    board: PlayBoard,
    version: Int,
    glyphs: GlyphCache,
    cells: Iterable<Pos>,
): Map<Pos, androidx.compose.ui.text.TextLayoutResult> {
    @Suppress("UNUSED_EXPRESSION")
    version
    return remember(version, glyphs) {
        buildMap {
            for (p in cells) {
                val m = board.noteMask(p.x, p.y)
                if (m != 0) {
                    glyphs.measureCenterMask(m)?.let { put(p, it) }
                }
            }
        }
    }
}

private fun DrawScope.hline(x0: Float, x1: Float, y: Float, color: Color, w: Float) =
    drawLine(color, Offset(x0, y), Offset(x1, y), w)

private fun DrawScope.vline(x: Float, y0: Float, y1: Float, color: Color, w: Float) =
    drawLine(color, Offset(x, y0), Offset(x, y1), w)

/** Thin cell separators inside a 9x9 grid. */
fun DrawScope.drawCellLines(grid: GridDef, toPx: (Pos) -> Offset, cellPx: Float) {
    val w = (cellPx * 0.03f).coerceAtLeast(1f)
    for (i in 0..9) {
        val x = toPx(Pos(grid.x, grid.y)).x + i * cellPx
        val y0 = toPx(Pos(grid.x, grid.y)).y
        vline(x, y0, y0 + 9 * cellPx, BoardColors.cellLine, w)
        val y = y0 + i * cellPx
        val x0 = toPx(Pos(grid.x, grid.y)).x
        hline(x0, x0 + 9 * cellPx, y, BoardColors.cellLine, w)
    }
}

/** Medium 3x3 box lines, or jigsaw region borders for IRREGULAR. */
fun DrawScope.drawBoxLines(grid: GridDef, toPx: (Pos) -> Offset, cellPx: Float) {
    val w = (cellPx * 0.07f).coerceAtLeast(1.5f)
    fun edge(x: Int, y: Int, horiz: Boolean) {
        val a = toPx(Pos(x, y))
        if (horiz) hline(a.x, a.x + cellPx, a.y, BoardColors.boxLine, w)
        else vline(a.x, a.y, a.y + cellPx, BoardColors.boxLine, w)
    }
    if (grid.variant == Variant.IRREGULAR) {
        val inRegion = grid.regions.map { it.toSet() }
        fun regionOf(p: Pos) = inRegion.indexOfFirst { p in it }
        for (dy in 0 until 9) for (dx in 0 until 9) {
            val p = Pos(grid.x + dx, grid.y + dy)
            val r = regionOf(p)
            if (dx + 1 < 9 && regionOf(Pos(p.x + 1, p.y)) != r) edge(p.x + 1, p.y, horiz = false)
            if (dy + 1 < 9 && regionOf(Pos(p.x, p.y + 1)) != r) edge(p.x, p.y + 1, horiz = true)
        }
    } else {
        for (i in 0..3) {
            val gx = grid.x + i * 3
            val gy = grid.y + i * 3
            val a = toPx(Pos(grid.x, grid.y))
            vline(toPx(Pos(gx, grid.y)).x, a.y, a.y + 9 * cellPx, BoardColors.boxLine, w)
            hline(a.x, a.x + 9 * cellPx, toPx(Pos(grid.x, gy)).y, BoardColors.boxLine, w)
        }
    }
}

/** Bold 9x9 outline. */
fun DrawScope.drawGridOutline(grid: GridDef, toPx: (Pos) -> Offset, cellPx: Float) {
    val w = (cellPx * 0.1f).coerceAtLeast(2f)
    val a = toPx(Pos(grid.x, grid.y))
    val s = 9 * cellPx
    drawRect(BoardColors.gridOutline, a, androidx.compose.ui.geometry.Size(s, s), style = Stroke(w))
}

/** Killer cages: dotted borders + small totals. */
fun DrawScope.drawCages(
    grid: GridDef,
    toPx: (Pos) -> Offset,
    cellPx: Float,
    measurer: TextMeasurer,
) {
    val w = (cellPx * 0.05f).coerceAtLeast(1f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(cellPx * 0.14f, cellPx * 0.1f))
    val memberOf = HashMap<Pos, Int>()
    grid.cages.forEachIndexed { i, c -> for (p in c.cells) memberOf[p] = i }
    for ((i, cage) in grid.cages.withIndex()) {
        for (p in cage.cells) {
            val tl = toPx(p)
            val r = Pos(p.x + 1, p.y)
            val d = Pos(p.x, p.y + 1)
            if (memberOf[r] != i) drawLine(
                BoardColors.cageLine, Offset(tl.x + cellPx, tl.y),
                Offset(tl.x + cellPx, tl.y + cellPx), w, pathEffect = dash,
            )
            if (memberOf[d] != i) drawLine(
                BoardColors.cageLine, Offset(tl.x, tl.y + cellPx),
                Offset(tl.x + cellPx, tl.y + cellPx), w, pathEffect = dash,
            )
            val l = Pos(p.x - 1, p.y)
            val u = Pos(p.x, p.y - 1)
            if (memberOf[l] != i) drawLine(
                BoardColors.cageLine, Offset(tl.x, tl.y),
                Offset(tl.x, tl.y + cellPx), w, pathEffect = dash,
            )
            if (memberOf[u] != i) drawLine(
                BoardColors.cageLine, Offset(tl.x, tl.y),
                Offset(tl.x + cellPx, tl.y), w, pathEffect = dash,
            )
        }
        // total at the top-left-most cell corner
        val anchor = cage.cells.minWithOrNull(compareBy({ it.y }, { it.x })) ?: continue
        val layout = measurer.measure(
            AnnotatedString(cage.total.toString()),
            TextStyle(
                fontSize = pxToSp(cellPx * 0.3f),
                color = BoardColors.dim,
            ),
        )
        val tl = toPx(anchor)
        drawText(layout, color = BoardColors.dim, topLeft = tl + Offset(cellPx * 0.06f, 0f))
    }
}

/**
 * Futoshiki inequality chevrons at edge midpoints, drawn as strokes
 * (no font dependency). Apex points at the smaller cell: lo < hi.
 */
fun DrawScope.drawInequalities(
    grid: GridDef,
    toPx: (Pos) -> Offset,
    cellPx: Float,
) {
    val w = (cellPx * 0.07f).coerceAtLeast(1.5f)
    val s = cellPx * 0.2f
    for ((lo, hi) in grid.inequalities) {
        val a = toPx(lo) + Offset(cellPx / 2f, cellPx / 2f)
        val b = toPx(hi) + Offset(cellPx / 2f, cellPx / 2f)
        val mid = (a + b) / 2f
        // direction from hi to lo (apex sits on the lo side)
        val dx = (a.x - b.x).let { if (it == 0f) 0f else it / kotlin.math.abs(it) }
        val dy = (a.y - b.y).let { if (it == 0f) 0f else it / kotlin.math.abs(it) }
        val apex = mid + Offset(dx * s, dy * s)
        // perpendicular spread
        val px = -dy
        val py = dx
        // if somehow diagonal (shouldn't happen), fall back to horizontal chevron
        val base = mid - Offset(dx * s * 0.4f, dy * s * 0.4f)
        drawLine(BoardColors.dim, apex, base + Offset(px * s, py * s), w)
        drawLine(BoardColors.dim, apex, base - Offset(px * s, py * s), w)
    }
}

/** Consecutive bridges: chunky pink bars across the shared edge (original style). */
fun DrawScope.drawDots(
    grid: GridDef,
    toPx: (Pos) -> Offset,
    cellPx: Float,
) {
    val thick = cellPx * 0.16f
    for ((a, b) in grid.dots) {
        val pa = toPx(a) + Offset(cellPx / 2f, cellPx / 2f)
        val pb = toPx(b) + Offset(cellPx / 2f, cellPx / 2f)
        val mid = (pa + pb) / 2f
        val len = cellPx * 0.52f
        if (a.y == b.y) {
            // horizontal pair: shared edge is vertical
            drawLine(
                BoardColors.bridgePink,
                mid + Offset(0f, -len / 2f),
                mid + Offset(0f, len / 2f),
                strokeWidth = thick,
            )
        } else {
            drawLine(
                BoardColors.bridgePink,
                mid + Offset(-len / 2f, 0f),
                mid + Offset(len / 2f, 0f),
                strokeWidth = thick,
            )
        }
    }
}

/** Sudoku-X corner-to-corner diagonals (behind digits). */
fun DrawScope.drawDiagonals(grid: GridDef, toPx: (Pos) -> Offset, cellPx: Float) {
    if (!grid.diagonals) return
    val w = (cellPx * 0.06f).coerceAtLeast(1f)
    val a = toPx(Pos(grid.x, grid.y))
    val s = 9 * cellPx
    drawLine(BoardColors.diagonal, a, a + Offset(s, s), w)
    drawLine(BoardColors.diagonal, a + Offset(s, 0f), a + Offset(0f, s), w)
}

/** Corridor labels (overview only): centered compass-style captions. */
fun DrawScope.drawLabels(
    labels: List<dev.supersudoku.core.BoardLabel>,
    toPx: (Pos) -> Offset,
    cellPx: Float,
    measurer: TextMeasurer,
) {
    // Bounding boxes of already-drawn labels (board px); a label whose box
    // would intersect one is skipped so words can never print over each other.
    val drawn = mutableListOf<Pair<Offset, androidx.compose.ui.geometry.Size>>()
    for (l in labels) {
        val layout = measurer.measure(
            AnnotatedString(l.text),
            TextStyle(
                fontSize = pxToSp(cellPx * 0.42f),
                color = BoardColors.dim,
                fontWeight = FontWeight.Bold,
            ),
        )
        val origin = toPx(Pos(l.x, l.y))
        val w = layout.size.width.toFloat()
        val h = layout.size.height.toFloat()
        // Center on the cell; clamp inside the board so edge labels stay visible.
        val cx = (origin.x + (cellPx - w) / 2f).coerceIn(0f, (size.width - w).coerceAtLeast(0f))
        val cy = (origin.y + (cellPx - h) / 2f).coerceIn(0f, (size.height - h).coerceAtLeast(0f))
        val box = Offset(cx, cy) to androidx.compose.ui.geometry.Size(w, h)
        if (drawn.any { (o, s) ->
                cx < o.x + s.width && o.x < cx + w && cy < o.y + s.height && o.y < cy + h
            }
        ) {
            continue
        }
        drawn.add(box)
        drawText(layout, color = BoardColors.dim, topLeft = Offset(cx, cy))
    }
}
