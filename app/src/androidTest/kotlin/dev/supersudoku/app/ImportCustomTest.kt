package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Import + custom puzzles: field, import action, invalid input errors. */
@RunWith(AndroidJUnit4::class)
class ImportCustomTest : E2eHelpers() {

    private fun openImport() {
        waitForTag("homeImport")
        compose.onNodeWithTag("homeImport").performClick()
        waitForText("Import puzzle")
    }

    @Test
    fun importScreen_showsFieldAndButton() {
        openImport()
        compose.onNodeWithTag("importUrl").assertIsDisplayed()
        compose.onNodeWithTag("importButton").assertIsDisplayed()
    }

    @Test
    fun import_emptyUrl_keepsImportDisabled() {
        openImport()
        // Button exists; with empty URL the import is a no-op.
        compose.onNodeWithTag("importButton").assertIsDisplayed()
        compose.onNodeWithText("Import puzzle").assertIsDisplayed()
    }

    @Test
    fun import_garbageUrl_showsError() {
        openImport()
        compose.onNodeWithTag("importUrl").performTextInput("not a penpa url")
        compose.onNodeWithTag("importButton").performClick()
        // Garbage input yields PenpaImportError("no penpa puzzle payload ...").
        waitForText("no penpa puzzle payload", substring = true, timeoutMs = 20_000)
    }

    @Test
    fun import_backReturnsHome() {
        openImport()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("homeSuper")
    }
}
