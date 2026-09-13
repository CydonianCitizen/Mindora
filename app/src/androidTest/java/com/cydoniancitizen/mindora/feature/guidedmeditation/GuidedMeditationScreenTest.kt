package com.cydoniancitizen.mindora.feature.guidedmeditation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import org.junit.Rule
import org.junit.Test

class GuidedMeditationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyShowsContentDurationAndStartWithoutSeekControls() {
        composeRule.setContent {
            MindoraTheme {
                TestScreen(GuidedMeditationUiState.Ready(details))
            }
        }

        composeRule.onNodeWithText("Guided title").assertIsDisplayed()
        composeRule.onNodeWithText("Guided description").assertIsDisplayed()
        composeRule.onNodeWithText("Duration: 05:00").assertIsDisplayed()
        composeRule.onNodeWithText("Start").assertIsDisplayed()
        composeRule.onNodeWithText("Seek").assertDoesNotExist()
        composeRule.onNodeWithText("Next").assertDoesNotExist()
        composeRule.onNodeWithText("Previous").assertDoesNotExist()
    }

    @Test
    fun playingShowsReadOnlyProgressPauseEndAndBackConfirmation() {
        composeRule.setContent {
            MindoraTheme {
                TestScreen(
                    GuidedMeditationUiState.Playing(
                        meditation = details,
                        position = Duration.ofSeconds(45),
                        totalDuration = Duration.ofMinutes(5),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Playing").assertIsDisplayed()
        composeRule.onNodeWithText("00:45 / 05:00").assertIsDisplayed()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()
        composeRule.onNodeWithText("End").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Navigate back").performClick()
        composeRule.onNodeWithText("End this meditation?").assertIsDisplayed()
        composeRule.onNodeWithText("Continue meditation").assertIsDisplayed()
        composeRule.onNodeWithText("End meditation").assertIsDisplayed()
    }

    @Test
    fun pausedSaveFailureAndFinishedExposeRequiredActions() {
        var state by mutableStateOf<GuidedMeditationUiState>(
            GuidedMeditationUiState.Paused(
                details,
                Duration.ofSeconds(10),
                Duration.ofMinutes(5),
            ),
        )
        composeRule.setContent {
            MindoraTheme { TestScreen(state) }
        }
        composeRule.onNodeWithText("Resume").assertIsDisplayed()

        state = GuidedMeditationUiState.SaveFailed(details, Duration.ofSeconds(10))
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Discard").assertIsDisplayed()

        state = GuidedMeditationUiState.Finished(
            details,
            MindfulnessSessionStatus.COMPLETED,
            Duration.ofSeconds(10),
        )
        composeRule.onNodeWithText("Guided meditation completed", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @androidx.compose.runtime.Composable
    private fun TestScreen(state: GuidedMeditationUiState) {
        GuidedMeditationScreen(
            uiState = state,
            onNavigateBack = {},
            onStart = {},
            onPlay = {},
            onPause = {},
            onEnd = {},
            onRetrySave = {},
            onDiscard = {},
            onClear = {},
        )
    }

    private companion object {
        val details = GuidedMeditationDetails(
            pathId = "test-path",
            stepId = "test-guided",
            title = "Guided title",
            description = "Guided description",
            plannedDuration = Duration.ofMinutes(5),
        )
    }
}
