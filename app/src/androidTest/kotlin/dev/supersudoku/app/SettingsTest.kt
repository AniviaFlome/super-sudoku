package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Settings: every section renders, back returns to the entry screen. */
@RunWith(AndroidJUnit4::class)
class SettingsTest : E2eHelpers() {

    private fun openSettings() {
        waitForTag("homeSettings")
        compose.onNodeWithTag("homeSettings").performClick()
        waitForText("Settings")
    }

    @Test
    fun settings_showsAllSections() {
        openSettings()
        compose.onNodeWithText("Input", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pencil marks", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Display", substring = true).performScrollTo().assertIsDisplayed()
        // Exact match: the Mistakes row shows "Board conflicts" as its value.
        compose.onNodeWithText("Board").performScrollTo().assertIsDisplayed()
        // Exact match: the choice row is titled "Color theme", not "Theme".
        compose.onNodeWithText("Theme").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settings_showsOrientationAndThemeChoices() {
        openSettings()
        compose.onNodeWithText("Orientation", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Color theme", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settings_backReturnsHome() {
        openSettings()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("homeSuper")
    }
}
