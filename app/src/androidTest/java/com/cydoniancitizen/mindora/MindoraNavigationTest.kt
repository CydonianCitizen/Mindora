package com.cydoniancitizen.mindora

import android.net.Uri

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cydoniancitizen.mindora.navigation.GuidedMeditationDestination
import com.cydoniancitizen.mindora.navigation.BreathingExerciseDestination
import com.cydoniancitizen.mindora.navigation.FreeMeditationDestination
import com.cydoniancitizen.mindora.navigation.PathDetailDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindoraNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun guidedRouteSafelyEncodesAndDecodesGlobalStepId() {
        val stepId = "guided/step with space"
        val route = GuidedMeditationDestination.createRoute(stepId)

        assertEquals("practice/guided/guided%2Fstep%20with%20space", route)
        assertEquals(stepId, Uri.decode(route.substringAfter("practice/guided/")))
    }

    @Test
    fun pathAndLinkedRuntimeRoutesSafelyEncodeContentIds() {
        val id = "path/step with space"

        assertEquals(
            "practice/path/path%2Fstep%20with%20space",
            PathDetailDestination.createRoute(id),
        )
        assertEquals(
            "practice/free-meditation/path%2Fstep%20with%20space",
            FreeMeditationDestination.createLinkedRoute(id),
        )
        assertEquals(
            "practice/breathing/path%2Fstep%20with%20space",
            BreathingExerciseDestination.createLinkedRoute(id),
        )
    }
    @Test
    fun topLevelDestinationsNavigateWithoutDuplicatingActiveDestination() {
        composeRule.onAllNodesWithText("Home").assertCountEquals(2)

        composeRule.onNodeWithContentDescription(
            "Navigate to Practice",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onAllNodesWithText("Practice").assertCountEquals(2)

        composeRule.onNodeWithContentDescription(
            "Navigate to History",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onAllNodesWithText("History").assertCountEquals(2)

        composeRule.onNodeWithContentDescription(
            "Navigate to Settings",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onAllNodesWithText("Settings").assertCountEquals(2)
        composeRule.onNodeWithContentDescription(
            "Navigate to Settings",
            useUnmergedTree = true,
        ).performClick()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onAllNodesWithText("Home").assertCountEquals(2)
    }

    @Test
    fun setGoalActionOpensSettingsAndKeepsBottomNavigation() {
        composeRule.onNodeWithText("Set a goal").performClick()

        composeRule.onAllNodesWithText("Settings").assertCountEquals(2)
        composeRule.onNodeWithContentDescription(
            "Navigate to Home",
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun freeMeditationOpensFromPracticeAndHidesBottomNavigation() {
        composeRule.onNodeWithContentDescription(
            "Navigate to Practice",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNodeWithText("Free meditation").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Select a duration").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Navigate to Home",
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithText("10 minutes").assertIsSelected()
    }

    @Test
    fun breathingExerciseOpensFromPracticeAndHidesBottomNavigation() {
        composeRule.onNodeWithContentDescription(
            "Navigate to Practice",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNodeWithText("Free meditation").assertIsDisplayed()
        composeRule.onNodeWithText("Breathing exercise").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Follow the breathing rhythm for five cycles.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("5 cycles").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Navigate to Home",
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun breathingStartShowsPhaseAndBackRequestsConfirmation() {
        composeRule.onNodeWithContentDescription(
            "Navigate to Practice",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNodeWithText("Breathing exercise").performClick()
        composeRule.onNodeWithText("Start").performClick()

        composeRule.onNodeWithText("Running").assertIsDisplayed()
        composeRule.onNodeWithText("Inhale").assertIsDisplayed()
        composeRule.onNodeWithText("Cycle 1 of 5").assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("End this exercise?").assertIsDisplayed()
        composeRule.onNodeWithText("Continue exercise").assertIsDisplayed()
        composeRule.onNodeWithText("End exercise").assertIsDisplayed()
    }

    @Test
    fun startShowsActiveSessionAndBackRequestsConfirmation() {
        composeRule.onNodeWithContentDescription(
            "Navigate to Practice",
            useUnmergedTree = true,
        ).performClick()
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
