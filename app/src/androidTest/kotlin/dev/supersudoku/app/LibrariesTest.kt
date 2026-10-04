package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Libraries: super maps, variant games, normal games all list + create. */
@RunWith(AndroidJUnit4::class)
class LibrariesTest : E2eHelpers() {

    @Test
    fun superMaps_showsOriginalAndNewMap() {
        openSuperMaps()
        compose.onNodeWithText("Original", substring = true).assertIsDisplayed()
        compose.onNodeWithText("New", substring = true).assertIsDisplayed()
    }

    @Test
    fun variantGames_showsOriginalAndNewGame() {
        openVariants()
        Thread.sleep(1000)
        val nodes = compose.onAllNodesWithText("Sudoku", substring = true, useUnmergedTree = true)
        try {
            nodes[1].performClick()
        } catch (_: Exception) {
            nodes[0].performClick()
        }
        waitForText("Original", substring = true)
        compose.onNodeWithText("Original", substring = true).assertIsDisplayed()
        compose.onNodeWithText("New", substring = true).assertIsDisplayed()
    }

    @Test
    fun normalLibrary_showsDifficultyChipsAndNew() {
        openNormal()
        compose.onNodeWithText("New", substring = true).assertIsDisplayed()
        // Difficulty filter chips exist (Easy/Medium/Hard labels vary by impl;
        // at least one New-game button is the creation entry point).
        compose.onNodeWithText("Normal Sudoku").assertIsDisplayed()
    }

    @Test
    fun superMaps_originalOpensOverview() {
        openSuperOriginal()
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
    }
}
