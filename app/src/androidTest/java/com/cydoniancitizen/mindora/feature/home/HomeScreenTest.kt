package com.cydoniancitizen.mindora.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noGoalShowsWeeklySummaryAndSettingsAction() {
        var openedSettings = false
        composeRule.setContent {
            MindoraTheme {
                HomeScreen(
                    uiState = HomeUiState.Content(Duration.ZERO, null, 0f, false),
                    onOpenSettings = { openedSettings = true },
                )
            }
        }

        composeRule.onNodeWithText("This week").assertIsDisplayed()
        composeRule.onNodeWithText("No weekly goal is set.").assertIsDisplayed()
        composeRule.onNodeWithText("Set a goal").performClick()
        assertTrue(openedSettings)
    }

    @Test
    fun configuredGoalShowsExactTextProgressAndStatus() {
        composeRule.setContent {
            MindoraTheme {
                HomeScreen(
                    uiState = HomeUiState.Content(
                        practicedDuration = Duration.ofMinutes(25),
                        targetMinutes = 60,
                        progressFraction = 25f / 60f,
                        isReached = false,
                    ),
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("25 of 60 minutes").assertIsDisplayed()
        composeRule.onNodeWithText("Weekly goal in progress").assertIsDisplayed()
    }
}
