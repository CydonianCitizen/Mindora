package com.cydoniancitizen.mindora.feature.practice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import org.junit.Assert.assertEquals
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
                    uiState = PracticeUiState.Empty,
                    onRetry = {},
                    onFreeMeditationClick = {},
                    onBreathingExerciseClick = {},
                    onPathClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Free meditation").assertIsDisplayed()
        composeRule.onNodeWithText("Breathing exercise").assertIsDisplayed()
        composeRule.onNodeWithText("No mindfulness paths are included yet.").assertIsDisplayed()
    }
}
