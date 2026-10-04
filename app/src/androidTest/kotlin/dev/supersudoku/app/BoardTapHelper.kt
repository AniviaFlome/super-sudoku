package dev.supersudoku.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput

/**
 * Coordinate taps on board canvases.
 *
 * Board cells are drawn on a bare Canvas with no per-cell semantics, so tests
 * tap by pixel math. This mirrors the production hit-testing in
 * SingleBoard.kt exactly: the 9x9 grid is sized by min(width, height) and
 * centered with ox/oy offsets (`cell = min(w,h)/9`, `lx = (x-ox)/cell`).
 * Tapping a cell CENTER keeps every tap unambiguous (never on a grid line,
 * never in letterbox dead space).
 */
object BoardTapHelper {
    /**
     * Tap single-board cell [index] (0..80, row-major) by its center.
     *
     * @param rule the compose rule driving the test (E2eHelpers exposes it as `compose`).
     */
    fun tapSingleBoardCell(rule: AndroidComposeTestRule<*, MainActivity>, index: Int) {
        require(index in 0..80) { "cell index out of range: $index" }
        val tag = rule.onNodeWithTag("singleBoard", useUnmergedTree = true)
        val bounds = tag.fetchSemanticsNode().boundsInRoot
        // Mirror SingleBoard.kt: cell = min(w,h)/9, grid centered via ox/oy.
        val cell = minOf(bounds.width, bounds.height) / 9f
        val ox = (bounds.width - 9f * cell) / 2f
        val oy = (bounds.height - 9f * cell) / 2f
        val col = index % 9
        val row = index / 9
        tag.performTouchInput {
            click(Offset(ox + (col + 0.5f) * cell, oy + (row + 0.5f) * cell))
        }
    }
}
