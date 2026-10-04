package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Mistakes + highlights settings affect board screens without crashing. */
@RunWith(AndroidJUnit4::class)
class MistakesHighlightsTest : E2eHelpers() {

    private fun openSettingsFromHome() {
        waitForTag("homeSettings")
        compose.onNodeWithTag("homeSettings").performClick()
        waitForText("Settings")
    }

    @Test
    fun mistakesSection_listsModes() {
        openSettingsFromHome()
        compose.onNodeWithText("Mistakes", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Highlight same digits", substring = true).assertIsDisplayed()
    }

    @Test
    fun togglingHighlightSameDigits_keepsSettingsVisible() {
        openSettingsFromHome()
        compose.onNodeWithText("Highlight same digits", substring = true).performClick()
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithText("Highlight same digits", substring = true).performClick()
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun inputSwitches_visible() {
        openSettingsFromHome()
        compose.onNodeWithText("Tap valued cell arms it", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Double-tap focuses grid", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Clear peer marks on entry", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Dim completed digits", substring = true).assertIsDisplayed()
    }
}
