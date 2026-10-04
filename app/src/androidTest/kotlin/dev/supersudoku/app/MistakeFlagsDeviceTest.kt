package dev.supersudoku.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device counterpart of MistakeFlagsTest: real taps on a fresh generated
 * variant game, asserting the board's flagged count becomes visible.
 *
 * Strategy: mint a pristine generated game (no user entries, so the first
 * fill of each cell sticks — enter() toggles an already-shown digit), pin
 * cell-first input, then fill every cell with 5. Givens keep their values;
 * every other cell takes 5. A fresh game has 28-44 empties spread over 9
 * rows, so by pigeonhole some row holds 4+ of our 5s: a certain row-unit
 * conflict, hence >= 2 flagged cells in CONFLICTS mode.
 */
@RunWith(AndroidJUnit4::class)
class MistakeFlagsDeviceTest : E2eHelpers() {

    /** Max N over boards reporting "Single board, N flagged". */
    private fun flaggedCount(): Int? {
        val key = SemanticsProperties.ContentDescription
        val descs = compose.onAllNodesWithContentDescription(
            "flagged", substring = true, useUnmergedTree = true
        ).fetchSemanticsNodes().flatMap { node ->
            @Suppress("UNCHECKED_CAST")
            (node.config.firstOrNull { it.key == key }?.value as? List<String>).orEmpty()
        }
        return descs.mapNotNull {
            Regex("""(\d+) flagged""").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.maxOrNull()
    }

    /** Pin cell-first input via the keypad mode switcher (label Cell/Digit/Popup). */
    private fun pinCellFirstMode() {
        repeat(3) {
            @Suppress("UNCHECKED_CAST")
            val label = (compose.onNodeWithTag("switchInputMode", useUnmergedTree = true)
                .fetchSemanticsNode()
                .config.firstOrNull { it.key == SemanticsProperties.Text }?.value as? List<*>)
                ?.joinToString()
            if (label == "Cell") return
            compose.onNodeWithTag("switchInputMode", useUnmergedTree = true).performClick()
        }
    }

    /** Pin CONFLICTS mistake mode through the settings UI. */
    private fun pinMistakeModeConflicts() {
        waitForTag("homeSettings")
        compose.onNodeWithTag("homeSettings").performClick()
        waitForText("Settings")
        compose.onNodeWithText("Mistakes").performScrollTo().performClick()
        // Wait until the dropdown opens: then TWO "Board conflicts" nodes
        // exist (row value + menu option). Must use onAllNodes here — the
        // singular matcher throws on ambiguity, which waitForText swallows
        // as absent, so it can never succeed while the menu is open.
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Board conflicts", useUnmergedTree = true)
                .fetchSemanticsNodes().size >= 2
        }
        // Two "Board conflicts" nodes exist while the dropdown is open (the
        // row's current-value label plus the menu option); the menu option
        // renders last.
        val options = compose.onAllNodesWithText("Board conflicts", useUnmergedTree = true)
        options[options.fetchSemanticsNodes().size - 1].performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("homeSuper")
    }

    @Test
    fun conflictsMode_flagsDuplicates() {
        pinMistakeModeConflicts()

        openVariants()
        waitForText("Each variant as its own", substring = true)
        Thread.sleep(800)
        compose.onAllNodesWithText("Sudoku", substring = true, useUnmergedTree = true)[0]
            .performClick()
        waitForText("New", substring = true, timeoutMs = 30_000)
        compose.onNodeWithText("New", substring = true, useUnmergedTree = true).performClick()
        waitForTag("singleBoard", timeoutMs = 120_000)

        pinCellFirstMode()

        for (i in 0..80) {
            BoardTapHelper.tapSingleBoardCell(compose, i)
            compose.onNodeWithTag("digit_5", useUnmergedTree = true).performClick()
        }

        compose.waitUntil(timeoutMillis = 15_000) { (flaggedCount() ?: 0) >= 2 }
        val n = flaggedCount()
        assertTrue("expected >= 2 flagged cells in CONFLICTS mode, saw $n", (n ?: 0) >= 2)
    }
}
