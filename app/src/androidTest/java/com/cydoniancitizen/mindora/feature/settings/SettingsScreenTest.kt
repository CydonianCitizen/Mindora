package com.cydoniancitizen.mindora.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fiveGoalChoicesUpdateSelectionAndReminderDefaultsDisabled() {
        var preferences by mutableStateOf(MindoraPreferences())
        composeRule.setContent {
            MindoraTheme {
                SettingsScreen(
                    uiState = SettingsUiState.Content(preferences),
                    notificationsAllowed = true,
                    permissionDenied = false,
                    onWeeklyGoalSelected = {
                        preferences = preferences.copy(weeklyGoalMinutes = it)
                    },
                    onReminderToggle = {},
                    onReminderTimeSelected = {},
                    onOpenNotificationSettings = {},
                    onDismissError = {},
                )
            }
        }

        listOf("Off", "30 minutes", "60 minutes", "90 minutes", "120 minutes")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("60 minutes").performClick()
        composeRule.onNodeWithText("60 minutes").assertIsSelected()
        composeRule.onNodeWithText("Delivery occurs around the selected time.")
            .assertIsDisplayed()
    }

    @Test
    fun blockedNotificationsShowSystemExplanation() {
        composeRule.setContent {
            MindoraTheme {
                SettingsScreen(
                    uiState = SettingsUiState.Content(
                        MindoraPreferences(dailyReminderEnabled = true),
                    ),
                    notificationsAllowed = false,
                    permissionDenied = false,
                    onWeeklyGoalSelected = {},
                    onReminderToggle = {},
                    onReminderTimeSelected = {},
                    onOpenNotificationSettings = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithText(
            "Daily reminder is enabled, but notifications are blocked by system settings.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Open notification settings").assertIsDisplayed()
    }
}
