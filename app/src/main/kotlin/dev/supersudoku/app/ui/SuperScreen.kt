package dev.supersudoku.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.supersudoku.app.state.MistakeMode
import dev.supersudoku.app.state.PuzzleSource
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.app.state.TapMode
import dev.supersudoku.app.state.nextTapMode
import dev.supersudoku.app.state.tapModeAbbr
import dev.supersudoku.app.state.SuperViewModel
import dev.supersudoku.core.GridDef
import dev.supersudoku.core.Pos
import dev.supersudoku.core.SuperPuzzle
import kotlinx.coroutines.launch
import kotlin.math.floor

@Composable
fun SuperScreen(
    vm: SuperViewModel,
    settings: SettingsRepo,
    source: PuzzleSource,
    initialFocusGridId: String? = null,
    /** Bump to re-apply [initialFocusGridId] even when it didn't change. */
    focusNonce: Int = 0,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    LaunchedEffect(vm, source) { vm.ensureLoaded(source) }
    LaunchedEffect(vm, source, initialFocusGridId, focusNonce) {
        vm.focusGridId = initialFocusGridId
    }
    SuperBoardContent(vm, settings, onBack, onOpenSettings)
}

@Composable
fun SuperBoardContent(
    vm: SuperViewModel,
    settings: SettingsRepo,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val puzzle by vm.puzzle.collectAsState()
    val sameDigitOn by settings.sameDigitHighlight.collectAsState(initial = true)
    val mistakeMode by settings.mistakeMode.collectAsState(initial = MistakeMode.CONFLICTS)
    val tapMode by settings.tapMode.collectAsState(initial = TapMode.SELECT)
    val bidir by settings.bidirectionalSelection.collectAsState(initial = true)
    val clearPeers by settings.autoClearPeerNotes.collectAsState(initial = true)
    val dimDone by settings.dimCompletedDigits.collectAsState(initial = true)
    val doubleTap by settings.focusOnDoubleTap.collectAsState(initial = true)
    var popupFor by remember { mutableStateOf<Pos?>(null) }
    val focus = rememberBoardFocus()
    // Dialogs steal keyboard focus; hand it back on dismiss.
    LaunchedEffect(popupFor) {
        if (popupFor == null) runCatching { focus.requestFocus() }
    }
    BoxWithConstraints(        Modifier.fillMaxSize().background(BoardColors.bg)
            .boardKeys(vm, focus, tapMode, clearPeers)
    ) {
        val wide = maxWidth > maxHeight && (maxWidth > 600.dp || maxHeight < 500.dp)
        val p = puzzle
        if (p == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            @Suppress("UNUSED_EXPRESSION")
            vm.boardVersion // subscribe
            val focusGrid = vm.focusGrid()
            if (wide && focusGrid != null) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        SuperTopBar(vm, onBack, onOpenSettings)
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            FocusBoard(vm, p, focusGrid, sameDigitOn, mistakeMode, tapMode, bidir, clearPeers) { popupFor = it }
                        }
                    }
                    Column(
                        Modifier.width(300.dp).fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(8.dp))
                        InputBar(vm, tapMode, settings, clearPeers, dimDone, vertical = true)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    SuperTopBar(vm, onBack, onOpenSettings)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (focusGrid == null) {
                            OverviewBoard(vm, p, sameDigitOn, mistakeMode, tapMode, bidir, clearPeers, doubleTap) { popupFor = it }
                        } else {
                            FocusBoard(vm, p, focusGrid, sameDigitOn, mistakeMode, tapMode, bidir, clearPeers) { popupFor = it }
                        }
                    }
                    InputBar(vm, tapMode, settings, clearPeers, dimDone)
                }
            }
            popupFor?.let { pos ->
                val f = vm.focusGrid()
                val marksMode = vm.notesMode || vm.cornerMode
                CellPopup(
                    title = if (f != null) "Cell ${pos.x - f.x + 1}, ${pos.y - f.y + 1}"
                    else "Cell ${pos.x + 1}, ${pos.y + 1}",
                    notesMode = marksMode,
                    canUndo = vm.canUndo(),
                    canRedo = vm.canRedo(),
                    onDigit = {
                        if (vm.board.inBounds(pos.x, pos.y) && !vm.board.isGiven(pos.x, pos.y)) {
                            vm.select(pos)
                            if (marksMode) vm.pencil(it) else vm.enter(it, clearPeers = clearPeers)
                        }
                        popupFor = null
                    },
                    onPencilDigit = {
                        if (vm.board.inBounds(pos.x, pos.y) && !vm.board.isGiven(pos.x, pos.y)) {
                            vm.select(pos)
                            vm.pencil(it)
                        }
                        popupFor = null
                    },
                    onToggleNotes = vm::toggleNotesMode,
                    onUndo = { vm.undo() },
                    onRedo = { vm.redo() },
                    onDismiss = { popupFor = null },
                    cornerMode = vm.cornerMode,
                    dimCompleted = dimDone,
                )
            }
        }
    }
}

