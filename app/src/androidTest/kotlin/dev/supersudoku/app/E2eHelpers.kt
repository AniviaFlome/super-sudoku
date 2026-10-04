package dev.supersudoku.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule

/**
 * Shared helpers for all e2e suites.
 *
 * Conventions: production code exposes stable testTags for canvases,
 * keypad keys and nav entry points (see Home/SuperScreen/SingleBoard/
 * Keypad/CustomPlay). Everything else is matched by visible text so the
 * tests break when user-facing copy changes.
 */
open class E2eHelpers {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    fun waitForTag(tag: String, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMillis = timeoutMs) {
            try {
                compose.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    fun waitForText(text: String, substring: Boolean = false, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMillis = timeoutMs) {
            try {
                compose.onNodeWithText(text, substring = substring, useUnmergedTree = true)
                    .assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    /** Home -> Super maps library. */
    fun openSuperMaps() {
        waitForTag("homeSuper")
        compose.onNodeWithTag("homeSuper").performClick()
        waitForText("Super maps")
    }

    /** Home -> Super maps -> Original puzzle overview board. */
    fun openSuperOriginal() {
        openSuperMaps()
        waitForText("Original", substring = true)
        compose.onNodeWithText("Original", substring = true, useUnmergedTree = true)
            .performClick()
        waitForTag("overviewBoard")
    }

    /** Home -> Variants list. */
    fun openVariants() {
        waitForTag("homeVariants")
        compose.onNodeWithTag("homeVariants").performClick()
        waitForText("Variants")
    }

    /** Home -> Normal Sudoku library. */
    fun openNormal() {
        waitForTag("homeNormal")
        compose.onNodeWithTag("homeNormal").performClick()
        waitForText("Normal Sudoku")
    }

    /** Home -> How to play. */
    fun openRules() {
        waitForTag("homeRules")
        compose.onNodeWithTag("homeRules").performClick()
        waitForText("How to play")
    }

    /** Open the first standalone variant game (or its Original) if present. */
    fun openFirstVariantGame(): Boolean {
        openVariants()
        // Variant rows are FolderRows with per-variant names; tap the first
        // one by waiting for the explainer text then clicking any variant row.
        waitForText("Each variant as its own", substring = true)
        Thread.sleep(800)
        val nodes = compose.onAllNodesWithText("Sudoku", substring = true, useUnmergedTree = true)
        return try {
            nodes[0].performClick()
            true
        } catch (_: Exception) {
            false
        }
    }
}
