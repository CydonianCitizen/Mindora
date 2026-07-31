package com.cydoniancitizen.mindora.feature.settings

import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.mindora.BuildConfig
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.LocalTime
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun realWeeklyGoalRendersAndStepperUsesSupportedIncrements() {
        var preferences by mutableStateOf(MindoraPreferences(weeklyGoalMinutes = 60))
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = preferences,
                    onWeeklyGoalSelected = {
                        preferences = preferences.copy(weeklyGoalMinutes = it)
                    },
                )
            }
        }

        composeRule.onNodeWithText("60 minutes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Increase weekly goal, current value: 60 minutes",
        ).performClick()
        composeRule.onNodeWithText("90 minutes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Decrease weekly goal, current value: 90 minutes",
        ).performClick()
        composeRule.onNodeWithText("60 minutes").assertIsDisplayed()
    }

    @Test
    fun weeklyGoalStepperDisablesControlsAtMinuteBoundaries() {
        var goal by mutableStateOf<Int?>(30)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(weeklyGoalMinutes = goal),
                    onWeeklyGoalSelected = { goal = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            "Decrease weekly goal, current value: 30 minutes",
        ).assertIsNotEnabled()

        composeRule.runOnIdle { goal = 120 }
        composeRule.onNodeWithContentDescription(
            "Increase weekly goal, current value: 120 minutes",
        ).assertIsNotEnabled()
    }

    @Test
    fun offGoalCanBeEnabledAndExistingGoalCanBeTurnedOff() {
        var goal by mutableStateOf<Int?>(null)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(weeklyGoalMinutes = goal),
                    onWeeklyGoalSelected = { goal = it },
                )
            }
        }

        composeRule.onNodeWithText("Off").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Increase weekly goal, current value: Off",
        ).performClick()
        assertEquals(30, goal)
        composeRule.onNodeWithText("Turn off weekly goal").performClick()
        assertEquals(null, goal)
    }

    @Test
    fun disabledReminderShowsLocalizedTimeAndIsNotActionable() {
        val time = LocalTime.of(7, 35)
        val displayTime = localizedTime(time)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(
                        dailyReminderEnabled = false,
                        dailyReminderTime = time,
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Daily reminder").assertIsOff()
        composeRule.onNodeWithText(displayTime).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reminder time, $displayTime")
            .assertIsNotEnabled()
    }

    @Test
    fun enabledReminderToggleEmitsActionAndTimeRowIsEnabled() {
        var toggleValue: Boolean? = null
        val time = LocalTime.of(20, 0)
        val displayTime = localizedTime(time)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(
                        dailyReminderEnabled = true,
                        dailyReminderTime = time,
                    ),
                    onReminderToggle = { toggleValue = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Daily reminder")
            .assertIsOn()
            .performClick()
        assertFalse(toggleValue ?: true)
        composeRule.onNodeWithContentDescription("Reminder time, $displayTime")
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun aboutUsesBuildConfigAndUnsupportedHtmlControlsAreAbsent() {
        val version = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        composeRule.setContent {
            MindoraTheme { SettingsScreenUnderTest(preferences = MindoraPreferences()) }
        }

        composeRule.onNodeWithText(version).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Privacy first").assertIsDisplayed()
        composeRule.onNodeWithText("Preferred session length").assertDoesNotExist()
        composeRule.onNodeWithText("Reset all data").assertDoesNotExist()
    }

    @Test
    fun pageSectionsAndGoalActionsExposeAccessibilitySemantics() {
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(weeklyGoalMinutes = 60),
                )
            }
        }

        composeRule.onNodeWithText("Settings").assert(heading)
        composeRule.onNodeWithText("Practice goals").assert(heading)
        composeRule.onNodeWithText("Reminders").assert(heading)
        composeRule.onNodeWithText("About").assert(heading)
        composeRule.onNodeWithContentDescription(
            "Decrease weekly goal, current value: 60 minutes",
        ).assertIsEnabled()
        composeRule.onNodeWithContentDescription("Daily reminder").assertIsOff()
    }

    @Test
    fun blockedNotificationsShowSystemExplanation() {
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(dailyReminderEnabled = true),
                    notificationsAllowed = false,
                )
            }
        }

        composeRule.onNodeWithText(
            "Daily reminder is enabled, but notifications are blocked by system settings.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Open notification settings").assertIsDisplayed()
    }

    @androidx.compose.runtime.Composable
    private fun SettingsScreenUnderTest(
        preferences: MindoraPreferences,
        notificationsAllowed: Boolean = true,
        onWeeklyGoalSelected: (Int?) -> Unit = {},
        onReminderToggle: (Boolean) -> Unit = {},
    ) {
        SettingsScreen(
            uiState = SettingsUiState.Content(preferences),
            notificationsAllowed = notificationsAllowed,
            permissionDenied = false,
            onWeeklyGoalSelected = onWeeklyGoalSelected,
            onReminderToggle = onReminderToggle,
            onReminderTimeSelected = {},
            onOpenNotificationSettings = {},
            onDismissError = {},
        )
    }

    private fun localizedTime(time: LocalTime): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
        }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }
}