@Composable
private fun SuperTopBar(
    vm: SuperViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {
            if (vm.focusGridId != null) vm.focusGridId = null else onBack()
        }) { Icon(Icons.Filled.ArrowBack, "Back", tint = BoardColors.given) }
        val focus = vm.focusGrid()
        Column(Modifier.weight(1f)) {
            Text(
                focus?.name ?: "Super Sudoku",
                style = MaterialTheme.typography.titleMedium,
                color = BoardColors.given,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            val sel = vm.selection
            val names = vm.gridsAtSelection().map { it.name }
            // In focus mode a single-grid subtitle would just echo the title.
            val showNames = sel != null && names.isNotEmpty() &&
                (focus == null || names.size > 1 || names[0] != focus.name)
            if (showNames) {
                Text(
                    names.joinToString(" + "),
                    style = MaterialTheme.typography.bodySmall,
                    color = BoardColors.dim,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            formatTime(vm.playSeconds),
            style = MaterialTheme.typography.titleSmall,
            color = BoardColors.dim,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, "Settings", tint = BoardColors.dim)
        }
    }
}

/** Clamp the overview camera origin, keeping some board visible. */
private fun clampOrigin(vm: SuperViewModel, o: Offset, vw: Float, vh: Float): Offset {
    val bw = vm.boardCols * vm.cellBase * vm.scale
    val bh = vm.boardRows * vm.cellBase * vm.scale
    return Offset(
        o.x.coerceIn(minOf(vw - bw, 0f) - vw * 0.5f, vw * 0.5f),
        o.y.coerceIn(minOf(vh - bh, 0f) - vh * 0.5f, vh * 0.5f),
    )
}

