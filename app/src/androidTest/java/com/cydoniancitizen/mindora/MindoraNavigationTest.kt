package com.cydoniancitizen.mindora

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindoraNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun topLevelDestinationsNavigateWithoutDuplicatingActiveDestination() {
        composeRule.onAllNodesWithText("Home").assertCountEquals(2)

        composeRule.onNodeWithContentDescription("Navigate to Practice").performClick()
        composeRule.onAllNodesWithText("Practice").assertCountEquals(2)

        composeRule.onNodeWithContentDescription("Navigate to History").performClick()
        composeRule.onAllNodesWithText("History").assertCountEquals(2)

        composeRule.onNodeWithContentDescription("Navigate to Settings").performClick()
        composeRule.onAllNodesWithText("Settings").assertCountEquals(2)
        composeRule.onNodeWithContentDescription("Navigate to Settings").performClick()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onAllNodesWithText("Home").assertCountEquals(2)
    }

    @Test
    fun freeMeditationOpensFromPracticeAndHidesBottomNavigation() {
        composeRule.onNodeWithContentDescription("Navigate to Practice").performClick()
        composeRule.onNodeWithText("Free meditation").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Select a duration").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Navigate to Home").assertDoesNotExist()
        composeRule.onNodeWithText("10 minutes").assertIsSelected()
    }

    @Test
    fun startShowsActiveSessionAndBackRequestsConfirmation() {
        composeRule.onNodeWithContentDescription("Navigate to Practice").performClick()
        composeRule.onNodeWithText("Free meditation").performClick()
        composeRule.onNodeWithText("Start").performClick()

        composeRule.onNodeWithText("Running").assertIsDisplayed()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("End this session?").assertIsDisplayed()
        composeRule.onNodeWithText("Continue session").assertIsDisplayed()
        composeRule.onNodeWithText("End session").assertIsDisplayed()
    }
}
