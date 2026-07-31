package com.cydoniancitizen.mindora

import android.content.Intent
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
import com.cydoniancitizen.mindora.core.reminder.ReminderNotificationPublisher
import com.cydoniancitizen.mindora.navigation.BreathingExerciseDestination
import com.cydoniancitizen.mindora.navigation.FreeMeditationDestination
import com.cydoniancitizen.mindora.navigation.GuidedMeditationDestination
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
    fun coldLaunchOpensPracticeAsStartDestination() {
        composeRule.onAllNodesWithText("Practice").assertCountEquals(2)
        composeRule.onAllNodesWithText("Practice")[1].assertIsSelected()
        composeRule.onNodeWithText("Home").assertDoesNotExist()
        composeRule.onNodeWithText("Weekly goal").assertIsDisplayed()
    }

    @Test
    fun reminderNotificationIntentNavigatesToPracticeFromWarmState() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Practice goals").assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.intentFlow.value = Intent(activity, MainActivity::class.java).apply {
                action = ReminderNotificationPublisher.ACTION_REMINDER_NOTIFICATION
            }
        }

        composeRule.onAllNodesWithText("Practice").assertCountEquals(2)
        composeRule.onAllNodesWithText("Practice")[1].assertIsSelected()
        composeRule.onNodeWithText("Home").assertDoesNotExist()
    }

    @Test
    fun topLevelDestinationsNavigateWithoutDuplicatingActiveDestination() {
        composeRule.onAllNodesWithText("Practice").assertCountEquals(2)
        composeRule.onAllNodesWithText("Practice")[1].assertIsSelected()

        composeRule.onAllNodesWithText("Practice")[1].performClick()
        composeRule.onAllNodesWithText("Practice").assertCountEquals(2)

        composeRule.onNodeWithText("History").performClick()
        composeRule.onAllNodesWithText("History").assertCountEquals(2)
        composeRule.onAllNodesWithText("History")[1].assertIsSelected()

        composeRule.onNodeWithText("Practice").performClick()
        composeRule.onAllNodesWithText("Practice").assertCountEquals(2)
        composeRule.onAllNodesWithText("Practice")[1].assertIsSelected()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onAllNodesWithText("Settings").assertCountEquals(2)
        composeRule.onAllNodesWithText("Settings")[1].assertIsSelected()
        composeRule.onNodeWithText("Practice goals").assertIsDisplayed()

        composeRule.onNodeWithText("Practice").performClick()
        composeRule.onAllNodesWithText("Practice").assertCountEquals(2)
        composeRule.onAllNodesWithText("Practice")[1].assertIsSelected()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Home").assertDoesNotExist()
    }

    @Test
    fun freeMeditationOpensFromPracticeAndBackReturnsToPractice() {
        composeRule.onNodeWithText("Free meditation").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Duration").assertIsDisplayed()
        composeRule.onNodeWithText("Practice").assertDoesNotExist()
        composeRule.onNodeWithText("10 minutes").assertIsSelected()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onAllNodesWithText("Practice").assertCountEquals(2)
    }

    @Test
    fun breathingExerciseOpensFromPracticeAndHidesBottomNavigation() {
        composeRule.onNodeWithText("Free meditation").assertIsDisplayed()
        composeRule.onNodeWithText("Breathing exercise").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Follow the breathing rhythm for five cycles.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("5 cycles").assertIsDisplayed()
        composeRule.onNodeWithText("Practice").assertDoesNotExist()
    }

    @Test
    fun breathingStartShowsPhaseAndBackRequestsConfirmation() {
        composeRule.onNodeWithText("Breathing exercise").performClick()
        composeRule.onNodeWithText("Start").performClick()

        composeRule.onNodeWithText("Running").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Inhale", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cycle 1 of 5", substring = true)
            .assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("End this exercise?").assertIsDisplayed()
        composeRule.onNodeWithText("Continue exercise").assertIsDisplayed()
        composeRule.onNodeWithText("End exercise").assertIsDisplayed()
    }

    @Test
    fun startShowsActiveSessionAndBackRequestsConfirmation() {
        composeRule.onNodeWithText("Free meditation").performClick()
        composeRule.onNodeWithText("Start session").performClick()

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
