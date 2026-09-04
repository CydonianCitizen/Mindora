package com.cydoniancitizen.mindora.feature.practice

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PracticeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedPathShowsProgressAndOpensExactPath() {
        var selectedPathId: String? = null
        composeRule.setContent {
            MindoraTheme {
                PracticeScreen(
                    uiState = PracticeUiState.Content(
                        listOf(
                            MindfulnessPathSummary(
                                id = "test-path",
                                title = "Test path",
                                description = "Test path description.",
                                completedSteps = 2,
                                totalSteps = 3,
                                progressFraction = 2f / 3f,
                            ),
                        ),
                    ),
                    onRetry = {},
                    onFreeMeditationClick = {},
                    onBreathingExerciseClick = {},
                    onPathClick = { selectedPathId = it },
                )
            }
        }

        composeRule.onNodeWithText("Test path").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Test path description.").assertIsDisplayed()
        composeRule.onNodeWithText("2 of 3 steps completed").assertIsDisplayed()
        assertEquals("test-path", selectedPathId)
    }

    @Test
    fun emptyCatalogueKeepsStandaloneActionsVisible() {
        composeRule.setContent {
            MindoraTheme {
                PracticeScreen(
                    uiState = PracticeUiState.Empty(),
                    onRetry = {},
                    onFreeMeditationClick = {},
                    onBreathingExerciseClick = {},
                    onPathClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Free meditation").assertIsDisplayed()
        composeRule.onNodeWithText("Breathing exercise").assertIsDisplayed()
        composeRule.onNodeWithText("About 50 seconds").assertIsDisplayed()
        composeRule.onNodeWithText("Mindfulness paths").assertDoesNotExist()
        composeRule.onNodeWithText("No mindfulness paths are included yet.").assertDoesNotExist()
    }

    @Test
    fun freeMeditationCardTriggersCallback() {
        var freeMeditationClickedCount = 0
        composeRule.setContent {
            MindoraTheme {
                PracticeScreen(
                    uiState = PracticeUiState.Empty(),
                    onRetry = {},
                    onFreeMeditationClick = { freeMeditationClickedCount++ },
                    onBreathingExerciseClick = {},
                    onPathClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Free meditation").performClick()
        assertEquals(1, freeMeditationClickedCount)
    }

    @Test
    fun breathingExerciseCardTriggersCallback() {
        var breathingClicked = false
        composeRule.setContent {
            MindoraTheme {
                PracticeScreen(
                    uiState = PracticeUiState.Empty(),
                    onRetry = {},
                    onFreeMeditationClick = {},
                    onBreathingExerciseClick = { breathingClicked = true },
                    onPathClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Breathing exercise").performClick()
        assertTrue(breathingClicked)
    }

    @Test
    fun weeklyGoalDataRendersCorrectlyAndHasSemantics() {
        composeRule.setContent {
            MindoraTheme {
                PracticeScreen(
                    uiState = PracticeUiState.Content(
                        paths = emptyList(),
                        weeklyGoal = WeeklyGoalUiModel(
                            practicedDuration = Duration.ofMinutes(30),
                            targetMinutes = 60,
                            progressFraction = 0.5f,
                            isReached = false,
                        ),
                    ),
                    onRetry = {},
                    onFreeMeditationClick = {},
                    onBreathingExerciseClick = {},
                    onPathClick = {},
                )
            }
        }

        composeRule.onNodeWithText("30/60").assertIsDisplayed()
        composeRule.onNodeWithText("50%").assertDoesNotExist()
        composeRule.onNodeWithText("30 of 60 minutes").assertIsDisplayed()
        composeRule.onNodeWithText("Weekly goal in progress").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("30 of 60 minutes").assertIsDisplayed()
        composeRule.onNodeWithText("Mindfulness paths").assertDoesNotExist()
    }

    @Test
    fun expandedLayoutBalancesActionCardHeights() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides Configuration().apply { screenWidthDp = 700 },
            ) {
                MindoraTheme {
                    PracticeScreen(
                        uiState = PracticeUiState.Empty(),
                        onRetry = {},
                        onFreeMeditationClick = {},
                        onBreathingExerciseClick = {},
                        onPathClick = {},
                    )
                }
            }
        }

        val freeMeditationHeight = composeRule.onNodeWithText("Free meditation")
            .fetchSemanticsNode()
            .boundsInRoot
            .height
        val breathingExerciseHeight = composeRule.onNodeWithText("Breathing exercise")
            .fetchSemanticsNode()
            .boundsInRoot
            .height

        assertEquals(
            freeMeditationHeight.toDouble(),
            breathingExerciseHeight.toDouble(),
            0.5,
        )
    }

    @Test
    fun topAppBarShowsAppNameAndPracticeHeading() {
        composeRule.setContent {
            MindoraTheme {
                PracticeScreen(
                    uiState = PracticeUiState.Empty(),
                    onRetry = {},
                    onFreeMeditationClick = {},
                    onBreathingExerciseClick = {},
                    onPathClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Mindora").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithContentDescription("Mindora").fetchSemanticsNodes().size,
        )
        check(
            composeRule.onNodeWithText("Practice")
                .fetchSemanticsNode()
                .config
                .contains(SemanticsProperties.Heading),
        )
    }
}
