package dev.supersudoku.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * OpenSudoku-style input panel: 3x3 digit square (big touch targets) plus a
 * side column of function buttons. Tap a digit to enter (digit or pencil mark
 * depending on notes mode); long-press a digit to force a pencil mark.
 *
 * Faithful to OpenSudoku's keypads:
 * - without an editable selection (cell-first), digits are disabled;
 * - the digit matching the selected cell's value renders checked;
 * - in digit-first mode the armed digit (or armed erase) renders checked;
 * - the erase button doubles as the erase-arm in digit-first mode.
 *
 * @param remaining optional remaining counts per digit (index 1..9), shown as
 * tiny badges; null hides them (e.g. multi-grid overview).
 * @param armedDigit currently armed digit (digit-first mode), or null.
 * @param checkedDigit digit to render checked (cell-first: selected cell value).
 * @param digitsEnabled when false, digit taps do nothing.
 * @param modeAbbr short label of the active input mode for the switcher button;
 * null hides the switcher.
 * @param cornerMode corner (superscript) marks active; notes button shows it.
 * @param dimCompleted dim digits with nothing left to place (needs [remaining]).
 * @param vertical side-panel layout for wide/tablet landscape screens.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadPanel(
    notesMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onDigit: (Int) -> Unit,
    onPencilDigit: (Int) -> Unit,
    onToggleNotes: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    remaining: IntArray? = null,
    armedDigit: Int? = null,
    checkedDigit: Int? = null,
    digitsEnabled: Boolean = true,
    modeAbbr: String? = null,
    onSwitchMode: (() -> Unit)? = null,
    cornerMode: Boolean = false,
    dimCompleted: Boolean = true,
    vertical: Boolean = false,
) {
    if (vertical) {
        Column(
            Modifier.background(BoardColors.bg).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DigitGrid(
                notesMode = notesMode,
                remaining = remaining,
                armedDigit = armedDigit,
                checkedDigit = checkedDigit,
                digitsEnabled = digitsEnabled,
                dimCompleted = dimCompleted,
                onDigit = onDigit,
                onPencilDigit = onPencilDigit,
            )
            Spacer(Modifier.height(6.dp))
            ToolsRow(
                notesMode = notesMode,
                cornerMode = cornerMode,
                canUndo = canUndo,
                canRedo = canRedo,
                onToggleNotes = onToggleNotes,
                onUndo = onUndo,
                onRedo = onRedo,
                modeAbbr = modeAbbr,
                onSwitchMode = onSwitchMode,
            )
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().background(BoardColors.bg).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            DigitGrid(
                notesMode = notesMode,
                remaining = remaining,
                armedDigit = armedDigit,
                checkedDigit = checkedDigit,
                digitsEnabled = digitsEnabled,
                dimCompleted = dimCompleted,
                onDigit = onDigit,
                onPencilDigit = onPencilDigit,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ToolsColumn(
                notesMode = notesMode,
                cornerMode = cornerMode,
                canUndo = canUndo,
                canRedo = canRedo,
                onToggleNotes = onToggleNotes,
                onUndo = onUndo,
                onRedo = onRedo,
                modeAbbr = modeAbbr,
                onSwitchMode = onSwitchMode,
            )
        }
    }
}
/** 3x3 digit grid shared by both pad orientations. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DigitGrid(
    notesMode: Boolean,
    remaining: IntArray?,
    armedDigit: Int?,
    checkedDigit: Int?,
    digitsEnabled: Boolean,
    dimCompleted: Boolean,
    onDigit: (Int) -> Unit,
    onPencilDigit: (Int) -> Unit,
) {
    for (r in 0 until 3) {
        Row(Modifier.fillMaxWidth()) {
            for (c in 0 until 3) {
                val d = r * 3 + c + 1
                        val checked = armedDigit == d || checkedDigit == d
                        val exhausted = dimCompleted && (remaining?.getOrNull(d) == 0) && !checked
                        Box(
                            Modifier
                                .weight(1f)
                                .height(52.dp)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (checked) BoardColors.selection
                                    else BoardColors.cellLine.copy(alpha = 0.45f)
                                )
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "Digit $d" + if (checked) ", selected" else ""
                                    role = Role.Button
                                }
                                .combinedClickable(
                                    enabled = digitsEnabled,
                                    onClick = { onDigit(d) },
                                    onLongClick = { onPencilDigit(d) },
                                    onClickLabel = "Enter $d",
                                    onLongClickLabel = "Pencil mark $d",
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                    Text(
                        d.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = when {
                            exhausted || (!digitsEnabled && !checked) ->
                                BoardColors.dim.copy(alpha = 0.45f)
                            notesMode -> BoardColors.note
                            else -> BoardColors.entry
                        },
                    )
                    val left = remaining?.getOrNull(d)
                    if (left != null) {
                        Text(
                            left.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (left == 0) BoardColors.dim.copy(alpha = 0.5f)
                            else BoardColors.dim,
                            modifier = Modifier.align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Vertical tool stack (phone portrait pad). */
@Composable
private fun ToolsColumn(
    notesMode: Boolean,
    cornerMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onToggleNotes: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modeAbbr: String?,
    onSwitchMode: (() -> Unit)?,
) {
    NotesButton(notesMode, cornerMode, onToggleNotes)
    IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(46.dp)) {
        Icon(
            Icons.Filled.Undo, "Undo",
            tint = if (canUndo) BoardColors.given else BoardColors.cellLine,
        )
    }
    IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(46.dp)) {
        Icon(
            Icons.Filled.Redo, "Redo",
            tint = if (canRedo) BoardColors.given else BoardColors.cellLine,
        )
    }
    if (modeAbbr != null && onSwitchMode != null) {
        TextButton(onClick = onSwitchMode) {
            Text(modeAbbr, style = MaterialTheme.typography.labelMedium, color = BoardColors.dim)
        }
    }
}

