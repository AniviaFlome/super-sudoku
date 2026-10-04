package dev.supersudoku.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * OpenSudoku-style input panel: a 5-column x 3-row grid mirroring
 * OpenSudoku's `im_keypad.xml` (`IMInsertOnTap`).
 *
 * - columns 0-2: digits 1-9 in a 3x3 block (big touch targets, big
 *   digit only); tap to enter (digit or pencil mark depending on
 *   mark mode); long-press a digit to force a pencil mark;
 * - column 3: the mark-mode radio column - Value (`enter_number`),
 *   Center (`primary_mark`), Corner (`secondary_mark`); exactly one is
 *   checked, both mark buttons off = value entry;
 * - column 4: Undo, Redo, and the input-mode switcher (OpenSudoku's
 *   `switch_input_mode`, which cycles its Popup/InsertOnTap/SelectOnTap
 *   methods just like our tap-mode switcher).
 *
 * Faithful to OpenSudoku's keypads:
 * - without an editable selection (cell-first), digits are disabled;
 * - the digit matching the selected cell's value renders checked;
 * - in digit-first mode the armed digit renders checked.
 *
 * @param remaining optional remaining counts per digit (index 1..9), used
 * only to dim completed digits; no badges are shown.
 * @param armedDigit currently armed digit (digit-first mode), or null.
 * @param checkedDigit digit to render checked (cell-first: selected cell value).
 * @param digitsEnabled when false, digit taps do nothing.
 * @param modeAbbr short label of the active input mode for the switcher button;
 * null hides the switcher.
 * @param cornerMode corner (superscript) marks active; exactly one of
 * value / center / corner is active at a time.
 * @param dimCompleted dim digits with nothing left to place (needs [remaining]).
 * @param vertical side-panel layout for wide/tablet landscape screens.
 * @param buttonsLeft mirror the pad: mode/tool button columns on the left,
 * digits on the right. Default false (current behavior).
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
    onToggleCorner: () -> Unit,
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
    buttonsLeft: Boolean = false,
) {
    val pad: @Composable () -> Unit = {
        PadGrid(
            notesMode = notesMode,
            cornerMode = cornerMode,
            remaining = remaining,
            armedDigit = armedDigit,
            checkedDigit = checkedDigit,
            digitsEnabled = digitsEnabled,
            dimCompleted = dimCompleted,
            canUndo = canUndo,
            canRedo = canRedo,
            onDigit = onDigit,
            onPencilDigit = onPencilDigit,
            onToggleNotes = onToggleNotes,
            onToggleCorner = onToggleCorner,
            onUndo = onUndo,
            onRedo = onRedo,
            modeAbbr = modeAbbr,
            onSwitchMode = onSwitchMode,
            buttonsLeft = buttonsLeft,
        )
    }
    if (vertical) {
        Column(
            Modifier.background(BoardColors.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            pad()
        }
        return
    }
    Column(
        Modifier.fillMaxWidth().background(BoardColors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        pad()
    }
}
/**
 * The shared 5x3 pad: 3 digit columns plus the mode-radio column and the
 * misc column (Undo / Redo / input-mode switch).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PadGrid(
    notesMode: Boolean,
    cornerMode: Boolean,
    remaining: IntArray?,
    armedDigit: Int?,
    checkedDigit: Int?,
    digitsEnabled: Boolean,
    dimCompleted: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onDigit: (Int) -> Unit,
    onPencilDigit: (Int) -> Unit,
    onToggleNotes: () -> Unit,
    onToggleCorner: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modeAbbr: String?,
    onSwitchMode: (() -> Unit)?,
    buttonsLeft: Boolean = false,
) {
    // Single source of truth for the radio column: the state toggles are
    // already mutually exclusive, and the guard below keeps exactly one
    // cell checked even if a caller ever passes both flags true.
    val centerActive = notesMode
    val cornerActive = cornerMode && !notesMode
    val valueMode = !notesMode && !cornerMode
    val onSelectValue = {
        if (notesMode) onToggleNotes()
        if (cornerMode) onToggleCorner()
    }
    for (r in 0 until 3) {
        Row(Modifier.fillMaxWidth()) {
            val digits: @Composable RowScope.() -> Unit = {
                for (c in 0 until 3) {
                    DigitCell(
                        d = r * 3 + c + 1,
                        notesMode = notesMode || cornerMode,
                        remaining = remaining,
                        armedDigit = armedDigit,
                        checkedDigit = checkedDigit,
                        digitsEnabled = digitsEnabled,
                        dimCompleted = dimCompleted,
                        onDigit = onDigit,
                        onPencilDigit = onPencilDigit,
                    )
                }
            }
            val side: @Composable RowScope.() -> Unit = {
            when (r) {
                0 -> {
                    ModeCell(
                        active = valueMode,
                        icon = Icons.Filled.Dialpad,
                        label = "value",
                        description = if (valueMode) "Value entry on" else "Value entry off",
                        activeTint = BoardColors.given,
                        onClick = onSelectValue,
                    )
                    ToolCell(
                        icon = Icons.Filled.Undo,
                        description = "Undo",
                        enabled = canUndo,
                        onClick = onUndo,
                    )
                }
                1 -> {
                    ModeCell(
                        active = centerActive,
                        icon = Icons.Filled.Edit,
                        label = "center",
                        description = if (centerActive) "Center marks on" else "Center marks off",
                        activeTint = BoardColors.entry,
                        onClick = onToggleNotes,
                    )
                    ToolCell(
                        icon = Icons.Filled.Redo,
                        description = "Redo",
                        enabled = canRedo,
                        onClick = onRedo,
                    )
                }
                else -> {
                    ModeCell(
                        active = cornerActive,
                        icon = Icons.Filled.NorthEast,
                        label = "corner",
                        description = if (cornerActive) "Corner marks on" else "Corner marks off",
                        activeTint = BoardColors.complete,
                        onClick = onToggleCorner,
                    )
                    SwitchCell(modeAbbr = modeAbbr, onSwitchMode = onSwitchMode)
                }
            }
            }
            if (buttonsLeft) {
                side()
                digits()
            } else {
                digits()
                side()
            }
        }
    }
}
/** One digit key: tap enters, long-press forces a pencil mark. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.DigitCell(
    d: Int,
    notesMode: Boolean,
    remaining: IntArray?,
    armedDigit: Int?,
    checkedDigit: Int?,
    digitsEnabled: Boolean,
    dimCompleted: Boolean,
    onDigit: (Int) -> Unit,
    onPencilDigit: (Int) -> Unit,
) {
                        val checked = armedDigit == d || checkedDigit == d
                        val exhausted = dimCompleted && (remaining?.getOrNull(d) == 0) && !checked
                        Box(
                            Modifier
                                .weight(1f)
                                .height(52.dp)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("digit_$d")
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
                    // Big digit only; completed digits dim via [remaining].
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
                }
    }

/**
 * One key of the mode-radio column (OpenSudoku's enter_number / primary_mark /
 * secondary_mark): exactly one of value/center/corner renders checked.
 */
@Composable
private fun RowScope.ModeCell(
    active: Boolean,
    icon: ImageVector,
    label: String,
    description: String,
    activeTint: Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .weight(1f)
            .height(52.dp)
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .testTag("mode_$label")
            .background(
                if (active) BoardColors.selection
                else BoardColors.cellLine.copy(alpha = 0.45f)
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon, null,
                tint = if (active) activeTint else BoardColors.dim,
                modifier = Modifier.size(22.dp),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) BoardColors.dim
                else BoardColors.dim.copy(alpha = 0.45f),
            )
        }
    }
}

