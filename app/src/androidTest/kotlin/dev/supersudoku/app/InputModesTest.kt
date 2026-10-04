package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Input modes: value / center / corner marks, undo/redo, mode switcher. */
@RunWith(AndroidJUnit4::class)
class InputModesTest : E2eHelpers() {

    @Test
    fun keypad_modesAndToolsVisibleOnSuper() {
        openSuperOriginal()
        // Mode keys (labels come from Keypad.kt: value / center / corner).
        compose.onNodeWithTag("mode_value").assertIsDisplayed()
        compose.onNodeWithTag("mode_center").assertIsDisplayed()
        compose.onNodeWithTag("mode_corner").assertIsDisplayed()
        // Undo / redo tools.
        compose.onNodeWithTag("tool_Undo").assertIsDisplayed()
        compose.onNodeWithTag("tool_Redo").assertIsDisplayed()
        // Input-mode switcher.
        compose.onNodeWithTag("switchInputMode").assertIsDisplayed()
    }

    @Test
    fun switchToCenterMarks_staysOnBoard() {
        openSuperOriginal()
        compose.onNodeWithTag("mode_center").performClick()
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
        compose.onNodeWithTag("mode_value").performClick()
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
    }

    @Test
    fun switchToCornerMarks_staysOnBoard() {
        openSuperOriginal()
        compose.onNodeWithTag("mode_corner").performClick()
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
        compose.onNodeWithTag("mode_value").performClick()
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
    }

    @Test
    fun switchInputMode_cyclesModes() {
        openSuperOriginal()
        compose.onNodeWithTag("switchInputMode").performClick()
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
        compose.onNodeWithTag("switchInputMode").performClick()
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
    }
}
