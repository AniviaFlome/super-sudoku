package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Super ring board: 33x33 overview renders, pinch-zoom is gesture-only, focus opens. */
@RunWith(AndroidJUnit4::class)
class SuperBoardTest : E2eHelpers() {

    @Test
    fun overview_rendersWithoutZoomButtons() {
        openSuperOriginal()
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
        // Zoom is pinch-gesture only now; the buttons are gone.
        compose.onNodeWithTag("zoomIn", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("zoomOut", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun overview_showsKeypad() {
        openSuperOriginal()
        // Keypad digits are always visible under the board.
        for (d in 1..9) {
            compose.onNodeWithTag("digit_$d").assertIsDisplayed()
        }
    }

    @Test
    fun tappingOverview_keepsOverviewVisible() {
        openSuperOriginal()
        compose.onNodeWithTag("overviewBoard").performClick()
        // A single tap selects but never leaves the overview.
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
    }
}
