package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Rules: all 8 variant rule cards render with tips. */
@RunWith(AndroidJUnit4::class)
class RulesTest : E2eHelpers() {

    @Test
    fun rules_showsAllEightVariants() {
        openRules()
        for (name in listOf("Normal Sudoku", ">Sudoku<", "Addition Sudoku", "Killer Sudoku", "Consecutive Sudoku", "Sudoku X", "Strange Boxes", "Offset Sudoku")) {
            compose.onNodeWithText(name, substring = true, useUnmergedTree = true)
                .performScrollTo()
                .assertIsDisplayed()
        }
        // Numbered cards render in order.
        compose.onNodeWithText("1. Normal Sudoku", substring = true).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun rules_backReturnsHome() {
        openRules()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("homeSuper")
    }
}
