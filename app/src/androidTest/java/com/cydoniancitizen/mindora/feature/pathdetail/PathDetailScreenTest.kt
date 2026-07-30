package com.cydoniancitizen.mindora.feature.pathdetail

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PathDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentShowsOrderedAccessibleRepeatableSteps() {
        val selected = mutableListOf<Pair<PathStepType, String>>()
        composeRule.setContent {
            MindoraTheme {
                PathDetailScreen(
                    uiState = content,
                    onNavigateBack = {},
                    onStepClick = { type, id -> selected += type to id },
                )
            }
        }

        composeRule.onNodeWithText("Test path").assertIsDisplayed()
        composeRule.onNodeWithText("Test path description.").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 3 steps completed").assertIsDisplayed()
        composeRule.onNodeWithText("1. Guided step").assertIsDisplayed()
        composeRule.onNodeWithText("Completed").assertIsDisplayed()
        composeRule.onNodeWithText("1. Guided step").performClick()
        assertEquals(PathStepType.GUIDED_MEDITATION to "guided-step", selected.single())

        composeRule.onNodeWithText("2. Free step").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Not completed").assertCountEquals(2)
        composeRule.onNodeWithText("3. Breathing step").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Duration: 00:20").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unavailableProvidesBackAction() {
        var backed = false
        composeRule.setContent {
            MindoraTheme {
                PathDetailScreen(
                    uiState = PathDetailUiState.Unavailable,
                    onNavigateBack = { backed = true },
                    onStepClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("This mindfulness path is unavailable.").assertIsDisplayed()
        composeRule.onNodeWithText("Back to Practice").performClick()
        assertEquals(true, backed)
    }

    private companion object {
        val content = PathDetailUiState.Content(
            title = "Test path",
            description = "Test path description.",
            completedSteps = 1,
            totalSteps = 3,
            progressFraction = 1f / 3f,
            steps = listOf(
                PathStepUiModel(
                    id = "guided-step",
                    title = "Guided step",
                    description = "Guided description.",
                    type = PathStepType.GUIDED_MEDITATION,
                    ordinal = 1,
                    completed = true,
                    displayDuration = Duration.ofMinutes(3),
                ),
                PathStepUiModel(
                    id = "free-step",
                    title = "Free step",
                    description = "Free description.",
                    type = PathStepType.FREE_MEDITATION,
                    ordinal = 2,
                    completed = false,
                    displayDuration = Duration.ofMinutes(2),
                ),
                PathStepUiModel(
                    id = "breathing-step",
                    title = "Breathing step",
                    description = "Breathing description.",
                    type = PathStepType.BREATHING_EXERCISE,
                    ordinal = 3,
                    completed = false,
                    displayDuration = Duration.ofSeconds(20),
                ),
            ),
        )
    }
}
