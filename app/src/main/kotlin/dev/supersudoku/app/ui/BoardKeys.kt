package dev.supersudoku.app.ui

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import dev.supersudoku.app.state.BoardPlayground
import dev.supersudoku.app.state.TapMode

/**
 * Hardware keyboard support for game screens (tablets, Chromebooks,
 * Waydroid): digits enter/arm, 0 + Backspace/Delete erase, arrows move
 * the selection, N cycles pencil marks, Ctrl+Z / Ctrl+Y undo/redo.
 */
@Composable
fun rememberBoardFocus(): FocusRequester {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
    }
    return focus
}

fun Modifier.boardKeys(
    vm: BoardPlayground,
    focus: FocusRequester,
    tapMode: TapMode,
    clearPeers: Boolean,
): Modifier = this
    .focusRequester(focus)
    .focusable()
    .onPreviewKeyEvent { e ->
        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        fun digit(d: Int): Boolean {
            vm.tapDigit(d, tapMode, clearPeers)
            return true
        }
        when (e.key) {
            Key.One, Key.NumPad1 -> digit(1)
            Key.Two, Key.NumPad2 -> digit(2)
            Key.Three, Key.NumPad3 -> digit(3)
            Key.Four, Key.NumPad4 -> digit(4)
            Key.Five, Key.NumPad5 -> digit(5)
            Key.Six, Key.NumPad6 -> digit(6)
            Key.Seven, Key.NumPad7 -> digit(7)
            Key.Eight, Key.NumPad8 -> digit(8)
            Key.Nine, Key.NumPad9 -> digit(9)
            Key.Zero, Key.NumPad0, Key.Backspace, Key.Delete -> {
                vm.erase()
                true
            }
            Key.DirectionUp -> {
                vm.moveSelection(0, -1)
                true
            }
            Key.DirectionDown -> {
                vm.moveSelection(0, 1)
                true
            }
            Key.DirectionLeft -> {
                vm.moveSelection(-1, 0)
                true
            }
            Key.DirectionRight -> {
                vm.moveSelection(1, 0)
                true
            }
            Key.N -> {
                vm.toggleNotesMode()
                true
            }
            Key.Z -> if (e.isCtrlPressed) {
                vm.undo()
                true
            } else false
            Key.U -> {
                vm.undo()
                true
            }
            Key.Y -> if (e.isCtrlPressed) {
                vm.redo()
                true
            } else false
            Key.R -> {
                vm.redo()
                true
            }
            else -> false
        }
    }