/** Horizontal tool strip (tablet landscape pad). */
@Composable
private fun ToolsRow(
    notesMode: Boolean,
    cornerMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onToggleNotes: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modeAbbr: String?,
    onSwitchMode: (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NotesButton(notesMode, cornerMode, onToggleNotes)
        IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(46.dp)) {
            Icon(
                Icons.Filled.Undo, "Undo",
                tint = if (canUndo) BoardColors.given else BoardColors.cellLine,
            )
        }
        IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(46.dp)) {
            Icon(
                Icons.Filled.Redo, "Redo",
                tint = if (canRedo) BoardColors.given else BoardColors.cellLine,
            )
        }
        if (modeAbbr != null && onSwitchMode != null) {
            TextButton(onClick = onSwitchMode) {
                Text(modeAbbr, style = MaterialTheme.typography.labelMedium, color = BoardColors.dim)
            }
        }
    }
}

/**
 * Notes control: cycles value -> center -> corner. The label line is always
 * rendered (fixed height) so toggling never resizes the pad or moves
 * the board.
 */
@Composable
private fun NotesButton(notesMode: Boolean, cornerMode: Boolean, onToggleNotes: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onToggleNotes, modifier = Modifier.size(46.dp)) {
            Icon(
                Icons.Filled.Edit,
                when {
                    cornerMode -> "Corner marks on"
                    notesMode -> "Notes on"
                    else -> "Notes off"
                },
                tint = when {
                    cornerMode -> BoardColors.complete
                    notesMode -> BoardColors.entry
                    else -> BoardColors.dim
                },
            )
        }
        Text(
            when {
                cornerMode -> "corner"
                notesMode -> "center"
                else -> "value"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (notesMode || cornerMode) BoardColors.dim
            else BoardColors.dim.copy(alpha = 0.45f),
        )
    }
}

/**
 * Popup cell editor (POPUP tap mode): a dialog with the same keypad.
 * Tapping a digit applies it to [pos] and closes (same digit clears).
 */
@Composable
fun CellPopup(
    title: String,
    notesMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onDigit: (Int) -> Unit,
    onPencilDigit: (Int) -> Unit,
    onToggleNotes: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDismiss: () -> Unit,
    cornerMode: Boolean = false,
    dimCompleted: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = BoardColors.entry) }
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = BoardColors.given,
            )
        },
        text = {
            KeypadPanel(
                notesMode = notesMode,
                canUndo = canUndo,
                canRedo = canRedo,
                onDigit = { onDigit(it); onDismiss() },
                onPencilDigit = { onPencilDigit(it); onDismiss() },
                onToggleNotes = onToggleNotes,
                onUndo = onUndo,
                onRedo = onRedo,
                cornerMode = cornerMode,
                dimCompleted = dimCompleted,
            )
        },
        containerColor = BoardColors.bg,
        titleContentColor = BoardColors.given,
        textContentColor = BoardColors.given,
    )
}
