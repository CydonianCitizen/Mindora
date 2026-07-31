package com.cydoniancitizen.mindora.feature.freemeditation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FreeMeditationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backNavigationIconTriggersBackCallback() {
        var backClicked = false
        composeRule.setContent {
            MindoraTheme {
                FreeMeditationScreen(
                    uiState = FreeMeditationUiState.Setup(),
                    onBack = { backClicked = true },
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

        composeRule.onNodeWithContentDescription("Navigate back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun initialDurationDisplaysDefaultTenMinutesAndSelectedDurationAccessibilityDescription() {
        composeRule.setContent {
            MindoraTheme {
                FreeMeditationScreen(
                    uiState = FreeMeditationUiState.Setup(),
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

        composeRule.onNodeWithText("Mindora").assertIsDisplayed()
        composeRule.onNodeWithText("Free meditation").assertIsDisplayed()
        composeRule.onNodeWithText("10:00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Selected duration: 10 minutes").assertIsDisplayed()
        composeRule.onNodeWithText("Duration").assertIsDisplayed()
        composeRule.onNodeWithText("10 minutes").assertIsSelected()
        composeRule.onNodeWithText("15 minutes").assertIsNotSelected()
        composeRule.onNodeWithText("30 minutes").assertIsDisplayed()
    }

    @Test
    fun selectingDurationChipUpdatesTimerDisplayAndSelectionState() {
        var selectedDuration = Duration.ofMinutes(10)
        composeRule.setContent {
            var state by rememberState { FreeMeditationUiState.Setup(selectedDuration = selectedDuration) }
            MindoraTheme {
                FreeMeditationScreen(
                    uiState = state,
                    onBack = {},
                    onSelectDuration = { newDuration ->
                        selectedDuration = newDuration
                        state = state.copy(selectedDuration = newDuration)
                    },
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

        composeRule.onNodeWithText("15 minutes").performClick()
        assertEquals(Duration.ofMinutes(15), selectedDuration)
        composeRule.onNodeWithText("15:00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Selected duration: 15 minutes").assertIsDisplayed()
        composeRule.onNodeWithText("15 minutes").assertIsSelected()
        composeRule.onNodeWithText("10 minutes").assertIsNotSelected()
    }

    @Test
    fun startSessionButtonTriggersStartCallbackWithSelectedDurationSemantics() {
        var startClicked = false
        composeRule.setContent {
            MindoraTheme {
                FreeMeditationScreen(
                    uiState = FreeMeditationUiState.Setup(selectedDuration = Duration.ofMinutes(15)),
                    onBack = {},
                    onSelectDuration = {},
                    onStart = { startClicked = true },
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

        composeRule.onNodeWithContentDescription("Start session, 15 minutes").performClick()
        assertTrue(startClicked)
    }

    @Test
    fun unsupportedOptionalSettingsAreNotDisplayed() {
        composeRule.setContent {
            MindoraTheme {
                FreeMeditationScreen(
                    uiState = FreeMeditationUiState.Setup(),
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

        composeRule.onNodeWithText("Ending Bell").assertDoesNotExist()
        composeRule.onNodeWithText("Warm Screen").assertDoesNotExist()
    }

    private inline fun <T> rememberState(crossinline initial: () -> T) = mutableStateOf(initial())
}
