package dev.supersudoku.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.supersudoku.app.state.MistakeMode
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.app.state.SingleBoardVm
import dev.supersudoku.app.state.TapMode
import dev.supersudoku.app.state.nextTapMode
import dev.supersudoku.app.state.tapModeAbbr
import dev.supersudoku.core.Pos
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * Shared 9x9 game screen (practice entries, standalone variants, variant
 * games): board, timer, OpenSudoku-faithful keypad, popup editor, hardware
 * keyboard. Completion stays hidden while playing. Wide tablets get board
 * + side keypad.
 */
@Composable
fun SingleBoardScreen(
    vm: SingleBoardVm,
    settings: SettingsRepo,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sameDigitOn by settings.sameDigitHighlight.collectAsState(initial = true)
    val mistakeMode by settings.mistakeMode.collectAsState(initial = MistakeMode.CONFLICTS)
    val tapMode by settings.tapMode.collectAsState(initial = TapMode.SELECT)
    val bidir by settings.bidirectionalSelection.collectAsState(initial = true)
    val clearPeers by settings.autoClearPeerNotes.collectAsState(initial = true)
    val dimDone by settings.dimCompletedDigits.collectAsState(initial = true)
    var popupFor by remember { mutableStateOf<Pos?>(null) }
    val focus = rememberBoardFocus()
    // Dialogs steal keyboard focus; hand it back on dismiss.
    LaunchedEffect(popupFor) {
        if (popupFor == null) runCatching { focus.requestFocus() }
    }
    rememberGameClock(vm)
    Column(
        Modifier.fillMaxSize().background(BoardColors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .boardKeys(vm, focus, tapMode, clearPeers)
    ) {
        Row(
            Modifier.fillMaxWidth().background(BoardColors.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = BoardColors.given)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = BoardColors.given,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BoardColors.dim,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Text(
                formatTime(vm.playSeconds),
                style = MaterialTheme.typography.titleSmall,
                color = BoardColors.dim,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        @Suppress("UNUSED_EXPRESSION")
        vm.boardVersion
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth > maxHeight && (maxWidth > 600.dp || maxHeight < 500.dp)
            if (!vm.ready) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (wide) {
                Row(Modifier.fillMaxSize().background(BoardColors.bg)) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().background(BoardColors.bg)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                            )
                    ) {
                        SingleBoard(vm, sameDigitOn, mistakeMode, tapMode, bidir) { popupFor = it }
                    }
                    Column(
                        Modifier.width(300.dp).fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Pad(
                            vm, settings, scope, tapMode, clearPeers, dimDone,
                            vertical = true,
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        SingleBoard(vm, sameDigitOn, mistakeMode, tapMode, bidir) { popupFor = it }
                    }
                    Pad(vm, settings, scope, tapMode, clearPeers, dimDone)
                }
            }
        }
        popupFor?.let { pos ->
            val marksMode = vm.notesMode || vm.cornerMode
            CellPopup(
                title = "Cell ${pos.x + 1}, ${pos.y + 1}",
                notesMode = marksMode,
                canUndo = vm.canUndo(),
                canRedo = vm.canRedo(),
                onDigit = {
                    if (!vm.board.isGiven(pos.x, pos.y)) {
                        vm.select(pos)
                        if (marksMode) vm.pencil(it) else vm.enter(it, clearPeers = clearPeers)
                    }
                    popupFor = null
                },
                onPencilDigit = {
                    if (!vm.board.isGiven(pos.x, pos.y)) {
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

@Composable
private fun Pad(
    vm: SingleBoardVm,
    settings: SettingsRepo,
    scope: kotlinx.coroutines.CoroutineScope,
    tapMode: TapMode,
    clearPeers: Boolean,
    dimDone: Boolean,
    vertical: Boolean = false,
) {
    val sel = vm.selection
    val editableSel = sel != null && !vm.board.isGiven(sel.x, sel.y)
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
        remaining = remember(vm.boardVersion) { vm.remainingCounts() },
        armedDigit = if (tapMode == TapMode.INSERT) vm.armedDigit else null,
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

@Composable
private fun SingleBoard(
    vm: SingleBoardVm,
    sameDigitOn: Boolean,
    mistakeMode: MistakeMode,
    tapMode: TapMode,
    bidir: Boolean,
    onPopup: (Pos) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cell = minOf(constraints.maxWidth, constraints.maxHeight) / 9f
        val ox = (constraints.maxWidth - 9 * cell) / 2f
        val oy = (constraints.maxHeight - 9 * cell) / 2f
        val glyphs = remember(measurer, density, cell) { GlyphCache(measurer, density, cell) }
        @Suppress("UNUSED_EXPRESSION")
        vm.boardVersion
        val centerMarks = rememberCenterMarks(
            vm.board, vm.boardVersion, glyphs,
            (0 until 9).flatMap { dy -> (0 until 9).map { dx -> Pos(dx, dy) } },
        )
        val flagged = remember(vm.boardVersion, mistakeMode) {
            vm.flaggedCells(mistakeMode)
        }
        val selVal = vm.selection?.let { vm.board.get(it) } ?: 0
        Canvas(
            Modifier
                .fillMaxSize()
                .background(BoardColors.bg)
                .pointerInput(cell, ox, oy, tapMode, bidir) {
                    detectTapGestures { tap ->
                        val lx = floor((tap.x - ox) / cell).toInt()
                        val ly = floor((tap.y - oy) / cell).toInt()
                        val pos =
                            if (lx in 0..8 && ly in 0..8) Pos(lx, ly) else null
                        if (vm.tapCell(pos, tapMode, bidir) && pos != null) onPopup(pos)
                    }
                }
        ) {
            val toPx = { p: Pos -> Offset(ox, oy) + Offset(p.x * cell, p.y * cell) }
            for (dy in 0 until 9) for (dx in 0 until 9) {
                val p = Pos(dx, dy)
                drawCell(
                    p, toPx, cell, glyphs,
                    value = vm.board.get(p),
                    isGiven = vm.board.isGiven(dx, dy),
                    center = centerMarks[p],
                    cornerMask = vm.board.cornerMask(dx, dy),
                    tint = null,
                    selected = p == vm.selection,
                    sameDigit = sameDigitOn && (
                        selVal != 0 && vm.board.get(p) == selVal && p != vm.selection ||
                            vm.armedDigit != null && vm.armedDigit != 0 && vm.board.get(p) == vm.armedDigit
                        ),
                    conflict = p in flagged,
                )
            }
            drawCellLines(vm.grid, toPx, cell)
            drawBoxLines(vm.grid, toPx, cell)
            drawGridOutline(vm.grid, toPx, cell)
        }
    }
}
