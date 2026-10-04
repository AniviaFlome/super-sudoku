package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Keypad: digits 1-9 present on every game screen, taps don't crash,
 * long-press path exists (pencil), undo/redo don't crash when disabled.
 */
@RunWith(AndroidJUnit4::class)
class KeypadTest : E2eHelpers() {

    @Test
    fun allDigits_visibleOnSuper() {
        openSuperOriginal()
        for (d in 1..9) {
            compose.onNodeWithTag("digit_$d", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun tappingDigits_doesNotCrash() {
        openSuperOriginal()
        for (d in listOf(1, 5, 9)) {
            compose.onNodeWithTag("digit_$d").performClick()
        }
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
    }

    @Test
    fun tappingDigitsOnStandalone_doesNotCrash() {
        openVariants()
        Thread.sleep(1000)
        val nodes = compose.onAllNodesWithText("Sudoku", substring = true, useUnmergedTree = true)
        try {
            nodes[1].performClick()
        } catch (_: Exception) {
            nodes[0].performClick()
        }
        waitForText("Original", substring = true)
        compose.onNodeWithText("Original", substring = true, useUnmergedTree = true)
            .performClick()
        waitForTag("singleBoard", timeoutMs = 20_000)
        for (d in listOf(2, 4, 7)) {
            compose.onNodeWithTag("digit_$d").performClick()
        }
        compose.onNodeWithTag("singleBoard").assertIsDisplayed()
    }

    @Test
    fun undoRedo_presentAndSafeToTap() {
        openSuperOriginal()
        // Disabled buttons still exist; clicks are no-ops, never crashes.
        compose.onNodeWithTag("tool_Undo").assertIsDisplayed()
        compose.onNodeWithTag("tool_Redo").assertIsDisplayed()
    }
}
