package dev.supersudoku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** App-level navigation: every home entry opens and back returns home. */
@RunWith(AndroidJUnit4::class)
class NavigationTest : E2eHelpers() {

    @Test
    fun home_showsAllEntries() {
        waitForTag("homeSuper")
        compose.onNodeWithTag("homeSuper").assertIsDisplayed()
        compose.onNodeWithTag("homeVariants").assertIsDisplayed()
        compose.onNodeWithTag("homeNormal").assertIsDisplayed()
        compose.onNodeWithTag("homeRules").assertIsDisplayed()
    }

    @Test
    fun home_toSuperMaps_andBack() {
        openSuperMaps()
        compose.onNodeWithText("Super maps").assertIsDisplayed()
        compose.onNodeWithText("Original", substring = true).assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("homeSuper")
    }

    @Test
    fun home_toVariants_andBack() {
        openVariants()
        compose.onNodeWithText("Each variant as its own", substring = true).assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("homeVariants")
    }

    @Test
    fun home_toNormal_andBack() {
        openNormal()
        compose.onNodeWithText("Classic generated puzzles", substring = true).assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("homeNormal")
    }

    @Test
    fun home_toRules_andBack() {
        openRules()
        compose.onNodeWithText("carykh", substring = true).assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("homeRules")
    }

    @Test
    fun superOriginal_backGoesToMaps_thenHome() {
        openSuperOriginal()
        compose.onNodeWithTag("overviewBoard").assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText("Super maps")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("homeSuper")
    }

    @Test
    fun home_toSettings_fromTopBar() {
        waitForTag("homeSettings")
        compose.onNodeWithTag("homeSettings").performClick()
        waitForText("Settings")
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }
}
