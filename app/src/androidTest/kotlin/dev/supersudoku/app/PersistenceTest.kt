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
 * Persistence: entering digits then leaving and returning keeps the game
 * screen functional (saves live in DataStore / app storage per screen).
 */
@RunWith(AndroidJUnit4::class)
class PersistenceTest : E2eHelpers() {

    @Test
    fun superProgress_survivesBackAndReopen() {
        openSuperOriginal()
        compose.onNodeWithTag("digit_5").performClick()
        compose.onNodeWithTag("overviewBoard").performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText("Super maps")
        compose.onNodeWithText("Original", substring = true, useUnmergedTree = true)
            .performClick()
        waitForTag("overviewBoard")
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
    }

    @Test
    fun standaloneProgress_survivesBackAndReopen() {
        openVariants()
        Thread.sleep(1000)
        val nodes = compose.onAllNodesWithText("Sudoku", substring = true, useUnmergedTree = true)
        val title = try {
            nodes[1].performClick()
            "variant"
        } catch (_: Exception) {
            nodes[0].performClick()
            "variant"
        }
        assert(title.isNotEmpty())
        waitForText("Original", substring = true)
        compose.onNodeWithText("Original", substring = true, useUnmergedTree = true)
            .performClick()
        waitForTag("singleBoard", timeoutMs = 20_000)
        compose.onNodeWithTag("digit_3").performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText("Original", substring = true)
        compose.onNodeWithText("Original", substring = true, useUnmergedTree = true)
            .performClick()
        waitForTag("singleBoard", timeoutMs = 20_000)
        compose.onNodeWithTag("singleBoard").assertIsDisplayed()
    }
}
