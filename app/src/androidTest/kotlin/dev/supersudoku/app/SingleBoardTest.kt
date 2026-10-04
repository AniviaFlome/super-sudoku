package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Single 9x9 boards: standalone variants, normal practice, custom play. */
@RunWith(AndroidJUnit4::class)
class SingleBoardTest : E2eHelpers() {

    private fun openStandaloneOriginal() {
        openVariants()
        // Tap the first variant row to reach its games list.
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
    }

    @Test
    fun standalone_boardRendersWithKeypad() {
        openStandaloneOriginal()
        compose.onNodeWithTag("singleBoard").assertIsDisplayed()
        for (d in 1..9) {
            compose.onNodeWithTag("digit_$d").assertIsDisplayed()
        }
    }

    @Test
    fun standalone_tappingBoardKeepsItVisible() {
        openStandaloneOriginal()
        compose.onNodeWithTag("singleBoard").performClick()
        compose.onNodeWithTag("singleBoard").assertIsDisplayed()
    }

    @Test
    fun normalLibrary_opensAndShowsNewGame() {
        openNormal()
        // New-game buttons are labelled "+ New <difficulty> game".
        compose.onNodeWithText("New", substring = true).assertIsDisplayed()
    }

    @Test
    fun normal_newEasyGame_opensBoard() {
        openNormal()
        waitForText("New", substring = true)
        compose.onNodeWithText("New", substring = true, useUnmergedTree = true).performClick()
        waitForTag("singleBoard", timeoutMs = 30_000)
        compose.onNodeWithTag("singleBoard").assertIsDisplayed()
    }
}