/** Overview: full 33x33 canvas with pinch-zoom + pan. Tap a grid to focus it. */
@Composable
private fun OverviewBoard(
    vm: SuperViewModel,
    puzzle: SuperPuzzle,
    sameDigitOn: Boolean,
    mistakeMode: MistakeMode,
    tapMode: TapMode,
    bidir: Boolean,
    clearPeers: Boolean,
    focusOnDoubleTap: Boolean,
    onPopup: (Pos) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val vw = constraints.maxWidth.toFloat()
        val vh = constraints.maxHeight.toFloat()
        LaunchedEffect(vw, vh) {
            val wasLandscape = vm.viewW > vm.viewH
            val isLandscape = vw > vh
            val firstFit = !vm.cameraFitted
            vm.viewW = vw
            vm.viewH = vh
            if (firstFit || wasLandscape != isLandscape) {
                // Genuine (re)fit: first show or rotation. Small jitters
                // (dialogs, insets) only clamp the origin, keeping zoom.
                val bw0 = maxOf(vm.boardCols, vm.boardRows)
                vm.cellBase = minOf(vw, vh) / bw0
                vm.scale = 1f
                vm.origin = Offset(
                    (vw - vm.boardCols * vm.cellBase) / 2f,
                    (vh - vm.boardRows * vm.cellBase) / 2f,
                )
                vm.cameraFitted = true
            } else {
                vm.origin = clampOrigin(vm, vm.origin, vw, vh)
            }
        }
        val cell = vm.cellBase * vm.scale
        val glyphs = remember(measurer, density, cell) { GlyphCache(measurer, density, cell) }
        @Suppress("UNUSED_EXPRESSION")
        vm.boardVersion
        val centerMarks = rememberCenterMarks(
            vm.board, vm.boardVersion, glyphs,
            (0 until vm.boardRows).flatMap { y -> (0 until vm.boardCols).map { x -> Pos(x, y) } },
        )
        val flagged = remember(vm.boardVersion, mistakeMode) {
            vm.flaggedCells(mistakeMode)
        }
        val selVal = vm.selection?.let { vm.board.get(it) } ?: 0
        // disjoint tints
        val disjointTint = remember(BoardColors.bg) {
            val m = HashMap<Pos, androidx.compose.ui.graphics.Color>()
            val dis = puzzle.grids.firstOrNull { it.id == "disjoint" }
            dis?.groups?.forEachIndexed { i, grp ->
                for (c in grp) m[c] = BoardColors.disjoint[i % BoardColors.disjoint.size]
            }
            m
        }
        val eqTint = remember(BoardColors.bg) {
            val m = HashMap<Pos, androidx.compose.ui.graphics.Color>()
            val add = puzzle.grids.firstOrNull { it.id == "addition" }
            for (e in add?.equations ?: emptyList()) {
                for (op in e.operands) for (c in op) m[c] = BoardColors.operandTint
                for (c in e.total) m[c] = BoardColors.totalTint
            }
            m
        }
        val xWash = remember(BoardColors.bg) {
            val m = HashMap<Pos, androidx.compose.ui.graphics.Color>()
            for (c in puzzle.grids.firstOrNull { it.id == "sudoku_x" }?.cells().orEmpty()) {
                m[c] = BoardColors.xWash
            }
            m
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .background(BoardColors.bg)
                .pointerInput(Unit) {
                    // mouse wheel: zoom at cursor (desktop/Waydroid); drag pans, pinch zooms
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll) continue
                            val change = event.changes.firstOrNull() ?: continue
                            val delta = change.scrollDelta
                            if (delta == Offset.Zero) continue
                            val z = (1f - delta.y * 0.15f).coerceIn(0.75f, 1.33f)
                            val ns = (vm.scale * z).coerceIn(0.4f, 20f)
                            val k = ns / vm.scale
                            var o = (vm.origin - change.position) * k + change.position
                            val bw = vm.boardCols * vm.cellBase * ns
                            val bh = vm.boardRows * vm.cellBase * ns
                            o = Offset(
                                o.x.coerceIn(
                                    minOf(vm.viewW - bw, 0f) - vm.viewW * 0.5f,
                                    vm.viewW * 0.5f,
                                ),
                                o.y.coerceIn(
                                    minOf(vm.viewH - bh, 0f) - vm.viewH * 0.5f,
                                    vm.viewH * 0.5f,
                                ),
                            )
                            vm.scale = ns
                            vm.origin = o
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .pointerInput(puzzle, tapMode, bidir, clearPeers, focusOnDoubleTap) {
                    // Manual tap handling: detectTapGestures with onDoubleTap set
                    // delays EVERY tap by the double-tap timeout (~400ms), which
                    // made selection feel laggy. Here taps select instantly and
                    // double-tap-to-focus is detected with our own time window.
                    awaitPointerEventScope {
                        var lastTapMs = 0L
                        var lastTapAt = Offset.Zero
                        while (true) {
                            awaitFirstDown()
                            val up = waitForUpOrCancellation() ?: continue
                            val now = System.currentTimeMillis()
                            val c = vm.cellBase * vm.scale
                            val bx = floor((up.position.x - vm.origin.x) / c).toInt()
                            val by = floor((up.position.y - vm.origin.y) / c).toInt()
                            val pos = Pos(bx, by)
                            val target =
                                if (bx in 0 until vm.boardCols && by in 0 until vm.boardRows &&
                                    puzzle.gridsAt(pos).isNotEmpty()
                                ) {
                                    pos
                                } else {
                                    null
                                }
                            if (vm.tapCell(target, tapMode, bidir, clearPeers)) onPopup(pos)
                            val quickSecond = now - lastTapMs < 350 &&
                                (up.position - lastTapAt).getDistance() < 48f
                            lastTapMs = now
                            lastTapAt = up.position
                            if (!quickSecond || !focusOnDoubleTap || tapMode == TapMode.POPUP) continue
                            if (bx in 0 until vm.boardCols && by in 0 until vm.boardRows) {
                                val gs = puzzle.gridsAt(Pos(bx, by))
                                if (gs.size == 1) {
                                    vm.focusGridId = gs[0].id
                                    vm.select(Pos(bx, by))
                                } else if (gs.isNotEmpty()) {
                                    // overlap: focus the grid containing most of the tap area?
                                    // prefer current focus/selection grid, else first
                                    val cur = gs.firstOrNull { it.id == vm.focusGridId }
                                        ?: gs.firstOrNull { g ->
                                            vm.selection?.let { s ->
                                                s.x in g.x until g.x + 9 && s.y in g.y until g.y + 9
                                            } == true
                                        } ?: gs[0]
                                    vm.focusGridId = cur.id
                                    vm.select(Pos(bx, by))
                                }
                            }
                            lastTapMs = 0L
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        // Boost pinch sensitivity: raw gesture zoom feels sluggish.
                        val boosted = 1f + (zoom - 1f) * 1.6f
                        val ns = (vm.scale * boosted).coerceIn(0.4f, 20f)
                        val k = ns / vm.scale
                        var o = (vm.origin - centroid) * k + centroid + pan
                        // clamp: keep some board visible
                        val bw = vm.boardCols * vm.cellBase * ns
                        val bh = vm.boardRows * vm.cellBase * ns
                        o = Offset(
                            o.x.coerceIn(minOf(vw - bw, 0f) - vw * 0.5f, vw * 0.5f),
                            o.y.coerceIn(minOf(vh - bh, 0f) - vh * 0.5f, vh * 0.5f),
                        )
                        vm.scale = ns
                        vm.origin = o
                    }
                }
        ) {
            val toPx = { p: Pos -> vm.origin + Offset(p.x * cell, p.y * cell) }
            // X diagonals first (behind digits)
            for (g in puzzle.grids) drawDiagonals(g, toPx, cell)
            // cells
            for (gy in 0 until vm.boardRows) for (gx in 0 until vm.boardCols) {
                val p = Pos(gx, gy)
                if (puzzle.gridsAt(p).isEmpty()) continue
                drawCell(
                    p, toPx, cell, glyphs,
                    value = vm.board.get(p),
                    isGiven = vm.board.isGiven(gx, gy),
                    center = centerMarks[p],
                    cornerMask = vm.board.cornerMask(gx, gy),
                    tint = disjointTint[p] ?: eqTint[p] ?: xWash[p],
                    selected = p == vm.selection,
                    sameDigit = sameDigitOn && (
                        selVal != 0 && vm.board.get(p) == selVal && p != vm.selection ||
                            vm.armedDigit != null && vm.armedDigit != 0 && vm.board.get(p) == vm.armedDigit
                        ),
                    conflict = p in flagged,
                )
            }
            for (g in puzzle.grids) {
                drawCellLines(g, toPx, cell)
                drawBoxLines(g, toPx, cell)
                drawGridOutline(g, toPx, cell)
                if (g.cages.isNotEmpty()) drawCages(g, toPx, cell, measurer)
                if (g.inequalities.isNotEmpty()) drawInequalities(g, toPx, cell)
                if (g.dots.isNotEmpty()) drawDots(g, toPx, cell)
            }
            drawLabels(puzzle.labels, toPx, cell, measurer)
        }
        // Floating zoom controls (kept off the top bar so titles never wrap).
        Column(
            Modifier.align(Alignment.BottomEnd).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.FilledTonalIconButton(
                onClick = { vm.scale = (vm.scale * 1.5f).coerceIn(0.4f, 20f) },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Filled.ZoomIn, "Zoom in", tint = BoardColors.given)
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.FilledTonalIconButton(
                onClick = { vm.scale = (vm.scale / 1.5f).coerceIn(0.4f, 20f) },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Filled.ZoomOut, "Zoom out", tint = BoardColors.given)
            }
        }
    }
}