/** Undo/redo key in the misc column. */
@Composable
private fun RowScope.ToolCell(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .weight(1f)
            .height(52.dp)
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .testTag("tool_$description")
            .background(BoardColors.cellLine.copy(alpha = 0.45f))
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, null,
            tint = if (enabled) BoardColors.given else BoardColors.cellLine,
        )
    }
}

/** Input-mode switcher key (OpenSudoku's switch_input_mode). */
@Composable
private fun RowScope.SwitchCell(
    modeAbbr: String?,
    onSwitchMode: (() -> Unit)?,
) {
    if (modeAbbr != null && onSwitchMode != null) {
        Box(
            Modifier
                .weight(1f)
                .height(52.dp)
                .padding(3.dp)
                .clip(RoundedCornerShape(10.dp))
                .testTag("switchInputMode")
                .background(BoardColors.cellLine.copy(alpha = 0.45f))
                .semantics(mergeDescendants = true) {
                    contentDescription = "Switch input mode"
                    role = Role.Button
                }
                .clickable(onClick = onSwitchMode),
            contentAlignment = Alignment.Center,
        ) {
            Text(modeAbbr, style = MaterialTheme.typography.titleMedium, color = BoardColors.dim)
        }
    } else {
        Spacer(Modifier.weight(1f))
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
    onToggleCorner: () -> Unit,
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
                onToggleCorner = onToggleCorner,
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
