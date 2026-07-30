package com.cydoniancitizen.mindora

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cydoniancitizen.mindora.feature.pathdetail.PathDetailScreen
import com.cydoniancitizen.mindora.feature.pathdetail.PathDetailUiState
import com.cydoniancitizen.mindora.feature.pathdetail.PathStepType
import com.cydoniancitizen.mindora.feature.pathdetail.PathStepUiModel
import com.cydoniancitizen.mindora.feature.practice.MindfulnessPathSummary
import com.cydoniancitizen.mindora.feature.practice.PracticeScreen
import com.cydoniancitizen.mindora.feature.practice.PracticeUiState
import com.cydoniancitizen.mindora.navigation.BreathingExerciseDestination
import com.cydoniancitizen.mindora.navigation.FreeMeditationDestination
import com.cydoniancitizen.mindora.navigation.GuidedMeditationDestination
import com.cydoniancitizen.mindora.navigation.MindoraDestination
import com.cydoniancitizen.mindora.navigation.PathDetailDestination
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import org.junit.Rule
import org.junit.Test

class M7NavigationFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedPathRoutesEveryStepAndBackReturnsToExistingPath() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            MindoraTheme {
                navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = MindoraDestination.PRACTICE.route,
                ) {
                    composable(MindoraDestination.PRACTICE.route) {
                        PracticeScreen(
                            uiState = practiceState,
                            onRetry = {},
                            onFreeMeditationClick = {},
                            onBreathingExerciseClick = {},
                            onPathClick = {
                                navController.navigate(PathDetailDestination.createRoute(it))
                            },
                        )
                    }
                    composable(PathDetailDestination.route) {
                        PathDetailScreen(
                            uiState = pathState,
                            onNavigateBack = navController::navigateUp,
                            onStepClick = { type, id ->
                                navController.navigate(
                                    when (type) {
                                        PathStepType.GUIDED_MEDITATION ->
                                            GuidedMeditationDestination.createRoute(id)

                                        PathStepType.FREE_MEDITATION ->
                                            FreeMeditationDestination.createLinkedRoute(id)

                                        PathStepType.BREATHING_EXERCISE ->
                                            BreathingExerciseDestination.createLinkedRoute(id)
                                    },
                                )
                            },
                        )
                    }
                    composable(FreeMeditationDestination.linkedRoute) {
                        Text("Linked free destination")
                    }
                    composable(BreathingExerciseDestination.linkedRoute) {
                        Text("Linked breathing destination")
                    }
                    composable(GuidedMeditationDestination.route) {
                        Text("Guided destination")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Test path").performClick()
        composeRule.onNodeWithText("Test path description.").assertIsDisplayed()

        composeRule.onNodeWithText("2. Free step").performScrollTo().performClick()
        composeRule.onNodeWithText("Linked free destination").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigateUp() }
        composeRule.onNodeWithText("Test path description.").assertIsDisplayed()

        composeRule.onNodeWithText("3. Breathing step").performScrollTo().performClick()
        composeRule.onNodeWithText("Linked breathing destination").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigateUp() }

        composeRule.onNodeWithText("1. Guided step").performScrollTo().performClick()
        composeRule.onNodeWithText("Guided destination").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigateUp() }
        composeRule.onNodeWithText("Test path description.").assertIsDisplayed()

        composeRule.runOnIdle { navController.navigateUp() }
        composeRule.onNodeWithText("Mindfulness paths").assertIsDisplayed()
    }

    private companion object {
        val practiceState = PracticeUiState.Content(
            listOf(
                MindfulnessPathSummary(
                    id = "test-path",
                    title = "Test path",
                    description = "Test path description.",
                    completedSteps = 1,
                    totalSteps = 3,
                    progressFraction = 1f / 3f,
                ),
            ),
        )
        val pathState = PathDetailUiState.Content(
            title = "Test path",
            description = "Test path description.",
            completedSteps = 1,
            totalSteps = 3,
            progressFraction = 1f / 3f,
            steps = listOf(
                step("guided-step", "Guided step", PathStepType.GUIDED_MEDITATION, 1, true),
                step("free-step", "Free step", PathStepType.FREE_MEDITATION, 2, false),
                step(
                    "breathing-step",
                    "Breathing step",
                    PathStepType.BREATHING_EXERCISE,
                    3,
                    false,
                ),
            ),
        )

        fun step(
            id: String,
            title: String,
            type: PathStepType,
            ordinal: Int,
            completed: Boolean,
        ) = PathStepUiModel(
            id = id,
            title = title,
            description = "$title description.",
            type = type,
            ordinal = ordinal,
            completed = completed,
            displayDuration = Duration.ofMinutes(1),
        )
    }
}