/** Focus: one 9x9 grid, fit to width. */
@Composable
private fun FocusBoard(
    vm: SuperViewModel,
    puzzle: SuperPuzzle,
    grid: GridDef,
    sameDigitOn: Boolean,
    mistakeMode: MistakeMode,
    tapMode: TapMode,
    bidir: Boolean,
    clearPeers: Boolean,
    onPopup: (Pos) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    Column(Modifier.fillMaxSize()) {
        // prev/next strip (completion stays hidden while playing)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val idx = puzzle.grids.indexOf(grid)
            val n = puzzle.grids.size
            TextButton(onClick = { vm.focusGridId = puzzle.grids[(idx + n - 1) % n].id }) {
                Text("‹ Prev", color = BoardColors.entry)
            }
            Spacer(Modifier.weight(1f))
            var showRule by remember { mutableStateOf(false) }
            TextButton(onClick = { showRule = true }) {
                Text("ⓘ Rules", color = BoardColors.entry)
            }
            if (showRule) {
                val r = ruleFor(grid.variant)
                AlertDialog(
                    onDismissRequest = { showRule = false },
                    confirmButton = {
                        TextButton(onClick = { showRule = false }) {
                            Text("Got it", color = BoardColors.entry)
                        }
                    },
                    title = { Text(r.name) },
                    text = {
                        Column {
                            Text(r.rule)
                            Spacer(Modifier.height(8.dp))
                            Text("Tip: ${r.tip}", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    containerColor = BoardColors.bg,
                    titleContentColor = BoardColors.given,
                    textContentColor = BoardColors.given,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.focusGridId = puzzle.grids[(idx + 1) % n].id }) {
                Text("Next ›", color = BoardColors.entry)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val cell = minOf(constraints.maxWidth, constraints.maxHeight) / 9f
            val ox = (constraints.maxWidth - 9 * cell) / 2f
            val oy = (constraints.maxHeight - 9 * cell) / 2f
            val glyphs = remember(measurer, density, cell) { GlyphCache(measurer, density, cell) }
            @Suppress("UNUSED_EXPRESSION")
            vm.boardVersion
            val centerMarks = rememberCenterMarks(vm.board, vm.boardVersion, glyphs, grid.cells())
            val flagged = remember(vm.boardVersion, grid.id, mistakeMode) {
                vm.flaggedCells(mistakeMode).filter { it in grid.cells() }.toSet()
            }
            val selVal = vm.selection?.let { vm.board.get(it) } ?: 0
            val dis = puzzle.grids.firstOrNull { it.id == "disjoint" }
            val disTint = remember(BoardColors.bg) {
                val m = HashMap<Pos, androidx.compose.ui.graphics.Color>()
                dis?.groups?.forEachIndexed { i, grp ->
                    for (c in grp) m[c] = BoardColors.disjoint[i % BoardColors.disjoint.size]
                }
                m
            }
            val add = puzzle.grids.firstOrNull { it.id == "addition" }
            val eqTint = remember(BoardColors.bg) {
                val m = HashMap<Pos, androidx.compose.ui.graphics.Color>()
                for (e in add?.equations ?: emptyList()) {
                    for (op in e.operands) for (c in op) m[c] = BoardColors.operandTint
                    for (c in e.total) m[c] = BoardColors.totalTint
                }
                m
            }
            val xWash = remember(BoardColors.bg) {
                val m = HashMap<Pos, androidx.compose.ui.graphics.Color>()
                for (c in puzzle.grids.firstOrNull { it.id == "sudoku_x" }?.cells().orEmpty()) {
                    m[c] = BoardColors.xWash
                }
                m
            }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .background(BoardColors.bg)
                    .pointerInput(grid.id, cell, ox, oy, tapMode, bidir, clearPeers) {
                        detectTapGestures { tap ->
                            val lx = floor((tap.x - ox) / cell).toInt()
                            val ly = floor((tap.y - oy) / cell).toInt()
                            val pos =
                                if (lx in 0..8 && ly in 0..8) Pos(grid.x + lx, grid.y + ly)
                                else null
                            if (vm.tapCell(pos, tapMode, bidir, clearPeers) && pos != null) onPopup(pos)
                        }
                    }
            ) {
                val toPx = { p: Pos -> Offset(ox, oy) + Offset((p.x - grid.x) * cell, (p.y - grid.y) * cell) }
                drawDiagonals(grid, toPx, cell)
                for (dy in 0 until 9) for (dx in 0 until 9) {
                    val p = Pos(grid.x + dx, grid.y + dy)
                    drawCell(
                        p, toPx, cell, glyphs,
                        value = vm.board.get(p),
                        isGiven = vm.board.isGiven(p.x, p.y),
                        center = centerMarks[p],
                        cornerMask = vm.board.cornerMask(p.x, p.y),
                        tint = disTint[p] ?: eqTint[p] ?: xWash[p],
                        selected = p == vm.selection,
                        sameDigit = sameDigitOn && (
                        selVal != 0 && vm.board.get(p) == selVal && p != vm.selection ||
                            vm.armedDigit != null && vm.armedDigit != 0 && vm.board.get(p) == vm.armedDigit
                        ),
                        conflict = p in flagged,
                    )
                }
                drawCellLines(grid, toPx, cell)
                drawBoxLines(grid, toPx, cell)
                drawGridOutline(grid, toPx, cell)
                if (grid.cages.isNotEmpty()) drawCages(grid, toPx, cell, measurer)
                if (grid.inequalities.isNotEmpty()) drawInequalities(grid, toPx, cell)
                if (grid.dots.isNotEmpty()) drawDots(grid, toPx, cell)
            }
        }
    }
}

/** Keypad adapter for the super board (see Keypad.kt). */
@Composable
fun InputBar(
    vm: SuperViewModel,
    tapMode: TapMode,
    settings: SettingsRepo,
    clearPeers: Boolean,
    dimDone: Boolean,
    vertical: Boolean = false,
) {
    @Suppress("UNUSED_EXPRESSION")
    vm.boardVersion // subscribe
    val scope = rememberCoroutineScope()
    val remaining = remember(vm.boardVersion, vm.focusGridId) { vm.remainingCounts() }
    val sel = vm.selection
    val editableSel = sel != null && vm.board.inBounds(sel.x, sel.y) && !vm.board.isGiven(sel.x, sel.y)
    val padEnabled = tapMode == TapMode.INSERT || editableSel
    KeypadPanel(
        notesMode = vm.notesMode || vm.cornerMode,
        canUndo = vm.canUndo(),
        canRedo = vm.canRedo(),
        onDigit = { vm.tapDigit(it, tapMode, clearPeers) },
        onPencilDigit = vm::pencil,
        onToggleNotes = vm::toggleNotesMode,
        onUndo = { vm.undo() },
        onRedo = { vm.redo() },
        remaining = remaining,
        armedDigit = if (tapMode == TapMode.INSERT) vm.armedDigit else null,
        // Cell-first: the digit matching the selected cell's value renders checked.
        checkedDigit = if (tapMode == TapMode.SELECT) {
            sel?.let { vm.board.get(it) }.takeIf { it in 1..9 }
        } else null,
        digitsEnabled = padEnabled,
        modeAbbr = tapModeAbbr(tapMode),
        onSwitchMode = { scope.launch { settings.setTapMode(nextTapMode(tapMode)) } },
        cornerMode = vm.cornerMode,
        dimCompleted = dimDone,
        vertical = vertical,
    )
}
