package com.cydoniancitizen.mindora

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cydoniancitizen.mindora.feature.breathing.BreathingExerciseConfig
import com.cydoniancitizen.mindora.feature.breathing.BreathingExerciseScreen
import com.cydoniancitizen.mindora.feature.breathing.BreathingExerciseUiState
import com.cydoniancitizen.mindora.feature.breathing.LinkedBreathingExerciseDetails
import com.cydoniancitizen.mindora.feature.freemeditation.FreeMeditationScreen
import com.cydoniancitizen.mindora.feature.freemeditation.FreeMeditationUiState
import com.cydoniancitizen.mindora.feature.freemeditation.LinkedFreeMeditationDetails
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import org.junit.Rule
import org.junit.Test

class LinkedPracticeRuntimeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun linkedFreeSetupShowsCatalogueContentAndNoDurationChoices() {
        composeRule.setContent {
            MindoraTheme {
                FreeMeditationScreen(
                    uiState = FreeMeditationUiState.Setup(linkedContent = linkedFree),
                    onBack = {},
                    onSelectDuration = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onRequestEnd = {},
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("Linked free").assertIsDisplayed()
        composeRule.onNodeWithText("Linked free description.").assertIsDisplayed()
        composeRule.onNodeWithText("Duration: 02:00").assertIsDisplayed()
        composeRule.onNodeWithText("5 minutes").assertDoesNotExist()
        composeRule.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun linkedBreathingSetupShowsCatalogueContentAndConfiguration() {
        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = BreathingExerciseUiState.Setup(
                        config = linkedBreathing.config,
                        linkedContent = linkedBreathing,
                    ),
                    onBack = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onRequestEnd = {},
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("Linked breathing").assertIsDisplayed()
        composeRule.onNodeWithText("Linked breathing description.").assertIsDisplayed()
        composeRule.onNodeWithText("2 cycles").assertIsDisplayed()
        composeRule.onNodeWithText("About 20 seconds").assertIsDisplayed()
        composeRule.onNodeWithText("Start").assertIsDisplayed()
    }

    private companion object {
        val linkedFree = LinkedFreeMeditationDetails(
            pathId = "path",
            stepId = "free",
            title = "Linked free",
            description = "Linked free description.",
            plannedDuration = Duration.ofMinutes(2),
        )
        val linkedBreathing = LinkedBreathingExerciseDetails(
            pathId = "path",
            stepId = "breathing",
            title = "Linked breathing",
            description = "Linked breathing description.",
            config = BreathingExerciseConfig(
                inhaleDuration = Duration.ofSeconds(3),
                holdAfterInhaleDuration = Duration.ofSeconds(1),
                exhaleDuration = Duration.ofSeconds(5),
                holdAfterExhaleDuration = Duration.ofSeconds(1),
                cycles = 2,
            ),
        )
    }
}
